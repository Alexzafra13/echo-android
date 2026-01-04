package com.echo.core.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorExtractor {

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

        // Try to get the most vibrant/dominant color, similar to web implementation
        val dominantSwatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.lightMutedSwatch
            ?: palette.darkMutedSwatch
            ?: palette.dominantSwatch

        return dominantSwatch?.let {
            Color(it.rgb)
        } ?: defaultColor
    }
}
