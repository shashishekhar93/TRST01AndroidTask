package com.shekhar.trst01androidtask.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.shekhar.trst01androidtask.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: EmployeeViewModel by viewModels()
    private lateinit var employeeAdapter: EmployeeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSwipeRefresh()
        setupSearch()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        employeeAdapter = EmployeeAdapter()
        binding.recyclerViewEmployees.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
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
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        updateUiState(state)
                    }
                }

                launch {
                    viewModel.employees.collectLatest { list ->
                        employeeAdapter.submitList(list)
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
                            Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG)
                                .setAction("Retry") { viewModel.refresh() }
                                .show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
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
                binding.recyclerViewEmployees.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
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
}
