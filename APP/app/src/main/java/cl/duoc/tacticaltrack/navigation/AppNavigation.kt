package cl.duoc.tacticaltrack.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cl.duoc.tacticaltrack.viewmodel.SessionViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import cl.duoc.tacticaltrack.model.AuthUser
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.tacticaltrack.TacticalTrackApplication
import cl.duoc.tacticaltrack.ui.screens.EvidenceScreen
import cl.duoc.tacticaltrack.ui.screens.HistoryScreen
import cl.duoc.tacticaltrack.ui.screens.HomeScreen
import cl.duoc.tacticaltrack.ui.screens.LoginScreen
import cl.duoc.tacticaltrack.ui.screens.NewVisitScreen
import cl.duoc.tacticaltrack.ui.screens.SupervisorScreen
import cl.duoc.tacticaltrack.ui.screens.VisitDetailScreen
import cl.duoc.tacticaltrack.viewmodel.LoginViewModel
import cl.duoc.tacticaltrack.viewmodel.VisitViewModel

@Composable
fun TacticalTrackApp(
    isExpanded: Boolean,
    pendingVisitId: Long? = null,
    onPushConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as TacticalTrackApplication

    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.Factory(app.preferencesRepository, app.authRepository)
    )
    val sessionScope: SessionViewModelStore = viewModel()
    val session by loginViewModel.uiState.collectAsStateWithLifecycle()
    val user = session.user
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, loginViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) loginViewModel.restoreSession()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(user?.id, user?.role, user?.supervisorId) {
        if (user == null) sessionScope.clearSession()
        cl.duoc.tacticaltrack.push.PushRegistration.setRole(context, user?.displayRole.orEmpty(), user?.supervisorId)
    }
    if (user == null) {
        LoginScreen(viewModel = loginViewModel)
        return
    }
    // El almacén conserva los formularios al rotar y se limpia al cerrar sesión.
    CompositionLocalProvider(LocalViewModelStoreOwner provides sessionScope) {
        SignedInApp(isExpanded, pendingVisitId, onPushConsumed, user) {
            loginViewModel.logout {
                cl.duoc.tacticaltrack.push.PushRegistration.endSession(context)
            }
        }
    }
}

@Composable
private fun SignedInApp(
    isExpanded: Boolean,
    pendingVisitId: Long?,
    onPushConsumed: () -> Unit,
    user: AuthUser,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as TacticalTrackApplication
    val visitViewModel: VisitViewModel = viewModel(
        factory = VisitViewModel.Factory(app.visitRepository, app.catalogRepository)
    )
    LaunchedEffect(user) { visitViewModel.setOperator(user.name, user.displayRole) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val bottomRoutes = setOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Supervisor.route
    )
    val showBottomBar = currentDestination?.route in bottomRoutes

    val role by visitViewModel.operatorRole.collectAsStateWithLifecycle()

    val supervisorSession by cl.duoc.tacticaltrack.push.PushRegistration.supervisorSession.collectAsStateWithLifecycle()
    LaunchedEffect(pendingVisitId, supervisorSession) {
        val id = pendingVisitId
        if (id != null && supervisorSession) {
            onPushConsumed()
            visitViewModel.openRemoteVisit(id) {
                navController.navigate(Screen.Detail.createRoute(id)) { launchSingleTop=true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = buildList {
                        add(Triple(Screen.Home.route, "Inicio", Icons.Default.Home))
                        add(Triple(Screen.History.route, "Historial", Icons.Default.History))
                        if (role == "Supervisor") {
                            add(Triple(Screen.Supervisor.route, "Supervisor", Icons.Default.Assignment))
                        }
                    }

                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any {
                                it.route == route
                            } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(scaffoldPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = visitViewModel,
                    isExpanded = isExpanded,
                    onLogout = onLogout,
                    onNewVisit = {
                        visitViewModel.clearForm()
                        navController.navigate(Screen.NewVisit.route)
                    }
                )
            }

            composable(Screen.NewVisit.route) {
                NewVisitScreen(
                    viewModel = visitViewModel,
                    onBack = navController::popBackStack,
                    onContinue = { navController.navigate(Screen.Evidence.route) }
                )
            }

            composable(Screen.Evidence.route) {
                EvidenceScreen(
                    viewModel = visitViewModel,
                    onBack = navController::popBackStack,
                    onSaved = { id ->
                        navController.navigate(Screen.Detail.createRoute(id)) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = visitViewModel,
                    onVisitClick = { id ->
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }

            composable(Screen.Supervisor.route) {
                SupervisorScreen(viewModel = visitViewModel)
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("visitId") { type = NavType.LongType }
                )
            ) { entry ->
                val visitId = entry.arguments?.getLong("visitId") ?: return@composable
                VisitDetailScreen(
                    visitId = visitId,
                    viewModel = visitViewModel,
                    onBack = navController::popBackStack
                )
            }
        }
    }
}

