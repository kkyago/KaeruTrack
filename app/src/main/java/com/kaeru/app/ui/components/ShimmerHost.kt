package com.kaeru.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import com.valentinilk.shimmer.defaultShimmerTheme
import com.valentinilk.shimmer.shimmer
import com.valentinilk.shimmer.LocalShimmerTheme


val ShimmerTheme =
    defaultShimmerTheme.copy(
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 800,
                        easing = LinearEasing,
                        delayMillis = 250,
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        shaderColors =
            listOf(
                Color.Unspecified.copy(alpha = 0.25f),
                Color.Unspecified.copy(alpha = 0.50f),
                Color.Unspecified.copy(alpha = 0.25f),
            ),
    )

@Composable
fun ShimmerHost(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    showGradient: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalShimmerTheme provides ShimmerTheme) {
        val baseModifier = modifier
            .shimmer()
            .graphicsLayer(alpha = 0.99f)

        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            modifier = if (showGradient) {
                baseModifier.drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent)),
                        blendMode = BlendMode.DstIn,
                    )
                }
            } else {
                baseModifier
            },
            content = content,
        )
    }
}

@Composable
fun ShimmerBasePlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp)
) {
    Spacer(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface)
    )
}

@Composable
fun TrackingHeaderPlaceholder(modifier: Modifier = Modifier) {
    ShimmerBasePlaceholder(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun TitlePlaceholder(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        ShimmerBasePlaceholder(modifier = Modifier.size(18.dp), shape = CircleShape)
        Spacer(modifier = Modifier.width(8.dp))
        ShimmerBasePlaceholder(modifier = Modifier.width(80.dp).height(16.dp))
    }
}

@Composable
fun LatestEventPlaceholder(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        ShimmerBasePlaceholder(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(14.dp))
        Spacer(modifier = Modifier.width(16.dp))
        ShimmerBasePlaceholder(
            modifier = Modifier
                .weight(1f)
                .height(120.dp),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun HistoryEventPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(48.dp)) {
            ShimmerBasePlaceholder(modifier = Modifier.size(20.dp), shape = CircleShape)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            ShimmerBasePlaceholder(modifier = Modifier.fillMaxWidth(0.6f).height(15.dp))
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBasePlaceholder(modifier = Modifier.fillMaxWidth(0.8f).height(13.dp))
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBasePlaceholder(
                modifier = Modifier.width(100.dp).height(22.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun TrackingShimmerSkeleton(modifier: Modifier = Modifier) {
    ShimmerHost(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        showGradient = false
    ) {
        TrackingHeaderPlaceholder()
        TitlePlaceholder()
        LatestEventPlaceholder()
        repeat(5) {
            HistoryEventPlaceholder()
        }
    }
}