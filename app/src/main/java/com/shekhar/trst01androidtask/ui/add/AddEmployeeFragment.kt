package com.shekhar.trst01androidtask.ui.add

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
import com.shekhar.trst01androidtask.databinding.FragmentAddEmployeeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEmployeeFragment : Fragment() {

    private var _binding: FragmentAddEmployeeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEmployeeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSaveEmployee.setOnClickListener {
            viewModel.resetError()
            viewModel.saveEmployee(
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

    private fun updateValidationErrors(validation: AddEmployeeFormValidation) {
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

    private fun updateUiState(state: AddEmployeeUiState) {
        when (state) {
            is AddEmployeeUiState.Idle -> {
                binding.layoutLoading.visibility = View.GONE
                binding.btnSaveEmployee.isEnabled = true
                binding.tvGeneralError.visibility = View.GONE
            }
            is AddEmployeeUiState.Loading -> {
                binding.layoutLoading.visibility = View.VISIBLE
                binding.btnSaveEmployee.isEnabled = false
                binding.tvGeneralError.visibility = View.GONE
            }
            is AddEmployeeUiState.Success -> {
                binding.layoutLoading.visibility = View.GONE
                binding.btnSaveEmployee.isEnabled = false
                Toast.makeText(requireContext(), "Employee added successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            is AddEmployeeUiState.Error -> {
                binding.layoutLoading.visibility = View.GONE
                binding.btnSaveEmployee.isEnabled = true
                binding.tvGeneralError.visibility = View.VISIBLE
                binding.tvGeneralError.text = state.message
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
