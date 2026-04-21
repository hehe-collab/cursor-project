import type { Metadata } from "next";
import { siteConfig } from "@/lib/site";

export const metadata: Metadata = {
  title: "About us",
  description: `Who operates ${siteConfig.brandName} and how we work with short drama internationally.`,
};

export default function AboutPage() {
  return (
    <div>
      <h1 className="text-3xl font-bold tracking-tight text-[var(--foreground)]">
        About {siteConfig.brandName}
      </h1>
      <p className="mt-2 text-sm text-[var(--muted)]">
        {siteConfig.legalEntityEn} &middot; {siteConfig.legalEntityZh}
      </p>
      <div className="prose-docs mt-8 max-w-3xl">
        <h2>Who we are</h2>
        <p>
          <strong>{siteConfig.brandName}</strong> is a brand we use in public
          for short-form dramatic series and related media work. It is operated
          by <strong>{siteConfig.legalEntityEn}</strong> (
          {siteConfig.legalEntityZh}), a company incorporated in the{" "}
          {siteConfig.jurisdiction} with a registered office in Suzhou.
        </p>
        <h2>What this website is for</h2>
        <p>{siteConfig.productLine}</p>
        <h2>What we focus on</h2>
        <p>
          We promote curated short drama line-ups and help audiences find them
          through marketing and media channels in target overseas regions,
          under each region&apos;s relevant advertising and content rules. We do
          not provide financial or legal advice, and we do not guarantee
          business results.
        </p>
        <h2>Regulatory and compliance</h2>
        <p>
          We work within platform rules, advertising standards, and applicable
          law. Requirements vary by product, market, and partner. If you are
          unsure, consult qualified counsel in your market.
        </p>
      </div>
    </div>
  );
}
