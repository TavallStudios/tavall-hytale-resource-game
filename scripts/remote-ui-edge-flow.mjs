import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function writeJson(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

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
  bot.chat("/kingdom ui upgrades");
  return waitForSnapshot(
    bot,
    (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage" && matchesExpectedValues(snapshot, expected),
    10_000,
    "upgrades page snapshot"
  );
}

async function main() {
  const clientModuleUrl = pathToFileURL(path.resolve(process.cwd(), "packages/client/dist/index.js")).href;
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "UiEdgeBot";
  const uuid = process.argv[5] ?? "323e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "ui-edge-flow");
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
    await bot.waitForReady(15_000);
    assertions.push("connected");
    await bot.waitForWorldActivity(10_000);
    assertions.push("world-joined");
    await delay(2_000);

    bot.chat("/kingdom ui");
    const debugSnapshot = await waitForSnapshot(bot, (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.DebugNavigatorPage", 10_000, "debug page");
    pages.push({ key: debugSnapshot.key, title: null, snapshot: debugSnapshot });
    assertions.push("debug-ui-opened");

    sendAction(bot, "OpenCastleInfo");
    const infoSnapshot = await waitForSnapshot(bot, (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleInfoPage" && snapshot.selectors.includes("#CastleId.Text"), 10_000, "castle info page");
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

    bot.chat("/kingdom citizens set 0");
    await delay(250);
    bot.chat("/kingdom troops set 0");
    await delay(250);
    bot.chat("/kingdom resources set food 0");
    await delay(250);
    bot.chat("/kingdom resources set wood 0");
    await delay(250);
    bot.chat("/kingdom resources set iron 0");
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

    sendAction(bot, "PromoteCitizen");
    upgradesSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => readSelectorValue(snapshot, "#FeedbackStatus.Text") === "Blocked: need at least 1 citizen.",
      5_000,
      "blocked promote feedback"
    );
    assertions.push("blocked-promote-feedback");

    bot.chat("/kingdom citizens set 2");
    await delay(250);
    bot.chat("/kingdom resources set food 4");
    await delay(250);
    bot.chat("/kingdom resources set wood 0");
    await delay(250);
    bot.chat("/kingdom resources set iron 1");
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

    bot.chat("/kingdom resources set wood 2");
    await delay(700);
    upgradesSnapshot = await openUpgrades(bot, {
      "#CitizenCount.Text": "2",
      "#TroopCount.Text": "0",
      "#FoodCount.Text": "4",
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

    sendAction(bot, "PromoteCitizen");
    upgradesSnapshot = await waitForSnapshot(
      bot,
      (snapshot) =>
        readSelectorValue(snapshot, "#FeedbackStatus.Text") === "Promotion complete."
        && readSelectorValue(snapshot, "#CitizenCount.Text") === "1"
        && readSelectorValue(snapshot, "#TroopCount.Text") === "1",
      5_000,
      "promotion completion"
    );
    assertions.push("promote-button-updates-state");

    sendAction(bot, "DemoteTroop");
    upgradesSnapshot = await waitForSnapshot(
      bot,
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
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    console.log(JSON.stringify(result, null, 2));
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
