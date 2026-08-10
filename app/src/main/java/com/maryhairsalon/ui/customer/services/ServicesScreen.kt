package com.maryhairsalon.ui.customer.services

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.maryhairsalon.data.models.Category
import com.maryhairsalon.data.models.Service
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.AccentAmberLight
import com.maryhairsalon.ui.theme.AccentDeep
import com.maryhairsalon.ui.theme.BackgroundDeep
import com.maryhairsalon.ui.theme.SurfaceLighter
import com.maryhairsalon.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    viewModel: ServicesViewModel = viewModel(),
    onBookNow: (Service) -> Unit,
    onMenuClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundDeep,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundDeep,
                    titleContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("♕", fontSize = 24.sp, color = AccentAmber)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Mary",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Hair salon",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search services or styles...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentAmber) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentAmber,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = SurfaceLighter,
                    unfocusedContainerColor = SurfaceLighter,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = AccentAmber)
                }
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(uiState.errorMessage!!, color = Color.White)
                }
                uiState.view == CatalogView.SEARCH -> {
                    val results = viewModel.searchResults
                    if (results.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("No services match your search — try a different term.", color = TextMuted)
                        }
                    } else {
                        ServiceList(services = results, onBookNow = onBookNow)
                    }
                }
                uiState.view == CatalogView.CATEGORY_DETAIL -> {
                    val category = viewModel.selectedCategory
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::backToCategories) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "All categories", tint = AccentAmber)
                        }
                        Text(category?.label ?: "", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    ServiceList(services = viewModel.selectedCategoryServices, onBookNow = onBookNow)
                }
                else -> CategoryGrid(
                    categories = uiState.categories,
                    onCategoryClick = viewModel::openCategory,
                    header = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("OUR SERVICES", style = MaterialTheme.typography.labelMedium, color = AccentAmber, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Find Your Perfect Style", style = MaterialTheme.typography.displayLarge, color = Color.White, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            Text("Pick a category to see everything on offer, or search for a style by name.", style = MaterialTheme.typography.bodyMedium, color = TextMuted, textAlign = TextAlign.Center)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit,
    header: @Composable () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(categories, key = { it.id }) { category ->
            Card(
                onClick = { onCategoryClick(category.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLighter)
            ) {
                Column {
                    AsyncImage(
                        model = category.image,
                        contentDescription = category.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    Column(Modifier.padding(12.dp)) {
                        Text(category.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        if (category.desc.isNotBlank()) {
                            Text(category.desc, style = MaterialTheme.typography.bodySmall, color = TextMuted, maxLines = 2)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${category.count} service${if (category.count == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceList(services: List<Service>, onBookNow: (Service) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(services, key = { it.id }) { service ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLighter)
            ) {
                Row(Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = service.image,
                        contentDescription = service.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(service.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        if (service.description.isNotBlank()) {
                            Text(
                                service.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 2
                            )
                        }
                        if (service.tags.isNotEmpty()) {
                            Text(
                                service.tags.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentAmber
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("R${service.price}", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Button(
                                onClick = { onBookNow(service) },
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(listOf(AccentDeep, AccentAmberLight)),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = BackgroundDeep
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text("Book Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
