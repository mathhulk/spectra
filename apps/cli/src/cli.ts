#! /usr/bin/env node

import { program } from "commander";

import { getConfig, getType } from "./lib/config.js";
import { runServer } from "./lib/server.js";
import path from "path";
import { readlink, symlink } from "fs/promises";
import repositories from "./lib/types/repositories.js";

program
  .name("minecraft-plugin")
  .version("1.0.0")
  .description("A Minecraft plugin");

program
  .command("versions")
  .argument("<server-type>")
  .option("-l, --latest", "get the latest version")
  .action(async (serverType, options) => {
    const type = getType(serverType);

    const versions = await (options.latest
      ? repositories[type].getLatestVersion()
      : repositories[type].getVersions());

    console.log(versions);

    process.exit(0);
  });

program
  .alias("dev")
  .option("-c, --config <config>", "path to the configuration file")
  .option("-f, --force", "force overwrite the server directory")
  .action(async (_, options) => {
    const config = await getConfig(options.config);

    const { directory } = await runServer(config);

    const targetPath = path.resolve(
      directory,
      "plugins",
      "SpigotScript",
      "scripts"
    );

    const outDir = path.resolve(process.cwd(), config.outDir ?? "dist");

    // Link the scripts directory to the output directory
    try {
      await readlink(targetPath);
    } catch {
      await symlink(outDir, targetPath);
    }
  });

program.parse();
