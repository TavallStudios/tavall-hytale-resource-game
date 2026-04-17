import path from "node:path";
import { delay, ensureBotBaseline, resolveBotClientModuleUrl, writeJson, printStructured, captureWorldSnapshot } from "./bot-flow-helpers.mjs";

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
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "OnboardingBot";
  const uuid = process.argv[5] ?? "523e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "onboarding-flow");
  const resultPath = path.join(outputDir, "scenario-result.txt");
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
    const baseline = await ensureBotBaseline(bot, assertions, {
      username,
      nearbyRadius: 12
    });
    await delay(1_500);

    let upgradesSnapshot = await openPageByCommand(
      bot,
      "/kingdom ui upgrades",
      (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage"
        && readSelectorValue(snapshot, "#TutorialStatus.Text") === "Step 1: confirm citizens and troops. Step 2: check the Food, Wood, and Iron cost. Step 3: promote once the route is ready.",
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
        && readSelectorValue(snapshot, "#TutorialStatus.Text") === "Step 1: follow the tour markers. Step 2: inspect the citizen and troop anchors. Step 3: leave through the exit lane when you are done.",
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
      clientSnapshot: {
        baseline: baseline.snapshot,
        final: captureWorldSnapshot(bot, 12)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    printStructured(result);
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
    printStructured(result, true);
    process.exitCode = 1;
  } finally {
    await bot.disconnect();
  }
}

await main();
