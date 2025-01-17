export default interface Repository {
  getVersions: () => Promise<string[]>;
  getLatestVersion: () => Promise<string>;
  downloadVersion: (
    serverPath: string,
    version: string,
    javaPath: string
  ) => Promise<void>;
}
