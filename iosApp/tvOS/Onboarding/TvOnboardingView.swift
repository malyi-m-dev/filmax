import SwiftUI
import Shared

/// Вход Apple TV: сначала выбор источника, потом его вход — код устройства kino.pub или домашний
/// сервер Jellyfin. «Меню» на пульте возвращает к выбору, а с выбора — закрывает приложение, как
/// принято на tvOS.
struct TvOnboardingView: View {
    private enum Stage {
        case choice, kinopub, homeServer
    }

    @State private var stage: Stage = .choice

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            switch stage {
            case .choice:
                TvSourceChoiceView(
                    onKinopub: { stage = .kinopub },
                    onHomeServer: { stage = .homeServer }
                )
            case .kinopub:
                TvKinopubSignInView { stage = .choice }
            case .homeServer:
                TvHomeServerView { stage = .choice }
            }
        }
    }
}

// MARK: - Выбор источника

private struct TvSourceChoiceView: View {
    let onKinopub: () -> Void
    let onHomeServer: () -> Void

    var body: some View {
        VStack(spacing: DS.Spacing.xxl) {
            VStack(spacing: DS.Spacing.sm) {
                Text("Filmax")
                    .font(TvType.hero)
                    .foregroundColor(Theme.accent)
                Text("Откуда будем смотреть?")
                    .font(TvType.section)
                    .foregroundColor(Theme.text)
            }
            HStack(spacing: DS.Spacing.xxl) {
                TvSourceCard(
                    title: ContentSource.kinopub.label,
                    subtitle: "Вход по коду на сайте",
                    systemImage: "play.tv.fill",
                    action: onKinopub
                )
                TvSourceCard(
                    title: ContentSource.jellyfin.label,
                    subtitle: "Jellyfin в вашей сети",
                    systemImage: "server.rack",
                    action: onHomeServer
                )
            }
            .focusSection()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Крупная карточка источника — системная `.card`-кнопка, как плитки приложений Apple TV.
private struct TvSourceCard: View {
    let title: String
    let subtitle: String
    let systemImage: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: DS.Spacing.md) {
                Image(systemName: systemImage)
                    .font(TvType.icon)
                    .foregroundColor(Theme.accent)
                Text(title)
                    .font(TvType.section)
                    .foregroundColor(Theme.text)
                Text(subtitle)
                    .font(TvType.caption)
                    .foregroundColor(Theme.onSurfaceVariant)
                    .multilineTextAlignment(.center)
            }
            .frame(width: TvMetrics.sourceCardWidth, height: TvMetrics.sourceCardHeight)
            .background(Theme.surface)
        }
        .buttonStyle(.card)
    }
}

// MARK: - kino.pub

/// Вход kino.pub по коду устройства: код — на экране, подтверждают на сайте с телефона или
/// компьютера, поллинг ведёт `OnboardingViewModel`.
private struct TvKinopubSignInView: View {
    let onBack: () -> Void
    @StateObject private var viewModel = OnboardingViewModel()

    init(onBack: @escaping () -> Void) {
        self.onBack = onBack
    }

    var body: some View {
        VStack(spacing: DS.Spacing.xxl) {
            HStack(alignment: .center, spacing: TvMetrics.columnGap) {
                instructions
                TvActivationCodeCard(viewModel: viewModel)
            }
            Button(action: onBack) {
                Label("Другой источник", systemImage: "chevron.left")
            }
            .tvActionStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task { await viewModel.start() }
        .onDisappear { viewModel.cancel() }
        .onExitCommand(perform: onBack)
    }

    private var instructions: some View {
        VStack(alignment: .leading, spacing: DS.Spacing.xl) {
            Text("Войдите в kino.pub")
                .font(TvType.screenTitle)
                .foregroundColor(Theme.text)
            TvSignInStep(number: 1, title: "Откройте на телефоне", detail: viewModel.verificationUri ?? "kino.pub/device")
            TvSignInStep(number: 2, title: "Войдите в аккаунт", detail: "тот, где оформлена подписка")
            TvSignInStep(number: 3, title: "Введите код", detail: "он справа — экран сам перейдёт дальше")
        }
    }

}

private struct TvSignInStep: View {
    let number: Int
    let title: String
    let detail: String

    var body: some View {
        HStack(spacing: DS.Spacing.lg) {
            Text("\(number)")
                .font(TvType.cardTitle)
                .foregroundColor(.white)
                .frame(width: TvMetrics.stepBadgeSize, height: TvMetrics.stepBadgeSize)
                .background(Circle().fill(Theme.accent))
            VStack(alignment: .leading, spacing: DS.Spacing.xs) {
                Text(title).font(TvType.cardTitle).foregroundColor(Theme.text)
                Text(detail).font(TvType.caption).foregroundColor(Theme.onSurfaceVariant)
            }
        }
    }
}

/// Карточка с кодом: код, адрес, ожидание подтверждения или ошибка с повтором.
private struct TvActivationCodeCard: View {
    @ObservedObject var viewModel: OnboardingViewModel

    var body: some View {
        VStack(spacing: DS.Spacing.lg) {
            if let error = viewModel.error {
                Text(error)
                    .font(TvType.body)
                    .foregroundColor(Theme.error)
                    .multilineTextAlignment(.center)
                Button { viewModel.retry() } label: {
                    Label("Попробовать снова", systemImage: "arrow.clockwise")
                }
                .tvActionStyle(.primary)
            } else {
                Text("КОД АКТИВАЦИИ")
                    .font(TvType.caption)
                    .fontWeight(.bold)
                    .tracking(TvMetrics.captionTracking)
                    .foregroundColor(Theme.accent)
                Text(viewModel.userCode ?? "· · · · ·")
                    .font(TvType.code)
                    .foregroundColor(Theme.text)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                if viewModel.polling {
                    HStack(spacing: DS.Spacing.md) {
                        ProgressView().tint(Theme.onSurfaceVariant)
                        Text("Ждём подтверждения…")
                            .font(TvType.caption)
                            .foregroundColor(Theme.onSurfaceVariant)
                    }
                }
            }
        }
        .frame(width: TvMetrics.codeCardWidth)
        .padding(DS.Spacing.xxl)
        .background(Theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: DS.Radius.xl))
        .overlay(
            RoundedRectangle(cornerRadius: DS.Radius.xl)
                .stroke(Theme.accentSoft, lineWidth: TvMetrics.outlineWidth)
        )
    }
}
