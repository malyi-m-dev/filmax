package com.filmax.data.tvlink

import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.domain.common.TelemetryText

/**
 * Крошка телеметрии связи с ТВ. Обрыв Wi-Fi или закрытый Filmax на ТВ — обычная эксплуатация, а не
 * баг, поэтому событием не уходит, но по хронологии отчёта видно, что связь была и почему пропала.
 */
internal fun linkTrail(message: String) {
    ErrorReporting.reporter.log(TelemetryText.line("tvlink: $message"))
}
