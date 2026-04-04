package com.kaeru.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import io.github.koalaplot.core.bar.DefaultBar
import io.github.koalaplot.core.bar.DefaultBarPosition
import io.github.koalaplot.core.bar.DefaultVerticalBarPlotEntry
import io.github.koalaplot.core.bar.VerticalBarPlot
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.AxisContent
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import io.github.koalaplot.core.xygraph.rememberIntLinearAxisModel

@Composable
private fun AxisLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
        maxLines = 1,
    )
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun KaeruActivityChart(
    xData: List<String>,
    yData: List<Int>,
    xAxisTitle: String,
    modifier: Modifier = Modifier
) {
    val yValuesMax = yData.maxOrNull() ?: 0
    val topYAxis = ((yValuesMax * 1.2f) + 1.5).toInt()
    val tintColor = MaterialTheme.colorScheme.secondary
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .horizontalScroll(scrollState)
            .clip(MaterialTheme.shapes.medium)
    ) {
        if (xData.isEmpty() || yData.isEmpty()) {
            Text("Sem dados no período", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@BoxWithConstraints
        }

        XYGraph(
            xAxisModel = remember(xData) { CategoryAxisModel(xData) },
            yAxisModel = rememberIntLinearAxisModel(
                range = 0..topYAxis,
                minViewExtent = topYAxis,
                maxViewExtent = topYAxis,
                minorTickCount = 0,
            ),
            xAxisContent = AxisContent(
                labels = { AxisLabel(it) },
                title = {
                    Text(
                        text = xAxisTitle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                style = rememberAxisStyle()
            ),
            yAxisContent = AxisContent(
                labels = { AxisLabel(it.toString()) },
                title = {},
                style = rememberAxisStyle(labelRotation = 90)
            ),
            modifier = Modifier
                .width(max(minWidth, (xData.size * 24).dp)),
        ) {
            val data = remember(xData, yData) {
                xData.zip(yData).map { (xd, yd) ->
                    DefaultVerticalBarPlotEntry(xd, DefaultBarPosition(0, yd))
                }
            }

            VerticalBarPlot(
                data = data,
                bar = { _, _, value ->
                    val currentYValue = value.y.end
                    val fontSizeDp = with(density) {
                        MaterialTheme.typography.labelSmall.fontSize.toDp()
                    }

                    DefaultBar(
                        color = tintColor.copy(
                            alpha = 0.5f + 0.5f * (currentYValue.toFloat() / yValuesMax.coerceAtLeast(1).toFloat())
                        ),
                        shape = MaterialTheme.shapes.small.copy(
                            bottomEnd = CornerSize(0.dp),
                            bottomStart = CornerSize(0.dp)
                        ),
                    )
                    Text(
                        text = currentYValue.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = -fontSizeDp * 2)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 2.dp)
                    )
                }
            )
        }
    }
}