package com.ventgui.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.utils.UpdateManager
import com.ventgui.app.data.utils.UpdateState
import com.ventgui.app.ui.components.UpdateDialog
import com.ventgui.app.ui.components.MidnightBlue
import com.ventgui.app.ui.components.CyberCyan
import com.ventgui.app.ui.screens.audit.*
import com.ventgui.app.ui.screens.content.*
import com.ventgui.app.ui.screens.dashboard.*
import com.ventgui.app.ui.screens.gallery.*
import com.ventgui.app.ui.screens.login.*
import com.ventgui.app.ui.screens.onboarding.*
import com.ventgui.app.ui.screens.profile.*
import com.ventgui.app.ui.screens.races.*
import com.ventgui.app.ui.screens.reports.*
import com.ventgui.app.ui.screens.sponsors.*
import com.ventgui.app.ui.screens.team.*
import com.ventgui.app.ui.screens.updates.*
import com.ventgui.app.ui.screens.vault.*
import com.ventgui.app.ui.theme.CantanhedehubTheme
import com.ventgui.app.ui.theme.CantanhedeTheme
import com.ventgui.app.data.model.Athlete
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ventgui.app.ui.screens.login.AuthViewModel
import com.ventgui.app.ui.screens.login.AuthState

enum class AppDestinations(val labelResId: Int, val icon: ImageVector) {
    DASHBOARD(R.string.nav_dashboard, Icons.Rounded.Home),
    TEAM(R.string.nav_team, Icons.Rounded.People),
    RACES(R.string.nav_races, Icons.Rounded.DateRange),
    GALLERY(R.string.nav_gallery, Icons.Rounded.PhotoLibrary),
    VAULT(R.string.nav_vault, Icons.Rounded.Lock),
    CONTENT_FACTORY(R.string.nav_content_factory, Icons.Rounded.AutoAwesome),
    SPONSORS(R.string.nav_sponsors, Icons.Rounded.Star),
    PROFILE(R.string.nav_profile, Icons.Rounded.Person),
    UPDATES(R.string.nav_updates, Icons.Rounded.History),
    REPORTS(R.string.nav_reports, Icons.Rounded.Analytics),
    ONBOARDING(R.string.nav_onboarding, Icons.Rounded.RocketLaunch),
    AUDIT_LOG(R.string.profile_audit_log, Icons.Rounded.Description),
    ATHLETE_DETAILS(R.string.nav_athlete_details, Icons.Rounded.Person)
}

class MainActivity : FragmentActivity() {
    private var isFirstTime = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        SupabaseClient.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            CantanhedehubTheme {
                MainApp(isFirstTime)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        SupabaseClient.client.handleDeeplinks(intent)
        val data = intent.data?.toString() ?: ""
        if (data.contains("type=signup") || intent.data?.fragment?.contains("type=signup") == true) {
            isFirstTime.value = true
        }
    }
}

val AppDestinationsSaver = Saver<AppDestinations, String>(
    save = { it.name },
    restore = { AppDestinations.valueOf(it) }
)



@Composable
fun MainApp(
    isFirstTimeState: MutableState<Boolean>,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val loginState by authViewModel.authState.collectAsStateWithLifecycle()
    var currentDestination by rememberSaveable(stateSaver = AppDestinationsSaver) { mutableStateOf(AppDestinations.DASHBOARD) }
    var selectedAthleteForDetails by remember { mutableStateOf<Athlete?>(null) }
    var isNavBarVisible by rememberSaveable { mutableStateOf(true) }
    var isFirstTime by isFirstTimeState
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    
    val configuration = LocalConfiguration.current

    val updateState by UpdateManager.updateState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.restoreSession(context)
    }

    LaunchedEffect(Unit) {
        UpdateManager.checkForUpdates()
    }

    LaunchedEffect(updateState) {
        if (updateState !is UpdateState.Idle) {
            showUpdateDialog = true
        }
    }

    if (showUpdateDialog && updateState !is UpdateState.Idle) {
        UpdateDialog(
            state = updateState,
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (loginState is AuthState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF80D8FF))
        }
    } else if (isFirstTime) {
        OnboardingScreen(onFinish = { isFirstTime = false })
    } else if (loginState is AuthState.Unauthenticated) {
        LoginScreen(
            onLoginSuccess = {},
            onSignUpSuccess = { 
                isFirstTime = true
            },
            authViewModel = authViewModel
        )
    } else {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val openDrawer = { scope.launch { drawerState.open() }; Unit }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = isNavBarVisible,
            drawerContent = {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(CantanhedeTheme.colors.midnightBlue)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .statusBarsPadding()
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header brand
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Text("C", color = MidnightBlue, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("CANTANHEDE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text("CYCLING HUB", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        val navItems = listOf(
                            AppDestinations.DASHBOARD,
                            AppDestinations.TEAM,
                            AppDestinations.RACES,
                            AppDestinations.GALLERY,
                            AppDestinations.VAULT,
                            AppDestinations.CONTENT_FACTORY,
                            AppDestinations.REPORTS,
                            AppDestinations.SPONSORS,
                            AppDestinations.PROFILE,
                            AppDestinations.UPDATES
                        )
                        
                        navItems.forEach { destination ->
                            val isSelected = currentDestination == destination
                            
                            // Animate selection transitions
                            val backgroundAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 0.12f else 0.0f,
                                animationSpec = tween(durationMillis = 300),
                                label = "navBgAlpha"
                            )
                            val borderAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 0.4f else 0.0f,
                                animationSpec = tween(durationMillis = 300),
                                label = "navBorderAlpha"
                            )
                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "navIconScale"
                            )
                            val textTranslation by animateDpAsState(
                                targetValue = if (isSelected) 6.dp else 0.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                                label = "navTextTranslation"
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CantanhedeTheme.colors.cyberCyan.copy(alpha = backgroundAlpha))
                                    .border(
                                        width = if (isSelected || borderAlpha > 0.01f) 1.dp else 0.dp,
                                        color = CantanhedeTheme.colors.cyberCyan.copy(alpha = borderAlpha),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        currentDestination = destination
                                        scope.launch { drawerState.close() }
                                    }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = stringResource(destination.labelResId),
                                    tint = if (isSelected) CantanhedeTheme.colors.cyberCyan else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(22.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(destination.labelResId),
                                    color = if (isSelected) CantanhedeTheme.colors.cyberCyan else Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    modifier = Modifier.offset(x = textTranslation)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CantanhedeTheme.colors.midnightBlue)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    when (currentDestination) {
                        AppDestinations.DASHBOARD -> DashboardScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onLogout = { 
                                authViewModel.logout(context)
                            },
                            onNavigateToProfile = { currentDestination = AppDestinations.PROFILE },
                            onNavigateToSection = { dest, action -> 
                                currentDestination = dest
                                pendingAction = action
                            },
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.TEAM -> EquipaScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onSelectionModeChange = { isNavBarVisible = !it },
                            initialOpenAddDialog = pendingAction == "ADD_ATHLETE",
                            onDialogOpened = { pendingAction = null },
                            onNavigateToAthleteDetails = { athlete ->
                                selectedAthleteForDetails = athlete
                                currentDestination = AppDestinations.ATHLETE_DETAILS
                            },
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.ATHLETE_DETAILS -> AthleteDetailsScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            athlete = selectedAthleteForDetails,
                            onBack = { currentDestination = AppDestinations.TEAM }
                        )
                        AppDestinations.RACES -> ProvasScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onSelectionModeChange = { isNavBarVisible = !it },
                            initialOpenAddDialog = pendingAction == "ADD_RACE",
                            onDialogOpened = { pendingAction = null },
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.GALLERY -> GaleriaScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onSelectionModeChange = { isNavBarVisible = !it },
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.VAULT -> VaultScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.CONTENT_FACTORY -> ContentFactoryScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.REPORTS -> ReportsScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.SPONSORS -> SponsorsScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onSelectionModeChange = { isNavBarVisible = !it },
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.PROFILE -> ProfileScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onLogout = { 
                                authViewModel.logout(context)
                            },
                            onBack = { currentDestination = AppDestinations.DASHBOARD },
                            onNavigateToAuditLog = { currentDestination = AppDestinations.AUDIT_LOG }
                        )
                        AppDestinations.AUDIT_LOG -> AuditLogScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onBack = { currentDestination = AppDestinations.PROFILE }
                        )
                        AppDestinations.UPDATES -> UpdatesScreen(
                            innerPadding = PaddingValues(bottom = 20.dp),
                            onOpenDrawer = openDrawer
                        )
                        AppDestinations.ONBOARDING -> {}
                    }
                }
            }
        }
    }
}