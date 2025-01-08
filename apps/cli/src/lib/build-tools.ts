import { createWriteStream, existsSync } from "fs";
import { Readable } from "stream";
import { finished } from "stream/promises";
import path from "path";
import { getCachePath } from "./cache.js";
import { spawn } from "child_process";

export const BUILD_TOOLS_URL =
  "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar";

export const downloadBuildTools = async () => {
  const cachePath = await getCachePath();
  const cachedBuildToolsPath = path.join(cachePath, "BuildTools.jar");

  // Verify whether BuildTools was already cached
  if (existsSync(cachedBuildToolsPath)) {
    console.log(`Using cached BuildTools.jar at ${cachedBuildToolsPath}`);

    return cachedBuildToolsPath;
  }

  console.log(
    `Cached BuildTools.jar not found at ${cachedBuildToolsPath}. Downloading from ${BUILD_TOOLS_URL}...`
  );

  // Download BuildTools
  const response = await fetch(BUILD_TOOLS_URL);

  if (!response.ok) {
    throw new Error(
      `Failed to download BuildTools.jar from ${BUILD_TOOLS_URL}: ${response.statusText}`
    );
  }

  if (!response.body) {
    throw new Error(
      `Failed to download BuildTools.jar from ${BUILD_TOOLS_URL}: Response body is empty`
    );
  }

  // Cache BuildTools
  const stream = createWriteStream(cachedBuildToolsPath);
  await finished(Readable.fromWeb(response.body).pipe(stream));

  console.log(`Successfully cached BuildTools.jar at ${cachedBuildToolsPath}`);

  return cachedBuildToolsPath;
};

export const runBuildTools = async (version: string): Promise<string> => {
  // Verify whether Spigot was already cached
  const cachePath = await getCachePath();
  const cachedSpigotPath = path.join(cachePath, `spigot-${version}.jar`);

  if (existsSync(cachedSpigotPath)) {
    console.log(
      `Using cached Spigot version ${version} at ${cachedSpigotPath}`
    );

    return cachedSpigotPath;
  }

  // Download BuildTools
  const cachedBuildToolsPath = await downloadBuildTools();

  console.log(
    `Cached Spigot version ${version} not found at ${cachedSpigotPath}. Running BuildTools.jar from ${cachedBuildToolsPath}...`
  );

  // Run BuildTools
  const buildToolsProcess = spawn(
    "java",
    ["-jar", cachedBuildToolsPath, "--rev", version],
    {
      cwd: cachePath,
    }
  );

  return new Promise((resolve, reject) => {
    buildToolsProcess.on("close", (code) => {
      if (code === 0) {
        resolve(cachedSpigotPath);

        return;
      }

      reject(
        new Error(
          `Failed to generate Spigot version ${version} using BuildTools.jar`
        )
      );
    });
  });
};
