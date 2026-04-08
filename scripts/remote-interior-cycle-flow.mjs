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

function sendAction(bot, action) {
  bot.sendPageEvent("Data", JSON.stringify({ Action: action }));
}

async function waitForSnapshot(bot, key, selector, timeoutMs, label) {
  const startedAt = Date.now();
  while ((Date.now() - startedAt) < timeoutMs) {
    const snapshot = bot.snapshotPage();
    if (snapshot?.key === key && (!selector || snapshot.selectors.includes(selector))) {
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
  const username = process.argv[4] ?? "InteriorCycleBot";
  const uuid = process.argv[5] ?? "423e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "interior-cycle-flow");
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
    await delay(3_000);

    for (let cycle = 1; cycle <= 3; cycle += 1) {
      await delay(1_500);

      bot.chat("/kingdom interior");
      await bot.waitForWorldActivity(20_000);
      await bot.waitForPage("com.tavall.hytale.resourcegame.ui.InteriorMainPage", 20_000);
      const interiorSnapshot = await waitForSnapshot(
        bot,
        "com.tavall.hytale.resourcegame.ui.InteriorMainPage",
        "#ExitInteriorButton",
        8_000,
        `interior page on cycle ${cycle}`
      );
      pages.push({ key: `cycle-${cycle}-interior`, title: null, snapshot: interiorSnapshot });
      assertions.push(`entered-interior-cycle-${cycle}`);

      sendAction(bot, "ExitInterior");
      await bot.waitForWorldActivity(20_000);
      await bot.waitForPage("com.tavall.hytale.resourcegame.ui.CastleMainPage", 20_000);
      const returnSnapshot = await waitForSnapshot(
        bot,
        "com.tavall.hytale.resourcegame.ui.CastleMainPage",
        "#EnterInteriorButton",
        8_000,
        `castle return page on cycle ${cycle}`
      );
      pages.push({ key: `cycle-${cycle}-return`, title: null, snapshot: returnSnapshot });
      assertions.push(`exited-interior-cycle-${cycle}`);
      await delay(1_500);
    }

    const result = {
      name: "remote-interior-cycle-flow",
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
      name: "remote-interior-cycle-flow",
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
