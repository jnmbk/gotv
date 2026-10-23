package com.gotv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gotv.player.data.repository.SqliteChannelRepository
import com.gotv.player.data.storage.FileLogoStorage
import com.gotv.player.network.AndroidNetworkMonitor
import com.gotv.player.player.Media3PlayerEngine
import com.gotv.player.ui.GoTvApp
import com.gotv.player.ui.qr.AndroidQrScannerProvider
import com.gotv.player.ui.theme.GoTvTheme

class MainActivity : ComponentActivity() {

    private val channelRepository by lazy {
        SqliteChannelRepository(applicationContext)
    }

    private val logoStorage by lazy {
        FileLogoStorage(applicationContext)
    }

    private val networkMonitor by lazy {
        AndroidNetworkMonitor(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoTvTheme {
                GoTvApp(
                    channelRepository = channelRepository,
                    logoStorage = logoStorage,
                    playerEngineFactory = { Media3PlayerEngine(applicationContext) },
                    networkMonitor = networkMonitor,
                    qrScanner = AndroidQrScannerProvider
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.close()
    }
}

