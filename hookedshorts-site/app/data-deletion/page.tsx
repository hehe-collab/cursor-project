import type { Metadata } from "next";
import Link from "next/link";
import { Prose } from "@/components/Prose";
import { siteConfig } from "@/lib/site";

export const metadata: Metadata = {
  title: "Data Deletion",
  description: `How to request deletion of data related to ${siteConfig.brandName} and the public website.`,
};

export default function DataDeletionPage() {
  const updated = "2026-04-22";

  return (
    <div>
      <h1 className="text-3xl font-bold tracking-tight text-[var(--foreground)]">
        Data Deletion Instructions
      </h1>
      <p className="mt-2 text-sm text-[var(--muted)]">Last updated: {updated}</p>
      <Prose>
        <p>
          {siteConfig.legalEntityEn} (&quot;we,&quot; &quot;us,&quot; or
          &quot;our&quot;) operates the public website at {siteConfig.siteUrl}{" "}
          under the {siteConfig.brandName} brand. This page explains how to
          request deletion of personal information in connection with this Site
          and general inquiries you send to us.
        </p>

        <h2>1. This Site has no public user accounts</h2>
        <p>
          The public Site does not offer sign-up, login, or a customer profile.
          We do not maintain a self-service &quot;delete my account&quot;
          button for the Site. If a future product or page adds accounts, we will
          update this page and the{" "}
          <Link href="/privacy">Privacy Policy</Link> accordingly.
        </p>

        <h2>2. How to request deletion of personal information</h2>
        <p>
          You may request deletion of personal information that we hold as
          described in our Privacy Policy by emailing us at:
        </p>
        <p>
          <a href={`mailto:${siteConfig.contactEmail}`}>
            {siteConfig.contactEmail}
          </a>
        </p>
        <p>Please use the subject line: &quot;Data deletion request&quot; and
          include, where possible:</p>
        <ul>
          <li>Your name and a reply email address.</li>
          <li>
            A short description of your interaction (for example, &quot;I
            previously emailed about a partnership in March&quot; or &quot;I
            am requesting deletion of my message history&quot;).
          </li>
          <li>
            The region in which you reside (if you are exercising regional
            privacy rights, such as in the EEA, UK, or U.S. states with privacy
            laws).
          </li>
        </ul>
        <p>
          We may need to verify your identity before processing your request, to
          protect the security of our systems and other individuals&apos;
          information. We will respond within a reasonable time frame as required
          by applicable law.
        </p>

        <h2>3. Information we may retain</h2>
        <p>
          We may keep certain records where the law requires or permits,
          including for security, financial reporting, and dispute resolution.
          Where we are unable to delete information for legal or technical
          reasons, we will explain the limitation, to the extent permitted by
          law.
        </p>

        <h2>4. Deletion in other products and third-party services</h2>
        <p>
          If you use another product or third-party platform that is related
          to our business but operated under separate terms, you may also need
          to use that product&apos;s help centre or the third party&apos;s
          tools to request deletion, as the case may be. This page only covers
          the public Site and email correspondence with {siteConfig.legalEntityEn}{" "}
          as described in our <Link href="/privacy">Privacy Policy</Link>.
        </p>
      </Prose>
    </div>
  );
}
