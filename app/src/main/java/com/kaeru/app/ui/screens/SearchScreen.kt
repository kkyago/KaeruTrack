package com.kaeru.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaeru.app.R

@Composable
fun SearchScreen(
    onNavigateToResult: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var trackingCode by remember { mutableStateOf("") }
    val finalCode = trackingCode.uppercase().trim()
    val focusManager = LocalFocusManager.current

    val carriers = listOf("Auto", stringResource(R.string.carrier_correios), stringResource(R.string.carrier_loggi), stringResource(R.string.carrier_shopee_xpress), stringResource(R.string.carrier_cainiao), stringResource(R.string.carrier_anjun), stringResource(R.string.carrier_melhor_envio), stringResource(R.string.carrier_total_express), stringResource(R.string.carrier_jt))
    var selectedCarrier by remember { mutableStateOf(carriers[0]) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(R.string.track),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.track_subtitle), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(48.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                        .compositeOver(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                .compositeOver(MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 16.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (trackingCode.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.code_example),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    BasicTextField(
                                        value = trackingCode,
                                        onValueChange = { trackingCode = it.trim() },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = {
                                            if (finalCode.isNotEmpty()) {
                                                focusManager.clearFocus()
                                                onNavigateToResult(finalCode, selectedCarrier)
                                            }
                                        })
                                    )
                                }
                                Box {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { isDropdownExpanded = true }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = selectedCarrier,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = isDropdownExpanded,
                                        onDismissRequest = { isDropdownExpanded = false },
                                        shape = RoundedCornerShape(16.dp),
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        carriers.forEach { carrier ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = carrier,
                                                        fontWeight = if (carrier == selectedCarrier) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    selectedCarrier = carrier
                                                    isDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        FilledIconButton(
                            onClick = {
                                if (finalCode.isNotEmpty()) {
                                    focusManager.clearFocus()
                                    onNavigateToResult(finalCode, selectedCarrier)
                                }
                            },
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    .compositeOver(MaterialTheme.colorScheme.surface),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    modifier = Modifier
                        .width(182.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.supports),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        FlowRow(
                            modifier = Modifier
                                .padding(horizontal = 11.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            maxItemsInEachRow = 4
                        ) {
                            val icons = listOf(
                                R.drawable.ic_correios, R.drawable.ic_shopee,
                                R.drawable.ic_aliexpress, R.drawable.ic_shein,
                                R.drawable.ic_loggi, R.drawable.ic_melhor_envio,
                                R.drawable.ic_tex, R.drawable.ic_jtex
                            )

                            icons.forEach { iconRes ->
                                CarrierMiniBox(iconRes)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarrierMiniBox(iconRes: Int) {
    val boxColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        .compositeOver(MaterialTheme.colorScheme.surface)

    Surface(
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = boxColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}