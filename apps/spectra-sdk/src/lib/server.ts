import path from "path";
import { spawn } from "child_process";
import { copyFile, stat, readFile, writeFile, readdir } from "fs/promises";
import { existsSync, Stats } from "fs";
import { Config, getDirectory, getType, getVersion } from "./config.js";
import repositories from "./types/repositories.js";
import downloadPlugin from "./plugin.js";

const VERSION_FILE = "version.json";

const setStatus = async (
  dirPath: string,
  type: string,
  local: boolean,
  version: string
) => {
  const versionPath = path.join(dirPath, VERSION_FILE);

  try {
    const status = { type, local, version };
    const text = JSON.stringify(status);
    await writeFile(versionPath, text);
  } catch (error) {
    throw new Error(`Failed to write status file: ${versionPath}`, {
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
  const versionPath = path.join(dirPath, VERSION_FILE);

  let valid = false;

  // Validate the existing repository
  exists: if (existsSync(versionPath)) {
    let stats: Stats;

    try {
      stats = await stat(versionPath);
    } catch (error) {
      throw new Error(`Failed to access status file: ${versionPath}`, {
        cause: error,
      });
    }

    if (!stats.isFile()) {
      throw new Error(`Not a file: ${versionPath}`);
    }

    let status: { type: string; version: string; local: boolean };

    try {
      const text = await readFile(versionPath, "utf-8");
      status = JSON.parse(text);
    } catch (error) {
      throw new Error(`Failed to read status file: ${versionPath}`, {
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

  if (!force) {
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
  }

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
  const type = getType(config.server?.type);

  // Validate the local executable if provided
  const executablePath = await getExecutablePath(config.server?.jar?.path);

  const local = !!executablePath;

  // Validate the server version if necessary
  const version =
    executablePath ?? (await getVersion(type, config.server?.version));

  // Validate the server directory
  const directory = await getDirectory(config.server?.dir);

  // Validate the server status
  const status = await getStatus(directory, type, local, version, force);

  const javaPath = config.server?.java?.path ?? "java";
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

    console.log(`Downloading ${type} ${version}...`);

    await repositories[type].downloadVersion(serverPath, version, javaPath);

    console.log("Done downloading!");
  }

  // Set the server status
  setStatus(directory, type, local, version);

  // Ensure the plugin exists
  try {
    console.log("Downloading plugin...");

    await downloadPlugin(directory);

    console.log("Done downloading!");
  } catch (error) {
    throw new Error("Failed to download plugin", { cause: error });
  }

  // Accept the EULA
  if (!config.server?.eula) {
    console.log(
      "You must accept the EULA (https://aka.ms/MinecraftEULA) before you can run a server. Please set eula to true in your configuration file"
    );

    process.exit(1);
  }

  const eulaPath = path.join(directory, "eula.txt");
  writeFile(eulaPath, "eula=true");

  // Run Spigot
  const javaAruments = config.server?.java?.args ?? ["-Xmx2G", "-Xms2G"];
  const serverArguments = config?.server?.jar?.args ?? ["nogui"];

  console.log("Starting server...");

  const serverProcess = spawn(
    javaPath,
    [...javaAruments, "-jar", serverPath, ...serverArguments],
    {
      cwd: directory,
      stdio: "inherit",
    }
  );

  // Ensure the child process reflects the parent
  process.on("SIGINT", () => {
    serverProcess.kill("SIGINT");
  });

  return { directory, process: serverProcess };
};
