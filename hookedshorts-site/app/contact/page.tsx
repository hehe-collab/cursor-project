import type { Metadata } from "next";
import { siteConfig } from "@/lib/site";

export const metadata: Metadata = {
  title: "Contact",
  description: `Reach ${siteConfig.brandName} and ${siteConfig.legalEntityEn}.`,
};

export default function ContactPage() {
  return (
    <div>
      <h1 className="text-3xl font-bold tracking-tight text-[var(--foreground)]">
        Contact
      </h1>
      <p className="mt-3 max-w-3xl text-base leading-relaxed text-[var(--muted)]">
        For general business inquiries, privacy requests, data-deletion
        instructions, and ad/platform verification, you can reach the team
        behind {siteConfig.brandName}. This is not a viewer helpdesk for
        individual episodes; if we offer a separate consumer support channel,
        use that for playback or billing issues.
      </p>
      <div className="panel mt-8 max-w-md p-6">
        <p className="badge-tech text-[var(--muted)]">Email</p>
        <p className="mt-2 text-lg">
          <a
            className="font-semibold text-[var(--accent-strong)] underline decoration-cyan-500/30 underline-offset-2 hover:decoration-cyan-400"
            href={`mailto:${siteConfig.contactEmail}`}
          >
            {siteConfig.contactEmail}
          </a>
        </p>
        <p className="mt-6 text-xs font-mono font-medium uppercase tracking-widest text-[var(--muted)]">
          Legal entity
        </p>
        <p className="mt-2 text-sm leading-relaxed text-[var(--muted)]">
          {siteConfig.legalEntityEn}
          <br />
          {siteConfig.legalEntityZh}
        </p>
        <p className="mt-4 text-sm text-[var(--muted)]">
          For privacy or deletion requests, include enough context for us to
          verify you (e.g. region, and how you contacted us). We may need more
          detail to process requests safely.
        </p>
      </div>
    </div>
  );
}
