package com.shekhar.trst01androidtask.util

import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity

object DocumentUtils {

    fun isPdfDocument(documentType: String?, documentUrl: String?, thumbnailUrl: String?): Boolean {
        if (documentType?.equals("PDF", ignoreCase = true) == true) {
            return true
        }
        if (isPdfUrl(documentUrl)) {
            return true
        }
        if (isPdfUrl(thumbnailUrl)) {
            return true
        }
        return false
    }

    fun isPdfUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || url.equals("null", ignoreCase = true)) {
            return false
        }
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        return cleanUrl.endsWith(".pdf", ignoreCase = true)
    }

    fun getPreviewUrl(document: DocumentEntity): String? {
        val docUrl = document.documentUrl?.takeIf { !it.isNullResourceOrBlank() }
        val thumbUrl = document.thumbnailUrl?.takeIf { !it.isNullResourceOrBlank() }

        if (thumbUrl != null && !isPdfUrl(thumbUrl)) {
            return thumbUrl
        }
        if (docUrl != null && !isPdfUrl(docUrl)) {
            return docUrl
        }
        return thumbUrl ?: docUrl
    }

    fun getPdfUrl(document: DocumentEntity): String? {
        val docUrl = document.documentUrl?.takeIf { !it.isNullResourceOrBlank() }
        val thumbUrl = document.thumbnailUrl?.takeIf { !it.isNullResourceOrBlank() }

        if (isPdfUrl(docUrl)) return docUrl
        if (isPdfUrl(thumbUrl)) return thumbUrl
        if (document.documentType?.equals("PDF", ignoreCase = true) == true) {
            return docUrl ?: thumbUrl
        }
        return docUrl ?: thumbUrl
    }

    private fun String?.isNullResourceOrBlank(): Boolean {
        return this.isNullOrBlank() || this.equals("null", ignoreCase = true)
    }
}
