package com.gothwad.accounts.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gothwad.accounts.core.GothwadAccounts

fun GothwadAccounts.openAccountManagement(activity: Activity) {
    activity.startActivity(Intent(activity, AccountManagementActivity::class.java))
}

class AccountManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GothwadAccountsTheme { GothwadAccountsScreen() } }
    }
}
