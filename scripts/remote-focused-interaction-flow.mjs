import path from "node:path";
import {
  aimAtGround,
  captureWorldSnapshot,
  createTraceSession,
  delay,
  ensureBotBaseline,
  resolveBotClientModuleUrl,
  writeJson, printStructured,
} from "./bot-flow-helpers.mjs";

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "FocusBot";
  const uuid = process.argv[5] ?? "823e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "focused-interaction-flow");
  const resultPath = path.join(outputDir, "scenario-result.txt");
  const startedAt = new Date().toISOString();
  const assertions = [];
  const pages = [];

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
      op: false,
      nearbyRadius: 16,
      settleDelayMs: 600
    });

    bot.chat("/kingdom castle align");
    await delay(1_250);

    bot.chat("/kingdom interact");
    const castlePage = await bot.waitForPage("com.tavall.hytale.resourcegame.ui.CastleMainPage", 8_000);
    pages.push({ key: castlePage.key, title: castlePage.title ?? null, snapshot: bot.snapshotPage() });
    assertions.push("castle-ui-opened-from-interact");
    bot.sendPageEvent("Dismiss", null);
    await delay(400);

    bot.chat("/kingdom nodes clear");
    await delay(600);
    bot.chat("/kingdom place node iron");
    await delay(600);
    await aimAtGround(bot, 90, 60, 0, 900);
    bot.chat("/kingdom place confirm");
    await delay(1_200);
    assertions.push("iron-node-placed");

    bot.chat("/kingdom nodes align 1");
    await delay(1_250);

    bot.chat("/kingdom interact");
    const nodePage = await bot.waitForPage("com.tavall.hytale.resourcegame.ui.ResourceNodePage", 8_000);
    pages.push({ key: nodePage.key, title: nodePage.title ?? null, snapshot: bot.snapshotPage() });
    assertions.push("node-ui-opened-from-interact");

    const result = {
      name: "remote-focused-interaction-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      clientSnapshot: {
        baseline: baseline.snapshot,
        final: captureWorldSnapshot(bot, 16)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await trace.flush();
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "remote-focused-interaction-flow",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      error: error instanceof Error ? error.message : String(error),
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
