package com.emilflach.lokcal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.emilflach.lokcal.data.SqlDriverFactory

class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(sqlDriverFactory = SqlDriverFactory(applicationContext)) }
    }
}
