package app.planlist.local.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.planlist.core.PriorityScale
import app.planlist.local.R
import app.planlist.local.ui.theme.Cinnabar
import app.planlist.local.ui.theme.Ink
import app.planlist.local.ui.theme.InkSoft
import app.planlist.local.ui.theme.Line
import app.planlist.local.ui.theme.PaperCard
import app.planlist.local.ui.theme.priorityColor
import kotlin.math.roundToInt

@Composable
fun MenuButton(
    label: String,
    value: String,
    options: List<String>,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueColor: Color = Ink,
) {
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.cd_dropdown, label, value)
    Box(modifier) {
        OutlinedButton(
            onClick = { if (enabled) open = true },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = description },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            border = BorderStroke(1.dp, if (enabled) Line else Line.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PaperCard,
                contentColor = Ink,
                disabledContainerColor = PaperCard,
                disabledContentColor = InkSoft,
            ),
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) valueColor else InkSoft,
                )
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        OptionMenu(open, options, onDismiss = { open = false }) { index ->
            onSelectIndex(index)
            open = false
        }
    }
}

@Composable
fun NumberMenu(
    label: String,
    value: String,
    options: List<String>,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Box {
            OutlinedButton(
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                border = BorderStroke(1.dp, Line),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PaperCard,
                    contentColor = Ink,
                ),
            ) {
                Text(
                    value,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            OptionMenu(open, options, onDismiss = { open = false }) { index ->
                onSelectIndex(index)
                open = false
            }
        }
    }
}

@Composable
private fun OptionMenu(
    open: Boolean,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    DropdownMenu(
        expanded = open,
        onDismissRequest = onDismiss,
        modifier = Modifier.heightIn(max = 320.dp),
        containerColor = PaperCard,
    ) {
        options.forEachIndexed { index, option ->
            DropdownMenuItem(
                text = { Text(option, color = Ink) },
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
fun PrioritySlider(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
) {
    var position by remember(resetKey) { mutableFloatStateOf(value.toFloat()) }
    val shown = position.roundToInt().coerceIn(PriorityScale.MIN, PriorityScale.MAX)
    val description = stringResource(R.string.cd_priority, shown)
    Column(
        modifier
            .fillMaxWidth()
            .background(PaperCard, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.field_priority),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = shown.toString(),
                color = priorityColor(shown),
                fontWeight = FontWeight.SemiBold,
                fontSize = (20 + shown * 2).sp,
                fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
            )
        }
        Slider(
            value = position,
            onValueChange = {
                position = it
                onChange(it.roundToInt().coerceIn(PriorityScale.MIN, PriorityScale.MAX))
            },
            valueRange = PriorityScale.MIN.toFloat()..PriorityScale.MAX.toFloat(),
            steps = PriorityScale.MAX - PriorityScale.MIN - 1,
            colors = SliderDefaults.colors(
                thumbColor = Cinnabar,
                activeTrackColor = Cinnabar,
                inactiveTrackColor = Line,
                activeTickColor = CreamTick,
                inactiveTickColor = InkSoft.copy(alpha = 0.35f),
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(PriorityScale.MIN.toString(), style = MaterialTheme.typography.labelSmall, color = InkSoft)
            Text(PriorityScale.MAX.toString(), style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
    }
}

private val CreamTick = Color(0xFFFFF8F3)
