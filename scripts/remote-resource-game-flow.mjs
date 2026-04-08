import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForPageOrNull(bot, pageKey, timeoutMs) {
  try {
    return await bot.waitForPage(pageKey, timeoutMs);
  } catch {
    return null;
  }
}

async function waitForPage(bot, timeoutMs) {
  if (bot.ui.currentPage) {
    return bot.ui.currentPage;
  }
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      bot.off("page", onPage);
      reject(new Error(`Timed out waiting for page after ${timeoutMs}ms`));
    }, timeoutMs);

    const onPage = (page) => {
      clearTimeout(timer);
      bot.off("page", onPage);
      resolve(page);
    };

    bot.on("page", onPage);
  });
}

const PAGE_KEYS = {
  upgrades: "com.tavall.hytale.resourcegame.ui.CastleUpgradesPage",
  debug: "com.tavall.hytale.resourcegame.ui.DebugNavigatorPage",
  interior: "com.tavall.hytale.resourcegame.ui.InteriorMainPage"
};

async function writeJson(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

async function main() {
  const clientModuleUrl = pathToFileURL(path.resolve(process.cwd(), "packages/client/dist/index.js")).href;
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "ResourceGameBot";
  const outputDir = process.argv[5] ?? path.resolve(process.cwd(), ".runs", "resource-game-flow");
  const resultPath = path.join(outputDir, "scenario-result.json");

  const startedAt = new Date().toISOString();
  const assertions = [];
  const pages = [];
  const commandMessages = [];

  const bot = await createBot({
    host,
    port,
    username,
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

    bot.chat("/kingdom resources set wood 80");
    await delay(300);
    bot.chat("/kingdom troops set 1");
    await delay(300);
    bot.chat("/kingdom citizens set 12");
    await delay(500);
    assertions.push("command-mutations");

    bot.chat("/kingdom ui upgrades");
    const upgradesPage = await bot.waitForPage(PAGE_KEYS.upgrades, 5_000);
    const upgradesSnapshot = bot.snapshotPage();
    const upgradesSnapshotJson = JSON.stringify(upgradesSnapshot);
    if (!upgradesSnapshotJson.includes("12") || !upgradesSnapshotJson.includes("80") || !upgradesSnapshotJson.includes("1")) {
      throw new Error(`Upgrade page did not contain expected values: ${upgradesSnapshotJson}`);
    }
    pages.push({ key: upgradesPage.key, title: upgradesPage.title ?? null, snapshot: upgradesSnapshot });
    assertions.push("upgrades-ui-opened");
    assertions.push("upgrades-ui-values");

    bot.chat("/kingdom ui debug");
    const debugPage = await bot.waitForPage(PAGE_KEYS.debug, 5_000);
    pages.push({ key: debugPage.key, title: debugPage.title ?? null, snapshot: bot.snapshotPage() });
    assertions.push("debug-ui-opened");

    bot.chat("/kingdom interior");
    await bot.waitForWorldActivity(20_000);
    assertions.push("entered-interior");
    const interiorPage = await waitForPageOrNull(bot, PAGE_KEYS.interior, 20_000);
    if (interiorPage) {
      pages.push({ key: interiorPage.key, title: interiorPage.title ?? null, snapshot: bot.snapshotPage() });
      assertions.push("interior-ui-opened");
    }

    bot.chat("/kingdom interior exit");
    await bot.waitForWorldActivity(10_000);
    await delay(1_000);
    assertions.push("exited-interior");

    const result = {
      name: "resource-game-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      commandMessages,
      outputDir,
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    const result = {
      name: "resource-game-flow",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      commandMessages,
      outputDir,
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
