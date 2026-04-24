import path from "node:path";
import { captureWorldSnapshot, delay, ensureBotBaseline, resolveBotClientModuleUrl, writeJson, printStructured, } from "./bot-flow-helpers.mjs";

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "CastleBot";
  const uuid = process.argv[5] ?? process.env.HYTALE_BOT_UUID;
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "castle-interaction-flow");
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

  try {
    await bot.trace.enable({ outputDir });
    const baseline = await ensureBotBaseline(bot, assertions, {
      username,
      nearbyRadius: 12
    });
    await delay(2_000);

    if (bot.ui.currentPage != null) {
      throw new Error(`Expected no passive castle UI on join, but saw ${bot.ui.currentPage.key}`);
    }
    assertions.push("castle-ui-not-auto-opened");

    bot.chat("/kingdom castle open");
    const castlePage = await bot.waitForPage("com.tavall.hytale.resourcegame.ui.CastleMainPage", 8_000);
    pages.push({ key: castlePage.key, title: castlePage.title ?? null, snapshot: bot.snapshotPage() });
    assertions.push("castle-ui-opened-by-command");

    const result = {
      name: "castle-interaction-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      clientSnapshot: {
        baseline: baseline.snapshot,
        final: captureWorldSnapshot(bot, 12)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "castle-interaction-flow",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
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
