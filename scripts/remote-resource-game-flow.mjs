import path from "node:path";
import { delay, ensureBotBaseline, resolveBotClientModuleUrl, waitForPageOrNull, writeJson, printStructured, captureWorldSnapshot, createTraceSession } from "./bot-flow-helpers.mjs";

const PAGE_KEYS = {
  upgrades: "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage",
  debug: "com.tavall.hytale.resourcegame.ui.DebugNavigatorPage",
  interior: "com.tavall.hytale.resourcegame.ui.InteriorMainPage"
};

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "ResourceGameBot";
  const uuid = process.argv[5] ?? process.env.HYTALE_BOT_UUID;
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "resource-game-flow");
  const resultPath = path.join(outputDir, "scenario-result.txt");

  const startedAt = new Date().toISOString();
  const assertions = [];
  const pages = [];
  const commandMessages = [];

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
      nearbyRadius: 16
    });
    await delay(1_000);

    bot.chat("/kingdom resources set wood 80");
    await delay(300);
    bot.chat("/kingdom troops set 1");
    await delay(300);
    bot.chat("/kingdom citizens set 12");
    await delay(500);
    assertions.push("command-mutations");

    bot.chat("/kingdom ui upgrades");
    const upgradesPage = await bot.waitForPage(PAGE_KEYS.upgrades, 5_000);
    const upgradesSnapshot = bot.snapshotPage();
    const upgradesSnapshotJson = JSON.stringify(upgradesSnapshot);
    if (!upgradesSnapshotJson.includes("12") || !upgradesSnapshotJson.includes("80") || !upgradesSnapshotJson.includes("1")) {
      throw new Error(`Upgrade page did not contain expected values: ${upgradesSnapshotJson}`);
    }
    pages.push({ key: upgradesPage.key, title: upgradesPage.title ?? null, snapshot: upgradesSnapshot });
    assertions.push("upgrades-ui-opened");
    assertions.push("upgrades-ui-values");

    bot.chat("/kingdom ui debug");
    const debugPage = await bot.waitForPage(PAGE_KEYS.debug, 5_000);
    pages.push({ key: debugPage.key, title: debugPage.title ?? null, snapshot: bot.snapshotPage() });
    assertions.push("debug-ui-opened");

    bot.chat("/kingdom interior");
    await bot.waitForWorldActivity(20_000);
    assertions.push("entered-interior");
    const interiorPage = await waitForPageOrNull(bot, PAGE_KEYS.interior, 20_000);
    if (interiorPage) {
      pages.push({ key: interiorPage.key, title: interiorPage.title ?? null, snapshot: bot.snapshotPage() });
      assertions.push("interior-ui-opened");
    }

    bot.chat("/kingdom interior exit");
    await bot.waitForWorldActivity(10_000);
    await delay(1_000);
    assertions.push("exited-interior");

    const finalSnapshot = captureWorldSnapshot(bot, 16);
    const result = {
      name: "resource-game-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      commandMessages,
      outputDir,
      clientSnapshot: {
        baseline: baseline.snapshot,
        final: finalSnapshot
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await trace.flush();
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "resource-game-flow",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      commandMessages,
      outputDir,
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
