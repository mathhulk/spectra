#! /usr/bin/env node

import { program } from "commander";
import { existsSync } from "fs";
import { cp, mkdir, readdir } from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";

const DEFAULT_TEMPLATE = "js";

const TEMPLATES_DIR = path.join(
  fileURLToPath(path.dirname(import.meta.url)),
  "..",
  "templates"
);

const getTemplate = async (template?: string) => {
  if (template) {
    const files = await readdir(TEMPLATES_DIR);

    if (!files.includes(template)) {
      console.error(`Template ${template} not found.`);

      process.exit(1);
    }

    return template;
  }

  return DEFAULT_TEMPLATE;
};

program
  .alias("dev")
  .option("-t, --template <template>", "template to use")
  .option("-f, --force", "force the creation of the project")
  .argument("[path]", "path to create the project in", ".")
  .action(async function () {
    const options = this.opts();
    const dirPath = this.args[0];

    const template = await getTemplate(options.template);

    const templateDir = path.join(TEMPLATES_DIR, template);
    const targetDir = path.resolve(process.cwd(), dirPath);

    try {
      const exists = existsSync(targetDir);

      if (exists && !options.force) {
        const existingFiles = await readdir(targetDir);

        if (existingFiles.length > 0) {
          console.error(
            `Directory ${targetDir} is not empty. Use --force to overwrite.`
          );

          process.exit(1);
        }
      } else {
        await mkdir(targetDir, { recursive: true });
      }

      const files = await readdir(templateDir);

      for (const file of files) {
        const src = path.join(templateDir, file);
        const dest = path.join(targetDir, file);

        await cp(src, dest, { recursive: true });
      }

      console.log(
        "Project created successfully. Run `npm install` to install dependencies and `npm run dev` to start the development server."
      );
    } catch (error) {
      throw new Error("Failed to create project", {
        cause: error,
      });
    }
  });

program.parse();
