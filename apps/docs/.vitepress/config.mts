import { defineConfig } from "vitepress";

export default defineConfig({
  title: "Spectra",
  description: "Minecraft server gameplay in TypeScript, and server-driven web UIs.",
  base: "/spectra/",
  lastUpdated: true,
  markdown: {
    lineNumbers: true,
  },
  themeConfig: {
    nav: [
      { text: "Guide", link: "/guide/what-is-spectra" },
      { text: "Scripting", link: "/scripting/scripts" },
      { text: "UI", link: "/ui/" },
      { text: "Java API", link: "/java/" },
      { text: "Reference", link: "/reference/script-api" },
    ],

    editLink: {
      pattern: "https://github.com/mathhulk/spectra/edit/nui-next/apps/docs/:path",
      text: "Edit this page on GitHub",
    },

    search: {
      provider: "local",
    },

    outline: "deep",

    sidebar: [
      {
        text: "Introduction",
        items: [
          { text: "What is Spectra?", link: "/guide/what-is-spectra" },
          { text: "Getting started", link: "/guide/getting-started" },
          { text: "Architecture", link: "/guide/architecture" },
          { text: "Project status", link: "/guide/status" },
        ],
      },
      {
        text: "Scripting",
        items: [
          { text: "Scripts and lifecycle", link: "/scripting/scripts" },
          { text: "Events", link: "/scripting/events" },
          { text: "Commands", link: "/scripting/commands" },
          { text: "Timers", link: "/scripting/timers" },
          { text: "World generation", link: "/scripting/world-generation" },
          { text: "Web APIs", link: "/scripting/web-apis" },
          { text: "Node.js compatibility", link: "/scripting/node" },
          { text: "Java interop", link: "/scripting/java" },
          { text: "TypeScript types", link: "/scripting/types" },
          { text: "Performance", link: "/scripting/performance" },
        ],
      },
      {
        text: "Server-driven UI",
        items: [
          { text: "Overview", link: "/ui/" },
          { text: "Client capabilities", link: "/ui/capabilities" },
          { text: "Opening views", link: "/ui/views" },
          { text: "Resources", link: "/ui/resources" },
          { text: "Messaging", link: "/ui/messaging" },
          { text: "HTTP and WebSockets", link: "/ui/http-and-websockets" },
          { text: "Network access", link: "/ui/network" },
          { text: "Game data and minimaps", link: "/ui/game-data" },
          { text: "Client mod", link: "/ui/client" },
          { text: "Security model", link: "/ui/security" },
        ],
      },
      {
        text: "Java API",
        items: [
          { text: "Overview", link: "/java/" },
          { text: "Clients", link: "/java/clients" },
          { text: "Views", link: "/java/views" },
          { text: "HTTP and WebSockets", link: "/java/http" },
          { text: "Resources", link: "/java/resources" },
        ],
      },
      {
        text: "Reference",
        items: [
          { text: "Script API", link: "/reference/script-api" },
          { text: "Configuration", link: "/reference/configuration" },
          { text: "Protocol", link: "/reference/protocol" },
          { text: "Platforms", link: "/reference/platforms" },
        ],
      },
    ],

    socialLinks: [{ icon: "github", link: "https://github.com/mathhulk/spectra" }],
  },
});
