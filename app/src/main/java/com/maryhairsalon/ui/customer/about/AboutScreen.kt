/**
 * AboutScreen.kt
 *
 * Composable screen introducing Mary Hair Salon's styling team and daily hair care advice.
 */
package com.maryhairsalon.ui.customer.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.maryhairsalon.data.models.User
import com.maryhairsalon.ui.components.SalonBrandRow
import com.maryhairsalon.ui.components.SalonHeroHeader
import com.maryhairsalon.ui.components.rememberTopScreenGradient
import com.maryhairsalon.ui.theme.AccentGlow

@Composable
fun AboutScreen(
    onMenuClick: () -> Unit,
    viewModel: AboutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val gradient = rememberTopScreenGradient()

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(gradient)
        ) {
            LazyVerticalGrid(
                modifier = Modifier.statusBarsPadding(),
                columns = GridCells.Adaptive(minSize = 240.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SalonBrandRow(onMenuClick = onMenuClick, modifier = Modifier.padding(bottom = 12.dp))
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SalonHeroHeader(
                        title = "The Stylists",
                        eyebrow = "MEET THE TEAM",
                        subtitle = "The people behind your next look.",
                        searchContent = null
                    )
                }

                when {
                    uiState.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }
                    uiState.errorMessage != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                            Text(uiState.errorMessage!!, color = colors.tertiary, textAlign = TextAlign.Center)
                        }
                    }
                    uiState.stylists.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                            Text(
                                "Our stylists will appear here once the team is set up.",
                                color = colors.tertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            TipOfTheDayCard(tip = uiState.tipOfTheDay, isLoading = uiState.isLoadingTip)
                        }
                        items(uiState.stylists, key = { it.id }) { stylist ->
                            StylistCard(stylist)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StylistCard(user: User) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
        ) {
            if (user.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = user.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        user.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString(""),
                        style = MaterialTheme.typography.displayLarge,
                        color = colors.primary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(AccentGlow)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                user.title.ifBlank { "Stylist" },
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(user.name, style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
    }
}

@Composable
private fun TipOfTheDayCard(tip: String?, isLoading: Boolean) {
    if (isLoading || tip == null) return
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Text("TIP OF THE DAY", style = MaterialTheme.typography.labelSmall, color = colors.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(tip, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
    }
}
