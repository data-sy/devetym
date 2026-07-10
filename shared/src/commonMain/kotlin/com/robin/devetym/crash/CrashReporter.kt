package com.robin.devetym.crash

/**
 * 크래시 리포팅 seam (M9 WU-4 · D1: Sentry). **플랫폼 seam 분리** — commonMain은 Sentry를 참조하지 않는다.
 *
 * ⚠️ **왜 commonMain 단일 KMP 배선이 아닌가(2026-07-10 실측)**: `io.sentry:sentry-kotlin-multiplatform`을
 * commonMain에 넣으면 iOS 네이티브 **테스트 실행 링크**(`:shared:iosSimulatorArm64Test`)가 Sentry Cocoa
 * 프레임워크를 못 찾아 깨진다(`ld: framework 'Sentry' not found`) — 이 프로젝트는 cocoapods 없이 XcodeGen+SKIE
 * 정적 프레임워크로 iOS를 빌드하기 때문. 정적 앱-프레임워크 링크는 `-lsqlite3`처럼 통과하나, 테스트 **실행파일**
 * 링크는 심볼을 완전 해석해야 한다. → seam을 분리해 iosMain에 Sentry 참조를 두지 않고, iOS 네이티브 크래시
 * 배선은 Swift/SPM 층(WU-11 Xcode)에 맡긴다. commonMain 단일 KMP 배선(cocoapods 도입)은 출시 후 백로그(Later).
 *
 * **방침 정합(§4 "크래시 진단 최소 수집")**: actual은 PII를 붙이지 않고(스택트레이스·기기 모델·OS 버전 등 최소
 * 진단 정보만) 전송한다. 애널리틱스(Firebase)와 별개 — 애널리틱스는 계속 미수집.
 */
expect object CrashReporter {
    /**
     * 앱 진입 시 **1회** 호출(`initKoin` 최상단 — 초기화 이후 조기 크래시부터 포착). `dsn`이 null/blank면
     * 초기화하지 않는다(멱등: 이미 초기화됐어도 재진입 무시). DSN은 플랫폼 셸이 주입(Android=BuildConfig).
     */
    fun init(dsn: String?)

    /** 배선 실증용 — 캡처 경로 성립 확인(수동 테스트 크래시). 미초기화 시 안전하게 무시. */
    fun captureTestMessage(message: String)

    /** 잡힌 예외를 진단 리포트로 전송. */
    fun capture(throwable: Throwable)
}
