package com.emilflach.lokcal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.emilflach.lokcal.camera.CameraManager
import com.emilflach.lokcal.camera.RequestCameraPermission
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@Composable
fun ScannerOverlay(
    viewModel: IntakeViewModel,
    waitingForCameraPermission: Boolean,
    onPermissionResult: (Boolean) -> Unit
) {
    val state = viewModel.state.collectAsState().value

    if (waitingForCameraPermission) {
        RequestCameraPermission { granted ->
            CameraManager.setPermissionsGranted(granted)
            onPermissionResult(granted)
            if (granted) viewModel.setShowScanner(true)
        }
    }

    if (state.showScanner) {
        ScannerViewContainer(
            onScan = viewModel::setQuery,
            onClose = { viewModel.setShowScanner(false) }
        )
    }
}
