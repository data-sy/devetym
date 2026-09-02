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
  // ⚠️ 사이트맵은 **SSG 집합만** 담는다(ADR-0013 Decision 6). 크롤러를 noindex 페이지로
  //    안내하지 않는다 — /search는 noindex이므로 여기서 빼야 한다. 빼지 않으면 Search Console이
  //    "사이트맵에 있는데 색인 안 됨"을 오류로 쌓는다.
  integrations: [
    sitemap({
      filter: (page) => !new URL(page).pathname.startsWith("/search"),
    }),
  ],

  // ⚠️ URL 하나에 표기 하나. 기본값(`trailingSlash: "ignore"`)이면 `/term/mutex`와
  //    `/term/mutex/`가 둘 다 200이 되어 중복 URL이 생기고, canonical과 내부 링크가
  //    어느 쪽이냐에 따라 색인이 갈린다. 슬래시 없는 쪽으로 고정한다.
  trailingSlash: "never",
  //
  // ⚠️ **`format: "file"`을 쓰면 안 된다 — 실측(2026-09-02, 전수 200 오라클이 잡았다).**
  //    그 모드는 용어 `index`(인덱스·색인 — DB 갈래)를 `dist/term/index.html`로 굽는데,
  //    정적 호스트는 그 파일을 **`/term/`의 디렉터리 인덱스**로도 해석한다. 그래서
  //    `/term/index`가 307이 되고, 사이트맵·canonical이 가리키는 URL과 실 응답이 갈린다.
  //    `directory` 모드는 `dist/term/index/index.html`이라 그 겹침이 아예 생기지 않는다.
  //    (`html_handling`을 바꿔도 해소되지 않았다 — 파일 배치가 원인이기 때문이다.)
  build: { inlineStylesheets: "auto", format: "directory" },

  vite: {
    // 번들 스냅샷(`shared/.../terms.json`)이 web/ 밖에 있다. **사본을 만들지 않기 위해**
    // 루트 밖 읽기를 연다 — 사본을 두면 ADR-0012가 막으려던 드리프트가 repo 안에서 재발한다.
    server: { fs: { allow: [".."] } },
  },
  // ⚠️ prefetch를 켜지 않는다. ADR-0009의 Positive는 *"650페이지가 JS 없이 즉시 렌더"*인데
  //    prefetchAll은 **모든 페이지**에 클라이언트 스크립트를 심는다(실측 2.25KB/gzip 1KB).
  //    검색 유입은 한 페이지만 보고 이탈하는 비중이 높아 선반입의 이득도 작다.
  //    필요해지면 W1b에서 링크 단위 opt-in(`data-astro-prefetch`)으로 켠다.
});
