package com.shekhar.trst01androidtask.ui.preview

import android.animation.ObjectAnimator
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnErrorBack.setOnClickListener {
            findNavController().navigateUp()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUiState(state)
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

    private fun bindDocument(document: DocumentEntity) {
        val title = document.documentName ?: "Document Preview"
        binding.tvTitle.text = title
        binding.tvDocumentName.text = title
        binding.tvDocumentType.text = "Type: ${document.documentType ?: "N/A"}"

        val dateText = DateFormatter.formatDate(document.createdDate ?: document.updatedDate)
        binding.tvDocumentDate.text = "Date: $dateText"

        val isVerified = document.isVerified == true
        binding.chipStatus.text = if (isVerified) "Verified" else "Pending"
        binding.chipStatus.setBackgroundResource(
            if (isVerified) R.drawable.bg_status_chip else R.drawable.bg_status_chip_inactive
        )

        val (imageUrl, isPdf) = evaluateDocumentUrl(document)

        when {
            isPdf -> {
                stopShimmerAnimation()
                showImageError("PDF document preview is not supported in this version.")
            }
            imageUrl.isNullOrBlank() -> {
                stopShimmerAnimation()
                showImageError("No document URL available for preview.")
            }
            else -> {
                loadImage(imageUrl)
            }
        }
    }

    private fun evaluateDocumentUrl(document: DocumentEntity): Pair<String?, Boolean> {
        val docUrl = document.documentUrl?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        val thumbUrl = document.thumbnailUrl?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

        fun isPdfUrl(url: String?): Boolean {
            if (url == null) return false
            val cleanUrl = url.substringBefore('?').substringBefore('#')
            return cleanUrl.endsWith(".pdf", ignoreCase = true)
        }

        if (document.documentType?.equals("PDF", ignoreCase = true) == true) {
            return Pair(null, true)
        }
        if (docUrl != null && isPdfUrl(docUrl)) {
            return Pair(null, true)
        }
        if (thumbUrl != null && isPdfUrl(thumbUrl)) {
            return Pair(null, true)
        }

        val targetUrl = docUrl ?: thumbUrl
        return Pair(targetUrl, false)
    }

    private fun loadImage(url: String) {
        startShimmerAnimation()
        binding.ivDocumentImage.visibility = View.GONE
        binding.layoutImageError.visibility = View.GONE

        Glide.with(binding.ivDocumentImage.context)
            .load(url)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    stopShimmerAnimation()
                    showImageError("Unable to load document preview. Please try again.")
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
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
        binding.layoutImageError.visibility = View.VISIBLE
        binding.tvImageErrorMessage.text = message
    }

    override fun onDestroyView() {
        stopShimmerAnimation()
        _binding?.ivDocumentImage?.let {
            Glide.with(this).clear(it)
        }
        _binding = null
        super.onDestroyView()
    }
}
