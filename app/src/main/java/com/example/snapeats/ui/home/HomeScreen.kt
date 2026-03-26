package com.example.snapeats.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snapeats.ui.components.FoodCard
import com.example.snapeats.ui.components.ProgressCircle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToScan: (source: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSourcePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SnapEats",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4CAF50)
                    )
                },
                actions = {
                    if (uiState.bmiCategory.isNotEmpty()) {
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = {
                                Text(
                                    text = uiState.bmiCategory,
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = uiState.bmiColor.copy(alpha = 0.9f),
                                labelColor = Color.White
                            ),
                            border = null,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSourcePicker = true },
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add meal",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0D1117), Color(0xFF1A1F2E))
                    )
                )
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        strokeWidth = 3.dp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF161B22)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ProgressCircle(
                                    currentCal = uiState.currentCal,
                                    targetCal = uiState.targetCal
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Calories Remaining",
                                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF8B949E)
                                )
                                Text(
                                    text = "${(uiState.targetCal - uiState.currentCal).coerceAtLeast(0)} kcal",
                                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }

                    // Grouped meal sections
                    if (uiState.mealsByType.isNotEmpty()) {
                        item {
                            Text(
                                text = "Today's Meals",
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6EDF3),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        val sectionOrder = listOf("Breakfast", "Lunch", "Snack", "Dinner", "Others")
                        sectionOrder.forEach { type ->
                            val mealsForType = uiState.mealsByType[type]
                            if (!mealsForType.isNullOrEmpty()) {
                                item(key = "header_$type") {
                                    MealTypeHeader(
                                        mealType = type,
                                        totalCal = mealsForType.sumOf { it.totalCal }
                                    )
                                }
                                items(
                                    items = mealsForType,
                                    key = { meal -> meal.id }
                                ) { meal ->
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        meal.foods.forEach { food ->
                                            FoodCard(food = food)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Empty state
                    if (uiState.mealsByType.isEmpty()) {
                        item {
                            EmptyMealsState()
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showSourcePicker) {
        ModalBottomSheet(
            onDismissRequest = { showSourcePicker = false },
            sheetState = sheetState,
            dragHandle = null,
            containerColor = Color(0xFF161B22),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Add a Meal",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6EDF3),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = {
                        Text("Take a photo", color = Color(0xFFE6EDF3))
                    },
                    supportingContent = {
                        Text("Use camera to scan your food", color = Color(0xFF8B949E))
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showSourcePicker = false
                                    onNavigateToScan("camera")
                                }
                            }
                        }
                )

                ListItem(
                    headlineContent = {
                        Text("Upload from gallery", color = Color(0xFFE6EDF3))
                    },
                    supportingContent = {
                        Text("Pick a photo from your device", color = Color(0xFF8B949E))
                    },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showSourcePicker = false
                                    onNavigateToScan("gallery")
                                }
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun MealTypeHeader(mealType: String, totalCal: Int) {
    val emoji = when (mealType) {
        "Breakfast" -> "🌅"
        "Lunch"     -> "☀️"
        "Snack"     -> "🍎"
        "Dinner"    -> "🌙"
        else        -> "🍽️"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F2937))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .background(Color(0xFF4CAF50))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$emoji $mealType",
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$totalCal kcal",
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
private fun EmptyMealsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🥗",
                style = androidx.compose.material3.MaterialTheme.typography.displayMedium
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Your plate is empty",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6EDF3)
                )
                Text(
                    text = "Tap + to log your first meal of the day",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8B949E)
                )
            }
        }
    }
}
