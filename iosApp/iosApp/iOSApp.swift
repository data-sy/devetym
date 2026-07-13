import SwiftUI
import StoreKit
import Shared

@main
struct iOSApp: App {
    init() {
        // startKoin 배선(architecture §4.7). Android의 DevEtymApp.onCreate와 대응.
        AppModuleKt.doInitKoin()
        // 앱 평가 프롬프트 주입 — SKStoreReviewController가 iOS 26 실기기에서 no-op이라
        // StoreKit 2(AppStore.requestReview, Swift 전용)를 seam 훅으로 넘긴다(IosSeams 참조).
        IosSeamsKt.iosReviewPresenter = {
            Task { @MainActor in
                if let scene = UIApplication.shared.connectedScenes
                    .compactMap({ $0 as? UIWindowScene })
                    .first(where: { $0.activationState == .foregroundActive }) {
                    AppStore.requestReview(in: scene)
                }
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
