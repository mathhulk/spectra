import path from "path";
import { spawn } from "child_process";
import {
  copyFile,
  mkdir,
  stat,
  readFile,
  writeFile,
  readdir,
} from "fs/promises";
import { existsSync, Stats } from "fs";
import { Config, getDirectory, getType, getVersion } from "./config.js";
import repositories from "./types/repositories.js";

// TODO: Download the plugin from a remote source
const PLUGIN_PATH =
  "/Users/matthewrowland/Documents/GitHub/mathhulk/spigot-script/apps/plugin/target/spigot-script-1.0-SNAPSHOT.jar";

const setStatus = async (
  filePath: string,
  type: string,
  local: boolean,
  version: string
) => {
  try {
    const status = { type, local, version };
    const text = JSON.stringify(status);
    await writeFile(filePath, text);
  } catch (error) {
    throw new Error(`Failed to write status file: ${filePath}`, {
      cause: error,
    });
  }
};

const getStatus = async (
  dirPath: string,
  type: string,
  local: boolean,
  version: string,
  force = false
) => {
  const statusPath = path.join(dirPath, "status.json");

  let valid = false;

  // Validate the existing status
  exists: if (existsSync(statusPath)) {
    let stats: Stats;

    try {
      stats = await stat(statusPath);
    } catch (error) {
      throw new Error(`Failed to access status file: ${statusPath}`, {
        cause: error,
      });
    }

    if (!stats.isFile()) {
      throw new Error(`Not a file: ${statusPath}`);
    }

    let status: { type: string; version: string; local: boolean };

    try {
      const text = await readFile(statusPath, "utf-8");
      status = JSON.parse(text);
    } catch (error) {
      throw new Error(`Failed to read status file: ${statusPath}`, {
        cause: error,
      });
    }

    if (
      status.type === type &&
      status.version === version &&
      status.local === local
    ) {
      valid = true;

      break exists;
    }

    if (status.type !== type) {
      console.error(`Type changed: ${status.type} -> ${type}`);
    }

    if (status.local !== local || status.version !== version) {
      console.error(`Version changed: ${status.version} -> ${version}`);
    }
  }

  if (valid) return true;

  if (force) {
    await setStatus(statusPath, type, local, version);

    return false;
  }

  try {
    const files = await readdir(dirPath);

    if (files.length > 0) {
      console.error(
        "Overwriting an existing server directory can cause unintended side effects; delete the server directory or use the --force option to continue"
      );

      process.exit(1);
    }
  } catch (error) {
    throw new Error(`Failed to read directory: ${dirPath}`, {
      cause: error,
    });
  }

  setStatus(statusPath, type, local, version);

  return false;
};

const getExecutablePath = async (executablePath?: string) => {
  if (!executablePath) return;

  const filePath = path.resolve(process.cwd(), executablePath);

  if (!existsSync(filePath)) {
    throw new Error(`File not found: ${filePath}`);
  }

  let stats: Stats;

  try {
    stats = await stat(filePath);
  } catch (error) {
    throw new Error(`Failed to access file: ${filePath}`, {
      cause: error,
    });
  }

  if (!stats.isFile()) {
    throw new Error(`Not a file: ${filePath}`);
  }

  return filePath;
};

// TODO: Class
export const runServer = async (config: Config, force = false) => {
  // Validate the server type
  const type = getType(config.type);

  // Validate the local executable if provided
  const executablePath = await getExecutablePath(config.jar?.path);

  const local = !!executablePath;

  // Validate the server version if necessary
  const version = executablePath ?? (await getVersion(type, config.version));

  // Validate the server directory
  const directory = await getDirectory(config.dir);

  // Validate the server status
  const status = await getStatus(directory, type, local, version, force);

  const javaPath = config.java?.path ?? "java";
  const serverPath = path.resolve(process.cwd(), directory, "server.jar");

  download: if (!existsSync(serverPath) || !status) {
    if (local) {
      try {
        await copyFile(executablePath, serverPath);
      } catch (error) {
        throw new Error(`Failed to copy file: ${executablePath}`, {
          cause: error,
        });
      }

      break download;
    }

    console.log("Downloading...");

    await repositories[type].downloadVersion(serverPath, version, javaPath);

    console.log("Done downloading!");
  }

  // Ensure the plugin exists
  const pluginsPath = path.join(directory, "plugins");
  const pluginPath = path.join(pluginsPath, "spigot-script.jar");
  const dataPath = path.join(pluginsPath, "SpigotScript");

  if (!existsSync(pluginPath)) {
    await mkdir(pluginsPath, { recursive: true });

    await copyFile(PLUGIN_PATH, pluginPath);
  }

  if (!existsSync(dataPath)) {
    await mkdir(dataPath, { recursive: true });
  }

  // Accept the EULA
  if (!config.eula) {
    console.log(
      "You must accept the EULA (https://aka.ms/MinecraftEULA) before you can run a server. Please set eula to true in your configuration file"
    );

    process.exit(1);
  }

  const eulaPath = path.join(directory, "eula.txt");
  writeFile(eulaPath, "eula=true");

  // Run Spigot
  const javaAruments = config.java?.args ?? ["-Xmx2G", "-Xms2G"];
  const serverArguments = config?.jar?.args ?? ["nogui"];

  console.log("Starting server...");

  const serverProcess = spawn(
    javaPath,
    [...javaAruments, "-jar", serverPath, ...serverArguments],
    {
      cwd: directory,
    }
  );

  return { directory, process: serverProcess };
};
