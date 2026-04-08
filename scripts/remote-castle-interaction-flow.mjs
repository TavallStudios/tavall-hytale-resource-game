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

async function main() {
  const clientModuleUrl = pathToFileURL(path.resolve(process.cwd(), "packages/client/dist/index.js")).href;
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "CastleBot";
  const outputDir = process.argv[5] ?? path.resolve(process.cwd(), ".runs", "castle-interaction-flow");
  const resultPath = path.join(outputDir, "scenario-result.json");
  const startedAt = new Date().toISOString();
  const assertions = [];
  const pages = [];

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
    await delay(3_000);

    const lookAttempts = [
      [0, 0, 0],
      [15, 0, 0],
      [-15, 0, 0],
      [180, 0, 0],
      [90, 0, 0],
      [-90, 0, 0],
      [0, 0, 0],
    ];
    let castlePage = null;
    for (const [yaw, pitch, roll] of lookAttempts) {
      bot.look(yaw, pitch, roll);
      await delay(1_250);
      castlePage = bot.ui.currentPage?.key === "com.tavall.hytale.resourcegame.ui.CastleMainPage"
        ? bot.ui.currentPage
        : null;
      if (castlePage) {
        break;
      }
    }
    if (!castlePage) {
      castlePage = await bot.waitForPage("com.tavall.hytale.resourcegame.ui.CastleMainPage", 8_000);
    }
    pages.push({ key: castlePage.key, title: castlePage.title ?? null, snapshot: bot.snapshotPage() });
    assertions.push("castle-ui-opened-from-look");

    const result = {
      name: "castle-interaction-flow",
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
      name: "castle-interaction-flow",
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
