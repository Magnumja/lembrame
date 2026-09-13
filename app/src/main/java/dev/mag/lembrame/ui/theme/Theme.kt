package dev.mag.lembrame.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Coral = Color(0xFFFF6B4A)
val Pink = Color(0xFFFF3D81)
val Cream = Color(0xFFF7F1E8)
val Ink = Color(0xFF1B1720)
val Night = Color(0xFF15131A)

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9CF),
    onPrimaryContainer = Color(0xFF3A0E00),
    secondary = Pink,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0E7DB),
    onSurfaceVariant = Color(0xFF6A6060),
    outline = Color(0xFFB9AEA6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A6B),
    onPrimary = Color(0xFF3A0E00),
    primaryContainer = Color(0xFF6B2A17),
    onPrimaryContainer = Color(0xFFFFD9CF),
    secondary = Color(0xFFFF7AA8),
    background = Night,
    onBackground = Color(0xFFF2EDF5),
    surface = Color(0xFF211E28),
    onSurface = Color(0xFFF2EDF5),
    surfaceVariant = Color(0xFF2C2834),
    onSurfaceVariant = Color(0xFFB8AFC2),
    outline = Color(0xFF6E667A),
)

val LembrameShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun LembrameTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    // Paleta própria (coral/rosa, igual ao ícone) em vez do Material You,
    // pra o app ter a mesma cara em qualquer papel de parede.
    val colors = if (dark) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, shapes = LembrameShapes, content = content)
}
