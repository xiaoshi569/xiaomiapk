package com.example.xiaomiwallet.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.xiaomiwallet.data.model.Account
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.accountDataStore: DataStore<Preferences> by preferencesDataStore(name = "accounts")

class AccountRepository(private val context: Context) {

    private val gson = Gson()

    private object PreferencesKeys {
        val ACCOUNTS_JSON = stringPreferencesKey("accounts_json")
    }

    val accountsFlow: Flow<List<Account>> = context.accountDataStore.data
        .map { preferences ->
            val jsonString = preferences[PreferencesKeys.ACCOUNTS_JSON]
            if (jsonString.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<Account>>() {}.type
                gson.fromJson(jsonString, type)
            }
        }

    suspend fun saveAccounts(accounts: List<Account>) {
        val jsonString = gson.toJson(accounts)
        context.accountDataStore.edit { settings ->
            settings[PreferencesKeys.ACCOUNTS_JSON] = jsonString
        }
    }

    suspend fun addAccount(account: Account) {
        val currentAccounts = accountsFlow.first()
        val updatedList = currentAccounts.toMutableList()
        // Remove existing account with the same alias if it exists
        updatedList.removeAll { it.us == account.us }
        updatedList.add(account)
        saveAccounts(updatedList)
    }

    suspend fun deleteAccount(us: String) {
        val currentAccounts = accountsFlow.first()
        val updatedList = currentAccounts.filterNot { it.us == us }
        saveAccounts(updatedList)
    }
}
