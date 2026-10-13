# Benchmark-сборка = release + фейки mock. Код фейков достаётся через Koin-модуль из
# MockFilmaxApplication и R8 его не вырезает; правила здесь — только то, что нужно замерам.
# Имена не обфусцируем: в трассе Perfetto видно, какой composable и какой класс тормозит.
-dontobfuscate
