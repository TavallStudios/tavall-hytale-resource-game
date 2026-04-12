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

async function main() {
  const clientModuleUrl = pathToFileURL(path.resolve(process.cwd(), "packages/client/dist/index.js")).href;
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
    await bot.waitForReady(15_000);
    assertions.push("connected");
    await bot.waitForWorldActivity(10_000);
    assertions.push("world-joined");
    await delay(2_000);

    bot.chat("/kd ui");
    const debugSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.DebugNavigatorPage"
        && readSelectorValue(snapshot, "#CacheStatus.Text") === "memory-only (Redis not configured)"
        && readSelectorValue(snapshot, "#PersistenceStatus.Text") === "in-memory fallback (Postgres not configured)"
        && readSelectorValue(snapshot, "#InteriorTutorialStatus.Text") === "pending"
        && readSelectorValue(snapshot, "#UpgradeTutorialStatus.Text") === "pending",
      10_000,
      "debug status snapshot"
    );
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
