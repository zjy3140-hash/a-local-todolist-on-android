package app.planlist.local.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.planlist.core.PlanStatus
import app.planlist.core.PriorityScale

val Paper = Color(0xFFE7E0D2)
val PaperCard = Color(0xFFF7F3EB)
val Ink = Color(0xFF1C1915)
val InkSoft = Color(0xFF6A6258)
val Cinnabar = Color(0xFFB4331F)
val Cream = Color(0xFFFFF8F3)
val Pine = Color(0xFF1E5C45)
val Stone = Color(0xFF8A8175)
val Line = Color(0xFFD5C8B4)

fun priorityColor(priority: Int): Color {
    val span = (PriorityScale.MAX - PriorityScale.MIN).toFloat()
    val t = (PriorityScale.coerce(priority) - PriorityScale.MIN) / span
    return lerp(Color(0xFFB7AA96), Cinnabar, t)
}

fun statusColor(status: PlanStatus): Color = when (status) {
    PlanStatus.IN_PROGRESS -> Cinnabar
    PlanStatus.DONE -> Pine
    PlanStatus.GIVE_UP -> Stone
}

private val Colors = lightColorScheme(
    primary = Cinnabar,
    onPrimary = Cream,
    primaryContainer = Color(0xFFF3D2C8),
    onPrimaryContainer = Color(0xFF3E0E08),
    secondary = Pine,
    onSecondary = Cream,
    background = Paper,
    onBackground = Ink,
    surface = PaperCard,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7DCCB),
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Color(0xFF8C1D18),
    onError = Color.White,
)

private val AppType = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        color = Ink,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        color = Ink,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        color = Ink,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        color = Ink,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = Ink,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Ink,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = InkSoft,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = Ink,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        color = InkSoft,
    ),
)

@Composable
fun PlanlistTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = AppType,
        content = content,
    )
}
