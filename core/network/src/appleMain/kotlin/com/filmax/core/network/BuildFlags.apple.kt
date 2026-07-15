package com.filmax.core.network

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual val isDebugBuild: Boolean = Platform.isDebugBinary
