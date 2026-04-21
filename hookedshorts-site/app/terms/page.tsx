import type { Metadata } from "next";
import Link from "next/link";
import { Prose } from "@/components/Prose";
import { siteConfig } from "@/lib/site";

export const metadata: Metadata = {
  title: "Terms of Service",
  description: `Terms governing use of the ${siteConfig.brandName} public website.`,
};

export default function TermsPage() {
  const updated = "2026-04-22";

  return (
    <div>
      <h1 className="text-3xl font-bold tracking-tight text-[var(--foreground)]">
        Terms of Service
      </h1>
      <p className="mt-2 text-sm text-[var(--muted)]">Last updated: {updated}</p>
      <Prose>
        <p>
          These Terms of Service (&quot;Terms&quot;) govern your use of the
          public website located at {siteConfig.siteUrl} (the
          &quot;Site&quot;), which is provided by{" "}
          <strong>{siteConfig.legalEntityEn}</strong> (
          {siteConfig.legalEntityZh}
          ) under the {siteConfig.brandName} brand. By using the Site, you agree
          to these Terms. If you do not agree, do not use the Site.
        </p>

        <h2>1. The Site and our services</h2>
        <p>
          The Site provides general information about us and our marketing and
          growth services. A separate contract governs any paid engagement, if
          applicable. The Site does not, by itself, create a client
          relationship.
        </p>

        <h2>2. Eligibility</h2>
        <p>
          You must be at least the age of digital consent in your jurisdiction to
          use the Site. If you use the Site on behalf of an organisation, you
          represent that you are authorised to do so.
        </p>

        <h2>3. Acceptable use</h2>
        <p>You agree not to:</p>
        <ul>
          <li>
            Use the Site in violation of any law, third-party right, or
            platform policy.
          </li>
          <li>
            Attempt to probe, scan, or test the vulnerability of the Site, or
            breach security or authentication measures, without our prior written
            authorisation.
          </li>
          <li>
            Interfere with the normal operation of the Site, including by
            introducing malware or excessive automated traffic.
          </li>
          <li>
            Misrepresent your identity or your affiliation with any person or
            entity.
          </li>
        </ul>

        <h2>4. Intellectual property</h2>
        <p>
          The content on the Site (text, design, and branding, excluding
          third-party marks) is owned by {siteConfig.legalEntityEn} or its
          licensors. You may view and print a reasonable number of copies for
          your internal, non-commercial reference. Other uses require our prior
          written permission.
        </p>

        <h2>5. Disclaimers</h2>
        <p>
          THE SITE AND ITS CONTENT ARE PROVIDED &quot;AS IS&quot; AND
          &quot;AS AVAILABLE,&quot; WITHOUT WARRANTIES OF ANY KIND, WHETHER
          EXPRESS, IMPLIED, OR STATUTORY, TO THE FULLEST EXTENT PERMITTED BY
          LAW, INCLUDING ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
          A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. WE DO NOT WARRANT THAT
          THE SITE WILL BE UNINTERRUPTED, ERROR-FREE, OR FREE OF HARMFUL
          COMPONENTS.
        </p>
        <p>
          The Site may contain links to third-party sites. We are not
          responsible for those sites. Your use of third-party services is
          subject to their terms and policies.
        </p>

        <h2>6. Limitation of liability</h2>
        <p>
          TO THE FULLEST EXTENT PERMITTED BY LAW, IN NO EVENT WILL{" "}
          {siteConfig.legalEntityEn} OR ITS OFFICERS, DIRECTORS, EMPLOYEES, OR
          AFFILIATES BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL,
          CONSEQUENTIAL, OR PUNITIVE DAMAGES, OR ANY LOSS OF PROFITS, DATA, OR
          GOODWILL, ARISING FROM OR RELATED TO THE SITE, WHETHER IN CONTRACT,
          TORT, OR OTHERWISE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
          DAMAGES.
        </p>
        <p>
          TO THE FULLEST EXTENT PERMITTED BY LAW, OUR AGGREGATE LIABILITY FOR
          CLAIMS ARISING FROM OR RELATING TO THE SITE SHALL NOT EXCEED THE
          GREATER OF (A) US $100, OR (B) THE AMOUNT YOU PAID US, IF ANY, FOR
          ACCESS TO THE SITE IN THE SIX (6) MONTHS PRECEDING THE CLAIM. SOME
          JURISDICTIONS DO NOT ALLOW CERTAIN LIMITATIONS; IN THOSE
          JURISDICTIONS, OUR LIABILITY IS LIMITED TO THE MAXIMUM EXTENT
          PERMITTED BY LAW.
        </p>

        <h2>7. Indemnity</h2>
        <p>
          You will defend, indemnify, and hold harmless {siteConfig.legalEntityEn}
          and its affiliates from and against any claims, damages, losses, and
          expenses (including reasonable legal fees) arising from your use of
          the Site, your content, or your violation of these Terms, to the extent
          permitted by law.
        </p>

        <h2>8. Governing law and dispute resolution</h2>
        <p>
          These Terms are governed by the laws of the People&apos;s Republic of
          China, without regard to conflict-of-law rules, subject to
          non-waivable consumer protections in your home jurisdiction, where
          applicable. The courts in Suzhou, Jiangsu, China, shall have exclusive
          jurisdiction for disputes related to the Site, except where
          applicable law requires otherwise. Before filing a claim, you agree
          to contact us in good faith at{" "}
          <a href={`mailto:${siteConfig.contactEmail}`}>
            {siteConfig.contactEmail}
          </a>{" "}
          to try to resolve the matter.
        </p>

        <h2>9. Changes</h2>
        <p>
          We may modify these Terms from time to time. We will update the
          &quot;Last updated&quot; date and, where required, provide additional
          notice. Your continued use of the Site after changes become effective
          constitutes your acceptance of the revised Terms, to the extent
          permitted by law.
        </p>

        <h2>10. Contact</h2>
        <p>
          Questions about these Terms:{" "}
          <a href={`mailto:${siteConfig.contactEmail}`}>
            {siteConfig.contactEmail}
          </a>
          .<br />
          For privacy, see our{" "}
          <Link href="/privacy">Privacy Policy</Link>.
        </p>
      </Prose>
    </div>
  );
}
