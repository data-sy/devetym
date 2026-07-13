package com.robin.devetym.di

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * M9-후속 셸 재설계 §2-D — mailto URL 조립 네이티브 실측. 종전 수동 encode(공백·개행만)는 한글 subject에서
 * `NSURL.URLWithString` nil → 조용한 no-op였다(실기기 3-5 "문의·오류 제보 전멸"의 코드상 확정 결함).
 * `NSURLComponents` 조립이 한글을 percent-encoding해 nil이 아님을 고정한다.
 */
class MailtoUrlTest {

    @Test
    fun test_한글제목_mailto가_nil아님() {
        val url = mailtoUrl("oddmuffinstudio@gmail.com", "DevEtym 문의", "")
        assertNotNull(url, "한글 subject mailto가 nil — §2-D 인코딩 결함 재발")
        val abs = url.absoluteString ?: ""
        assertTrue(abs.startsWith("mailto:oddmuffinstudio@gmail.com?"), "mailto 수신자 형식 붕괴: $abs")
        assertFalse(abs.contains("DevEtym 문의"), "한글이 percent-encoding 안 됨(raw 노출): $abs")
        assertTrue(abs.contains("subject="), "subject 쿼리 부재: $abs")
    }

    @Test
    fun test_한글본문_개행포함_mailto가_nil아님() {
        val url = mailtoUrl("a@b.com", "DevEtym 오류 제보: mutex", "본문 첫 줄\n둘째 줄")
        assertNotNull(url, "개행 포함 한글 body mailto가 nil")
    }
}
