package com.example.snapeats.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapeats.data.local.dao.MealLogDao
import com.example.snapeats.data.local.dao.UserDao
import com.example.snapeats.data.local.entity.MealLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

data class HistoryUiState(
    val groupedLogs: Map<String, List<MealLog>> = emptyMap(),
    val weeklyTotals: List<Int> = List(7) { 0 },
    val sevenDayAverage: Int = 0,
    val dailyTarget: Int = 2000,
    val isLoading: Boolean = true,
    val error: String? = null,
    val pendingDeleteId: Int? = null
)

class HistoryViewModel(
    private val mealLogDao: MealLogDao,
    private val userDao: UserDao,
    private val userId: Int
) : ViewModel() {

    private var deleteJob: Job? = null
    private val _pendingDeleteId = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        mealLogDao.getAllMealLogs(userId),
        _pendingDeleteId
    ) { allLogs, pendingId ->
        buildUiState(allLogs, pendingId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(isLoading = true)
    )

    fun deleteMealLog(log: MealLog) {
        deleteJob?.let { prior ->
            if (prior.isActive) prior.cancel()
            val previousId = _pendingDeleteId.value
            if (previousId != null) {
                viewModelScope.launch {
                    val logToDelete = uiState.value.groupedLogs.values.flatten()
                        .firstOrNull { it.id == previousId } ?: return@launch
                    mealLogDao.deleteMealLog(logToDelete)
                }
            }
        }

        _pendingDeleteId.update { log.id }

        deleteJob = viewModelScope.launch {
            delay(5_000L)
            commitPendingDelete()
        }
    }

    fun undoDelete() {
        deleteJob?.cancel()
        deleteJob = null
        _pendingDeleteId.update { null }
    }

    fun allLogsSnapshot(): List<MealLog> =
        uiState.value.groupedLogs.values.flatten()

    private fun commitPendingDelete() {
        val idToDelete = _pendingDeleteId.value ?: return
        _pendingDeleteId.update { null }
        viewModelScope.launch {
            val allLogs = uiState.value.groupedLogs.values.flatten()
            val logToDelete = allLogs.firstOrNull { it.id == idToDelete } ?: return@launch
            mealLogDao.deleteMealLog(logToDelete)
        }
    }

    private suspend fun buildUiState(
        allLogs: List<MealLog>,
        pendingDeleteId: Int?
    ): HistoryUiState {
        val user = userDao.getUser(userId).first()
        val dailyTarget = user?.dailyCalTarget ?: 2000

        val visibleLogs = if (pendingDeleteId != null) {
            allLogs.filter { it.id != pendingDeleteId }
        } else {
            allLogs
        }

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val groupedLogs: Map<String, List<MealLog>> = visibleLogs
            .sortedByDescending { it.timestamp }
            .groupBy { log -> dateFormatter.format(Date(log.timestamp)) }

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weeklyTotals = (6 downTo 0).map { daysAgo ->
            val day = today.minusDays(daysAgo.toLong())
            val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            visibleLogs
                .filter { it.timestamp in dayStart until dayEnd }
                .sumOf { it.totalCal }
        }

        val sevenDayAverage = weeklyTotals.average().toInt()

        return HistoryUiState(
            groupedLogs = groupedLogs,
            weeklyTotals = weeklyTotals,
            sevenDayAverage = sevenDayAverage,
            dailyTarget = dailyTarget,
            isLoading = false,
            error = null,
            pendingDeleteId = pendingDeleteId
        )
    }

    companion object {
        fun factory(
            mealLogDao: MealLogDao,
            userDao: UserDao,
            userId: Int
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(mealLogDao, userDao, userId) as T
            }
        }
    }
}
