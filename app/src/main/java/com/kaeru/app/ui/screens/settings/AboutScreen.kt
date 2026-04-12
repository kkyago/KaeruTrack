package com.kaeru.app.ui.screens.settings

import DynamicFloatingLogo
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaeru.app.BuildConfig
import com.kaeru.app.R
import com.kaeru.app.ui.components.Material3SettingsGroup
import com.kaeru.app.ui.components.Material3SettingsItem
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.kaeru.app.ui.components.DeveloperCard

private data class CommunityLink(
    val label: String,
    val description: String? = null,
    val iconRes: Int,
    val url: String
)

private val communityLinks = listOf(
    CommunityLink(
        label = "Código Fonte",
        description = "Veja o repositório no GitHub",
        iconRes = R.drawable.github,
        url = "https://github.com/kkyago/KaeruTrack"
    ),
    CommunityLink(
        label = "Licença",
        description = "GPL-3.0 License",
        iconRes = R.drawable.github,
        url = "https://github.com/kkyago/KaeruTrack/blob/main/LICENSE"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val ClimateCrisis = FontFamily(
        Font(R.font.climatecrisis)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            ExpandableLogoCard()

            Spacer(Modifier.height(24.dp))

            DeveloperCard(uriHandler = uriHandler)

            Spacer(Modifier.height(32.dp))

            Material3SettingsGroup(
                title = "Projeto & Comunidade",
                items = communityLinks.map { link ->
                    Material3SettingsItem(
                        icon = painterResource(link.iconRes),
                        title = { Text(link.label, fontWeight = FontWeight.SemiBold) },
                        description = link.description?.let { { Text(it) } },
                        onClick = { uriHandler.openUri(link.url) }
                    )
                }
            )

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun ExpandableLogoCard() {
    var isExpanded by remember { mutableStateOf(false) }
    val ClimateCrisis = FontFamily(
        Font(R.font.climatecrisis)
    )

    val animatedSize by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else 80.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_size"
    )

    val logoContent = remember {
        movableContentOf {
            DynamicFloatingLogo(
                baseSize = animatedSize,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    val textContent = remember(isExpanded) {
        movableContentOf {
            Column(
                horizontalAlignment = if (isExpanded) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = "Kaeru",
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = ClimateCrisis,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (BuildConfig.DEBUG) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "DEBUG",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isExpanded = !isExpanded
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                logoContent()
                Spacer(Modifier.height(24.dp))
                textContent()
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                logoContent()
                Spacer(Modifier.width(20.dp))
                textContent()
            }
        }
    }
}