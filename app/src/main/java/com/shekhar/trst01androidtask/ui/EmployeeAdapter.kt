package com.shekhar.trst01androidtask.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.databinding.ItemEmployeeBinding


class EmployeeAdapter(
    private val onViewDetailsClick: (EmployeeEntity) -> Unit
) : ListAdapter<EmployeeEntity, EmployeeAdapter.EmployeeViewHolder>(
    EmployeeDiffCallback()
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EmployeeViewHolder {
        val binding = ItemEmployeeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EmployeeViewHolder(binding, onViewDetailsClick)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EmployeeViewHolder(
        private val binding: ItemEmployeeBinding,
        private val onViewDetailsClick: (EmployeeEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(employee: EmployeeEntity) {
            val context = binding.root.context
            val name = employee.employeeName ?: "Unknown"

            binding.tvEmployeeName.text = name

            val initials = name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")

            binding.tvAvatar.text = initials.ifEmpty { "?" }

            binding.tvDepartment.text =
                "Dept ID: ${employee.departmentId ?: "N/A"}"

            binding.tvEmployeeId.text =
                "ID: ${employee.employeeId} • Branch: ${employee.branchId ?: "N/A"}"

            val isActive = employee.isActive == true

            binding.chipStatus.text = if (isActive) "Active" else "Inactive"

            if (isActive) {
                binding.chipStatus.setBackgroundResource(R.drawable.bg_status_chip)
                binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.primary_dark))
            } else {
                binding.chipStatus.setBackgroundResource(R.drawable.bg_status_chip_inactive)
                binding.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            binding.btnViewDetails.setOnClickListener {
                onViewDetailsClick(employee)
            }
        }
    }

    class EmployeeDiffCallback : DiffUtil.ItemCallback<EmployeeEntity>() {

        override fun areItemsTheSame(
            oldItem: EmployeeEntity,
            newItem: EmployeeEntity
        ): Boolean {
            return oldItem.employeeId == newItem.employeeId
        }

        override fun areContentsTheSame(
            oldItem: EmployeeEntity,
            newItem: EmployeeEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
