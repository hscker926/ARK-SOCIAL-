package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand Gradient Accents (ARK SOCIAL Signature Prism)
val ArkCoral = Color(0xFFFF5A5F)
val ArkRose = Color(0xFFE1306C)
val ArkAmber = Color(0xFFFF9F43)
val ArkIndigo = Color(0xFF6C5CE7)
val ArkTeal = Color(0xFF00C2A8)

val ArkBrandGradient = Brush.linearGradient(
    colors = listOf(ArkAmber, ArkCoral, ArkRose, ArkIndigo)
)

val ArkStoryRingGradient = Brush.sweepGradient(
    colors = listOf(ArkAmber, ArkCoral, ArkRose, ArkIndigo, ArkAmber)
)

// Light Theme Tokens (Crisp White Canvas + Obsidian Ink)
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF5F6F9)
val LightSurfaceContainer = Color(0xFFF0F2F7)
val LightOnBackground = Color(0xFF0E1116)
val LightOnSurface = Color(0xFF0E1116)
val LightOnSurfaceVariant = Color(0xFF5D6676)
val LightOutline = Color(0xFFE2E6EE)

// Dark Theme Tokens (Deep Obsidian)
val DarkBackground = Color(0xFF0B0D12)
val DarkSurface = Color(0xFF10131B)
val DarkSurfaceVariant = Color(0xFF1A1F2B)
val DarkSurfaceContainer = Color(0xFF171B26)
val DarkOnBackground = Color(0xFFF6F7FA)
val DarkOnSurface = Color(0xFFF6F7FA)
val DarkOnSurfaceVariant = Color(0xFF9AA3B5)
val DarkOutline = Color(0xFF282E3D)
