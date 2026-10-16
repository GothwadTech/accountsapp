package com.gothwad.accounts

import android.app.Application
import com.gothwad.accounts.core.GothwadAccounts
import com.gothwad.accounts.core.GothwadConfig

class AccountsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GothwadAccounts.init(this, GothwadConfig(
            clientId = "gothwad-accounts-android",
            redirectUri = "$packageName:/oauth/callback",
        ))
    }
}
