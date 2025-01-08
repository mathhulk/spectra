import { ServerOptions } from "./server.js";
import path from "path";

export type Mode = "development" | "production";

export interface Config {
  server?: ServerOptions;
  outDir?: string;
}

export const defineConfig = (
  options: Config | (() => Config) | (() => Promise<Config>)
) => {
  return options;
};

export const getConfig = async (relativePath?: string): Promise<Config> => {
  try {
    const filePath = path.resolve(
      process.cwd(),
      relativePath ?? "minecraft.config.js"
    );

    const module = await import(filePath);

    if (typeof module.default === "function") {
      return await module.default();
    }

    return module.default;
  } catch {
    console.log("Missing or invalid configuration file");

    process.exit(1);
  }
};
