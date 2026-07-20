package com.filmax.app.update

import android.content.Context
import com.filmax.app.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Частичный DTO релиза GitHub — только поля, нужные для обновления. */
@Serializable
private data class ReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("assets") val assets: List<AssetDto> = emptyList(),
)

@Serializable
private data class AssetDto(
    @SerialName("name") val name: String = "",
    /** API-URL ассета: с токеном отдаёт файл и из приватного репозитория. */
    @SerialName("url") val url: String = "",
    @SerialName("size") val size: Long = 0,
)

/**
 * Обновления приложения из GitHub Releases: релизный CI публикует `filmax-X.Y.Z.apk` на каждый
 * тег vX.Y.Z, здесь мы читаем `releases/latest`, сравниваем с установленной версией и качаем APK.
 *
 * HTTP — голый [HttpURLConnection], а не Ktor из core:network: тому нужен весь стек kino.pub
 * (авторизация, refresh-токены), а здесь два запроса к чужому хосту без общего состояния.
 *
 * Репозиторий приватный, поэтому запросы идут с токеном из `BuildConfig.UPDATE_GITHUB_TOKEN`
 * (fine-grained, только чтение contents). Без токена проверка тихо не находит обновлений.
 */
class GitHubUpdateRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val json = Json { ignoreUnknownKeys = true }

    /** Свежий релиз, если он новее установленной версии; null — обновляться не на что. */
    suspend fun latestUpdate(): UpdateInfo? = withContext(ioDispatcher) {
        runCatching { fetchLatestRelease() }.getOrNull()?.let { release ->
            val version = release.tagName.removePrefix("v")
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return@let null
            if (!isNewer(candidate = version, current = BuildConfig.VERSION_NAME)) return@let null
            UpdateInfo(version = version, assetUrl = apk.url, sizeBytes = apk.size)
        }
    }

    /**
     * Качает APK релиза в кэш и возвращает файл. [onProgress] получает долю 0..1.
     *
     * Редирект на CDN обрабатываем вручную: `assets[].url` отвечает 302 на подписанный URL
     * objects.githubusercontent.com, и туда токен передавать нельзя — CDN отвечает на него 400.
     */
    suspend fun downloadApk(info: UpdateInfo, onProgress: suspend (Float) -> Unit): File =
        withContext(ioDispatcher) {
            val dir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
            val apkFile = File(dir, "filmax-${info.version}.apk")

            var connection = openConnection(info.assetUrl, accept = ACCEPT_BINARY, authorized = true)
            connection.instanceFollowRedirects = false
            if (connection.responseCode in REDIRECT_CODES) {
                val location = connection.getHeaderField("Location")
                    ?: error("GitHub вернул редирект без Location")
                connection.disconnect()
                connection = openConnection(location, accept = ACCEPT_BINARY, authorized = false)
            }
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Скачивание APK: HTTP ${connection.responseCode}"
            }

            connection.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (info.sizeBytes > 0) onProgress(copied.toFloat() / info.sizeBytes)
                    }
                }
            }
            apkFile
        }

    private fun fetchLatestRelease(): ReleaseDto? {
        val url = "https://api.github.com/repos/${BuildConfig.UPDATE_GITHUB_REPO}/releases/latest"
        val connection = openConnection(url, accept = ACCEPT_GITHUB_JSON, authorized = true)
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<ReleaseDto>(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, accept: String, authorized: Boolean): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", accept)
            // GitHub API отклоняет запросы без User-Agent.
            setRequestProperty("User-Agent", "filmax-app")
            val token = BuildConfig.UPDATE_GITHUB_TOKEN
            if (authorized && token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

    private companion object {
        const val UPDATES_DIR = "updates"
        const val ACCEPT_GITHUB_JSON = "application/vnd.github+json"
        const val ACCEPT_BINARY = "application/octet-stream"
        const val TIMEOUT_MS = 15_000
        const val COPY_BUFFER_BYTES = 64 * 1024
        val REDIRECT_CODES = 300..399
    }
}

/**
 * Сравнение версий `X.Y.Z`: true, когда [candidate] новее [current]. Суффиксы сборок
 * (`-debug`, `-demo`) отбрасываются, недостающие компоненты считаются нулями.
 */
internal fun isNewer(candidate: String, current: String): Boolean {
    val candidateParts = parseVersion(candidate)
    val currentParts = parseVersion(current)
    val length = maxOf(candidateParts.size, currentParts.size)
    for (index in 0 until length) {
        val candidatePart = candidateParts.getOrElse(index) { 0 }
        val currentPart = currentParts.getOrElse(index) { 0 }
        if (candidatePart != currentPart) return candidatePart > currentPart
    }
    return false
}

private fun parseVersion(raw: String): List<Int> =
    raw.removePrefix("v")
        .substringBefore("-")
        .split(".")
        .mapNotNull { part -> part.trim().toIntOrNull() }
