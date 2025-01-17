import { createWriteStream } from "fs";
import { Readable } from "stream";
import { finished } from "stream/promises";
import Repository from "./repository.js";

interface PaperVersions {
  version_groups: string[];
  versions: string[];
}

interface PaperBuild {
  build: number;
}

interface PaperBuilds {
  builds: PaperBuild[];
}

const getPaperBuilds = async (version: string) => {
  const response = await fetch(
    `https://api.papermc.io/v2/projects/paper/versions/${version}/builds`
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.statusText}`);
  }

  const data = await response.json();

  return data as PaperBuilds;
};

const getPaperVersions = async () => {
  const response = await fetch(`https://api.papermc.io/v2/projects/paper`);

  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.statusText}`);
  }

  const data = await response.json();

  return data as PaperVersions;
};

const paper: Repository = {
  getVersions: async () => {
    try {
      const { versions } = await getPaperVersions();
      return versions;
    } catch (error) {
      throw new Error("Failed to get Paper versions", {
        cause: error,
      });
    }
  },

  getLatestVersion: async () => {
    try {
      const { versions } = await getPaperVersions();

      const latestVersion = versions.pop();
      if (!latestVersion) throw new Error("No versions found");

      return latestVersion;
    } catch (error) {
      throw new Error("Failed to get the latest Paper version", {
        cause: error,
      });
    }
  },

  downloadVersion: async (serverPath: string, version: string) => {
    try {
      // Get the latest build for the specified version
      const { builds } = await getPaperBuilds(version);

      const latestBuild = builds.toSorted((a, b) => a.build - b.build).pop();
      if (!latestBuild) throw new Error("No builds found");

      // Download the latest build
      const fileName = `paper-${version}-${latestBuild.build}.jar`;

      const response = await fetch(
        `https://api.papermc.io/v2/projects/paper/versions/${version}/builds/${latestBuild.build}/downloads/${fileName}`
      );

      if (!response.ok) {
        throw new Error(`Failed to fetch: ${response.statusText}`);
      }

      if (!response.body) {
        throw new Error("No response body");
      }

      const stream = createWriteStream(serverPath);
      const pipe = Readable.fromWeb(response.body).pipe(stream);
      await finished(pipe);
    } catch (error) {
      throw new Error("Failed to download Paper version", {
        cause: error,
      });
    }
  },
};

export default paper;
