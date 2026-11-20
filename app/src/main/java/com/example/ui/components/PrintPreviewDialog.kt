package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BillWithItems
import com.example.ui.theme.InkNavy
import com.example.util.InvoicePdfGenerator
import com.example.util.PageOrientation
import com.example.util.PdfPrintOptions
import kotlin.math.roundToInt

private data class MarginPreset(
    val label: String,
    val points: Float,
    val testTag: String
)

private val MarginPresets = listOf(
    MarginPreset("Narrow", 16f, "print_margin_narrow"),
    MarginPreset("Normal", 24f, "print_margin_normal"),
    MarginPreset("Wide", 44f, "print_margin_wide")
)

@Composable
fun PrintPreviewDialog(
    billWithItems: BillWithItems,
    initialOptions: PdfPrintOptions = PdfPrintOptions(),
    onDismiss: () -> Unit,
    onConfirmPrint: (PdfPrintOptions) -> Unit
) {
    var options by remember(billWithItems.bill.id) { mutableStateOf(initialOptions) }

    val previewBitmap = remember(billWithItems, options) {
        InvoicePdfGenerator.renderPreviewBitmap(
            billWithItems = billWithItems,
            options = options,
            maxDimensionPx = 840
        )
    }
    val previewImage = remember(previewBitmap) { previewBitmap.asImageBitmap() }
    val pageAspectRatio = options.pageWidth.toFloat() / options.pageHeight.toFloat()
    val marginMm = (options.clampedMargin * 25.4f / 72f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Print Preview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = InkNavy
                        )
                        Text(
                            text = "A4 • ${if (options.orientation == PageOrientation.PORTRAIT) "Portrait" else "Landscape"} • ${options.clampedMargin.roundToInt()} pt margin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("print_preview_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Print Preview"
                        )
                    }
                }

                // Live Scaled A4 Page Preview Stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 210.dp, max = 310.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = previewImage,
                        contentDescription = "Live PDF Print Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .aspectRatio(pageAspectRatio)
                            .shadow(6.dp, RoundedCornerShape(3.dp))
                            .background(Color.White, RoundedCornerShape(3.dp))
                            .border(0.5.dp, Color(0xFF94A3B8), RoundedCornerShape(3.dp))
                    )
                }

                // Orientation Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Page Orientation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = InkNavy
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = options.orientation == PageOrientation.PORTRAIT,
                            onClick = {
                                options = options.copy(orientation = PageOrientation.PORTRAIT)
                            },
                            label = { Text("Portrait") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CropPortrait,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InkNavy,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("print_orientation_portrait")
                        )
                        FilterChip(
                            selected = options.orientation == PageOrientation.LANDSCAPE,
                            onClick = {
                                options = options.copy(orientation = PageOrientation.LANDSCAPE)
                            },
                            label = { Text("Landscape") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CropLandscape,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InkNavy,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("print_orientation_landscape")
                        )
                    }
                }

                // Page Margins Control
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page Margins",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = InkNavy
                        )
                        Text(
                            text = "${options.clampedMargin.roundToInt()} pt (~${"%.1f".format(marginMm)} mm)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MarginPresets.forEach { preset ->
                            val isSelected = kotlin.math.abs(options.clampedMargin - preset.points) < 1f
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    options = options.copy(marginPoints = preset.points)
                                },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InkNavy,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(preset.testTag)
                            )
                        }
                    }

                    Slider(
                        value = options.clampedMargin,
                        onValueChange = { newMargin ->
                            options = options.copy(marginPoints = newMargin.roundToInt().toFloat())
                        },
                        valueRange = 12f..60f,
                        colors = SliderDefaults.colors(
                            thumbColor = InkNavy,
                            activeTrackColor = InkNavy
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("print_margin_slider")
                    )
                }

                HorizontalDivider()

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_preview_cancel_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { onConfirmPrint(options) },
                        colors = ButtonDefaults.buttonColors(containerColor = InkNavy),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("print_preview_confirm_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
