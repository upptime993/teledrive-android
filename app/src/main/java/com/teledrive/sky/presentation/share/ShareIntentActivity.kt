package com.teledrive.sky.presentation.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.teledrive.sky.ui.theme.SkyTextPrimary
import com.teledrive.sky.ui.theme.TeleDriveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareIntentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uris = mutableListOf<Uri>()
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.add(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            }
        }

        setContent {
            TeleDriveTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Mengunggah ${uris.size} file...", color = SkyTextPrimary)
                }
            }
        }
        // TODO: initiate upload via UploadWorker
        finish()
    }
}
