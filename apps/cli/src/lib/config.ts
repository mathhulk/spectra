import { existsSync, Stats } from "fs";
import path from "path";
import process from "process";
import repositories from "./types/repositories.js";
import { mkdir, stat } from "fs/promises";

/**
 * Options for running the Minecraft server
 */
export interface Config {
  /**
   * You must accept the [EULA](https://aka.ms/MinecraftEULA) before you can run a server
   * @default false
   */
  eula?: boolean;

  /**
   * Output directory
   * @default "dist"
   */
  outDir?: string;

  /**
   * Server directory
   * @default ".minecraft"
   */
  dir?: string;

  /**
   * Server type
   * @default "spigot"
   */
  type?: "spigot" | "paper" | "purpur";

  /**
   * Server version
   *
   * Ignored if `jar.path` is provided; defaults to the latest version otherwise
   */
  version?: string;

  java?: {
    /**
     * Path to the Java executable
     * @default "java"
     */
    path?: string;

    /**
     * Arguments to pass to the Java executable
     * @default ["-Xmx2G', '-Xms2G"]
     */
    args?: string[];
  };

  jar?: {
    /**
     * Path to the server executable
     *
     * Ignores `version` and uses the provided file instead
     */
    path?: string;

    /**
     * Arguments to pass to the server executable
     * @default ["nogui"]
     */
    args?: string[];
  };
}

type Type = keyof typeof repositories;

export const getType = (type?: string) => {
  const types = Object.keys(repositories);

  if (!type) return types[0] as Type;

  if (!types.includes(type)) {
    console.error(
      `Invalid server type: ${type}. Available types: ${repositories}`
    );

    process.exit(1);
  }

  return type as Type;
};

export const getVersion = async (
  type: keyof typeof repositories,
  version?: string
) => {
  if (!version) return await repositories[type].getLatestVersion();

  const versions = await repositories[type].getVersions();

  if (!versions.includes(version)) {
    console.error(
      `Invalid version: ${version}. Available versions: ${versions}`
    );

    process.exit(1);
  }

  return version;
};

export const getDirectory = async (dir = ".minecraft") => {
  const dirPath = path.join(process.cwd(), dir);

  if (!existsSync(dirPath)) {
    try {
      mkdir(dirPath, { recursive: true });
    } catch (error) {
      throw new Error(`Failed to create directory: ${dirPath}`, {
        cause: error,
      });
    }

    return dirPath;
  }

  let stats: Stats;

  try {
    stats = await stat(dirPath);
  } catch (error) {
    throw new Error(`Failed to access directory: ${dirPath}`, {
      cause: error,
    });
  }

  if (!stats.isDirectory()) {
    throw new Error(`Not a directory: ${dirPath}`);
  }

  return dirPath;
};

export const defineConfig = (
  options: Config | (() => Config) | (() => Promise<Config>)
) => {
  return options;
};

export const getConfig = async (configPath?: string): Promise<Config> => {
  let config: Config | undefined;

  // Force the provided path to be used; otherwise, fall back to the default paths
  const fileNames = configPath
    ? [configPath]
    : ["minecraft.config.mjs", "minecraft.config.js"];

  // Try each file until one can be imported
  for (const fileName of fileNames) {
    const filePath = path.resolve(process.cwd(), fileName);

    if (!existsSync(filePath)) continue;

    try {
      const module = await import(filePath);

      if (typeof module.default === "function") {
        config = await module.default();
      }

      config = module.default;

      // Use the first imported file
      break;
    } catch {
      continue;
    }
  }

  if (typeof config !== "object") {
    console.log("Missing or invalid configuration file");

    process.exit(1);
  }

  return config;
};
