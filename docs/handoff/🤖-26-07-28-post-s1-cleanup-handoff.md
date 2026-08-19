# 🤖 S1 이후 정리 — 잔여 3건 핸드오프

> # 🟡 부분 소진 — 3건 중 1건만 끝났다 (2026-08-19 실측)
>
> | 항목 | 상태 |
> |---|---|
> | **A. 데이터면 문서 2건 정정** | ❌ **미해소** — `docs/architecture.md:250`은 여전히 *"서버 신규 구축은 `devetym-proxy`"*(미래형)이고, `cache-delivery-milestones.md` §M0·§M1에도 완료 마커가 없다. **데이터면이라 승인 게이트** — ROADMAP 백로그 `[Docs]` 항목으로도 등재했다. |
> | **B. Node 기본값 전환** | ✅ **완료** — 실측: `node -v` = v20.19.5, `nvm default` = 20. |
> | **C. 서류 정돈 트랙 착수** | ⬜ **미착수** — ROADMAP 백로그 `[Ops]`(`~/Downloads/devetym-release` 정돈)로 이월. 순서·안전선은 그 항목에 있다. |
>
> **A와 C는 아직 살아 있으므로 이 문서를 지우지 말 것.** 진행 상태 정본은 [`ROADMAP.md`](../../ROADMAP.md).

> **성격: 콜드 세션 착수용 실행 문서.** 이 문서를 읽는 세션은 **이전 대화 맥락이 없다고 가정**한다.
> 서로 **독립적인 잔여 작업 3건**이라 순서 강제가 없다. 하나만 골라 해도 되고, 전부 해도 된다.
>
> **작성**: 2026-07-28 (서버 캐시 S1 배포 직후 정리 세션).
> **진행 상태 정본은 이 문서가 아니라 [`../../ROADMAP.md`](../../ROADMAP.md)** — 충돌하면 ROADMAP을 신뢰한다.

---

## 0. 전제 확인 — 먼저 이것부터

이 문서는 **서버 캐시 S1이 배포·병합 완료된 상태**를 전제한다. 착수 전 실측한다.

```bash
# 앱: App Store 라이브 (2026-07-27 게시, Apple ID 6790429958)
# 서버: devetym-proxy 캐시 가동 중 (CACHE_DISABLED = "0")

git -C ~/devetym log --oneline -3 main          # S1 스펙·클라 테스트가 main에 있어야 함
gh -R data-sy/devetym pr list --state merged --limit 3      # #17 · #18 병합 확인
gh -R data-sy/devetym-proxy pr list --state merged --limit 2 # #3 병합 확인
```

**병합이 안 돼 있으면** 아래 3건 중 **C(서류 정돈)만 착수 금지**다 — 병합 전에 문서를 지우면 PR 본문·ROADMAP 링크가 깨진다. A·B는 병합과 무관하게 진행 가능.

> ⚠️ **Node 22 필수** — `~/devetym-proxy`에서 무언가 실행할 일이 생기면 먼저 `nvm use`.
> wrangler 4.114·miniflare 4가 Node ≥22를 강제한다. 이게 B 작업의 배경이기도 하다.

---

## A. 데이터면 문서 2건 정정 `[사람 승인 → AI]`

`/refresh-ops-docs`가 stale을 발견했으나 **데이터면은 승인 게이트**라 손대지 않고 남긴 것이다.
사람 승인을 받은 뒤 고친다. 승인 없이 고치지 말 것.

### A-1. `docs/architecture.md:250`

현재 서술이 **미래형**이다 — 서버 캐시는 2026-07-28 이미 구축·배포됐다.

```
4. **네트워킹** — Ktor 클라이언트 + Claude 요청/응답(tool_use 파싱). **프록시 = read-through 캐시**
   (서버 D1→API·write-back, 클라엔 투명; ADR-0006). 서버 신규 구축은 `devetym-proxy`.
                                                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 미래형
```

**제안**: "서버 신규 구축은 `devetym-proxy`" → **"서버 구현은 `devetym-proxy`(S1 슬라이스로 2026-07-28 가동 — D1 `entries`/`aliases`, 정본 키=AI keyword, 부정 분기 30일 TTL)"**.
근거 정본은 [`docs/specs/server-m0-m1-cache-read-through-draft.md`](../specs/server-m0-m1-cache-read-through-draft.md).

### A-2. `docs/cache-delivery-milestones.md` §M0 · §M1

두 마일스톤이 **구현 완료됐는데 완료 표시가 없다.** 이 문서는 불변식(INV-1~13)의 정본이라
"아직 안 지은 것"으로 읽히면 다음 세션이 중복 착수한다.

**제안**: §M0(term key 정규화·D1 스키마)·§M1(Worker read-through)에 완료 마커 + S1 스펙 링크 추가.
후속 슬라이스(M2 single-flight · M3 품질 게이트 · M5 승격 잡)는 **미착수 그대로 유지**.

⚠️ **INV-1~13 자체는 건드리지 말 것.** 제약을 소비하지 생성하지 않는다. 특히 **INV-8(temperature 강제)은
S1에서 의도적으로 분리돼 여전히 미달성**이며, 이걸 "달성"으로 바꾸면 안 된다.

---

## B. Node 기본값 전환 `[사람 확인 → 실행]`

**증상**: 새 셸에서 `~/devetym-proxy` 작업을 하면 wrangler가 실행을 거부한다.

```
Wrangler requires at least Node.js v22.0.0. You are using v20.19.5.
```

**현재 상태**: nvm에 22.23.1이 설치돼 있으나 `default`가 여전히 **20**이다.
`devetym-proxy`에 `.nvmrc`(=22)와 `engines.node >=22`가 있어 `nvm use`로는 맞춰지지만,
매 세션 수동이라 잊기 쉽다.

```bash
nvm alias default          # 현재 → 20 (v20.19.5)
nvm ls                     # 22.23.1 설치돼 있는지
```

**선행 확인이 필요해 자동 실행하지 않았다** — 이 머신의 **다른 프로젝트가 Node 20에 묶여 있는지** 봐야 한다.
`~/devetym`(Gradle/KMP)은 Node를 안 쓰므로 무관하다.

- 다른 Node 프로젝트가 없거나 22 호환이면 → `nvm alias default 22`
- 20이 필요한 프로젝트가 있으면 → **바꾸지 말고** 그 프로젝트에도 `.nvmrc`를 두고 현행 유지

---

## C. 서류 정돈 트랙 착수 `[사람 발의]`

**이 항목의 정본은 별도 문서다** — 여기 중복하지 않는다:
📄 [`26-07-14-doc-pruning-backlog.md`](26-07-14-doc-pruning-backlog.md) (헌장·인벤토리·안전선·절차)

작업 브랜치 `chore/doc-pruning`. 진입 문구는 그 문서 상단에 있다.

### 2026-07-28 시점에 새로 늘어난 정돈 대상

원 인벤토리(2026-07-14) 이후 **역할이 끝난 문서가 3건 더 생겼다.** 셋 다 상단에 `⛔ 역할 종료` 배너가 붙어 있다:

| 문서 | 상태 |
|---|---|
| [`🤖-26-07-27-server-cache-s1-handoff.md`](🤖-26-07-27-server-cache-s1-handoff.md) | 비준 완료로 역할 종료 |
| [`🤖-26-07-27-server-cache-s1-impl-handoff.md`](🤖-26-07-27-server-cache-s1-impl-handoff.md) | 구현·배포 완료로 역할 종료. 본문에 **실측이 반증한 문장 1건**(NBSP 트림)이 취소선+정정 박스로 남아 있다 |
| [`26-07-13-ios-submission-handoff.md`](26-07-13-ios-submission-handoff.md) | 게시 완료로 역할 종료 |

**삭제 전 선행 조건**: 위 문서들을 참조하는 **ROADMAP·README 링크를 먼저 정리**해야 한다.
지금 ROADMAP 백로그 항목 I와 M9 항목이 이들을 가리키고 있다.

⚠️ 그리고 **이 문서(`🤖-26-07-28-post-s1-cleanup-handoff.md`) 자신도** A·B가 끝나면 정돈 대상이 된다.

---

## 안전 규율 (이 세션이 하지 않을 것)

- **거버넌스 자동 수정 금지** — 페르소나·design-prompt·검수 기준·`CLAUDE.md`. 발견해도 제안만
- **데이터면(specs·ADR·architecture·cache-delivery-milestones) 임의 수정 금지** — A는 **승인받은 뒤에만**
- **INV-1~13 수정 금지** — 제약은 소비 대상이지 생성 대상이 아니다
- **브랜치 삭제 금지** — 병합 후에도 보존
- **push·머지는 사람 지시로만**
- **서버 배포·`--remote` 마이그레이션·Anthropic 실호출 금지** — S1은 이미 배포됐고 추가 작업 없음

---

## 참고 — S1 이후 관측 지점 (작업 아님)

정리 작업은 아니지만 알아 둘 것. `usage_log`의 `cache_hit` 비율이 서버 캐시의 성과 지표다.

```bash
cd ~/devetym-proxy && nvm use
npx wrangler d1 execute devetym-usage --remote \
  --command "SELECT cache_hit, COUNT(*) AS n FROM usage_log GROUP BY cache_hit"
npx wrangler d1 execute devetym-cache --remote \
  --command "SELECT term_key, hit_count FROM entries ORDER BY hit_count DESC LIMIT 20"
```

부정 분기(`not_dev_term`·`possible_typo`) 재생성이 잦으면 `NEGATIVE_TTL_DAYS`(현재 30) 조정 근거가 된다
— 상수 1개 + 재배포로 끝나도록 격리해 뒀다(스펙 §9-2).
