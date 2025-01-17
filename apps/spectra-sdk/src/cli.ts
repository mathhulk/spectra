#! /usr/bin/env node

import { program } from "commander";

import { getConfig, getType } from "./lib/config.js";
import { runServer } from "./lib/server.js";
import path from "path";
import { readlink, symlink } from "fs/promises";
import repositories from "./lib/types/repositories.js";
import esbuild from "esbuild";

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
  .action(async function () {
    const options = this.opts();

    // Parse the configuration file
    const config = await getConfig(options.config);

    // Run the server
    const { directory } = await runServer(config, options.force);

    // Build the script(s)
    const format = config.build?.format ?? "esm";
    const bundle = config.build?.bundle ?? true;
    const platform = config.build?.platform ?? "node";
    const outdir = config.build?.outdir ?? "dist";
    const entryPoints = config.build?.entryPoints ?? [
      path.resolve(process.cwd(), "src" + path.sep + "main.ts"),
    ];

    const context = await esbuild.context({
      format,
      bundle,
      platform,
      outdir,
      entryPoints,
    });

    await context.watch();

    // Link the scripts directory to the output directory
    const targetPath = path.resolve(directory, "scripts");
    const outDir = path.resolve(process.cwd(), outdir);

    try {
      await readlink(targetPath);
    } catch {
      await symlink(outDir, targetPath);
    }
  });

program.parse();
