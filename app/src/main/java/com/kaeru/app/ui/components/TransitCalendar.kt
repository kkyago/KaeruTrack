package com.kaeru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kaeru.app.tracking.utils.DateUtils
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.yearMonth
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TransitCalendarDialog(
    firstDateStr: String?,
    lastDateStr: String?,
    savedAt: Long,
    isDelivered: Boolean,
    onDismiss: () -> Unit
) {
    val rawEndDate = remember(lastDateStr, isDelivered) {
        if (isDelivered) {
            DateUtils.parseToLocalDate(lastDateStr) ?: LocalDate.now()
        } else {
            LocalDate.now()
        }
    }
    val rawStartDate = remember(firstDateStr, savedAt, isDelivered, rawEndDate) {
        if (isDelivered) {
            rawEndDate
        } else {
            DateUtils.parseToLocalDate(firstDateStr)
                ?: Instant.ofEpochMilli(savedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }
    val startDate = if (rawStartDate.isAfter(rawEndDate)) rawEndDate else rawStartDate
    val endDate = if (rawStartDate.isAfter(rawEndDate)) rawStartDate else rawEndDate
    val currentMonth = remember { startDate.yearMonth }
    val startMonth = remember { startDate.yearMonth.minusMonths(12) }
    val endMonth = remember { endDate.yearMonth.plusMonths(12) }
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth
    )
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primaryContainer

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = "Tempo em Trânsito",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val visibleMonth = state.firstVisibleMonth.yearMonth
                    val monthName = visibleMonth.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                        .replaceFirstChar { it.uppercase() }

                    IconButton(onClick = {
                        coroutineScope.launch { state.animateScrollToMonth(visibleMonth.minusMonths(1)) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                    }

                    Text(
                        text = "$monthName ${visibleMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = {
                        coroutineScope.launch { state.animateScrollToMonth(visibleMonth.plusMonths(1)) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalCalendar(
                    state = state,
                    userScrollEnabled = true,
                    dayContent = { day ->
                        DayCell(
                            day = day,
                            startDate = startDate,
                            endDate = endDate,
                            primaryColor = primaryColor,
                            trackColor = trackColor
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("FECHAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    startDate: LocalDate,
    endDate: LocalDate,
    primaryColor: Color,
    trackColor: Color
) {
    if (day.position != DayPosition.MonthDate) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    val isStartDate = day.date == startDate
    val isEndDate = day.date == endDate
    val isBetween = day.date.isAfter(startDate) && day.date.isBefore(endDate)
    val isSingleDay = isStartDate && isEndDate

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isBetween || (!isSingleDay && (isStartDate || isEndDate))) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(
                        fraction = if (isBetween) 1f else 0.5f
                    )
                    .align(
                        if (isStartDate) Alignment.CenterEnd else if (isEndDate) Alignment.CenterStart else Alignment.Center
                    )
                    .background(trackColor)
            )
        }

        if (isStartDate || isEndDate) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        }

        Text(
            text = day.date.dayOfMonth.toString(),
            color = if (isStartDate || isEndDate) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isStartDate || isEndDate || isBetween) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}