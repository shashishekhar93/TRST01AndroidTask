package com.shekhar.trst01androidtask.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.data.local.entity.DocumentEntity
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.data.local.entity.MapAreaEntity
import com.shekhar.trst01androidtask.databinding.FragmentEmployeeDetailsBinding
import com.shekhar.trst01androidtask.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmployeeDetailsFragment : Fragment() {

    private var _binding: FragmentEmployeeDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EmployeeDetailsViewModel by viewModels()
    private lateinit var documentAdapter: DocumentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnErrorBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnEdit.setOnClickListener {
            val bundle = bundleOf("employeeId" to viewModel.employeeId)
            findNavController().navigate(
                R.id.action_employeeDetailsFragment_to_editEmployeeFragment,
                bundle
            )
        }
        binding.btnDelete.setOnClickListener {
            val bundle = bundleOf("employeeId" to viewModel.employeeId)
            findNavController().navigate(
                R.id.action_employeeDetailsFragment_to_deleteEmployeeBottomSheet,
                bundle
            )
        }
        binding.btnOpenMapArea.setOnClickListener {
            val bundle = bundleOf("employeeId" to viewModel.employeeId)
            findNavController().navigate(
                R.id.action_employeeDetailsFragment_to_employeeMapFragment,
                bundle
            )
        }
    }

    private fun setupRecyclerView() {
        documentAdapter = DocumentAdapter { document ->
            val bundle = bundleOf(
                "employeeId" to viewModel.employeeId,
                "documentId" to document.documentId
            )
            findNavController().navigate(
                R.id.action_employeeDetailsFragment_to_documentPreviewFragment,
                bundle
            )
        }
        binding.rvDocuments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = documentAdapter
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

    private fun updateUiState(state: EmployeeDetailsUiState) {
        when (state) {
            is EmployeeDetailsUiState.Loading -> {
                binding.progressBarLoading.visibility = View.VISIBLE
                binding.scrollViewContent.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
            }
            is EmployeeDetailsUiState.NotFound -> {
                binding.progressBarLoading.visibility = View.GONE
                binding.scrollViewContent.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = "Employee not found."
            }
            is EmployeeDetailsUiState.Error -> {
                binding.progressBarLoading.visibility = View.GONE
                binding.scrollViewContent.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
            is EmployeeDetailsUiState.Success -> {
                binding.progressBarLoading.visibility = View.GONE
                binding.scrollViewContent.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE

                bindEmployeeInfo(state.employee)
                bindDocumentsInfo(state.documents)
                bindMapAreaInfo(state.mapArea)
            }
        }
    }

    private fun bindMapAreaInfo(mapArea: MapAreaEntity?) {
        if (mapArea != null && mapArea.coordinates.isNotEmpty()) {
            val count = mapArea.coordinates.size
            val pointText = if (count == 1) "1 point" else "$count points"
            binding.tvMapAreaStatus.text = "Area saved ($pointText)"
        } else {
            binding.tvMapAreaStatus.text = "No area saved"
        }
    }

    private fun bindEmployeeInfo(employee: EmployeeEntity) {
        val name = employee.employeeName ?: "Unknown"
        binding.tvEmployeeName.text = name

        val initials = name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
        binding.tvAvatar.text = initials.ifEmpty { "?" }

        binding.tvEmployeeIdSubtitle.text = "Employee ID: #${employee.employeeId}"

        val isActive = employee.isActive == true
        binding.chipStatus.text = if (isActive) "Active" else "Inactive"
        binding.chipStatus.setBackgroundResource(
            if (isActive) R.drawable.bg_status_chip else R.drawable.bg_status_chip_inactive
        )

        binding.tvDepartmentId.text = employee.departmentId?.toString() ?: "N/A"
        binding.tvBranchId.text = employee.branchId?.toString() ?: "N/A"
        binding.tvManagerId.text = employee.managerId?.toString() ?: "N/A"
        binding.tvCreatedDate.text = DateFormatter.formatDate(employee.createdDate)
        binding.tvUpdatedDate.text = DateFormatter.formatDate(employee.updatedDate)
    }

    private fun bindDocumentsInfo(documents: List<DocumentEntity>) {
        documentAdapter.submitList(documents)

        if (documents.isEmpty()) {
            binding.layoutDocumentsEmpty.visibility = View.VISIBLE
            binding.rvDocuments.visibility = View.GONE
            binding.tvDocumentCount.text = "0 Documents"
        } else {
            binding.layoutDocumentsEmpty.visibility = View.GONE
            binding.rvDocuments.visibility = View.VISIBLE
            val countText = if (documents.size == 1) "1 Document" else "${documents.size} Documents"
            binding.tvDocumentCount.text = countText
        }
    }

    override fun onDestroyView() {
        binding.rvDocuments.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
