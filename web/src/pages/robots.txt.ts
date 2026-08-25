import type { APIRoute } from "astro";
import { IS_CANONICAL_HOST, absoluteUrl } from "../config/site";

export const prerender = true;

/**
 * ⚠️ 미리보기 호스트에서는 전면 차단한다. `<meta robots noindex>`만으로는
 *    이미 색인된 URL을 되돌리기 어렵고, 애초에 크롤링을 안 하는 편이 낫다.
 *    도메인이 붙으면(W0b) 자동으로 허용으로 바뀐다 — 손댈 곳 없다.
 */
export const GET: APIRoute = () => {
  const body = IS_CANONICAL_HOST
    ? `User-agent: *\nAllow: /\n\nSitemap: ${absoluteUrl("/sitemap-index.xml")}\n`
    : `# 미리보기 배포 — 색인 금지 (W0a)\nUser-agent: *\nDisallow: /\n`;

  return new Response(body, {
    headers: { "content-type": "text/plain; charset=utf-8" },
  });
};
