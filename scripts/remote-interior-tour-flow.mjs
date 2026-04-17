import path from "node:path";
import {
  captureWorldSnapshot,
  delay,
  ensureBotBaseline,
  findNearbyEntityByPosition,
  resolveBotClientModuleUrl,
  waitForWorldSnapshot,
  writeJson, printStructured,
} from "./bot-flow-helpers.mjs";

const INTERIOR_PAGE_KEY = "com.tavall.hytale.resourcegame.ui.InteriorMainPage";
const FIRST_TUTORIAL_TEXT = "Step 1: follow the tour markers. Step 2: inspect the citizen and troop anchors. Step 3: leave through the exit lane when you are done.";
const COMPLETE_TUTORIAL_TEXT = "Interior tutorial complete: citizen and troop anchors stay here while the upgrade pipeline grows.";
const CITIZEN_ANCHOR = { x: 3.5, y: 121.0, z: 2.5 };
const TROOP_ANCHOR = { x: -2.5, y: 121.0, z: 2.5 };
const TOUR_MARKERS = [
  { x: 0.5, y: 121.0, z: -1.0 },
  { x: 2.0, y: 121.0, z: 1.5 },
  { x: -1.0, y: 121.0, z: 1.5 },
  { x: 0.5, y: 121.0, z: -2.5 }
];

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

function hasAllTourMarkers(snapshot) {
  return TOUR_MARKERS.every((position) => findNearbyEntityByPosition(snapshot, position));
}

function hasNoTourMarkers(snapshot) {
  return TOUR_MARKERS.every((position) => !findNearbyEntityByPosition(snapshot, position));
}

function hasPopulationAnchors(snapshot) {
  return Boolean(findNearbyEntityByPosition(snapshot, CITIZEN_ANCHOR) && findNearbyEntityByPosition(snapshot, TROOP_ANCHOR));
}

async function enterInterior(bot, expectedTutorialText) {
  const pageSnapshot = await openPageByCommand(
    bot,
    "/kingdom ui interior",
    (snapshot) => snapshot?.key === INTERIOR_PAGE_KEY
      && readSelectorValue(snapshot, "#TutorialStatus.Text") === expectedTutorialText,
    15_000,
    "interior tutorial page"
  );
  bot.chat("/kingdom interior");
  await bot.waitForWorldActivity(10_000);
  return pageSnapshot;
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "InteriorTourBot";
  const uuid = process.argv[5] ?? "723e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "interior-tour-flow");
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
      nearbyRadius: 12
    });
    await delay(1_500);

    const firstInteriorPage = await enterInterior(bot, FIRST_TUTORIAL_TEXT);
    pages.push({ key: firstInteriorPage.key, snapshot: firstInteriorPage });
    assertions.push("first-interior-page-opened");
    assertions.push("first-interior-entered");

    let firstInteriorWorldSnapshot = captureWorldSnapshot(bot, 20);
    if (firstInteriorWorldSnapshot.nearbyEntities.length > 0) {
      firstInteriorWorldSnapshot = await waitForWorldSnapshot(
        bot,
        (snapshot) => hasPopulationAnchors(snapshot) && hasAllTourMarkers(snapshot),
        10_000,
        "first interior tour markers",
        20
      );
      assertions.push("first-tour-markers-visible");
      assertions.push("population-anchors-visible");
    } else {
      assertions.push("interior-entity-scan-unavailable");
    }

    bot.chat("/kingdom interior exit");
    await bot.waitForWorldActivity(10_000);
    assertions.push("interior-exited");
    await delay(1_000);

    const result = {
      name: "remote-interior-tour-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      clientSnapshot: {
        baseline: baseline.snapshot,
        firstInterior: firstInteriorWorldSnapshot,
        tutorialCompletionExpected: COMPLETE_TUTORIAL_TEXT
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "remote-interior-tour-flow",
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
