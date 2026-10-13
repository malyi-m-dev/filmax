package com.filmax.core.testing

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImage
import coil3.test.FakeImageLoaderEngine
import org.junit.rules.ExternalResource

/** Серая заглушка вместо любой картинки: цвет кадра в UI-тесте ничего не проверяет. */
private const val PLACEHOLDER_COLOR = 0xFF808080.toInt()

/**
 * Картинки в UI-тестах без сети и декодирования. Фикстуры :data:fake ссылаются на настоящие
 * адреса (picsum.photos): без подмены Coil качал бы их и декодировал в фоновом потоке, а
 * нативная графика Robolectric на Linux в таком потоке роняет JVM целиком («Class not found:
 * java/nio/FloatBuffer», exit 134) — на CI это валило тесты, которые успевали дождаться картинки.
 * Правило ставит загрузчик, который сразу отдаёт заглушку, и снимает его после теста.
 */
@OptIn(DelicateCoilApi::class)
class FakeImagesRule : ExternalResource() {

    override fun before() {
        SingletonImageLoader.setUnsafe { context ->
            val engine = FakeImageLoaderEngine.Builder()
                .default(FakeImage(color = PLACEHOLDER_COLOR))
                .build()
            ImageLoader.Builder(context).components { add(engine) }.build()
        }
    }

    override fun after() {
        SingletonImageLoader.reset()
    }
}
