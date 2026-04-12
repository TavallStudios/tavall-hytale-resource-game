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

async function openPageByCommand(bot, command, predicate, timeoutMs, label) {
  const startedAt = Date.now();
  while ((Date.now() - startedAt) < timeoutMs) {
    bot.chat(command);
    try {
      return await waitForSnapshot(bot, predicate, 2_500, label);
    } catch {
      await delay(500);
    }
  }
  throw new Error(`Timed out waiting for ${label}`);
}

function sendAction(bot, action) {
  bot.sendPageEvent("Data", JSON.stringify({ Action: action }));
}

async function main() {
  const clientModuleUrl = pathToFileURL(path.resolve(process.cwd(), "packages/client/dist/index.js")).href;
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "OnboardingBot";
  const uuid = process.argv[5] ?? "523e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "onboarding-flow");
  const resultPath = path.join(outputDir, "scenario-result.json");
  const assertions = [];
  const pages = [];
  const startedAt = new Date().toISOString();

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

    let upgradesSnapshot = await openPageByCommand(
      bot,
      "/kingdom ui upgrades",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#TutorialStatus.Text") === "First join tip: promote citizens here when Food, Wood, and Iron are ready.",
      15_000,
      "first upgrade tutorial"
    );
    pages.push({ key: upgradesSnapshot.key, snapshot: upgradesSnapshot });
    assertions.push("upgrade-tutorial-first-open");

    sendAction(bot, "PromoteCitizen");
    await delay(1_000);
    upgradesSnapshot = await openPageByCommand(
      bot,
      "/kingdom ui upgrades",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#CitizenCount.Text") === "11"
        && readSelectorValue(snapshot, "#TroopCount.Text") === "1"
        && readSelectorValue(snapshot, "#TutorialStatus.Text") === "Tutorial complete: use this page to convert citizens when resources allow.",
      15_000,
      "upgrade tutorial cleared after action"
    );
    pages.push({ key: upgradesSnapshot.key, snapshot: upgradesSnapshot });
    assertions.push("upgrade-tutorial-cleared");

    upgradesSnapshot = await openPageByCommand(
      bot,
      "/kingdom ui upgrades",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#TutorialStatus.Text") === "Tutorial complete: use this page to convert citizens when resources allow.",
      15_000,
      "upgrade tutorial remains cleared"
    );
    assertions.push("upgrade-tutorial-stays-cleared");

    let interiorSnapshot = await openPageByCommand(
      bot,
      "/kingdom ui interior",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.InteriorMainPage"
        && readSelectorValue(snapshot, "#TutorialStatus.Text") === "First interior visit: anchor displays show your citizen and troop totals while later stations grow around them.",
      20_000,
      "first interior tutorial"
    );
    pages.push({ key: interiorSnapshot.key, snapshot: interiorSnapshot });
    assertions.push("interior-tutorial-before-visit");

    const result = {
      name: "remote-onboarding-flow",
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
      name: "remote-onboarding-flow",
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
