package com.example.snapeats.ui.history

import androidx.lifecycle.ViewModel
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

// ---------------------------------------------------------------------------
// UiState
// ---------------------------------------------------------------------------

data class HistoryUiState(
    /**
     * Meal logs grouped by human-readable date ("dd MMM yyyy"), in descending
     * chronological order. Each group is also sorted newest-first.
     */
    val groupedLogs: Map<String, List<MealLog>> = emptyMap(),

    /**
     * Daily calorie totals for the past 7 days.
     * Index 0 = six days ago, index 6 = today.
     * Days with no logs contribute 0.
     */
    val weeklyTotals: List<Int> = List(7) { 0 },

    /** Average daily calories over the past 7 days (rounded). */
    val sevenDayAverage: Int = 0,

    /** User's computed daily calorie target from the Harris-Benedict formula. */
    val dailyTarget: Int = 2000,

    val isLoading: Boolean = true,
    val error: String? = null,

    /**
     * ID of the log currently pending deletion.
     * While this is non-null the row is hidden in the UI and a Snackbar with
     * "Undo" is visible. After 5 seconds the row is physically deleted.
     */
    val pendingDeleteId: Int? = null
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class HistoryViewModel(
    private val mealLogDao: MealLogDao,
    private val userDao: UserDao
) : ViewModel() {

    /** Active countdown Job. Cancelling it reverts the optimistic delete. */
    private var deleteJob: Job? = null

    /** Emits the ID of the row that is optimistically hidden pending deletion. */
    private val _pendingDeleteId = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        mealLogDao.getAllMealLogs(), // Flow<List<MealLog>> — Room emits on every change
        _pendingDeleteId
    ) { allLogs, pendingId ->
        buildUiState(allLogs, pendingId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(isLoading = true)
    )

    // -------------------------------------------------------------------------
    // Public events
    // -------------------------------------------------------------------------

    /**
     * Begins the delete flow for [log].
     *
     * 1. Any previously pending delete is committed immediately (its Job is
     *    already cancelled-or-complete, so we just call the DB delete now).
     * 2. The new log's ID is stored in [_pendingDeleteId] so the UI hides it.
     * 3. A 5-second countdown Job is launched. When it completes it calls
     *    [commitPendingDelete] to do the actual Room deletion.
     */
    fun deleteMealLog(log: MealLog) {
        // Commit any prior pending delete before starting a new one
        deleteJob?.let { prior ->
            if (prior.isActive) prior.cancel()
            // Commit the previously-pending id synchronously on this coroutine
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

    /**
     * Cancels the pending delete countdown, restoring the row to the list.
     * Safe to call even if there is no active pending delete.
     */
    fun undoDelete() {
        deleteJob?.cancel()
        deleteJob = null
        _pendingDeleteId.update { null }
    }

    /**
     * Returns a flat snapshot of all currently-visible logs.
     * Used by [PdfExporter] to access raw [MealLog] entities.
     */
    fun allLogsSnapshot(): List<MealLog> =
        uiState.value.groupedLogs.values.flatten()

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /** Performs the physical Room deletion for the currently-pending ID. */
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
        val user = userDao.getUser().first()
        val dailyTarget = user?.dailyCalTarget ?: 2000

        // Exclude the row that is currently pending deletion
        val visibleLogs = if (pendingDeleteId != null) {
            allLogs.filter { it.id != pendingDeleteId }
        } else {
            allLogs
        }

        // ---- Group by date string ("dd MMM yyyy"), descending ---------------
        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val groupedLogs: Map<String, List<MealLog>> = visibleLogs
            .sortedByDescending { it.timestamp }
            .groupBy { log -> dateFormatter.format(Date(log.timestamp)) }

        // ---- Weekly totals (index 0 = 6 days ago, index 6 = today) ----------
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
}
