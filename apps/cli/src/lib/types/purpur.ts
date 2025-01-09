import { createWriteStream } from "fs";
import { finished } from "stream/promises";
import { Readable } from "stream";
import Repository from "./repository.js";

interface PurpurProject {
  metadata: {
    current: string;
  };
  versions: string[];
}

const getPurpurVersions = async () => {
  const response = await fetch("https://api.purpurmc.org/v2/purpur");

  if (!response.ok) {
    throw new Error(`Failed to fetch: ${response.statusText}`);
  }

  const data = await response.json();

  return data as PurpurProject;
};

const purpur: Repository = {
  getVersions: async () => {
    try {
      const project = await getPurpurVersions();
      return project.versions;
    } catch (error) {
      throw new Error("Failed to get Purpur versions", {
        cause: error,
      });
    }
  },

  getLatestVersion: async () => {
    try {
      const project = await getPurpurVersions();
      return project.metadata.current;
    } catch (error) {
      throw new Error("Failed to get the latest Purpur version", {
        cause: error,
      });
    }
  },

  downloadVersion: async (serverPath: string, version: string) => {
    try {
      const response = await fetch(
        `https://api.purpurmc.org/v2/purpur/${version}/latest/download`
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
      throw new Error("Failed to download Purpur version", {
        cause: error,
      });
    }
  },
};

export default purpur;
