package com.auraflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.auraflow.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val manrope = GoogleFont("Manrope")

val Manrope = FontFamily(
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.ExtraLight),
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = manrope, fontProvider = provider, weight = FontWeight.Bold)
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)

val AuraTypography = Typography(
    displayLarge = style(52, FontWeight.ExtraLight, 60, -1f),
    displayMedium = style(42, FontWeight.ExtraLight, 50, -0.5f),
    displaySmall = style(34, FontWeight.Light, 42),
    headlineLarge = style(30, FontWeight.Light, 38),
    headlineMedium = style(26, FontWeight.Normal, 34),
    headlineSmall = style(22, FontWeight.Medium, 30),
    titleLarge = style(20, FontWeight.SemiBold, 28),
    titleMedium = style(16, FontWeight.SemiBold, 24, 0.1f),
    titleSmall = style(14, FontWeight.SemiBold, 20, 0.1f),
    bodyLarge = style(17, FontWeight.Normal, 26),
    bodyMedium = style(15, FontWeight.Normal, 22),
    bodySmall = style(13, FontWeight.Normal, 18),
    labelLarge = style(14, FontWeight.SemiBold, 20, 0.4f),
    labelMedium = style(12, FontWeight.Medium, 16, 0.5f),
    labelSmall = style(11, FontWeight.Medium, 16, 0.5f)
)
