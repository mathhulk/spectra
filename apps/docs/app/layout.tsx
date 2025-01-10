import { Footer, Layout, Navbar } from "nextra-theme-docs";
import { getPageMap } from "nextra/page-map";
import "nextra-theme-docs/style.css";

export default async function RootLayout({ children }) {
  const pageMap = await getPageMap();

  return (
    <html lang="en" dir="ltr" suppressHydrationWarning>
      <body>
        <Layout
          navbar={
            <Navbar
              logo={<b>Spectra</b>}
              projectLink="https://github.com/mathhulk/spectra"
            />
          }
          pageMap={pageMap}
          docsRepositoryBase="https://github.com/mathhulk/spectra/tree/main/apps/docs"
          footer={<Footer>Test</Footer>}
        >
          {children}
        </Layout>
      </body>
    </html>
  );
}
