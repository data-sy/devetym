// @ts-check
import { defineConfig } from "astro/config";
import cloudflare from "@astrojs/cloudflare";
import sitemap from "@astrojs/sitemap";
import { SITE_URL } from "./src/config/site.ts";

/**
 * ADR-0009: Astro + React 아일랜드 / Cloudflare.
 * ADR-0013(제안): 용어 페이지는 SSG, 그 위에 **조회 전용** SSR 폴백.
 *   → 그래서 output은 `static`이 아니라 `server` + 페이지별 prerender다.
 *     `static`으로 두면 SSR 폴백 라우트를 얹을 자리가 아예 없다.
 *
 * ⚠️ 기본은 prerender=true (정적). SSR이 필요한 라우트만 자기 파일에서
 *    `export const prerender = false`로 뒤집는다.
 */
export default defineConfig({
  site: SITE_URL,
  output: "server",
  adapter: cloudflare({ imageService: "compile" }),
  // robots.txt가 /sitemap-index.xml을 선언하므로 **실제로 존재해야 한다**.
  // 없는 사이트맵을 Search Console에 제출하면 오류로 남는다(2026-08-25 404 실측 후 추가).
  integrations: [sitemap()],
  build: { inlineStylesheets: "auto" },
  // ⚠️ prefetch를 켜지 않는다. ADR-0009의 Positive는 *"650페이지가 JS 없이 즉시 렌더"*인데
  //    prefetchAll은 **모든 페이지**에 클라이언트 스크립트를 심는다(실측 2.25KB/gzip 1KB).
  //    검색 유입은 한 페이지만 보고 이탈하는 비중이 높아 선반입의 이득도 작다.
  //    필요해지면 W1b에서 링크 단위 opt-in(`data-astro-prefetch`)으로 켠다.
});
