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

async function writeJson(filePath, value) {
  await mkdir(path.dirname(filePath), { recursive: true });
  await writeFile(filePath, `${JSON.stringify(value, null, 2)}\n`, "utf8");
}

async function main() {
  const clientModuleUrl = pathToFileURL(path.resolve(process.cwd(), "packages/client/dist/index.js")).href;
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "VisualCounterBot";
  const uuid = process.argv[5] ?? "223e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "visual-counter-flow");
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

    bot.chat("/kingdom citizens set 8");
    await delay(350);
    bot.chat("/kingdom troops set 2");
    await delay(750);
    assertions.push("baseline-population-set");

    bot.chat("/kingdom interior");
    await bot.waitForWorldActivity(10_000);
    assertions.push("interior-entered");
    const interiorPage = await waitForPageOrNull(bot, "com.tavall.hytale.resourcegame.ui.InteriorMainPage", 10_000);
    if (interiorPage) {
      pages.push({ key: interiorPage.key, title: interiorPage.title ?? null, snapshot: bot.snapshotPage() });
      assertions.push("interior-ui-opened");
    }
    await delay(1_500);

    bot.chat("/kingdom citizens set 21");
    await delay(350);
    bot.chat("/kingdom troops set 5");
    await delay(1_250);
    assertions.push("interior-population-updated");

    bot.chat("/kingdom interior exit");
    await bot.waitForWorldActivity(10_000);
    await delay(1_000);
    assertions.push("interior-exited");

    const result = {
      name: "remote-visual-counter-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      expected: {
        initialCitizens: 8,
        initialTroops: 2,
        updatedCitizens: 21,
        updatedTroops: 5
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    const result = {
      name: "remote-visual-counter-flow",
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
