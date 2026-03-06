import Foundation
import shared

/// Bridges the Kotlin `IosFlowTimerViewModel` to SwiftUI via `ObservableObject`.
/// Subscribes to Kotlin StateFlows using callback-based collection.
@MainActor
final class FlowTimerObservable: ObservableObject {
    @Published var uiState = TimerUiState(
        timerState: TimerState.Idle(),
        displayMillis: 25 * 60_000,
        durationMinutes: 25,
        sandProgress: 0.0,
        completedSessionCount: 0
    )
    @Published var showDurationPicker = false

    private let viewModel = IosFlowTimerViewModel()
    private var uiStateCancellable: FlowCancellable?
    private var pickerCancellable: FlowCancellable?

    init() {
        uiStateCancellable = viewModel.observeUiState { [weak self] state in
            DispatchQueue.main.async { self?.uiState = state }
        }
        pickerCancellable = viewModel.observeShowPicker { [weak self] show in
            DispatchQueue.main.async { self?.showDurationPicker = show }
        }
    }

    deinit {
        uiStateCancellable?.cancel()
        pickerCancellable?.cancel()
        viewModel.destroy()
    }

    func onTapHourglass()              { viewModel.onTapHourglass() }
    func onLongPressReset()            { viewModel.onLongPressReset() }
    func onTapSetDuration()            { viewModel.onTapSetDuration() }
    func onDismissDurationPicker()     { viewModel.onDismissDurationPicker() }
    func onConfirmDuration(_ minutes: Int) { viewModel.onConfirmDuration(minutes: Int32(minutes)) }
}
