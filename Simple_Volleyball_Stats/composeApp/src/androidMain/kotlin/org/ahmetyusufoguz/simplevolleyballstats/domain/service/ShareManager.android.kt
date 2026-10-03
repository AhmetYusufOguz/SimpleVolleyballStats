package org.ahmetyusufoguz.simplevolleyballstats.domain.service

import android.content.Context
import android.content.Intent

actual class ShareManager(private val context: Context) {
    actual fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SVS Maç Raporu")
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, "Raporu Paylaş").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(chooser)
    }
}
