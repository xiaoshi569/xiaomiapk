package com.example.xiaomiwallet.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xiaomiwallet.data.model.Account
import com.example.xiaomiwallet.data.model.ExchangeConfig
import com.example.xiaomiwallet.ui.viewmodel.ExchangeViewModel

@Composable
fun ExchangeScreen(viewModel: ExchangeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("会员兑换管理", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(uiState.statusText, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))


        AddExchangeCard(
            accounts = uiState.accounts,
            onAddConfig = { us, config -> viewModel.addExchangeConfig(us, config) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { viewModel.runExchange() }, modifier = Modifier.fillMaxWidth()) {
            Text("执行所有兑换")
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.accounts, key = { it.us }) { account ->
                AccountExchangeItem(
                    account = account,
                    onDeleteConfig = { configType -> viewModel.deleteExchangeConfig(account.us, configType) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExchangeCard(
    accounts: List<Account>,
    onAddConfig: (String, ExchangeConfig) -> Unit
) {
    var selectedAccountUs by remember { mutableStateOf("") }
    var selectedMembership by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var membershipDropdownExpanded by remember { mutableStateOf(false) }

    val membershipTypes = listOf("腾讯视频", "爱奇艺", "优酷", "芒果TV")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("添加兑换配置", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Account Dropdown
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded,
                onExpandedChange = { accountDropdownExpanded = !accountDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedAccountUs,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("选择账号") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = accountDropdownExpanded,
                    onDismissRequest = { accountDropdownExpanded = false }
                ) {
                    accounts.filter { it.userId != null }.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.us) },
                            onClick = {
                                selectedAccountUs = account.us
                                accountDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Membership Dropdown
            ExposedDropdownMenuBox(
                expanded = membershipDropdownExpanded,
                onExpandedChange = { membershipDropdownExpanded = !membershipDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedMembership,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("会员类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = membershipDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = membershipDropdownExpanded,
                    onDismissRequest = { membershipDropdownExpanded = false }
                ) {
                    membershipTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedMembership = type
                                membershipDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("手机号") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedAccountUs.isNotBlank() && selectedMembership.isNotBlank() && phoneNumber.isNotBlank()) {
                        onAddConfig(selectedAccountUs, ExchangeConfig(selectedMembership, phoneNumber))
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("添加配置")
            }
        }
    }
}

@Composable
fun AccountExchangeItem(
    account: Account,
    onDeleteConfig: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "账号: ${account.us}", fontWeight = FontWeight.Bold)
            Text(text = "小米ID: ${account.userId ?: "未登录"}", fontSize = 14.sp)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            if (account.exchangeConfigs.isEmpty()) {
                Text("暂无兑换配置", fontSize = 14.sp)
            } else {
                account.exchangeConfigs.forEach { config ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📺 ${config.type} → ${config.phone}")
                        IconButton(onClick = { onDeleteConfig(config.type) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除配置")
                        }
                    }
                }
            }
        }
    }
}
