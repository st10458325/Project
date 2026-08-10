package com.maryhairsalon.ui.customer.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.maryhairsalon.data.models.User
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.AccentGlow
import com.maryhairsalon.ui.theme.BackgroundDeep
import com.maryhairsalon.ui.theme.SurfaceLighter
import com.maryhairsalon.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onMenuClick: () -> Unit,
    viewModel: AboutViewModel = viewModel()
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
                        Box(Modifier.size(32.dp).padding(4.dp), contentAlignment = Alignment.Center) {
                            Text("♕", fontSize = 24.sp, color = AccentAmber)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Mary", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Hair salon", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
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
        when {
            uiState.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = AccentAmber)
            }
            uiState.errorMessage != null -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                Text(uiState.errorMessage!!, color = TextMuted)
            }
            uiState.stylists.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), Alignment.Center) {
                Text(
                    "Our stylists will appear here once the team is set up.",
                    color = TextMuted,
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center
                )
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text("MEET THE TEAM", style = MaterialTheme.typography.labelMedium, color = AccentAmber, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("The Stylists", style = MaterialTheme.typography.displayLarge, color = Color.White, textAlign = TextAlign.Center)
                    }
                }
                items(uiState.stylists, key = { it.id }) { stylist ->
                    StylistCard(stylist)
                }
            }
        }
    }
}

@Composable
private fun StylistCard(user: User) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)  // portrait ratio — scales with the card's actual width instead of assuming 340dp always looks right
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceLighter)
        )  {
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
                        color = AccentAmber
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
                color = AccentAmber,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(user.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}