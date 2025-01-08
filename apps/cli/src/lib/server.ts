import path from "path";
import { runBuildTools } from "./build-tools.js";
import { spawn } from "child_process";
import { copyFile, mkdir } from "fs/promises";
import { existsSync } from "fs";

/**
 * Options for running the Minecraft server
 */
export interface ServerOptions {
  /**
   * Server directory; defaults to `.minecraft`
   */
  dir?: string;

  /**
   * Server type; defaults to `spigot`
   */
  type: "spigot";

  /**
   * Server version; defaults to the latest stable version
   */
  version?: string;

  java?: {
    /**
     * Path to the Java executable; defaults to `java`
     */
    path?: string;

    /**
     * Arguments to pass to the Java process; defaults to `["-Xmx2G", "-Xms2G"]`
     */
    args?: string[];
  };

  jar?: {
    /**
     * Path to the server executable; skips the cache and build or download process when provided
     */
    path?: string;

    /**
     * Arguments to pass to the server.jar; defaults to `["nogui"]`
     */
    args?: string[];
  };
}

// TODO: Download the plugin from a remote source
const PLUGIN_PATH =
  "/Users/matthewrowland/Documents/GitHub/mathhulk/spigot-script/target/spigot-script-1.0-SNAPSHOT.jar";

export const runServer = async (server?: ServerOptions) => {
  const directory = server?.dir ?? ".minecraft";

  // TODO: Get the latest version based on the server type as the default
  const version = server?.version ?? "1.21.4";

  const javaPath = server?.java?.path ?? "java";
  const javaAruments = server?.java?.args ?? ["-Xmx2G", "-Xms2G"];

  const inputPath = server?.jar?.path ?? (await runBuildTools(version));
  const serverArguments = server?.jar?.args ?? ["nogui"];

  // Ensure the Minecraft directory exists
  const directoryPath = path.join(process.cwd(), directory);

  if (!existsSync(directoryPath)) {
    await mkdir(directoryPath, { recursive: true });
  }

  // Copy Spigot to the Minecraft directory
  const outputPath = path.join(directoryPath, "server.jar");
  await copyFile(inputPath, outputPath);

  console.log(`Using Spigot version ${version} from ${outputPath}`);

  // Ensure the plugin exists
  const pluginPath = path.join(directoryPath, "plugins", "spigot-script.jar");

  const scriptsPath = path.join(
    directoryPath,
    "plugins",
    "SpigotScript",
    "scripts"
  );

  if (!existsSync(pluginPath)) {
    await copyFile(PLUGIN_PATH, pluginPath);
  }

  if (!existsSync(scriptsPath)) {
    await mkdir(scriptsPath, { recursive: true });
  }

  // Run Spigot
  const serverProcess = spawn(
    javaPath,
    [...javaAruments, "-jar", outputPath, ...serverArguments],
    {
      cwd: directoryPath,
    }
  );

  return { path: outputPath, process: serverProcess };
};
