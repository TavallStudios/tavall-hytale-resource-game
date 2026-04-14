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

function assertInfraValue(selector, actualValue, allowedValues) {
  if (!allowedValues.includes(actualValue)) {
    throw new Error(`Unexpected ${selector}: ${actualValue}`);
  }
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "HealthBot";
  const uuid = process.argv[5] ?? "623e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "data-health-flow");
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
      nearbyRadius: 12
    });
    await delay(1_500);

    bot.chat("/kd ui");
    const debugSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.DebugNavigatorPage"
        && readSelectorValue(snapshot, "#CacheStatus.Text") != null
        && readSelectorValue(snapshot, "#PersistenceStatus.Text") != null
        && readSelectorValue(snapshot, "#InteriorTutorialStatus.Text") != null
        && readSelectorValue(snapshot, "#InteriorTourStatus.Text") != null
        && readSelectorValue(snapshot, "#UpgradeTutorialStatus.Text") != null,
      15_000,
      "debug status snapshot"
    );
    assertInfraValue("#CacheStatus.Text", readSelectorValue(debugSnapshot, "#CacheStatus.Text"), [
      "memory-only (Redis not configured)",
      "memory+redis (connected)"
    ]);
    assertInfraValue("#PersistenceStatus.Text", readSelectorValue(debugSnapshot, "#PersistenceStatus.Text"), [
      "in-memory fallback (Postgres not configured)",
      "postgres (connected)"
    ]);
    if (readSelectorValue(debugSnapshot, "#InteriorTutorialStatus.Text") !== "pending") {
      throw new Error(`Unexpected interior tutorial status: ${readSelectorValue(debugSnapshot, "#InteriorTutorialStatus.Text")}`);
    }
    if (readSelectorValue(debugSnapshot, "#InteriorTourStatus.Text") !== "pending") {
      throw new Error(`Unexpected interior tour status: ${readSelectorValue(debugSnapshot, "#InteriorTourStatus.Text")}`);
    }
    if (readSelectorValue(debugSnapshot, "#UpgradeTutorialStatus.Text") !== "pending") {
      throw new Error(`Unexpected upgrade tutorial status: ${readSelectorValue(debugSnapshot, "#UpgradeTutorialStatus.Text")}`);
    }
    pages.push({ key: debugSnapshot.key, snapshot: debugSnapshot });
    assertions.push("debug-health-output");

    bot.chat("/kd ui upgrades");
    let upgradesSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#CitizenCount.Text") === "12"
        && readSelectorValue(snapshot, "#TroopCount.Text") === "0"
        && readSelectorValue(snapshot, "#FoodCount.Text") === "40"
        && readSelectorValue(snapshot, "#WoodCount.Text") === "25"
        && readSelectorValue(snapshot, "#IronCount.Text") === "10",
      10_000,
      "baseline upgrades snapshot"
    );
    pages.push({ key: upgradesSnapshot.key, snapshot: upgradesSnapshot });
    assertions.push("baseline-upgrade-state");

    bot.chat("/kd citizens set not-a-number");
    await delay(500);
    bot.chat("/kd ui upgrades");
    upgradesSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#CitizenCount.Text") === "12"
        && readSelectorValue(snapshot, "#TroopCount.Text") === "0",
      10_000,
      "invalid citizen amount leaves state unchanged"
    );
    assertions.push("invalid-citizen-amount-does-not-mutate");

    bot.chat("/kd resources add stone 5");
    await delay(500);
    bot.chat("/kd ui upgrades");
    upgradesSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#FoodCount.Text") === "40"
        && readSelectorValue(snapshot, "#WoodCount.Text") === "25"
        && readSelectorValue(snapshot, "#IronCount.Text") === "10",
      10_000,
      "invalid resource type leaves state unchanged"
    );
    pages.push({ key: upgradesSnapshot.key, snapshot: upgradesSnapshot });
    assertions.push("invalid-resource-does-not-mutate");

    const result = {
      name: "remote-data-health-flow",
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
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    const result = {
      name: "remote-data-health-flow",
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

