package com.weiji.wang.capture

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.weiji.wang.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val uris = streamUris(intent)

        if (uris.isEmpty()) {
            startMain(text, emptyList())
            finish()
            return
        }

        Toast.makeText(this, "正在收录…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val copied = withContext(Dispatchers.IO) {
                uris.mapIndexedNotNull { index, uri -> copyToCache(uri, index) }
            }
            startMain(text, copied)
            finish()
        }
    }

    private fun startMain(text: String, files: List<String>) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_SHARE_TEXT, text)
                .putStringArrayListExtra(MainActivity.EXTRA_SHARE_URIS, ArrayList(files))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
    }

    @Suppress("DEPRECATION")
    private fun streamUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        val multiple = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (!multiple.isNullOrEmpty()) return multiple
        val single = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        return listOfNotNull(single)
    }

    private fun copyToCache(uri: Uri, index: Int): String? = runCatching {
        val mime = com.weiji.wang.core.storage.MimeTypes.of(this, uri)
        val ext = android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mime)
            ?: "bin"
        val file = File(cacheDir, "share-${System.currentTimeMillis()}-$index.$ext")
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        Uri.fromFile(file).toString()
    }.getOrNull()
}
