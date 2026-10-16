package com.lumaplay.tv.player

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLifecycleTest {

    private class FakePlayer {
        var pauseCallCount = 0

        fun pause() {
            pauseCallCount++
        }
    }

    private class DummyLifecycleOwner : LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = throw UnsupportedOperationException("Not needed for observer event dispatch")
    }

    private fun createLifecycleObserver(fakePlayer: FakePlayer): LifecycleEventObserver {
        return LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    fakePlayer.pause()
                }
                else -> Unit
            }
        }
    }

    @Test
    fun testObserverPausesOnPause() {
        val player = FakePlayer()
        val observer = createLifecycleObserver(player)
        val owner = DummyLifecycleOwner()

        observer.onStateChanged(owner, Lifecycle.Event.ON_PAUSE)
        assertEquals(1, player.pauseCallCount)
    }

    @Test
    fun testObserverPausesOnStop() {
        val player = FakePlayer()
        val observer = createLifecycleObserver(player)
        val owner = DummyLifecycleOwner()

        observer.onStateChanged(owner, Lifecycle.Event.ON_STOP)
        assertEquals(1, player.pauseCallCount)
    }

    @Test
    fun testObserverDoesNotPauseOnOtherEvents() {
        val player = FakePlayer()
        val observer = createLifecycleObserver(player)
        val owner = DummyLifecycleOwner()

        observer.onStateChanged(owner, Lifecycle.Event.ON_CREATE)
        observer.onStateChanged(owner, Lifecycle.Event.ON_START)
        observer.onStateChanged(owner, Lifecycle.Event.ON_RESUME)
        observer.onStateChanged(owner, Lifecycle.Event.ON_DESTROY)

        assertEquals(0, player.pauseCallCount)
    }
}
