import SwiftUI
import shared

/// Pixel-art hourglass card. Tap to start/stop, long-press to reset.
struct HourglassView: View {
    let timerState: TimerState
    let sandProgress: Double   // 0.0 = empty, 1.0 = full
    let onTap: () -> Void
    let onLongPress: () -> Void

    private var isRunning: Bool { timerState is TimerStateRunning }
    private var isFinished: Bool { timerState is TimerStateFinished }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(red: 0.08, green: 0.14, blue: 0.22))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(glowColor.opacity(0.4), lineWidth: 1)
                )
                .shadow(color: glowColor.opacity(isRunning ? 0.3 : 0.1), radius: 16)

            VStack(spacing: 20) {
                HourglassShape(progress: sandProgress, isFinished: isFinished)
                    .frame(width: 120, height: 160)

                if isRunning {
                    Text("TAP TO STOP")
                        .font(.system(size: 9, design: .monospaced))
                        .foregroundColor(Color(red: 0.49, green: 0.78, blue: 0.89).opacity(0.6))
                } else if isFinished {
                    Text("TAP TO RESET")
                        .font(.system(size: 9, design: .monospaced))
                        .foregroundColor(Color(red: 0.49, green: 0.78, blue: 0.89).opacity(0.6))
                } else {
                    Text("TAP TO START")
                        .font(.system(size: 9, design: .monospaced))
                        .foregroundColor(Color(red: 0.49, green: 0.78, blue: 0.89).opacity(0.6))
                }
            }
            .padding(24)
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
        .onLongPressGesture(minimumDuration: 0.5) { onLongPress() }
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint(isRunning ? "Long press to reset" : "Tap to start timer")
        .accessibilityAddTraits(.isButton)
    }

    private var glowColor: Color {
        isFinished
            ? Color(red: 1.0, green: 0.85, blue: 0.3)
            : Color(red: 0.49, green: 0.78, blue: 0.89)
    }

    private var accessibilityLabel: String {
        if isRunning { return "Timer running" }
        if isFinished { return "Timer complete" }
        return "Timer idle"
    }
}

/// Pure SwiftUI hourglass shape drawn with paths.
private struct HourglassShape: View {
    let progress: Double   // 0–1, how much sand has fallen
    let isFinished: Bool

    var body: some View {
        Canvas { context, size in
            let w = size.width
            let h = size.height
            let mid = h / 2

            // Outer hourglass outline
            var outline = Path()
            outline.move(to: CGPoint(x: 0, y: 0))
            outline.addLine(to: CGPoint(x: w, y: 0))
            outline.addLine(to: CGPoint(x: w / 2 + 4, y: mid - 2))
            outline.addLine(to: CGPoint(x: w, y: h))
            outline.addLine(to: CGPoint(x: 0, y: h))
            outline.addLine(to: CGPoint(x: w / 2 - 4, y: mid + 2))
            outline.closePath()

            context.fill(outline, with: .color(Color(red: 0.13, green: 0.22, blue: 0.33)))
            context.stroke(outline, with: .color(Color(red: 0.49, green: 0.78, blue: 0.89).opacity(0.5)), lineWidth: 1.5)

            // Top chamber — remaining sand (drains as progress increases)
            let topRemaining = max(0, 1 - progress)
            let topH = (mid - 8) * topRemaining
            var topSand = Path()
            let topY = mid - 8 - topH
            topSand.move(to: CGPoint(x: 4, y: topY))
            topSand.addLine(to: CGPoint(x: w - 4, y: topY))
            topSand.addLine(to: CGPoint(x: w / 2 + 2, y: mid - 4))
            topSand.addLine(to: CGPoint(x: w / 2 - 2, y: mid - 4))
            topSand.closePath()
            context.fill(topSand, with: .color(sandColor))

            // Bottom chamber — collected sand (fills as progress increases)
            let botH = (mid - 8) * progress
            var botSand = Path()
            botSand.move(to: CGPoint(x: w / 2 - 2, y: mid + 4))
            botSand.addLine(to: CGPoint(x: w / 2 + 2, y: mid + 4))
            botSand.addLine(to: CGPoint(x: w - 4, y: h - 4))
            botSand.addLine(to: CGPoint(x: 4, y: h - 4))
            // Only fill up to botH from bottom
            let fillTop = h - 4 - botH
            botSand = Path()
            botSand.addRect(CGRect(x: 4, y: fillTop, width: w - 8, height: botH))
            // Clip to hourglass bottom half
            var botClip = Path()
            botClip.move(to: CGPoint(x: w / 2 - 2, y: mid + 4))
            botClip.addLine(to: CGPoint(x: w / 2 + 2, y: mid + 4))
            botClip.addLine(to: CGPoint(x: w - 4, y: h - 4))
            botClip.addLine(to: CGPoint(x: 4, y: h - 4))
            botClip.closePath()
            context.clip(to: botClip)
            context.fill(botSand, with: .color(sandColor))
        }
    }

    private var sandColor: Color {
        isFinished
            ? Color(red: 1.0, green: 0.85, blue: 0.3)
            : Color(red: 0.49, green: 0.78, blue: 0.89)
    }
}
