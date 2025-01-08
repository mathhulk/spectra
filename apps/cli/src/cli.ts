#! /usr/bin/env node

import { program } from "commander";

import { getConfig } from "./lib/config.js";
import { runServer } from "./lib/server.js";
import { symlink } from "fs/promises";
import path from "path";

program
  .name("minecraft-plugin")
  .version("1.0.0")
  .description("A Minecraft plugin");

program
  .alias("dev")
  .option("-c, --config <config>", "path to the configuration file")
  .option("-f, --force", "force the server to run")
  .action(async (_, options) => {
    const config = await getConfig(options.config);

    const { path: serverPath } = await runServer(config.server);

    const targetPath = path.resolve(
      serverPath,
      "plugins",
      "SpigotScript",
      "scripts"
    );

    const outDir = path.resolve(process.cwd(), config.outDir ?? "dist");

    // Link the scripts directory to the output directory
    await symlink(targetPath, outDir, "dir");
  });

program.parse();
