package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.getRoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MealsListScreen(
    repo: IntakeRepository,
    onBack: () -> Unit,
    onOpenMeal: (Long) -> Unit,
) {
    val colors = LocalRecipesColors.current

    BackHandler {
        onBack()
    }
    val mealsState = remember { mutableStateOf(emptyList<Meal>()) }

    LaunchedEffect(Unit) {
        mealsState.value = repo.listAllMeals()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meals") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundPage,
                    titleContentColor = colors.foregroundDefault,
                    navigationIconContentColor = colors.foregroundDefault,
                    actionIconContentColor = colors.foregroundDefault,
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(mealsState.value, key = { it.id }) { meal ->

                    ListItem(
                        headlineContent = { Text(meal.name) },
                        supportingContent = {
                            val portions = meal.total_portions
                            val (kcal, grams) = remember { repo.computeMealTotals(meal.id) }
                            Text("Portions: $portions, Kcal: $kcal, Grams: ${grams.toInt()}")
                        },
                        modifier = Modifier.clip(getRoundedCornerShape(
                            index = mealsState.value.indexOf(meal),
                            size = mealsState.value.size
                        )).clickable { onOpenMeal(meal.id) }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
