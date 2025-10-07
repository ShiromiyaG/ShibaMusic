package com.shirou.shibamusic.ui.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PlayerColorExtractor - Extrai cores dominantes da artwork do álbum
 */
object PlayerColorExtractor {
    
    /**
     * Extrai cores da imagem usando Palette API
     */
    suspend fun extractColors(
        imageUrl: String?,
        context: android.content.Context,
        defaultColor: Color = Color(0xFF1A1A1A)
    ): PlayerColors = withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrEmpty()) {
            return@withContext PlayerColors(
                background = defaultColor,
                surface = defaultColor,
                accent = Color(0xFF6200EE)
            )
        }
        
        val imageLoader = ImageLoader(context)
        
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Palette precisa de software bitmap
                .build()
            
            val result = imageLoader.execute(request)
            
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                bitmap?.let {
                    return@withContext extractColorsFromBitmap(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        PlayerColors(
            background = defaultColor,
            surface = defaultColor,
            accent = Color(0xFF6200EE)
        )
    }
    
    /**
     * Extrai cores do bitmap usando Palette
     */
    private fun extractColorsFromBitmap(bitmap: Bitmap): PlayerColors {
        val palette = Palette.from(bitmap).generate()
        
        // Pega cores dominantes em ordem de preferência
        val vibrant = palette.vibrantSwatch
        val darkVibrant = palette.darkVibrantSwatch
        val darkMuted = palette.darkMutedSwatch
        val dominant = palette.dominantSwatch
        
        // Background: cor escura e vibrante
        val backgroundColor = darkVibrant?.rgb
            ?: darkMuted?.rgb
            ?: dominant?.rgb
            ?: 0xFF1A1A1A.toInt()
        
        // Surface: um pouco mais claro que o background
        val surfaceColor = lightenColor(backgroundColor, 0.1f)
        
        // Accent: cor vibrante para destaques
        val accentColor = vibrant?.rgb
            ?: dominant?.rgb
            ?: 0xFF6200EE.toInt()
        
        return PlayerColors(
            background = Color(backgroundColor),
            surface = Color(surfaceColor),
            accent = Color(accentColor)
        )
    }
    
    /**
     * Clareia uma cor em uma porcentagem
     */
    private fun lightenColor(color: Int, factor: Float): Int {
        val a = android.graphics.Color.alpha(color)
        val r = (android.graphics.Color.red(color) * (1 + factor)).toInt().coerceIn(0, 255)
        val g = (android.graphics.Color.green(color) * (1 + factor)).toInt().coerceIn(0, 255)
        val b = (android.graphics.Color.blue(color) * (1 + factor)).toInt().coerceIn(0, 255)
        return android.graphics.Color.argb(a, r, g, b)
    }
}

/**
 * Classe de dados para armazenar cores extraídas
 */
data class PlayerColors(
    val background: Color,
    val surface: Color,
    val accent: Color
)

/**
 * Composable que extrai cores da artwork e fornece via State
 */
@Composable
fun rememberPlayerColors(
    imageUrl: String?,
    defaultColor: Color = Color(0xFF1A1A1A)
): State<PlayerColors> {
    val context = LocalContext.current
    
    return produceState(
        initialValue = PlayerColors(
            background = defaultColor,
            surface = defaultColor,
            accent = Color(0xFF6200EE)
        ),
        key1 = imageUrl
    ) {
        value = PlayerColorExtractor.extractColors(
            imageUrl = imageUrl,
            context = context,
            defaultColor = defaultColor
        )
    }
}
