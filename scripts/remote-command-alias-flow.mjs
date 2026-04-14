import path from "node:path";
import { delay, ensureBotBaseline, resolveBotClientModuleUrl, writeJson, captureWorldSnapshot } from "./bot-flow-helpers.mjs";

function readSelectorValue(snapshot, selector) {
  const command = snapshot?.commands?.find((entry) => entry.type === "Set" && entry.selector === selector);
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

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "AliasBot";
  const uuid = process.argv[5] ?? "423e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "command-alias-flow");
  const resultPath = path.join(outputDir, "scenario-result.json");
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
    await delay(1_500);

    bot.chat("/kd ui");
    const debugSnapshot = await waitForKeyedPage(bot, "com.tavall.hytale.resourcegame.ui.DebugNavigatorPage", 10_000);
    pages.push({ key: debugSnapshot.key, title: null, snapshot: debugSnapshot });
    assertions.push("kd-ui-opened-debug");

    sendAction(bot, "OpenCastleMain");
    const castleSnapshot = await waitForKeyedPage(bot, "com.tavall.hytale.resourcegame.ui.CastleMainPage", 10_000);
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

    bot.chat("/kd ui upgrades");
    const upgradesSnapshot = await waitForSnapshot(
      bot,
      (snapshot) =>
        snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#CitizenCount.Text") === "9"
        && readSelectorValue(snapshot, "#TroopCount.Text") === "4"
        && readSelectorValue(snapshot, "#FoodCount.Text") === "44"
        && readSelectorValue(snapshot, "#WoodCount.Text") === "55"
        && readSelectorValue(snapshot, "#IronCount.Text") === "13",
      10_000,
      "kd upgrades page values"
    );
    pages.push({ key: upgradesSnapshot.key, title: null, snapshot: upgradesSnapshot });
    assertions.push("kd-upgrades-opened");
    assertions.push("kd-upgrades-values");

    sendAction(bot, "OpenCastleMain");
    const returnedCastleSnapshot = await waitForKeyedPage(bot, "com.tavall.hytale.resourcegame.ui.CastleMainPage", 10_000);
    pages.push({ key: returnedCastleSnapshot.key, title: null, snapshot: returnedCastleSnapshot });
    assertions.push("upgrades-back-to-castle-main");

    bot.chat("/kd castle");
    const directCastleSnapshot = await waitForKeyedPage(bot, "com.tavall.hytale.resourcegame.ui.CastleMainPage", 10_000);
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
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    const result = {
      name: "remote-command-alias-flow",
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
    console.error(JSON.stringify(result, null, 2));
    process.exitCode = 1;
  } finally {
    await bot.disconnect();
  }
}

await main();

