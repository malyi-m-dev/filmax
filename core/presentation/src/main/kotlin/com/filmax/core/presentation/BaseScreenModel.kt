package com.filmax.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.error.toAppError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Базовый MVI-ScreenModel проекта. Единственное место, где presentation-слой касается
 * `androidx.lifecycle.ViewModel` — фичи об этом не знают и наследуют только [BaseScreenModel].
 * Это снимает техдолг #9 (Android-only ViewModel в фичах) и держит фичи KMP-ready: при
 * переходе на commonMain/Decompose меняется только этот класс, а не каждая фича.
 *
 * Триада MVI:
 *  - [STATE] — единый неизменяемый снимок экрана (state down);
 *  - [EVENT] — намерения пользователя, приходят через [dispatch] (events up);
 *  - [SIDE_EFFECT] — одноразовые эффекты (навигация, snackbar, …), доставляются через [postSideEffect].
 *
 * Удержание экземпляра и автоотмена [screenModelScope] обеспечиваются механизмом ViewModel
 * (переживает поворот экрана, привязан к back stack-записи навигации).
 */
abstract class BaseScreenModel<STATE : Any, SIDE_EFFECT : Any, EVENT : Any>(
    initialState: STATE,
) : ViewModel() {

    private val sideEffectsQueue: MutableList<SIDE_EFFECT> = mutableListOf()
    private var sideEffectsSubscriber: ((SIDE_EFFECT) -> Unit)? = null

    private val mainThreadDispatcher = Dispatchers.Main.immediate

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(initialState)

    /** Текущая ошибка для модального окна (null — модалки нет). */
    private val _error: MutableStateFlow<AppError?> = MutableStateFlow(null)

    /** Текущий снимок состояния. Доступен подклассам для чтения внутри корутин. */
    protected val state: STATE
        get() = _state.value

    private val updateStateLock = Mutex()
    private val sideEffectLock = Mutex()

    /** Единая точка входа для пользовательских событий экрана. */
    abstract fun dispatch(event: EVENT)

    /** Первичная загрузка данных экрана. Вызывается подклассом из его `init` после инициализации зависимостей. */
    protected abstract fun onFetchData()

    /** Скоуп жизненного цикла ScreenModel (отменяется в [onCleared]). */
    protected val screenModelScope: CoroutineScope = viewModelScope

    /**
     * Запускает корутину в [screenModelScope] на [dispatcher], предоставляя актуальный снимок [STATE].
     * Исключения внутри блока изолируются, чтобы один сбой не ронял ScreenModel.
     */
    protected fun screenModelScope(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        call: suspend CoroutineScope.(STATE) -> Unit,
    ): Job = screenModelScope.launch(dispatcher) {
        runCatching { call(state) }
    }

    /** Отправляет одноразовый side-effect. Если подписчика ещё нет — эффект буферизуется до подписки. */
    protected suspend fun postSideEffect(effect: SIDE_EFFECT) {
        sideEffectLock.withLock {
            withContext(mainThreadDispatcher) {
                sideEffectsSubscriber?.invoke(effect) ?: sideEffectsQueue.add(effect)
            }
        }
    }

    /** Атомарно обновляет состояние на main-потоке. */
    protected suspend fun updateState(call: (STATE) -> STATE) {
        updateStateLock.withLock {
            withContext(mainThreadDispatcher) {
                _state.emit(call(state))
            }
        }
    }

    /** Подписка экрана на состояние как на Compose [State]. */
    @Composable
    fun collectAsState(): State<STATE> {
        return _state.collectAsState()
    }

    /**
     * Резолвит ошибку запроса в [AppError] и показывает модалку.
     * Вызывается из ScreenModel в ветке [RequestResult.Error]:
     * `showError(result)` или `showError(message, cause)`.
     */
    protected suspend fun showError(error: AppError) {
        withContext(mainThreadDispatcher) { _error.emit(error) }
    }

    protected suspend fun showError(message: String?, cause: Throwable? = null) {
        showError(AppError.resolve(message, cause))
    }

    protected suspend fun showError(error: RequestResult.Error) {
        showError(error.toAppError())
    }

    /** Закрывает модалку ошибки. Вызывается из UI. */
    fun dismissError() {
        _error.value = null
    }

    /** Повторная загрузка данных — для кнопки «Повторить» в модалке ошибки. */
    fun retry() {
        _error.value = null
        onFetchData()
    }

    /** Подписка экрана на текущую ошибку для показа [com.filmax.core.ui]-модалки. */
    @Composable
    fun collectErrorAsState(): State<AppError?> {
        return _error.collectAsState()
    }

    /** Подписка экрана на side-effects. Буферизованные до подписки эффекты доставляются сразу. */
    @Composable
    fun collectSideEffect(key: Any? = Unit, onSideEffect: (SIDE_EFFECT) -> Unit) {
        val job = remember { mutableStateOf<Job?>(null) }
        DisposableEffect(key1 = key) {
            job.value = screenModelScope.launch(mainThreadDispatcher) {
                job.value?.let { runCatching { it.cancelAndJoin() } }
                sideEffectsSubscriber = onSideEffect
                sideEffectsQueue.forEach { postSideEffect(it) }
                sideEffectsQueue.clear()
            }
            onDispose {
                screenModelScope.launch(mainThreadDispatcher) {
                    sideEffectsSubscriber = null
                    job.value?.let { runCatching { it.cancelAndJoin() } }
                }
            }
        }
    }

    override fun onCleared() {
        sideEffectsSubscriber = null
        super.onCleared()
    }
}
