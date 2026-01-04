package com.echo.core.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorExtractor {

    // EchoCoral hue is around 15-20 (orange/red range)
    // We want to avoid colors in the orange/red range (hue 0-40 and 340-360)
    private const val CORAL_HUE_MIN = 0f
    private const val CORAL_HUE_MAX = 45f
    private const val RED_HUE_MIN = 340f

    // Alternative color when extracted color is too similar to coral (a nice teal/blue)
    private val alternativeColor = Color(0xFF2D7D9A)

    suspend fun extractDominantColor(
        context: Context,
        imageUrl: String?,
        defaultColor: Color = Color(0xFF1A1A1A)
    ): Color {
        if (imageUrl.isNullOrEmpty()) return defaultColor

        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()

                val result = loader.execute(request)

                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        extractColorFromBitmap(bitmap, defaultColor)
                    } else {
                        defaultColor
                    }
                } else {
                    defaultColor
                }
            } catch (e: Exception) {
                defaultColor
            }
        }
    }

    private fun extractColorFromBitmap(bitmap: Bitmap, defaultColor: Color): Color {
        val palette = Palette.from(bitmap).generate()

        // Try to get the most vibrant/dominant color
        val dominantSwatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.lightMutedSwatch
            ?: palette.darkMutedSwatch
            ?: palette.dominantSwatch

        val extractedColor = dominantSwatch?.let { Color(it.rgb) } ?: defaultColor

        // Check if color is too similar to coral/orange
        if (isColorSimilarToCoral(extractedColor)) {
            // Try to find an alternative color from the palette that's not orange/red
            val alternativeSwatch = findAlternativeColor(palette)
            if (alternativeSwatch != null) {
                return Color(alternativeSwatch.rgb)
            }
            // If no good alternative, use our default teal
            return alternativeColor
        }

        return extractedColor
    }

    private fun isColorSimilarToCoral(color: Color): Boolean {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(
            android.graphics.Color.rgb(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            ),
            hsl
        )

        val hue = hsl[0]
        val saturation = hsl[1]

        // Only consider it "similar to coral" if it's saturated enough
        // Low saturation colors (grays, whites, blacks) are fine
        if (saturation < 0.3f) return false

        // Check if hue is in the orange/red range
        return (hue >= CORAL_HUE_MIN && hue <= CORAL_HUE_MAX) || hue >= RED_HUE_MIN
    }

    private fun findAlternativeColor(palette: Palette): Palette.Swatch? {
        // Try to find a color that's NOT in the orange/red range
        val swatches = listOfNotNull(
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.dominantSwatch
        )

        for (swatch in swatches) {
            val color = Color(swatch.rgb)
            if (!isColorSimilarToCoral(color)) {
                return swatch
            }
        }

        return null
    }
}
