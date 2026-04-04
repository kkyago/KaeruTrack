package com.kaeru.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaeru.app.tracking.TrackingViewModel
import com.kaeru.app.ui.components.KaeruActivityChart
import androidx.compose.ui.res.stringResource
import com.kaeru.app.R
import com.kaeru.app.tracking.utils.TrackingCarrier
import com.kaeru.app.ui.components.Material3SettingsGroup
import com.kaeru.app.ui.components.Material3SettingsItem

enum class KaeruStatPeriod(val label: Int, val xAxisTitle: Int) {
    WEEK(R.string.week_label, R.string.days_label),
    MONTH(R.string.month_label, R.string.weeks_label),
    YEAR(R.string.year_label, R.string.months_label)
}

data class CarrierStat(val name: String, val avgDays: Int, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: TrackingViewModel) {
    val selectedPeriod by viewModel.selectedStatPeriod.collectAsState()
    val carrierStats by viewModel.carrierEfficiency.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val allpackages by viewModel.historyList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = allpackages.size.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.registered_packages),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stats.first.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.in_transit_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stats.second.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.delivered_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.tracking_activity),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(KaeruStatPeriod.entries) { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { viewModel.setStatPeriod(period) },
                    label = {
                        Text(
                            stringResource(id = period.label),
                            fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    border = null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Crossfade(targetState = chartData, label = "chart_anim") { data ->
                if (data.first.isNotEmpty()) {
                    KaeruActivityChart(
                        xData = data.first,
                        yData = data.second,
                        xAxisTitle = stringResource(id = selectedPeriod.xAxisTitle),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.average_delivery_time),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        )

        if (carrierStats.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data_enough),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Material3SettingsGroup(
                    items = buildList {
                        carrierStats.forEach { stat ->
                            val (carrierName, carrierIcon) = getCarrierDetails(stat.carrier)

                            add(
                                Material3SettingsItem(
                                    icon = rememberVectorPainter(carrierIcon),
                                    title = { Text(carrierName, fontWeight = FontWeight.Bold) },
                                    trailingContent = {
                                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(
                                                text = "${stat.avgDays} dias",
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    },
                                    onClick = { }
                                )
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

fun getCarrierDetails(carrier: TrackingCarrier): Pair<String, ImageVector> {
    return when (carrier) {
        TrackingCarrier.CORREIOS -> Pair("Correios", Icons.Outlined.LocalShipping)
        TrackingCarrier.LOGGI -> Pair("Loggi", Icons.Outlined.TwoWheeler)
        TrackingCarrier.JT_EXPRESS -> Pair("J&T Express", Icons.Outlined.DirectionsCar)
        TrackingCarrier.CAINIAO -> Pair("Cainiao", Icons.Outlined.FlightLand)
        TrackingCarrier.SHOPEE -> Pair("Shopee Xpress", Icons.Outlined.ShoppingBag)
        TrackingCarrier.TOTAL_EXPRESS -> Pair("Total Express", Icons.Outlined.Moped)
        TrackingCarrier.ANJUN -> Pair("Anjun Express", Icons.Outlined.FlightTakeoff)
        TrackingCarrier.MELHOR_ENVIO -> Pair("Melhor Envio", Icons.Outlined.Inventory)
        else -> Pair("Desconhecida", Icons.Outlined.LocalShipping)
    }
}