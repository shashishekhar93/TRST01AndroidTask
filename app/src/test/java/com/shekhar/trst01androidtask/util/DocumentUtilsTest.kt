package com.shekhar.trst01androidtask.util

import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentUtilsTest {

    @Test
    fun testIsPdfDocumentByType() {
        val doc = DocumentEntity(1, 1, "Test", "PDF", true, null, null, "http://example.com/doc", null)
        assertTrue(DocumentUtils.isPdfDocument(doc.documentType, doc.documentUrl, doc.thumbnailUrl))
    }

    @Test
    fun testIsPdfUrlWithExtension() {
        assertTrue(DocumentUtils.isPdfUrl("http://example.com/document.pdf"))
        assertTrue(DocumentUtils.isPdfUrl("https://example.com/path/file.PDF"))
    }

    @Test
    fun testIsPdfUrlWithQueryParameters() {
        assertTrue(DocumentUtils.isPdfUrl("https://example.com/document.pdf?token=123&expire=456"))
        assertTrue(DocumentUtils.isPdfUrl("https://example.com/file.PDF?param=value#fragment"))
    }

    @Test
    fun testIsPdfUrlEdgeCases() {
        assertFalse(DocumentUtils.isPdfUrl(null))
        assertFalse(DocumentUtils.isPdfUrl(""))
        assertFalse(DocumentUtils.isPdfUrl("null"))
        assertFalse(DocumentUtils.isPdfUrl("http://example.com/image.jpg"))
        assertFalse(DocumentUtils.isPdfUrl("http://example.com/pdf.png"))
    }

    @Test
    fun testDocumentUtilsPdfDetectionCombinations() {
        val doc1 = DocumentEntity(1, 1, "Doc", "IMAGE", true, null, null, "https://example.com/file.pdf?token=999", null)
        assertTrue(DocumentUtils.isPdfDocument(doc1.documentType, doc1.documentUrl, doc1.thumbnailUrl))

        val doc2 = DocumentEntity(2, 1, "Doc", "ID", true, null, null, "https://example.com/image.jpg", "https://example.com/thumb.pdf")
        assertTrue(DocumentUtils.isPdfDocument(doc2.documentType, doc2.documentUrl, doc2.thumbnailUrl))

        val doc3 = DocumentEntity(3, 1, "Doc", "ID", true, null, null, "https://example.com/image.jpg", "https://example.com/thumb.jpg")
        assertFalse(DocumentUtils.isPdfDocument(doc3.documentType, doc3.documentUrl, doc3.thumbnailUrl))
    }
}
