import type { NextConfig } from "next";
import { fileURLToPath } from "node:url";
import path from "node:path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const nextConfig: NextConfig = {
  output: "export",
  images: {
    unoptimized: true,
  },
  // Monorepo lockfile in repo root: pin tracing to this app so builds are deterministic.
  outputFileTracingRoot: path.join(__dirname, ".."),
};

export default nextConfig;
