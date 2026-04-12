import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicClientCodecBuilder;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.handler.codec.quic.QuicStreamType;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridges the existing TCP-only bot harness into the Hytale QUIC listener.
 * The bot still opens a local TCP socket, while this bridge opens a single
 * bidirectional QUIC stream to the real server and forwards raw protocol bytes.
 */
public final class HytaleQuicTcpBridge {

    private static final String[] APPLICATION_PROTOCOLS = {"hytale/2", "hytale/1"};

    private HytaleQuicTcpBridge() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: HytaleQuicTcpBridge <tcp-host> <tcp-port> <quic-host> <quic-port>");
            System.exit(2);
            return;
        }

        String tcpHost = args[0];
        int tcpPort = Integer.parseInt(args[1]);
        String quicHost = args[2];
        int quicPort = Integer.parseInt(args[3]);

        QuicSslContext sslContext = buildClientSslContext();
        InetSocketAddress quicAddress = new InetSocketAddress(quicHost, quicPort);

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(tcpHost, tcpPort));
            System.out.printf(
                "[%s] Bridge ready tcp=%s:%d quic=%s:%d%n",
                Instant.now(),
                tcpHost,
                tcpPort,
                quicHost,
                quicPort
            );

            while (true) {
                Socket tcpSocket = serverSocket.accept();
                tcpSocket.setTcpNoDelay(true);
                Thread workerThread = new Thread(
                    new BridgeWorker(tcpSocket, quicAddress, sslContext),
                    "hytale-quic-bridge-" + tcpSocket.getPort()
                );
                workerThread.setDaemon(true);
                workerThread.start();
            }
        }
    }

    private static QuicSslContext buildClientSslContext() throws CertificateException {
        SelfSignedCertificate certificate = new SelfSignedCertificate("localhost");
        return QuicSslContextBuilder.forClient()
            .keyManager(certificate.key(), null, certificate.cert())
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .applicationProtocols(APPLICATION_PROTOCOLS)
            .earlyData(false)
            .build();
    }

    static void logError(String message, Throwable throwable) {
        System.err.printf("[%s] %s%n", Instant.now(), message);
        if (throwable != null) {
            throwable.printStackTrace(System.err);
        }
    }

    static void logInfo(String message) {
        System.out.printf("[%s] %s%n", Instant.now(), message);
    }
}

final class BridgeWorker implements Runnable {

    private final Socket tcpSocket;
    private final InetSocketAddress quicAddress;
    private final QuicSslContext sslContext;

    BridgeWorker(Socket tcpSocket, InetSocketAddress quicAddress, QuicSslContext sslContext) {
        this.tcpSocket = tcpSocket;
        this.quicAddress = quicAddress;
        this.sslContext = sslContext;
    }

    @Override
    public void run() {
        AtomicBoolean closed = new AtomicBoolean(false);
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
        Channel udpChannel = null;
        QuicChannel quicChannel = null;
        QuicStreamChannel quicStream = null;
        try (Socket socket = this.tcpSocket;
             InputStream tcpInput = socket.getInputStream();
             OutputStream tcpOutput = new BufferedOutputStream(socket.getOutputStream())) {
            udpChannel = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioDatagramChannel.class)
                .handler(new QuicClientCodecBuilder()
                    .sslContext(this.sslContext)
                    .maxIdleTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .initialMaxData(10_000_000)
                    .initialMaxStreamDataBidirectionalLocal(1_000_000)
                    .initialMaxStreamDataBidirectionalRemote(1_000_000)
                    .initialMaxStreamDataUnidirectional(1_000_000)
                    .initialMaxStreamsBidirectional(128)
                    .initialMaxStreamsUnidirectional(128)
                    .build())
                .bind(0)
                .sync()
                .channel();

            quicChannel = QuicChannel.newBootstrap(udpChannel)
                .handler(new ChannelInboundHandlerAdapter())
                .streamHandler(new ChannelInboundHandlerAdapter())
                .remoteAddress(this.quicAddress)
                .connect()
                .sync()
                .getNow();

            quicStream = quicChannel.createStream(
                QuicStreamType.BIDIRECTIONAL,
                new QuicToTcpForwardHandler(tcpOutput, closed, socket)
            ).sync().getNow();

            Thread tcpPump = new Thread(
                new TcpToQuicPump(tcpInput, quicStream, closed, socket),
                "hytale-quic-bridge-pump-" + socket.getPort()
            );
            tcpPump.setDaemon(true);
            tcpPump.start();
            tcpPump.join();
        } catch (Exception exception) {
            HytaleQuicTcpBridge.logError("Bridge worker failed", exception);
        } finally {
            closeChannel(quicStream);
            closeChannel(quicChannel);
            closeChannel(udpChannel);
            eventLoopGroup.shutdownGracefully().syncUninterruptibly();
            HytaleQuicTcpBridge.logInfo("Bridge worker closed");
        }
    }

    private static void closeChannel(Channel channel) {
        if (channel == null) {
            return;
        }
        channel.close().syncUninterruptibly();
    }
}

final class TcpToQuicPump implements Runnable {

    private static final int BUFFER_SIZE = 8192;

    private final InputStream tcpInput;
    private final QuicStreamChannel quicStream;
    private final AtomicBoolean closed;
    private final Socket tcpSocket;

    TcpToQuicPump(InputStream tcpInput, QuicStreamChannel quicStream, AtomicBoolean closed, Socket tcpSocket) {
        this.tcpInput = tcpInput;
        this.quicStream = quicStream;
        this.closed = closed;
        this.tcpSocket = tcpSocket;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            while (!this.closed.get()) {
                int read = this.tcpInput.read(buffer);
                if (read < 0) {
                    closeQuietly();
                    return;
                }
                byte[] payload = new byte[read];
                System.arraycopy(buffer, 0, payload, 0, read);
                this.quicStream.eventLoop().submit(
                    () -> this.quicStream.writeAndFlush(Unpooled.wrappedBuffer(payload))
                ).syncUninterruptibly();
            }
        } catch (Exception exception) {
            HytaleQuicTcpBridge.logError("TCP to QUIC forwarding failed", exception);
        } finally {
            closeQuietly();
        }
    }

    private void closeQuietly() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        try {
            this.tcpSocket.close();
        } catch (IOException ignored) {
        }
        this.quicStream.close().syncUninterruptibly();
    }
}

final class QuicToTcpForwardHandler extends ChannelInitializer<QuicStreamChannel> {

    private final OutputStream tcpOutput;
    private final AtomicBoolean closed;
    private final Socket tcpSocket;

    QuicToTcpForwardHandler(OutputStream tcpOutput, AtomicBoolean closed, Socket tcpSocket) {
        this.tcpOutput = tcpOutput;
        this.closed = closed;
        this.tcpSocket = tcpSocket;
    }

    @Override
    protected void initChannel(QuicStreamChannel channel) {
        channel.pipeline().addLast(new QuicToTcpByteHandler(this.tcpOutput, this.closed, this.tcpSocket));
    }
}

final class QuicToTcpByteHandler extends ChannelInboundHandlerAdapter {

    private final OutputStream tcpOutput;
    private final AtomicBoolean closed;
    private final Socket tcpSocket;

    QuicToTcpByteHandler(OutputStream tcpOutput, AtomicBoolean closed, Socket tcpSocket) {
        this.tcpOutput = tcpOutput;
        this.closed = closed;
        this.tcpSocket = tcpSocket;
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if (!(message instanceof ByteBuf byteBuf)) {
            super.channelRead(context, message);
            return;
        }
        try {
            byte[] payload = ByteBufUtil.getBytes(byteBuf);
            synchronized (this.tcpOutput) {
                this.tcpOutput.write(payload);
                this.tcpOutput.flush();
            }
        } finally {
            byteBuf.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        closeQuietly(context);
        super.channelInactive(context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        HytaleQuicTcpBridge.logError("QUIC to TCP forwarding failed", cause);
        closeQuietly(context);
    }

    private void closeQuietly(ChannelHandlerContext context) {
        if (!this.closed.compareAndSet(false, true)) {
            context.close();
            return;
        }
        try {
            this.tcpSocket.close();
        } catch (IOException ignored) {
        }
        context.close();
    }
}
