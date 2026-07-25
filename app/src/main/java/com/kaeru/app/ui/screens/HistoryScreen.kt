package com.kaeru.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaeru.app.tracking.database.TrackingEntity
import com.kaeru.app.tracking.TrackingViewModel
import java.util.concurrent.TimeUnit
import com.kaeru.app.R
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import com.kaeru.app.tracking.utils.DateUtils
import com.kaeru.app.ui.components.TransitCalendarDialog
import kotlin.math.abs
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.kaeru.app.ui.components.AnimatedFilterChip
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.ColorFilter
import com.kaeru.app.tracking.utils.isDeliveredStatus
import com.kaeru.app.tracking.utils.rememberBooleanPreference
import com.kaeru.app.tracking.utils.rememberEnumPreference
import com.kaeru.app.ui.components.SortHeader
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.kaeru.app.ui.components.LibrarySearchHeader
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.KeyboardArrowUp

enum class OrderType { DATE_ADDED, ALPHABETIC, LAST_UPDATED }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TrackingViewModel,
    currentFilter: TrackingFilter,
    onFilterChange: (TrackingFilter) -> Unit,
    onNavigateToResult: (String) -> Unit
) {
    val history by viewModel.historyList.collectAsState()
    var sortType by rememberEnumPreference("PREF_SORT_TYPE", OrderType.LAST_UPDATED)
    var sortDescending by rememberBooleanPreference("PREF_SORT_DESC", true)
    val backgroundColor = MaterialTheme.colorScheme.background
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground
    val filteredHistory = remember(history, currentFilter, sortType, sortDescending, searchQuery) {
        val statusFiltered = when (currentFilter) {
            TrackingFilter.IN_TRANSIT -> history.filter { !it.lastStatus.isDeliveredStatus() }
            TrackingFilter.DELIVERED -> history.filter { it.lastStatus.isDeliveredStatus() }
            TrackingFilter.ALL -> history
        }

        val searchFiltered = if (searchQuery.isNotBlank()) {
            statusFiltered.filter {
                it.description.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
            }
        } else {
            statusFiltered
        }

        when (sortType) {
            OrderType.ALPHABETIC -> {
                if (sortDescending) {
                    searchFiltered.sortedByDescending { it.description.ifBlank { it.code }.lowercase() }
                } else {
                    searchFiltered.sortedBy { it.description.ifBlank { it.code }.lowercase() }
                }
            }
            OrderType.DATE_ADDED -> {
                if (sortDescending) {
                    searchFiltered.sortedByDescending { it.savedAt }
                } else {
                    searchFiltered.sortedBy { it.savedAt }
                }
            }
            OrderType.LAST_UPDATED -> {
                if (sortDescending) {
                    searchFiltered
                } else {
                    searchFiltered.reversed()
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isSwipeEnabled by viewModel.isSwipeToDeleteEnabled.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val isSelectionMode = selectedPackages.isNotEmpty()

    val listState = rememberLazyListState()
    val showScrollToTopFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 1 }
    }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    val deletedOrder = stringResource(R.string.deleted_order)
    val undoAction = stringResource(R.string.undo_action)

    LaunchedEffect(Unit) {
        viewModel.undoDeleteEvent.collect { deletedItems ->
            if (deletedItems.isNotEmpty()) {
                val result = snackbarHostState.showSnackbar(
                    message = deletedOrder,
                    actionLabel = undoAction,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restorePackages(deletedItems)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent,
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTopFab && !isSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedFilterChip(
                            selected = currentFilter == TrackingFilter.IN_TRANSIT,
                            onClick = { onFilterChange(TrackingFilter.IN_TRANSIT) },
                            label = stringResource(R.string.in_transit_label),
                            icon = Icons.Outlined.LocalShipping
                        )

                        AnimatedFilterChip(
                            selected = currentFilter == TrackingFilter.DELIVERED,
                            onClick = { onFilterChange(TrackingFilter.DELIVERED) },
                            label = stringResource(R.string.delivered_label),
                            icon = Icons.Default.CheckCircle
                        )

                        AnimatedFilterChip(
                            selected = currentFilter == TrackingFilter.ALL,
                            onClick = { onFilterChange(TrackingFilter.ALL) },
                            label = stringResource(R.string.all_label),
                            icon = Icons.Default.CheckCircle
                        )
                    }
                    LibrarySearchHeader(
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onBack = {
                            isSearchActive = false
                            searchQuery = ""
                        },
                        keyboardController = keyboardController,
                        modifier = Modifier.padding(bottom = 0.dp)
                    ) {
                        SortHeader(
                            sortType = sortType,
                            sortDescending = sortDescending,
                            onSortTypeChange = { sortType = it },
                            onSortDescendingChange = { sortDescending = it },
                            sortTypeText = { type ->
                                when (type) {
                                    OrderType.DATE_ADDED -> R.string.sort_by_date_added
                                    OrderType.ALPHABETIC -> R.string.sort_by_alphabetic
                                    OrderType.LAST_UPDATED -> R.string.sort_by_last_updated
                                }
                            }
                        )

                        Spacer(Modifier.weight(1f))

                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Pesquisar Encomenda",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            if (filteredHistory.isEmpty()) {
                item {
                    EmptyHistoryState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(1f)
                    )
                }
            } else {
                items(filteredHistory, key = { it.code }) { item ->

                    var hasFiredDelete by remember { mutableStateOf(false) }
                    val deletedOrder = stringResource(R.string.deleted_order)
                    val undoAction = stringResource(R.string.undo_action)

                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { totalDistance -> totalDistance * 0.5f },
                        confirmValueChange = { dismissValue ->

                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {

                                if (!hasFiredDelete) {
                                    hasFiredDelete = true

                                    viewModel.deleteTracking(item.code)

                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedOrder,
                                            actionLabel = undoAction,
                                            duration = SnackbarDuration.Short
                                        )

                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreTracking(item)
                                        }
                                    }
                                }
                                false
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromEndToStart = isSwipeEnabled,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { SwipeToDeleteIcon(dismissState) },
                        modifier = Modifier.animateItem()
                    ) {
                        val isSelected = selectedPackages.contains(item.code)
                        HistoryCardNew(
                            item = item,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onClick = {
                                if (isSelectionMode) {
                                    viewModel.togglePackageSelection(item.code)
                                } else {
                                    onNavigateToResult(item.code)
                                }
                            },
                            onLongClick = {
                                viewModel.togglePackageSelection(item.code)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryCardNew(
    item: TrackingEntity,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardColor = MaterialTheme.colorScheme.surfaceContainerLow
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val isDelivered = item.lastStatus.isDeliveredStatus()
    val daysCount = remember(item.savedAt) {
        val diff = System.currentTimeMillis() - item.savedAt
        TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0)
    }
    val calculatedDays = remember(item.code, item.lastDate, item.firstDate, item.savedAt, isDelivered) {
        DateUtils.calculateDays(item.lastDate, item.firstDate, item.savedAt, isDelivered)
    }
    val expressiveShapes = remember {
        listOf(
            MaterialShapes.Circle,
            MaterialShapes.Square,
            MaterialShapes.Slanted,
            MaterialShapes.Pill,
            MaterialShapes.Arrow,
            MaterialShapes.Pentagon,
            MaterialShapes.Gem,
            MaterialShapes.Sunny,
            MaterialShapes.Cookie4Sided,
            MaterialShapes.Cookie9Sided,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Clover8Leaf,
        )
    }
    val shapeIndex = remember(item.code) {
        abs(item.code.hashCode()) % expressiveShapes.size
    }
    val dynamicShape = expressiveShapes[shapeIndex].toShape()
    var showCalendar by remember { mutableStateOf(false) }
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "selectionProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        ),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = dynamicShape,
                    color = primaryColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawCircle(
                                    color = primaryColor,
                                    radius = size.width * selectionProgress,
                                    center = center
                                )
                            }
                    ) {
                        AnimatedContent(
                            targetState = isSelected,
                            transitionSpec = {
                                (scaleIn(tween(250)) + fadeIn(tween(250))) togetherWith
                                        (scaleOut(tween(250)) + fadeOut(tween(250)))
                            },
                            label = "iconTransition"
                        ) { selected ->
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selecionado",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_package_outlined),
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.description.ifBlank { stringResource(R.string.unnamed_package) },
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.code,
                        color = subTextColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (isSelectionMode) Modifier else Modifier.clickable { showCalendar = true })
                        .padding(4.dp)
                ) {
                    Surface(
                        color = primaryColor.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDelivered) Icons.Default.CheckCircle else Icons.Outlined.LocalShipping,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDelivered) stringResource(R.string.delivered) else stringResource(R.string.in_transit),
                                color = primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.days_ago, calculatedDays),
                        color = subTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = Center
                    )

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = item.lastStatus,
                color = subTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    if (showCalendar) {
        TransitCalendarDialog(
            firstDateStr = item.firstDate,
            lastDateStr = item.lastDate,
            savedAt = item.savedAt,
            isDelivered = isDelivered,
            onDismiss = { showCalendar = false }
        )
    }
}

enum class TrackingFilter {
    IN_TRANSIT, DELIVERED, ALL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteIcon(state: SwipeToDismissBoxState) {
    val isDragging = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val dragAmount = if (!isDragging) 0f else {
        if (state.targetValue == SwipeToDismissBoxValue.Settled) {
            state.progress * 0.5f
        } else {
            0.5f + (1f - state.progress) * 0.5f
        }
    }
    val iconAlpha = dragAmount.coerceIn(0f, 1f)
    val iconScale = (0.5f + (dragAmount * 0.7f)).coerceIn(0.5f, 1.2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .size(28.dp)
                .scale(iconScale)
                .alpha(iconAlpha)
        )
    }
}

@Composable
fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_screen),
            contentDescription = null,
            modifier = Modifier
                .size(220.dp)
                .padding(bottom = 24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = stringResource(R.string.empty_screen_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 22.sp,
            modifier = Modifier
                .padding(bottom = 15.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.empty_screen_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}