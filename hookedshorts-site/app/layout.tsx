import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { SiteHeader } from "@/components/SiteHeader";
import { SiteFooter } from "@/components/SiteFooter";
import { siteConfig } from "@/lib/site";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const base = siteConfig.siteUrl;

export const metadata: Metadata = {
  metadataBase: new URL(base),
  title: {
    default: "Hooked Shorts — Short drama growth & media",
    template: "%s | Hooked Shorts",
  },
  description:
    "Hooked Shorts is the public brand of our company. We market short-form drama line-ups for international audiences. Suzhou, China.",
  openGraph: {
    type: "website",
    locale: "en_US",
    url: base,
    siteName: siteConfig.brandName,
    title: "Hooked Shorts",
    description:
      "Short drama promotion; public site for company and compliance.",
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} min-h-dvh font-sans antialiased`}
      >
        <div className="tech-bg" aria-hidden />
        <div className="relative z-10 flex min-h-dvh flex-col">
          <SiteHeader />
          <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-10 md:px-6">
            {children}
          </main>
          <SiteFooter />
        </div>
      </body>
    </html>
  );
}
