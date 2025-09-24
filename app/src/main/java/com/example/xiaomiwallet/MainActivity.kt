package com.example.xiaomiwallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.xiaomiwallet.ui.screen.MainScreen
import com.example.xiaomiwallet.ui.theme.XiaomiWalletTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XiaomiWalletTheme {
                MainScreen()
            }
        }
    }
}
