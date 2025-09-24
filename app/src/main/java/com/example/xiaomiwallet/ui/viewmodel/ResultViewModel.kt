package com.example.xiaomiwallet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xiaomiwallet.data.ResultManager
import com.example.xiaomiwallet.data.TaskResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ResultUiState(
    val results: List<TaskResult> = emptyList(),
    val totalExecutions: Int = 0,
    val successfulExecutions: Int = 0,
    val failedExecutions: Int = 0,
    val lastExecutionTime: String = "暂无",
    val selectedResult: TaskResult? = null,
    val showDetailsDialog: Boolean = false
)

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ResultManager.results.collect { results ->
                _uiState.update { currentState ->
                    currentState.copy(
                        results = results.sortedByDescending { it.startTime },
                        totalExecutions = results.size,
                        successfulExecutions = results.count { it.success },
                        failedExecutions = results.count { !it.success },
                        lastExecutionTime = results.maxByOrNull { it.startTime }?.startTime ?: "暂无"
                    )
                }
            }
        }
    }

    fun refreshResults() {
        // ResultManager会自动更新，无需手动刷新
    }

    fun clearAllResults() {
        ResultManager.clearResults()
    }

    fun deleteResult(resultId: String) {
        ResultManager.deleteResult(resultId)
    }

    fun showResultDetails(result: TaskResult) {
        _uiState.update { 
            it.copy(
                selectedResult = result,
                showDetailsDialog = true
            ) 
        }
    }

    fun hideResultDetails() {
        _uiState.update { 
            it.copy(
                selectedResult = null,
                showDetailsDialog = false
            ) 
        }
    }


}
