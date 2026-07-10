# DevEtym 시스템 프롬프트 v2 — 핸드오프

> v2 직교성 검증 측정(`results/2026-05-15_2250/`) 결과를 바탕으로 한 구체적 변경 지시.
> Claude Code에게 이 문서 + 현재 시스템 프롬프트 파일 + sanity test 파일 + `Scripts/prompt-probe/metrics.py`를 함께 주고 반영 요청.
>
> **MVP 단계 product 결정**: 품질 향상보다 latency·cost 안정성을 우선. 측정으로 강하게 검증된 변경 둘(alias_strict + null guard)만 채택하고, closing·selfcheck는 launch 후 retention 데이터를 보고 v3 라운드에서 재검토.

---

## §1. 변경 요약

| # | 변경 | 우선순위 | 캐시 영향 |
|---|---|---|---|
| 1 | `aliases` 항목에 한정 수식어 부정 예시 추가 (alias_strict 처방) | 필수 | 캐시 무효화 1회 (#2와 묶음) |
| 2 | `[도구 선택]` 섹션 끝에 프로그래밍 예약어 보호 룰 추가 (null guard) | 필수 | 위와 함께 묶음 |
| 3 | `Scripts/prompt-probe/metrics.py`의 `aliases_has_qualifier` 함수 false positive 수정 | 권장 | 해당 없음 (test 인프라) |

**묶음 원칙**: 1·2는 시스템 프롬프트 문자열을 수정하므로 같은 커밋·릴리즈에 묶어 캐시 무효화 비용을 1회로 제한. 3은 production prompt와 무관하므로 *별도 커밋*으로 분리해 bisect·revert 시 cache 회계 헷갈림 방지.

**명시적 비채택**: `closing` 처방·`selfcheck` 처방·thinking budget 조정·페르소나 정교화·few-shot 5번째 예시 — 각각 §5에서 근거 설명.

---

## §2. 각 변경별 상세

### 변경 1: `aliases` 한정 수식어 부정 예시 추가

**대상 파일**: `DevEtym/DevEtym/Services/ClaudeAPIService.swift` 의 `systemPrompt` 문자열

**위치**: `[필드별 작성 기준 — return_term_entry]` 섹션의 `aliases` 항목. *"정의·번역·상위 개념은 포함하지 않는다"* 줄 다음, *"보통 1~3개"* 줄 앞에 한 줄 삽입.

**변경 전**:
```
- aliases: 동일 개념을 지칭하는 대체 표기의 배열. 허용:
  (1) 한글 음차 (예: "뮤텍스", "데몬")
  (2) 약어의 풀네임 (예: "Java Persistence API")
  (3) 철자 변이 (예: "demon")
  정의·번역·상위 개념은 포함하지 않는다 (예: "소프트웨어 결함"은 alias가 아님).
  보통 1~3개. 적절한 대체 표기가 없으면 빈 배열을 반환한다.
```

**변경 후**:
```
- aliases: 동일 개념을 지칭하는 대체 표기의 배열. 허용:
  (1) 한글 음차 (예: "뮤텍스", "데몬")
  (2) 약어의 풀네임 (예: "Java Persistence API")
  (3) 철자 변이 (예: "demon")
  정의·번역·상위 개념은 포함하지 않는다 (예: "소프트웨어 결함"은 alias가 아님).
  또한 기본 용어가 약어가 아니면 한정 수식어를 붙인 변형(예: "HTTP cookie", "웹 쿠키", "Java thread")은 alias가 아니다. (2)는 약어 → 풀네임 1:1 대응에만 적용된다.
  보통 1~3개. 적절한 대체 표기가 없으면 빈 배열을 반환한다.
```

**근거**:
v2 측정에서 `alias_strict` 처방의 효과가 `cookie` keyword 4/4 cell에서 일관 검증됨 — 켜면 `aliases: ["쿠키"]`로 깔끔, 끄면 `["쿠키", "HTTP 쿠키", "웹 쿠키"]` 같은 한정 수식어 변형 혼입. 메인 이펙트 측면에서 `aliases_qualifier` 비율 baseline 2/10 → `alias_strict` 단독 1/9로 감소. cell 간 횡단으로도 효과 일관(closing × alias_strict, selfcheck × alias_strict, 셋 다 — 셋 모두 cookie의 한정 수식어 제거됨).

**예상 부작용 / 회피책**:
v2 측정에서 `null` keyword가 `alias_strict` 켜진 셀에서 `not_dev_term`으로 오분류됨. 후속 데이터 분석 결과 이는 *alias_strict 단독 부작용이 아니라 prompt 길이 증가에 따른 priors 시프트*로 재해석됨(같은 회귀가 `closing` 단독 셀에서도 발생). 변경 2(null guard)로 직접 보정. §3 차단 조건에서 검증.

---

### 변경 2: `[도구 선택]` 섹션에 프로그래밍 예약어 보호 룰 추가

**대상 파일**: 변경 1과 동일

**위치**: `[도구 선택 — 매우 중요]` 섹션의 마지막 bullet *"입력이 개발 용어의 오타로 추정되면 ..."* 다음에 새 bullet 추가.

**변경 전**:
```
[도구 선택 — 매우 중요]
- 당신은 모든 입력에 대해 반드시 세 도구 중 정확히 하나를 호출하여 응답해야 합니다. 일반 텍스트 응답은 절대 허용되지 않습니다.
- 입력이 개발 용어로 판단되면 return_term_entry 도구를 호출하여 각 필드를 채우세요.
- 입력이 개발 용어가 아니면 return_not_dev_term 도구를 호출하세요.
- 입력이 개발 용어의 오타로 추정되면 return_possible_typo 도구를 호출하고 suggestion에 올바른 용어를 넣으세요.
```

**변경 후**:
```
[도구 선택 — 매우 중요]
- 당신은 모든 입력에 대해 반드시 세 도구 중 정확히 하나를 호출하여 응답해야 합니다. 일반 텍스트 응답은 절대 허용되지 않습니다.
- 입력이 개발 용어로 판단되면 return_term_entry 도구를 호출하여 각 필드를 채우세요.
- 입력이 개발 용어가 아니면 return_not_dev_term 도구를 호출하세요.
- 입력이 개발 용어의 오타로 추정되면 return_possible_typo 도구를 호출하고 suggestion에 올바른 용어를 넣으세요.
- 입력 문자열이 'null', 'undefined', 'void', 'None', 'nil', 'NaN' 같은 프로그래밍 예약어인 경우, 빈 입력으로 해석하지 말고 return_term_entry로 처리하세요.
```

**근거**:
v2 측정에서 `null` keyword의 분기 정확도가 cell 8개 중 5개에서 실패함:
- ✗ closing, alias_strict, closing__alias_strict, selfcheck__alias_strict, closing__selfcheck__alias_strict
- ✓ baseline, selfcheck, closing__selfcheck

실패 패턴은 raw thinking 분석에서 *"null/empty message"·"null/empty input"* 같은 "빈 입력 해석"이 공통. 즉 모델이 시스템 prompt가 길어질수록 `null` 토큰을 *값 부재 시그널*로 미세 편향하는 priors 시프트가 원인. selfcheck 단독은 thinking 단계에서 *"Wait — null could actually be a development term"* 같은 자기검수로 우연히 복구하지만, MVP에서 selfcheck를 채택하지 않으므로 prompt 본문에 *priors 직접 보정 룰*을 박는 것이 일차 방어.

`null`은 Tony Hoare의 "billion-dollar mistake"로 유명한 핵심 개념어로 사전 사용자 검색 빈도가 매우 높을 것으로 추정 — 회귀를 production에 그대로 박는 것은 무시 불가.

**예상 부작용 / 회피책**:
- 6개 예약어 목록은 흔한 케이스 위주로 선택. 누락 가능 어휘(`empty`, `default`, `bottom` 등)는 *일반 개발 용어 분기*가 이미 잘 처리하므로 별도 명시 불필요. 추후 production에서 회귀 발견 시 목록 확장.
- 룰이 너무 강해 *실제로 비개발어인 사용자 입력*("null이라는 게 뭐예요?" 같은 자연어 질문)을 잘못 잡을 위험은 거의 없음 — DevEtym 입력 UI는 단일 keyword만 받음.

---

### 변경 3: `aliases_has_qualifier` 메트릭 false positive 수정

**대상 파일**: `Scripts/prompt-probe/metrics.py`

**위치**: 파일 상단의 함수 정의 영역. `aliases_has_qualifier` 함수 위에 `_is_acronym_expansion` 헬퍼 추가하고, 기존 함수 시그니처·본문 변경.

**변경 전**:
```python
def aliases_has_qualifier(aliases: list[str]) -> bool:
    """aliases 배열에 한정 수식어 prefix 변형이 있는지 검출.

    예: ['쿠키', 'HTTP cookie', '웹 쿠키'] → True (HTTP, 웹 prefix 검출)
        ['뮤텍스', 'mutual exclusion'] → False
    """
    if not aliases:
        return False

    for alias in aliases:
        for q in QUALIFIER_PREFIXES:
            if alias.startswith(q):
                return True
    return False
```

**변경 후**:
```python
def _is_acronym_expansion(alias: str, keyword: str) -> bool:
    """alias가 keyword(약어)의 영어 풀네임인지 판단.

    예: alias='Java Persistence API', keyword='jpa' → True (J·P·A 매칭)
        alias='HTTP cookie', keyword='cookie' → False (단어 첫 글자 H·c ≠ c·o·o·k·i·e)
    """
    words = alias.strip().split()
    keyword_lower = keyword.strip().lower()
    if len(words) != len(keyword_lower) or len(words) < 2:
        return False
    return all(
        w and w[0].isascii() and w[0].isalpha() and w[0].lower() == c
        for w, c in zip(words, keyword_lower)
    )


def aliases_has_qualifier(aliases: list[str], keyword: str) -> bool:
    """aliases 배열에 한정 수식어 prefix 변형이 있는지 검출.

    영어 약어 풀네임은 v1 alias 룰 (2)에 부합하므로 검출 제외.
    예: ['쿠키', 'HTTP cookie', '웹 쿠키'] → True
        ['Java Persistence API', '자바 영속성 API'] → False (전자는 약어 풀네임)
        ['뮤텍스', 'mutual exclusion'] → False
    """
    if not aliases:
        return False
    for alias in aliases:
        if _is_acronym_expansion(alias, keyword):
            continue
        for q in QUALIFIER_PREFIXES:
            if alias.startswith(q):
                return True
    return False
```

**호출부 갱신 필요**: `compute_metrics` 함수에서 `aliases_has_qualifier(aliases)` 호출을 `aliases_has_qualifier(aliases, expected["keyword"])`로 변경. 같은 파일 안의 변경.

**근거**:
v2 측정에서 `jpa` keyword의 `alias_strict` 단독 cell이 `aliases: Java Persistence API | 자바 영속성 API`로 적합한 출력을 냈음에도 `qualifier=True`로 검출됨. 원인은 "Java "가 `QUALIFIER_PREFIXES`에 포함되어 풀네임 형태도 prefix 매칭에 걸리는 것. 이는 alias_strict 처방의 실패가 아니라 *메트릭 함수의 한계*. v3 측정 신뢰도에 직결되므로 fix 필요.

**알려진 한계 (fix 후에도 남는 케이스)**: 한글 풀네임 형태(예: "자바 영속성 API")는 이 fix로는 여전히 검출됨. 이는 alias_strict 룰의 *진정한 미세 갭*(번역 형태를 alias_strict 부정 예시가 명시적으로 잡지 못함)이므로 v3 카드로 보존.

**예상 부작용**: 시그니처 변경으로 다른 호출 지점 있으면 break. 현재 `metrics.py` 안에서만 호출되므로 영향 범위 좁음.

---

## §3. 반영 후 스모크 테스트 — **production push 차단 조건**

**⚠️ 다음 검증을 통과하기 전에는 production push 금지.**

### §3.1 (필수) 1-cell probe 재측정

변경 1·2 반영 후 즉시 동일 probe 코드(`Scripts/prompt-probe/probe_prompt.py`)로 새 baseline에 해당하는 단일 cell을 측정한다. cell은 *v2 production prompt 그 자체* — 즉 alias_strict + null guard 둘 다 켜진 prompt.

**측정 사양**:
- cell 수: 1 (v2 production prompt)
- keyword: v2 측정과 동일한 15개 (in_shot 4 + out_of_shot 6 + branch_check 5)
- 비용: ~15 호출, 약 2~3분, 약 $0.10~0.20
- 비교 baseline: v2 측정 결과의 `baseline` cell, `alias_strict` 단독 cell

**구현 방법**: `Scripts/prompt-probe/prompts/build.py`의 `CELL_CONFIGS`를 임시 수정해 `(False, False, True)` 한 줄만 두고, `components.py`에 null guard 본문 룰을 추가한 상태로 실행. 또는 별도 `probe_v2_acceptance.py` 스크립트 작성.

### §3.2 통과 기준 (모두 만족 필요)

| 메트릭 | 기준 | v2 측정 baseline 대비 |
|---|---|---|
| `null` 분기 | `term_entry` ✓ | baseline 동일 |
| `null` qualifier | False | baseline 동일 |
| 기타 branch_check 4개 | 모두 baseline과 동일 분기 | apple/lunch → not_dev_term, mutext/redus/semafore → possible_typo |
| `cookie` qualifier | False | baseline `True` → fix |
| oos_naming 평균 | 280~310 사이 | baseline 293 부근 유지 |
| latency 평균 | ≤ 8.5s | baseline 8s 부근 유지 |
| in_shot 4개 회귀 | naming 길이·aliases가 baseline 대비 큰 변동 없음 | mutex·jpa·daemon·bug 출력 사람 검증 |

**한 항목이라도 실패 시 production push 차단.** §3.5 fallback 트리로.

### §3.3 raw 응답 사람 검증

자동 메트릭과 별개로 다음 3개 raw 응답을 사람이 직접 읽어 품질 회귀 없음 확인:

- `null` raw — namingReason이 Hoare/billion-dollar mistake 같은 핵심 콘텐츠를 담는지
- `cookie` raw — aliases가 `["쿠키"]`로 깔끔한지 (한정 수식어 미혼입)
- `jpa` raw — aliases가 약어 풀네임을 정상 보유하는지 (메트릭 fix와 별개)

---

## §3.5 Fallback 옵션 — §3 차단 조건 실패 시

**우선순위 1 (null guard 미동작 시)**: null guard 문구를 더 강한 부정형으로 재작성.
```
- 입력 문자열이 'null', 'undefined', 'void', 'None', 'nil', 'NaN' 같은 프로그래밍 예약어인 경우, **절대 빈 입력으로 해석하지 말고** 반드시 return_term_entry로 처리하세요.
```
재측정 후 다시 §3 차단 조건 검증.

**우선순위 2 (강화해도 안 되면)**: alias_strict 보류, null guard만 단독 채택. 즉 변경 2만 반영하고 변경 1을 v3로 미룸. *qualifier 회귀는 받아들이되 null 회귀는 안 받아들임* — null이 사용자 검색 빈도·인지도 측면에서 훨씬 critical.

**우선순위 3 (마지막 안전망)**: v2 통째로 보류, v1 production 상태 유지. selfcheck 단독을 v3 라운드의 1순위 카드로 격상 (selfcheck 단독은 v2 측정에서 null 15/15, oos_under_270 3/6, latency 12.8s — quality 최저선 확보. MVP 13s 부담은 그때 다시 평가).

---

## §4. 반영 순서 — 커밋 분리

**커밋 1 (production prompt — 캐시 무효화 1회)**:
- 변경 1: aliases 부정 예시 추가
- 변경 2: null guard 추가
- 캐시 무효화 비용 동시 흡수
- 메시지 예: `feat: add alias_strict rule and null guard to system prompt`

**커밋 2 (test 인프라 — 캐시 무관)**:
- 변경 3: `metrics.py`의 `aliases_has_qualifier` fix + `_is_acronym_expansion` 헬퍼 추가
- 커밋 1과 분리하는 이유: production prompt 변경(cache 무효화 1회 묶음)과 test 인프라 변경을 같은 커밋에 묶으면 future bisect·revert 시 cache 회계 헷갈림
- 메시지 예: `fix: exclude acronym expansions from qualifier detection in metrics.py`

**커밋 3 (가능하면 — §3 차단 조건 검증 결과 기록)**:
- v2 acceptance probe 결과(`results/{run_id}/`)를 commit으로 남김
- 메시지 예: `chore: add v2 acceptance probe results`

**순서**:
1. 커밋 2 먼저 (메트릭 fix를 acceptance probe가 활용해야 정확한 비교 baseline)
2. 커밋 1 (production prompt 변경)
3. §3 acceptance probe 실행
4. 통과 시 커밋 3 → production deploy
5. 실패 시 §3.5 fallback로 이동, 통과 후 deploy

---

## §5. 명시적으로 하지 않는 것

### `closing` 처방 채택 안 함 — v3 카드
v2 측정에서 `closing` 단독은 oos_naming 평균 293 → 347, 최대 311 → 427로 *역효과* 확인. 마지막 문장을 *제거*하지 않고 *새 정보로 채워 추가*하는 패턴 발견. `closing__selfcheck` 세트로만 의미 있는 시너지(6/6 perfect length) — selfcheck 없이는 채택 가치 없음. selfcheck 비채택과 함께 v3 카드로 보존.

### `selfcheck` 처방 채택 안 함 — v3 카드
v2 측정에서 quality는 명확히 개선되나(분기 15/15, oos_under_270 3/6) latency 8s → 13s 증가. MVP 단계 product 결정으로 보류. 근거:
- **누적 좌절 임계**: v1 production이 이미 8s로 사용자 인내 임계 안쪽. +5초 증가가 임계를 넘김. MVP 첫 진입 사용자는 quality-aware 단계 이전.
- **비용 부담**: selfcheck 셀의 output_tokens가 baseline의 2~3배(~500 → ~1500). 사용자 1만명 × 5검색 가정 시 호출당 $0.005 차이만 잡아도 ~$250/월. 매출 0 MVP 단계에서 무시 못 함.
- **재검토 트리거**: launch 후 *북마크/재방문 빈도* 및 *AI 응답 만족도* 데이터로 closing+selfcheck의 quality 향상이 누적 가치를 만드는지 검증한 뒤 v3에서 도입.

### selfcheck step 1·3 보강 — N/A
selfcheck 자체 비채택이므로 보강도 N/A. v3에서 selfcheck 도입할 때 함께 결정.

### Thinking budget 조정 — v4 카드
v2 측정에서 selfcheck 셀의 thinking_chars 700~900 (limit 2000의 35~45%). budget을 1000으로 줄여도 효과 미미 가능. 직교성 검증과 함께 측정해야 하므로 v4로 보존.

### Few-shot 5번째 예시 추가 — v3 카드
null guard를 본문 룰로 처리하기로 결정. 5번째 예시는 검증되지 않은 효과 + 캐시 비용 + sanity test 변경 부담. 현재 4개로 충분.

### 페르소나 정교화 — v3 카드
v2 라운드 시작 전 합의대로 보류. spec이 이미 구체적이라 ROI 거의 0, 잘못 뾰족하면 톤 가이드와 충돌·잡음 증가 위험.

### 한글 풀네임 형태 alias 처리 — v3 카드
"자바 영속성 API" 같은 한글 풀네임/번역 형태는 v1 alias 룰의 (2)에 명시되지 않은 회색지대. v2 alias_strict 처방의 부정 예시도 이를 잡지 못함. 측정 데이터로 본 빈도가 낮고 사용자 영향 불명확하므로 v3 데이터 보고 결정.

### 메트릭 인프라 추가 회귀 테스트 — v3 카드
이번 fix는 jpa 케이스에 한정. v3 측정 전 더 광범위한 false positive·false negative 검증이 필요할 수 있으나, v2 acceptance에 필요한 수준은 본 fix로 충분.

---

## §6. 캐시 무효화 비용 회계

- 커밋 1로 시스템 프롬프트 변경 → 첫 production 호출에서 1회 cache_create (~$0.02 추정)
- 이후 모든 호출은 cache_read로 정상화
- v2 acceptance probe(§3) 실행 시 cell 1개의 첫 호출이 cache_create, 나머지 14개는 cache_read — 총 cache 비용 ~$0.02
- 총 v2 도입 cache 비용: ~$0.04 (1회성)

v1 라운드와 마찬가지로 *변경을 한 커밋에 묶는 것*으로 cache 무효화 1회 비용에 흡수.
