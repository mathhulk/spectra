import { defineConfig } from "spectra-sdk";

export default defineConfig({
  server: {
    // You must accept the EULA (https://aka.ms/MinecraftEULA) before you can run a server
    eula: true,
    type: "paper",
  },
});
