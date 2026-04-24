import path from "node:path";
import { delay, ensureBotBaseline, resolveBotClientModuleUrl, writeJson, printStructured, captureWorldSnapshot } from "./bot-flow-helpers.mjs";

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "ProbeBot";
  const uuid = process.argv[5] ?? "e23e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "connect-probe");

  const resultPath = path.join(outputDir, "scenario-result.txt");
  const startedAt = new Date().toISOString();
  const assertions = [];

  const bot = await createBot({
    host,
    port,
    username,
    uuid,
    quic: {
      readyTimeoutMs: Number.parseInt(process.env.RESOURCE_GAME_QUIC_READY_TIMEOUT_MS ?? "30000", 10)
    },
    autoConnect: true,
    autoAcknowledgePages: true
  });

  let baselineSnapshot = null;
  try {
    await bot.trace.enable({ outputDir });
    const baseline = await ensureBotBaseline(bot, assertions, {
      username,
      nearbyRadius: 12
    });
    baselineSnapshot = baseline.snapshot;
    await delay(1500);
    assertions.push("idle-waited");

    const result = {
      name: "remote-connect-disconnect",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      clientSnapshot: {
        baseline: baselineSnapshot,
        final: captureWorldSnapshot(bot, 12)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "remote-connect-disconnect",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      clientSnapshot: {
        baseline: baselineSnapshot,
        final: captureWorldSnapshot(bot, 12)
      },
      error: error instanceof Error ? error.message : String(error),
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    try {
      await bot.trace.flush(outputDir);
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

