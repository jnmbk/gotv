package com.gotv.player.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Observes whether the device currently has a usable network connection. */
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

/** Monitor that always reports connectivity; used where network awareness is not needed. */
object AlwaysOnlineNetworkMonitor : NetworkMonitor {
    override val isOnline: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}

/** Test double whose connectivity is driven manually. */
class FakeNetworkMonitor(initialOnline: Boolean = true) : NetworkMonitor {
    private val _isOnline = MutableStateFlow(initialOnline)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }
}
