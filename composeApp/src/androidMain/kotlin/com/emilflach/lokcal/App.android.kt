package com.emilflach.lokcal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.emilflach.lokcal.data.SqlDriverFactory
import com.emilflach.lokcal.health.HealthManager
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppActivity : ComponentActivity() {

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    private val healthPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(permissions)) {
            HealthManager.setPermissionsGranted(true)
        }
    }

    private var healthConnectClient: HealthConnectClient? = null
    private val activityScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(sqlDriverFactory = SqlDriverFactory(applicationContext)) }
        FileKit.init(this)

        healthConnectClient = getHealthClientOrNull()
        healthConnectClient?.let { client ->
            HealthManager.setHealthProvider(client)
        }
    }

    override fun onResume() {
        super.onResume()
        healthConnectClient?.let { client ->
            activityScope.launch {
                val granted = client.permissionController.getGrantedPermissions()
                if (granted.containsAll(permissions)) {
                    HealthManager.setPermissionsGranted(true)
                } else {
                    healthPermissions.launch(permissions)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    private fun getHealthClientOrNull(): HealthConnectClient? {
        val status = HealthConnectClient.getSdkStatus(this, "com.google.android.apps.healthdata")
        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectClient.getOrCreate(this)
            HealthConnectClient.SDK_UNAVAILABLE,
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> null
            else -> null
        }
    }
}
