package com.example.snapeats.ui.recs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snapeats.domain.model.Food
import com.example.snapeats.ui.components.FoodCard

// ---------------------------------------------------------------------------
// Meal type filter options
// ---------------------------------------------------------------------------
private val MEAL_TYPE_FILTERS = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")

// ---------------------------------------------------------------------------
// Entry point composable
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecsScreen(
    viewModel: RecsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedMealType by viewModel.selectedMealType.collectAsStateWithLifecycle()
    val isVeg by viewModel.isVeg.collectAsStateWithLifecycle()
    val isVegan by viewModel.isVegan.collectAsStateWithLifecycle()

    // SearchBar active/inactive state — local UI concern, not in ViewModel
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            RecsTopBar(
                remainingCal = uiState.remainingCal,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ----------------------------------------------------------------
            // Search bar
            // ----------------------------------------------------------------
            RecsSearchBar(
                query = searchQuery,
                isActive = isSearchActive,
                onQueryChange = viewModel::onSearchQueryChange,
                onActiveChange = { isSearchActive = it },
                onClear = {
                    viewModel.onSearchQueryChange("")
                    isSearchActive = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isSearchActive) 0.dp else 16.dp)
            )

            AnimatedVisibility(visible = !isSearchActive) {
                // ------------------------------------------------------------
                // Filter row — meal types + dietary toggles
                // ------------------------------------------------------------
                FilterRow(
                    selectedMealType = selectedMealType,
                    isVeg = isVeg,
                    isVegan = isVegan,
                    onMealTypeSelected = viewModel::onMealTypeSelected,
                    onVegToggle = viewModel::onVegToggle,
                    onVeganToggle = viewModel::onVeganToggle,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ----------------------------------------------------------------
            // Main content — animated transitions between states
            // ----------------------------------------------------------------
            AnimatedContent(
                targetState = when {
                    uiState.isLoading -> RecsContentState.Loading
                    uiState.error != null || uiState.foods.isEmpty() -> RecsContentState.Empty
                    else -> RecsContentState.Results
                },
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut()
                },
                modifier = Modifier.fillMaxSize(),
                label = "recs_content"
            ) { contentState ->
                when (contentState) {
                    RecsContentState.Loading -> RecsLoadingContent()
                    RecsContentState.Empty -> RecsEmptyContent(
                        message = uiState.error ?: "No meals found for your calorie budget.",
                        onRetry = viewModel::retry
                    )
                    RecsContentState.Results -> RecsFoodList(
                        foods = uiState.foods
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Content state enum — drives AnimatedContent
// ---------------------------------------------------------------------------

private enum class RecsContentState { Loading, Empty, Results }

// ---------------------------------------------------------------------------
// TopAppBar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecsTopBar(
    remainingCal: Int,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (remainingCal > 0)
                        "$remainingCal kcal remaining today"
                    else
                        "Daily budget reached",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (remainingCal > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        scrollBehavior = scrollBehavior
    )
}

// ---------------------------------------------------------------------------
// SearchBar
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecsSearchBar(
    query: String,
    isActive: Boolean,
    onQueryChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onActiveChange(false) },
                expanded = isActive,
                onExpandedChange = onActiveChange,
                placeholder = { Text("Search meals, e.g. grilled chicken") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                }
            )
        },
        expanded = isActive,
        onExpandedChange = onActiveChange,
        modifier = modifier,
        content = {
            // Search suggestions could go here in a future iteration
        }
    )
}

// ---------------------------------------------------------------------------
// Filter row — meal type chips + Veg / Vegan toggles
// ---------------------------------------------------------------------------

@Composable
private fun FilterRow(
    selectedMealType: String,
    isVeg: Boolean,
    isVegan: Boolean,
    onMealTypeSelected: (String) -> Unit,
    onVegToggle: () -> Unit,
    onVeganToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Meal type chips — scrollable horizontal row
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(MEAL_TYPE_FILTERS) { filter ->
                FilterChip(
                    selected = selectedMealType == filter,
                    onClick = { onMealTypeSelected(filter) },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Dietary toggles — fixed on the right
        Spacer(modifier = Modifier.width(4.dp))

        FilterChip(
            selected = isVeg,
            onClick = onVegToggle,
            label = { Text("Veg") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        FilterChip(
            selected = isVegan,
            onClick = onVeganToggle,
            label = { Text("Vegan") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Loading state
// ---------------------------------------------------------------------------

@Composable
private fun RecsLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Finding meals for your budget…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Empty / error state
// ---------------------------------------------------------------------------

@Composable
private fun RecsEmptyContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Results list
// ---------------------------------------------------------------------------

@Composable
private fun RecsFoodList(foods: List<Food>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = foods,
            key = { food -> "${food.name}_${food.calories}" }
        ) { food ->
            FoodCard(food = food)
        }
    }
}
