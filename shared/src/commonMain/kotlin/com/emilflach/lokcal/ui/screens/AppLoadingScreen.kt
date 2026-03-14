package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors

@Composable
internal fun AppLoadingScreen(seedingProgress: Float?) {
    val colors = LocalRecipesColors.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.backgroundPage
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (seedingProgress != null) {
                CircularProgressIndicator(
                    progress = { seedingProgress },
                    color = colors.backgroundBrand,
                    trackColor = colors.backgroundBrand.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Seeding data... ${(seedingProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.foregroundDefault
                )
            } else {
                CircularProgressIndicator(color = colors.backgroundBrand)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Initializing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.foregroundDefault
                )
            }
        }
    }
}
