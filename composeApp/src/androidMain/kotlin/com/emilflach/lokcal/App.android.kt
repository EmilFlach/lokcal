package com.emilflach.lokcal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.emilflach.lokcal.data.SqlDriverFactory
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(sqlDriverFactory = SqlDriverFactory(applicationContext)) }
        FileKit.init(this)
    }
}
