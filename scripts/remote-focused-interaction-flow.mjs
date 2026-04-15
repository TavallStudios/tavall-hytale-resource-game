import path from "node:path";
import {
  aimAtGround,
  captureWorldSnapshot,
  delay,
  ensureBotBaseline,
  focusTargetWithSweep,
  resolveBotClientModuleUrl,
  writeJson
} from "./bot-flow-helpers.mjs";

async function main() {
  const clientModuleUrl = resolveBotClientModuleUrl();
  const { createBot } = await import(clientModuleUrl);

  const host = process.argv[2] ?? "127.0.0.1";
  const port = Number.parseInt(process.argv[3] ?? "5520", 10);
  const username = process.argv[4] ?? "FocusBot";
  const uuid = process.argv[5] ?? "823e4567-e89b-12d3-a456-426614174000";
  const outputDir = process.argv[6] ?? path.resolve(process.cwd(), ".runs", "focused-interaction-flow");
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
    const baseline = await ensureBotBaseline(bot, assertions, {
      username,
      nearbyRadius: 16
    });
    await delay(1_000);

    bot.chat("/kingdom castle align");
    await delay(1_500);
    await focusTargetWithSweep(bot, {
      command: "/kingdom scan",
      expectedText: "focus: castle",
      label: "castle scan"
    });
    assertions.push("scan-castle-focus");

    bot.chat("/kingdom interact");
    const castlePage = await bot.waitForPage("com.tavall.hytale.resourcegame.ui.CastleMainPage", 8_000);
    pages.push({ key: castlePage.key, title: castlePage.title ?? null, snapshot: bot.snapshotPage() });
    assertions.push("castle-ui-opened-from-interact");

    bot.chat("/kingdom nodes clear");
    await delay(600);
    bot.chat("/kingdom place node iron");
    await delay(600);
    await aimAtGround(bot, 90, 60, 0, 900);
    bot.chat("/kingdom place confirm");
    await delay(1_200);
    assertions.push("iron-node-placed");

    bot.chat("/kingdom nodes align 1");
    await delay(1_500);
    await focusTargetWithSweep(bot, {
      command: "/kingdom scan",
      expectedText: "focus: iron node",
      label: "iron node scan"
    });
    assertions.push("scan-node-focus");

    bot.chat("/kingdom nodes status focus");
    await focusTargetWithSweep(bot, {
      command: "/kingdom nodes status focus",
      expectedText: "iron",
      label: "focused node status",
      attempts: [{ yaw: 0, pitch: 0, roll: 0 }],
      timeoutPerAttemptMs: 2_000
    });
    assertions.push("node-status-from-focus");

    bot.chat("/kingdom interact");
    const nodePage = await bot.waitForPage("com.tavall.hytale.resourcegame.ui.ResourceNodePage", 8_000);
    pages.push({ key: nodePage.key, title: nodePage.title ?? null, snapshot: bot.snapshotPage() });
    assertions.push("node-ui-opened-from-interact");

    const result = {
      name: "remote-focused-interaction-flow",
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
    console.log(JSON.stringify(result, null, 2));
  } catch (error) {
    const result = {
      name: "remote-focused-interaction-flow",
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
