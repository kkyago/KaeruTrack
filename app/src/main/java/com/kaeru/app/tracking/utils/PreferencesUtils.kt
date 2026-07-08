package com.kaeru.app.tracking.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(key: String, defaultValue: T): MutableState<T> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kaeru_ui_prefs", Context.MODE_PRIVATE) }

    val savedName = prefs.getString(key, null)
    val initialValue = savedName?.let {
        try { enumValueOf<T>(it) } catch (e: Exception) { defaultValue }
    } ?: defaultValue

    val state = remember { mutableStateOf(initialValue) }

    return remember {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(newValue) {
                    state.value = newValue
                    prefs.edit().putString(key, newValue.name).apply()
                }
            override fun component1() = state.value
            override fun component2(): (T) -> Unit = { this.value = it }
        }
    }
}

@Composable
fun rememberBooleanPreference(key: String, defaultValue: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kaeru_ui_prefs", Context.MODE_PRIVATE) }

    val initialValue = prefs.getBoolean(key, defaultValue)
    val state = remember { mutableStateOf(initialValue) }

    return remember {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = state.value
                set(newValue) {
                    state.value = newValue
                    prefs.edit().putBoolean(key, newValue).apply()
                }
            override fun component1() = state.value
            override fun component2(): (Boolean) -> Unit = { this.value = it }
        }
    }
}