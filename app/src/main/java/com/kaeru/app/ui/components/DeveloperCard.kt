package com.kaeru.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kaeru.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeveloperCard(uriHandler: UriHandler) {
    val ClimateCrisis = FontFamily(
        Font(R.font.climatecrisis)
    )
    ElevatedCard(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ExpressiveAvatar(
                    avatarUrl = "https://github.com/kkyago.png",
                    sizeDp = 100
                )

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "Kkyago",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = ClimateCrisis,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 34.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Desenvolvedor Principal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val siteInteraction = remember { MutableInteractionSource() }
            val gitInteraction = remember { MutableInteractionSource() }
            val instaInteraction = remember { MutableInteractionSource() }

            val isSitePressed by siteInteraction.collectIsPressedAsStateWithPulse()
            val isGitPressed by gitInteraction.collectIsPressedAsStateWithPulse()
            val isInstaPressed by instaInteraction.collectIsPressedAsStateWithPulse()

            val siteWeight = animateExpressiveWeight(isSitePressed, isGitPressed || isInstaPressed, 1.5f, 0.75f)
            val gitWeight = animateExpressiveWeight(isGitPressed, isSitePressed || isInstaPressed, 1.5f, 0.75f)
            val instaWeight = animateExpressiveWeight(isInstaPressed, isSitePressed || isGitPressed, 1.5f, 0.75f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpressiveIconButton(
                    onClick = { uriHandler.openUri("https://kaerutrack.netlify.app") },
                    iconRes = R.drawable.ic_language,
                    interactionSource = siteInteraction,
                    weight = siteWeight
                )
                ExpressiveIconButton(
                    onClick = { uriHandler.openUri("https://github.com/kkyago") },
                    iconRes = R.drawable.github,
                    interactionSource = gitInteraction,
                    weight = gitWeight
                )
                ExpressiveIconButton(
                    onClick = { uriHandler.openUri("https://instagram.com/kkyago") },
                    iconRes = R.drawable.ic_instagram,
                    interactionSource = instaInteraction,
                    weight = instaWeight
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { uriHandler.openUri("https://ko-fi.com/kkyago") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Outlined.Coffee, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.ko_fi), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun MutableInteractionSource.collectIsPressedAsStateWithPulse(): State<Boolean> {
    val isPressed = remember { mutableStateOf(false) }

    LaunchedEffect(this) {
        var pressStartTime = 0L
        this@collectIsPressedAsStateWithPulse.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    pressStartTime = System.currentTimeMillis()
                    isPressed.value = true
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    val duration = System.currentTimeMillis() - pressStartTime
                    if (duration < 150L) {
                        delay(150L - duration)
                    }
                    isPressed.value = false
                }
            }
        }
    }
    return isPressed
}

@Composable
private fun animateExpressiveWeight(
    isPressed: Boolean,
    isSiblingPressed: Boolean,
    expandedWeight: Float,
    shrunkWeight: Float
): Float {
    val weight by animateFloatAsState(
        targetValue = if (isPressed) expandedWeight else if (isSiblingPressed) shrunkWeight else 1f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 50, easing = LinearOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.6f, stiffness = 500f)
        },
        label = "expressive_weight"
    )
    return weight
}

@Composable
private fun RowScope.ExpressiveIconButton(
    onClick: () -> Unit,
    iconRes: Int,
    interactionSource: MutableInteractionSource,
    weight: Float
) {
    FilledTonalButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .weight(weight)
            .height(48.dp)
    ) {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveAvatar(avatarUrl: String, sizeDp: Int) {
    val expressiveShapes = remember {
        listOf(MaterialShapes.Cookie6Sided, MaterialShapes.Cookie7Sided, MaterialShapes.Cookie9Sided, MaterialShapes.Cookie12Sided, MaterialShapes.Cookie4Sided)
    }
    var shapeIndex by remember { mutableIntStateOf(0) }
    val currentShape = expressiveShapes[shapeIndex % expressiveShapes.size].toShape()

    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        onClick = {
            shapeIndex++
            coroutineScope.launch {
                scale.animateTo(0.75f, tween(100))
                scale.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium))
            }
        },
        modifier = Modifier
            .size(sizeDp.dp)
            .scale(scale.value),
        shape = currentShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 4.dp,
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1f / scale.value.coerceAtLeast(0.01f)),
            placeholder = null
        )
    }
}