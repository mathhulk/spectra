import { createWriteStream, existsSync } from "fs";
import { mkdir } from "fs/promises";
import path from "path";
import { Readable } from "stream";
import { finished } from "stream/promises";

interface User {
  login: string;
  id: number;
  node_id: string;
  avatar_url: string;
  gravatar_id: string;
  url: string;
  html_url: string;
  followers_url: string;
  following_url: string;
  gists_url: string;
  starred_url: string;
  subscriptions_url: string;
  organizations_url: string;
  repos_url: string;
  events_url: string;
  received_events_url: string;
  type: string;
  site_admin: boolean;
}

interface Asset {
  url: string;
  id: number;
  node_id: string;
  name: string;
  label: string;
  uploader: User;
  content_type: string;
  state: string;
  size: number;
  download_count: number;
  created_at: string;
  updated_at: string;
  browser_download_url: string;
}

interface Release {
  url: string;
  assets_url: string;
  upload_url: string;
  html_url: string;
  id: number;
  author: User;
  node_id: string;
  tag_name: string;
  target_commitish: string;
  name: string;
  draft: boolean;
  prerelease: boolean;
  created_at: string;
  published_at: string;
  assets: Asset[];
  tarball_url: string;
  zipball_url: string;
  body: string;
}

const getLatestRelease = async () => {
  const response = await fetch(
    "https://api.github.com/repos/mathhulk/spectra/releases"
  );

  if (!response.ok) {
    throw new Error("Failed to fetch releases");
  }

  const releases = (await response.json()) as Release[];

  return releases[0];
};

const getEnvironment = () => {
  return "bukkit";
};

const downloadPlugin = async (directory: string) => {
  const environment = getEnvironment();

  const release = await getLatestRelease();

  const pluginsPath = path.join(directory, "plugins");
  const pluginPath = path.join(pluginsPath, "spectra.jar");

  if (!existsSync(pluginPath)) {
    await mkdir(pluginsPath, { recursive: true });
  }

  const asset = release.assets.find((asset) =>
    asset.name.includes(environment)
  );

  if (!asset) {
    throw new Error(`Failed to find asset for environment: ${environment}`);
  }

  const response = await fetch(asset.browser_download_url);

  if (!response.ok) {
    throw new Error("Failed to fetch");
  }

  if (!response.body) {
    throw new Error(`No response body`);
  }

  const stream = createWriteStream(pluginPath);
  await finished(Readable.fromWeb(response.body).pipe(stream));
};

export default downloadPlugin;
