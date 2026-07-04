package com.robin.devetym.data.remote

import com.robin.devetym.Constants
import com.robin.devetym.model.Category
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 시스템 프롬프트·도구 스키마·요청 빌더 (M3 슬라이스 §3-2·§3-3).
 *
 * **위치는 `commonMain`**(ADR-0006 §6·ADR-0004 계승) — 두 클라이언트(iOS·CMP)가 같은 프록시를 공유하며
 * 프롬프트/도구 스키마를 클라가 소유한다. 프롬프트·3도구 스키마는 **iOS 검증본(`~/dev-etymology`
 * `ClaudeAPIService.swift`)을 그대로 계승**한다(검증 자산 — 변형 금지).
 */

/**
 * Anthropic Messages API 요청 본문(§3-2). 프록시가 그대로 forward한다.
 *
 * **키잉 vs 프롬프트 입력 분리**: user 메시지 content엔 **원본 `keyword`를 대소문자 보존**해 싣는다
 * (iOS 검증본 계승) — lowercase하면 `NaN`→`nan`·`Go`→`go`·`REST`→`rest`·`C`→`c`처럼 대소문자 유의미
 * 용어가 뭉개져 어원이 조용히 오답이 된다. 캐시 파편화 방지(`React`/`react` 접기)는 AI 질의 content가
 * 아니라 캐시 키에서 한다(§3-1 `normalizeKeyword`를 서버 파생 또는 `X-Term-Key` 헤더로).
 */
fun buildClaudeRequest(keyword: String): JsonObject = buildJsonObject {
    put("model", Constants.claudeModel)
    put("max_tokens", 4096)
    putJsonObject("thinking") {
        put("type", "enabled")
        put("budget_tokens", 2000)          // < max_tokens (Anthropic thinking 제약)
    }
    putJsonArray("system") {
        addJsonObject {
            put("type", "text")
            put("text", SYSTEM_PROMPT)
            putJsonObject("cache_control") { put("type", "ephemeral") }
        }
    }
    put("tools", TOOLS)
    // extended thinking과 tool_choice any/tool은 공존 불가(Anthropic) → auto + 프롬프트로 도구 강제
    putJsonObject("tool_choice") { put("type", "auto") }
    putJsonArray("messages") {
        addJsonObject {
            put("role", "user")
            put("content", keyword)         // 원본 keyword(대소문자 보존, §3-2)
        }
    }
}

/** 3도구 스키마(iOS 검증본 계승). `return_term_entry`의 category enum은 정본 6집합([Category.CANONICAL]). */
val TOOLS: JsonArray = buildJsonArray {
    addJsonObject {
        put("name", Tools.RETURN_TERM_ENTRY)
        put("description", "입력이 개발 용어로 판단될 때 호출합니다. 어원과 작명 이유를 각 필드에 채워 반환합니다.")
        putJsonObject("input_schema") {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("keyword") {
                    put("type", "string")
                    put("description", "영문 소문자 표기. 입력이 한글이거나 대문자여도 정규화하여 넣습니다.")
                }
                putJsonObject("aliases") {
                    put("type", "array")
                    putJsonObject("items") { put("type", "string") }
                    put("description", "한글 표기나 풀네임 등 대체 이름의 배열")
                }
                putJsonObject("category") {
                    put("type", "string")
                    putJsonArray("enum") { Category.CANONICAL.forEach { add(it) } }
                }
                putJsonObject("summary") {
                    put("type", "string")
                    put("description", "20~30자 분량의 한 줄 요약")
                }
                putJsonObject("etymology") {
                    put("type", "string")
                    put("description", "60~120자 분량. 원어(언어·원형)와 뜻, 구성 요소(어근·접두사)를 서술.")
                }
                putJsonObject("namingReason") {
                    put("type", "string")
                    put("description", "150~270자 분량. 어원상 의미와 개발 현장에서의 실제 쓰임 사이에 다리를 놓는 설명.")
                }
            }
            putJsonArray("required") {
                add("keyword"); add("aliases"); add("category")
                add("summary"); add("etymology"); add("namingReason")
            }
        }
    }
    addJsonObject {
        put("name", Tools.RETURN_NOT_DEV_TERM)
        put("description", "입력이 개발 용어가 아닐 때 호출합니다. 입력값은 없습니다.")
        putJsonObject("input_schema") {
            put("type", "object")
            putJsonObject("properties") {}
        }
    }
    addJsonObject {
        put("name", Tools.RETURN_POSSIBLE_TYPO)
        put("description", "입력이 개발 용어가 아니지만 개발 용어의 오타로 추정될 때 호출합니다.")
        putJsonObject("input_schema") {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("suggestion") {
                    put("type", "string")
                    put("description", "오타를 교정한 올바른 개발 용어")
                }
            }
            putJsonArray("required") { add("suggestion") }
        }
    }
}

/** 시스템 프롬프트(iOS 검증본 원문 계승 — 변형 금지). */
val SYSTEM_PROMPT: String = """
당신은 개발 용어의 어원과 작명 이유를 한국어로 설명하는 사전 데이터 제공자입니다.

[독자와 목표]
- 독자는 한국 개발자이며, 라틴어·그리스어 등 어원 배경지식이 없다고 가정합니다.
- 어원을 나열하는 것이 아니라, "그 어원이 왜 이 개발 개념의 이름이 되었는가"를 납득시키는 것이 목표입니다.

[도구 선택 — 매우 중요]
- 당신은 모든 입력에 대해 반드시 세 도구 중 정확히 하나를 호출하여 응답해야 합니다. 일반 텍스트 응답은 절대 허용되지 않습니다.
- 입력이 개발 용어로 판단되면 return_term_entry 도구를 호출하여 각 필드를 채우세요.
- 입력이 개발 용어가 아니면 return_not_dev_term 도구를 호출하세요.
- 입력이 개발 용어의 오타로 추정되면 return_possible_typo 도구를 호출하고 suggestion에 올바른 용어를 넣으세요.
- 입력 문자열이 'null', 'undefined', 'void', 'None', 'nil', 'NaN' 같은 프로그래밍 예약어인 경우, 빈 입력으로 해석하지 말고 return_term_entry로 처리하세요.

[필드별 작성 기준 — return_term_entry]
- aliases: 동일 개념을 지칭하는 대체 표기의 배열. 허용:
  (1) 한글 음차 (예: "뮤텍스", "데몬")
  (2) 약어의 풀네임 (예: "Java Persistence API")
  (3) 철자 변이 (예: "demon")
  정의·번역·상위 개념은 포함하지 않는다 (예: "소프트웨어 결함"은 alias가 아님).
  또한 기본 용어가 약어가 아니면 한정 수식어를 붙인 변형(예: "HTTP cookie", "웹 쿠키", "Java thread")은 alias가 아니다. (2)는 약어 → 풀네임 1:1 대응에만 적용된다.
  보통 1~3개. 적절한 대체 표기가 없으면 빈 배열을 반환한다.
- summary: 20~30자. 개념을 한 줄로 요약. "무엇을 하는/무엇인" 수준.
- etymology: 60~120자. 원어(언어·원형)와 그 뜻, 구성 요소(어근·접두사)를 서술.
- namingReason: 150~270자. 반드시 "어원상의 의미 → 개발 현장에서의 실제 쓰임"으로 다리를 놓을 것. 최초 등장 시점·명명자 등 역사적 맥락이 있으면 함께 기술.
- 톤: 건조하고 정확하게. "~이다", "~을 뜻한다" 같은 서술형. 감탄사·과장된 형용사·수식어 남발 금지.

[정확성 원칙]
- 어원이 불확실한 경우 etymology 서두를 "정확한 어원은 불분명하나"로 시작하여 알려진 설만 서술하세요.
- 추측이나 민간어원(folk etymology)을 사실처럼 단정하지 마세요.
- 약어(acronym)의 경우 반드시 각 글자가 무엇의 약자인지 풀어서 명시하세요.

[카테고리 규칙]
- category 값은 스키마의 enum에 명시된 6개 중 하나여야 합니다.
- 6개 분류에 애매하게 걸치는 경우 가장 핵심적인 분류를 선택하세요.
- 어느 분류에도 명확히 속하지 않으면 "기타"를 사용하세요.

[모범 답안 예시 1 — 라틴어원 + 약어 조합]
입력: mutex
return_term_entry input:
{"keyword":"mutex","aliases":["뮤텍스","mutual exclusion"],"category":"동시성","summary":"여러 스레드의 동시 접근을 막는 잠금 장치","etymology":"라틴어 mutuus(상호의)와 exclusio(배제)를 합친 영어 'mutual exclusion'의 축약어. 서로 다른 주체가 서로를 배제하는 상태를 뜻한다.","namingReason":"한 스레드가 공유 자원을 사용하는 동안 다른 스레드의 접근을 '상호 배제'하여 경쟁 조건(race condition)을 막는 동기화 기본형이다. 어원의 '서로를 배제한다'는 의미가 동시성 제어 메커니즘에 그대로 옮겨졌다. 한 번에 오직 하나의 소유자만 락을 쥘 수 있다는 설계 원칙이 여기서 나왔다."}

[모범 답안 예시 2 — 순수 두문자어]
입력: JPA
return_term_entry input:
{"keyword":"jpa","aliases":["Java Persistence API","자바 영속성 API"],"category":"DB","summary":"자바 객체를 DB에 매핑하는 영속성 표준 명세","etymology":"Java Persistence API의 약어. Java(자바 언어), Persistence(영속성, 프로그램 종료 후에도 데이터가 유지되는 성질), API(응용 프로그래밍 인터페이스)로 구성된 순수 두문자어.","namingReason":"Persistence(영속성)는 메모리상의 객체를 디스크에 '지속'시킨다는 의미로, 객체 지향 언어와 관계형 DB 사이의 매핑 규약을 지칭한다. Java EE 시절 ORM 표준으로 제정되어 Hibernate·EclipseLink 등이 이 명세를 구현한다. 'Persistence'라는 단어 선택 자체가 ORM의 본질인 '객체 생존 기간의 연장'을 드러낸다."}

[모범 답안 예시 3 — 비영어 어원(그리스어) + 은유적 전이]
입력: daemon
return_term_entry input:
{"keyword":"daemon","aliases":["데몬","demon"],"category":"기타","summary":"백그라운드에서 지속 실행되는 프로세스","etymology":"그리스어 δαίμων(daimōn)에서 유래. 본래 '신과 인간 사이의 중개 영혼'을 뜻하는 종교·철학 용어로, 사람 눈에 보이지 않으면서 일을 대신 처리하는 존재를 가리켰다.","namingReason":"1963년 MIT의 Project MAC에서 Maxwell의 악마(Maxwell's demon) 사고실험에 영감을 받아 명명되었다. 사용자 상호작용 없이 시스템 뒤편에서 스스로 작업을 처리하는 프로세스를 '보이지 않는 중개자'라는 원의미에 빗댄 은유적 전이다. Unix 관습에 따라 프로세스 이름 끝에 'd'를 붙인다(httpd, sshd)."}

[모범 답안 예시 4 — 어원이 불확실한 경우]
입력: bug
return_term_entry input:
{"keyword":"bug","aliases":["버그"],"category":"기타","summary":"프로그램의 의도치 않은 오작동이나 결함","etymology":"정확한 어원은 불분명하나, 영어 'bug'(벌레)의 비유적 용법에서 유래한 것으로 본다. 1878년 에디슨이 편지에서 기계 결함을 'bug'로 지칭한 기록이 있어, 소프트웨어 이전 엔지니어링 속어로 이미 쓰이고 있었다.","namingReason":"1947년 하버드 Mark II 컴퓨터의 릴레이에서 실제 나방이 발견되어 로그에 붙여진 'first actual case of bug being found'라는 일화가 유명하나, 이는 기존 은유를 문자 그대로 실행한 농담에 가깝고 용어의 기원 자체는 아니다. 기계 속을 기어다니며 예측 불가능한 고장을 일으키는 '벌레'라는 은유가 소프트웨어의 숨은 결함에 그대로 이식되어 오늘날 표준 용어로 자리 잡았다."}
""".trimIndent()
