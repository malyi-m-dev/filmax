import Foundation
import Shared

// Общие приёмы интеропа с KMP-слоем (проверены на Mac, см. iosApp/README):
// - `RequestResult<T>` — sealed interface, в Swift это протокол без generic-параметра: SKIE разбирает
//   его `onEnum(of:)`, а данные успеха приходят как `Any?` и приводятся к ожидаемому типу;
// - Kotlin `Int` — это `Int32`, `Int?` — `KotlinInt?`, `Boolean` в Flow — `KotlinBoolean`.

/// Данные успешного ответа или текст ошибки.
enum Loaded<Value> {
    case value(Value)
    case failure(String)

    var value: Value? {
        if case .value(let value) = self { return value }
        return nil
    }
}

/// Разбирает ответ общего слоя: успех с данными нужного типа или текст ошибки.
func loaded<Value>(_ result: any RequestResult, fallback: String) -> Loaded<Value> {
    switch onEnum(of: result) {
    case .success(let success):
        guard let value = success.data as? Value else { return .failure(fallback) }
        return .value(value)
    case .error(let failure):
        return .failure(failure.message ?? fallback)
    }
}

/// Вызов общего слоя целиком: исключение моста (отмена, сбой интеропа) — тоже ошибка экрана.
func request<Value>(
    fallback: String,
    _ call: () async throws -> any RequestResult
) async -> Loaded<Value> {
    do {
        return loaded(try await call(), fallback: fallback)
    } catch {
        return .failure(error.localizedDescription)
    }
}

/// Успех без данных (`RequestResult<Unit>`): true — сервер принял.
func succeeded(_ call: () async throws -> any RequestResult) async -> Bool {
    guard let result = try? await call() else { return false }
    if case .success = onEnum(of: result) { return true }
    return false
}

extension Optional where Wrapped == Int32 {
    /// `Int?` для Kotlin: nil остаётся nil.
    var kotlin: KotlinInt? { map { KotlinInt(int: $0) } }
}
