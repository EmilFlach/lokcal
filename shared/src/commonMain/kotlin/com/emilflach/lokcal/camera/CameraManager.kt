package com.emilflach.lokcal.camera

object CameraManager {
    private var permissionsGranted: Boolean = false

    fun setPermissionsGranted(granted: Boolean) {
        permissionsGranted = granted
    }

    fun arePermissionsGranted(): Boolean = permissionsGranted
}