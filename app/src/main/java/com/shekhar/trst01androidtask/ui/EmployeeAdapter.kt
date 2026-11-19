package com.shekhar.trst01androidtask.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.databinding.ItemEmployeeBinding

class EmployeeAdapter : ListAdapter<EmployeeEntity, EmployeeAdapter.EmployeeViewHolder>(EmployeeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val binding = ItemEmployeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmployeeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EmployeeViewHolder(private val binding: ItemEmployeeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(employee: EmployeeEntity) {
            val name = employee.employeeName ?: "Unknown"
            binding.tvEmployeeName.text = name

            val initials = name.split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
            binding.tvAvatar.text = initials.ifEmpty { "?" }

            binding.tvDepartment.text = "Dept ID: ${employee.departmentId ?: "N/A"}"
            binding.tvEmployeeId.text = "ID: ${employee.employeeId} • Branch: ${employee.branchId ?: "N/A"}"

            val isActive = employee.isActive == true
            binding.chipStatus.text = if (isActive) "Active" else "Inactive"
            binding.chipStatus.setBackgroundResource(
                if (isActive) R.drawable.bg_status_chip else R.drawable.bg_status_chip_inactive
            )
        }
    }

    class EmployeeDiffCallback : DiffUtil.ItemCallback<EmployeeEntity>() {
        override fun areItemsTheSame(oldItem: EmployeeEntity, newItem: EmployeeEntity): Boolean {
            return oldItem.employeeId == newItem.employeeId
        }

        override fun areContentsTheSame(oldItem: EmployeeEntity, newItem: EmployeeEntity): Boolean {
            return oldItem == newItem
        }
    }
}
