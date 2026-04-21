import Link from "next/link";
import { siteConfig, dramaGenres } from "@/lib/site";

export default function Home() {
  return (
    <div>
      <section className="panel relative overflow-hidden p-8 md:p-12">
        <div
          className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-[var(--accent)] opacity-[0.08] blur-3xl"
          aria-hidden
        />
        <p className="badge-tech">Short drama &amp; media distribution</p>
        <h1 className="mt-4 max-w-2xl text-3xl font-bold tracking-tight text-[var(--foreground)] md:text-4xl md:leading-tight">
          <span className="bg-gradient-to-r from-[var(--foreground)] to-[var(--accent-strong)] bg-clip-text text-transparent">
            Short-form series
          </span>{" "}
          for international audiences
        </h1>
        <p className="mt-5 max-w-2xl text-base leading-relaxed text-[var(--muted)]">
          {siteConfig.brandName} is the public brand of {siteConfig.legalEntityEn}.
          We build promotion and media programmes around curated short drama
          line-ups in target overseas regions. This site is for company
          information and compliance, not a full episode library.
        </p>
        <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
          <Link
            href="/services"
            className="inline-flex h-11 items-center justify-center rounded-lg bg-gradient-to-r from-cyan-500 to-sky-600 px-6 text-sm font-semibold text-slate-950 shadow-lg shadow-cyan-500/20 transition hover:brightness-110"
          >
            What we do
          </Link>
          <Link
            href="/contact"
            className="inline-flex h-11 items-center justify-center rounded-lg border border-[var(--border)] bg-[var(--surface)] px-6 text-sm font-semibold text-[var(--foreground)] transition hover:border-[var(--accent)]/50"
          >
            Contact
          </Link>
        </div>
      </section>

      <section className="mt-10">
        <h2 className="badge-tech text-[var(--muted)]">Genres we work with</h2>
        <p className="mt-2 max-w-2xl text-sm text-[var(--muted)]">
          Representative categories across our current promotional catalogue
          (illustrative; titles and availability vary by market).
        </p>
        <ul className="mt-6 flex flex-wrap gap-2.5">
          {dramaGenres.map((g) => (
            <li
              key={g}
              className="rounded-full border border-[var(--border)] bg-[var(--surface)] px-3.5 py-1.5 text-sm text-[var(--foreground)]"
            >
              {g}
            </li>
          ))}
        </ul>
      </section>

      <section className="mt-12 grid gap-4 md:grid-cols-3">
        {[
          {
            title: "Catalogue & reach",
            text: "Campaigns and packaging are planned around our short drama line-ups and how audiences discover new series in each market.",
          },
          {
            title: "Media & performance",
            text: "Data-informed planning for creative, reach, and reporting across approved channels, without promising specific ad outcomes.",
          },
          {
            title: "Trust & verification",
            text: "Registered entity, public policies, and a contact point for ad platforms, partners, and compliance reviews.",
          },
        ].map((item) => (
          <div key={item.title} className="panel p-6 transition duration-200">
            <h2 className="font-mono text-xs font-medium uppercase tracking-wider text-[var(--accent-strong)]">
              {item.title}
            </h2>
            <p className="mt-3 text-sm leading-relaxed text-[var(--muted)]">
              {item.text}
            </p>
          </div>
        ))}
      </section>
    </div>
  );
}
