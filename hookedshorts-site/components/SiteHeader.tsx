import Link from "next/link";
import { siteConfig } from "@/lib/site";

const nav = [
  { href: "/", label: "Home" },
  { href: "/about", label: "About" },
  { href: "/services", label: "Services" },
  { href: "/contact", label: "Contact" },
];

export function SiteHeader() {
  return (
    <header className="relative z-20 border-b border-[var(--border)] bg-[#0a1020]/85 backdrop-blur-md">
      <div className="mx-auto flex max-w-5xl flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between md:px-6">
        <Link href="/" className="group flex items-center gap-2">
          <span
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-[var(--border)] bg-[var(--surface)] text-xs font-bold text-[var(--accent-strong)]"
            aria-hidden
          >
            HS
          </span>
          <span className="text-lg font-semibold tracking-tight text-[var(--foreground)] transition group-hover:text-[var(--accent-strong)]">
            {siteConfig.brandName}
          </span>
        </Link>
        <nav
          className="font-mono flex flex-wrap gap-x-5 gap-y-2 text-xs font-medium uppercase tracking-wider text-[var(--muted)]"
          aria-label="Primary"
        >
          {nav.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="transition-colors hover:text-[var(--accent-strong)]"
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}
