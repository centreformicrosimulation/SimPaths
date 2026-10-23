import { defineConfig } from "@playwright/test";

const externalServer = process.env.SIMPATHS_DOCS_URL;

export default defineConfig({
  testDir: ".",
  testMatch: "layout.spec.mjs",
  outputDir: "test-results",
  reporter: [["list"]],
  fullyParallel: false,
  workers: 1,
  use: {
    baseURL: externalServer || "http://127.0.0.1:8142",
    colorScheme: "light",
    reducedMotion: "reduce",
    screenshot: "only-on-failure",
    trace: "retain-on-failure"
  },
  webServer: externalServer
    ? undefined
    : {
        command: "mkdocs serve -a 127.0.0.1:8142",
        cwd: "../..",
        url: "http://127.0.0.1:8142/",
        reuseExistingServer: true,
        timeout: 120000
      },
  projects: [
    {
      name: "desktop",
      use: { viewport: { width: 1280, height: 720 } }
    },
    {
      name: "mobile",
      use: { viewport: { width: 390, height: 844 } }
    }
  ]
});
