package com.shirou.shibamusic.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.shirou.shibamusic.R
import com.shirou.shibamusic.ui.theme.ShibaMusicTheme

class ShibaMusicWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // In a real app, we would fetch the current song state here
        // For now, we'll use placeholder data or basic state if available
        provideContent {
            GlanceTheme {
                ShibaMusicWidgetContent()
            }
        }
    }

    @Composable
    private fun ShibaMusicWidgetContent() {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork Placeholder
                Image(
                    provider = ImageProvider(R.drawable.shiba_vector),
                    contentDescription = "Album Art",
                    modifier = GlanceModifier
                        .size(64.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                )

                Column(
                    modifier = GlanceModifier
                        .padding(start = 16.dp)
                        .defaultWeight()
                ) {
                    Text(
                        text = "No Song Playing",
                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                        maxLines = 1
                    )
                    Text(
                        text = "Select a song to play",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                        maxLines = 1
                    )
                }

                // Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_skip_previous),
                        contentDescription = "Previous",
                        modifier = GlanceModifier
                            .size(32.dp)
                            .padding(4.dp)
                            .clickable { /* TODO: Handle previous */ }
                    )
                    
                    Image(
                        provider = ImageProvider(R.drawable.ic_play),
                        contentDescription = "Play",
                        modifier = GlanceModifier
                            .size(48.dp)
                            .padding(4.dp)
                            .clickable { /* TODO: Handle play */ }
                    )

                    Image(
                        provider = ImageProvider(R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        modifier = GlanceModifier
                            .size(32.dp)
                            .padding(4.dp)
                            .clickable { /* TODO: Handle next */ }
                    )
                }
            }
        }
    }
}
