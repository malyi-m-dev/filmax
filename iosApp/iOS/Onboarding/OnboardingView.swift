import SwiftUI

struct OnboardingView: View {
    @StateObject private var viewModel = OnboardingViewModel()

    var body: some View {
        VStack(spacing: 0) {
            Text("Filmax")
                .font(.system(size: 34, weight: .heavy))
                .foregroundColor(Theme.accent)
                .padding(.top, 32)

            Spacer()

            Group {
                switch viewModel.step {
                case 0: WelcomeStep()
                case 1: FeaturesStep()
                default: ActivationStep(viewModel: viewModel)
                }
            }
            .transition(.opacity)
            .animation(.easeInOut, value: viewModel.step)

            Spacer()

            StepIndicators(current: viewModel.step, total: OnboardingViewModel.stepCount)
                .padding(.bottom, 24)

            BottomActions(viewModel: viewModel)
                .padding(.horizontal, 28)
                .padding(.bottom, 24)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Theme.background.ignoresSafeArea())
        .onDisappear { viewModel.cancel() }
    }
}

// MARK: - Шаги

private struct WelcomeStep: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "film.stack")
                .font(.system(size: 84))
                .foregroundColor(Theme.accent)
            Text("Кино и сериалы всегда под рукой")
                .font(.system(size: 28, weight: .bold))
                .multilineTextAlignment(.center)
                .foregroundColor(.white)
            Text("Тысячи фильмов, сериалов и аниме в одном приложении")
                .font(.system(size: 16))
                .multilineTextAlignment(.center)
                .foregroundColor(Theme.onSurfaceVariant)
        }
        .padding(.horizontal, 28)
    }
}

private struct FeaturesStep: View {
    private let features: [(String, String, String)] = [
        ("play.rectangle.fill", "Смотрите онлайн", "HD-качество, аудио и субтитры на выбор"),
        ("bookmark.fill", "Своя библиотека", "Избранное, история и продолжение просмотра"),
        ("sparkles.tv.fill", "На всех экранах", "iPhone, iPad и Apple TV"),
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            ForEach(features, id: \.1) { icon, title, subtitle in
                HStack(spacing: 16) {
                    Image(systemName: icon)
                        .font(.system(size: 24))
                        .foregroundColor(Theme.accent)
                        .frame(width: 52, height: 52)
                        .background(Theme.accent.opacity(0.18))
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    VStack(alignment: .leading, spacing: 3) {
                        Text(title).font(.system(size: 18, weight: .semibold)).foregroundColor(.white)
                        Text(subtitle).font(.system(size: 14)).foregroundColor(Theme.onSurfaceVariant)
                    }
                    Spacer()
                }
            }
        }
        .padding(.horizontal, 28)
    }
}

private struct ActivationStep: View {
    @ObservedObject var viewModel: OnboardingViewModel

    var body: some View {
        HStack(alignment: .top, spacing: 24) {
            VStack(alignment: .leading, spacing: 22) {
                Text("Активируйте устройство")
                    .font(.system(size: 26, weight: .heavy))
                    .foregroundColor(.white)
                stepRow(1, "Откройте", "kino.pub/device")
                stepRow(2, "Войдите", "в свой аккаунт KinoPub")
                stepRow(3, "Введите", "код активации справа")
            }
            Spacer()
            codeCard
        }
        .padding(.horizontal, 28)
    }

    private func stepRow(_ number: Int, _ title: String, _ subtitle: String) -> some View {
        HStack(spacing: 14) {
            Text("\(number)")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(.white)
                .frame(width: 34, height: 34)
                .background(Theme.accent)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                Text(subtitle).font(.system(size: 14)).foregroundColor(Theme.onSurfaceVariant)
            }
        }
    }

    @ViewBuilder private var codeCard: some View {
        VStack(spacing: 16) {
            if let error = viewModel.error {
                Text(error)
                    .font(.system(size: 14))
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)
                Button("Попробовать снова") { viewModel.retry() }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
            } else {
                Text("КОД АКТИВАЦИИ")
                    .font(.system(size: 13, weight: .bold))
                    .tracking(2)
                    .foregroundColor(Theme.accent)
                Text(viewModel.userCode ?? "· · · · ·")
                    .font(.system(size: 52, weight: .heavy))
                    .tracking(8)
                    .foregroundColor(.white)
                if let uri = viewModel.verificationUri {
                    Text(uri).font(.system(size: 16, weight: .semibold)).foregroundColor(Theme.accent)
                }
                if viewModel.polling {
                    HStack(spacing: 8) {
                        ProgressView().tint(Theme.onSurfaceVariant)
                        Text("Ожидаем подтверждение…").font(.system(size: 14)).foregroundColor(Theme.onSurfaceVariant)
                    }
                }
            }
        }
        .frame(maxWidth: 380)
        .padding(28)
        .background(Theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .overlay(RoundedRectangle(cornerRadius: 28).stroke(Theme.accent.opacity(0.5), lineWidth: 1))
    }
}

// MARK: - Общие элементы

private struct StepIndicators: View {
    let current: Int
    let total: Int
    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<total, id: \.self) { index in
                Capsule()
                    .fill(index == current ? Theme.accent : Theme.onSurfaceVariant.opacity(0.35))
                    .frame(width: index == current ? 24 : 8, height: 8)
                    .animation(.easeInOut, value: current)
            }
        }
    }
}

private struct BottomActions: View {
    @ObservedObject var viewModel: OnboardingViewModel

    var body: some View {
        switch viewModel.step {
        case 0:
            Button { withAnimation { viewModel.next() } } label: {
                Text("Начать").frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent).tint(Theme.accent).controlSize(.large)
        case 1:
            HStack(spacing: 12) {
                Button { withAnimation { viewModel.prev() } } label: {
                    Text("Назад").frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered).tint(Theme.accent).controlSize(.large)
                Button { withAnimation { viewModel.next() } } label: {
                    Text("Далее").frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent).tint(Theme.accent).controlSize(.large)
            }
        default:
            Button { withAnimation { viewModel.prev() } } label: {
                Text("Назад").frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered).tint(Theme.accent).controlSize(.large)
        }
    }
}
