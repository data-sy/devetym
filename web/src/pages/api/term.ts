import type { APIRoute } from "astro";

/**
 * 웹 생성 요청의 same-site 착지점 (W1b).
 *
 * ⚠️ **이 라우트가 존재하는 이유는 편의가 아니라 「브라우저당 3건」 층의 실재 여부다.**
 *    브라우저가 워커를 `workers.dev`로 직접 부르면 식별 쿠키가 서드파티 쿠키가 되어
 *    **Safari가 차단**한다 → 매 요청이 새 id를 받고 3건 한도가 사실상 사라진다
 *    (핸드오프 §5-12 · ADR-0010). 같은 등록가능도메인에서 서빙해야 그 층이 실재한다.
 *
 * ⚠️ **fetch로 프록시를 다시 부르지 않고 service binding으로 넘긴다.** 인터넷을 한 번 더
 *    타면 프록시가 보는 `CF-Connecting-IP`가 우리 워커의 것으로 바뀔 위험이 있고, 그러면
 *    **IP 15건/일 층이 전 사용자 공용 버킷 하나로 붕괴**해 15건 뒤 모든 웹 사용자가 429를
 *    받는다. service binding은 요청을 그대로 넘긴다(런타임 내부 호출 · 엣지 재처리 없음).
 *    → 배포 후 실측 항목이다. 프록시가 본 IP가 서로 다른 기기에서 다르게 잡히는지 확인할 것.
 *
 * ⚠️ **여기서 정책을 만들지 않는다.** 한도·Turnstile·CORS·캐시는 전부 프록시가 정본이고
 *    (ADR-0010·0011), 이 파일은 전달만 한다. 여기에 판정을 한 줄이라도 두면 같은 규칙이
 *    두 곳에 살게 되고, 웹 표면만 조용히 갈라진다.
 */
export const prerender = false;

/** 프록시로 넘길 때 실어 보내는 헤더. 나머지는 버린다(전달자가 늘리지 않는다). */
const FORWARD_HEADERS = [
  "content-type",
  "origin",
  "cookie",
  "x-turnstile-token",
  "cf-connecting-ip",
  "user-agent",
  "accept-language",
];

export const POST: APIRoute = async ({ request, locals }) => {
  const proxy = (locals as any)?.runtime?.env?.PROXY;
  if (!proxy) {
    // 바인딩이 없으면 **조용히 다른 경로로 새지 않는다.** 폴백으로 workers.dev를 직접 부르면
    // 위 ⚠️의 쿠키·IP 붕괴가 그대로 일어나고, 그때는 아무도 알아채지 못한다.
    return json({ error: "proxy_binding_missing" }, 503);
  }

  const headers = new Headers();
  for (const name of FORWARD_HEADERS) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }

  const upstream = await proxy.fetch(
    // 프록시는 경로를 보지 않는다(POST면 전부 같은 핸들러). 호스트는 자리표시자다.
    new Request("https://devetym-proxy.internal/", {
      method: "POST",
      headers,
      body: await request.text(),
    }),
  );

  // 응답은 상태·본문 그대로. **`Set-Cookie`를 반드시 살려서 넘긴다** — 이게 빠지면
  // 브라우저가 id를 못 받아 매 요청이 새 사용자가 되고, 3건 층이 다시 사라진다.
  const out = new Headers();
  const contentType = upstream.headers.get("content-type");
  if (contentType) out.set("content-type", contentType);
  for (const cookie of upstream.headers.getSetCookie()) {
    out.append("set-cookie", cookie);
  }
  // 생성 응답은 절대 캐시하지 않는다 — 한도 계수와 개인 식별이 걸려 있다.
  out.set("cache-control", "no-store");

  return new Response(await upstream.text(), { status: upstream.status, headers: out });
};

function json(obj: unknown, status: number): Response {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
}
