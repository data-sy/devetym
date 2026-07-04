import SwiftUI
import Shared

/// 얇은 iOS 셸 — shared의 Compose 진입점(MainViewController)을 SwiftUI에 호스팅(architecture §3).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
