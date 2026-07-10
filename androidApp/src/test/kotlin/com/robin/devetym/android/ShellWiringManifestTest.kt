package com.robin.devetym.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * M9 WU-10 — 셸 배선 회귀 가드.
 *
 * 첫 기동 크래시(`ClassNotFoundException`)의 오라클: `AndroidManifest`의 `android:name`(application/activity)이
 * **실 클래스로 해석되는지**. 4축 green(assembleDebug·Robolectric 그래프 테스트)은 이 미스매치를 조용히 통과했다 —
 * manifest가 `.DevEtymApp`(→ `com.robin.devetym.DevEtymApp`)을 가리켰으나 실 클래스는 `com.robin.devetym.android.DevEtymApp`
 * 라 기동 즉시 프로세스 즉사(M9 Android 에뮬 스모크서 포착·수정). iOS `-lsqlite3` 링크 갭과 동류(실행 오라클 부재).
 *
 * 순수 JVM 테스트(Robolectric·앱 부팅 불요): 소스 매니페스트 파싱 → namespace로 FQCN 해석 →
 * `Class.forName`(로드만, 초기화 X) + 상위형 검증 + 실 셸 클래스 크로스체크.
 */
class ShellWiringManifestTest {
    // androidApp `build.gradle.kts`의 namespace. 상대 `.Foo`는 이 base로 해석된다.
    private val namespace = "com.robin.devetym"

    private fun manifestFile(): File {
        val candidates = listOf(
            "src/main/AndroidManifest.xml",            // CWD = androidApp 모듈 디렉터리
            "androidApp/src/main/AndroidManifest.xml", // CWD = repo 루트
        )
        // 테스트 실행 CWD가 모듈/루트 어느 쪽이든 견고하게: user.dir부터 위로 훑어 첫 존재 후보 반환.
        var dir: File? = File(System.getProperty("user.dir") ?: ".")
        while (dir != null) {
            for (c in candidates) {
                val f = File(dir, c)
                if (f.exists()) return f
            }
            dir = dir.parentFile
        }
        throw AssertionError("AndroidManifest.xml 미발견 (user.dir=${System.getProperty("user.dir")})")
    }

    /** android:name → FQCN. `.Foo`=상대(namespace 접두), 단순명=namespace.단순명, FQCN은 그대로. */
    private fun resolve(name: String): String = when {
        name.startsWith(".") -> namespace + name
        !name.contains(".") -> "$namespace.$name"
        else -> name
    }

    @Test
    fun manifest_names_resolve_to_real_shell_classes() {
        // namespace-unaware 파싱 → 속성 리터럴명 "android:name"으로 직접 조회(NS 인식 함정 회피).
        val doc = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = false }
            .newDocumentBuilder()
            .parse(manifestFile())

        // application
        val app = doc.getElementsByTagName("application").item(0) as Element
        val appName = app.getAttribute("android:name")
        assertTrue("application android:name 누락", appName.isNotBlank())
        val appFqcn = resolve(appName)
        val appClass = Class.forName(appFqcn, false, javaClass.classLoader)
        assertTrue(
            "$appFqcn 은 android.app.Application 서브클래스가 아님",
            android.app.Application::class.java.isAssignableFrom(appClass),
        )
        assertEquals("manifest application이 실 셸 클래스와 불일치", DevEtymApp::class.java.name, appFqcn)

        // activity(전수) — 각 android:name이 실 Activity로 로드돼야 함.
        val activities = doc.getElementsByTagName("activity")
        assertTrue("activity 선언 없음", activities.length > 0)
        for (i in 0 until activities.length) {
            val el = activities.item(i) as Element
            val actName = el.getAttribute("android:name")
            assertTrue("activity android:name 누락", actName.isNotBlank())
            val fqcn = resolve(actName)
            val cls = Class.forName(fqcn, false, javaClass.classLoader)
            assertTrue(
                "$fqcn 은 android.app.Activity 서브클래스가 아님",
                android.app.Activity::class.java.isAssignableFrom(cls),
            )
        }

        // 런처 액티비티가 실 MainActivity로 해석되는지 크로스체크(양방향 결속).
        val referencesMainActivity = (0 until activities.length).any {
            resolve((activities.item(it) as Element).getAttribute("android:name")) == MainActivity::class.java.name
        }
        assertTrue("manifest가 실 MainActivity를 참조하지 않음", referencesMainActivity)
    }
}
