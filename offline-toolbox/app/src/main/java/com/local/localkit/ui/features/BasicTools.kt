package com.local.localkit.ui.features

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.local.localkit.core.*
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

private val numberFormat = DecimalFormat("0.############")

@Composable
fun CalculatorScreen() {
    var expression by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }
    val keys = listOf("sin(", "cos(", "sqrt(", "^", "7", "8", "9", "÷", "4", "5", "6", "×", "1", "2", "3", "-", "0", ".", "(", ")")

    ToolPage("Calculate locally", "Supports +, −, ×, ÷, %, powers, parentheses and common functions.") {
        OutlinedTextField(
            value = expression,
            onValueChange = { expression = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
            placeholder = { Text("12 × (3 + 4)") },
            supportingText = { if (result.isNotBlank()) Text("= $result", style = MaterialTheme.typography.titleLarge) },
            minLines = 2
        )
        keys.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    OutlinedButton(onClick = { expression += key }, modifier = Modifier.weight(1f)) { Text(key) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { expression = ""; result = "" }, modifier = Modifier.weight(1f)) { Text("Clear") }
            OutlinedButton(onClick = { expression = expression.dropLast(1) }, modifier = Modifier.weight(1f)) { Text("Delete") }
            Button(onClick = {
                result = runCatching { numberFormat.format(ArithmeticEngine.evaluate(expression)) }.getOrElse { "Check expression" }
            }, modifier = Modifier.weight(2f)) { Text("=") }
        }
    }
}

@Composable
fun UnitConverterScreen() {
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var fromIndex by rememberSaveable { mutableIntStateOf(0) }
    var toIndex by rememberSaveable { mutableIntStateOf(1) }
    var input by rememberSaveable { mutableStateOf("1") }
    val category = UnitConversions.categories[categoryIndex]
    val value = input.toDoubleOrNull()
    val output = value?.let { numberFormat.format(UnitConversions.convert(it, category.units[fromIndex], category.units[toIndex])) }.orEmpty()

    ToolPage("Convert without a server", "Length, mass, temperature, data, volume and speed.") {
        ChoiceDropdown("Category", UnitConversions.categories.map { it.name }, categoryIndex) {
            categoryIndex = it; fromIndex = 0; toIndex = 1
        }
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { ChoiceDropdown("From", category.units.map { it.symbol }, fromIndex) { fromIndex = it } }
            Box(Modifier.weight(1f)) { ChoiceDropdown("To", category.units.map { it.symbol }, toIndex) { toIndex = it } }
        }
        ResultCard(if (output.isBlank()) "Enter a number" else "$output ${category.units[toIndex].symbol}")
    }
}

@Composable
fun DateCalculatorScreen() {
    var first by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var second by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var shift by rememberSaveable { mutableStateOf("30") }
    val firstDate = runCatching { LocalDate.parse(first) }.getOrNull()
    val secondDate = runCatching { LocalDate.parse(second) }.getOrNull()

    ToolPage("Date math", "Use ISO dates (YYYY-MM-DD). Nothing is uploaded or added to a calendar.") {
        OutlinedTextField(first, { first = it }, Modifier.fillMaxWidth(), label = { Text("Start date") }, singleLine = true)
        OutlinedTextField(second, { second = it }, Modifier.fillMaxWidth(), label = { Text("End date") }, singleLine = true)
        ResultCard(if (firstDate != null && secondDate != null) "${kotlin.math.abs(ChronoUnit.DAYS.between(firstDate, secondDate))} days apart" else "Enter valid dates")
        HorizontalDivider()
        OutlinedTextField(shift, { shift = it }, Modifier.fillMaxWidth(), label = { Text("Days to add (use negative to subtract)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        ResultCard(firstDate?.let { date -> shift.toLongOrNull()?.let { "$date → ${date.plusDays(it)}" } } ?: "Enter a valid shift")
    }
}

@Composable
fun TextWorkbenchScreen() {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var find by rememberSaveable { mutableStateOf("") }
    var replace by rememberSaveable { mutableStateOf("") }
    val stats = TextOperations.stats(text)

    ToolPage("Private text workbench", "Paste deliberately; this app never monitors your clipboard.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { text = readClipboard(context) }, modifier = Modifier.weight(1f)) { Text("Paste") }
            OutlinedButton(onClick = { copy(context, text) }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy") }
            OutlinedButton(onClick = { text = "" }, modifier = Modifier.weight(1f)) { Text("Clear") }
        }
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 220.dp), label = { Text("Text") }, minLines = 8)
        Text("${stats.first} words  ·  ${stats.second} characters  ·  ${stats.third} lines", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowButtonRow(listOf(
            "UPPER" to { text = text.uppercase() },
            "lower" to { text = text.lowercase() },
            "Title Case" to { text = TextOperations.titleCase(text) },
            "Sort lines" to { text = TextOperations.sortLines(text) },
            "Dedupe" to { text = TextOperations.dedupeLines(text) },
            "Clean spaces" to { text = TextOperations.compactWhitespace(text) }
        ))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(find, { find = it }, Modifier.weight(1f), label = { Text("Find") }, singleLine = true)
            OutlinedTextField(replace, { replace = it }, Modifier.weight(1f), label = { Text("Replace") }, singleLine = true)
        }
        Button(onClick = { if (find.isNotEmpty()) text = text.replace(find, replace) }, enabled = find.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("Replace all") }
    }
}

@Composable
fun PasswordGeneratorScreen() {
    val context = LocalContext.current
    var length by rememberSaveable { mutableFloatStateOf(20f) }
    var symbols by rememberSaveable { mutableStateOf(true) }
    var passphrase by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf(SecretGenerator.password(20, true)) }

    fun regenerate() { result = if (passphrase) SecretGenerator.passphrase((length / 4).toInt().coerceIn(3, 8)) else SecretGenerator.password(length.toInt(), symbols) }

    ToolPage("Generate, copy, forget", "Generated secrets are never saved in history or preferences.") {
        ResultCard(result, monospace = true)
        Button(onClick = { copy(context, result) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copy") }
        Text(if (passphrase) "Words: ${(length / 4).toInt().coerceIn(3, 8)}" else "Length: ${length.toInt()}")
        Slider(value = length, onValueChange = { length = it }, valueRange = 12f..64f, steps = 51)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(passphrase, { passphrase = it; regenerate() }); Text("Passphrase")
            Spacer(Modifier.weight(1f))
            Checkbox(symbols, { symbols = it; regenerate() }, enabled = !passphrase); Text("Symbols")
        }
        FilledTonalButton(onClick = { regenerate() }, modifier = Modifier.fillMaxWidth()) { Text("Generate another") }
    }
}

@Composable
fun ToolPage(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ResultCard(value: String, monospace: Boolean = false) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(value, Modifier.padding(18.dp), style = MaterialTheme.typography.titleLarge.copy(fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChoiceDropdown(label: String, choices: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${choices.getOrElse(selected) { "—" }}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            choices.forEachIndexed { index, choice -> DropdownMenuItem(text = { Text(choice) }, onClick = { onSelected(index); open = false }) }
        }
    }
}

@Composable
private fun FlowButtonRow(items: List<Pair<String, () -> Unit>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item -> OutlinedButton(onClick = item.second, modifier = Modifier.weight(1f), contentPadding = PaddingValues(6.dp)) { Text(item.first) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun copy(context: Context, value: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("LocalKit", value))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun readClipboard(context: Context): String = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
    .primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
