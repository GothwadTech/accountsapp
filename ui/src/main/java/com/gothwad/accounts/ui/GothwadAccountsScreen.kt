package com.gothwad.accounts.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.AuthFailure
import com.gothwad.accounts.core.GothwadAccounts

@Composable
fun SignInButton(modifier: Modifier = Modifier) {
    val activity = LocalContext.current.activity()
    Button(onClick = { activity?.let { GothwadAccounts.signIn(it) } }, enabled = activity != null, modifier = modifier) {
        Text(stringResource(R.string.gothwad_sign_in))
    }
}

/** Shared host screen; authentication forms remain on the existing website. */
@Composable
fun GothwadAccountsScreen() {
    val state by GothwadAccounts.state.collectAsStateWithLifecycle()
    val error by GothwadAccounts.error.collectAsStateWithLifecycle()
    val activity = LocalContext.current.activity()
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.gothwad_accounts), style = MaterialTheme.typography.headlineLarge)
            when (val current = state) {
                AccountState.Initializing -> CircularProgressIndicator()
                AccountState.SignedOut -> {
                    Text(stringResource(R.string.gothwad_auth_description))
                    SignInButton()
                }
                is AccountState.SignedIn -> {
                    Text(current.account.name ?: current.account.username ?: current.account.userId, style = MaterialTheme.typography.titleLarge)
                    current.account.email?.let { Text(it) }
                    Text(stringResource(R.string.gothwad_choose_account), style = MaterialTheme.typography.titleMedium)
                    current.all.forEach { account ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(account.name ?: account.username ?: account.userId)
                                TextButton(onClick = { GothwadAccounts.switchTo(account.userId) }, enabled = account.userId != current.account.userId) {
                                    Text(stringResource(if (account.userId == current.account.userId) R.string.gothwad_active else R.string.gothwad_switch))
                                }
                            }
                        }
                    }
                    TextButton(onClick = { activity?.let { GothwadAccounts.signIn(it, chooseAccount = true) } }) { Text(stringResource(R.string.gothwad_add_account)) }
                    TextButton(onClick = { activity?.let { GothwadAccounts.manageOnWeb(it) } }) { Text(stringResource(R.string.gothwad_manage_web)) }
                    Text(stringResource(R.string.gothwad_local_removal_notice), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { GothwadAccounts.signOut(current.account.userId) }) { Text(stringResource(R.string.gothwad_remove_local)) }
                }
            }
            error?.let {
                Text(stringResource(when (it) {
                    AuthFailure.BrowserUnavailable -> R.string.gothwad_browser_error
                    AuthFailure.InvalidCallback -> R.string.gothwad_callback_error
                    AuthFailure.LoginCancelled -> R.string.gothwad_cancelled
                    AuthFailure.SessionExpired -> R.string.gothwad_expired
                    AuthFailure.StorageUnavailable -> R.string.gothwad_storage_error
                    AuthFailure.Unavailable -> R.string.gothwad_unavailable
                }), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { GothwadAccounts.clearError() }) { Text(stringResource(R.string.gothwad_dismiss)) }
            }
        }
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
