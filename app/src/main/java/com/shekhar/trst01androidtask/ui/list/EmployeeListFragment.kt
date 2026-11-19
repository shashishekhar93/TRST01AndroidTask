package com.shekhar.trst01androidtask.ui.list

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.data.local.entity.EmployeeEntity
import com.shekhar.trst01androidtask.databinding.FragmentEmployeeListBinding
import com.shekhar.trst01androidtask.ui.EmployeeAdapter
import com.shekhar.trst01androidtask.ui.EmployeeUiState
import com.shekhar.trst01androidtask.ui.EmployeeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmployeeListFragment : Fragment() {

    private var _binding: FragmentEmployeeListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EmployeeViewModel by activityViewModels()

    private lateinit var employeeAdapter: EmployeeAdapter

    private var selectedBranchId: Int? = null
    private var availableBranchIds: List<Int> = emptyList()

    private var displayedEmployees: List<EmployeeEntity> = emptyList()

    companion object {
        private const val KEY_SELECTED_BRANCH = "selected_branch_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedBranchId = savedInstanceState?.let {
            if (it.containsKey(KEY_SELECTED_BRANCH)) {
                it.getInt(KEY_SELECTED_BRANCH)
            } else {
                null
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        selectedBranchId?.let {
            outState.putInt(KEY_SELECTED_BRANCH, it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeRefresh()
        setupSearch()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        employeeAdapter = EmployeeAdapter { employee ->
            val bundle = bundleOf("employeeId" to employee.employeeId)

            findNavController().navigate(
                R.id.action_employeeListFragment_to_employeeDetailsFragment,
                bundle
            )
        }

        binding.recyclerViewEmployees.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = employeeAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }
    }

    private fun setupListeners() {
        binding.btnRetry.setOnClickListener {
            viewModel.refresh()
        }

        binding.fabAddEmployee.setOnClickListener {
            findNavController().navigate(
                R.id.action_employeeListFragment_to_addEmployeeFragment
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiState.collectLatest { state ->
                        updateUiState(state)
                    }
                }

                launch {
                    viewModel.allEmployees.collectLatest { employees ->

                        availableBranchIds = employees
                            .mapNotNull { it.branchId }
                            .distinct()
                            .sorted()

                        // If the selected branch no longer exists, select All.
                        if (
                            selectedBranchId != null &&
                            selectedBranchId !in availableBranchIds
                        ) {
                            selectedBranchId = null
                        }

                        renderBranchTabs()
                        submitFilteredEmployees(viewModel.employees.value)
                    }
                }

                launch {
                    viewModel.employees.collectLatest { employees ->
                        submitFilteredEmployees(employees)
                    }
                }

                launch {
                    viewModel.isRefreshing.collectLatest { isRefreshing ->
                        binding.swipeRefreshLayout.isRefreshing = isRefreshing
                    }
                }

                launch {
                    viewModel.error.collectLatest { errorMessage ->
                        if (!errorMessage.isNullOrBlank()) {
                            Snackbar.make(
                                binding.root,
                                errorMessage,
                                Snackbar.LENGTH_LONG
                            )
                                .setAction("Retry") {
                                    viewModel.refresh()
                                }
                                .show()

                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun renderBranchTabs() {
        binding.layoutBranchTabs.removeAllViews()

        // First tab: All employees.
        addBranchTab(
            title = "All",
            branchId = null
        )

        // Remaining tabs: Actual branch IDs from the database.
        availableBranchIds.forEach { branchId ->
            addBranchTab(
                title = "Branch $branchId",
                branchId = branchId
            )
        }
    }

    private fun addBranchTab(
        title: String,
        branchId: Int?
    ) {
        val isSelected = selectedBranchId == branchId

        val tab = TextView(requireContext()).apply {
            text = title
            textSize = 14f
            setTextColor(
                if (isSelected) Color.WHITE else Color.rgb(55, 65, 81)
            )

            gravity = android.view.Gravity.CENTER

            setPadding(
                dpToPx(18),
                dpToPx(10),
                dpToPx(18),
                dpToPx(10)
            )

            background = GradientDrawable().apply {
                cornerRadius = dpToPx(24).toFloat()

                setColor(
                    if (isSelected) {
                        Color.rgb(37, 99, 235)
                    } else {
                        Color.rgb(243, 244, 246)
                    }
                )

                if (!isSelected) {
                    setStroke(
                        dpToPx(1),
                        Color.rgb(209, 213, 219)
                    )
                }
            }

            isClickable = true
            isFocusable = true
        }

        val params = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = dpToPx(8)
        }

        binding.layoutBranchTabs.addView(tab, params)

        tab.setOnClickListener {
            if (selectedBranchId != branchId) {
                selectedBranchId = branchId

                renderBranchTabs()

                submitFilteredEmployees(viewModel.employees.value)
            }
        }
    }

    private fun submitFilteredEmployees(
        searchFilteredEmployees: List<EmployeeEntity>
    ) {
        displayedEmployees = if (selectedBranchId == null) {
            searchFilteredEmployees
        } else {
            searchFilteredEmployees.filter {
                it.branchId == selectedBranchId
            }
        }

        employeeAdapter.submitList(displayedEmployees)

        updateFilteredContentVisibility()
    }

    private fun updateFilteredContentVisibility() {
        if (viewModel.uiState.value !is EmployeeUiState.Success) {
            return
        }

        val hasEmployees = displayedEmployees.isNotEmpty()

        binding.recyclerViewEmployees.visibility =
            if (hasEmployees) View.VISIBLE else View.GONE

        binding.layoutEmpty.visibility =
            if (hasEmployees) View.GONE else View.VISIBLE

        binding.layoutError.visibility = View.GONE
        binding.shimmerView.root.visibility = View.GONE
    }

    private fun updateUiState(state: EmployeeUiState) {
        when (state) {
            is EmployeeUiState.Loading -> {
                binding.shimmerView.root.visibility = View.VISIBLE
                binding.recyclerViewEmployees.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
            }

            is EmployeeUiState.Success -> {
                binding.shimmerView.root.visibility = View.GONE
                updateFilteredContentVisibility()
            }

            is EmployeeUiState.Empty -> {
                binding.shimmerView.root.visibility = View.GONE
                binding.recyclerViewEmployees.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
            }

            is EmployeeUiState.Error -> {
                binding.shimmerView.root.visibility = View.GONE
                binding.recyclerViewEmployees.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    private fun dpToPx(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        binding.recyclerViewEmployees.adapter = null
        _binding = null
        super.onDestroyView()
    }
}