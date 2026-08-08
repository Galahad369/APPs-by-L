package com.satire.uselesscalculator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

internal const val MONTHLY_PRICE = "$29.99"

private enum class OnboardingStage { TERMS, PERMISSIONS, WALLET_PARODY, CALCULATOR }

private data class PermissionDemand(
    val permission: String,
    val title: String,
    val excuse: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UselessCalculatorApp(onQuit = ::finishAndRemoveTask)
        }
    }
}

@Composable
private fun UselessCalculatorApp(onQuit: () -> Unit) {
    var stage by rememberSaveable { mutableStateOf(OnboardingStage.TERMS) }
    BackHandler(onBack = onQuit)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFFD60A),
            onPrimary = Color(0xFF111111),
            secondary = Color(0xFFFF4D6D),
            background = Color(0xFF061A40),
            surface = Color(0xFF102A56),
            surfaceVariant = Color(0xFF173B70),
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCED8EA),
        ),
    ) {
        Surface(Modifier.fillMaxSize()) {
            when (stage) {
                OnboardingStage.TERMS -> TermsScreen(
                    onAccept = { stage = OnboardingStage.PERMISSIONS },
                    onDecline = onQuit,
                )
                OnboardingStage.PERMISSIONS -> PermissionGauntlet(
                    onComplete = { stage = OnboardingStage.WALLET_PARODY },
                    onDenied = onQuit,
                )
                OnboardingStage.WALLET_PARODY -> WalletPasswordParody(
                    onContinue = { stage = OnboardingStage.CALCULATOR },
                    onDecline = onQuit,
                )
                OnboardingStage.CALCULATOR -> CalculatorScreen()
            }
        }
    }
}

@Composable
private fun TermsScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    val scroll = rememberScrollState()
    var readEverything by rememberSaveable { mutableStateOf(false) }
    var waiveCommonSense by rememberSaveable { mutableStateOf(false) }
    var acceptPremiumMath by rememberSaveable { mutableStateOf(false) }
    val reachedBottom by remember {
        derivedStateOf { scroll.maxValue > 0 && scroll.value >= scroll.maxValue - 4 }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.background(Color(0xFFD00000)).padding(18.dp)) {
            Text("MANDATORY TERMS", fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Zuckerberg-tier legal endurance test · no skipping", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = {
                if (scroll.maxValue == 0) 0f else scroll.value.toFloat() / scroll.maxValue
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(scroll).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0B14))) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFFD60A))
                    Text(
                        "SATIRE: these terms have no legal purpose. The app stores nothing, uses no network, and exists to mock hostile onboarding.",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            termsClauses.forEachIndexed { index, clause ->
                Text(
                    text = "${index + 1}.${(index * 37 + 11) % 1000}  $clause",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
            Text("FINAL BINDING CHECKBOX CEREMONY", fontWeight = FontWeight.Black, color = Color(0xFFFFD60A))
            MandatoryCheck(
                "I solemnly claim I read every ridiculous clause.",
                readEverything,
            ) { readEverything = it }
            MandatoryCheck(
                "I waive my right to ask why a calculator needs any permission.",
                waiveCommonSense,
            ) { waiveCommonSense = it }
            MandatoryCheck(
                "I understand arithmetic may be a premium feature.",
                acceptPremiumMath,
            ) { acceptPremiumMath = it }
            Button(
                onClick = onAccept,
                enabled = reachedBottom && readEverything && waiveCommonSense && acceptPremiumMath,
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) {
                Text("ACCEPT EVERYTHING FOREVER", fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
                Text("Decline, erase progress, and quit")
            }
            Text(
                if (reachedBottom) "You somehow reached the bottom." else "Keep scrolling. Freedom is disabled.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MandatoryCheck(text: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PermissionGauntlet(onComplete: () -> Unit, onDenied: () -> Unit) {
    val context = LocalContext.current
    val demands = remember { permissionDemands() }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) index++ else onDenied()
    }

    LaunchedEffect(index) {
        if (index >= demands.size) {
            onComplete()
        } else if (ContextCompat.checkSelfPermission(context, demands[index].permission) == PackageManager.PERMISSION_GRANTED) {
            index++
        }
    }

    if (index >= demands.size) return
    val demand = demands[index]
    Column(
        modifier = Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("PERMISSION ${index + 1} OF ${demands.size}", color = Color(0xFFFFD60A), fontWeight = FontWeight.Black)
            LinearProgressIndicator(
                progress = { (index + 1f) / demands.size },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = Color(0xFFFF4D6D),
            )
            Spacer(Modifier.height(20.dp))
            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(74.dp), tint = Color(0xFFFFD60A))
            Text(demand.title, fontSize = 32.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
            Text(demand.excuse, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0B14))) {
                Text(
                    "This permission is not used. The request is the joke. Denying it immediately quits and resets this onboarding circus.",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { launcher.launch(demand.permission) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
            ) {
                Text("REQUEST ${demand.title.uppercase()}", fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = onDenied, modifier = Modifier.fillMaxWidth()) {
                Text("Deny and lose all progress")
            }
        }
    }
}

@Composable
private fun WalletPasswordParody(onContinue: () -> Unit, onDecline: () -> Unit) {
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("FINAL PRIVACY INVASION", color = Color(0xFFFFD60A), fontWeight = FontWeight.Black)
            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(86.dp), tint = Color(0xFFFF4D6D))
            Text("Bitcoin wallet password", fontSize = 34.sp, lineHeight = 37.sp, fontWeight = FontWeight.Black)
            Text("A truly terrible calculator would ask for it here.", fontSize = 19.sp)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PASSWORD / SEED PHRASE", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        "[INPUT DISABLED — NEVER ENTER REAL WALLET SECRETS]",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD60A),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                "This app cannot accept, read, store, or transmit a password. This screen is a safety warning disguised as ragebait.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MandatoryCheck(
                "I understand legitimate apps should never request wallet passwords or seed phrases.",
                acknowledged,
            ) { acknowledged = it }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onContinue,
                enabled = acknowledged,
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) { Text("CONTINUE TO THE ‘CALCULATOR’", fontWeight = FontWeight.Black) }
            OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
                Text("Protect my dignity and quit")
            }
        }
    }
}

@Composable
private fun CalculatorScreen() {
    var display by rememberSaveable { mutableStateOf("") }
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    var paymentFailed by rememberSaveable { mutableStateOf(false) }
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "−"),
        listOf("C", "0", ".", "+"),
    )

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Calculate, contentDescription = null, tint = Color(0xFFFFD60A))
            Column {
                Text("Calculator FREE", fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("0 calculations remaining", color = Color(0xFFFF4D6D), fontWeight = FontWeight.Bold)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().height(128.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF02060F)),
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
                Text(
                    text = display.ifBlank { "0" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 38.sp,
                    maxLines = 2,
                    textAlign = TextAlign.End,
                )
            }
        }
        Text("Typing is free. Answers require Calculator Premium™.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        rows.forEach { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { token ->
                    Button(
                        onClick = { display = appendCalculatorInput(display, token) },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (token == "C") Color(0xFFD00000) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = Color.White,
                        ),
                    ) { Text(token, fontSize = 25.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        Button(
            onClick = { showPaywall = true },
            modifier = Modifier.fillMaxWidth().height(68.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD60A)),
        ) {
            Text("=", fontSize = 34.sp, fontWeight = FontWeight.Black)
        }
    }

    if (showPaywall) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Unlock the equals sign") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CALCULATOR PREMIUM", color = Color(0xFFFF4D6D), fontWeight = FontWeight.Black)
                    Text("$MONTHLY_PRICE / month", fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("Includes up to one answer per billing cycle. Operators sold separately. Auto-renews emotionally.")
                    Text("SATIRE ONLY — no billing SDK or real purchase exists.", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPaywall = false
                    paymentFailed = true
                }) { Text("SUBSCRIBE FOR $MONTHLY_PRICE") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPaywall = false }) { Text("Remain answerless") }
            },
        )
    }

    if (paymentFailed) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Payment failed successfully") },
            text = { Text("Billing is unavailable because this satire app refuses to collect money. The answer remains classified.") },
            confirmButton = {
                Button(onClick = {
                    paymentFailed = false
                    showPaywall = true
                }) { Text("Return to paywall") }
            },
        )
    }
}

internal fun appendCalculatorInput(current: String, token: String): String {
    if (token == "C") return ""
    return (current + token).takeLast(32)
}

private fun permissionDemands(): List<PermissionDemand> = buildList {
    add(PermissionDemand(Manifest.permission.CAMERA, "Camera access", "Required to photograph numbers you could type yourself."))
    add(PermissionDemand(Manifest.permission.RECORD_AUDIO, "Microphone access", "Required to hear you sigh when the calculator still refuses to calculate."))
    add(PermissionDemand(Manifest.permission.READ_CONTACTS, "Contact access", "Required to identify friends who might lend you a real calculator."))
    add(PermissionDemand(Manifest.permission.READ_CALENDAR, "Calendar access", "Required to schedule your future subscription regret."))
    add(PermissionDemand(Manifest.permission.ACCESS_FINE_LOCATION, "Precise location", "Required to determine whether arithmetic is legal in your postcode."))
    add(PermissionDemand(Manifest.permission.READ_PHONE_STATE, "Phone access", "Required to confirm this rectangular object is allegedly a phone."))
    add(PermissionDemand(Manifest.permission.READ_CALL_LOG, "Call-log access", "Required to count how often you called someone better at mathematics."))
    add(PermissionDemand(Manifest.permission.BODY_SENSORS, "Body-sensor access", "Required to measure your rising blood pressure."))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(PermissionDemand(Manifest.permission.ACTIVITY_RECOGNITION, "Activity access", "Required to detect the exact moment you walk away."))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(PermissionDemand(Manifest.permission.READ_MEDIA_IMAGES, "Photo access", "Required to search for screenshots of useful calculators."))
        add(PermissionDemand(Manifest.permission.READ_MEDIA_VIDEO, "Video access", "Required to study tutorials about pressing equals."))
        add(PermissionDemand(Manifest.permission.READ_MEDIA_AUDIO, "Music access", "Required to play nothing while you wait for an answer."))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(PermissionDemand(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, "Selected-photo access", "Required to respect your careful selection by reading absolutely none of it."))
        }
    } else {
        add(PermissionDemand(Manifest.permission.READ_EXTERNAL_STORAGE, "File access", "Required to inspect files that have absolutely nothing to do with arithmetic."))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(PermissionDemand(Manifest.permission.BLUETOOTH_SCAN, "Nearby-device scan", "Required to locate a calculator that actually works."))
        add(PermissionDemand(Manifest.permission.BLUETOOTH_CONNECT, "Nearby-device connection", "Required to refuse to connect to that calculator."))
    }
}

private val termsClauses: List<String> = buildList {
    add("These Terms are parody, confer no meaningful rights, and are deliberately longer than the software deserves.")
    add("The word Calculator may refer to a screen containing buttons that resemble arithmetic controls without producing arithmetic.")
    repeat(72) { index ->
        add(
            when (index % 8) {
                0 -> "You grant the Calculator permission to contemplate the philosophical concept of the number ${index + 1}."
                1 -> "You acknowledge that scrolling is not productivity, even when accompanied by serious-looking legal numbering."
                2 -> "The Calculator reserves the right to move an imaginary comma without notice, purpose, or competence."
                3 -> "Any resemblance to a reasonable privacy policy is accidental and should be reported to absolutely nobody."
                4 -> "Arithmetic availability may vary by mood, battery percentage, lunar phase, and fictional shareholder expectations."
                5 -> "You agree not to reverse-engineer why a calculator wants access to anything beyond the buttons on its own screen."
                6 -> "Premium mathematical symbols may be renamed, rearranged, emotionally deprecated, or placed behind another dialog."
                else -> "By continuing, you certify that clause ${index + 3} was definitely read rather than rapidly flicked past."
            }
        )
    }
    add("No password, seed phrase, payment detail, contact, recording, image, location, or other private data is collected by this satire app.")
    add("The only sensible term is the final one: never give real credentials to software merely because it asks confidently.")
}
