package com.kaeru.app.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Language
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kaeru.app.R
import com.kaeru.app.ui.components.Material3SettingsGroup
import com.kaeru.app.ui.components.Material3SettingsItem
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.kaeru.app.tracking.TrackingViewModel
import com.kaeru.app.ui.components.EnumDialog
import com.kaeru.app.ui.components.ReleaseNotesCard
import android.provider.Settings
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.runtime.setValue
import com.kaeru.app.data.utils.GithubRelease
import com.kaeru.app.data.utils.UpdateManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TrackingViewModel,
    onBack: () -> Unit,
    onAppearanceClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onUpdaterClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    updateRelease: GithubRelease?
) {
    val uriHandler = LocalUriHandler.current
    val checkUpdatesEnabled by viewModel.checkUpdatesOnStart.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    var showAppLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val appLanguage by viewModel.appLanguage.collectAsState()
    var showChangelog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Material3SettingsGroup(
                title = stringResource(R.string.settings_interface),
                items = buildList {
                    add(
                        Material3SettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.Palette),
                            title = { Text(stringResource(R.string.settings_themes)) },
                            onClick = onAppearanceClick
                        )
                    )
                }
            )

            Material3SettingsGroup(
                title = stringResource(R.string.settings_storage),
                items = buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.ic_restore),
                            title = { Text(stringResource(R.string.settings_backup)) },
                            onClick = onBackupClick
                        )
                    )
                }
            )

            if (showAppLanguageDialog) {
                EnumDialog(
                    onDismiss = { showAppLanguageDialog = false },
                    onSelect = { selectedLang ->
                        viewModel.setLanguage(selectedLang)
                        showAppLanguageDialog = false
                        activity?.recreate()
                    },
                    title = stringResource(R.string.app_language),
                    current = appLanguage,
                    values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                    valueText = { code ->
                        LanguageCodeToName.getOrElse(code) { stringResource(R.string.system_default) }
                    }
                )
            }

            Material3SettingsGroup(
                title = stringResource(R.string.app_language),
                items = listOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Material3SettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.Language),
                            title = { Text(stringResource(R.string.app_language)) },
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APP_LOCALE_SETTINGS,
                                        "package:${context.packageName}".toUri()
                                    )
                                )
                            }
                        )
                    } else {
                        Material3SettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.Language),
                            title = { Text(stringResource(R.string.app_language)) },
                            description = {
                                Text(
                                    LanguageCodeToName.getOrElse(appLanguage) { stringResource(R.string.system_default) }
                                )
                            },
                            onClick = { showAppLanguageDialog = true }
                        )
                    }
                )
            )

            Material3SettingsGroup(
                title = stringResource(R.string.settings_system),
                items = buildList {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.ic_update),
                            title = { Text(stringResource(R.string.settings_updater)) },
                            onClick = onUpdaterClick
                        )
                    )
                    add(
                        Material3SettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.Newspaper),
                            title = { Text("Changelog") },
                            onClick = { showChangelog = true }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.Info),
                            title = { Text(stringResource(R.string.settings_about)) },
                            onClick = onAboutClick
                        )
                    )
                    updateRelease?.let { release ->
                        if (checkUpdatesEnabled && updateRelease != null) {
                            if (checkUpdatesEnabled) {
                                add(
                                    Material3SettingsItem(
                                        icon = painterResource(R.drawable.ic_update),
                                        title = {
                                            Text(
                                                text = stringResource(R.string.new_version_available),
                                            )
                                        },
                                        description = {
                                            Text(
                                                text = release.tagName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        showBadge = true,
                                        onClick = { uriHandler.openUri(release.htmlUrl) }
                                    )
                                )
                            }
                        }
                    }
                }
            )
            if (showChangelog) {
                ChangelogSheet(onDismiss = { showChangelog = false })
            }
            updateRelease?.let { release ->
                    if (checkUpdatesEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ReleaseNotesCard(release = release)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
val SYSTEM_DEFAULT = "system"
val LanguageCodeToName = mapOf(
    "en" to "English",
    "pt-BR" to "Português (Brasil)"
)