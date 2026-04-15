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

function sendAction(bot, action) {
  bot.sendPageEvent("Data", JSON.stringify({ Action: action }));
}

function parseStockValue(snapshot) {
  const raw = `${readSelectorValue(snapshot, "#StockStatus.Text")}`;
  const match = raw.match(/^(\d+)\s*\/\s*(\d+)/);
  if (!match) {
    return null;
  }
  return {
    current: Number.parseInt(match[1], 10),
    max: Number.parseInt(match[2], 10)
  };
}

async function openNodePage(bot, expected) {
  bot.chat("/kingdom nodes select 1");
  return waitForSnapshot(
    bot,
    (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.ResourceNodePage"
      && Object.entries(expected).every(([selector, value]) => `${readSelectorValue(snapshot, selector)}` === `${value}`),
    10_000,
    "resource node page"
  );
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "NodeBot";
  const uuid = process.argv[5] ?? "523e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "node-assignment-flow");
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
      nearbyRadius: 16
    });
    await delay(1_000);

    const setupCommands = [
      "/kingdom nodes clear",
      "/kingdom troops set 9",
      "/kingdom nodes place food"
    ];
    for (const command of setupCommands) {
      bot.chat(command);
      await delay(500);
    }
    assertions.push("node-placed");

    let snapshot = await openNodePage(bot, {
      "#AssignedTroops.Text": "0",
      "#AvailableTroops.Text": "9",
      "#GainPerTick.Text": "+0/tick",
      "#StockStatus.Text": "180 / 180 (100%)",
      "#RegenStatus.Text": "+10 / tick",
      "#RouteStatus.Text": "No supply lane"
    });
    pages.push({ key: snapshot.key, title: null, snapshot });
    assertions.push("node-ui-opened");

    sendAction(bot, "NodeAssignThree");
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#AssignedTroops.Text") === "3"
        && readSelectorValue(candidate, "#AvailableTroops.Text") === "6"
        && readSelectorValue(candidate, "#GainPerTick.Text") === "+12/tick"
        && readSelectorValue(candidate, "#RouteStatus.Text") === "Supply lane active: 1 convoy markers",
      5_000,
      "assign three update"
    );
    assertions.push("node-assign-three");

    sendAction(bot, "NodeAssignAll");
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#AssignedTroops.Text") === "9"
        && readSelectorValue(candidate, "#AvailableTroops.Text") === "0"
        && readSelectorValue(candidate, "#GainPerTick.Text") === "+36/tick"
        && readSelectorValue(candidate, "#RouteStatus.Text") === "Supply lane active: 3 convoy markers",
      5_000,
      "assign all update"
    );
    assertions.push("node-assign-all");

    await delay(13_000);
    bot.chat("/kingdom nodes select 1");
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => {
        if (candidate.key !== "com.tavall.hytale.resourcegame.ui.ResourceNodePage") {
          return false;
        }
        const stock = parseStockValue(candidate);
        return readSelectorValue(candidate, "#AssignedTroops.Text") === "9"
          && stock != null
          && stock.current < 180
          && stock.max === 180;
      },
      10_000,
      "node stock drain update"
    );
    assertions.push("node-stock-drained");

    sendAction(bot, "NodeRecallOne");
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#AssignedTroops.Text") === "8"
        && readSelectorValue(candidate, "#AvailableTroops.Text") === "1"
        && readSelectorValue(candidate, "#GainPerTick.Text") === "+32/tick"
        && readSelectorValue(candidate, "#RouteStatus.Text") === "Supply lane active: 3 convoy markers",
      5_000,
      "recall one update"
    );
    assertions.push("node-recall-one");

    sendAction(bot, "NodeRecallAll");
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#AssignedTroops.Text") === "0"
        && readSelectorValue(candidate, "#AvailableTroops.Text") === "9"
        && readSelectorValue(candidate, "#GainPerTick.Text") === "+0/tick"
        && readSelectorValue(candidate, "#RouteStatus.Text") === "No supply lane",
      5_000,
      "recall all update"
    );
    pages.push({ key: snapshot.key, title: null, snapshot });
    assertions.push("node-recall-all");

    const result = {
      name: "remote-node-assignment-flow",
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
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    const result = {
      name: "remote-node-assignment-flow",
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
