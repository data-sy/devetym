---
layout: default
title: 개발 어원 사전 (DevEtym)
permalink: /
---

# 개발 어원 사전 (DevEtym)

**개발 용어의 어원과 작명 이유를 한국어로 풀어 설명하는 사전 앱.**
단순히 뜻을 알려주는 게 아니라 *왜 그 이름이 붙었는지*를 설명해 개념 이해와 기억을 돕습니다.

> mutex, daemon, kernel, Arne Andersson tree… 매일 쓰는 이름은 어디서 왔을까?

Android · iOS 단일 코드베이스(Kotlin / Compose Multiplatform)로 만들었습니다.

## 주요 기능

- **오프라인 번들 사전** — 큐레이션한 개발 용어 650개 이상을 네트워크 없이 즉시 검색.
- **AI 생성 폴백** — 번들에 없는 용어는 AI가 어원·명명 이유·카테고리를 생성해 기기에 캐시.
- **별칭 검색** — `Arne Andersson tree` → `aa-tree`처럼 별칭으로도 바로 도달.
- **북마크 · 검색 히스토리** — 모두 **기기 안에만** 저장(개인정보를 서버로 보내지 않음).
- **라이트 / 다크 / 시스템 외관** 및 Dynamic Type(글자 크기) 지원.

## 지원 환경

| 플랫폼 | 최소 버전 |
|---|---|
| Android | 8.0 (API 26) 이상 |
| iOS | 16 이상 |

## 개인정보 보호

앱은 **개인을 식별하는 정보를 수집하지 않습니다.** 북마크·히스토리·열람 내용은 기기 안에만 저장됩니다. 번들에 없는 용어를 검색할 때에 한해, 정의 생성을 위해 검색어가 암호화되어 서버로 전송됩니다. 자세한 내용은 아래 방침을 참고하세요.

## 문서

- [개인정보 처리방침](./privacy-policy/)
- [이용약관](./terms-of-service/)

## 저장소

[github.com/data-sy/devetym](https://github.com/data-sy/devetym)

## 문의

- 이메일: oddmuffinstudio@gmail.com
