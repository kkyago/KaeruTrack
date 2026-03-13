package com.kaeru.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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

                OutlinedTextField(
                    value = trackingCode,
                    onValueChange = { trackingCode = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    placeholder = { Text(stringResource(R.string.code_example)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (finalCode.isNotEmpty()) {
                            focusManager.clearFocus()
                            onNavigateToResult(finalCode, selectedCarrier)
                        }
                    }),
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
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
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                DropdownMenu(
                                    expanded = isDropdownExpanded,
                                    onDismissRequest = { isDropdownExpanded = false },
                                    shape = RoundedCornerShape(16.dp)
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
                            Spacer(modifier = Modifier.width(4.dp))
                            FilledIconButton(
                                onClick = {
                                    if (finalCode.isNotEmpty()) {
                                        focusManager.clearFocus()
                                        onNavigateToResult(finalCode, selectedCarrier)
                                    }
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.supports),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row {
                    Icon(
                        painter = painterResource(R.drawable.ic_correios),
                        contentDescription = null
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_shopee),
                        contentDescription = null
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_aliexpress),
                        contentDescription = null
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_shein),
                        contentDescription = null
                    )
                }
                Row {
                    Icon(
                        painter = painterResource(R.drawable.ic_loggi),
                        contentDescription = null
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_melhor_envio),
                        contentDescription = null
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_tex),
                        contentDescription = null
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_jtex),
                        contentDescription = null
                    )
                }
            }
        }
    }
}