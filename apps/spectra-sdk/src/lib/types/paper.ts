import { createHash } from "crypto";
import { createWriteStream } from "fs";
import { rename, rm } from "fs/promises";
import { Readable, Transform } from "stream";
import { pipeline } from "stream/promises";
import Repository from "./repository.js";

// PaperMC's Fill API (v3). The v2 API was sunset and returns HTTP 410.
const API = "https://fill.papermc.io/v3/projects/paper";

// Fill asks clients to identify themselves.
const HEADERS = {
  "User-Agent": "spectra-sdk (https://github.com/mathhulk/spectra)",
};

interface ProjectResponse {
  /** Version groups (newest first) mapped to their versions (newest first). */
  versions: Record<string, string[]>;
}

interface BuildResponse {
  id: number;
  channel: "STABLE" | "BETA" | "ALPHA" | "RECOMMENDED";
  downloads: Record<
    string,
    { name: string; url: string; size: number; checksums: { sha256: string } }
  >;
}

const getJson = async <T>(url: string): Promise<T> => {
  const response = await fetch(url, { headers: HEADERS });

  if (!response.ok) {
    throw new Error(`Failed to fetch ${url}: ${response.status} ${response.statusText}`);
  }

  return (await response.json()) as T;
};

const isRelease = (version: string) => /^\d+(\.\d+)*$/.test(version);

const getVersions = async () => {
  const { versions } = await getJson<ProjectResponse>(API);

  return Object.values(versions).flat();
};

const paper: Repository = {
  getVersions: async () => {
    try {
      return await getVersions();
    } catch (error) {
      throw new Error("Failed to get Paper versions", { cause: error });
    }
  },

  getLatestVersion: async () => {
    try {
      const latest = (await getVersions()).find(isRelease);
      if (!latest) throw new Error("No release versions found");

      return latest;
    } catch (error) {
      throw new Error("Failed to get the latest Paper version", {
        cause: error,
      });
    }
  },

  downloadVersion: async (serverPath: string, version: string) => {
    try {
      const build = await getJson<BuildResponse>(
        `${API}/versions/${encodeURIComponent(version)}/builds/latest`
      );

      const download = build.downloads["server:default"];
      if (!download) throw new Error(`Build ${build.id} has no server download`);

      const response = await fetch(download.url, { headers: HEADERS });

      if (!response.ok || !response.body) {
        throw new Error(`Failed to download ${download.url}: ${response.status}`);
      }

      // Write to a temporary file and verify the checksum before replacing the server jar.
      const hash = createHash("sha256");
      const temporaryPath = `${serverPath}.download`;

      await pipeline(
        Readable.fromWeb(response.body),
        new Transform({
          transform(chunk, _encoding, callback) {
            hash.update(chunk);
            callback(null, chunk);
          },
        }),
        createWriteStream(temporaryPath)
      );

      const digest = hash.digest("hex");

      if (digest !== download.checksums.sha256) {
        await rm(temporaryPath, { force: true });

        throw new Error(
          `Checksum mismatch for ${download.name}: expected ${download.checksums.sha256}, got ${digest}`
        );
      }

      await rename(temporaryPath, serverPath);
    } catch (error) {
      throw new Error("Failed to download Paper version", { cause: error });
    }
  },
};

export default paper;
