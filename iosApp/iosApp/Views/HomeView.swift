import SwiftUI
import shared

/// Main home screen — mirrors the Android gesture-driven layout.
/// No visible buttons; all interaction via tap/long-press on the hourglass.
struct HomeView: View {
    @StateObject private var observable = FlowTimerObservable()

    var body: some View {
        ZStack {
            // Background
            Color(red: 0.051, green: 0.106, blue: 0.165)
                .ignoresSafeArea()

            StarFieldView()

            VStack(spacing: 0) {
                // ── Title ──
                VStack(spacing: 4) {
                    Text("FLOW")
                        .font(.system(size: 32, design: .monospaced).bold())
                        .foregroundColor(.white)
                    Text("TIME")
                        .font(.system(size: 32, design: .monospaced).bold())
                        .foregroundColor(.white)

                    if observable.uiState.completedSessionCount > 0 {
                        Spacer().frame(height: 8)
                        let count = Int(observable.uiState.completedSessionCount)
                        Text("\(count) flow session\(count != 1 ? "s" : "")")
                            .font(.system(size: 10, design: .monospaced))
                            .foregroundColor(Color(red: 0.49, green: 0.78, blue: 0.89).opacity(0.5))
                    }
                }
                .padding(.top, 48)

                Spacer()

                // ── Hourglass ──
                HourglassView(
                    timerState: observable.uiState.timerState,
                    sandProgress: Double(observable.uiState.sandProgress),
                    onTap: observable.onTapHourglass,
                    onLongPress: observable.onLongPressReset
                )

                Spacer()

                // ── Timer readout ──
                TimerReadoutView(
                    formattedTime: observable.uiState.formattedTime,
                    isIdle: observable.uiState.isIdle,
                    onTapSetDuration: observable.onTapSetDuration
                )
                .padding(.bottom, 48)
            }
            .padding(.horizontal, 24)
        }
        .sheet(isPresented: $observable.showDurationPicker) {
            DurationPickerView(
                currentMinutes: Int(observable.uiState.durationMinutes),
                onConfirm: observable.onConfirmDuration,
                onDismiss: observable.onDismissDurationPicker
            )
        }
    }
}
