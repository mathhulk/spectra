#! /usr/bin/env node

import { program } from "commander";
import { readdir } from "fs/promises";
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

    console.log("Template:", template);
  });

program.parse();
