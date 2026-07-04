import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // startKoin 배선(architecture §4.7). Android의 DevEtymApp.onCreate와 대응.
        AppModuleKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
