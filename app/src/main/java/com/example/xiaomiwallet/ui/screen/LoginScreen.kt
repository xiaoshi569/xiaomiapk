package com.example.xiaomiwallet.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.xiaomiwallet.ui.viewmodel.LoginMethod
import com.example.xiaomiwallet.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("账号登录", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Login Method Toggle
        SegmentedButtonToggle(
            selected = uiState.loginMethod,
            onSelectedChange = { viewModel.onLoginMethodChange(it) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.alias,
            onValueChange = { viewModel.onAliasChange(it) },
            label = { Text("账号别名 (必填)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !uiState.isLoading
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.loginMethod) {
            LoginMethod.QR -> QrLoginContent(viewModel)
            LoginMethod.COOKIE -> CookieLoginContent(viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = uiState.statusText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun QrLoginContent(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { viewModel.generateQrCode() },
            enabled = !uiState.isLoading && uiState.alias.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("生成二维码")
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        uiState.qrUrl?.let { url ->
            Image(
                painter = rememberAsyncImagePainter(url),
                contentDescription = "登录二维码",
                modifier = Modifier.size(240.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieLoginContent(viewModel: LoginViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = uiState.userId,
            onValueChange = { viewModel.onUserIdChange(it) },
            label = { Text("userId (必填)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.passToken,
            onValueChange = { viewModel.onPassTokenChange(it) },
            label = { Text("passToken (必填)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { viewModel.saveAccountFromCookie() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存账号")
        }
    }
}

@Composable
fun SegmentedButtonToggle(
    selected: LoginMethod,
    onSelectedChange: (LoginMethod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { onSelectedChange(LoginMethod.QR) },
            colors = if (selected == LoginMethod.QR) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.weight(1f)
        ) {
            Text("扫码登录")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onSelectedChange(LoginMethod.COOKIE) },
            colors = if (selected == LoginMethod.COOKIE) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
            modifier = Modifier.weight(1f)
        ) {
            Text("Cookie登录")
        }
    }
}
