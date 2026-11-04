package com.maryhairsalon

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.maryhairsalon.data.repository.AuthRepository
import com.maryhairsalon.data.repository.NotificationRepository
import com.maryhairsalon.ui.auth.AuthViewModel
import com.maryhairsalon.ui.auth.LoginScreen
import com.maryhairsalon.ui.auth.SignupScreen
import com.maryhairsalon.ui.components.RightNavigationDrawer
import com.maryhairsalon.ui.customer.bookings.BookingFormScreen
import com.maryhairsalon.ui.customer.bookings.BookingsScreen
import com.maryhairsalon.ui.customer.services.ServicesScreen
import com.maryhairsalon.ui.employee.EmployeeDashboardScreen
import com.maryhairsalon.ui.manager.ManagerDashboardScreen
import com.maryhairsalon.ui.theme.MaryHairSalonTheme
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Group
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.maryhairsalon.ui.customer.about.AboutScreen
import com.maryhairsalon.ui.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.res.stringResource
import com.maryhairsalon.R
import com.maryhairsalon.data.local.ThemeMode
import com.maryhairsalon.ui.components.VerifyEmailBanner
import com.maryhairsalon.data.local.ThemePreferences
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permissions for Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        enableEdgeToEdge()
        setContent {
            val themePreferences = remember { ThemePreferences(applicationContext) }
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            MaryHairSalonTheme(themeMode = themeMode) {
                val rootNavController = rememberNavController()

                // Determine initial screen based on whether a user is already authenticated
                val startDestination =
                    if (FirebaseAuth.getInstance().currentUser != null) "loading" else "login"

                NavHost(rootNavController, startDestination = startDestination) {
                    // Intermediate screen to fetch user profile and route to the correct dashboard
                    composable("loading") {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        LaunchedEffect(Unit) {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            Log.d("MainActivity", "Loading screen: checking role for uid=$uid")
                            if (uid != null) {
                                // Never let a slow/absent network hold the launch screen
                                // hostage — if this takes too long, just proceed as a
                                // customer rather than freeze on the spinner forever.
                                val role = withTimeoutOrNull(4000) {
                                    AuthRepository().getUserRole(uid)
                                } ?: "customer"
                                Log.d("MainActivity", "Routing decision: role=$role")

                                // Fire-and-forget: fetching an FCM token needs a real
                                // network round-trip with no offline fallback, so it must
                                // never block navigation. If it fails or times out, we
                                // just try again on the next launch.
                                CoroutineScope(Dispatchers.IO).launch {
                                    withTimeoutOrNull(5000) {
                                        runCatching { NotificationRepository().registerCurrentDevice(uid) }
                                            .onFailure { Log.e("MainActivity", "Failed to register FCM token", it) }
                                    }
                                }

                                val dest = when (role) {
                                    "employee" -> "employee_main"
                                    "manager" -> "manager_main"
                                    else -> "main"
                                }
                                rootNavController.navigate(dest) { popUpTo(0) }
                            } else {
                                Log.d("MainActivity", "No UID found, redirecting to login")
                                rootNavController.navigate("login") { popUpTo(0) }
                            }
                        }
                    }
                    composable("login") {
                        LoginScreen(
                            onLoggedIn = {
                                rootNavController.navigate("loading") { popUpTo(0) }
                            },
                            onGoToSignup = { rootNavController.navigate("signup") }
                        )
                    }
                    composable("signup") {
                        SignupScreen(
                            onSignedUp = { rootNavController.navigate("loading") { popUpTo(0) } },
                            onGoToLogin = { rootNavController.popBackStack() }
                        )
                    }
                    // Primary customer navigation graph
                    composable("main") {
                        MainScreen(
                            onLogout = {
                                FirebaseAuth.getInstance().signOut()
                                rootNavController.navigate("login") { popUpTo(0) }
                            }
                        )
                    }
                    // Entry point for stylists
                    composable("employee_main") {
                        EmployeeMainScreen(
                            onLogout = {
                                FirebaseAuth.getInstance().signOut()
                                rootNavController.navigate("login") { popUpTo(0) }
                            }
                        )
                    }
                    // Entry point for salon management
                    composable("manager_main") {
                        ManagerMainScreen(
                            onLogout = {
                                FirebaseAuth.getInstance().signOut()
                                rootNavController.navigate("login") { popUpTo(0) }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted")
            } else {
                Log.d("MainActivity", "Notification permission denied")
            }
        }
    }
}

/**
 * Main customer interface featuring bottom navigation and a side drawer.
 */
@Composable
private fun MainScreen(onLogout: () -> Unit) {
    val tabNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()

    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isBookingForm = currentRoute?.startsWith("booking?") == true

    // Helper function for navigating between main tabs while preserving state
    fun navigateToTab(route: String) {
        tabNavController.navigate(route) {
            popUpTo(tabNavController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    RightNavigationDrawer(
        drawerState = drawerState,
        user = authUiState.userProfile,
        gesturesEnabled = !isBookingForm,
        onSettingsClick = {
            scope.launch { drawerState.close() }
            tabNavController.navigate("settings")
        },
        onLogout = onLogout
    ) {
        Scaffold(
            bottomBar = {
                val colors = MaterialTheme.colorScheme
                if (currentRoute == "services" || currentRoute == "about" || currentRoute == "bookings") {
                    // Keep the whole bottom section compact INCLUDING the system
                    // navigation inset. The old 64.dp row plus navigationBarsPadding()
                    // made the total height 64.dp + the device's inset, which is
                    // especially noticeable on phones with taller navigation areas.
                    val bottomInset = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                    val targetTotalHeight = 68.dp
                    val itemRowHeight = (targetTotalHeight - bottomInset).coerceAtLeast(44.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemRowHeight),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactNavigationItem(
                                selected = currentRoute == "services",
                                icon = Icons.Default.ContentCut,
                                label = stringResource(R.string.nav_services),
                                onClick = { navigateToTab("services") }
                            )
                            CompactNavigationItem(
                                selected = currentRoute == "about",
                                icon = Icons.Default.Group,
                                label = stringResource(R.string.nav_about),
                                onClick = { navigateToTab("about") }
                            )
                            CompactNavigationItem(
                                selected = currentRoute == "bookings",
                                icon = Icons.Default.CalendarMonth,
                                label = stringResource(R.string.nav_bookings),
                                onClick = { navigateToTab("bookings") }
                            )
                        }
                        // Paint the same background behind Android's gesture or
                        // three-button navigation area, but include it in the
                        // target total height instead of adding it on top.
                        Spacer(Modifier.height(bottomInset))
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (!authUiState.isEmailVerified) {
                    VerifyEmailBanner(
                        isSending = authUiState.isSendingVerification,
                        message = authUiState.verificationMessage,
                        onResend = authViewModel::resendVerificationEmail,
                        onRefresh = authViewModel::refreshVerificationStatus
                    )
                }
                NavHost(
                    tabNavController,
                    startDestination = "services",
                    modifier = Modifier.weight(1f)
                ) {
                    composable("services") {
                        ServicesScreen(
                            onBookNow = { service ->
                                // Prefill the booking form with service details
                                tabNavController.navigate(
                                    "booking?categoryId=${service.category}&style=${service.title}&price=${service.price}"
                                )
                            },
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    }
                    composable("about") {
                        AboutScreen(onMenuClick = { scope.launch { drawerState.open() } })
                    }
                    composable("bookings") {
                        BookingsScreen(
                            onLogout = onLogout,
                            onReschedule = { booking -> tabNavController.navigate("booking?bookingId=${booking.id}") },
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                    }
                    // Combined route for new bookings and rescheduling existing ones
                    composable(
                        route = "booking?categoryId={categoryId}&style={style}&price={price}&bookingId={bookingId}",
                        arguments = listOf(
                            navArgument("categoryId") { type = NavType.StringType; nullable = true },
                            navArgument("style") { type = NavType.StringType; nullable = true },
                            navArgument("price") { type = NavType.StringType; nullable = true },
                            navArgument("bookingId") { type = NavType.StringType; nullable = true }
                        )
                    ) { backStackEntry ->
                        BookingFormScreen(
                            prefillCategoryId = backStackEntry.arguments?.getString("categoryId"),
                            prefillStyle = backStackEntry.arguments?.getString("style"),
                            prefillPrice = backStackEntry.arguments?.getString("price"),
                            prefillBookingId = backStackEntry.arguments?.getString("bookingId"),
                            onDone = {
                                tabNavController.navigate("bookings") {
                                    popUpTo(tabNavController.graph.findStartDestination().id) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    // no saveState / restoreState: we want the form gone for good
                                }
                            },
                            onCancel = { tabNavController.popBackStack() },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { tabNavController.popBackStack() })
                    }
                }
            }
        }
    }
}

/** A compact bottom-navigation destination with no large Material nav-bar padding. */
@Composable
private fun RowScope.CompactNavigationItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val itemColor = if (selected) colors.primary else colors.tertiary

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = itemColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = itemColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun EmployeeMainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()

    RightNavigationDrawer(
        drawerState = drawerState,
        user = authUiState.userProfile,
        onSettingsClick = {
            scope.launch { drawerState.close() }
            navController.navigate("settings")
        },
        onLogout = onLogout
    ) {
        NavHost(navController, startDestination = "dashboard") {
            composable("dashboard") {
                EmployeeDashboardScreen(
                    onLogout = onLogout,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun ManagerMainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()

    RightNavigationDrawer(
        drawerState = drawerState,
        user = authUiState.userProfile,
        onSettingsClick = {
            scope.launch { drawerState.close() }
            navController.navigate("settings")
        },
        onLogout = onLogout
    ) {
        NavHost(navController, startDestination = "dashboard") {
            composable("dashboard") {
                ManagerDashboardScreen(
                    onLogout = onLogout,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
