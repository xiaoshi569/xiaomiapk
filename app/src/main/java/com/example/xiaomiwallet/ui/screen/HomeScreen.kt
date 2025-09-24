package com.example.xiaomiwallet.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xiaomiwallet.ui.theme.XiaomiWalletTheme
import com.example.xiaomiwallet.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "小米钱包每日任务",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Config Section ---
        OutlinedTextField(
            value = uiState.licenseKey,
            onValueChange = { viewModel.onLicenseKeyChange(it) },
            label = { Text("授权码 (必填)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.pushPlusToken,
            onValueChange = { viewModel.onPushPlusTokenChange(it) },
            label = { Text("Push Plus Token (可选)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { viewModel.saveSettings() }) {
                Text("保存配置")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { viewModel.verifyLicense() }) {
                Text("验证授权")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Run Section ---
        if (uiState.isCountingDown) {
            Column {
                Button(
                    onClick = { viewModel.cancelAutoRun() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Red)
                ) {
                    Text("取消自动运行 (${uiState.countdownSeconds}s)", fontSize = 18.sp)
                }
            }
        } else {
            Button(
                onClick = { viewModel.runAllTasks() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("执行签到任务", fontSize = 18.sp)
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(checked = uiState.autoRunEnabled, onCheckedChange = { viewModel.onAutoRunChange(it) })
            Text("启动时自动运行任务")
        }
        
        // 只有在禁用自动运行时才显示手动倒计时按钮
        if (!uiState.autoRunEnabled && uiState.licenseVerified && !uiState.isCountingDown) {
            Button(
                onClick = { viewModel.startAutoRunCountdown() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("手动倒计时运行")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Status Section ---
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("账号数量: ${uiState.accountCount}", fontWeight = FontWeight.Bold)
                Text("已登录: ${uiState.loggedInCount}", fontWeight = FontWeight.Bold)
                Text(
                    text = if (uiState.licenseVerified) "授权状态: ✅ 已验证" else "授权状态: ❌ 未验证",
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            uiState.statusText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    XiaomiWalletTheme {
        HomeScreen()
    }
}
