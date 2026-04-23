package com.hans.ext.kernelmanager.ui.theme

import androidx.compose.ui.graphics.Color

// ── Background & Surface ──────────────────────────────────────────────────
val BackgroundDark       = Color(0xFF0F0F12)
val SurfaceDark          = Color(0xFF17171B)
val SurfaceVariant       = Color(0xFF1E1E24)
val SurfaceBright        = Color(0xFF222228)   // elevated cards
val OutlineDark          = Color(0xFF2C2C34)
val OutlineVariant       = Color(0xFF3A3A44)

// ── Text ──────────────────────────────────────────────────────────────────
val OnSurfaceDark        = Color(0xFFE2E2E8)
val OnSurfaceMuted       = Color(0xFF86868F)
val InverseSurface       = Color(0xFFE2E2E8)   // snackbar background
val InverseOnSurface     = Color(0xFF0F0F12)   // snackbar text

// ── Primary — soft indigo ─────────────────────────────────────────────────
val Primary              = Color(0xFF8B93E8)
val PrimaryDim           = Color(0xFF6870CF)
val PrimaryContainer     = Color(0xFF252650)   // tinted container behind primary content
val OnPrimary            = Color(0xFFFFFFFF)
val OnPrimaryContainer   = Color(0xFFBDC2FF)

// ── Secondary — sage green ────────────────────────────────────────────────
val Secondary            = Color(0xFF4EBD87)
val SecondaryContainer   = Color(0xFF1A3A2D)
val OnSecondary          = Color(0xFFFFFFFF)
val OnSecondaryContainer = Color(0xFF90D4B3)

// ── Tertiary — warm amber ─────────────────────────────────────────────────
val Tertiary             = Color(0xFFD9914A)
val TertiaryContainer    = Color(0xFF3A2710)
val OnTertiary           = Color(0xFFFFFFFF)
val OnTertiaryContainer  = Color(0xFFFFCC99)

// ── Error ─────────────────────────────────────────────────────────────────
val Error                = Color(0xFFCF6679)
val ErrorContainer       = Color(0xFF3B1320)
val OnError              = Color(0xFFFFFFFF)
val OnErrorContainer     = Color(0xFFFFB3BC)

// ── Status semantics (legacy aliases, kept for existing references) ────────
val StatusGreen          = Secondary
val StatusAmber          = Tertiary
val StatusRed            = Error

// ── Light mode (fallback, rarely used) ───────────────────────────────────
val BackgroundLight      = Color(0xFFF4F4F7)
val SurfaceLight         = Color(0xFFFFFFFF)
val OnSurfaceLight       = Color(0xFF18181C)

// ── Legacy aliases — kept to avoid breaking existing references ───────────
val CardGradientStart    = SurfaceDark
val CardGradientEnd      = BackgroundDark
