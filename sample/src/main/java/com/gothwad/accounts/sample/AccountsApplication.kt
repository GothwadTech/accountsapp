package com.gothwad.accounts.sample

import android.app.Application
import com.gothwad.accounts.core.GothwadAccounts
import com.gothwad.accounts.core.GothwadConfig

class AccountsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GothwadAccounts.init(this, GothwadConfig(
            clientId = "gothwad-accounts-sample-${BuildConfig.FLAVOR}",
            redirectUri = "$packageName:/oauth/callback",
        ))
    }
}
