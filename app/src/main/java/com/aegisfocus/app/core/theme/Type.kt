package com.aegisfocus.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.aegisfocus.app.R

/**
 * Aegis Focus Typography System — iOS-inspired clean hierarchy with robust bilingual
 * support (English + Bangla) ensuring complex conjuncts and diacritics never clip.
 */
private val defaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/** Josefin Sans (with Bangla letters from Hind Siliguri) for all normal text. */
val AegisBodyFamily = FontFamily(
    Font(R.font.aegis_body_400, FontWeight.Normal),
    Font(R.font.aegis_body_500, FontWeight.Medium),
    Font(R.font.aegis_body_600, FontWeight.SemiBold),
    Font(R.font.aegis_body_700, FontWeight.Bold)
)

/** Cabin Sketch (with Bangla letters) for titles and big numbers. Use only at 18sp and larger. */
val AegisTitleFamily = FontFamily(
    Font(R.font.aegis_title_regular, FontWeight.Normal),
    Font(R.font.aegis_title_bold, FontWeight.Bold)
)

val AegisTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AegisTitleFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.8).sp,
        lineHeightStyle = defaultLineHeightStyle,
    ),
    displayMedium = TextStyle(
        fontFamily = AegisTitleFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = defaultLineHeightStyle,
    ),
    headlineLarge = TextStyle(
        fontFamily = AegisTitleFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = defaultLineHeightStyle,
    ),
    headlineMedium = TextStyle(
        fontFamily = AegisTitleFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp,
        lineHeightStyle = defaultLineHeightStyle,
    ),
    titleLarge = TextStyle(
        fontFamily = AegisBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        lineHeightStyle = defaultLineHeightStyle,
    ),
    titleMedium = TextStyle(
        fontFamily = AegisBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        lineHeightStyle = defaultLineHeightStyle,
    ),
    bodyLarge = TextStyle(
        fontFamily = AegisBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp,
        lineHeightStyle = defaultLineHeightStyle,
    ),
    bodyMedium = TextStyle(
        fontFamily = AegisBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.15.sp,
        lineHeightStyle = defaultLineHeightStyle,
        color = TextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = AegisBodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        lineHeightStyle = defaultLineHeightStyle,
        color = TextMuted
    ),
    labelLarge = TextStyle(
        fontFamily = AegisBodyFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp,
        lineHeightStyle = defaultLineHeightStyle,
    ),
    labelMedium = TextStyle(
        fontFamily = AegisBodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
        lineHeightStyle = defaultLineHeightStyle,
        color = TextSecondary
    )
)
