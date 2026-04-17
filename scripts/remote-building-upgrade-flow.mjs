import path from "node:path";
import {
  captureWorldSnapshot,
  createTraceSession,
  delay,
  ensureBotBaseline,
  resolveBotClientModuleUrl,
  waitForPageOrNull,
  waitForServerMessage,
  waitForWorldSnapshot,
  writeJson, printStructured,
} from "./bot-flow-helpers.mjs";

const BUILDING_DETAIL_PAGE = "com.tavall.hytale.resourcegame.ui.BuildingDetailPage";
const BUILDINGS_OVERVIEW_PAGE = "com.tavall.hytale.resourcegame.ui.CastleBuildingsPage";
const INTERIOR_MAIN_PAGE = "com.tavall.hytale.resourcegame.ui.InteriorMainPage";

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

async function openBuildingsOverview(bot, statusSelector, expectedText, timeoutMs = 12_000) {
  const startedAt = Date.now();
  let attempt = 0;
  while ((Date.now() - startedAt) < timeoutMs) {
    if (attempt % 2 === 0) {
      sendAction(bot, "OpenBuildings");
    } else {
      bot.chat("/kd ui buildings");
    }
    attempt += 1;

    const retryUntil = Date.now() + 1_500;
    while (Date.now() < retryUntil) {
      const snapshot = bot.snapshotPage();
      if (snapshot?.key === BUILDINGS_OVERVIEW_PAGE
          && `${readSelectorValue(snapshot, statusSelector)}`.includes(expectedText)) {
        return snapshot;
      }
      await delay(150);
    }
  }
  throw new Error("Timed out waiting for castle buildings overview");
}

async function waitForInteriorReady(bot) {
  const interiorPage = await waitForPageOrNull(bot, INTERIOR_MAIN_PAGE, 12_000);
  if (interiorPage) {
    return { via: "page", page: interiorPage };
  }
  try {
    const worldSnapshot = await waitForWorldSnapshot(
      bot,
      (snapshot) => {
        const position = snapshot?.position;
        return position
          && Math.abs(position.x - 0.5) <= 1.5
          && position.y >= 119
          && position.y <= 123
          && Math.abs(position.z - 0.5) <= 1.5;
      },
      12_000,
      "interior world position"
    );
    return { via: "position", worldSnapshot };
  } catch {
  }
  try {
    const serverMessage = await waitForServerMessage(
      bot,
      (message) => message.toLowerCase().includes("interior ready"),
      12_000,
      "interior ready"
    );
    return { via: "message", serverMessage };
  } catch {
    return { via: "timeout" };
  }
}

async function placeBuildingUntilDetail(bot, buildingType, displayName, levelPredicate, attempts = 4) {
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    bot.chat(`/kingdom buildings stage ${buildingType}`);
    await delay(1_250);

    bot.chat("/kingdom place confirm");
    await delay(1_250);

    bot.chat(`/kingdom buildings select ${buildingType}`);
    const page = await waitForPageOrNull(bot, BUILDING_DETAIL_PAGE, 5_000);
    if (!page) {
      await delay(1_000);
      continue;
    }

    try {
      return await waitForSnapshot(
        bot,
        (snapshot) => snapshot.key === BUILDING_DETAIL_PAGE
          && readSelectorValue(snapshot, "#BuildingTitle.Text") === displayName
          && levelPredicate(`${readSelectorValue(snapshot, "#LevelText.Text")}`),
        6_000,
        `${displayName} detail`
      );
    } catch {
      await delay(1_000);
    }
  }
  throw new Error(`Failed to place or open ${displayName} after ${attempts} attempts`);
}

async function finishAndRefreshBuilding(bot, buildingType) {
  bot.sendPageEvent("Dismiss", null);
  await delay(500);
  bot.chat(`/kd buildings finish ${buildingType}`);
  await delay(1_500);
  bot.chat(`/kd buildings select ${buildingType}`);
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "BuildingBot";
  const uuid = process.argv[5] ?? "a23e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "building-upgrade-flow");
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
  const trace = createTraceSession(bot, outputDir);

  try {
    await trace.enable();
    const baseline = await ensureBotBaseline(bot, assertions, {
      username,
      nearbyRadius: 16
    });
    await delay(1_000);

    const setupCommands = [
      "/kingdom buildings clear",
      "/kingdom resources set food 250",
      "/kingdom resources set wood 250",
      "/kingdom resources set iron 250"
    ];
    for (const command of setupCommands) {
      bot.chat(command);
      await delay(450);
    }
    assertions.push("building-setup-complete");

    let buildingDetailSnapshot = await placeBuildingUntilDetail(
      bot,
      "farmstead",
      "Farmstead",
      (levelText) => levelText.includes("L0 -> L1")
    );
    assertions.push("farmstead-placed");
    if (!`${readSelectorValue(buildingDetailSnapshot, "#StatusText.Text")}`.includes("%")) {
      throw new Error(`Expected under-construction building status, got ${readSelectorValue(buildingDetailSnapshot, "#StatusText.Text")}`);
    }
    pages.push({ key: BUILDING_DETAIL_PAGE, title: null, snapshot: buildingDetailSnapshot });
    assertions.push("building-detail-opened");

    await finishAndRefreshBuilding(bot, "farmstead");
    buildingDetailSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === BUILDING_DETAIL_PAGE
        && readSelectorValue(snapshot, "#LevelText.Text") === "L1"
        && readSelectorValue(snapshot, "#StatusText.Text") === "Operational"
        && readSelectorValue(snapshot, "#EffectText.Text") === "Passive yield: +2 Food, +0 Wood, +0 Iron per tick.",
      10_000,
      "completed level one building detail"
    );
    assertions.push("farmstead-level-one-complete");

    sendAction(bot, "BuildingStartUpgrade");
    buildingDetailSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === BUILDING_DETAIL_PAGE
        && readSelectorValue(snapshot, "#FeedbackStatus.Text") === "Farmstead upgrade started."
        && readSelectorValue(snapshot, "#LevelText.Text") === "L1 -> L2",
      6_000,
      "building upgrade start"
    );
    assertions.push("farmstead-upgrade-started-from-ui");

    await finishAndRefreshBuilding(bot, "farmstead");
    buildingDetailSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === BUILDING_DETAIL_PAGE
        && readSelectorValue(snapshot, "#LevelText.Text") === "L2"
        && readSelectorValue(snapshot, "#StatusText.Text") === "Operational"
        && readSelectorValue(snapshot, "#EffectText.Text") === "Passive yield: +4 Food, +0 Wood, +0 Iron per tick.",
      12_000,
      "completed level two building detail"
    );
    pages.push({ key: BUILDING_DETAIL_PAGE, title: null, snapshot: buildingDetailSnapshot });
    assertions.push("farmstead-level-two-complete");

    const overviewSnapshot = await openBuildingsOverview(bot, "#FarmsteadStatus.Text", "L2");
    if (!`${readSelectorValue(overviewSnapshot, "#FarmsteadStatus.Text")}`.includes("+4F +0W +0I / tick")) {
      throw new Error(`Expected upgraded farmstead effect in overview, got ${readSelectorValue(overviewSnapshot, "#FarmsteadStatus.Text")}`);
    }
    pages.push({ key: BUILDINGS_OVERVIEW_PAGE, title: null, snapshot: overviewSnapshot });
    assertions.push("building-overview-reflects-upgrade");

    bot.chat("/kingdom interior");
    await bot.waitForWorldActivity(20_000);
    const interiorReady = await waitForInteriorReady(bot);
    await delay(2_000);
    pages.push({ key: INTERIOR_MAIN_PAGE, title: interiorReady.via, snapshot: interiorReady.page ?? interiorReady.worldSnapshot ?? interiorReady.serverMessage ?? null });
    assertions.push("entered-interior-for-buildings");
    assertions.push(`interior-ready-${interiorReady.via}`);

    let barracksSnapshot = await placeBuildingUntilDetail(
      bot,
      "barracks",
      "Barracks",
      (levelText) => levelText.includes("L0 -> L1")
    );
    assertions.push("barracks-placed");
    if (!`${readSelectorValue(barracksSnapshot, "#StatusText.Text")}`.includes("%")) {
      throw new Error(`Expected barracks construction status, got ${readSelectorValue(barracksSnapshot, "#StatusText.Text")}`);
    }
    pages.push({ key: BUILDING_DETAIL_PAGE, title: null, snapshot: barracksSnapshot });
    assertions.push("barracks-detail-opened");

    await finishAndRefreshBuilding(bot, "barracks");
    barracksSnapshot = await waitForSnapshot(
      bot,
      (snapshot) => snapshot.key === BUILDING_DETAIL_PAGE
        && readSelectorValue(snapshot, "#LevelText.Text") === "L1"
        && readSelectorValue(snapshot, "#StatusText.Text") === "Operational"
        && readSelectorValue(snapshot, "#EffectText.Text") === "Training effect: promotion discount -1 Food, -0 Wood, -0 Iron.",
      12_000,
      "completed barracks detail"
    );
    pages.push({ key: BUILDING_DETAIL_PAGE, title: null, snapshot: barracksSnapshot });
    assertions.push("barracks-level-one-complete");

    bot.chat("/kingdom interior exit");
    await bot.waitForWorldActivity(12_000);
    await delay(1_000);
    assertions.push("exited-interior-after-buildings");

    const result = {
      name: "remote-building-upgrade-flow",
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
    await trace.flush();
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "remote-building-upgrade-flow",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      error: error instanceof Error ? error.message : String(error),
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    try {
      await trace.flush();
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
