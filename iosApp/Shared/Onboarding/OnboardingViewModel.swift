import Combine
import Foundation
import Shared
import UIKit

/// Презентейшен онбординга на Swift. Бизнес-логика (device-flow, поллинг) — на общих
/// use-case'ах KMP (`RequestDeviceCodeUseCase`/`PollForTokenUseCase`); UI-состояние — здесь.
@MainActor
final class OnboardingViewModel: ObservableObject {
    @Published var step: Int = 0
    @Published var userCode: String?
    @Published var verificationUri: String?
    @Published var polling: Bool = false
    @Published var error: String?

    static let stepCount = 3

    private let requestDeviceCode = UseCaseProvider.shared.requestDeviceCodeUseCase()
    private let pollForToken = UseCaseProvider.shared.pollForTokenUseCase()
    private let user = RepositoryProvider.shared.user
    private var flow: Task<Void, Never>?

    func next() {
        let target = min(step + 1, Self.stepCount - 1)
        step = target
        if target == 2 { startDeviceFlow() }
    }

    func prev() {
        step = max(0, step - 1)
        error = nil
    }

    func retry() {
        error = nil
        userCode = nil
        verificationUri = nil
        startDeviceFlow()
    }

    func cancel() {
        flow?.cancel()
        flow = nil
    }

    private func startDeviceFlow() {
        flow?.cancel()
        flow = Task { await requestAndPoll() }
    }

    private func requestAndPoll() async {
        do {
            let result = try await requestDeviceCode.invoke()
            switch onEnum(of: result) {
            case .success(let success):
                // generic-параметр RequestResult<T> стирается в ObjC-интеропе → data приходит как AnyObject?.
                guard let deviceCode = success.data as? DeviceCode else {
                    error = "Некорректный ответ сервера."
                    return
                }
                userCode = deviceCode.userCode
                verificationUri = deviceCode.verificationUri
                polling = true
                await poll(
                    code: deviceCode.code,
                    intervalSec: Int(deviceCode.interval),
                    expiresIn: Int(deviceCode.expiresIn)
                )
            case .error(let requestError):
                error = requestError.message ?? "Не удалось получить код активации."
            }
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func poll(code: String, intervalSec: Int, expiresIn: Int) async {
        let deadline = Date().addingTimeInterval(TimeInterval(expiresIn))
        while Date() < deadline {
            if Task.isCancelled { return }
            try? await Task.sleep(nanoseconds: UInt64(max(1, intervalSec)) * 1_000_000_000)
            let timestamp = Int64(Date().timeIntervalSince1970)
            do {
                let result = try await pollForToken.invoke(code: code, username: "", timestamp: timestamp)
                if case .success = onEnum(of: result) {
                    // Токены сохранены общим data-слоем (multiplatform-settings);
                    // поток авторизации переключит RootView на главный экран.
                    // Сообщаем бэку о клиенте (kino.pub device/notify) сразу после входа —
                    // best-effort: ошибка регистрации не должна мешать авторизации.
                    await registerDevice()
                    polling = false
                    return
                }
                // .error здесь — «код ещё не подтверждён», продолжаем поллинг.
            } catch {
                // Сетевой сбой одной попытки не прерывает поллинг.
            }
        }
        polling = false
        error = "Время ожидания истекло. Попробуйте снова."
    }

    private func registerDevice() async {
        let device = UIDevice.current
        _ = try? await user.registerDevice(
            title: device.model,
            hardware: Self.hardwareIdentifier(),
            software: "Filmax · \(device.systemName) \(device.systemVersion)"
        )
    }

    /// Идентификатор модели («AppleTV6,2», «iPhone14,5») — аналог `Build.DEVICE` на Android.
    private static func hardwareIdentifier() -> String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce(into: "") { partial, element in
            guard let value = element.value as? Int8, value != 0 else { return }
            partial.append(Character(UnicodeScalar(UInt8(value))))
        }
        return identifier.isEmpty ? "apple" : identifier
    }
}
