import path from "path";
import os from "os";
import { existsSync } from "fs";
import { mkdir } from "fs/promises";

export const CACHE_DIRECTORY =
  process.platform === "win32"
    ? path.join(os.homedir(), "AppData", "Local", "vite-plugin-minecraft")
    : path.join(os.homedir(), ".cache", "vite-plugin-minecraft");

export const getCachePath = async () => {
  const exists = existsSync(CACHE_DIRECTORY);

  if (!exists) {
    await mkdir(CACHE_DIRECTORY, { recursive: true });
  }

  return CACHE_DIRECTORY;
};
