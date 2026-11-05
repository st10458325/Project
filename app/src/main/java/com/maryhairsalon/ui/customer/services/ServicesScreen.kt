/**
 * ServicesScreen.kt
 *
 * Service catalog screen rendering categories, service lists, and search results.
 */
package com.maryhairsalon.ui.customer.services

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.maryhairsalon.data.models.Category
import com.maryhairsalon.data.models.Service
import com.maryhairsalon.ui.components.SalonBrandRow
import com.maryhairsalon.ui.components.SalonHeroHeader
import com.maryhairsalon.ui.components.SalonSearchField
import com.maryhairsalon.ui.components.SimpleFixedTopBar
import com.maryhairsalon.ui.components.rememberTopScreenGradient
import com.maryhairsalon.ui.theme.AccentAmberLight
import com.maryhairsalon.ui.theme.AccentDeep
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars

@Composable
fun ServicesScreen(
    viewModel: ServicesViewModel = viewModel(),
    onBookNow: (Service) -> Unit,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(uiState.errorMessage!!, color = colors.onBackground)
                }
                uiState.view == CatalogView.SEARCH -> Column(Modifier.fillMaxSize()) {
                    SimpleFixedTopBar(onMenuClick) {}
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        SalonSearchField(uiState.searchQuery, viewModel::setSearchQuery, placeholder = "Search...")
                    }
                    val results = viewModel.searchResults
                    if (results.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("No services match your search — try a different term.", color = colors.tertiary)
                        }
                    } else ServiceList(results, onBookNow)
                }
                uiState.view == CatalogView.CATEGORY_DETAIL -> Column(Modifier.fillMaxSize()) {
                    val category = viewModel.selectedCategory
                    SimpleFixedTopBar(onMenuClick) {
                        IconButton(onClick = viewModel::backToCategories) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All categories", tint = colors.primary)
                        }
                        Text(category?.label ?: "", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                    }
                    ServiceList(viewModel.selectedCategoryServices, onBookNow)
                }
                else -> CategoriesWithHero(
                    uiState = uiState,
                    onMenuClick = onMenuClick,
                    onCategoryClick = viewModel::openCategory,
                    onSearchChange = viewModel::setSearchQuery
                )
            }
        }
    }
}

@Composable
private fun CategoriesWithHero(
    uiState: ServicesUiState,
    onMenuClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchChange: (String) -> Unit
) {
    val gridState = rememberLazyGridState()
    val armed by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

    val floatingSearchVisible = remember { mutableStateOf(true) }
    val currentArmed = rememberUpdatedState(armed)
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                when {
                    available.y < -1f -> floatingSearchVisible.value = false
                    available.y > 1f && currentArmed.value -> floatingSearchVisible.value = true
                }
                return Offset.Zero
            }
        }
    }

    val currentSearchQuery = rememberUpdatedState(uiState.searchQuery)
    val currentOnSearchChange = rememberUpdatedState(onSearchChange)

    val searchField = remember {
        movableContentOf<Boolean> { elevated ->
            SalonSearchField(
                value = currentSearchQuery.value,
                onValueChange = { currentOnSearchChange.value(it) },
                elevated = elevated
            )
        }
    }

    val gradient = rememberTopScreenGradient()

    Box(Modifier.fillMaxSize().background(gradient).nestedScroll(connection)) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.statusBarsPadding(),
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SalonBrandRow(onMenuClick = onMenuClick, modifier = Modifier.padding(bottom = 12.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SalonHeroHeader(
                    title = "Find Your Perfect Style",
                    eyebrow = "OUR SERVICES",
                    subtitle = "Pick a category to see everything on offer, or search for a style by name.",
                    searchContent = if (!armed) {
                        { searchField(false) }
                    } else null
                )
            }
            items(uiState.categories, key = { it.id }) { category ->
                CategoryCard(category, onClick = { onCategoryClick(category.id) })
            }
        }

        if (armed && floatingSearchVisible.value) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                searchField(true)
            }
        }
    }
}

@Composable
private fun CategoryCard(category: Category, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(230.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(colors.onSurface.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (category.image.isNotBlank()) {
                    AsyncImage(
                        model = category.image,
                        contentDescription = category.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = colors.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(category.label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1)
                    if (category.desc.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(category.desc, style = MaterialTheme.typography.bodySmall, color = colors.tertiary, maxLines = 2)
                    }
                }
                Text(
                    "${category.count} service${if (category.count == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary
                )
            }
        }
    }
}

@Composable
private fun ServiceList(services: List<Service>, onBookNow: (Service) -> Unit) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(services, key = { it.id }) { service ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = service.image,
                        contentDescription = service.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp).background(colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(service.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        if (service.description.isNotBlank()) {
                            Text(service.description, style = MaterialTheme.typography.bodySmall, color = colors.tertiary, maxLines = 2)
                        }
                        if (service.tags.isNotEmpty()) {
                            Text(service.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = colors.primary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("R${service.price}", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                            Button(
                                onClick = { onBookNow(service) },
                                modifier = Modifier.background(
                                    brush = Brush.horizontalGradient(listOf(AccentDeep, AccentAmberLight)),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) { Text("Book Now", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
