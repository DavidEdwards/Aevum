package dae.aevum.ui.composables

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import dae.aevum.R
import dae.aevum.ui.composables.dialogs.AddUserDialog
import dae.aevum.ui.composables.dialogs.CompletedWorklogDialog
import dae.aevum.ui.composables.issues.IssueListScreen
import dae.aevum.ui.composables.issues.IssueScreen
import dae.aevum.ui.composables.users.UserListScreen
import dae.aevum.ui.composables.worklogs.WorklogEditorScreen
import dae.aevum.ui.composables.worklogs.WorklogListScreen
import dae.aevum.ui.composables.worklogs.WorklogSplitterIssueScreen
import dae.aevum.ui.composables.worklogs.WorklogSplitterTimeScreen
import dae.aevum.ui.models.UserUiModel
import dae.aevum.ui.viewmodels.IssueViewModel
import dae.aevum.ui.viewmodels.UserViewModel
import dae.aevum.ui.viewmodels.WorklogViewModel
import dae.aevum.utils.IssueId
import timber.log.Timber

sealed class Screen(
    val route: String,
    @StringRes val resourceId: Int,
    val icon: ImageVector
) {
    object Users : Screen("users", R.string.users, Icons.Outlined.AccountCircle)
    object Issues : Screen("issues", R.string.issues, Icons.Outlined.Home)
    object IssueView : Screen("issueView/{issueId}", R.string.issue_view, Icons.Outlined.Search)
    object Worklogs : Screen("worklogs", R.string.pending, Icons.Outlined.List)
    object WorklogEditor :
        Screen("worklogEditor/{worklogId}", R.string.worklog_editor, Icons.Outlined.Edit)

    object WorklogSplitIssueSelector :
        Screen(
            "worklog/split/issueSelector/{worklogId}",
            R.string.issue_selector,
            Icons.Outlined.Edit
        )

    object WorklogSplitTimeSelector :
        Screen(
            "worklog/split/timeSelector/{worklogId}/{issueId}",
            R.string.time_selector,
            Icons.Outlined.Edit
        )

    object WorklogNewDialog :
        Screen("worklog/new/{worklogId}", R.string.worklog_new_dialog, Icons.Outlined.Create)

    object UserNewDialog :
        Screen("users/new", R.string.add_user, Icons.Outlined.Add)
}

val screens = listOf(
    Screen.Users,
    Screen.Issues,
    Screen.IssueView,
    Screen.Worklogs,
    Screen.WorklogEditor,
    Screen.WorklogNewDialog,
    Screen.UserNewDialog,
    Screen.WorklogSplitIssueSelector,
    Screen.WorklogSplitTimeSelector,
)

val bottomNav = listOf(
    Screen.Users,
    Screen.Issues,
    Screen.Worklogs,
)

@Composable
fun Router() {
    val navController = rememberNavController()

    var currentScreen by remember {
        mutableStateOf<Screen>(Screen.Issues)
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val screen = screens.find { it.route == backStackEntry.destination.route }
                ?: return@collect

            currentScreen = screen
        }
    }

    AppScaffold(navController = navController, currentScreen = currentScreen)
}

@Composable
fun AppScaffold(
    navController: NavHostController,
    currentScreen: Screen,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val activeUser by userViewModel.activeUser.collectAsState(initial = null)
    val startDestination = if (activeUser == null) {
        Screen.Users.route
    } else {
        Screen.Issues.route
    }

    Scaffold(
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            BottomBar(
                navController = navController,
                activeUser = activeUser
            )
        },
        topBar = {
            TopBar(currentScreen = currentScreen)
        },
        floatingActionButton = {
            Fab(navController = navController, currentScreen = currentScreen)
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            innerPadding = innerPadding,
            userViewModel = userViewModel,
            startDestination = startDestination
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
private fun TopBar(
    currentScreen: Screen
) {
    TopAppBar(
        title = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                tint = Color.Unspecified
            )
            Text(
                text = stringResource(currentScreen.resourceId),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    activeUser: UserUiModel?
) {
    BottomNavigation(
        backgroundColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        bottomNav.forEach { screen ->
            val selected =
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val enabled = when (screen) {
                Screen.Users -> true
                else -> activeUser != null
            }

            val color = if (!enabled)
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            else if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onBackground

            BottomNavigationItem(
                icon = {
                    Icon(
                        screen.icon, contentDescription = null,
                        tint = color
                    )
                },
                label = {
                    Text(
                        stringResource(screen.resourceId),
                        color = color
                    )
                },
                enabled = enabled,
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                    }
                }
            )
        }
    }
}

@Composable
private fun Fab(
    navController: NavHostController,
    currentScreen: Screen
) {
    if (currentScreen is Screen.Worklogs) {
        val worklogViewModel: WorklogViewModel = hiltViewModel()
        val pendingWorklogs by worklogViewModel.pendingWorklogs.collectAsState(initial = emptyList())
        val nonActivePending = pendingWorklogs.filter { !it.active }

        if (nonActivePending.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    Timber.v("Uploading pending worklogs to Jira")
                    worklogViewModel.postPendingWorklogs()
                },
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = stringResource(R.string.upload_worklogs)
                )
            }
        }
    } else if (currentScreen is Screen.Users) {
        FloatingActionButton(
            onClick = {
                Timber.v("Adding new user")
                navController.navigate("users/new")
            },
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.add_user)
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    userViewModel: UserViewModel,
    startDestination: String
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Screen.Users.route) {
                UserListScreen(
                    selectUser = { id ->
                        userViewModel.selectUser(id)
                    },
                    deleteUser = { id ->
                        userViewModel.removeUser(id)
                    }
                )
            }

            composable(Screen.Issues.route) {
                val viewModel: IssueViewModel = hiltViewModel()
                IssueListScreen(
                    onNavigateToIssue = { issueId ->
                        navController.navigate("issueView/${issueId.value}")
                    },
                    onTogglePinIssue = { issueId ->
                        viewModel.toggleIssuePin(issueId)
                    }
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
                    onUpdate = { worklogId, from, to, summary ->
                        viewModel.updateWorklog(worklogId, from, to, summary)
                    },
                    deleteWorklog = { worklogId ->
                        viewModel.deleteLog(worklogId = worklogId)
                    }
                )
            }
            composable(Screen.Worklogs.route) {
                val viewModel: WorklogViewModel = hiltViewModel()
                WorklogListScreen(
                    onNavigateToEditor = { worklogId ->
                        navController.navigate("worklogEditor/${worklogId}")
                    },
                    onUpdate = { worklogId, from, to, summary ->
                        viewModel.updateWorklog(worklogId, from, to, summary)
                    },
                    stopLogging = { worklogId ->
                        navController.navigate("worklog/new/$worklogId")
                    },
                    deleteWorklog = { worklogId ->
                        viewModel.deleteLog(worklogId = worklogId)
                    }
                )
            }
            composable(Screen.WorklogEditor.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val worklogId = backStackEntry.arguments?.getString("worklogId")?.toLong()
                WorklogEditorScreen(
                    worklogId = worklogId,
                    updateWorklog = { id, from, to, summary ->
                        viewModel.updateWorklog(id, from, to, summary)
                        navController.popBackStack()
                    },
                    viewModel = viewModel,
                    splitWorklog = { id ->
                        navController.navigate("worklog/split/issueSelector/$id")
                    }
                )
            }
            composable(Screen.WorklogSplitIssueSelector.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val worklogId = backStackEntry.arguments?.getString("worklogId")?.toLong()
                WorklogSplitterIssueScreen(
                    worklogId = worklogId,
                    issueSelected = { id ->
                        navController.navigate("worklog/split/timeSelector/$worklogId/${id.value}")
                    },
                    viewModel = viewModel
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
                                splitTime
                            ) { newWorklogId ->
                                navController.popBackStack(Screen.Worklogs.route, false)
                                navController.navigate("worklogEditor/${newWorklogId}")
                            }
                        }
                    )
                }
            }

            dialog(Screen.WorklogNewDialog.route) { backStackEntry ->
                val viewModel: WorklogViewModel = hiltViewModel()
                val worklogId = backStackEntry.arguments?.getString("worklogId")?.toLong()
                    ?: throw IllegalStateException("No worklog ID")
                CompletedWorklogDialog(
                    navController = navController,
                    worklogId = worklogId,
                    viewModel = viewModel
                )
            }
            dialog(Screen.UserNewDialog.route) {
                val viewModel: UserViewModel = hiltViewModel()
                AddUserDialog(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}