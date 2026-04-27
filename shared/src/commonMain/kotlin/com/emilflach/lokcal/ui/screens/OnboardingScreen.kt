package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emilflach.lokcal.theme.LocalRecipesColors

@Composable
internal fun OnboardingScreen(onGetStarted: () -> Unit) {
    val colors = LocalRecipesColors.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.backgroundPage
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Text(
                text = "Lokcal",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    color = colors.foregroundBrand
                )
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "A calorie tracker that is instant, uses real grocery data and respects your privacy",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = colors.foregroundDefault,
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Don't set a goal yet. Just track for four weeks to see how you actually eat. Learn the rhythm of weighing food at home and estimating while out.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.foregroundSupport,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            OnboardingStep(number = "1", text = "Log everything. Even (especially) the bad days.")
            Spacer(Modifier.height(16.dp))
            OnboardingStep(number = "2", text = "Weigh yourself daily. Do this for one month to find your baseline, then switch to weekly.")
            Spacer(Modifier.height(16.dp))
            OnboardingStep(number = "3", text = "Review the data. After 4 weeks, your stats are complete enough to start making informed changes.")

            Spacer(Modifier.weight(1.5f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.backgroundBrand,
                    contentColor = colors.onBackgroundBrand
                )
            ) {
                Text(
                    text = "Start Tracking",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "A starting calorie goal was set for you. Adjust it anytime in Settings.",
                style = MaterialTheme.typography.bodySmall.copy(color = colors.foregroundDisabled),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OnboardingStep(number: String, text: String) {
    val colors = LocalRecipesColors.current
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.labelLarge.copy(
                color = colors.foregroundBrand,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.width(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(color = colors.foregroundDefault)
        )
    }
}
