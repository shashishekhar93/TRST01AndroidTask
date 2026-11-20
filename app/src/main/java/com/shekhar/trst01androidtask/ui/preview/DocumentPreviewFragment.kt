package com.shekhar.trst01androidtask.ui.preview

import android.animation.ObjectAnimator
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.databinding.FragmentDocumentPreviewBinding
import com.shekhar.trst01androidtask.util.DateFormatter
import com.shekhar.trst01androidtask.util.DocumentUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DocumentPreviewFragment : Fragment() {

    private var _binding: FragmentDocumentPreviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DocumentPreviewViewModel by viewModels()

    private var shimmerAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDocumentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInsets(view)
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnErrorBack.setOnClickListener {
            findNavController().navigateUp()
        }

        observeViewModel()
    }

    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.headerBar.updatePadding(top = statusBarInset.top)
            binding.layoutContent.updatePadding(bottom = navBarInset.bottom)
            insets
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUiState(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pdfState.collectLatest { state ->
                    updatePdfState(state)
                }
            }
        }
    }

    private fun updateUiState(state: DocumentPreviewUiState) {
        when (state) {
            is DocumentPreviewUiState.Loading -> {
                stopShimmerAnimation()
                binding.progressBarLoading.visibility = View.VISIBLE
                binding.layoutContent.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
            }
            is DocumentPreviewUiState.NotFound -> {
                stopShimmerAnimation()
                binding.progressBarLoading.visibility = View.GONE
                binding.layoutContent.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = "Requested document could not be found."
            }
            is DocumentPreviewUiState.Error -> {
                stopShimmerAnimation()
                binding.progressBarLoading.visibility = View.GONE
                binding.layoutContent.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
            is DocumentPreviewUiState.Success -> {
                binding.progressBarLoading.visibility = View.GONE
                binding.layoutContent.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE

                bindDocument(state.document)
            }
        }
    }

    private fun updatePdfState(state: PdfState) {
        when (state) {
            is PdfState.Idle -> {
                // do nothing
            }
            is PdfState.Loading -> {
                startShimmerAnimation()
                binding.scrollPdfContainer.visibility = View.GONE
                binding.layoutImageError.visibility = View.GONE
            }
            is PdfState.Success -> {
                stopShimmerAnimation()
                binding.layoutImageError.visibility = View.GONE
                binding.scrollPdfContainer.visibility = View.VISIBLE

                binding.layoutPdfPages.removeAllViews()
                for (bitmap in state.bitmaps) {
                    val imageView = ImageView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            bottomMargin = 16
                        }
                        setImageBitmap(bitmap)
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    binding.layoutPdfPages.addView(imageView)
                }
            }
            is PdfState.Error -> {
                stopShimmerAnimation()
                binding.scrollPdfContainer.visibility = View.GONE
                showImageError(state.message)
            }
        }
    }

    private fun bindDocument(document: DocumentEntity) {
        val context = requireContext()
        val title = document.documentName ?: "Document Preview"
        binding.tvTitle.text = title
        binding.tvDocumentName.text = title
        binding.tvDocumentType.text = "Type: ${document.documentType ?: "N/A"}"

        val dateText = DateFormatter.formatDate(document.createdDate ?: document.updatedDate)
        binding.tvDocumentDate.text = "Date: $dateText"

        val isVerified = document.isVerified == true
        binding.chipStatus.text = if (isVerified) "Verified" else "Pending"
        if (isVerified) {
            binding.chipStatus.setBackgroundResource(R.drawable.bg_status_chip)
            binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
        } else {
            binding.chipStatus.setBackgroundResource(R.drawable.bg_status_chip_inactive)
            binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        val isPdf = DocumentUtils.isPdfDocument(document.documentType, document.documentUrl, document.thumbnailUrl)

        if (isPdf) {
            binding.ivDocumentImage.visibility = View.GONE
            binding.scrollPdfContainer.visibility = View.VISIBLE
            binding.layoutImageError.visibility = View.GONE

            val pdfUrl = DocumentUtils.getPdfUrl(document)
            if (pdfUrl.isNullOrBlank()) {
                stopShimmerAnimation()
                showImageError("No PDF document URL available for preview.")
            } else {
                viewModel.loadPdf(pdfUrl)
            }
        } else {
            binding.scrollPdfContainer.visibility = View.GONE
            val imageUrl = DocumentUtils.getPreviewUrl(document)
            if (imageUrl.isNullOrBlank()) {
                stopShimmerAnimation()
                showImageError("No document URL available for preview.")
            } else {
                loadImage(imageUrl)
            }
        }
    }

    private fun loadImage(url: String) {
        android.util.Log.d("DocumentPreview", "Loading image URL: $url")

        stopShimmerAnimation()

        binding.ivDocumentImage.visibility = View.VISIBLE
        binding.ivDocumentImage.setImageDrawable(null)
        binding.layoutImageError.visibility = View.GONE
        binding.scrollPdfContainer.visibility = View.GONE

        startShimmerAnimation()

        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.bg_shimmer_placeholder)
            .listener(object : RequestListener<Drawable> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.e(
                        "DocumentPreview",
                        "Image loading failed. URL: $url",
                        e
                    )

                    e?.rootCauses?.forEach {
                        android.util.Log.e("DocumentPreview", "Root cause: ${it.message}", it)
                    }

                    stopShimmerAnimation()
                    showImageError("Unable to load document preview.")
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.d(
                        "DocumentPreview",
                        "Image loaded successfully. Source: $dataSource"
                    )

                    stopShimmerAnimation()
                    binding.layoutImageError.visibility = View.GONE
                    binding.ivDocumentImage.visibility = View.VISIBLE
                    return false
                }
            })
            .into(binding.ivDocumentImage)
    }


    private fun startShimmerAnimation() {
        binding.shimmerViewImage.visibility = View.VISIBLE
        if (shimmerAnimator == null) {
            shimmerAnimator = ObjectAnimator.ofFloat(binding.shimmerViewImage, "alpha", 0.3f, 1.0f).apply {
                duration = 800
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
            }
        } else if (shimmerAnimator?.isRunning != true) {
            shimmerAnimator?.start()
        }
    }

    private fun stopShimmerAnimation() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
        _binding?.shimmerViewImage?.visibility = View.GONE
    }

    private fun showImageError(message: String) {
        binding.ivDocumentImage.visibility = View.GONE
        binding.scrollPdfContainer.visibility = View.GONE
        binding.layoutImageError.visibility = View.VISIBLE
        binding.tvImageErrorMessage.text = message
    }

    override fun onDestroyView() {
        stopShimmerAnimation()
        _binding?.ivDocumentImage?.let {
            Glide.with(this).clear(it)
        }
        _binding?.layoutPdfPages?.removeAllViews()
        _binding = null
        super.onDestroyView()
    }
}
