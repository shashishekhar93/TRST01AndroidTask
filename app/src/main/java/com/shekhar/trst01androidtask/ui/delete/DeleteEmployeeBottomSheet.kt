package com.shekhar.trst01androidtask.ui.delete

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.databinding.DialogDeleteEmployeeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DeleteEmployeeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogDeleteEmployeeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeleteEmployeeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDeleteEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            viewModel.resetError()
            viewModel.confirmDelete()
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
    }

    private fun updateUiState(state: DeleteEmployeeUiState) {
        when (state) {
            is DeleteEmployeeUiState.Loading -> {
                binding.progressBarDeleting.visibility = View.VISIBLE
                binding.btnDelete.isEnabled = false
                binding.btnCancel.isEnabled = false
                binding.tvDeleteError.visibility = View.GONE
            }
            is DeleteEmployeeUiState.NotFound -> {
                Toast.makeText(requireContext(), "Employee not found", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            is DeleteEmployeeUiState.Content -> {
                val empName = state.employee.employeeName.orEmpty().ifBlank { "Employee" }
                val empId = state.employee.employeeId
                binding.tvConfirmationMessage.text =
                    "Are you sure you want to delete $empName (ID: #$empId)? This action cannot be undone. The employee's associated documents and saved map area will also be deleted."

                if (state.isDeleting) {
                    binding.progressBarDeleting.visibility = View.VISIBLE
                    binding.btnDelete.isEnabled = false
                    binding.btnCancel.isEnabled = false
                    binding.tvDeleteError.visibility = View.GONE
                } else {
                    binding.progressBarDeleting.visibility = View.GONE
                    binding.btnDelete.isEnabled = true
                    binding.btnCancel.isEnabled = true
                }

                if (state.deleteError != null) {
                    binding.tvDeleteError.visibility = View.VISIBLE
                    binding.tvDeleteError.text = state.deleteError
                } else {
                    binding.tvDeleteError.visibility = View.GONE
                }

                if (state.isSuccess) {
                    dismiss()
                    Toast.makeText(requireContext(), "Employee deleted successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack(R.id.employeeListFragment, false)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
