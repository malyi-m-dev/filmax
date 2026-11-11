import Combine
import Foundation
import Shared

/// Вход на домашний сервер Jellyfin (iOS и tvOS): поиск серверов в сети, адрес вручную, затем
/// имя и пароль или Quick Connect — код подтверждают в Jellyfin на телефоне, и пароль пультом
/// набирать не нужно. Логика сети и сессии — в общем `HomeServerRepository`.
@MainActor
final class HomeServerViewModel: ObservableObject {
    enum Step { case servers, credentials, quickConnect }

    @Published var step: Step = .servers
    @Published var discovered: [HomeServer] = []
    @Published var searching = false
    @Published var address = ""
    @Published var server: HomeServer?
    @Published var username = ""
    @Published var password = ""
    @Published var quickCode: String?
    @Published var busy = false
    @Published var error: String?

    private let repository = RepositoryProvider.shared.homeServer
    private let sources = RepositoryProvider.shared.sources
    private var quickConnect: Task<Void, Never>?

    /// Серверы в домашней сети (широковещательный поиск UDP 7359).
    func discover() async {
        searching = true
        discovered = (try? await repository.discover()) ?? []
        searching = false
    }

    func choose(_ found: HomeServer) async {
        await connect(found.address)
    }

    /// Адрес без схемы дополняется `http://` и портом 8096 — это решает общий слой.
    func connectToAddress() async {
        await connect(address.trimmingCharacters(in: .whitespaces))
    }

    func signIn() async {
        guard let server else { return }
        busy = true
        error = nil
        let ok = await succeeded { try await self.repository.login(server: server, username: self.username, password: self.password) }
        busy = false
        if ok { await finish() } else { error = "Не получилось войти: проверьте имя и пароль." }
    }

    func startQuickConnect() {
        guard let server else { return }
        step = .quickConnect
        quickConnect?.cancel()
        quickConnect = Task { await runQuickConnect(server) }
    }

    func back() {
        quickConnect?.cancel()
        error = nil
        step = step == .quickConnect ? .credentials : .servers
    }

    private func connect(_ address: String) async {
        guard !address.isEmpty else { return }
        busy = true
        error = nil
        let result: Loaded<HomeServer> = await request(fallback: "Сервер не ответил.") {
            try await self.repository.connect(address: address)
        }
        switch result {
        case .value(let connected):
            server = connected
            step = .credentials
        case .failure(let message):
            error = message
        }
        busy = false
    }

    private func runQuickConnect(_ server: HomeServer) async {
        let started: Loaded<QuickConnectCode> = await request(fallback: "Quick Connect выключен на сервере.") {
            try await self.repository.startQuickConnect(server: server)
        }
        guard case .value(let code) = started else {
            if case .failure(let message) = started { error = message }
            return
        }
        quickCode = code.code
        while !Task.isCancelled {
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            let checked: Loaded<KotlinBoolean> = await request(fallback: "") {
                try await self.repository.checkQuickConnect(server: server, code: code)
            }
            if case .value(let approved) = checked, approved.boolValue {
                await finish()
                return
            }
        }
    }

    /// Вошли — источник переключается на сервер, сессия покажет его Главную.
    private func finish() async {
        try? await sources.select(source: .jellyfin)
    }
}
