package com.kaeru.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.api.services.drive.DriveScopes
import com.kaeru.app.AppDestinations
import com.kaeru.app.R
import com.kaeru.app.TopAppBar
import com.kaeru.app.data.helper.GoogleDriveHelper
import com.kaeru.app.data.utils.GithubRelease
import com.kaeru.app.tracking.TrackingViewModel
import com.kaeru.app.tracking.database.BackupLog
import com.kaeru.app.tracking.utils.isDeliveredStatus
import com.kaeru.app.ui.components.EditProfileDialog
import com.kaeru.app.ui.components.ProfileDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

enum class BackupFlow { NONE, BACKUP_MENU, OVERWRITE_LIST, RESTORE_MENU, RESTORE_LIST, HISTORY }
data class BackupFile(val id: String, val name: String, val date: String, val isLatest: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaeruTabsScreen(
    viewModel: TrackingViewModel,
    updateRelease: GithubRelease?,
    onNavigateToResult: (String, String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val defaultTab by viewModel.defaultOpenTab.collectAsState()
    var currentTab by rememberSaveable(defaultTab) { mutableStateOf(defaultTab) }
    val isSlimNav by viewModel.isSlimNav.collectAsState()
    val bottomBarHeight = if (isSlimNav) 80.dp else 96.dp

    val localName by viewModel.userName.collectAsState()
    val localBio by viewModel.userBio.collectAsState()
    val localAvatar by viewModel.userAvatar.collectAsState(initial = null)

    val historyList by viewModel.historyList.collectAsState()
    val defaultFilter by viewModel.defaultHistoryFilter.collectAsState()
    var currentFilter by rememberSaveable(defaultFilter) { mutableStateOf(defaultFilter) }

    val filteredCount = remember(historyList, currentFilter) {
        when (currentFilter) {
            TrackingFilter.IN_TRANSIT -> historyList.count { !it.lastStatus.isDeliveredStatus() }
            TrackingFilter.DELIVERED -> historyList.count { it.lastStatus.isDeliveredStatus() }
            TrackingFilter.ALL -> historyList.size
        }
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    var currentBackupFlow by remember { mutableStateOf(BackupFlow.NONE) }
    var cloudBackups by remember { mutableStateOf<List<BackupFile>>(emptyList()) }
    val scope = rememberCoroutineScope()

    if (showEditDialog) {
        EditProfileDialog(
            currentName = localName,
            currentBio = localBio,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newBio ->
                viewModel.updateProfile(newName, newBio)
                showEditDialog = false
            }
        )
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.updateAvatar(it.toString()) }
    }

    val context = LocalContext.current
    var isUserLoggedIn by remember { mutableStateOf(false) }
    val driveHelper = remember { GoogleDriveHelper(context) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val loginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            android.widget.Toast.makeText(context, "Logado como: ${account.email}", android.widget.Toast.LENGTH_SHORT).show()
            isUserLoggedIn = true
            driveHelper.initializeDrive(account)
        } catch (e: ApiException) {
            android.widget.Toast.makeText(context, "Erro no login: ${e.statusCode}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    val checkUpdatesEnabled by viewModel.checkUpdatesOnStart.collectAsState()
    val hasUpdate = updateRelease != null && checkUpdatesEnabled

    LaunchedEffect(Unit) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            isUserLoggedIn = true
            driveHelper.initializeDrive(account)
        }
    }

    val backupLogs by viewModel.backupLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                currentTab = currentTab,
                packageCount = filteredCount,
                userAvatar = localAvatar,
                hasUpdate = hasUpdate,
                onAvatarClick = { showProfileDialog = true },
                onSettingsClick = onNavigateToSettings
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.height(bottomBarHeight)) {
                AppDestinations.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == currentTab,
                        onClick = { currentTab = destination },
                        label = if (isSlimNav) null else { { Text(stringResource(destination.label)) }},
                        alwaysShowLabel = !isSlimNav,
                        icon = {
                            when (val icon = destination.icon) {
                                is ImageVector -> Icon(imageVector = icon, contentDescription = null)
                                is Int -> Icon(painter = painterResource(id = icon), contentDescription = null)
                            }
                        }
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val duration = 400
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally(animationSpec = tween(duration)) { it } + fadeIn(tween(duration)) togetherWith
                                slideOutHorizontally(animationSpec = tween(duration)) { -it } + fadeOut(tween(duration))
                    } else {
                        slideInHorizontally(animationSpec = tween(duration)) { -it } + fadeIn(tween(duration)) togetherWith
                                slideOutHorizontally(animationSpec = tween(duration)) { it } + fadeOut(tween(duration))
                    }
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    AppDestinations.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            currentFilter = currentFilter,
                            onFilterChange = { newFilter -> currentFilter = newFilter },
                            onNavigateToResult = { code -> onNavigateToResult(code, "Auto") }
                        )
                    }
                    AppDestinations.SEARCH -> SearchScreen(onNavigateToResult = onNavigateToResult)
                    AppDestinations.CHARTS -> StatisticsScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileDialog(
            userName = localName,
            userBio = localBio,
            userAvatar = localAvatar,
            isUserLoggedIn = isUserLoggedIn,
            onDismiss = { showProfileDialog = false },
            onLoginClick = { loginLauncher.launch(googleSignInClient.signInIntent) },
            onLogoutClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    isUserLoggedIn = false
                    driveHelper.clearDrive()
                }
            },
            onMakeBackup = {
                if (isUserLoggedIn) {
                currentBackupFlow = BackupFlow.BACKUP_MENU
            } else {
                android.widget.Toast.makeText(context, "Conecte uma conta Google para salvar na nuvem.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onRestoreBackup = {
                if (isUserLoggedIn) {
                currentBackupFlow = BackupFlow.RESTORE_MENU
            } else {
                android.widget.Toast.makeText(context, "Conecte uma conta Google para restaurar da nuvem.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onViewHistory = { currentBackupFlow = BackupFlow.HISTORY },
            onPhotoClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onEditProfileClick = { showEditDialog = true }
        )
    }

    when (currentBackupFlow) {
        BackupFlow.NONE -> {}

        BackupFlow.BACKUP_MENU -> {
            BackupMenuDialog(
                onDismiss = { currentBackupFlow = BackupFlow.NONE },
                onNewBackup = {
                    scope.launch {
                        android.widget.Toast.makeText(context, "Iniciando backup...", android.widget.Toast.LENGTH_SHORT).show()
                        val jsonBackup = viewModel.exportDatabaseToJson()
                        val existingBackups = driveHelper.listBackups()

                        if (existingBackups.size >= 5) {
                            driveHelper.deleteFile(existingBackups.last().id)
                        }

                        val success = driveHelper.uploadBackup(jsonBackup)

                        if (success) {
                            viewModel.logBackupEvent(
                                type = "NA NUVEM",
                                action = "BACKUP",
                                fileName = "${System.currentTimeMillis()}.json"
                            )
                            android.widget.Toast.makeText(context, "Backup salvo com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Erro ao salvar backup.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        currentBackupFlow = BackupFlow.NONE
                    }
                },
                onOverwrite = {
                    scope.launch {
                        android.widget.Toast.makeText(context, "Buscando backups...", android.widget.Toast.LENGTH_SHORT).show()
                        val realBackups = driveHelper.listBackups()
                        val sdf = SimpleDateFormat("HH:mm:ss • dd/MM/yyyy", Locale.getDefault())

                        cloudBackups = realBackups.mapIndexed { index, file ->
                            val formattedDate = if (file.modifiedTime != null) {
                                sdf.format(Date(file.modifiedTime.value))
                            } else "Unknown date"

                            BackupFile(id = file.id, name = file.name, date = formattedDate, isLatest = index == 0)
                        }
                        currentBackupFlow = BackupFlow.OVERWRITE_LIST
                    }
                }
            )
        }

        BackupFlow.OVERWRITE_LIST -> {
            BackupSelectionDialog(
                title = stringResource(R.string.backup_overwrite),
                backups = cloudBackups,
                onDismiss = { currentBackupFlow = BackupFlow.NONE },
                onSelect = { selectedBackup ->
                    scope.launch {
                        android.widget.Toast.makeText(context, "Sobrescrevendo...", android.widget.Toast.LENGTH_SHORT).show()

                        val jsonBackup = viewModel.exportDatabaseToJson()
                        val success = driveHelper.uploadBackup(jsonBackup, selectedBackup.id)

                        if (success) {
                            viewModel.logBackupEvent(
                                type = "NA NUVEM",
                                action = "BACKUP",
                                fileName = "${System.currentTimeMillis()}.json"
                            )
                            android.widget.Toast.makeText(context, "Backup sobrescrito com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Erro ao sobrescrever.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        currentBackupFlow = BackupFlow.NONE
                    }
                }
            )
        }

        BackupFlow.RESTORE_MENU -> {
            RestoreMenuDialog(
                onDismiss = { currentBackupFlow = BackupFlow.NONE },
                onRestoreLatest = {
                    scope.launch {
                        android.widget.Toast.makeText(context, "Restaurando...", android.widget.Toast.LENGTH_SHORT).show()

                        val existing = driveHelper.listBackups()
                        val latest = existing.firstOrNull()

                        if (latest != null) {
                            val downloadedJson = driveHelper.restoreBackup(latest.id)
                            if (downloadedJson != null) {
                                viewModel.importJsonToDatabase(downloadedJson)

                                viewModel.logBackupEvent(
                                    type = "DA NUVEM",
                                    action = "RESTAURADO",
                                    fileName = "${System.currentTimeMillis()}.json"
                                )
                                android.widget.Toast.makeText(context, "Pacotes restaurados!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Erro ao baixar arquivo.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Nenhum backup encontrado.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        currentBackupFlow = BackupFlow.NONE
                    }
                },
                onChooseFromList = {
                    scope.launch {
                        android.widget.Toast.makeText(context, "Buscando backups...", android.widget.Toast.LENGTH_SHORT).show()

                        val realBackups = driveHelper.listBackups()
                        val sdf = SimpleDateFormat("HH:mm:ss • dd/MM/yyyy", Locale.getDefault())

                        cloudBackups = realBackups.mapIndexed { index, file ->
                            val formattedDate = if (file.modifiedTime != null) {
                                sdf.format(Date(file.modifiedTime.value))
                            } else "Data desconhecida"

                            BackupFile(id = file.id, name = file.name, date = formattedDate, isLatest = index == 0)
                        }
                        currentBackupFlow = BackupFlow.RESTORE_LIST
                    }
                }
            )
        }

        BackupFlow.RESTORE_LIST -> {
            BackupSelectionDialog(
                title = stringResource(R.string.choose_backup),
                backups = cloudBackups,
                onDismiss = { currentBackupFlow = BackupFlow.NONE },
                onSelect = { selectedBackup ->
                    scope.launch {
                        android.widget.Toast.makeText(context, "Baixando dados...", android.widget.Toast.LENGTH_SHORT).show()
                        val downloadedJson = driveHelper.restoreBackup(selectedBackup.id)

                        if (downloadedJson != null) {
                            viewModel.importJsonToDatabase(downloadedJson)

                            viewModel.logBackupEvent(
                                type = "DA NUVEM",
                                action = "RESTAURADO",
                                fileName = "${System.currentTimeMillis()}.json"
                            )
                            android.widget.Toast.makeText(context, "Pacotes restaurados!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Erro ao restaurar", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        currentBackupFlow = BackupFlow.NONE
                    }
                }
            )
        }

        BackupFlow.HISTORY -> {
            BackupHistoryDialog(
                logs = backupLogs,
                onDismiss = { currentBackupFlow = BackupFlow.NONE },
                onClear = { viewModel.clearBackupHistory() }
            )
        }
    }
}

@Composable
fun BackupMenuDialog(
    onDismiss: () -> Unit,
    onNewBackup: () -> Unit,
    onOverwrite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.make_backup)) },
        text = { Text(stringResource(R.string.make_backup_desc)) },
        confirmButton = {
            Button(onClick = onNewBackup) { Text("Criar Novo") }
        },
        dismissButton = {
            TextButton(onClick = onOverwrite) { Text("Sobrescrever") }
        }
    )
}

@Composable
fun RestoreMenuDialog(
    onDismiss: () -> Unit,
    onRestoreLatest: () -> Unit,
    onChooseFromList: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_backup)) },
        text = { Text(stringResource(R.string.restore_backup_desc)) },
        confirmButton = {
            Button(onClick = onRestoreLatest) { Text("Mais Recente") }
        },
        dismissButton = {
            TextButton(onClick = onChooseFromList) { Text("Escolher") }
        }
    )
}

@Composable
fun BackupSelectionDialog(
    title: String,
    backups: List<BackupFile>,
    onDismiss: () -> Unit,
    onSelect: (BackupFile) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (backups.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_backup_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AlertDialogDefaults.textContentColor
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(backups) { backup ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onSelect(backup) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = backup.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (backup.isLatest) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = "Mais recente",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = backup.date,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                }
            }
        }
    }
}

@Composable
fun BackupHistoryDialog(
    logs: List<BackupLog>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    val sdf = SimpleDateFormat("HH:mm:ss • dd/MM/yyyy", LocalLocale.current.platformLocale)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(28.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Histórico",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (logs.isEmpty()) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Nenhum registro encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(logs) { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp)
                            ) {
                                Text(
                                    text = log.fileName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = sdf.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                val actionText = log.action.lowercase().replaceFirstChar { it.uppercase() }
                                val typeText = log.type.lowercase().replaceFirstChar { it.lowercase() }

                                Text(
                                    text = "$actionText $typeText",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}