package com.auraflow.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.ui.theme.ErrorSoft
import com.auraflow.ui.theme.TextMuted

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onCreateProfile: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onLoggedIn() }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text("С возвращением", style = MaterialTheme.typography.headlineLarge)
        Text("Войдите, чтобы продолжить свой план.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        Field("Email", state.email, null, viewModel::onEmailChange, KeyboardType.Email)
        Field("Пароль", state.password, null, viewModel::onPasswordChange, KeyboardType.Password, secret = true, imeAction = ImeAction.Done)
        state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ErrorSoft) }
        Button(onClick = viewModel::login, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (state.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Войти")
        }
        TextButton(onClick = onCreateProfile, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Создать новый профиль")
        }
    }
}
