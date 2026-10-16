package com.gothwad.accounts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gothwad.accounts.ui.GothwadAccountsScreen
import com.gothwad.accounts.ui.GothwadAccountsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GothwadAccountsTheme { GothwadAccountsScreen() } }
    }
}
