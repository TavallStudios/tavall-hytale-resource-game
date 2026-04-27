import path from "node:path";
import { captureWorldSnapshot, delay, ensureBotBaseline, resolveBotClientModuleUrl, writeJson, printStructured, } from "./bot-flow-helpers.mjs";

function readSelectorValue(snapshot, selector) {
  const command = snapshot?.commands?.slice().reverse().find((entry) => entry.type === "Set" && entry.selector === selector);
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

async function openWoodNodeDetail(bot, timeoutMs = 15_000) {
  const startedAt = Date.now();
  let attempt = 0;
  while ((Date.now() - startedAt) < timeoutMs) {
    bot.chat("/kingdom nodes select 1");
    attempt += 1;
    try {
      return await waitForSnapshot(
        bot,
        (snapshot) => snapshot.key === "com.tavall.hytale.resourcegame.ui.ResourceNodePage"
          && `${readSelectorValue(snapshot, "#NodeTitle.Text")}`.toLowerCase().includes("wood"),
        Math.min(4_000, Math.max(1_000, timeoutMs - (Date.now() - startedAt))),
        `wood node detail attempt ${attempt}`
      );
    } catch {
      await delay(500);
    }
  }
  throw new Error(`Timed out waiting for wood node detail; finalSnapshot=${JSON.stringify(bot.snapshotPage())}`);
}

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "PlacementBot";
  const uuid = process.argv[5] ?? "723e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "placement-flow");
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
      nearbyRadius: 16
    });
    await delay(1_000);
    bot.chat("/kingdom nodes clear");
    await delay(500);

    bot.chat("/kingdom place node wood");
    await delay(600);
    bot.chat("/kingdom place confirm here");
    await delay(1_200);
    assertions.push("node-placement-confirmed");

    const nodeSnapshot = await openWoodNodeDetail(bot);
    const title = `${readSelectorValue(nodeSnapshot, "#NodeTitle.Text")}`.toLowerCase();
    if (!title.includes("wood")) {
      throw new Error(`Unexpected node title after placement: ${JSON.stringify(nodeSnapshot)}`);
    }
    pages.push({ key: nodeSnapshot.key, title: null, snapshot: nodeSnapshot });
    assertions.push("node-ui-opened-after-placement");

    const result = {
      name: "remote-placement-flow",
      success: true,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      clientSnapshot: {
        baseline: baseline.snapshot,
        final: captureWorldSnapshot(bot, 16)
      },
      finalServerMessage: bot.getServerMessages().at(-1) ?? null
    };
    await bot.trace.flush(outputDir);
    await writeJson(resultPath, result);
    printStructured(result);
  } catch (error) {
    const result = {
      name: "remote-placement-flow",
      success: false,
      startedAt,
      endedAt: new Date().toISOString(),
      assertions,
      pages,
      error: error instanceof Error ? error.message : String(error),
      finalPageSnapshot: bot.snapshotPage(),
      clientSnapshot: {
        final: captureWorldSnapshot(bot, 16)
      },
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
