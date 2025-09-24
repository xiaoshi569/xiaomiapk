package com.example.xiaomiwallet.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class TaskResult(
    val id: String,
    val accountName: String,
    val startTime: String,
    val endTime: String,
    val duration: String,
    val success: Boolean,
    val errorMessage: String = "",
    val logs: List<String> = emptyList(),
    val exchangeResults: List<String> = emptyList()
)

object ResultManager {
    private val _results = MutableStateFlow<List<TaskResult>>(emptyList())
    val results: StateFlow<List<TaskResult>> = _results.asStateFlow()

    fun addTaskResult(
        accountName: String,
        success: Boolean,
        errorMessage: String = "",
        logs: List<String> = emptyList(),
        exchangeResults: List<String> = emptyList(),
        startTime: String,
        endTime: String
    ) {
        val startDate = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(startTime)
        } catch (e: Exception) {
            Date()
        }
        
        val endDate = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(endTime)
        } catch (e: Exception) {
            Date()
        }
        
        val duration = if (startDate != null && endDate != null) {
            val durationMs = endDate.time - startDate.time
            val minutes = durationMs / 60000
            val seconds = (durationMs % 60000) / 1000
            "${minutes}分${seconds}秒"
        } else {
            "未知"
        }

        val newResult = TaskResult(
            id = UUID.randomUUID().toString(),
            accountName = accountName,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            success = success,
            errorMessage = errorMessage,
            logs = logs,
            exchangeResults = exchangeResults
        )

        _results.value = (_results.value + newResult).takeLast(50) // 只保留最近50条记录
    }

    fun clearResults() {
        _results.value = emptyList()
    }

    fun deleteResult(resultId: String) {
        _results.value = _results.value.filter { it.id != resultId }
    }
}
