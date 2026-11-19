package com.shekhar.trst01androidtask.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.shekhar.trst01androidtask.databinding.FragmentEditEmployeeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditEmployeeFragment : Fragment() {

    private var _binding: FragmentEditEmployeeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditEmployeeViewModel by viewModels()
    private var hasPrepopulated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            hasPrepopulated = savedInstanceState.getBoolean(KEY_HAS_PREPOPULATED, false)
        }

        setupListeners()
        observeViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HAS_PREPOPULATED, hasPrepopulated)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnErrorBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveEmployee.setOnClickListener {
            viewModel.resetError()
            viewModel.updateEmployee(
                name = binding.etEmployeeName.text.toString(),
                departmentIdStr = binding.etDepartmentId.text.toString(),
                branchIdStr = binding.etBranchId.text.toString(),
                managerIdStr = binding.etManagerId.text.toString(),
                isActive = binding.switchIsActive.isChecked
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.validationState.collectLatest { validation ->
                        updateValidationErrors(validation)
                    }
                }

                launch {
                    viewModel.uiState.collectLatest { state ->
                        updateUiState(state)
                    }
                }
            }
        }
    }

    private fun updateValidationErrors(validation: EditEmployeeFormValidation) {
        if (validation.nameError != null) {
            binding.tvErrorName.visibility = View.VISIBLE
            binding.tvErrorName.text = validation.nameError
        } else {
            binding.tvErrorName.visibility = View.GONE
        }

        if (validation.departmentError != null) {
            binding.tvErrorDepartmentId.visibility = View.VISIBLE
            binding.tvErrorDepartmentId.text = validation.departmentError
        } else {
            binding.tvErrorDepartmentId.visibility = View.GONE
        }

        if (validation.branchError != null) {
            binding.tvErrorBranchId.visibility = View.VISIBLE
            binding.tvErrorBranchId.text = validation.branchError
        } else {
            binding.tvErrorBranchId.visibility = View.GONE
        }

        if (validation.managerError != null) {
            binding.tvErrorManagerId.visibility = View.VISIBLE
            binding.tvErrorManagerId.text = validation.managerError
        } else {
            binding.tvErrorManagerId.visibility = View.GONE
        }
    }

    private fun updateUiState(state: EditEmployeeUiState) {
        when (state) {
            is EditEmployeeUiState.Loading -> {
                binding.progressBarInitialLoading.visibility = View.VISIBLE
                binding.scrollViewForm.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.layoutSavingLoading.visibility = View.GONE
            }
            is EditEmployeeUiState.NotFound -> {
                binding.progressBarInitialLoading.visibility = View.GONE
                binding.scrollViewForm.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.layoutSavingLoading.visibility = View.GONE
                binding.tvErrorMessage.text = "Employee not found."
            }
            is EditEmployeeUiState.Error -> {
                binding.progressBarInitialLoading.visibility = View.GONE
                binding.scrollViewForm.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.layoutSavingLoading.visibility = View.GONE
                binding.tvErrorMessage.text = state.message
            }
            is EditEmployeeUiState.Content -> {
                binding.progressBarInitialLoading.visibility = View.GONE
                binding.scrollViewForm.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE

                if (!hasPrepopulated) {
                    binding.etEmployeeName.setText(state.employee.employeeName.orEmpty())
                    binding.etDepartmentId.setText(state.employee.departmentId?.toString().orEmpty())
                    binding.etBranchId.setText(state.employee.branchId?.toString().orEmpty())
                    binding.etManagerId.setText(state.employee.managerId?.toString().orEmpty())
                    binding.switchIsActive.isChecked = state.employee.isActive == true
                    binding.tvEmployeeIdHeader.text = "Editing details for Employee ID: #${state.employee.employeeId}"
                    hasPrepopulated = true
                }

                if (state.isSaving) {
                    binding.layoutSavingLoading.visibility = View.VISIBLE
                    binding.btnSaveEmployee.isEnabled = false
                } else {
                    binding.layoutSavingLoading.visibility = View.GONE
                    binding.btnSaveEmployee.isEnabled = !state.isSuccess
                }

                if (state.saveError != null) {
                    binding.tvGeneralError.visibility = View.VISIBLE
                    binding.tvGeneralError.text = state.saveError
                } else {
                    binding.tvGeneralError.visibility = View.GONE
                }

                if (state.isSuccess) {
                    Toast.makeText(requireContext(), "Employee updated successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val KEY_HAS_PREPOPULATED = "has_prepopulated"
    }
}
