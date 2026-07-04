package com.robin.devetym.di

import com.robin.devetym.Greeting
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** 공통 Koin 배선(architecture §4.7). 플랫폼별 구현은 platformModule에서 채운다(M0엔 없음). */
val appModule = module {
    single { Greeting() }
}

/** 양 플랫폼 진입점에서 호출하는 startKoin 골격. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(appModule)
}

/** iOS(Swift)에서 부르는 무인자 진입 — 기본인자 브릿지 없이 안전하게 호출. */
fun doInitKoin() {
    initKoin()
}
