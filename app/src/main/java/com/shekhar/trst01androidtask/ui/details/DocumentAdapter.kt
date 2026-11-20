package com.shekhar.trst01androidtask.ui.details

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.databinding.ItemDocumentBinding
import com.shekhar.trst01androidtask.util.DateFormatter
import com.shekhar.trst01androidtask.util.DocumentUtils

class DocumentAdapter(
    private val onDocumentClick: (DocumentEntity) -> Unit
) : ListAdapter<DocumentEntity, DocumentAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DocumentViewHolder(binding, onDocumentClick)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DocumentViewHolder(
        private val binding: ItemDocumentBinding,
        private val onDocumentClick: (DocumentEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: DocumentEntity) {
            val context = itemView.context
            binding.tvDocumentName.text = document.documentName ?: "Untitled Document"
            binding.tvDocumentType.text = "Type: ${document.documentType ?: "N/A"}"
            val dateText = DateFormatter.formatDate(document.createdDate ?: document.updatedDate)
            binding.tvDocumentDate.text = "Date: $dateText"

            val isVerified = document.isVerified == true
            binding.chipVerification.text = if (isVerified) "Verified" else "Pending"
            if (isVerified) {
                binding.chipVerification.setBackgroundResource(R.drawable.bg_status_chip)
                binding.chipVerification.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
            } else {
                binding.chipVerification.setBackgroundResource(R.drawable.bg_status_chip_inactive)
                binding.chipVerification.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            val isPdf = DocumentUtils.isPdfDocument(document.documentType, document.documentUrl, document.thumbnailUrl)

            if (isPdf) {
                Glide.with(context).clear(binding.ivThumbnail as View)
                binding.ivThumbnail.setImageResource(R.drawable.ic_document_pdf)
                binding.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_INSIDE
                binding.ivThumbnail.setBackgroundResource(R.drawable.bg_shimmer_placeholder)
            } else {
                val imageUrl = DocumentUtils.getPreviewUrl(document)
                binding.ivThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.ivThumbnail.setBackgroundResource(R.drawable.bg_shimmer_placeholder)
                Glide.with(binding.ivThumbnail.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_shimmer_placeholder)
                    .error(R.drawable.bg_shimmer_placeholder)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            }

            binding.root.setOnClickListener {
                onDocumentClick(document)
            }
        }
    }

    class DocumentDiffCallback : DiffUtil.ItemCallback<DocumentEntity>() {
        override fun areItemsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity): Boolean {
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity): Boolean {
            return oldItem == newItem
        }
    }
}
