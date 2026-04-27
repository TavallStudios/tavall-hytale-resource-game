import path from "node:path";
import {
  captureWorldSnapshot,
  delay,
  ensureBotBaseline,
  writeJson, printStructured,
  resolveBotClientModuleUrl
} from "./bot-flow-helpers.mjs";

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

function parseLeadingNumber(rawText) {
  const match = `${rawText}`.match(/^\+?(\d+)/);
  return match ? Number.parseInt(match[1], 10) : null;
}

async function openNodePage(bot) {
  bot.chat("/kingdom nodes select 1");
  await bot.waitForPage("com.tavall.hytale.resourcegame.ui.ResourceNodePage", 10_000);
  return waitForSnapshot(
    bot,
    (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.ResourceNodePage",
    5_000,
    "resource node page snapshot"
  );
}

async function refreshNodePage(bot) {
  bot.chat("/kingdom nodes select 1");
  await delay(400);
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "NodeBot";
  const uuid = process.argv[5] ?? "523e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "node-assignment-flow");
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
      nearbyRadius: 16
    });
    await delay(1_000);

    const setupCommands = [
      "/kingdom nodes clear",
      "/kingdom troops set 9",
      "/kingdom place node food"
    ];
    for (const command of setupCommands) {
      bot.chat(command);
      await delay(500);
    }
    assertions.push("node-placement-armed");

    bot.chat("/kingdom place confirm here");
    await delay(1_250);
    assertions.push("node-placed");

    let snapshot = await openNodePage(bot);
    if (!`${readSelectorValue(snapshot, "#NodeTitle.Text")}`.toLowerCase().includes("food")) {
      throw new Error(`Unexpected node title after placement: ${JSON.stringify(snapshot)}`);
    }
    pages.push({ key: snapshot.key, title: null, snapshot });
    assertions.push("node-ui-opened");

    bot.chat("/kingdom nodes add 1 3");
    await refreshNodePage(bot);
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#AssignedTroops.Text") === "3"
        && readSelectorValue(candidate, "#AvailableTroops.Text") === "6"
        && Number.parseInt(`${readSelectorValue(candidate, "#AssignedWorkers.Text")}`, 10) > 0
        && parseLeadingNumber(readSelectorValue(candidate, "#GainPerTick.Text")) >= 12
        && readSelectorValue(candidate, "#StatusText.Text") === "Rich"
        && `${readSelectorValue(candidate, "#RouteStatus.Text")}`.startsWith("Supply lane active:"),
      5_000,
      "assign three update"
    );
    assertions.push("node-assign-three");

    bot.chat("/kingdom nodes assign 1 9");
    await refreshNodePage(bot);
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#AssignedTroops.Text") === "9"
        && readSelectorValue(candidate, "#AvailableTroops.Text") === "0"
        && Number.parseInt(`${readSelectorValue(candidate, "#AssignedWorkers.Text")}`, 10) > 0
        && parseLeadingNumber(readSelectorValue(candidate, "#GainPerTick.Text")) >= 36
        && readSelectorValue(candidate, "#StatusText.Text") === "Rich"
        && `${readSelectorValue(candidate, "#RouteStatus.Text")}`.startsWith("Supply lane active:"),
      5_000,
      "assign all update"
    );
    assertions.push("node-assign-all");

    bot.chat("/kingdom tick run 1");
    await refreshNodePage(bot);
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
          && stock.max === 180
          && readSelectorValue(candidate, "#StatusText.Text") === "Rich";
      },
      10_000,
      "node stock drain update"
    );
    assertions.push("node-stock-drained");

    bot.chat("/kingdom nodes stock 1 8");
    await refreshNodePage(bot);
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#StockStatus.Text") === "8 / 180 (4%)"
        && readSelectorValue(candidate, "#StatusText.Text") === "Low",
      10_000,
      "node low stock update"
    );
    assertions.push("node-low-stock");

    bot.chat("/kingdom nodes recall 1 1");
    await refreshNodePage(bot);
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#AssignedTroops.Text") === "8"
        && readSelectorValue(candidate, "#AvailableTroops.Text") === "1"
        && `${readSelectorValue(candidate, "#GainPerTick.Text")}`.startsWith("+8/tick")
        && readSelectorValue(candidate, "#StatusText.Text") === "Low"
        && `${readSelectorValue(candidate, "#RouteStatus.Text")}`.startsWith("Supply lane active:"),
      5_000,
      "recall one update"
    );
    assertions.push("node-recall-one");

    bot.chat("/kingdom nodes recall 1 all");
    await refreshNodePage(bot);
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => readSelectorValue(candidate, "#AssignedTroops.Text") === "0"
        && readSelectorValue(candidate, "#AvailableTroops.Text") === "9"
        && Number.parseInt(`${readSelectorValue(candidate, "#AssignedWorkers.Text")}`, 10) > 0
        && parseLeadingNumber(readSelectorValue(candidate, "#GainPerTick.Text")) > 0
        && readSelectorValue(candidate, "#StatusText.Text") === "Low"
        && `${readSelectorValue(candidate, "#RouteStatus.Text")}`.startsWith("Supply lane active:"),
      5_000,
      "recall all update"
    );
    pages.push({ key: snapshot.key, title: null, snapshot });
    assertions.push("node-recall-all");

    bot.chat("/kingdom nodes pillage 1");
    await refreshNodePage(bot);
    snapshot = await waitForSnapshot(
      bot,
      (candidate) => {
        if (readSelectorValue(candidate, "#NodeTitle.Text") === "Node not found") {
          return true;
        }
        const stock = parseStockValue(candidate);
        return stock != null
          && stock.current < 8
          && parseLeadingNumber(readSelectorValue(candidate, "#PillageReward.Text")) >= 0;
      },
      8_000,
      "manual pillage drain"
    );
    assertions.push("node-manual-pillage");

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
    printStructured(result);
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
    printStructured(result, true);
    process.exitCode = 1;
  } finally {
    await bot.disconnect();
  }
}

await main();
