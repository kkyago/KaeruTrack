package com.kaeru.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaeru.app.AppDestinations
import com.kaeru.app.R
import com.kaeru.app.tracking.TrackingViewModel
import com.kaeru.app.ui.components.EnumDialog
import com.kaeru.app.ui.components.IconSwitch
import com.kaeru.app.ui.components.Material3SettingsGroup
import com.kaeru.app.ui.components.Material3SettingsItem
import com.kaeru.app.ui.screens.TrackingFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel: TrackingViewModel,
    onBack: () -> Unit,
    onEditColorsClick: () -> Unit
) {
    val isSwipeEnabled by viewModel.isSwipeToDeleteEnabled.collectAsState()
    val isSlimNav by viewModel.isSlimNav.collectAsState()
    val currentTab by viewModel.defaultOpenTab.collectAsState()
    val currentFilter by viewModel.defaultHistoryFilter.collectAsState()
    var showTabDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.themes_and_appearance)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.theme),
                items = buildList {
                    add(
                        Material3SettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.ColorLens),
                            title = { Text(stringResource(R.string.theme)) },
                            description = { Text(stringResource(R.string.theme_description)) },
                            onClick = onEditColorsClick
                        )
                    )
                }
            )
            Material3SettingsGroup(
                title = stringResource(R.string.appearance),
                items = listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.swipe_to_delete)) },
                        icon = painterResource(R.drawable.ic_swipe),
                        trailingContent = {
                            IconSwitch(
                                checked = isSwipeEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.toggleSwipeToDelete(isEnabled)
                                }
                            )
                        },
                        onClick = { viewModel.toggleSwipeToDelete(!isSwipeEnabled) }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.slim_nav_bar)) },
                        icon = painterResource(R.drawable.ic_navbar),
                        trailingContent = {
                            IconSwitch(
                                checked = isSlimNav,
                                onCheckedChange = { isEnabled ->
                                    viewModel.toggleSlimNav(isEnabled)
                                }
                            )
                        },
                        onClick = { viewModel.toggleSlimNav(!isSlimNav) }
                    ),

                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.change_default_open_tab)) },
                        description = {
                            Text(
                                when(currentTab) {
                                    AppDestinations.SEARCH -> stringResource(R.string.search_tab_label)
                                    AppDestinations.HISTORY -> stringResource(R.string.history_tab_label)
                                    AppDestinations.PROFILE -> stringResource(R.string.profile_tab_label)
                                }
                            ) },
                        icon = painterResource(R.drawable.ic_nav_corner),
                        onClick = { showTabDialog = true }
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.change_default_history_chip)) },
                        description = {
                            Text(
                                when(currentFilter) {
                                    TrackingFilter.IN_TRANSIT -> stringResource(R.string.in_transit_label)
                                    TrackingFilter.DELIVERED -> stringResource(R.string.delivered_label)
                                    TrackingFilter.ALL -> stringResource(R.string.all_label)
                                }
                            )
                        },
                        icon = painterResource(R.drawable.ic_chip),
                        onClick = { showFilterDialog = true }
                    )
                )
            )
            if (showFilterDialog) {
                EnumDialog(
                    onDismiss = { showFilterDialog = false },
                    onSelect = {
                        viewModel.setDefaultHistoryFilter(it)
                        showFilterDialog = false
                    },
                    title = " ",
                    current = currentFilter,
                    values = TrackingFilter.entries,
                    valueText = {
                        when (it) {
                            TrackingFilter.IN_TRANSIT -> stringResource(R.string.in_transit_label)
                            TrackingFilter.DELIVERED -> stringResource(R.string.delivered_label)
                            TrackingFilter.ALL -> stringResource(R.string.all_label)
                        }
                    }
                )
            }
            if (showTabDialog) {
                EnumDialog(
                    onDismiss = { showTabDialog = false },
                    onSelect = {
                        viewModel.setDefaultTab(it)
                        showTabDialog = false
                    },
                    title = " ",
                    current = currentTab,
                    values = AppDestinations.entries,
                    valueText = {
                        when (it) {
                            AppDestinations.SEARCH -> stringResource(R.string.search_tab_label)
                            AppDestinations.HISTORY -> stringResource(R.string.history_tab_label)
                            AppDestinations.PROFILE -> stringResource(R.string.profile_tab_label)
                        }
                    }
                )
            }
        }
    }
}