import path from "node:path";
import {
  captureWorldSnapshot,
  createTraceSession,
  delay,
  ensureBotBaseline,
  printStructured,
  resolveBotClientModuleUrl,
  writeJson
} from "./bot-flow-helpers.mjs";

async function sendCommand(bot, command, assertions, delayMs = 750) {
  bot.chat(command);
  await delay(delayMs);
  assertions.push(`${command} sent`);
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "AccountHoloBot";
  const uuid = process.argv[5] ?? "423e4567-e89b-12d3-a456-426614174111";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "account-hologram-flow");
  const resultPath = path.join(outputDir, "scenario-result.txt");
  const startedAt = new Date().toISOString();
  const assertions = [];

  const bot = await createBot({
    host,
    port,
    username,
    uuid,
    autoConnect: true,
    autoAcknowledgePages: true
  });
  const trace = createTraceSession(bot, outputDir);

  try {
    await trace.enable();
    const baseline = await ensureBotBaseline(bot, assertions, {
      username,
      nearbyRadius: 12,
      settleDelayMs: 2_000
    });

    await sendCommand(bot, "/kd account status", assertions);
    await sendCommand(bot, "/kd account setlevel 12", assertions);
    await sendCommand(bot, "/kd account debug on", assertions);
    await sendCommand(bot, "/kd account debug status", assertions);
    await sendCommand(bot, "/kd hologram stack Account debug|Hologram test", assertions, 1_500);
    await delay(1_000);
    await sendCommand(bot, "/kd hologram status", assertions);
    await sendCommand(bot, "/kd hologram clear", assertions, 1_000);

    const result = {
      name: "remote-account-hologram-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      clientSnapshot: {
        baseline: baseline.snapshot,
        final: captureWorldSnapshot(bot, 12)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await trace.flush();
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "remote-account-hologram-flow",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      error: error instanceof Error ? error.message : String(error),
      clientSnapshot: {
        final: captureWorldSnapshot(bot, 12)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    try {
      await trace.flush();
      await writeJson(resultPath, result);
    } catch {
    }
    printStructured(result, true);
    process.exitCode = 1;
  } finally {
    await bot.disconnect();
  }
}

await main();
