import Link from "next/link";
import { siteConfig } from "@/lib/site";

const legal = [
  { href: "/privacy", label: "Privacy Policy" },
  { href: "/terms", label: "Terms of Service" },
  { href: "/data-deletion", label: "Data Deletion" },
];

export function SiteFooter() {
  return (
    <footer className="relative z-20 mt-auto border-t border-[var(--border)] bg-[#070b14]/90 backdrop-blur-md">
      <div className="mx-auto max-w-5xl px-4 py-10 md:px-6">
        <div className="grid gap-8 sm:grid-cols-2">
          <div>
            <p className="text-sm font-semibold text-[var(--foreground)]">
              {siteConfig.brandName}
            </p>
            <p className="mt-2 text-sm leading-relaxed text-[var(--muted)]">
              {siteConfig.brandName} is operated by{" "}
              <span className="text-[var(--foreground)]">
                {siteConfig.legalEntityEn}
              </span>{" "}
              ({siteConfig.legalEntityZh}).
            </p>
          </div>
          <div className="sm:text-right">
            <p className="text-xs font-medium font-mono uppercase tracking-widest text-[var(--muted)]">
              Legal
            </p>
            <ul className="mt-2 flex flex-col gap-2 sm:items-end">
              {legal.map((item) => (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className="text-sm text-[var(--accent-strong)] underline-offset-4 hover:underline"
                  >
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
        <p className="mt-8 text-xs text-[var(--muted)]">
          © {new Date().getFullYear()}{" "}
          {siteConfig.legalEntityEn}. All rights reserved.
        </p>
      </div>
    </footer>
  );
}
