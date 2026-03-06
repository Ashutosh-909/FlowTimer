import SwiftUI

/// Pixel-styled sheet for picking a focus duration (5–120 minutes, in 5-min steps).
struct DurationPickerView: View {
    let currentMinutes: Int
    let onConfirm: (Int) -> Void
    let onDismiss: () -> Void

    @State private var selectedMinutes: Int

    init(currentMinutes: Int, onConfirm: @escaping (Int) -> Void, onDismiss: @escaping () -> Void) {
        self.currentMinutes = currentMinutes
        self.onConfirm = onConfirm
        self.onDismiss = onDismiss
        _selectedMinutes = State(initialValue: currentMinutes)
    }

    private let steps = stride(from: 5, through: 120, by: 5).map { $0 }

    var body: some View {
        ZStack {
            Color(red: 0.051, green: 0.106, blue: 0.165).ignoresSafeArea()

            VStack(spacing: 24) {
                Text("FOCUS DURATION")
                    .font(.system(size: 12, design: .monospaced).bold())
                    .foregroundColor(.white)

                Picker("Duration", selection: $selectedMinutes) {
                    ForEach(steps, id: \.self) { min in
                        Text("\(min) MIN")
                            .font(.system(size: 14, design: .monospaced))
                            .tag(min)
                    }
                }
                .pickerStyle(.wheel)
                .frame(height: 160)

                HStack(spacing: 16) {
                    Button(action: onDismiss) {
                        Text("CANCEL")
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(Color(red: 0.49, green: 0.78, blue: 0.89).opacity(0.6))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.white.opacity(0.2), lineWidth: 1)
                            )
                    }

                    Button(action: { onConfirm(selectedMinutes) }) {
                        Text("CONFIRM")
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color(red: 0.49, green: 0.78, blue: 0.89).opacity(0.25))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color(red: 0.49, green: 0.78, blue: 0.89), lineWidth: 1)
                            )
                            .cornerRadius(12)
                    }
                }
                .padding(.horizontal, 24)
            }
            .padding(.top, 32)
        }
        .presentationDetents([.medium])
        .preferredColorScheme(.dark)
    }
}
