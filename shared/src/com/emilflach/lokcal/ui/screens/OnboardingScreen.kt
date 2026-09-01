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
import lokcal.shared.generated.resources.Res
import lokcal.shared.generated.resources.onboarding_body
import lokcal.shared.generated.resources.onboarding_footnote
import lokcal.shared.generated.resources.onboarding_start_tracking
import lokcal.shared.generated.resources.onboarding_step1
import lokcal.shared.generated.resources.onboarding_step2
import lokcal.shared.generated.resources.onboarding_step3
import lokcal.shared.generated.resources.onboarding_tagline
import lokcal.shared.generated.resources.onboarding_title
import org.jetbrains.compose.resources.stringResource

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
                text = stringResource(Res.string.onboarding_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    color = colors.foregroundBrand
                )
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.onboarding_tagline),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = colors.foregroundDefault,
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = stringResource(Res.string.onboarding_body),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.foregroundSupport,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            OnboardingStep(number = "1", text = stringResource(Res.string.onboarding_step1))
            Spacer(Modifier.height(16.dp))
            OnboardingStep(number = "2", text = stringResource(Res.string.onboarding_step2))
            Spacer(Modifier.height(16.dp))
            OnboardingStep(number = "3", text = stringResource(Res.string.onboarding_step3))

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
                    text = stringResource(Res.string.onboarding_start_tracking),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.onboarding_footnote),
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
