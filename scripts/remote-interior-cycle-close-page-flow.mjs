import path from "node:path";
import {
  captureWorldSnapshot,
  createTraceSession,
  delay,
  ensureBotBaseline,
  resolveBotClientModuleUrl,
  waitForPageOrNull,
  writeJson,
  printStructured
} from "./bot-flow-helpers.mjs";

function sendBoundAction(bot, selector, fallbackAction) {
  const snapshot = bot.snapshotPage();
  const binding = snapshot?.eventBindings?.find((entry) => entry.selector === selector && entry.data);
  bot.sendPageEvent("Data", binding?.data ?? JSON.stringify({ Action: fallbackAction }));
}

async function waitForSnapshot(bot, key, selector, timeoutMs, label) {
  const startedAt = Date.now();
  while ((Date.now() - startedAt) < timeoutMs) {
    const snapshot = bot.snapshotPage();
    if (snapshot?.key === key && (!selector || snapshot.selectors.includes(selector))) {
      return snapshot;
    }
    await delay(150);
  }
  throw new Error(`Timed out waiting for ${label}`);
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "InteriorCycleBot";
  const uuid = process.argv[5] ?? "423e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "interior-cycle-close-page-flow");
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
      nearbyRadius: 12
    });
    await delay(3_000);

    for (let cycle = 1; cycle <= 3; cycle += 1) {
      await delay(1_500);

      bot.chat("/kingdom interior");
      await bot.waitForWorldActivity(20_000);
      await bot.waitForPage("com.tavall.hytale.resourcegame.ui.InteriorMainPage", 20_000);
      const interiorSnapshot = await waitForSnapshot(
        bot,
        "com.tavall.hytale.resourcegame.ui.InteriorMainPage",
        "#ExitInteriorButton",
        8_000,
        `interior page on cycle ${cycle}`
      );
      pages.push({ key: `cycle-${cycle}-interior`, snapshot: interiorSnapshot });
      assertions.push(`entered-interior-cycle-${cycle}`);

      sendBoundAction(bot, "#ExitInteriorButton", "ExitInterior");
      const uiReturnPage = await waitForPageOrNull(bot, "com.tavall.hytale.resourcegame.ui.CastleMainPage", 7_500);
      if (!uiReturnPage) {
        assertions.push(`exit-ui-event-fallback-cycle-${cycle}`);
        bot.chat("/kingdom interior exit");
      }
      await bot.waitForWorldActivity(20_000);
      await bot.waitForPage("com.tavall.hytale.resourcegame.ui.CastleMainPage", 20_000);
      const returnSnapshot = await waitForSnapshot(
        bot,
        "com.tavall.hytale.resourcegame.ui.CastleMainPage",
        "#EnterInteriorButton",
        8_000,
        `castle return page on cycle ${cycle}`
      );
      pages.push({ key: `cycle-${cycle}-return`, snapshot: returnSnapshot });
      assertions.push(`exited-interior-cycle-${cycle}`);
      await delay(1_500);
    }

    sendBoundAction(bot, "#CloseButton", "Close");
    assertions.push("closed-castle-page-before-disconnect");
    await delay(1_500);

    const result = {
      name: "remote-interior-cycle-close-page-flow",
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
    await trace.flush();
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "remote-interior-cycle-close-page-flow",
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
