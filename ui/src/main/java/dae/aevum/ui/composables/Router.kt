package dae.aevum.ui.composables

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dae.aevum.common.R
import dae.aevum.ui.composables.dialogs.AddUserDialog
import dae.aevum.ui.composables.dialogs.CompletedWorklogDialog
import dae.aevum.ui.composables.issues.IssueList
import dae.aevum.ui.composables.issues.IssueListScreen
import dae.aevum.ui.composables.issues.IssueScreen
import dae.aevum.ui.composables.reusable.LocalScaffoldContentPadding
import dae.aevum.ui.composables.users.UserListScreen
import dae.aevum.ui.composables.users.UserSettingsScreen
import dae.aevum.ui.composables.worklogs.WorklogBarChartScreen
import dae.aevum.ui.composables.worklogs.WorklogDayDetailsScreen
import dae.aevum.ui.composables.worklogs.WorklogEditorScreen
import dae.aevum.ui.composables.worklogs.WorklogListScreen
import dae.aevum.ui.composables.worklogs.WorklogSplitterIssueScreen
import dae.aevum.ui.composables.worklogs.WorklogSplitterTimeScreen
import dae.aevum.ui.composables.worklogs.WorklogSummaryScreen
import dae.aevum.ui.models.UserUiModel
import dae.aevum.ui.viewmodels.ActiveUserState
import dae.aevum.ui.viewmodels.IssueViewModel
import dae.aevum.ui.viewmodels.UserViewModel
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.IssueId
import dae.aevum.utils.JiraAuthState
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class Screen(
    val route: String,
    @param:StringRes val resourceId: Int,
    val icon: ImageVector,
) {
    data object Users : Screen("users", R.string.users, Icons.Outlined.AccountCircle)

    data object Issues : Screen("issues", R.string.issues, Icons.Outlined.Home)

    data object IssueView :
        Screen("issueView/{issueId}", R.string.issue_view, Icons.Outlined.Search)

    data object Worklogs : Screen("worklogs", R.string.pending, Icons.AutoMirrored.Outlined.List)

    data object WorklogsChangeId : Screen(
        "worklogChangeIssue/{worklogId}",
        R.string.change_issue_of_worklog,
        Icons.Outlined.Edit,
    )

    data object WorklogEditor :
        Screen("worklogEditor/{worklogId}", R.string.worklog_editor, Icons.Outlined.Edit)

    data object WorklogSplitIssueSelector :
        Screen(
            "worklog/split/issueSelector/{worklogId}",
            R.string.split_worklog,
            Icons.Outlined.Edit,
        )

    data object WorklogSplitTimeSelector :
        Screen(
            "worklog/split/timeSelector/{worklogId}/{issueId}",
            R.string.time_selector,
            Icons.Outlined.Edit,
        )

    data object WorklogNewDialog :
        Screen("worklog/new/{worklogId}", R.string.worklog_new_dialog, Icons.Outlined.Create)

    data object UserNewDialog :
        Screen("users/new", R.string.add_user, Icons.Outlined.Add)

    data object UserSettings :
        Screen("users/{userId}/settings", R.string.user_settings, Icons.Outlined.AccountCircle)

    data object Summary : Screen("summary", R.string.summary_title, Icons.Outlined.BarChart)

    data object SummaryBar : Screen("summary/bar", R.string.summary_title, Icons.Outlined.Home)

    data object SummaryDetails :
        Screen("summary/details", R.string.summary_title, Icons.Outlined.Home)

    data object SummaryDetailsForDay :
        Screen("summary/details/{day}", R.string.summary_title, Icons.Outlined.Home)
}

val screens =
    listOf(
        Screen.Users,
        Screen.UserSettings,
        Screen.Issues,
        Screen.IssueView,
        Screen.Worklogs,
        Screen.Summary,
        Screen.SummaryBar,
        Screen.SummaryDetails,
        Screen.SummaryDetailsForDay,
        Screen.WorklogsChangeId,
        Screen.WorklogEditor,
        Screen.WorklogNewDialog,
        Screen.UserNewDialog,
        Screen.WorklogSplitIssueSelector,
        Screen.WorklogSplitTimeSelector,
    )

val bottomNav =
    listOf(
        Screen.Users,
        Screen.Issues,
        Screen.Summary,
        Screen.Worklogs,
    )

@Composable
fun Router(
    onReady: () -> Unit = {},
    pendingStopWorklogId: Long? = null,
    onStopDialogOpened: () -> Unit = {},
    pendingEditorWorklogId: Long? = null,
    onEditorOpened: () -> Unit = {},
    pendingSearchQuery: String? = null,
    onSearchQueryConsumed: () -> Unit = {},
    pendingOpenFeature: String? = null,
    onOpenFeatureConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    var currentScreen by remember {
        mutableStateOf<Screen>(Screen.Issues)
    }
    var searchQueryForIssues by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val screen =
                screens.find { it.route == backStackEntry.destination.route }
                    ?: return@collect

            currentScreen = screen
        }
    }

    LaunchedEffect(pendingStopWorklogId) {
        val id = pendingStopWorklogId ?: return@LaunchedEffect
        navController.navigate("worklog/new/$id")
        onStopDialogOpened()
    }

    LaunchedEffect(pendingEditorWorklogId) {
        val id = pendingEditorWorklogId ?: return@LaunchedEffect
        navController.navigate("worklogEditor/$id")
        onEditorOpened()
    }

    LaunchedEffect(pendingSearchQuery) {
        val query = pendingSearchQuery ?: return@LaunchedEffect
        searchQueryForIssues = query
        navController.navigate(Screen.Issues.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        onSearchQueryConsumed()
    }

    LaunchedEffect(pendingOpenFeature) {
        val feature = pendingOpenFeature ?: return@LaunchedEffect
        val route =
            when (feature) {
                dae.aevum.utils.Intents.FEATURE_ISSUES -> Screen.Issues.route
                dae.aevum.utils.Intents.FEATURE_WORKLOGS -> Screen.Worklogs.route
                dae.aevum.utils.Intents.FEATURE_SUMMARY -> Screen.Summary.route
                else -> {
                    Timber.w("Unknown Assistant feature: %s", feature)
                    onOpenFeatureConsumed()
                    return@LaunchedEffect
                }
            }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        onOpenFeatureConsumed()
    }

    AppScaffold(
        navController = navController,
        currentScreen = currentScreen,
        onReady = onReady,
        pendingSearchQuery = searchQueryForIssues,
        onPendingSearchQueryConsumed = { searchQueryForIssues = null },
    )
}

@Composable
fun AppScaffold(
    navController: NavHostController,
    currentScreen: Screen,
    onReady: () -> Unit = {},
    pendingSearchQuery: String? = null,
    onPendingSearchQueryConsumed: () -> Unit = {},
    userViewModel: UserViewModel = hiltViewModel(),
) {
    val activeUserState by userViewModel.activeUserState.collectAsState()
    val tokenExpired by JiraAuthState.tokenExpired.collectAsState()

    val isLoading = activeUserState is ActiveUserState.Loading
    val activeUser = (activeUserState as? ActiveUserState.Loaded)?.user

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            onReady()
        }
    }

    RequestNotificationPermissionIfNeeded(activeUser != null)

    if (isLoading) {
        return
    }

    val startDestination =
        if (activeUser == null) {
            Screen.Users.route
        } else {
            Screen.Issues.route
        }

    val snackbarHostState = remember { androidx.compose.material.SnackbarHostState() }
    RequestLocalNetworkPermissionOnFailure(snackbarHostState = snackbarHostState)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = {
            androidx.compose.material.SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            BottomBar(
                navController = navController,
                activeUser = activeUser,
            )
        },
        topBar = {
            AppTopBar(
                currentScreen = currentScreen,
                showTokenExpiredBanner = tokenExpired && activeUser != null,
            )
        },
        floatingActionButton = {
            Fab(
                navController = navController,
                currentScreen = currentScreen,
                snackbarHostState = snackbarHostState,
            )
        },
    ) { insets ->
        Timber.v(
            "Insets: top=%s bottom=%s left=%s right=%s",
            insets.calculateTopPadding(),
            insets.calculateBottomPadding(),
            insets.calculateLeftPadding(LayoutDirection.Ltr),
            insets.calculateRightPadding(LayoutDirection.Ltr),
        )
        CompositionLocalProvider(LocalScaffoldContentPadding provides insets) {
            AppNavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                userViewModel = userViewModel,
                startDestination = startDestination,
                pendingSearchQuery = pendingSearchQuery,
                onPendingSearchQueryConsumed = onPendingSearchQueryConsumed,
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestNotificationPermissionIfNeeded(enabled: Boolean) {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return
    }

    val notificationPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(enabled, notificationPermission.status.isGranted) {
        if (!notificationPermission.status.isGranted) {
            notificationPermission.launchPermissionRequest()
        }
    }
}

/**
 * Only after a LAN proofreader connect failure: show why, then ask for
 * ACCESS_LOCAL_NETWORK (Android 17+ / targetSdk 37).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestLocalNetworkPermissionOnFailure(
    snackbarHostState: androidx.compose.material.SnackbarHostState,
    viewModel: dae.aevum.ui.viewmodels.LocalNetworkPermissionViewModel = hiltViewModel(),
) {
    if (Build.VERSION.SDK_INT < dae.aevum.utils.LocalNetworkUrls.ACCESS_LOCAL_NETWORK_API) {
        return
    }

    val localNetworkPermission =
        rememberPermissionState(dae.aevum.utils.LocalNetworkUrls.ACCESS_LOCAL_NETWORK_PERMISSION)
    val rationale = stringResource(R.string.local_network_permission_rationale)

    LaunchedEffect(viewModel) {
        viewModel.accessRequired.collect {
            Timber.i("LAN proofreader failed; showing rationale and requesting ACCESS_LOCAL_NETWORK")
            // Snackbar gives context; permission dialog is launched in parallel (don't wait for dismiss).
            launch { snackbarHostState.showSnackbar(rationale) }
            if (!localNetworkPermission.status.isGranted) {
                localNetworkPermission.launchPermissionRequest()
            }
        }
    }
}

@Composable
private fun AppTopBar(
    currentScreen: Screen,
    showTokenExpiredBanner: Boolean,
) {
    Column {
        TopBar(currentScreen = currentScreen)
        if (showTokenExpiredBanner) {
            TokenExpiredBanner()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TokenExpiredBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = stringResource(R.string.token_expired_error),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
@Preview
private fun TopBarPreview1() {
    TopBar(currentScreen = Screen.Issues)
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TopBarPreview2() {
    TopBar(currentScreen = Screen.IssueView)
}

@Composable
private fun TopBar(currentScreen: Screen) {
    TopAppBar(
        modifier =
            Modifier.windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    tint = Color.Unspecified,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Crossfade(targetState = currentScreen) { screen ->
                    Text(
                        text = stringResource(screen.resourceId),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        },
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    activeUser: UserUiModel?,
) {
    BottomNavigation(
        modifier =
            Modifier.windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        bottomNav.forEach { screen ->
            val route = currentDestination?.route.orEmpty()
            val selected =
                when (screen) {
                    Screen.Users -> {
                        route == Screen.Users.route || route.startsWith("users/")
                    }

                    else -> {
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    }
                }
            val enabled =
                when (screen) {
                    Screen.Users -> true
                    else -> activeUser != null
                }

            val color =
                if (!enabled) {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                } else if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                }

            BottomNavigationItem(
                icon = {
                    Icon(
                        screen.icon,
                        contentDescription = null,
                        tint = color,
                    )
                },
                label = {
                    Text(
                        stringResource(screen.resourceId),
                        color = color,
                    )
                },
                enabled = enabled,
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                    }
                },
            )
        }
    }
}

@Composable
private fun Fab(
    navController: NavHostController,
    currentScreen: Screen,
    worklogViewModel: WorklogViewModel = hiltViewModel(),
    snackbarHostState: androidx.compose.material.SnackbarHostState,
) {
    val uploadState by worklogViewModel.uploadState.collectAsState()
    val splitError by worklogViewModel.splitError.collectAsState()
    val successMessage =
        (uploadState as? dae.aevum.utils.WorklogUploadResult.Success)?.let {
            stringResource(R.string.upload_worklogs_success, it.uploadedCount)
        }
    val partialMessage =
        (uploadState as? dae.aevum.utils.WorklogUploadResult.PartialFailure)?.let {
            stringResource(R.string.upload_worklogs_partial, it.failedIssueKeys.joinToString())
        }
    val failedFallback = stringResource(R.string.upload_worklogs_failed)
    val splitFailedMessage = stringResource(R.string.split_worklog_failed)

    LaunchedEffect(uploadState) {
        when (val state = uploadState) {
            is dae.aevum.utils.WorklogUploadResult.Success -> {
                successMessage?.let { snackbarHostState.showSnackbar(it) }
                worklogViewModel.clearUploadState()
            }

            is dae.aevum.utils.WorklogUploadResult.PartialFailure -> {
                partialMessage?.let { snackbarHostState.showSnackbar(it) }
                worklogViewModel.clearUploadState()
            }

            is dae.aevum.utils.WorklogUploadResult.Failed -> {
                snackbarHostState.showSnackbar(state.message ?: failedFallback)
                worklogViewModel.clearUploadState()
            }

            else -> {
                // Idle / success already handled above
            }
        }
    }

    LaunchedEffect(splitError) {
        if (splitError != null) {
            snackbarHostState.showSnackbar(splitFailedMessage)
            worklogViewModel.clearSplitError()
        }
    }

    if (currentScreen is Screen.Worklogs) {
        val pendingWorklogs by worklogViewModel.pendingWorklogs.collectAsState(initial = emptyList())
        val nonActivePending = pendingWorklogs.filter { !it.active }
        val isUploading = uploadState is dae.aevum.utils.WorklogUploadResult.Running
        var showBlankSummaryDialog by remember { mutableStateOf(false) }
        val blankCount = worklogViewModel.countBlankSummaries(pendingWorklogs)
        val uploadContentDescription =
            if (isUploading) {
                stringResource(R.string.upload_worklogs_running)
            } else {
                stringResource(R.string.upload_worklogs)
            }

        if (showBlankSummaryDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showBlankSummaryDialog = false },
                title = { Text(stringResource(R.string.blank_summary_upload_title)) },
                text = {
                    Text(
                        text =
                            androidx.compose.ui.res.pluralStringResource(
                                R.plurals.blank_summary_upload_message,
                                blankCount,
                                blankCount,
                            ),
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showBlankSummaryDialog = false
                            Timber.v("Uploading pending worklogs to Jira (blank summaries confirmed)")
                            worklogViewModel.postPendingWorklogs()
                        },
                    ) {
                        Text(stringResource(R.string.upload_anyway))
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { showBlankSummaryDialog = false },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }

        if (nonActivePending.isNotEmpty() || isUploading) {
            FloatingActionButton(
                onClick = {
                    if (!isUploading) {
                        if (blankCount > 0) {
                            showBlankSummaryDialog = true
                        } else {
                            Timber.v("Uploading pending worklogs to Jira")
                            worklogViewModel.postPendingWorklogs()
                        }
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                if (isUploading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = uploadContentDescription,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    } else if (currentScreen is Screen.Users) {
        FloatingActionButton(
            onClick = {
                Timber.v("Adding new user")
                navController.navigate("users/new")
            },
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_user),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    userViewModel: UserViewModel,
    startDestination: String,
    pendingSearchQuery: String? = null,
    onPendingSearchQueryConsumed: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ) {
            composable(Screen.Users.route) {
                UserListScreen(
                    selectUser = { id ->
                        userViewModel.selectUser(id)
                    },
                    openSettings = { id ->
                        navController.navigate("users/$id/settings")
                    },
                )
            }
            composable(Screen.UserSettings.route) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull()
                if (userId != null) {
                    UserSettingsScreen(
                        userId = userId,
                        viewModel = userViewModel,
                        onUserDeleted = {
                            navController.popBackStack()
                        },
                    )
                }
            }

            composable(Screen.Issues.route) {
                val viewModel: IssueViewModel = hiltViewModel()
                IssueListScreen(
                    initialSearchQuery = pendingSearchQuery,
                    onInitialSearchQueryConsumed = onPendingSearchQueryConsumed,
                    onNavigateToIssue = { issueId ->
                        navController.navigate("issueView/${issueId.value}")
                    },
                    onTogglePinIssue = { issueId ->
                        viewModel.toggleIssuePin(issueId)
                    },
                    onNavigateToSummary = {
                        navController.navigate(Screen.Summary.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                        }
                    },
                )
            }
            composable(Screen.IssueView.route) { backStackEntry ->
                val viewModel: IssueViewModel = hiltViewModel()
                val id = backStackEntry.arguments?.getString("issueId")?.let { IssueId(it) }
                IssueScreen(
                    issueId = id,
                    stopLogging = {
                        navController.navigate("worklog/new/$it")
                    },
                    deleteWorklog = { worklogId ->
                        viewModel.deleteLog(worklogId = worklogId)
                    },
                    changeIssueId = { worklogId ->
                        // navController.popBackStack(Screen.Worklogs.route, false)
                        navController.navigate("worklogChangeIssue/$worklogId")
                    },
                    splitWorklog = { worklogId ->
                        // navController.popBackStack(Screen.Worklogs.route, false)
                        navController.navigate("worklog/split/issueSelector/$worklogId")
                    },
                    incrementWorklogTime = { workId, amount ->
                        viewModel.incrementWorklogTime(workId, amount)
                    },
                    decrementWorklogTime = { workId, amount ->
                        viewModel.decrementWorklogTime(workId, amount)
                    },
                    onNavigateToEditor = { worklogId ->
                        navController.navigate("worklogEditor/$worklogId")
                    },
                )
            }
            composable(Screen.Worklogs.route) {
                val viewModel: WorklogViewModel = hiltViewModel()
                WorklogListScreen(
                    onNavigateToEditor = { worklogId ->
                        navController.navigate("worklogEditor/$worklogId")
                    },
                    stopLogging = { worklogId ->
                        navController.navigate("worklog/new/$worklogId")
                    },
                    deleteWorklog = { worklogId ->
                        viewModel.deleteLog(worklogId = worklogId)
                    },
                    changeIssueId = { worklogId ->
                        // navController.popBackStack(Screen.Worklogs.route, false)
                        navController.navigate("worklogChangeIssue/$worklogId")
                    },
                    splitWorklog = { worklogId ->
                        // navController.popBackStack(Screen.Worklogs.route, false)
                        navController.navigate("worklog/split/issueSelector/$worklogId")
                    },
                    incrementWorklogTime = { workId, amount ->
                        viewModel.incrementWorklogTime(workId, amount)
                    },
                    decrementWorklogTime = { workId, amount ->
                        viewModel.decrementWorklogTime(workId, amount)
                    },
                )
            }
            composable(Screen.Summary.route) {
                val viewModel: WorklogViewModel = hiltViewModel()
                val targets by viewModel.timesheetTargets.collectAsState()
                WorklogSummaryScreen(
                    viewModel = viewModel,
                    onOpenBarChart = { navController.navigate(Screen.SummaryBar.route) },
                    onOpenDayDetailsForDay = { day ->
                        navController.navigate("summary/details/$day")
                    },
                    weekdayTargetHours = targets.weekdayTargetHours,
                    weekTargetHours = targets.weekTargetHours,
                    workingDays = targets.workingDays,
                )
            }
            composable(Screen.SummaryBar.route) {
                val viewModel: WorklogViewModel = hiltViewModel()
                val targets by viewModel.timesheetTargets.collectAsState()
                WorklogBarChartScreen(
                    viewModel = viewModel,
                    weekdayTargetHours = targets.weekdayTargetHours,
                    workingDays = targets.workingDays,
                )
            }
            composable(Screen.SummaryDetails.route) {
                val viewModel: WorklogViewModel = hiltViewModel()
                WorklogDayDetailsScreen(viewModel = viewModel)
            }
            composable(Screen.SummaryDetailsForDay.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val dayArg = backStackEntry.arguments?.getString("day")
                val day = dayArg?.let { java.time.LocalDate.parse(it) }
                WorklogDayDetailsScreen(viewModel = viewModel, day = day)
            }
            composable(Screen.WorklogsChangeId.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val worklogId = backStackEntry.arguments?.getString("worklogId")?.toLong()

                if (worklogId != null) {
                    IssueList(
                        onIssueSelected = { issueId ->
                            viewModel.moveWorklogIssue(worklogId, issueId)

//                            val options = NavOptions.Builder()
//                                .setPopUpTo(Screen.Issues.route, false)
//                                .build()
                            navController.popBackStack()
                            navController.navigate("issueView/${issueId.value}")
                        },
                    )
                }
            }
            composable(Screen.WorklogEditor.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val worklogId = backStackEntry.arguments?.getString("worklogId")?.toLong()
                val scope = rememberCoroutineScope()
                WorklogEditorScreen(
                    worklogId = worklogId,
                    updateWorklog = { id, from, to, summary ->
                        scope.launch {
                            viewModel.updateWorklogAwait(id, from, to, summary)
                            navController.popBackStack()
                        }
                    },
                    viewModel = viewModel,
                    splitWorklog = { id ->
                        // navController.popBackStack(Screen.Worklogs.route, false)
                        navController.navigate("worklog/split/issueSelector/$id")
                    },
                )
            }
            composable(Screen.WorklogSplitIssueSelector.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val worklogId = backStackEntry.arguments?.getString("worklogId")?.toLong()
                WorklogSplitterIssueScreen(
                    worklogId = worklogId,
                    issueSelected = { id ->
                        // navController.popBackStack()
                        navController.navigate("worklog/split/timeSelector/$worklogId/${id.value}")
                    },
                    viewModel = viewModel,
                )
            }
            composable(Screen.WorklogSplitTimeSelector.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val worklogId = backStackEntry.arguments?.getString("worklogId")?.toLong()
                val issueId = backStackEntry.arguments?.getString("issueId")

                if (worklogId != null && issueId != null) {
                    WorklogSplitterTimeScreen(
                        worklogId = worklogId,
                        issueId = IssueId(issueId),
                        viewModel = viewModel,
                        splitWorklog = { oldWorklogId, newIssueId, splitTime ->
                            viewModel.splitWorklog(
                                oldWorklogId,
                                newIssueId,
                                splitTime,
                            ) { newWorklogId ->
                                navController.popBackStack(Screen.Worklogs.route, false)
                                navController.navigate("worklogEditor/$newWorklogId")
                            }
                        },
                    )
                }
            }

            dialog(Screen.WorklogNewDialog.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val worklogId =
                    backStackEntry.arguments?.getString("worklogId")?.toLong()
                        ?: throw IllegalStateException("No worklog ID")
                CompletedWorklogDialog(
                    navController = navController,
                    worklogId = worklogId,
                    viewModel = viewModel,
                )
            }
            dialog(Screen.UserNewDialog.route) {
                val viewModel: UserViewModel = hiltViewModel()
                AddUserDialog(
                    navController = navController,
                    viewModel = viewModel,
                )
            }
        }
    }
}
