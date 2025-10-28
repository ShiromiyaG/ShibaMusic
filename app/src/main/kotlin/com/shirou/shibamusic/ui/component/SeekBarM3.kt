package com.shirou.shibamusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Slider do Material 3 como usado no Twelve (LineageOS)
 * Usa o Slider padrão do Android sem customizações pesadas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBarM3(
    value: Float,
    bufferedValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    var sliderValue by remember { mutableFloatStateOf(value) }
    
    LaunchedEffect(value) {
        sliderValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    }
    
    Slider(
        value = sliderValue,
        onValueChange = { newValue ->
            sliderValue = newValue
            onValueChange(newValue)
        },
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier,
        // Usa as cores padrão do Material 3
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SeekBarM3Preview() {
    MaterialTheme {
        var sliderPosition by rememberSaveable { mutableFloatStateOf(0.35f) }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Slider do Twelve (LineageOS)")
            Text(text = "Value: %.2f".format(sliderPosition))
            
            SeekBarM3(
                value = sliderPosition,
                bufferedValue = min(1f, sliderPosition + 0.25f),
                valueRange = 0f..1f,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {},
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
