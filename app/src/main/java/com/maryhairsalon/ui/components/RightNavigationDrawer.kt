package com.maryhairsalon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maryhairsalon.data.models.User
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.BackgroundDeep
import com.maryhairsalon.ui.theme.SurfaceLighter
import com.maryhairsalon.ui.theme.TextMuted

@Composable
fun RightNavigationDrawer(
    drawerState: DrawerState,
    user: User?,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.width(300.dp).fillMaxHeight(),
                        drawerContainerColor = BackgroundDeep,
                        drawerShape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Account",
                                modifier = Modifier.size(80.dp),
                                tint = AccentAmber
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = user?.name ?: "User",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            
                            Spacer(Modifier.weight(1f))
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Button(
                                onClick = onLogout,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.1f),
                                    contentColor = Color.Red
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Logout", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            content = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    content()
                }
            }
        )
    }
}
