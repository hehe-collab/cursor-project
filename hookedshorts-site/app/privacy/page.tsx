import type { Metadata } from "next";
import Link from "next/link";
import { Prose } from "@/components/Prose";
import { siteConfig } from "@/lib/site";

export const metadata: Metadata = {
  title: "Privacy Policy",
  description: `How ${siteConfig.brandName} collects and uses information on hookedshorts.com.`,
};

export default function PrivacyPage() {
  const updated = "2026-04-22";

  return (
    <div>
      <h1 className="text-3xl font-bold tracking-tight text-[var(--foreground)]">
        Privacy Policy
      </h1>
      <p className="mt-2 text-sm text-[var(--muted)]">Last updated: {updated}</p>
      <Prose>
        <p>
          This Privacy Policy describes how{" "}
          <strong>{siteConfig.legalEntityEn}</strong> (&quot;we,&quot;
          &quot;us,&quot; or &quot;our&quot;) processes information when you
          visit the public website at{" "}
          <Link href="/">{siteConfig.siteUrl.replace("https://", "")}</Link>{" "}
          (the &quot;Site&quot;). Our consumer brand for this Site is{" "}
          <strong>{siteConfig.brandName}</strong>.
        </p>
        <p>
          If the English name of our company on your records or business license
          differs from the name above, please rely on the official name shown
          on your contract or invoice. You may also contact us at{" "}
          <a href={`mailto:${siteConfig.contactEmail}`}>
            {siteConfig.contactEmail}
          </a>{" "}
          to confirm the correct legal entity name.
        </p>

        <h2>1. Who we are</h2>
        <p>
          <strong>Controller:</strong> {siteConfig.legalEntityEn} (
          {siteConfig.legalEntityZh}
          ).<br />
          <strong>Contact (privacy):</strong>{" "}
          <a href={`mailto:${siteConfig.contactEmail}`}>
            {siteConfig.contactEmail}
          </a>
        </p>

        <h2>2. What this Site does</h2>
        <p>
          The Site is an informational website about our company and services.
          It does not require you to create an account. We do not sell products
          directly through this Site.
        </p>

        <h2>3. Information we may collect</h2>
        <p>Depending on how you use the Site, we may process:</p>
        <ul>
          <li>
            <strong>Technical and usage data:</strong> such as IP address,
            browser type, device type, referring page, pages viewed, and
            approximate region derived from IP (for security, fraud prevention,
            and basic analytics).
          </li>
          <li>
            <strong>Information you send us:</strong> if you email us, we process
            the contents of your message and your email address to respond.
          </li>
        </ul>

        <h2>4. Purposes and legal bases (EEA/UK users)</h2>
        <p>
          If you are in the European Economic Area or the United Kingdom, we
          rely on the following legal bases where applicable:
        </p>
        <ul>
          <li>
            <strong>Legitimate interests</strong> in operating, securing, and
            improving the Site, understanding aggregate traffic, and responding
            to inquiries.
          </li>
          <li>
            <strong>Consent</strong> where required for non-essential cookies or
            similar technologies (if we enable them; see your browser controls).
          </li>
        </ul>

        <h2>5. Cookies and similar technologies</h2>
        <p>
          We may use essential cookies required for the Site to function. If we
          deploy optional analytics or marketing tools in the future, we will
          update this policy and, where required, request consent. You can
          control cookies through your browser settings.
        </p>

        <h2>6. Sharing of information</h2>
        <p>
          We use trusted infrastructure and service providers to host and
          protect the Site (for example, hosting, DNS, and security providers).
          They may process data on our instructions and under contractual
          obligations. We do not sell your personal information.
        </p>

        <h2>7. International transfers</h2>
        <p>
          Our business is based in {siteConfig.jurisdiction}. If you access the
          Site from other regions, your information may be processed in China
          and other countries where our providers operate. We implement
          appropriate safeguards as required by applicable law.
        </p>

        <h2>8. Retention</h2>
        <p>
          We keep server and security logs for as long as needed for security
          and legal compliance, typically on a rolling basis. Emails you send
          are retained for as long as needed to address your request and
          maintain business records, unless a longer period is required by law.
        </p>

        <h2>9. Your rights</h2>
        <p>
          Depending on where you live, you may have the right to access,
          correct, delete, or restrict certain processing, or to object. To
          exercise your rights, contact us at{" "}
          <a href={`mailto:${siteConfig.contactEmail}`}>
            {siteConfig.contactEmail}
          </a>
          . We may need to verify your identity. You may also lodge a complaint
          with a supervisory authority in your country.
        </p>

        <h2>10. California residents (U.S.)</h2>
        <p>
          If you are a California resident, you may have additional rights under
          applicable state privacy laws, including the right to know, delete,
          and opt out of certain &quot;sharing&quot; or &quot;sale&quot; of
          personal information as those terms are defined by law. We do not
          believe we &quot;sell&quot; or &quot;share&quot; personal information
          for cross-context behavioural advertising through this Site as
          currently configured. You may still contact us with requests at the
          email address above.
        </p>

        <h2>11. Children</h2>
        <p>
          The Site is not directed to children, and we do not knowingly collect
          personal information from children.
        </p>

        <h2>12. Changes</h2>
        <p>
          We may update this Privacy Policy from time to time. The &quot;Last
          updated&quot; date at the top will change when we do. Continued use
          of the Site after changes means you accept the updated policy, to the
          extent permitted by law.
        </p>
      </Prose>
    </div>
  );
}
