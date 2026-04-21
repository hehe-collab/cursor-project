import type { Metadata } from "next";
import { dramaGenres } from "@/lib/site";

export const metadata: Metadata = {
  title: "Services",
  description:
    "Short drama line-ups and performance marketing for overseas audiences.",
};

export default function ServicesPage() {
  return (
    <div>
      <h1 className="text-3xl font-bold tracking-tight text-[var(--foreground)]">
        Services
      </h1>
      <p className="mt-3 max-w-3xl text-base leading-relaxed text-[var(--muted)]">
        We focus on <strong>promotion, media buying, and creative</strong> for
        short-form dramatic series. Scope may vary by title, market, and
        campaign.
      </p>

      <h2 className="mt-10 font-mono text-sm font-medium uppercase tracking-wider text-[var(--accent-strong)]">
        Drama types in our catalogue
      </h2>
      <p className="mt-2 max-w-3xl text-sm text-[var(--muted)]">
        The following are representative labels we use when describing our
        line-up to partners and audiences.
      </p>
      <ul className="mt-4 max-w-3xl list-disc space-y-2 pl-5 text-[var(--muted)]">
        {dramaGenres.map((g) => (
          <li key={g}>
            <span className="text-[var(--foreground)]">{g}</span>
          </li>
        ))}
      </ul>

      <ul className="mt-8 max-w-3xl list-disc space-y-4 pl-5 text-[var(--muted)]">
        <li>
          <span className="font-semibold text-[var(--foreground)]">
            User acquisition &amp; media:
          </span>{" "}
          planning, testing, and reporting in advertising accounts we operate
          for approved short drama campaigns, in line with each
          network&apos;s policies.
        </li>
        <li>
          <span className="font-semibold text-[var(--foreground)]">
            Creative &amp; packaging:
          </span>{" "}
          short-form ad concepts, hooks, and localisation; iteration where
          performance data is available.
        </li>
        <li>
          <span className="font-semibold text-[var(--foreground)]">
            Measurement:
          </span>{" "}
          campaign and traffic metrics using tools permitted by the platforms
          we work with and our agreed measurement setup.
        </li>
        <li>
          <span className="font-semibold text-[var(--foreground)]">
            Public company information:
          </span>{" "}
          this website lists our legal entity, policies, and contact details
          for ad networks, marketplaces, and business verification.
        </li>
      </ul>
      <h2 className="mt-12 text-xl font-semibold text-[var(--foreground)]">
        TikTok for Business (advertising &amp; marketing)
      </h2>
      <p className="mt-3 max-w-3xl text-base leading-relaxed text-[var(--muted)]">
        We use{" "}
        <strong className="text-[var(--foreground)]">
          TikTok for Business
        </strong>{" "}
        in the same way as other advertisers: to run, manage, and optimise
        user-acquisition and performance marketing for our{" "}
        <strong>short drama offers</strong>—through Business Center, Ads
        Manager, and the commercial tools available in the regions where we
        operate, subject to TikTok&apos;s terms and advertising policies. Full
        episodes are not hosted on this corporate site.
      </p>
      <p className="mt-3 max-w-3xl text-sm leading-relaxed text-[var(--muted)]">
        We are not a TikTok product reseller, and we do not claim a special
        commercial partnership. If a platform integration (for example,
        Business API) is required only for the minimum workflow the platform
        allows, we will use it for that same advertising and reporting purpose
        and within TikTok&apos;s rules.
      </p>

      <p className="mt-8 max-w-3xl text-sm text-[var(--muted)]">
        We do not sell paid subscriptions on this website. This page does not
        promise ad approval, chart ranking, or revenue.
      </p>
    </div>
  );
}
