import SwiftUI
import Shared

/// Вход на домашний сервер Jellyfin: серверы в сети находятся сами, адрес можно ввести вручную;
/// дальше имя и пароль или Quick Connect. Нативная форма iOS — список секций.
struct HomeServerLoginView: View {
    @StateObject private var viewModel = HomeServerViewModel()

    var body: some View {
        Form {
            switch viewModel.step {
            case .servers: serversSection
            case .credentials: credentialsSection
            case .quickConnect: quickConnectSection
            }
            if let error = viewModel.error {
                Section { Text(error).foregroundColor(Theme.error) }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Theme.background.ignoresSafeArea())
        .navigationTitle(viewModel.server?.name ?? "Домашний сервер")
        .toolbar {
            if viewModel.step != .servers {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Назад") { viewModel.back() }
                }
            }
        }
        .navigationBarBackButtonHidden(viewModel.step != .servers)
        .task { await viewModel.discover() }
        .disabled(viewModel.busy)
    }

    @ViewBuilder private var serversSection: some View {
        Section("Серверы в сети") {
            if viewModel.searching {
                ProgressView()
            } else if viewModel.discovered.isEmpty {
                Text("Не нашли сервер — введите адрес ниже").foregroundColor(Theme.textSecondary)
            }
            ForEach(viewModel.discovered, id: \.address) { server in
                Button {
                    Task { await viewModel.choose(server) }
                } label: {
                    VStack(alignment: .leading) {
                        Text(server.name).foregroundColor(Theme.text)
                        Text(server.address).font(.footnote).foregroundColor(Theme.textSecondary)
                    }
                }
            }
            Button("Искать ещё раз") { Task { await viewModel.discover() } }
        }
        Section("Адрес вручную") {
            TextField("192.168.1.20:8096", text: $viewModel.address)
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            Button("Подключиться") { Task { await viewModel.connectToAddress() } }
                .disabled(viewModel.address.isEmpty)
        }
    }

    @ViewBuilder private var credentialsSection: some View {
        Section("Учётная запись Jellyfin") {
            TextField("Имя", text: $viewModel.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            SecureField("Пароль", text: $viewModel.password)
            Button("Войти") { Task { await viewModel.signIn() } }
                .disabled(viewModel.username.isEmpty)
        }
        if viewModel.server?.quickConnect == true {
            Section {
                Button("Войти по коду Quick Connect") { viewModel.startQuickConnect() }
            } footer: {
                Text("Код подтверждают в Jellyfin на другом устройстве — пароль вводить не нужно.")
            }
        }
    }

    @ViewBuilder private var quickConnectSection: some View {
        Section {
            VStack(spacing: DS.Spacing.md) {
                if let code = viewModel.quickCode {
                    Text(code)
                        .font(.system(size: 40, weight: .bold, design: .monospaced))
                        .frame(maxWidth: .infinity)
                    Text("Откройте Jellyfin → Quick Connect и введите этот код")
                        .font(.footnote)
                        .foregroundColor(Theme.textSecondary)
                        .multilineTextAlignment(.center)
                    ProgressView()
                } else {
                    ProgressView()
                }
            }
            .padding(.vertical, DS.Spacing.md)
        }
    }
}
