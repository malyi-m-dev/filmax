package com.filmax.feature.tvlink.common

import com.filmax.data.fake.FakeTvRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Связь с запомненным ТВ держится, пока Filmax на экране телефона, и только одна. */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteKeeperScreenModelTest {

    private val remote = FakeTvRemote()
    private val model by lazy { RemoteKeeperScreenModel(remote) }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `shown app keeps the link and hidden app stops`() = runTest {
        model.dispatch(RemoteKeeperEvent.Shown)
        assertEquals(1, remote.keepers.value)

        model.dispatch(RemoteKeeperEvent.Hidden)

        assertEquals(0, remote.keepers.value)
    }

    @Test
    fun `shown twice keeps a single link`() = runTest {
        model.dispatch(RemoteKeeperEvent.Shown)
        model.dispatch(RemoteKeeperEvent.Shown)

        assertEquals(1, remote.keepers.value)
    }
}
