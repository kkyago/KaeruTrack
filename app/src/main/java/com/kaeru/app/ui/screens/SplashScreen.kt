package com.kaeru.app.ui.screens

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlinx.coroutines.delay
import kotlin.random.Random

class CachedPolygonShape(private val polygon: RoundedPolygon) : Shape {
    private var cachedSize: Size = Size.Unspecified
    private var cachedOutline: Outline? = null

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size == cachedSize && cachedOutline != null) {
            return cachedOutline!!
        }

        val path = polygon.toPath()
        val bounds = RectF()
        path.computeBounds(bounds, true)

        val scale = minOf(size.width / bounds.width(), size.height / bounds.height())
        val matrix = Matrix().apply {
            postTranslate(-bounds.centerX(), -bounds.centerY())
            postScale(scale, scale)
            postTranslate(size.width / 2f, size.height / 2f)
        }
        path.transform(matrix)

        val outline = Outline.Generic(path.asComposePath())
        cachedSize = size
        cachedOutline = outline
        return outline
    }
}

data class FloatingShapeConfig(
    val shape: Shape,
    val size: Dp,
    val offsetX: Dp,
    val offsetY: Dp,
    val color: Color,
    val rotationDuration: Int,
    val translationDuration: Int,
    val translationRange: Float,
    val clockwise: Boolean
)

@Composable
fun FloatingShapeComponent(config: FloatingShapeConfig) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (config.clockwise) 360f else -360f,
        animationSpec = infiniteRepeatable(tween(config.rotationDuration, easing = LinearEasing)),
        label = "rot"
    )

    val translationY by infiniteTransition.animateFloat(
        initialValue = -config.translationRange,
        targetValue = config.translationRange,
        animationSpec = infiniteRepeatable(tween(config.translationDuration, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "trans"
    )

    Box(
        modifier = Modifier
            .offset(x = config.offsetX, y = config.offsetY)
            .graphicsLayer {
                this.translationY = translationY
                this.rotationZ = rotation
            }
            .size(config.size)
            .clip(config.shape)
            .background(config.color)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500)
        onSplashFinished()
    }

    val textScale = remember { Animatable(0.5f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        textAlpha.animateTo(1f, tween(500, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        textScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val shapesList = listOf(
        MaterialShapes.Cookie4Sided, MaterialShapes.Cookie9Sided,
        MaterialShapes.Clover4Leaf, MaterialShapes.Clover8Leaf,
        MaterialShapes.Slanted, MaterialShapes.Gem, MaterialShapes.Sunny
    )
    val colorsList = listOf(primary, secondary, tertiary)

    val randomConfigs = remember {
        val configs = mutableListOf<FloatingShapeConfig>()
        val cols = 4
        val rows = 6

        val cellWidth = 100
        val cellHeight = 140

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val baseX = (c * cellWidth) - (cols * cellWidth / 2) + (cellWidth / 2)
                val baseY = (r * cellHeight) - (rows * cellHeight / 2) + (cellHeight / 2)

                configs.add(
                    FloatingShapeConfig(
                        shape = CachedPolygonShape(shapesList.random()),
                        size = (30..90).random().dp,
                        offsetX = (baseX + (-35..35).random()).dp,
                        offsetY = (baseY + (-45..45).random()).dp,
                        color = colorsList.random().copy(alpha = (8..22).random() / 100f),
                        rotationDuration = (15000..35000).random(),
                        translationDuration = (3000..7000).random(),
                        translationRange = (20..50).random().toFloat(),
                        clockwise = Random.nextBoolean()
                    )
                )
            }
        }
        configs.shuffled()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        randomConfigs.forEach { config ->
            FloatingShapeComponent(config)
        }

        Text(
            text = "Kaeru",
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Black,
            fontSize = 88.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .scale(textScale.value)
                .alpha(textAlpha.value)
        )
    }
}