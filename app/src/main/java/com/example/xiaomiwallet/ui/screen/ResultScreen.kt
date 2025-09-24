package com.example.xiaomiwallet.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xiaomiwallet.data.TaskResult
import com.example.xiaomiwallet.ui.viewmodel.ResultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(viewModel: ResultViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "运行结果",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                Button(
                    onClick = { viewModel.refreshResults() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("刷新")
                }
                
                Button(
                    onClick = { viewModel.clearAllResults() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清空")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 统计信息
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("执行统计", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("总执行次数: ${uiState.totalExecutions}")
                Text("成功次数: ${uiState.successfulExecutions}")
                Text("失败次数: ${uiState.failedExecutions}")
                Text("最后执行: ${uiState.lastExecutionTime}")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (uiState.results.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无执行记录",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.results, key = { it.id }) { result ->
                    TaskResultCard(
                        result = result,
                        onDelete = { viewModel.deleteResult(it.id) },
                        onViewDetails = { viewModel.showResultDetails(it) }
                    )
                }
            }
        }
    }
    
    // 详情对话框
    if (uiState.showDetailsDialog) {
        uiState.selectedResult?.let { selectedResult ->
            TaskResultDetailsDialog(
                result = selectedResult,
                onDismiss = { viewModel.hideResultDetails() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskResultDetailsDialog(
    result: TaskResult,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    // 构建完整的日志文本用于复制
    val fullLogText = buildString {
        appendLine("=== 任务执行详情 ===")
        appendLine("账号: ${result.accountName}")
        appendLine("开始时间: ${result.startTime}")
        appendLine("结束时间: ${result.endTime}")
        appendLine("执行时长: ${result.duration}")
        appendLine("执行结果: ${if (result.success) "成功" else "失败"}")
        if (result.errorMessage.isNotBlank()) {
            appendLine("错误信息: ${result.errorMessage}")
        }
        appendLine()
        appendLine("=== 执行日志 ===")
        result.logs.forEach { log ->
            appendLine(log)
        }
        if (result.exchangeResults.isNotEmpty()) {
            appendLine()
            appendLine("=== 兑换结果 ===")
            result.exchangeResults.forEach { exchangeResult ->
                appendLine(exchangeResult)
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("任务执行详情")
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(fullLogText))
                    }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "复制日志"
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("基本信息", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("账号: ${result.accountName}")
                            Text("开始时间: ${result.startTime}")
                            Text("结束时间: ${result.endTime}")
                            Text("执行时长: ${result.duration}")
                            Text(
                                text = "执行结果: ${if (result.success) "✅ 成功" else "❌ 失败"}",
                                color = if (result.success) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                            
                            if (result.errorMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "错误信息: ${result.errorMessage}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                if (result.logs.isNotEmpty()) {
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("执行日志", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "可选择文本复制",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                SelectionContainer {
                                    Column {
                                        result.logs.forEach { log ->
                                            Text(
                                                text = log,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (result.exchangeResults.isNotEmpty()) {
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("兑换结果", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "可选择文本复制",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                SelectionContainer {
                                    Column {
                                        result.exchangeResults.forEach { exchangeResult ->
                                            Text(
                                                text = exchangeResult,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskResultCard(
    result: TaskResult,
    onDelete: (TaskResult) -> Unit,
    onViewDetails: (TaskResult) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    
    // 构建单条记录的完整信息用于复制
    val cardLogText = buildString {
        appendLine("=== 任务执行记录 ===")
        appendLine("账号: ${result.accountName}")
        appendLine("开始时间: ${result.startTime}")
        appendLine("结束时间: ${result.endTime}")
        appendLine("执行时长: ${result.duration}")
        appendLine("执行结果: ${if (result.success) "成功" else "失败"}")
        if (result.errorMessage.isNotBlank()) {
            appendLine("错误信息: ${result.errorMessage}")
        }
        if (result.logs.isNotEmpty()) {
            appendLine()
            appendLine("=== 执行日志 ===")
            result.logs.forEach { log ->
                appendLine(log)
            }
        }
        if (result.exchangeResults.isNotEmpty()) {
            appendLine()
            appendLine("=== 兑换结果 ===")
            result.exchangeResults.forEach { exchangeResult ->
                appendLine(exchangeResult)
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* 普通点击不做任何事 */ },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboardManager.setText(AnnotatedString(cardLogText))
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 长按复制提示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "长按复制日志",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "账号: ${result.accountName}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = result.startTime,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    TextButton(onClick = { onViewDetails(result) }) {
                        Text("查看详情")
                    }
                    
                    IconButton(onClick = { onDelete(result) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (result.success) "✅ 执行成功" else "❌ 执行失败",
                    color = if (result.success) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
                
                Text(
                    text = "耗时: ${result.duration}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (result.errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.errorMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// 数据类定义
