import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
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

function assertSnapshotValues(snapshot, expected) {
  const actual = {
    citizens: readSelectorValue(snapshot, "#CitizenCount.Text"),
    troops: readSelectorValue(snapshot, "#TroopCount.Text"),
    food: readSelectorValue(snapshot, "#FoodCount.Text"),
    wood: readSelectorValue(snapshot, "#WoodCount.Text"),
    iron: readSelectorValue(snapshot, "#IronCount.Text")
  };
  for (const [key, value] of Object.entries(expected)) {
    if (`${actual[key]}` !== `${value}`) {
      throw new Error(`Expected ${key}=${value} but saw ${actual[key]}`);
    }
  }
  return actual;
}

async function openUpgradesAndAssert(bot, expected) {
  bot.chat("/kingdom ui upgrades");
  const page = await bot.waitForPage("com.tavall.hytale.resourcegame.ui.CastleUpgradesPage", 10_000);
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
    8_000,
    "upgrades snapshot values"
  );
  const actual = assertSnapshotValues(snapshot, expected);
  return { page, snapshot, actual };
}

async function main() {
  const clientModuleUrl = pathToFileURL(path.resolve(process.cwd(), "packages/client/dist/index.js")).href;
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
        await delay(350);
      }
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
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    const result = {
      name: `remote-persistence-${mode}`,
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      expected,
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
