import SwiftUI

/// Displays the formatted timer countdown with an optional "set duration" prompt.
struct TimerReadoutView: View {
    let formattedTime: String
    let isIdle: Bool
    let onTapSetDuration: () -> Void

    var body: some View {
        VStack(spacing: 12) {
            Text(formattedTime)
                .font(.system(size: 36, design: .monospaced).bold())
                .foregroundColor(.white)
                .monospacedDigit()
                .accessibilityLabel(formattedTime)

            if isIdle {
                Button(action: onTapSetDuration) {
                    Text("Set your focus time")
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundColor(Color(red: 0.49, green: 0.78, blue: 0.89).opacity(0.7))
                        .underline()
                }
                .accessibilityLabel("Set focus duration")
            }
        }
    }
}
