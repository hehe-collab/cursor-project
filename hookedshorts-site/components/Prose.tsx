import type { ReactNode } from "react";

export function Prose({ children }: { children: ReactNode }) {
  return (
    <div className="prose-docs max-w-none text-[15px] leading-relaxed text-[var(--foreground)]">
      {children}
    </div>
  );
}
