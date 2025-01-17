import { defineConfig } from "vitepress";

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "Spectra",
  description: "A VitePress Site",
  base: "/spectra/",
  lastUpdated: true,
  markdown: {
    lineNumbers: true,
  },
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: "Guide", link: "/guide/what-is-spectra" },
      { text: "Reference", link: "/reference" },
      { text: "Examples", link: "/examples" },
    ],

    editLink: {
      pattern: "https://github.com/mathhulk/spectra/edit/main/apps/docs/:path",
      text: "Edit this page on GitHub",
    },

    search: {
      provider: "local",
    },

    sidebar: [
      {
        text: "Introduction",
        collapsed: false,
        items: [
          { text: "What is Spectra?", link: "/guide/what-is-spectra" },
          { text: "Getting started", link: "/guide/getting-started" },
        ],
      },
      {
        text: "SDK",
        collapsed: false,
      },
      {
        text: "Software",
        collapsed: false,
        items: [
          {
            text: "Bukkit",
            link: "/guide/bukkit",
          },
          {
            text: "Spigot",
            link: "/guide/spigot",
          },
          {
            text: "Paper",
            link: "/guide/paper",
          },
          {
            text: "Purpur",
            link: "/guide/purpur",
          },
          {
            text: "BungeeCord",
            link: "/guide/bungeecord",
          },
          {
            text: "Velocity",
            link: "/guide/velocity",
          },
        ],
      },
    ],

    socialLinks: [
      { icon: "github", link: "https://github.com/mathhulk/spectra" },
    ],
  },
});
