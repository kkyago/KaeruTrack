import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kaeru.app.R

@Composable
fun DynamicFloatingLogo(
    modifier: Modifier = Modifier,
    baseSize: Dp = 140.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "floating_logo")

    val float1 by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_large"
    )

    val float2 by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_medium"
    )

    val float3 by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_small"
    )

    val largeSize = baseSize * 0.55f
    val mediumSize = baseSize * 0.15f
    val smallSize = baseSize * 0.10f

    Column(
        modifier = modifier.width(baseSize),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.about_icon_1),
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier
                .size(largeSize)
                .offset(y = float1.dp)
        )
        Spacer(Modifier.height(8.dp))
        Icon(
            painter = painterResource(R.drawable.about_icon_2),
            contentDescription = null,
            tint = primaryColor.copy(alpha = 0.7f),
            modifier = Modifier
                .size(mediumSize)
                .offset(
                    x = (baseSize * 0.15f),
                    y = float2.dp - (baseSize * 0.0005f)
                )
        )
        Spacer(Modifier.height(5.dp))
        Icon(
            painter = painterResource(R.drawable.about_icon_2),
            contentDescription = null,
            tint = primaryColor.copy(alpha = 0.4f),
            modifier = Modifier
                .size(smallSize)
                .offset(
                    x = -(baseSize * 0.1f),
                    y = float3.dp - (baseSize * 0.05f)
                )
        )
    }
}