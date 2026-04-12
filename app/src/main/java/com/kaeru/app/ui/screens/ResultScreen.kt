package com.kaeru.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaeru.app.R
import com.kaeru.app.tracking.TrackingEvent
import com.kaeru.app.tracking.TrackingViewModel
import com.kaeru.app.tracking.utils.DateUtils
import com.kaeru.app.tracking.utils.TrackingCarrier
import com.kaeru.app.tracking.utils.isDeliveredStatus
import com.kaeru.app.ui.components.CpfInputDialog
import com.kaeru.app.ui.components.TrackingShimmerSkeleton

val SoftRed = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    trackingCode: String,
    carrier: String,
    viewModel: TrackingViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val historyList by viewModel.historyList.collectAsState()
    val savedItem = remember(historyList, trackingCode) {
        historyList.find { it.code == trackingCode }
    }
    val isSaved = savedItem != null
    val toolbarTitle = savedItem?.description?.ifBlank { stringResource(R.string.tracking) } ?: stringResource(R.string.tracking)
    var showEditDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(savedItem?.description ?: "") }

    LaunchedEffect(trackingCode) {
        if (viewModel.trackingResult?.tracking_code != trackingCode) {
            viewModel.trackPackage(trackingCode, carrier = carrier)
        }
    }

    if (viewModel.isLoading || viewModel.showCpfDialog) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.tracking)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                TrackingShimmerSkeleton()

                if (viewModel.showCpfDialog) {
                    val isError = viewModel.errorMessage?.contains("CPF", ignoreCase = true) == true
                    CpfInputDialog(
                        title = if (isError) stringResource(R.string.cpf_incorrect_title) else stringResource(
                            R.string.cpf_necessary_title
                        ),
                        message = if (isError) {
                            stringResource(R.string.cpf_incorrect_desc, viewModel.pendingJtCode)
                        } else {
                            stringResource(R.string.cpf_necessary_desc, viewModel.pendingJtCode)
                        },
                        onDismiss = {
                            viewModel.showCpfDialog = false
                            viewModel.errorMessage = null
                            onBack()
                        },
                        onSubmit = { cpfDigitado ->
                            viewModel.submitCpfAndTrack(cpfDigitado)
                        }
                    )
                }
            }
        }
    } else if (viewModel.trackingResult != null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = toolbarTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        val removedMessage = stringResource(R.string.removed)
                        if (isSaved) {
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(Icons.Outlined.Edit, contentDescription = null)
                            }
                            IconButton(onClick = {
                                viewModel.deleteTracking(trackingCode)
                                Toast.makeText(context,removedMessage, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = SoftRed
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.openSaveDialog() }) {
                                Icon(
                                    Icons.Default.Save,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            }
                        }
                        if (showEditDialog) {
                            AlertDialog(
                                onDismissRequest = { showEditDialog = false },
                                title = { Text(stringResource(R.string.edit_name)) },
                                text = {
                                    OutlinedTextField(
                                        value = tempName,
                                        onValueChange = { tempName = it },
                                        label = { Text(stringResource(R.string.new_name)) },
                                        singleLine = true
                                    )
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        viewModel.updatePackageDescription(trackingCode, tempName)
                                        showEditDialog = false
                                    }) { Text(stringResource(R.string.save)) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEditDialog = false }) { Text(
                                        stringResource(R.string.cancel)
                                    ) }
                                }
                            )
                        }
                    },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    viewModel.isLoading -> {
                        TrackingShimmerSkeleton()
                    }

                    viewModel.errorMessage != null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(viewModel.errorMessage ?: stringResource(R.string.error), color = primaryColor)
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) { Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }

                    viewModel.trackingResult != null -> {
                        val result = viewModel.trackingResult!!
                        val events = result.events ?: emptyList()

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            item {
                                val copiedMessage = stringResource(R.string.copied)
                                TrackingHeaderCardCompact(
                                    code = result.tracking_code ?: trackingCode,
                                    lastStatus = events.firstOrNull()?.status ?: stringResource(R.string.awaiting),
                                    carrier = stringResource(id = detectCarrier(result.tracking_code ?: trackingCode)),
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(it))
                                        Toast.makeText(context,copiedMessage, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            }

                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.LocalShipping,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.history),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            if (events.isEmpty()) {
                                item {
                                    Text(
                                        stringResource(R.string.no_events),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                itemsIndexed(events) { index, event ->
                                    if (index == 0) {
                                        LatestEventCard(
                                            event = event,
                                            hasNext = events.size > 1
                                        )
                                    } else {
                                        HistoryEventItem(
                                            event = event,
                                            isLastItem = index == events.lastIndex
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (viewModel.showSaveDialog) {
                    AlertDialog(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onDismissRequest = { viewModel.showSaveDialog = false },
                        title = { Text(stringResource(R.string.save_package)) },
                        text = {
                            Column {
                                Text(stringResource(R.string.name_for_identification))
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = viewModel.packageDescription,
                                    onValueChange = { viewModel.packageDescription = it },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            val savedMessage = stringResource(R.string.saved)
                            Button(
                                onClick = {
                                    viewModel.saveTracking(context)
                                    Toast.makeText(context,savedMessage, Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) { Text(
                                stringResource(R.string.save), color = MaterialTheme.colorScheme.onPrimary) }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.showSaveDialog = false }) {
                                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                }
            }
        }
    }
    else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.systemBars,
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ErrorScreen(
                    code = trackingCode,
                    onBack = onBack
                )
            }
        }
    }
}

fun detectCarrier(code: String): Int {
    return TrackingCarrier.fromCode(code).nameRes
}

@Composable
fun TrackingHeaderCardCompact(
    code: String,
    lastStatus: String,
    carrier: String,
    onCopy: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDelivered = lastStatus.isDeliveredStatus()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.code),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
                ) {
                val displayCode = if (code.length > 10) code.take(10) + "..." else code
                Text(
                    text = displayCode,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { onCopy(code) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy),
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Surface(
                    color = primaryColor.copy(alpha = 0.15f),
                    shape = CircleShape
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_package_outlined),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = carrier,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun LatestEventCard(
    event: TrackingEvent,
    hasNext: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val gapPx = with(density) { 24.dp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.35f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Restart), label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Restart), label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .drawBehind {
                    if (hasNext) {
                        drawLine(
                            color = lineColor,
                            start = Offset(size.width / 2f, size.height / 2f),
                            end = Offset(size.width / 2f, size.height + gapPx),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(41.dp)
                        .scale(pulseScale)
                        .background(primaryColor.copy(alpha = pulseAlpha), RoundedCornerShape(14.dp))
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(primaryColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val isDelivered = event.status?.isDeliveredStatus() ?: false
                    val icon = if (isDelivered) Icons.Default.Check else Icons.Default.LocalShipping
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.5.dp, primaryColor),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = event.status ?: stringResource(R.string.update),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!event.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = event.location, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                val prettyDate = DateUtils.formatDatePretty(event.date, event.time)
                DateBadge(prettyDate, primaryColor)
            }
        }
    }
}

@Composable
fun HistoryEventItem(
    event: TrackingEvent,
    isLastItem: Boolean
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = textColor.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val gapPx = with(density) { 24.dp.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .drawBehind {
                    drawLine(
                        color = lineColor,
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height / 2f),
                        strokeWidth = 2.dp.toPx()
                    )

                    if (!isLastItem) {
                        drawLine(
                            color = lineColor,
                            start = Offset(size.width / 2f, size.height / 2f),
                            end = Offset(size.width / 2f, size.height + gapPx),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(14.dp))
                    .border(BorderStroke(1.5.dp, lineColor), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = event.status ?: stringResource(R.string.status),
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (!event.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.location,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val prettyDate = DateUtils.formatDatePretty(event.date, event.time)
            DateBadge(prettyDate, textColor, isHighlight = false)
        }
    }
}

@Composable
fun DateBadge(text: String, color: Color, isHighlight: Boolean = true) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Schedule, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}