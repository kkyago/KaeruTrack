package com.kaeru.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaeru.app.MainActivity
import com.kaeru.app.ui.theme.KaeruTrackTheme
import com.kaeru.app.data.utils.CrashHandler
import com.kaeru.app.ui.screens.settings.KaeruThemeMode
import kotlinx.coroutines.delay
import kotlin.system.exitProcess
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Email
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.kaeru.app.R
import androidx.core.net.toUri

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashLog = intent.getStringExtra(CrashHandler.EXTRA_CRASH_LOG) ?: "Log não encontrado."

        val prefs = getSharedPreferences("kaeru_prefs", Context.MODE_PRIVATE)
        val themeModeStr = prefs.getString("theme_mode", KaeruThemeMode.SYSTEM.name) ?: KaeruThemeMode.SYSTEM.name
        val themeMode = KaeruThemeMode.valueOf(themeModeStr)
        val themeColorInt = prefs.getInt("theme_color", 0xFF006C4C.toInt())
        val isAmoled = prefs.getBoolean("is_amoled", false)

        setContent {
            var isCopiado by remember { mutableStateOf(false) }
            if (isCopiado) {
                LaunchedEffect(Unit) {
                    delay(2000)
                    isCopiado = false
                }
            }
            val useDarkTheme = when (themeMode) {
                KaeruThemeMode.LIGHT -> false
                KaeruThemeMode.DARK -> true
                KaeruThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            KaeruTrackTheme(
                darkTheme = useDarkTheme,
                pureBlack = isAmoled,
                seedColor = Color(themeColorInt)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Spacer(modifier = Modifier.height(32.dp))

                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_crashlog_screen),
                                contentDescription = null,
                                modifier = Modifier.size(200.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Text(
                            text = stringResource(R.string.unexpected_error),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.unexpected_error_description),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {

                                Text(
                                    text = crashLog,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState())
                                )

                                val buttonWidth by androidx.compose.animation.core.animateDpAsState(
                                    targetValue = if (isCopiado) 120.dp else 35.dp,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                    ),
                                    label = "buttonWidth"
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 12.dp, end = 12.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            val clipboard =
                                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip =
                                                ClipData.newPlainText("Crash Log Kaeru", crashLog)
                                            clipboard.setPrimaryClip(clip)
                                            isCopiado = true
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.size(width = buttonWidth, height = 35.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .fillMaxHeight()
                                                .width(buttonWidth - 21.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(
                                                        topEnd = 9.dp,
                                                        bottomEnd = 9.dp
                                                    )
                                                )
                                                .padding(start = 24.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = isCopiado,
                                                enter = androidx.compose.animation.fadeIn(
                                                    animationSpec = androidx.compose.animation.core.tween(delayMillis = 100)
                                                ),
                                                exit = androidx.compose.animation.fadeOut(
                                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 100)
                                                )
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.copied),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 14.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .size(35.dp)
                                                .border(
                                                    width = 0.1.dp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    shape = RoundedCornerShape(9.dp)
                                                )
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(9.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_copy),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    }
                                    startActivity(intent)
                                    finish()
                                    exitProcess(0)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.restart_app))
                            }

                            Button(
                                onClick = {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = "mailto:".toUri()
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support.kaeru@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, "(Describe the problem)")
                                        putExtra(Intent.EXTRA_TEXT, crashLog)
                                    }
                                    startActivity(Intent.createChooser(emailIntent, ""))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.send_logs))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}