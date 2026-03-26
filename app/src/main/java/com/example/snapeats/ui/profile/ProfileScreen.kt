package com.example.snapeats.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snapeats.domain.usecase.GoalAdjustment
import com.example.snapeats.ui.components.InputStepper

// ── Activity level descriptor ────────────────────────────────────────────────

private data class ActivityOption(
    val label: String,
    val description: String,
    val factor: Float
)

private val ACTIVITY_OPTIONS = listOf(
    ActivityOption("Sedentary",        "Desk job, little or no exercise",            1.2f),
    ActivityOption("Lightly Active",   "Light exercise 1–3 days / week",             1.375f),
    ActivityOption("Moderately Active","Moderate exercise 3–5 days / week",          1.55f),
    ActivityOption("Very Active",      "Hard exercise 6–7 days / week",              1.725f),
    ActivityOption("Extra Active",     "Very hard exercise + physical job",           1.9f)
)

// ── Screen ───────────────────────────────────────────────────────────────────

/**
 * Profile / Onboarding screen.
 *
 * When [isOnboarding] is true the top-bar title reads "Welcome to SnapEats" and
 * navigation to Home is triggered on the very first successful save.
 * When false (returning user editing their profile) the title reads "My Profile".
 *
 * @param viewModel      Injected [ProfileViewModel].
 * @param isOnboarding   True when no User record exists yet.
 * @param onSaveSuccess  Called after a successful save — navigate to Home from here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    isOnboarding: Boolean,
    onSaveSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Pre-fill form from existing user record ──────────────────────────────
    val existingUser = uiState.user

    var heightCm        by rememberSaveable { mutableFloatStateOf(existingUser?.height        ?: 170f) }
    var weightKg        by rememberSaveable { mutableFloatStateOf(existingUser?.weight        ?: 70f)  }
    var age             by rememberSaveable { mutableIntStateOf(existingUser?.age             ?: 25)   }
    var isMale          by rememberSaveable { mutableStateOf(existingUser?.isMale             ?: true) }
    var activityFactor  by rememberSaveable { mutableFloatStateOf(existingUser?.activityFactor ?: 1.2f) }

    // Keep form in sync if the DB value loads after initial composition
    LaunchedEffect(existingUser) {
        if (existingUser != null) {
            heightCm       = existingUser.height
            weightKg       = existingUser.weight
            age            = existingUser.age
            isMale         = existingUser.isMale
            activityFactor = existingUser.activityFactor
        }
    }

    // ── Field validation ─────────────────────────────────────────────────────
    val heightError = when {
        heightCm < 50f  -> "Minimum 50 cm"
        heightCm > 250f -> "Maximum 250 cm"
        else            -> null
    }
    val weightError = when {
        weightKg < 20f  -> "Minimum 20 kg"
        weightKg > 300f -> "Maximum 300 kg"
        else            -> null
    }
    val ageError = when {
        age < 5   -> "Minimum 5 years"
        age > 120 -> "Maximum 120 years"
        else      -> null
    }
    val isFormValid = heightError == null && weightError == null && ageError == null

    // ── Side-effects ─────────────────────────────────────────────────────────
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            viewModel.clearSaveSuccess()
            onSaveSuccess()
        }
    }

    // ── Layout ───────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isOnboarding) "Welcome to SnapEats" else "My Profile"
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        if (uiState.isLoading && existingUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── BMI Result Card ──────────────────────────────────────────────
            uiState.bmiResult?.let { result ->
                BmiCard(
                    bmi      = result.bmi,
                    category = result.category,
                    color    = result.color
                )
            }

            // ── Measurements ─────────────────────────────────────────────────
            SectionLabel("Body Measurements")

            InputStepper(
                value         = heightCm,
                onValueChange = { heightCm = it },
                min           = 50f,
                max           = 250f,
                step          = 0.5f,
                label         = "Height",
                unit          = " cm"
            )
            heightError?.let { FieldError(it) }

            InputStepper(
                value         = weightKg,
                onValueChange = { weightKg = it },
                min           = 20f,
                max           = 300f,
                step          = 0.5f,
                label         = "Weight",
                unit          = " kg"
            )
            weightError?.let { FieldError(it) }

            InputStepper(
                value         = age.toFloat(),
                onValueChange = { age = it.toInt() },
                min           = 5f,
                max           = 120f,
                step          = 1f,
                label         = "Age",
                unit          = " yrs"
            )
            ageError?.let { FieldError(it) }

            // ── Biological Sex ───────────────────────────────────────────────
            HorizontalDivider()
            SectionLabel("Biological Sex")

            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(true to "Male", false to "Female").forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = isMale == value,
                            onClick  = { isMale = value }
                        )
                        Text(
                            text  = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // ── Activity Level ───────────────────────────────────────────────
            HorizontalDivider()
            SectionLabel("Activity Level")

            ACTIVITY_OPTIONS.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = activityFactor == option.factor,
                        onClick  = { activityFactor = option.factor }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text  = option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Save Button ──────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.saveProfile(
                        heightCm       = heightCm,
                        weightKg       = weightKg,
                        age            = age,
                        isMale         = isMale,
                        activityFactor = activityFactor,
                        goal           = GoalAdjustment.MAINTAIN
                    )
                },
                enabled  = isFormValid && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color     = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isOnboarding) "Get Started" else "Save Profile")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Private sub-composables ───────────────────────────────────────────────────

@Composable
private fun BmiCard(
    bmi: Float,
    category: String,
    color: Color
) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 600),
        label = "bmi_card_color"
    )

    // Choose a text colour that is legible against the card background
    val contentColor = if (animatedColor.luminance() > 0.4f) Color.Black else Color.White

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = animatedColor)
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text  = "Your BMI",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text       = "%.2f".format(bmi),
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color      = contentColor
            )
            Text(
                text  = category,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun FieldError(message: String) {
    Text(
        text     = message,
        style    = MaterialTheme.typography.bodySmall,
        color    = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp)
    )
}
