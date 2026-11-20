package com.shekhar.trst01androidtask.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.shekhar.trst01androidtask.R
import com.shekhar.trst01androidtask.data.local.converter.LatLngPoint
import com.shekhar.trst01androidtask.databinding.FragmentEmployeeMapBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmployeeMapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentEmployeeMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EmployeeMapViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private val markers = mutableListOf<Marker>()
    private var currentPolyline: Polyline? = null
    private var currentPolygon: Polygon? = null

    private var hasInitialCameraMoved = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        initMap()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            handleBackPress()
        }
        binding.btnErrorBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnSaveArea.setOnClickListener {
            viewModel.saveMapArea()
        }
        binding.btnClearArea.setOnClickListener {
            showClearConfirmationDialog()
        }
        binding.btnRemovePoint.setOnClickListener {
            viewModel.removeSelectedPoint()
        }
    }

    private fun initMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isCompassEnabled = true

        googleMap?.setOnMapClickListener { latLng ->
            viewModel.addPoint(LatLngPoint(latLng.latitude, latLng.longitude))
        }

        googleMap?.setOnMarkerClickListener { marker ->
            val index = marker.tag as? Int
            if (index != null) {
                viewModel.selectPoint(index)
            }
            true
        }

        googleMap?.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                val index = marker.tag as? Int
                if (index != null) {
                    viewModel.selectPoint(index)
                }
            }

            override fun onMarkerDrag(marker: Marker) {
            }

            override fun onMarkerDragEnd(marker: Marker) {
                val index = marker.tag as? Int
                if (index != null) {
                    viewModel.movePoint(
                        index,
                        LatLngPoint(marker.position.latitude, marker.position.longitude)
                    )
                }
            }
        })

        val state = viewModel.uiState.value
        if (state is EmployeeMapUiState.Success) {
            updateMapGraphics(state)
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

    private fun updateUiState(state: EmployeeMapUiState) {
        when (state) {
            is EmployeeMapUiState.Loading -> {
                binding.progressBarLoading.visibility = View.VISIBLE
                binding.layoutContent.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
            }
            is EmployeeMapUiState.NotFound -> {
                binding.progressBarLoading.visibility = View.GONE
                binding.layoutContent.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = "Employee not found."
            }
            is EmployeeMapUiState.Success -> {
                binding.progressBarLoading.visibility = if (state.isSaving || state.isClearing) View.VISIBLE else View.GONE
                binding.layoutContent.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE

                val empName = state.employeeName ?: "Employee"
                binding.tvHeaderTitle.text = "Map Area: $empName"
                binding.tvEmployeeIdSubtitle.text = "Employee ID: #${state.employeeId}"

                updateInstructionAndButtons(state)
                updateMapGraphics(state)

                if (state.validationError != null) {
                    Toast.makeText(requireContext(), state.validationError, Toast.LENGTH_SHORT).show()
                    viewModel.clearUserMessages()
                } else if (state.userMessage != null) {
                    Toast.makeText(requireContext(), state.userMessage, Toast.LENGTH_SHORT).show()
                    viewModel.clearUserMessages()
                } else if (state.errorMessage != null) {
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                    viewModel.clearUserMessages()
                }
            }
        }
    }

    private fun updateInstructionAndButtons(state: EmployeeMapUiState.Success) {
        val count = state.points.size
        binding.tvPointCount.text = "$count Point${if (count == 1) "" else "s"}"

        val instructionText = when {
            count == 0 -> "Tap anywhere on the map to add boundary points."
            count < 3 -> "Add at least 3 points to create a closed polygon ($count/3 points added)."
            state.selectedIndex != null -> "Point ${state.selectedIndex + 1} selected. Drag to move or click 'Remove Point'."
            else -> "Polygon complete ($count points). Drag markers to adjust boundary."
        }
        binding.tvInstruction.text = instructionText

        binding.btnRemovePoint.visibility = if (state.selectedIndex != null) View.VISIBLE else View.GONE
        binding.btnSaveArea.isEnabled = !state.isSaving && !state.isClearing
        binding.btnClearArea.isEnabled = !state.isSaving && !state.isClearing && (state.isSavedInDb || count > 0)
    }

    private fun updateMapGraphics(state: EmployeeMapUiState.Success) {
        val map = googleMap ?: return

        markers.forEach { it.remove() }
        markers.clear()
        currentPolyline?.remove()
        currentPolyline = null
        currentPolygon?.remove()
        currentPolygon = null

        val points = state.points
        if (points.isEmpty()) {
            if (!hasInitialCameraMoved) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(20.5937, 78.9629), 4f))
                hasInitialCameraMoved = true
            }
            return
        }

        val latLngs = points.map { LatLng(it.latitude, it.longitude) }

        latLngs.forEachIndexed { index, latLng ->
            val isSelected = (index == state.selectedIndex)
            val markerOptions = MarkerOptions()
                .position(latLng)
                .draggable(true)
                .title("Point ${index + 1}")
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        if (isSelected) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_RED
                    )
                )

            val marker = map.addMarker(markerOptions)
            if (marker != null) {
                marker.tag = index
                markers.add(marker)
            }
        }

        if (latLngs.size < 3) {
            val polylineOptions = PolylineOptions()
                .addAll(latLngs)
                .color(Color.BLUE)
                .width(6f)
            currentPolyline = map.addPolyline(polylineOptions)
        } else {
            val polygonOptions = PolygonOptions()
                .addAll(latLngs)
                .strokeColor(Color.parseColor("#1E88E5"))
                .fillColor(Color.parseColor("#331E88E5"))
                .strokeWidth(6f)
            currentPolygon = map.addPolygon(polygonOptions)
        }

        if (!hasInitialCameraMoved && latLngs.isNotEmpty()) {
            hasInitialCameraMoved = true
            if (latLngs.size == 1) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngs[0], 15f))
            } else {
                val boundsBuilder = LatLngBounds.Builder()
                latLngs.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            }
        }
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Map Area")
            .setMessage("Are you sure you want to clear the polygon area for this employee?")
            .setPositiveButton("Clear") { _, _ ->
                viewModel.clearMapArea()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleBackPress() {
        val state = viewModel.uiState.value
        if (state is EmployeeMapUiState.Success && state.hasUnsavedChanges) {
            AlertDialog.Builder(requireContext())
                .setTitle("Discard Unsaved Changes?")
                .setMessage("You have unsaved changes to the map area. Do you want to discard them?")
                .setPositiveButton("Discard") { _, _ ->
                    findNavController().navigateUp()
                }
                .setNegativeButton("Keep Editing", null)
                .show()
        } else {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        markers.forEach { it.remove() }
        markers.clear()
        currentPolyline?.remove()
        currentPolyline = null
        currentPolygon?.remove()
        currentPolygon = null
        googleMap = null
        _binding = null
        super.onDestroyView()
    }
}
