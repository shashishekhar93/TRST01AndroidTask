package com.shekhar.trst01androidtask.ui.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shekhar.trst01androidtask.data.repository.EmployeeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.absoluteValue

sealed interface PdfState {
    data object Idle : PdfState
    data object Loading : PdfState
    data class Success(val bitmaps: List<Bitmap>) : PdfState
    data class Error(val message: String) : PdfState
}

@HiltViewModel
class DocumentPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EmployeeRepository,
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    val employeeId: Int = savedStateHandle.get<Int>("employeeId") ?: -1
    val documentId: Int = savedStateHandle.get<Int>("documentId") ?: -1

    val uiState: StateFlow<DocumentPreviewUiState> = if (employeeId == -1 || documentId == -1) {
        flowOf(DocumentPreviewUiState.NotFound)
    } else {
        repository.observeDocumentsForEmployee(employeeId)
            .map { docs ->
                val document = docs.find { it.documentId == documentId }
                if (document != null) {
                    DocumentPreviewUiState.Success(document)
                } else {
                    DocumentPreviewUiState.NotFound
                }
            }
            .catch { e ->
                emit(DocumentPreviewUiState.Error(e.message ?: "Failed to load document"))
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DocumentPreviewUiState.Loading
    )

    private val _pdfState = MutableStateFlow<PdfState>(PdfState.Idle)
    val pdfState: StateFlow<PdfState> = _pdfState.asStateFlow()

    private var currentPdfUrl: String? = null

    fun loadPdf(url: String) {
        if (currentPdfUrl == url && (_pdfState.value is PdfState.Success || _pdfState.value is PdfState.Loading)) {
            return
        }
        currentPdfUrl = url
        viewModelScope.launch(Dispatchers.IO) {
            _pdfState.value = PdfState.Loading
            var parcelFileDescriptor: ParcelFileDescriptor? = null
            var pdfRenderer: PdfRenderer? = null
            val bitmaps = mutableListOf<Bitmap>()
            try {
                val cacheDir = context.cacheDir
                val fileName = "pdf_${url.hashCode().absoluteValue}.pdf"
                val cachedFile = File(cacheDir, fileName)

                if (!cachedFile.exists() || cachedFile.length() == 0L) {
                    val request = Request.Builder().url(url).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw RuntimeException("Failed to download PDF: ${response.code}")
                        }
                        val body = response.body ?: throw RuntimeException("Empty response body")
                        FileOutputStream(cachedFile).use { output ->
                            body.byteStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                    }
                }

                parcelFileDescriptor = ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(parcelFileDescriptor)
                val pageCount = pdfRenderer.pageCount
                if (pageCount <= 0) {
                    throw RuntimeException("PDF document has no pages")
                }

                for (i in 0 until pageCount) {
                    pdfRenderer.openPage(i).use { page ->
                        val width = page.width * 2
                        val height = page.height * 2
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)
                    }
                }

                _pdfState.value = PdfState.Success(bitmaps)
            } catch (e: Exception) {
                bitmaps.forEach { it.recycle() }
                _pdfState.value = PdfState.Error(e.message ?: "Failed to render PDF")
            } finally {
                try {
                    pdfRenderer?.close()
                } catch (_: Exception) {}
                try {
                    parcelFileDescriptor?.close()
                } catch (_: Exception) {}
            }
        }
    }
}
