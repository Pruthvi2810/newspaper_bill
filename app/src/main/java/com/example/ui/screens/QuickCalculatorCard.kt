package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InkNavy
import com.example.ui.theme.Terracotta

@Composable
fun QuickCalculatorCard(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var display by remember { mutableStateOf("0") }
    var firstOperand by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var clearOnNextDigit by remember { mutableStateOf(false) }

    fun formatResult(value: Double): String {
        return if (value % 1.0 == 0.0 && value in -1e12..1e12) {
            value.toLong().toString()
        } else {
            String.format("%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun evaluate(a: Double, b: Double, op: String): Double {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b != 0.0) a / b else 0.0
            else -> b
        }
    }

    fun onKey(key: String) {
        when (key) {
            "C" -> {
                display = "0"
                firstOperand = null
                pendingOp = null
                clearOnNextDigit = false
            }
            "⌫" -> {
                if (clearOnNextDigit) {
                    display = "0"
                    clearOnNextDigit = false
                } else {
                    display = if (display.length > 1) display.dropLast(1) else "0"
                }
            }
            "+", "-", "×", "÷" -> {
                val current = display.toDoubleOrNull() ?: 0.0
                if (firstOperand != null && pendingOp != null && !clearOnNextDigit) {
                    val res = evaluate(firstOperand!!, current, pendingOp!!)
                    display = formatResult(res)
                    firstOperand = res
                } else {
                    firstOperand = current
                }
                pendingOp = key
                clearOnNextDigit = true
            }
            "=" -> {
                val current = display.toDoubleOrNull() ?: 0.0
                if (firstOperand != null && pendingOp != null) {
                    val res = evaluate(firstOperand!!, current, pendingOp!!)
                    display = formatResult(res)
                    firstOperand = null
                    pendingOp = null
                    clearOnNextDigit = true
                }
            }
            "." -> {
                if (clearOnNextDigit) {
                    display = "0."
                    clearOnNextDigit = false
                } else if (!display.contains(".")) {
                    display += "."
                }
            }
            else -> {
                if (clearOnNextDigit || display == "0") {
                    display = key
                    clearOnNextDigit = false
                } else if (display.length < 12) {
                    display += key
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, InkNavy.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Calculator",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkNavy
                )
                IconButton(onClick = onClose, modifier = Modifier.height(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Calculator", tint = InkNavy)
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEEF3FA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (firstOperand != null && pendingOp != null) {
                        Text(
                            text = "${formatResult(firstOperand!!)} $pendingOp",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF5A6270)
                        )
                    }
                    Text(
                        text = display,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkNavy,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
            }

            val rows = listOf(
                listOf("C", "⌫", "÷", "×"),
                listOf("7", "8", "9", "-"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "="),
                listOf("0", "00", ".")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { key ->
                        val isOperator = key in listOf("+", "-", "×", "÷")
                        val isEquals = key == "="
                        val isClear = key == "C" || key == "⌫"
                        val containerColor = when {
                            isEquals -> Terracotta
                            isOperator -> InkNavy
                            isClear -> Color(0xFFE4E9F2)
                            else -> Color(0xFFF6F8FC)
                        }
                        val contentColor = when {
                            isEquals || isOperator -> Color.White
                            else -> InkNavy
                        }
                        Button(
                            onClick = { onKey(key) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = containerColor,
                                contentColor = contentColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .weight(if (key == "0") 2f else 1f)
                                .height(42.dp)
                        ) {
                            Text(text = key, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
