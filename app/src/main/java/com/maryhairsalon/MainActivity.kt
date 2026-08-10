package com.maryhairsalon

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import com.maryhairsalon.ui.theme.BackgroundDeep
import com.maryhairsalon.ui.theme.AccentAmber
import com.maryhairsalon.ui.theme.TextMuted
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Group
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.maryhairsalon.ui.customer.about.AboutScreen
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            MaryHairSalonTheme {
                val rootNavController = rememberNavController()
                val startDestination =
                    if (FirebaseAuth.getInstance().currentUser != null) "loading" else "login"

                NavHost(rootNavController, startDestination = startDestination) {
                    composable("loading") {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        LaunchedEffect(Unit) {
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            Log.d("MainActivity", "Loading screen: checking role for uid=$uid")
                            if (uid != null) {
                                val role = AuthRepository().getUserRole(uid)
                                Log.d("MainActivity", "Routing decision: role=$role")

                                runCatching { NotificationRepository().registerCurrentDevice(uid) }
                                    .onFailure { Log.e("MainActivity", "Failed to register FCM token", it) }

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
                    composable("main") {
                        MainScreen(
                            onLogout = {
                                FirebaseAuth.getInstance().signOut()
                                rootNavController.navigate("login") { popUpTo(0) }
                            }
                        )
                    }
                    composable("employee_main") {
                        EmployeeMainScreen(
                            onLogout = {
                                FirebaseAuth.getInstance().signOut()
                                rootNavController.navigate("login") { popUpTo(0) }
                            }
                        )
                    }
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

@Composable
private fun MainScreen(onLogout: () -> Unit) {
    val tabNavController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()

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
        onLogout = onLogout
    ) {
        Scaffold(
            bottomBar = {
                val backStackEntry by tabNavController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                if (currentRoute == "services" || currentRoute == "about" || currentRoute == "bookings") {
                    NavigationBar(
                        containerColor = BackgroundDeep,
                        contentColor = TextMuted,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(75.dp)
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "services",
                            onClick = { navigateToTab("services") },
                            icon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                            label = { Text("Services") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentAmber, selectedTextColor = AccentAmber,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "about",
                            onClick = { navigateToTab("about") },
                            icon = { Icon(Icons.Default.Group, contentDescription = null) },
                            label = { Text("About") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentAmber, selectedTextColor = AccentAmber,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = currentRoute == "bookings",
                            onClick = { navigateToTab("bookings") },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            label = { Text("Bookings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentAmber, selectedTextColor = AccentAmber,
                                unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                tabNavController,
                startDestination = "services",
                modifier = Modifier.padding(padding)
            ) {
                composable("services") {
                    ServicesScreen(
                        onBookNow = { service ->
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
                        onDone = { navigateToTab("bookings") },
                        onCancel = { tabNavController.popBackStack() },
                    )
                }
            }
        }
    }
}
@Composable
private fun EmployeeMainScreen(onLogout: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()

    RightNavigationDrawer(
        drawerState = drawerState,
        user = authUiState.userProfile,
        onLogout = onLogout
    ) {
        EmployeeDashboardScreen(
            onLogout = onLogout,
            onMenuClick = { scope.launch { drawerState.open() } }
        )
    }
}

@Composable
private fun ManagerMainScreen(onLogout: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()

    RightNavigationDrawer(
        drawerState = drawerState,
        user = authUiState.userProfile,
        onLogout = onLogout
    ) {
        ManagerDashboardScreen(
            onLogout = onLogout,
            onMenuClick = { scope.launch { drawerState.open() } }
        )
    }
}
