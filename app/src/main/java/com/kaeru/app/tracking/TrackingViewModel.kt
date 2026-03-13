package com.kaeru.app.tracking

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kaeru.app.tracking.database.TrackingDao
import com.kaeru.app.tracking.database.TrackingEntity
import com.kaeru.app.ui.screens.settings.KaeruThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.kaeru.app.data.UserPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import com.kaeru.app.data.BackupData
import com.kaeru.app.data.utils.GithubRelease
import kotlinx.coroutines.Job
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kaeru.app.AppDestinations
import com.kaeru.app.data.utils.UpdateWorker
import com.kaeru.app.tracking.utils.isDeliveredStatus
import com.kaeru.app.ui.screens.TrackingFilter
import com.kaeru.app.ui.screens.settings.SYSTEM_DEFAULT
import java.util.concurrent.TimeUnit

class TrackingViewModel(
    application: Application,
    private val repository: TrackingRepository,
    private val dao: TrackingDao
) : AndroidViewModel(application) {
    private val _updateRelease = MutableStateFlow<GithubRelease?>(null)
    val updateRelease = _updateRelease.asStateFlow()
    fun setUpdateRelease(release: GithubRelease?) {
        _updateRelease.value = release
    }
    private val userPrefs = UserPreferences(application)
    val userName: StateFlow<String> = userPrefs.userName
        .map { name ->
            name.ifBlank { "Usuário" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Usuário"
        )
    val userBio: StateFlow<String> = userPrefs.userBio
        .map { bio ->
            bio.ifBlank { "Bio" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Bio"
        )
    val userAvatar = userPrefs.userAvatar

    val historyList: StateFlow<List<TrackingEntity>> = dao.getAllTracking()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stats = historyList.map { list ->
        val deliveredCount = list.count { item ->
            item.lastStatus.isDeliveredStatus()
        }

        val inTransitCount = list.size - deliveredCount

        Pair(inTransitCount, deliveredCount)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Pair(0, 0)
    )

    fun updateProfile(name: String, bio: String) {
        userPrefs.saveName(name)
        userPrefs.saveBio(bio)
    }

    fun updateAvatar(uri: String) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            getApplication<Application>().contentResolver.takePersistableUriPermission(Uri.parse(uri), takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        userPrefs.saveAvatar(uri)
    }
    private val prefs = application.getSharedPreferences("kaeru_prefs", Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        KaeruThemeMode.valueOf(prefs.getString("theme_mode", KaeruThemeMode.SYSTEM.name) ?: KaeruThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<KaeruThemeMode> = _themeMode.asStateFlow()
    private val _currentThemeColor = MutableStateFlow(prefs.getInt("theme_color", 0xFF006C4C.toInt()))
    val currentThemeColor: StateFlow<Int> = _currentThemeColor.asStateFlow()
    private val _isAmoled = MutableStateFlow(prefs.getBoolean("is_amoled", false))
    val isAmoled: StateFlow<Boolean> = _isAmoled.asStateFlow()
    fun setThemeMode(mode: KaeruThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }
    fun setThemeColor(colorInt: Int) {
        _currentThemeColor.value = colorInt
        prefs.edit().putInt("theme_color", colorInt).apply()
    }
    fun toggleAmoled(enabled: Boolean) {
        _isAmoled.value = enabled
        prefs.edit().putBoolean("is_amoled", enabled).apply()
    }
    private val _checkUpdatesOnStart = MutableStateFlow(prefs.getBoolean("check_updates_start", true))
    val checkUpdatesOnStart: StateFlow<Boolean> = _checkUpdatesOnStart.asStateFlow()
    fun toggleCheckUpdatesOnStart(enabled: Boolean) {
        _checkUpdatesOnStart.value = enabled
        prefs.edit().putBoolean("check_updates_start", enabled).apply()
    }
    var trackingResult by mutableStateOf<TrackingResponse?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var showSaveDialog by mutableStateOf(false)
    var packageDescription by mutableStateOf("")
    private val _isSwipeToDeleteEnabled = MutableStateFlow(prefs.getBoolean("swipe_to_delete", false))
    val isSwipeToDeleteEnabled: StateFlow<Boolean> = _isSwipeToDeleteEnabled.asStateFlow()
    var showCpfDialog by mutableStateOf(false)
    var pendingJtCode by mutableStateOf("")
    var currentCpf by mutableStateOf<String?>(null)

    fun toggleSwipeToDelete(enabled: Boolean) {
        _isSwipeToDeleteEnabled.value = enabled
        prefs.edit().putBoolean("swipe_to_delete", enabled).apply()
    }
    private val _isSlimNav = MutableStateFlow(prefs.getBoolean("slim_navbar", false))
    val isSlimNav: StateFlow<Boolean> = _isSlimNav.asStateFlow()

    fun toggleSlimNav(enabled: Boolean) {
        _isSlimNav.value = enabled
        prefs.edit().putBoolean("slim_navbar", enabled).apply()
    }
    private val _defaultOpenTab = MutableStateFlow(
        AppDestinations.valueOf(prefs.getString("default_tab", AppDestinations.SEARCH.name) ?: AppDestinations.SEARCH.name)
    )
    val defaultOpenTab: StateFlow<AppDestinations> = _defaultOpenTab.asStateFlow()

    fun setDefaultTab(tab: AppDestinations) {
        _defaultOpenTab.value = tab
        prefs.edit().putString("default_tab", tab.name).apply()
    }
    private val _defaultHistoryFilter = MutableStateFlow(
        TrackingFilter.valueOf(prefs.getString("default_filter", TrackingFilter.IN_TRANSIT.name) ?: TrackingFilter.IN_TRANSIT.name)
    )
    val defaultHistoryFilter: StateFlow<TrackingFilter> = _defaultHistoryFilter.asStateFlow()

    fun setDefaultHistoryFilter(filter: TrackingFilter) {
        _defaultHistoryFilter.value = filter
        prefs.edit().putString("default_filter", filter.name).apply()
    }
    private var searchJob: Job? = null
    fun trackPackage(code: String, carrier: String = "Auto", providedCpf: String? = null) {
        searchJob?.cancel()
        errorMessage = null
        trackingResult = null
        val cleanCode = code.trim()
        if (carrier == "Auto" && !repository.isCarrierSupported(cleanCode)) {
            isLoading = false
            return
        }
        isLoading = true
        searchJob = viewModelScope.launch {
            val savedItem = historyList.value.find { it.code == cleanCode }
            var resolvedCpf = providedCpf
            if (cleanCode.length >= 10 && cleanCode.all { it.isDigit() }) {
                val cpfToUse = providedCpf ?: savedItem?.cpf ?: currentCpf

                if (cpfToUse.isNullOrEmpty()) {
                    pendingJtCode = cleanCode
                    showCpfDialog = true
                    isLoading = false
                    return@launch
                }
                resolvedCpf = cpfToUse
                currentCpf = cpfToUse
            }
            val isAlreadyDelivered = savedItem?.lastStatus?.isDeliveredStatus() ?: false
            if (isAlreadyDelivered) {
                val cached = TrackingCache.get(cleanCode)
                if (cached != null) {
                    trackingResult = cached
                } else {
                    trackingResult = TrackingResponse(
                        tracking_code = cleanCode,
                        events = listOf(
                            TrackingEvent(
                                status = savedItem?.lastStatus,
                                date = savedItem?.lastDate?.substringBefore(" ") ?: "",
                                time = savedItem?.lastDate?.substringAfter(" ") ?: "",
                                location = "Histórico Local",
                                subStatus = null
                            )
                        )
                    )
                }
                isLoading = false
                return@launch
            }
            val response = repository.trackPackage(cleanCode, forceRefresh = true, carrier = carrier, cpf = resolvedCpf)
            if (response != null) {
                trackingResult = response
                updateHistoryStatusIfExists(cleanCode, response)
            } else {
                if (cleanCode.length >= 10 && cleanCode.all { it.isDigit() } && providedCpf != null) {
                errorMessage = "Erro de CPF"
                showCpfDialog = true
            } else {
                errorMessage = "Pacote não encontrado ou erro na conexão."
            }
            }
            isLoading = false
        }
    }

    fun submitCpfAndTrack(cpfDigitado: String) {
        showCpfDialog = false
        errorMessage = null
        val codeToTrack = pendingJtCode
        if (codeToTrack.isNotEmpty() && cpfDigitado.isNotEmpty()) {
            trackPackage(code = codeToTrack, providedCpf = cpfDigitado)
        }
    }

    companion object {
        fun scheduleTrackingWorker(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<TrackingWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "KaeruTrackingWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        fun scheduleUpdateWorker(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val periodicRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "KaeruUpdateWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }

    fun saveTracking(context: Context) {
        val currentResult = trackingResult ?: return
        val currentCode = currentResult.tracking_code ?: return
        val lastEvent = currentResult.events?.firstOrNull()
        val firstEvent = currentResult.events?.lastOrNull()

        viewModelScope.launch {
            val entity = TrackingEntity(
                code = currentCode,
                description = packageDescription.ifBlank { "Encomenda Sem Nome" },
                lastStatus = lastEvent?.status ?: "Aguardando",
                lastDate = lastEvent?.date ?: "",
                firstDate = firstEvent?.date ?: "",
                cpf = currentCpf
            )
            dao.insertTracking(entity)

            scheduleTrackingWorker(context)

            showSaveDialog = false
            packageDescription = ""
        }
    }

    fun updatePackageDescription(code: String, newName: String) {
        viewModelScope.launch {
            dao.updateDescription(code, newName)
        }
    }
    fun deleteTracking(code: String) {
        viewModelScope.launch {
            dao.deleteTracking(code)
        }
    }
    fun restoreTracking(item: TrackingEntity) {
        viewModelScope.launch {
            dao.insertTracking(item)
        }
    }
    private suspend fun updateHistoryStatusIfExists(code: String, response: TrackingResponse) {
        val savedItem = historyList.value.find { it.code == code } ?: return
        val latestEvent = response.events?.firstOrNull() ?: return
        val firstEvent = response.events?.lastOrNull() ?: return

        val updatedItem = savedItem.copy(
            lastStatus = latestEvent.status ?: savedItem.lastStatus,
            lastDate = "${latestEvent.date ?: ""} ${latestEvent.time ?: ""}".trim(),
            firstDate = "${firstEvent.date ?: ""} ${firstEvent.time ?: ""}".trim(),
            cpf = currentCpf ?: savedItem.cpf,
        )
        dao.insertTracking(updatedItem)
    }

    fun openSaveDialog() {
        showSaveDialog = true
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentName = userName.first()
                val currentBio = userBio.first()
                val currentHistory = historyList.first()
                val backup = BackupData(
                    userName = currentName,
                    userBio = currentBio,
                    history = currentHistory
                )
                val jsonString = Gson().toJson(backup)

                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                launch(Dispatchers.Main) {
                }

            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                }
            }
        }
    }
    fun importBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val stringBuilder = StringBuilder()

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val jsonString = stringBuilder.toString()
                val backup = Gson().fromJson(jsonString, BackupData::class.java)
                userPrefs.saveName(backup.userName)
                userPrefs.saveBio(backup.userBio)
                if (backup.history.isNotEmpty()) {
                    dao.insertAll(backup.history)
                }

                launch(Dispatchers.Main) {
                }

            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                }
            }
        }
    }
    private val _appLanguage = MutableStateFlow(SYSTEM_DEFAULT)
    val appLanguage = _appLanguage.asStateFlow()
    fun setLanguage(languageCode: String) {
        userPrefs.saveLanguage(languageCode)
        _appLanguage.value = languageCode
        val appLocale: LocaleListCompat = if (languageCode == SYSTEM_DEFAULT) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}