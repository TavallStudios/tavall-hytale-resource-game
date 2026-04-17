import path from "node:path";
import { delay, ensureBotBaseline, resolveBotClientModuleUrl, writeJson, printStructured, captureWorldSnapshot } from "./bot-flow-helpers.mjs";

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

function assertSnapshotValues(snapshot, expected) {
  const actual = {
    citizens: readSelectorValue(snapshot, "#CitizenCount.Text"),
    troops: readSelectorValue(snapshot, "#TroopCount.Text"),
    food: readSelectorValue(snapshot, "#FoodCount.Text"),
    wood: readSelectorValue(snapshot, "#WoodCount.Text"),
    iron: readSelectorValue(snapshot, "#IronCount.Text")
  };
  if (`${actual.citizens}` !== `${expected.citizens}`) {
    throw new Error(`Expected citizens=${expected.citizens} but saw ${actual.citizens}`);
  }
  if (`${actual.troops}` !== `${expected.troops}`) {
    throw new Error(`Expected troops=${expected.troops} but saw ${actual.troops}`);
  }
  for (const resourceKey of [ "food", "wood", "iron" ]) {
    const actualValue = Number.parseInt(`${actual[resourceKey]}`, 10);
    const expectedValue = Number.parseInt(`${expected[resourceKey]}`, 10);
    if (Number.isNaN(actualValue) || actualValue < expectedValue) {
      throw new Error(`Expected ${resourceKey}>=${expectedValue} but saw ${actual[resourceKey]}`);
    }
  }
  return actual;
}

async function openUpgradesAndAssert(bot, expected) {
  let lastError = null;
  for (let attempt = 0; attempt < 3; attempt += 1) {
    bot.chat("/kingdom ui upgrades");
    try {
      const page = await bot.waitForPage("com.tavall.hytale.resourcegame.ui.CastleUpgradesPage", 20_000);
      const snapshot = await waitForSnapshot(
        bot,
        (candidate) => {
          try {
            assertSnapshotValues(candidate, expected);
            return true;
          } catch {
            return false;
          }
        },
        20_000,
        "upgrades snapshot values"
      );
      const actual = assertSnapshotValues(snapshot, expected);
      return { page, snapshot, actual };
    } catch (error) {
      lastError = error;
      await delay(1_000);
    }
  }
  throw lastError ?? new Error("Timed out waiting for upgrades snapshot values");
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const mode = process.argv[2] ?? "seed";
  const host = process.argv[3] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[4] ?? "5520", 10);
  const username = process.argv[5] ?? "PersistenceBot";
  const uuid = process.argv[6] ?? "123e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[7] ?? path.resolve(process.cwd(), ".runs", `persistence-${mode}`);

  const expected = {
    citizens: 17,
    troops: 3,
    food: 61,
    wood: 73,
    iron: 29
  };

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

  let baselineSnapshot = null;
  try {
    await bot.trace.enable({ outputDir });
    const baseline = await ensureBotBaseline(bot, assertions, {
      username,
      nearbyRadius: 12
    });
    baselineSnapshot = baseline.snapshot;
    await delay(750);

    if (mode === "seed") {
      const commands = [
        "/kingdom citizens set 17",
        "/kingdom troops set 3",
        "/kingdom resources set food 61",
        "/kingdom resources set wood 73",
        "/kingdom resources set iron 29"
      ];
      for (const command of commands) {
        bot.chat(command);
        await delay(500);
      }
      await delay(1_500);
      assertions.push("state-mutated");
    }

    const upgrades = await openUpgradesAndAssert(bot, expected);
    pages.push({ key: upgrades.page.key, title: upgrades.page.title ?? null, snapshot: upgrades.snapshot });
    assertions.push(mode === "seed" ? "seed-values-verified" : "rehydrated-values-verified");

    if (mode === "verify") {
      bot.chat("/kingdom interior");
      await bot.waitForWorldActivity(20_000);
      assertions.push("interior-entered");
      await delay(1_000);
    }

    const result = {
      name: `remote-persistence-${mode}`,
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      expected,
      clientSnapshot: {
        baseline: baselineSnapshot,
        final: captureWorldSnapshot(bot, 12)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: `remote-persistence-${mode}`,
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      expected,
      clientSnapshot: {
        baseline: baselineSnapshot,
        final: captureWorldSnapshot(bot, 12)
      },
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
