import SwiftUI

/// Онбординг Apple TV. Логика — общий `OnboardingViewModel` (тот же, что на iOS),
/// UI — TV-раскладка: крупные элементы, TV-safe отступы, навигация пультом (focus engine).
struct TvOnboardingView: View {
    @StateObject private var viewModel = OnboardingViewModel()
    @FocusState private var primaryFocused: Bool

    var body: some View {
        VStack(spacing: 0) {
            Text("Filmax")
                .font(.system(size: 56, weight: .heavy))
                .foregroundColor(Theme.accent)
                .padding(.top, 60)

            Spacer()

            Group {
                switch viewModel.step {
                case 0: welcome
                case 1: features
                default: activation
                }
            }
            .animation(.easeInOut, value: viewModel.step)

            Spacer()

            actions
                .padding(.bottom, 60)
        }
        .padding(.horizontal, 90) // TV-safe зона
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Theme.background.ignoresSafeArea())
        .onAppear { primaryFocused = true }
        .onDisappear { viewModel.cancel() }
    }

    // MARK: Шаги

    private var welcome: some View {
        VStack(spacing: 24) {
            Image(systemName: "film.stack")
                .font(.system(size: 140))
                .foregroundColor(Theme.accent)
            Text("Кино и сериалы всегда под рукой")
                .font(.system(size: 46, weight: .bold))
                .foregroundColor(.white)
            Text("Тысячи фильмов, сериалов и аниме на большом экране")
                .font(.system(size: 26))
                .foregroundColor(Theme.onSurfaceVariant)
        }
        .multilineTextAlignment(.center)
    }

    private var features: some View {
        HStack(spacing: 40) {
            featureCard("play.rectangle.fill", "Смотрите онлайн", "HD-качество, аудио и субтитры")
            featureCard("bookmark.fill", "Своя библиотека", "Избранное и продолжение просмотра")
            featureCard("sparkles.tv.fill", "На всех экранах", "iPhone, iPad и Apple TV")
        }
    }

    private func featureCard(_ icon: String, _ title: String, _ subtitle: String) -> some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 56))
                .foregroundColor(Theme.accent)
                .frame(width: 120, height: 120)
                .background(Theme.accent.opacity(0.18))
                .clipShape(RoundedRectangle(cornerRadius: 28))
            Text(title).font(.system(size: 28, weight: .semibold)).foregroundColor(.white)
            Text(subtitle)
                .font(.system(size: 20)).foregroundColor(Theme.onSurfaceVariant)
                .multilineTextAlignment(.center)
        }
        .frame(width: 320)
    }

    private var activation: some View {
        HStack(alignment: .center, spacing: 80) {
            VStack(alignment: .leading, spacing: 34) {
                Text("Активируйте устройство")
                    .font(.system(size: 44, weight: .heavy))
                    .foregroundColor(.white)
                step(1, "Откройте", "kino.pub/device")
                step(2, "Войдите", "в аккаунт KinoPub")
                step(3, "Введите", "код активации справа")
            }
            codeCard
        }
    }

    private func step(_ number: Int, _ title: String, _ subtitle: String) -> some View {
        HStack(spacing: 20) {
            Text("\(number)")
                .font(.system(size: 26, weight: .bold)).foregroundColor(.white)
                .frame(width: 56, height: 56).background(Theme.accent).clipShape(Circle())
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.system(size: 26, weight: .bold)).foregroundColor(.white)
                Text(subtitle).font(.system(size: 22)).foregroundColor(Theme.onSurfaceVariant)
            }
        }
    }

    @ViewBuilder private var codeCard: some View {
        VStack(spacing: 22) {
            if let error = viewModel.error {
                Text(error)
                    .font(.system(size: 22)).foregroundColor(.white)
                    .multilineTextAlignment(.center)
                Button("Попробовать снова") { viewModel.retry() }
                    .buttonStyle(.borderedProminent).tint(Theme.accent)
            } else {
                Text("КОД АКТИВАЦИИ")
                    .font(.system(size: 20, weight: .bold)).tracking(3).foregroundColor(Theme.accent)
                Text(viewModel.userCode ?? "· · · · ·")
                    .font(.system(size: 84, weight: .heavy)).tracking(12).foregroundColor(.white)
                if let uri = viewModel.verificationUri {
                    Text(uri).font(.system(size: 26, weight: .semibold)).foregroundColor(Theme.accent)
                }
                if viewModel.polling {
                    HStack(spacing: 12) {
                        ProgressView().tint(Theme.onSurfaceVariant)
                        Text("Ожидаем подтверждение…").font(.system(size: 22)).foregroundColor(Theme.onSurfaceVariant)
                    }
                }
            }
        }
        .frame(width: 560)
        .padding(44)
        .background(Theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 36))
        .overlay(RoundedRectangle(cornerRadius: 36).stroke(Theme.accent.opacity(0.5), lineWidth: 2))
    }

    // MARK: Действия (навигация пультом)

    @ViewBuilder private var actions: some View {
        switch viewModel.step {
        case 0:
            Button { viewModel.next() } label: { Text("Начать").padding(.horizontal, 40) }
                .buttonStyle(.borderedProminent).tint(Theme.accent)
                .focused($primaryFocused)
        case 1:
            HStack(spacing: 24) {
                Button { viewModel.prev() } label: { Text("Назад").padding(.horizontal, 30) }
                    .buttonStyle(.bordered).tint(Theme.accent)
                Button { viewModel.next() } label: { Text("Далее").padding(.horizontal, 30) }
                    .buttonStyle(.borderedProminent).tint(Theme.accent)
                    .focused($primaryFocused)
            }
        default:
            Button { viewModel.prev() } label: { Text("Назад").padding(.horizontal, 40) }
                .buttonStyle(.bordered).tint(Theme.accent)
                .focused($primaryFocused)
        }
    }
}
