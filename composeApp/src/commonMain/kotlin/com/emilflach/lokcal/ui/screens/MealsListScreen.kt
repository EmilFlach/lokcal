package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.theme.LocalRecipesColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsListScreen(
    repo: IntakeRepository,
    onBack: () -> Unit,
    onOpenMeal: (Long) -> Unit,
) {
    val colors = LocalRecipesColors.current
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
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(mealsState.value, key = { it.id }) { meal ->

                    ListItem(
                        headlineContent = { Text(meal.name) },
                        supportingContent = {
                            val portions = meal.total_portions
                            val (kcal, grams) = remember { repo.computeMealTotals(meal.id) }
                            Text("Portions: $portions, Kcal: $kcal, Grams: ${grams.toInt()}")
                        },
                        modifier = Modifier.clickable { onOpenMeal(meal.id) }
                    )
                }
            }
        }
    }
}
