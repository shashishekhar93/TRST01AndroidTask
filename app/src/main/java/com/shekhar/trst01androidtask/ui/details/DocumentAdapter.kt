package com.shekhar.trst01androidtask.ui.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.databinding.ItemDocumentBinding
import com.shekhar.trst01androidtask.util.DateFormatter

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
            binding.tvDocumentName.text = document.documentName ?: "Untitled Document"
            binding.tvDocumentType.text = "Type: ${document.documentType ?: "N/A"}"
            val dateText = DateFormatter.formatDate(document.createdDate ?: document.updatedDate)
            binding.tvDocumentDate.text = "Date: $dateText"

            val isVerified = document.isVerified == true
            binding.chipVerification.text = if (isVerified) "Verified" else "Pending"
            binding.chipVerification.setBackgroundResource(
                if (isVerified) R.drawable.bg_status_chip else R.drawable.bg_status_chip_inactive
            )

            val imageUrl = document.thumbnailUrl.takeIf { !it.isNullResourceOrBlank() }
                ?: document.documentUrl.takeIf { !it.isNullResourceOrBlank() }

            Glide.with(binding.ivThumbnail.context)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(binding.ivThumbnail)

            binding.root.setOnClickListener {
                onDocumentClick(document)
            }
        }

        private fun String?.isNullResourceOrBlank(): Boolean {
            return this.isNullOrBlank() || this.equals("null", ignoreCase = true)
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
