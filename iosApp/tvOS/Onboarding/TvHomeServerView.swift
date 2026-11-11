import SwiftUI
import Shared

/// Вход на домашний сервер Jellyfin: серверы в сети или адрес вручную, затем вход. Главный путь на
/// ТВ — Quick Connect: код подтверждают в Jellyfin на телефоне, и пароль пультом набирать не нужно;
/// имя и пароль — запасной вариант.
struct TvHomeServerView: View {
    let onBack: () -> Void
    @StateObject private var viewModel = HomeServerViewModel()

    init(onBack: @escaping () -> Void) {
        self.onBack = onBack
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(alignment: .leading, spacing: DS.Spacing.xl) {
                Text("Домашний сервер")
                    .font(TvType.screenTitle)
                    .foregroundColor(Theme.text)
                step
                if let error = viewModel.error {
                    Text(error)
                        .font(TvType.body)
                        .foregroundColor(Theme.error)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, DS.tvSafeHorizontal)
            .padding(.vertical, DS.tvSafeVertical)
        }
        .task { await viewModel.discover() }
        .onExitCommand(perform: back)
    }

    @ViewBuilder private var step: some View {
        switch viewModel.step {
        case .servers:
            TvServerListStep(viewModel: viewModel, onBack: onBack)
        case .credentials:
            TvCredentialsStep(viewModel: viewModel)
        case .quickConnect:
            TvQuickConnectStep(viewModel: viewModel)
        }
    }

    /// «Меню» — шаг назад внутри входа, а с первого шага — к выбору источника.
    private func back() {
        if viewModel.step == .servers {
            onBack()
        } else {
            viewModel.back()
        }
    }
}

// MARK: - Серверы

private struct TvServerListStep: View {
    @ObservedObject var viewModel: HomeServerViewModel
    let onBack: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.lg) {
            discovered
            Text("Или введите адрес")
                .font(TvType.section)
                .foregroundColor(Theme.text)
                .padding(.top, DS.Spacing.lg)
            addressRow
            Button(action: onBack) {
                Label("Другой источник", systemImage: "chevron.left")
            }
            .tvActionStyle(.secondary)
            .padding(.top, DS.Spacing.lg)
        }
    }

    @ViewBuilder private var discovered: some View {
        Text("Серверы в сети")
            .font(TvType.section)
            .foregroundColor(Theme.text)
        if viewModel.searching {
            HStack(spacing: DS.Spacing.md) {
                ProgressView().tint(Theme.accent)
                Text("Ищем серверы…").font(TvType.body).foregroundColor(Theme.textSecondary)
            }
        } else if viewModel.discovered.isEmpty {
            Text("В сети серверов не нашлось — роутер может не пропускать поиск. Введите адрес вручную.")
                .font(TvType.body)
                .foregroundColor(Theme.textSecondary)
                .frame(maxWidth: TvMetrics.textColumnWidth, alignment: .leading)
        }
        ForEach(viewModel.discovered, id: \.address) { server in
            Button {
                Task { await viewModel.choose(server) }
            } label: {
                VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                    Text(server.name).font(TvType.cardTitle)
                    Text(server.address).font(TvType.caption)
                }
                .frame(width: TvMetrics.formWidth, alignment: .leading)
            }
            .tvActionStyle(.secondary)
            .disabled(viewModel.busy)
        }
        Button {
            Task { await viewModel.discover() }
        } label: {
            Label("Искать снова", systemImage: "arrow.clockwise")
        }
        .tvActionStyle(.secondary)
        .disabled(viewModel.searching)
    }

    private var addressRow: some View {
        HStack(spacing: DS.Spacing.lg) {
            // Схему и порт 8096 дописывает общий слой — достаточно «192.168.1.20».
            TextField("192.168.1.20:8096", text: $viewModel.address)
                .autocorrectionDisabled()
                .frame(maxWidth: TvMetrics.formWidth)
            Button {
                Task { await viewModel.connectToAddress() }
            } label: {
                Label("Подключиться", systemImage: "arrow.right")
            }
            .tvActionStyle(.primary)
            .disabled(viewModel.busy || viewModel.address.trimmingCharacters(in: .whitespaces).isEmpty)
            if viewModel.busy {
                ProgressView().tint(Theme.accent)
            }
        }
        .focusSection()
    }
}

// MARK: - Вход

private struct TvCredentialsStep: View {
    @ObservedObject var viewModel: HomeServerViewModel

    /// Код Quick Connect — главный путь, если сервер его принимает; иначе главный — пароль.
    private var quickConnect: Bool { viewModel.server?.quickConnect == true }

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.lg) {
            if let server = viewModel.server {
                VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                    Text(server.name).font(TvType.section).foregroundColor(Theme.text)
                    Text(server.address).font(TvType.caption).foregroundColor(Theme.onSurfaceVariant)
                }
            }
            if quickConnect {
                Button { viewModel.startQuickConnect() } label: {
                    Label("Войти по коду", systemImage: "qrcode")
                }
                .tvActionStyle(.primary)
                Text("или с именем и паролем")
                    .font(TvType.caption)
                    .foregroundColor(Theme.onSurfaceVariant)
            }
            passwordForm
            Button { viewModel.back() } label: {
                Label("Другой сервер", systemImage: "chevron.left")
            }
            .tvActionStyle(.secondary)
        }
    }

    private var passwordForm: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.md) {
            TextField("Имя пользователя", text: $viewModel.username)
                .autocorrectionDisabled()
            SecureField("Пароль", text: $viewModel.password)
            HStack(spacing: DS.Spacing.lg) {
                Button {
                    Task { await viewModel.signIn() }
                } label: {
                    Label("Войти", systemImage: "person.badge.key")
                }
                .tvActionStyle(quickConnect ? .secondary : .primary)
                .disabled(viewModel.busy || viewModel.username.isEmpty)
                if viewModel.busy {
                    ProgressView().tint(Theme.accent)
                }
            }
        }
        .frame(maxWidth: TvMetrics.formWidth, alignment: .leading)
        .focusSection()
    }
}

// MARK: - Quick Connect

private struct TvQuickConnectStep: View {
    @ObservedObject var viewModel: HomeServerViewModel

    var body: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.lg) {
            Text("КОД ДЛЯ ВХОДА")
                .font(TvType.caption)
                .fontWeight(.bold)
                .tracking(TvMetrics.captionTracking)
                .foregroundColor(Theme.accent)
            Text(viewModel.quickCode ?? "· · · · · ·")
                .font(TvType.code)
                .foregroundColor(Theme.text)
            Text("Подтвердите код в Jellyfin на телефоне")
                .font(TvType.section)
                .foregroundColor(Theme.text)
            Text("Профиль → Quick Connect — экран сам перейдёт дальше")
                .font(TvType.body)
                .foregroundColor(Theme.textSecondary)
            if viewModel.quickCode != nil && viewModel.error == nil {
                HStack(spacing: DS.Spacing.md) {
                    ProgressView().tint(Theme.onSurfaceVariant)
                    Text("Ждём подтверждения…")
                        .font(TvType.caption)
                        .foregroundColor(Theme.onSurfaceVariant)
                }
            }
            Button { viewModel.back() } label: {
                Label("Войти с паролем", systemImage: "chevron.left")
            }
            .tvActionStyle(.secondary)
        }
    }
}
