package com.example.xiaomiwallet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xiaomiwallet.data.AccountRepository
import com.example.xiaomiwallet.data.model.Account
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepository = AccountRepository(application)

    val accounts: StateFlow<List<Account>> = accountRepository.accountsFlow
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteAccount(us: String) {
        viewModelScope.launch {
            accountRepository.deleteAccount(us)
        }
    }
}
