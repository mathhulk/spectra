import os from "os";
import { createWriteStream, existsSync } from "fs";
import { Readable } from "stream";
import { finished } from "stream/promises";
import path from "path";
import { spawn } from "child_process";
import { copyFile, mkdir } from "fs/promises";
import Repository from "./repository.js";

const getCacheDirectory = () => {
  const homeDir = os.homedir();
  const platform = os.platform();

  let root;

  if (platform === "win32") {
    root = process.env.LOCALAPPDATA || path.join(homeDir, "AppData", "Local");
  } else if (platform === "darwin") {
    root = path.join(homeDir, "Library", "Caches");
  } else {
    root = process.env.XDG_CACHE_HOME || path.join(homeDir, ".cache");
  }

  return path.join(root, "Spectra", "spigot");
};

const BUILD_TOOLS_URL =
  "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";

const downloadSpigot = async (
  serverPath: string,
  version: string,
  javaPath: string
) => {
  const cacheDir = getCacheDirectory();

  console.log(cacheDir);

  if (!existsSync(cacheDir)) {
    try {
      await mkdir(cacheDir, { recursive: true });
    } catch (error) {
      throw new Error("Failed to create temporary directory", {
        cause: error,
      });
    }
  }

  // Download
  const response = await fetch(BUILD_TOOLS_URL);

  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.statusText}`);
  }

  if (!response.body) {
    throw new Error(`No response body`);
  }

  const executable = path.join(cacheDir, "BuildTools.jar");

  console.log(executable);

  const stream = createWriteStream(executable);
  await finished(Readable.fromWeb(response.body).pipe(stream));

  // Run
  const buildToolsProcess = spawn(
    javaPath,
    ["-jar", executable, "--rev", version],
    {
      cwd: cacheDir,
      stdio: "inherit",
    }
  );

  return new Promise<void>((resolve, reject) => {
    buildToolsProcess.on("close", async (code) => {
      try {
        if (code === 0) {
          // Copy the output
          const output = path.join(cacheDir, "spigot-" + version + ".jar");
          await copyFile(output, serverPath);

          resolve();
        }

        throw new Error(`BuildTools failed with exit code ${code}`);
      } catch (error) {
        reject(error);
      }
    });
  });
};

const getSpigotVersions = async () => {
  const response = await fetch("https://hub.spigotmc.org/versions");

  if (!response.ok) {
    throw new Error(`Failed to fetch versions: ${response.statusText}`);
  }

  const text = await response.text();
  const matches = text.matchAll(/<a href="(.*?\..*?)\.json">/g);

  return Array.from(matches).map((match) => match[1]);
};

const getLatestSpigotVersion = async () => {
  const versions = await getSpigotVersions();

  const latest = versions.toSorted((a, b) => {
    const [majorA, minorA, patchA = 0] = a.split(".").map(Number);
    const [majorB, minorB, patchB = 0] = b.split(".").map(Number);

    if (majorA !== majorB) return majorA - majorB;
    if (minorA !== minorB) return minorA - minorB;
    return patchA - patchB;
  });

  const latestVersion = latest.pop();
  if (!latestVersion) throw new Error("No versions found");

  return latestVersion;
};

const spigot: Repository = {
  getVersions: async () => {
    try {
      return await getSpigotVersions();
    } catch (error) {
      throw new Error("Failed to get Spigot versions", {
        cause: error,
      });
    }
  },

  getLatestVersion: async () => {
    try {
      return await getLatestSpigotVersion();
    } catch (error) {
      throw new Error("Failed to get the latest Spigot version", {
        cause: error,
      });
    }
  },

  downloadVersion: async (
    serverPath: string,
    version: string,
    javaPath: string
  ) => {
    try {
      await downloadSpigot(serverPath, version, javaPath);
    } catch (error) {
      throw new Error("Failed to download Spigot version", {
        cause: error,
      });
    }
  },
};

export default spigot;
