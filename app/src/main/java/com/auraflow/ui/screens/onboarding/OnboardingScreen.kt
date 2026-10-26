package com.auraflow.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.auraflow.domain.model.DayPart
import com.auraflow.domain.model.Experience
import com.auraflow.domain.model.Goal
import com.auraflow.domain.model.OnboardingAnswers
import com.auraflow.domain.plan.PlanGenerator
import com.auraflow.ui.components.GlassCard
import com.auraflow.ui.theme.NeonCyan
import com.auraflow.ui.theme.NeonViolet
import com.auraflow.ui.theme.Outline
import com.auraflow.ui.theme.TextMuted

private class Option(val title: String, val description: String)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onLogin: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onFinished() }
    BackHandler(enabled = state.step > 0) { viewModel.back() }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        StepDots(current = state.step, total = state.totalSteps)
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "onboardingStep",
            modifier = Modifier.weight(1f)
        ) { step ->
            when (step) {
                0 -> QuestionStep(
                    title = "Что сейчас важнее всего?",
                    hint = "От цели зависит, какому этапу практики достанется больше времени.",
                    options = listOf(
                        Option("Снизить стресс", "Больше дыхания с длинным выдохом"),
                        Option("Лучше засыпать", "Сканирование тела появится раньше"),
                        Option("Собраться и сфокусироваться", "Больше времени на объект внимания"),
                        Option("Просто попробовать", "Все этапы поровну")
                    ),
                    selected = state.goal?.ordinal,
                    onSelect = { viewModel.selectGoal(Goal.entries[it]) }
                )
                1 -> QuestionStep(
                    title = "Медитировали раньше?",
                    hint = "Определяет стартовую длительность и то, когда появятся новые этапы.",
                    options = listOf(
                        Option("Никогда", "Начнём с 5 минут"),
                        Option("Иногда", "Начнём с 8 минут"),
                        Option("Регулярно", "Начнём с 10 минут и сразу со всех этапов")
                    ),
                    selected = state.experience?.ordinal,
                    onSelect = { viewModel.selectExperience(Experience.entries[it]) }
                )
                2 -> QuestionStep(
                    title = "Сколько времени готовы уделять?",
                    hint = "Это верхняя граница — к ней план придёт к 7-му дню.",
                    options = MINUTE_OPTIONS.map { Option("$it минут", if (it == 5) "Коротко, но каждый день" else "В день") },
                    selected = state.minutes?.let { MINUTE_OPTIONS.indexOf(it) }?.takeIf { it >= 0 },
                    onSelect = { viewModel.selectMinutes(MINUTE_OPTIONS[it]) }
                )
                3 -> QuestionStep(
                    title = "Когда удобнее практиковать?",
                    hint = "В это время можно будет включить напоминание.",
                    options = listOf(
                        Option("Утром", "Около 08:00"),
                        Option("Днём", "Около 13:00"),
                        Option("Вечером", "Около 21:00")
                    ),
                    selected = state.dayPart?.ordinal,
                    onSelect = { viewModel.selectDayPart(DayPart.entries[it]) }
                )
                4 -> state.answers?.let { answers ->
                    PlanStep(answers, rebuild = state.rebuild, busy = state.busy, onConfirm = viewModel::confirmPlan)
                }
                else -> RegistrationStep(state, viewModel)
            }
        }
        if (state.step == 0 && !state.rebuild) {
            TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Уже есть профиль? Войти")
            }
        }
    }
}

@Composable
private fun StepDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            val width by animateDpAsState(if (i == current) 28.dp else 8.dp, label = "dot")
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (i <= current) NeonCyan else Outline)
            )
        }
    }
}

@Composable
private fun QuestionStep(title: String, hint: String, options: List<Option>, selected: Int?, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Spacer(Modifier.height(8.dp))
        options.forEachIndexed { index, option ->
            GlassCard(Modifier.fillMaxWidth(), highlighted = index == selected, onClick = { onSelect(index) }) {
                Text(option.title, style = MaterialTheme.typography.titleMedium)
                Text(option.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@Composable
private fun PlanStep(answers: OnboardingAnswers, rebuild: Boolean, busy: Boolean, onConfirm: () -> Unit) {
    val plan = PlanGenerator.generate(answers)
    val reasons = PlanGenerator.explain(answers)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ваш план на 7 дней", style = MaterialTheme.typography.headlineLarge)
            for (reason in reasons) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .padding(top = 8.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NeonViolet)
                    )
                    Text(reason, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            Spacer(Modifier.height(4.dp))
            for (day in plan) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("День ${day.day}", style = MaterialTheme.typography.labelLarge, color = NeonCyan, modifier = Modifier.weight(1f))
                        Text("${day.minutes} мин", style = MaterialTheme.typography.labelLarge, color = TextMuted)
                    }
                    Text(day.title, style = MaterialTheme.typography.titleMedium)
                    Text(day.stages.joinToString(" · ") { it.stage.title }, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onConfirm, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(if (rebuild) "Сохранить план" else "Продолжить")
        }
    }
}

@Composable
private fun RegistrationStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Создайте профиль", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Данные хранятся только на этом устройстве. Пароль сохраняется в виде хеша и защищает вход в приложение.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Field("Как к вам обращаться", state.name, state.nameError, viewModel::onNameChange, KeyboardType.Text)
        Field("Email", state.email, state.emailError, viewModel::onEmailChange, KeyboardType.Email)
        Field("Пароль", state.password, state.passwordError, viewModel::onPasswordChange, KeyboardType.Password, secret = true)
        Field(
            "Повторите пароль",
            state.confirmation,
            state.confirmationError,
            viewModel::onConfirmationChange,
            KeyboardType.Password,
            secret = true,
            imeAction = ImeAction.Done
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::register, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (state.busy) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Создать профиль")
            }
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    error: String?,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType,
    secret: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    )
}

private val MINUTE_OPTIONS = listOf(5, 10, 15)
