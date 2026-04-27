import path from "node:path";
import { delay, ensureBotBaseline, resolveBotClientModuleUrl, writeJson, printStructured, captureWorldSnapshot } from "./bot-flow-helpers.mjs";

function readSelectorValue(snapshot, selector) {
  const command = snapshot?.commands?.slice().reverse().find((entry) => entry.type === "Set" && entry.selector === selector);
  if (!command) {
    return null;
  }
  try {
    const parsed = JSON.parse(command.data);
    return parsed?.[0] ?? null;
  } catch {
    return null;
  }
}

async function waitForSnapshot(bot, predicate, timeoutMs, label) {
  const startedAt = Date.now();
  while ((Date.now() - startedAt) < timeoutMs) {
    const snapshot = bot.snapshotPage();
    if (snapshot && predicate(snapshot)) {
      return snapshot;
    }
    await delay(150);
  }
  throw new Error(`Timed out waiting for ${label}`);
}

async function waitForKeyedPage(bot, pageKey, timeoutMs) {
  await bot.waitForPage(pageKey, timeoutMs);
  return waitForSnapshot(bot, (snapshot) => snapshot.key === pageKey, 5_000, `${pageKey} snapshot`);
}

function sendAction(bot, action) {
  bot.sendPageEvent("Data", JSON.stringify({ Action: action }));
}

function isBotConnected(bot) {
  return typeof bot.isConnected === "function" ? bot.isConnected() : true;
}

function summarizePageSnapshot(snapshot) {
  if (!snapshot) {
    return null;
  }
  return {
    key: snapshot.key,
    selectors: snapshot.selectors,
    cacheStatus: readSelectorValue(snapshot, "#CacheStatus.Text"),
    citizenCount: readSelectorValue(snapshot, "#CitizenCount.Text"),
    troopCount: readSelectorValue(snapshot, "#TroopCount.Text"),
    foodCount: readSelectorValue(snapshot, "#FoodCount.Text"),
    woodCount: readSelectorValue(snapshot, "#WoodCount.Text"),
    ironCount: readSelectorValue(snapshot, "#IronCount.Text")
  };
}

async function chatUntilSnapshot(bot, command, predicate, timeoutMs, label) {
  const startedAt = Date.now();
  let lastError = null;
  while ((Date.now() - startedAt) < timeoutMs) {
    if (!isBotConnected(bot)) {
      break;
    }
    try {
      bot.chat(command);
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
      break;
    }

    const retryUntil = Date.now() + 1_500;
    while ((Date.now() - startedAt) < timeoutMs && Date.now() < retryUntil) {
      const snapshot = bot.snapshotPage();
      if (snapshot && predicate(snapshot)) {
        return snapshot;
      }
      await delay(150);
    }
  }

  throw new Error(
    `Timed out waiting for ${label}; finalSnapshot=${JSON.stringify(summarizePageSnapshot(bot.snapshotPage()))}; finalServerMessage=${bot.getServerMessages().at(-1) ?? null}; lastSendError=${lastError}`
  );
}

async function sendActionUntilSnapshot(bot, action, predicate, timeoutMs, label) {
  const startedAt = Date.now();
  let lastError = null;
  while ((Date.now() - startedAt) < timeoutMs) {
    if (!isBotConnected(bot)) {
      break;
    }
    try {
      sendAction(bot, action);
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
      break;
    }

    const retryUntil = Date.now() + 1_500;
    while ((Date.now() - startedAt) < timeoutMs && Date.now() < retryUntil) {
      const snapshot = bot.snapshotPage();
      if (snapshot && predicate(snapshot)) {
        return snapshot;
      }
      await delay(150);
    }
  }

  throw new Error(
    `Timed out waiting for ${label}; finalSnapshot=${JSON.stringify(summarizePageSnapshot(bot.snapshotPage()))}; finalServerMessage=${bot.getServerMessages().at(-1) ?? null}; lastSendError=${lastError}`
  );
}

function resourceAtLeast(snapshot, selector, minimum) {
  const value = Number.parseInt(`${readSelectorValue(snapshot, selector)}`, 10);
  return !Number.isNaN(value) && value >= minimum;
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "AliasBot";
  const uuid = process.argv[5] ?? "423e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "command-alias-flow");
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
      nearbyRadius: 14
    });
    await delay(2_500);

    const debugSnapshot = await chatUntilSnapshot(
      bot,
      "/kd ui",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.DebugNavigatorPage",
      18_000,
      "debug page from /kd ui"
    );
    pages.push({ key: debugSnapshot.key, title: null, snapshot: debugSnapshot });
    assertions.push("kd-ui-opened-debug");

    const castleSnapshot = await sendActionUntilSnapshot(
      bot,
      "OpenCastleMain",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleMainPage",
      12_000,
      "castle main from debug action"
    );
    if (!castleSnapshot.selectors.includes("#EnterInteriorButton")) {
      throw new Error("Castle main page missing interior button selector.");
    }
    pages.push({ key: castleSnapshot.key, title: null, snapshot: castleSnapshot });
    assertions.push("castle-main-opened-from-debug");

    bot.chat("/kd citizens set 9");
    await delay(250);
    bot.chat("/kd troops set 4");
    await delay(250);
    bot.chat("/kd resources set food 44");
    await delay(250);
    bot.chat("/kd resources set wood 55");
    await delay(250);
    bot.chat("/kd resources set iron 13");
    await delay(700);
    assertions.push("kd-mutations-applied");

    const upgradesSnapshot = await chatUntilSnapshot(
      bot,
      "/kd ui upgrades",
      (snapshot) =>
        snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#CitizenCount.Text") === "9"
        && readSelectorValue(snapshot, "#TroopCount.Text") === "4"
        && resourceAtLeast(snapshot, "#FoodCount.Text", 44)
        && resourceAtLeast(snapshot, "#WoodCount.Text", 55)
        && resourceAtLeast(snapshot, "#IronCount.Text", 13),
      15_000,
      "kd upgrades page values"
    );
    pages.push({ key: upgradesSnapshot.key, title: null, snapshot: upgradesSnapshot });
    assertions.push("kd-upgrades-opened");
    assertions.push("kd-upgrades-values");

    const returnedCastleSnapshot = await sendActionUntilSnapshot(
      bot,
      "OpenCastleMain",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleMainPage",
      12_000,
      "castle main from upgrades action"
    );
    pages.push({ key: returnedCastleSnapshot.key, title: null, snapshot: returnedCastleSnapshot });
    assertions.push("upgrades-back-to-castle-main");

    const directCastleSnapshot = await chatUntilSnapshot(
      bot,
      "/kd castle",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleMainPage",
      12_000,
      "castle main from /kd castle"
    );
    pages.push({ key: directCastleSnapshot.key, title: null, snapshot: directCastleSnapshot });
    assertions.push("kd-castle-command-opened");

    const result = {
      name: "remote-command-alias-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      clientSnapshot: {
        baseline: baseline.snapshot,
        final: captureWorldSnapshot(bot, 14)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "remote-command-alias-flow",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      error: error instanceof Error ? error.message : String(error),
      finalPageSnapshot: summarizePageSnapshot(bot.snapshotPage()),
      clientSnapshot: {
        final: captureWorldSnapshot(bot, 14)
      },
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
