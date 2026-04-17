import path from "node:path";
import { captureWorldSnapshot, createTraceSession, delay, ensureBotBaseline, resolveBotClientModuleUrl, writeJson, printStructured } from "./bot-flow-helpers.mjs";

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

function sendAction(bot, action) {
  bot.sendPageEvent("Data", JSON.stringify({ Action: action }));
}

async function sendActionUntilSnapshot(bot, action, predicate, timeoutMs, label) {
  const startedAt = Date.now();
  while ((Date.now() - startedAt) < timeoutMs) {
    sendAction(bot, action);
    const retryUntil = Date.now() + 1_250;
    while (Date.now() < retryUntil) {
      const snapshot = bot.snapshotPage();
      if (snapshot && predicate(snapshot)) {
        return snapshot;
      }
      await delay(150);
    }
  }
  throw new Error(`Timed out waiting for ${label}`);
}

async function dismissPage(bot) {
  bot.sendPageEvent("Dismiss", null);
  await delay(350);
}

function matchesExpectedValues(snapshot, expected) {
  if (!expected) {
    return true;
  }
  for (const [selector, value] of Object.entries(expected)) {
    if (`${readSelectorValue(snapshot, selector)}` !== `${value}`) {
      return false;
    }
  }
  return true;
}

async function openUpgrades(bot, expected = null) {
  await dismissPage(bot);
  bot.chat("/kd ui upgrades");
  return waitForSnapshot(
    bot,
    (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage" && matchesExpectedValues(snapshot, expected),
    10_000,
    "upgrades page snapshot"
  );
}

async function openCastleInfo(bot) {
  try {
    sendAction(bot, "OpenCastleInfo");
    return await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleInfoPage" && snapshot.selectors.includes("#CastleId.Text"),
      10_000,
      "castle info page"
    );
  } catch (error) {
    bot.chat("/kingdom ui info");
    return await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleInfoPage" && snapshot.selectors.includes("#CastleId.Text"),
      15_000,
      "castle info page (fallback)"
    );
  }
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "UiEdgeBot";
  const uuid = process.argv[5] ?? "323e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "ui-edge-flow");
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
    await delay(5_000);

    bot.chat("/kd ui");
    const debugSnapshot = await waitForSnapshot(bot, (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.DebugNavigatorPage", 15_000, "debug page");
    pages.push({ key: debugSnapshot.key, title: null, snapshot: debugSnapshot });
    assertions.push("debug-ui-opened");

    const infoSnapshot = await openCastleInfo(bot);
    pages.push({ key: infoSnapshot.key, title: null, snapshot: infoSnapshot });
    assertions.push("castle-info-opened-from-ui");

    sendAction(bot, "OpenCastleMain");
    const castleMainSnapshot = await waitForSnapshot(bot, (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleMainPage" && snapshot.selectors.includes("#EnterInteriorButton"), 10_000, "castle main page");
    pages.push({ key: castleMainSnapshot.key, title: null, snapshot: castleMainSnapshot });
    assertions.push("returned-to-castle-main");

    sendAction(bot, "OpenResources");
    const resourcesSnapshot = await waitForSnapshot(bot, (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleResourcesPage" && snapshot.selectors.includes("#FoodCount.Text"), 10_000, "resources page");
    pages.push({ key: resourcesSnapshot.key, title: null, snapshot: resourcesSnapshot });
    assertions.push("resources-opened-from-ui");

    await dismissPage(bot);
    bot.chat("/kd citizens set 0");
    await delay(250);
    bot.chat("/kd troops set 0");
    await delay(250);
    bot.chat("/kd resources set food 0");
    await delay(250);
    bot.chat("/kd resources set wood 0");
    await delay(250);
    bot.chat("/kd resources set iron 0");
    await delay(700);

    let upgradesSnapshot = await openUpgrades(bot, {
      "#CitizenCount.Text": "0",
      "#TroopCount.Text": "0",
      "#FoodCount.Text": "0",
      "#WoodCount.Text": "0",
      "#IronCount.Text": "0"
    });
    if (readSelectorValue(upgradesSnapshot, "#PromoteStatus.Text") !== "Blocked: need at least 1 citizen.") {
      throw new Error(`Unexpected promote blocked status: ${readSelectorValue(upgradesSnapshot, "#PromoteStatus.Text")}`);
    }
    if (readSelectorValue(upgradesSnapshot, "#DemoteStatus.Text") !== "Blocked: need at least 1 troop.") {
      throw new Error(`Unexpected demote blocked status: ${readSelectorValue(upgradesSnapshot, "#DemoteStatus.Text")}`);
    }
    assertions.push("upgrade-blocked-status-visible");

    upgradesSnapshot = await sendActionUntilSnapshot(
      bot,
      "PromoteCitizen",
      (snapshot) => readSelectorValue(snapshot, "#FeedbackStatus.Text") === "Blocked: need at least 1 citizen.",
      5_000,
      "blocked promote feedback"
    );
    assertions.push("blocked-promote-feedback");

    await dismissPage(bot);
    bot.chat("/kd citizens set 2");
    await delay(250);
    bot.chat("/kd resources set food 4");
    await delay(250);
    bot.chat("/kd resources set wood 0");
    await delay(250);
    bot.chat("/kd resources set iron 1");
    await delay(700);

    upgradesSnapshot = await openUpgrades(bot, {
      "#CitizenCount.Text": "2",
      "#TroopCount.Text": "0",
      "#FoodCount.Text": "4",
      "#WoodCount.Text": "0",
      "#IronCount.Text": "1"
    });
    if (readSelectorValue(upgradesSnapshot, "#PromoteStatus.Text") !== "Blocked: need 2 Wood.") {
      throw new Error(`Unexpected wood blocked status: ${readSelectorValue(upgradesSnapshot, "#PromoteStatus.Text")}`);
    }
    assertions.push("resource-blocked-status-visible");

    await dismissPage(bot);
    bot.chat("/kd resources set wood 2");
    await delay(700);
    upgradesSnapshot = await openUpgrades(bot, {
      "#CitizenCount.Text": "2",
      "#TroopCount.Text": "0",
      "#WoodCount.Text": "2",
      "#IronCount.Text": "1"
    });
    if (readSelectorValue(upgradesSnapshot, "#PromoteStatus.Text") !== "Ready: promote 1 citizen into 1 troop.") {
      throw new Error(`Unexpected ready status: ${readSelectorValue(upgradesSnapshot, "#PromoteStatus.Text")}`);
    }
    if (readSelectorValue(upgradesSnapshot, "#PromotionCost.Text") !== "Cost per promotion: 4 Food, 2 Wood, 1 Iron.") {
      throw new Error(`Unexpected cost summary: ${readSelectorValue(upgradesSnapshot, "#PromotionCost.Text")}`);
    }
    assertions.push("promotion-ready-status-visible");

    upgradesSnapshot = await sendActionUntilSnapshot(
      bot,
      "PromoteCitizen",
      (snapshot) =>
        readSelectorValue(snapshot, "#FeedbackStatus.Text") === "Promotion complete."
        && readSelectorValue(snapshot, "#CitizenCount.Text") === "1"
        && readSelectorValue(snapshot, "#TroopCount.Text") === "1",
      5_000,
      "promotion completion"
    );
    assertions.push("promote-button-updates-state");

    upgradesSnapshot = await sendActionUntilSnapshot(
      bot,
      "DemoteTroop",
      (snapshot) =>
        readSelectorValue(snapshot, "#FeedbackStatus.Text") === "Demotion complete."
        && readSelectorValue(snapshot, "#CitizenCount.Text") === "2"
        && readSelectorValue(snapshot, "#TroopCount.Text") === "0",
      5_000,
      "demotion completion"
    );
    pages.push({ key: "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage", title: null, snapshot: upgradesSnapshot });
    assertions.push("demote-button-updates-state");

    const result = {
      name: "remote-ui-edge-flow",
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
      name: "remote-ui-edge-flow",
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
