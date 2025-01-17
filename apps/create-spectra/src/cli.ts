#! /usr/bin/env node

import { program } from "commander";
import { cp, readdir } from "fs/promises";
import path from "path";

const DEFAULT_TEMPLATE = "javascript";

const TEMPLATES_DIR = path.join(__dirname, "..", "templates");

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
  .action(async function () {
    const options = this.opts();

    const template = await getTemplate(options.template);

    // Copy the template to the current directory
    console.log(`Creating project using template: ${template}`);

    const templateDir = path.join(TEMPLATES_DIR, template);
    const currentDir = process.cwd();

    try {
      const files = await readdir(templateDir);

      for (const file of files) {
        const src = path.join(templateDir, file);
        const dest = path.join(currentDir, file);

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
