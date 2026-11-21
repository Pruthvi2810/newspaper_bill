package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InkNavy
import com.example.ui.theme.Terracotta
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun QuickCalendarCard(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    headerDragModifier: Modifier = Modifier
) {
    val todayCal = remember { Calendar.getInstance() }
    var displayedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf<Int?>(todayCal.get(Calendar.DAY_OF_MONTH)) }

    val monthCal = remember(displayedYear, displayedMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday .. 7 = Saturday
    val leadingEmptyCells = firstDayOfWeek - 1

    val sundayCount = remember(displayedYear, displayedMonth) {
        var count = 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
        }
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                count++
            }
        }
        count
    }

    val monthTitle = remember(displayedYear, displayedMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
    }

    val isCurrentMonth =
        displayedYear == todayCal.get(Calendar.YEAR) && displayedMonth == todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, InkNavy.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Draggable Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(headerDragModifier),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag Calendar",
                        tint = InkNavy.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Quick Calendar",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = InkNavy
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isCurrentMonth) {
                        TextButton(
                            onClick = {
                                displayedYear = todayCal.get(Calendar.YEAR)
                                displayedMonth = todayCal.get(Calendar.MONTH)
                                selectedDay = todayDay
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Calendar", tint = InkNavy)
                    }
                }
            }

            // Month Switcher Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEEF3FA),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (displayedMonth == 0) {
                                displayedMonth = 11
                                displayedYear--
                            } else {
                                displayedMonth--
                            }
                            selectedDay = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = InkNavy
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkNavy
                        )
                        Text(
                            text = "$daysInMonth Days • $sundayCount Sundays",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF5A6270)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (displayedMonth == 11) {
                                displayedMonth = 0
                                displayedYear++
                            } else {
                                displayedMonth++
                            }
                            selectedDay = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = InkNavy
                        )
                    }
                }
            }

            // Day of Week Headers
            val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEachIndexed { idx, label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (idx == 0) Terracotta else Color(0xFF5A6270)
                    )
                }
            }

            // Calendar Grid
            val totalCells = leadingEmptyCells + daysInMonth
            val totalRows = (totalCells + 6) / 7
            for (rowIndex in 0 until totalRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (colIndex in 0 until 7) {
                        val cellIndex = rowIndex * 7 + colIndex
                        val dayNum = cellIndex - leadingEmptyCells + 1
                        if (dayNum in 1..daysInMonth) {
                            val isToday = isCurrentMonth && dayNum == todayDay
                            val isSelected = selectedDay == dayNum
                            val isSunday = colIndex == 0
                            val bgColor = when {
                                isSelected -> InkNavy
                                isToday -> Terracotta.copy(alpha = 0.16f)
                                else -> Color.Transparent
                            }
                            val textColor = when {
                                isSelected -> Color.White
                                isSunday -> Terracotta
                                isToday -> InkNavy
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .clickable { selectedDay = dayNum },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
