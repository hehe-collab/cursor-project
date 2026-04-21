"use client";

/**
 * Root-level error UI (required for App Router). Keeps the dev/ prod
 * RSC client manifest in sync; avoids Turbopack "global-error" missing-module issues.
 */
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html lang="en">
      <body
        style={{
          margin: 0,
          minHeight: "100vh",
          display: "grid",
          placeContent: "center",
          placeItems: "center",
          gap: "1rem",
          background: "#060a12",
          color: "#e8edf5",
          fontFamily: "system-ui, sans-serif",
        }}
      >
        <h1 style={{ fontSize: "1.25rem" }}>Something went wrong</h1>
        <p style={{ color: "#8b9cb3", maxWidth: "28rem", textAlign: "center" }}>
          {error.message}
        </p>
        <button
          type="button"
          onClick={() => reset()}
          style={{
            padding: "0.5rem 1rem",
            borderRadius: "0.5rem",
            border: "1px solid rgba(56, 189, 248, 0.35)",
            background: "rgba(15, 23, 42, 0.8)",
            color: "#38bdf8",
            cursor: "pointer",
          }}
        >
          Try again
        </button>
      </body>
    </html>
  );
}
