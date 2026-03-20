package com.kaeru.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kaeru.app.R
import kotlinx.coroutines.delay

@Composable
fun DefaultDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                horizontalAlignment = horizontalAlignment,
                modifier = modifier.padding(24.dp)
            ) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.iconContentColor) {
                        Box(Modifier.align(Alignment.CenterHorizontally)) {
                            icon()
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (title != null) {
                    CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                            Box(
                                Modifier.align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally)
                            ) {
                                title()
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                content()

                if (buttons != null) {
                    Spacer(Modifier.height(24.dp))
                    FlowRow(
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                                buttons()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionPromptDialog(
    title: String? = null,
    titleBar: @Composable (RowScope.() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onReset: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title = if (titleBar != null) {
            { Row { titleBar() } }
        } else if (title != null) {
            {
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        } else null,
        buttons = {
            if (onReset != null) {
                Row(modifier = Modifier.weight(1f)) {
                    TextButton(onClick = { onReset() }) {
                        Text("Resetar")
                    }
                }
            }
            if (onCancel != null) {
                TextButton(onClick = { onCancel() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(android.R.string.ok))
            }
        }
    ) {
        content()
    }
}

@Composable
fun ListDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .padding(vertical = 24.dp)
                    .imePadding(),
            ) {
                LazyColumn(content = content)
            }
        }
    }
}

@Composable
fun InfoLabel(
    text: String
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(horizontal = 8.dp)
) {
    Icon(
        painter = painterResource(android.R.drawable.ic_dialog_info),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(4.dp)
    )
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun TextFieldDialog(
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    initialTextFieldValue: TextFieldValue = TextFieldValue(),
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    autoFocus: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 10,
    isInputValid: (String) -> Boolean = { it.isNotEmpty() },
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: (String) -> Unit = {},
    textFields: List<Pair<String, TextFieldValue>>? = null,
    onTextFieldsChange: ((Int, TextFieldValue) -> Unit)? = null,
    onDoneMultiple: ((List<String>) -> Unit)? = null,
    onDismiss: () -> Unit,
    autoDismiss: Boolean = true,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val legacyFieldState = remember { mutableStateOf(initialTextFieldValue) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            delay(300)
            focusRequester.requestFocus()
        }
    }

    DefaultDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        icon = icon,
        title = title,
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }

            val isValid = textFields?.all { isInputValid(it.second.text) }
                ?: isInputValid(legacyFieldState.value.text)

            TextButton(
                enabled = isValid,
                onClick = {
                    if (autoDismiss) onDismiss()
                    if (textFields != null && onDoneMultiple != null) {
                        onDoneMultiple(textFields.map { it.second.text })
                    } else {
                        onDone(legacyFieldState.value.text)
                    }
                }
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        }
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f, fill = false)
        ) {
            if (textFields != null) {
                textFields.forEachIndexed { index, (label, value) ->
                    TextField(
                        value = value,
                        onValueChange = { onTextFieldsChange?.invoke(index, it) },
                        placeholder = { Text(label) },
                        singleLine = singleLine,
                        maxLines = maxLines,
                        colors = OutlinedTextFieldDefaults.colors(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                            keyboardType = keyboardType
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (onDoneMultiple != null) {
                                    onDoneMultiple(textFields.map { it.second.text })
                                    if (autoDismiss) onDismiss()
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index < textFields.size - 1) 12.dp else 0.dp)
                            .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                    )
                }
            } else {
                TextField(
                    value = legacyFieldState.value,
                    onValueChange = { legacyFieldState.value = it },
                    placeholder = placeholder,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    colors = OutlinedTextFieldDefaults.colors(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (singleLine) ImeAction.Done else ImeAction.None,
                        keyboardType = keyboardType
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onDone(legacyFieldState.value.text)
                            if (autoDismiss) onDismiss()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            extraContent?.invoke()
        }
    }
}

@Composable
fun CpfInputDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var cpfInput by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(title) },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
            TextButton(
                onClick = {
                    onSubmit(cpfInput)
                },
                enabled = cpfInput.length == 11
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = cpfInput,
                onValueChange = { novoValor ->
                    val apenasNumeros = novoValor.filter { it.isDigit() }
                    if (apenasNumeros.length <= 11) {
                        cpfInput = apenasNumeros
                    }
                },
                label = { Text(stringResource(R.string.cpf_only_numbers)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (cpfInput.length == 11) {
                            onDismiss()
                            onSubmit(cpfInput)
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    }
}

@Composable
fun BatteryOptimizationDialog(
    onDismiss: () -> Unit,
    onNeverShowAgain: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Desative a otimização de bateria") },
        text = {
            Text("A otimização de bateria está ativa e restringindo as notificações de entrega. Você gostaria de desativar?")
        },
        confirmButton = {
            Button(onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                onDismiss()
            }) {
                Text("Desativar")
            }
        },
        dismissButton = {
            TextButton(onClick = onNeverShowAgain) {
                Text("Cancelar")
            }
        }
    )
}