/**
 * Single source of truth for public-facing copy.
 * If your business license uses a different English name, update `legalEntityEn`
 * here and re-run `npm run build` so Privacy / Terms / footer stay consistent.
 */
export const siteConfig = {
  siteUrl: "https://hookedshorts.com",
  brandName: "Hooked Shorts",
  /** Must match TikTok Developer & legal documents; align with license if different. */
  legalEntityEn:
    "Suzhou Jingyu Heyao Network Information Technology Co., Ltd.",
  legalEntityZh: "苏州景屿和曜网络信息科技有限公司",
  contactEmail: "admin@hookedshorts.com",
  jurisdiction: "People's Republic of China",
  /** Public site purpose (no channel-specific tech on this page). */
  productLine:
    "We promote and distribute short-form dramatic series in international markets. This public website is for company information, policies, and platform verification only—not for hosting or streaming our full episode catalogue.",
} as const;

/** English labels for drama types we work with (editorial; adjust to match your catalogue). */
export const dramaGenres = [
  "Romance & emotional arcs",
  "Revenge & comeback stories",
  "CEO, workplace & modern city drama",
  "Fantasy, period & costume series",
  "Suspense, mystery & thriller",
  "Family & slice-of-life storytelling",
] as const;
