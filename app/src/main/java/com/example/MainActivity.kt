package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ai.CartoonAiService
import com.example.data.repository.CartoonRepository
import com.example.data.repository.LocalStudioStore
import com.example.data.repository.attemptAutoSignIn
import com.example.data.repository.authStateFlow
import com.example.data.repository.signOutUser
import com.example.firebase.configureAppCheck
import com.example.media.CartoonVideoRenderer
import com.example.media.VoiceAndAudioEngine
import com.example.ui.components.CartoonAiTopBar
import com.example.ui.components.StudioGradientBackground
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CharactersScreen
import com.example.ui.screens.GeneratorStudioScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ProjectsAndAccountScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.CartoonAiTheme
import com.example.ui.theme.CyanSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.StudioDeepBg
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.CartoonStudioViewModel
import com.example.ui.viewmodel.DataUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_STUDIO = "studio"
const val ROUTE_CHARACTERS = "characters"
const val ROUTE_PROJECTS = "projects"
const val ROUTE_PLAYER = "player/{projectId}"

data class TopNavDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val topNavDestinations = listOf(
    TopNavDestination(ROUTE_HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    TopNavDestination(ROUTE_STUDIO, R.string.nav_create, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    TopNavDestination(ROUTE_CHARACTERS, R.string.nav_characters, Icons.Filled.Face, Icons.Outlined.Face),
    TopNavDestination(ROUTE_PROJECTS, R.string.nav_projects, Icons.Filled.FolderSpecial, Icons.Outlined.FolderSpecial)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before setContent, ViewModels, or Firebase ops
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            CartoonAiTheme {
                CartoonAiRoot()
            }
        }
    }
}

@Composable
fun CartoonAiRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var showOptionalSignIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    if (showOptionalSignIn && currentUser == null) {
        AuthScreen(
            onAuthSuccess = { showOptionalSignIn = false },
            onContinueWithoutLogin = { showOptionalSignIn = false }
        )
    } else {
        key(currentUser?.uid ?: "guest") {
            CartoonAiStudioApp(
                currentUser = currentUser,
                onSignInClick = { showOptionalSignIn = true },
                onSignOut = {
                    signOutUser(credentialManager, scope)
                }
            )
        }
    }
}

@Composable
fun CartoonAiStudioApp(
    currentUser: FirebaseUser?,
    onSignInClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val databaseId = remember(appContext) { appContext.getString(R.string.firestore_database_id) }
    val repository = remember(databaseId, currentUser?.uid) {
        if (currentUser != null) {
            CartoonRepository(FirebaseFirestore.getInstance(databaseId))
        } else {
            null
        }
    }
    val localStore = remember(appContext) { LocalStudioStore(appContext.filesDir) }
    val aiService = remember { CartoonAiService() }
    val audioEngine = remember(appContext) { VoiceAndAudioEngine(appContext) }
    val videoRenderer = remember(appContext) { CartoonVideoRenderer(appContext) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null && repository != null) {
            repository.ensureUserProfile(currentUser)
        }
    }

    val viewModel: CartoonStudioViewModel = viewModel(
        key = currentUser?.uid ?: "guest",
        factory = CartoonStudioViewModel.provideFactory(
            repository = repository,
            localStore = localStore,
            aiService = aiService,
            audioEngine = audioEngine,
            videoRenderer = videoRenderer,
            filesDir = appContext.filesDir,
            currentUserId = currentUser?.uid
        )
    )

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val charactersState by viewModel.charactersState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val scenesState by viewModel.selectedProjectScenesState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val pipelineState by viewModel.pipelineState.collectAsStateWithLifecycle()
    val charBusy by viewModel.characterGenBusy.collectAsStateWithLifecycle()
    val uiNotice by viewModel.uiNotice.collectAsStateWithLifecycle()

    val userProfile = (userProfileState as? DataUiState.Success)?.data
    val projects = (projectsState as? DataUiState.Success)?.data.orEmpty()
    val characters = (charactersState as? DataUiState.Success)?.data.orEmpty()
    val history = (historyState as? DataUiState.Success)?.data.orEmpty()
    val scenes = (scenesState as? DataUiState.Success)?.data.orEmpty()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiNotice) {
        val msg = uiNotice
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotice()
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openPlayer(projectId: String) {
        viewModel.selectProject(projectId)
        navController.navigate("player/$projectId") {
            launchSingleTop = true
        }
    }

    StudioGradientBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail && currentRoute != ROUTE_PLAYER) {
                        NavigationBar(
                            containerColor = StudioDeepBg.copy(alpha = 0.96f),
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            topNavDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyanSpark,
                                        selectedTextColor = Color.White,
                                        indicatorColor = ElectricViolet.copy(alpha = 0.28f),
                                        unselectedIconColor = StudioTextSecondary,
                                        unselectedTextColor = StudioTextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail && currentRoute != ROUTE_PLAYER) {
                        NavigationRail(
                            containerColor = StudioSurface.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            topNavDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = CyanSpark,
                                        selectedTextColor = Color.White,
                                        indicatorColor = ElectricViolet.copy(alpha = 0.30f),
                                        unselectedIconColor = StudioTextSecondary,
                                        unselectedTextColor = StudioTextSecondary
                                    )
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (currentRoute != ROUTE_PLAYER) {
                            CartoonAiTopBar(
                                isSignedIn = currentUser != null,
                                onStatusClick = { navigateTopLevel(ROUTE_PROJECTS) }
                            )
                        }

                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_HOME,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(ROUTE_HOME) {
                                val homeProject = projects.firstOrNull { it.id == pipelineState.activeProjectId }
                                HomeDashboardScreen(
                                    userProfile = userProfile,
                                    projects = projects,
                                    characters = characters,
                                    formState = formState,
                                    pipelineState = pipelineState,
                                    completedProject = homeProject,
                                    onPromptChange = viewModel::updatePrompt,
                                    onClearPrompt = viewModel::clearPrompt,
                                    onGenerateCartoon = { viewModel.generateCartoonMovie() },
                                    onRetryGeneration = { viewModel.retryFailedGeneration() },
                                    onCancelGeneration = viewModel::cancelGeneration,
                                    onRegenerateProject = { proj -> viewModel.regenerateCartoon(proj) },
                                    onVideoDownloaded = { proj, res ->
                                        viewModel.recordVideoDownload(
                                            project = proj,
                                            fileName = res.fileName,
                                            displayLocation = res.displayLocation,
                                            bytesCopied = res.bytesCopied
                                        )
                                    },
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    onCreateNewCartoon = { navigateTopLevel(ROUTE_STUDIO) },
                                    onLaunchPreset = { preset ->
                                        viewModel.selectStyle(preset.style)
                                        if (preset.prompt.isNotBlank()) {
                                            viewModel.updatePrompt(preset.prompt)
                                        }
                                        navigateTopLevel(ROUTE_STUDIO)
                                    },
                                    onOpenProject = { proj -> openPlayer(proj.id) },
                                    onOpenCharacters = { navigateTopLevel(ROUTE_CHARACTERS) },
                                    onOpenProjects = { navigateTopLevel(ROUTE_PROJECTS) }
                                )
                            }

                            composable(ROUTE_STUDIO) {
                                val studioProject = projects.firstOrNull { it.id == pipelineState.activeProjectId }
                                GeneratorStudioScreen(
                                    formState = formState,
                                    pipelineState = pipelineState,
                                    completedProject = studioProject,
                                    totalProjectsCount = projects.size,
                                    savedCharacters = characters,
                                    onPromptChange = viewModel::updatePrompt,
                                    onClearPrompt = viewModel::clearPrompt,
                                    onRandomStory = viewModel::pickRandomStory,
                                    onGenerateStory = viewModel::generateAiStoryIdea,
                                    onApplyCategoryTheme = viewModel::applyQuickTheme,
                                    onSelectStyle = viewModel::selectStyle,
                                    onSelectDuration = viewModel::selectDuration,
                                    onSelectLanguage = viewModel::selectLanguage,
                                    onSelectAspectRatio = viewModel::selectAspectRatio,
                                    onSelectResolution = viewModel::selectResolution,
                                    onToggleCharacter = viewModel::toggleCharacterSelection,
                                    onGenerateCartoon = {
                                        viewModel.generateCartoonMovie()
                                    },
                                    onRetryGeneration = {
                                        viewModel.retryFailedGeneration()
                                    },
                                    onRegenerateProject = { proj ->
                                        viewModel.regenerateCartoon(proj)
                                    },
                                    onCancelGeneration = viewModel::cancelGeneration,
                                    onOpenVideoPlayer = { pid -> openPlayer(pid) },
                                    onVideoDownloaded = { proj, res ->
                                        viewModel.recordVideoDownload(
                                            project = proj,
                                            fileName = res.fileName,
                                            displayLocation = res.displayLocation,
                                            bytesCopied = res.bytesCopied
                                        )
                                    },
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }

                            composable(ROUTE_CHARACTERS) {
                                CharactersScreen(
                                    characters = characters,
                                    isBusy = charBusy,
                                    onCreateCharacter = viewModel::createCharacter,
                                    onDeleteCharacter = viewModel::deleteCharacter
                                )
                            }

                            composable(ROUTE_PROJECTS) {
                                ProjectsAndAccountScreen(
                                    isSignedIn = currentUser != null,
                                    userProfile = userProfile,
                                    projects = projects,
                                    characters = characters,
                                    history = history,
                                    onOpenProject = { proj -> openPlayer(proj.id) },
                                    onDeleteProject = viewModel::deleteProject,
                                    onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                                    onClearHistory = viewModel::clearAllHistory,
                                    onSignInClick = onSignInClick,
                                    onSignOut = onSignOut
                                )
                            }

                            composable(
                                route = ROUTE_PLAYER,
                                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                            ) { entry ->
                                val pid = entry.arguments?.getString("projectId")
                                LaunchedEffect(pid) {
                                    viewModel.selectProject(pid)
                                }
                                val activeProj = projects.firstOrNull { it.id == pid }
                                val projectNumber = remember(projects, pid) {
                                    val idx = projects.indexOfFirst { it.id == pid }
                                    if (idx >= 0) (projects.size - idx).coerceAtLeast(1) else 1
                                }
                                VideoPlayerScreen(
                                    project = activeProj,
                                    scenes = scenes,
                                    pipelineState = pipelineState,
                                    defaultMovieNumber = projectNumber,
                                    onBack = { navController.popBackStack() },
                                    onSaveToMyProjects = viewModel::saveProjectToMyProjects,
                                    onRegenerateCartoon = { proj ->
                                        viewModel.regenerateCartoon(proj) { newPid ->
                                            openPlayer(newPid)
                                        }
                                    },
                                    onRegenerateScene = viewModel::regenerateSingleScene,
                                    onEditStoryAndRegenerate = { proj, title, prompt ->
                                        viewModel.editStoryAndRegenerate(proj, title, prompt) { newPid ->
                                            openPlayer(newPid)
                                        }
                                    },
                                    onVideoDownloaded = { proj, res ->
                                        viewModel.recordVideoDownload(
                                            project = proj,
                                            fileName = res.fileName,
                                            displayLocation = res.displayLocation,
                                            bytesCopied = res.bytesCopied
                                        )
                                    },
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
