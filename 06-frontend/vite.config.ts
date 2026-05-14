import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

export default defineConfig({
    plugins: [
        scalaJSPlugin({
            projectID: "frontend", // must match the sbt project ID in your build.sbt
            cwd: ".."              // build.sbt is one level up (repo root)
        })
    ],
    server: {
        port: 5173,
        // If your http4s runs on 8080, uncomment to proxy API routes in dev:
        proxy: { "/api": "http://localhost:8080" }
    },
    build: {
        outDir: "dist"
    }
});