import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // startKoin 배선(architecture §4.7). Android의 DevEtymApp.onCreate와 대응.
        AppModuleKt.doInitKoin()
        // ⚠️ 종전 iosReviewPresenter(StoreKit 2) 주입은 제거됐다(#19) — 리뷰 요청이 프롬프트 API가 아니라
        // App Store 딥링크가 되면서 Swift 전용 API가 필요 없어졌고, seam이 Kotlin 안에서 닫힌다.
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
