package ch.tichu.counter.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Immutable
sealed interface KeypadKey {
    data class Digit(val value: Int) : KeypadKey

    data object Backspace : KeypadKey

    data object ToggleSign : KeypadKey

    data object Confirm : KeypadKey
}

@Composable
fun Keypad(
    onKey: (KeypadKey) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    confirmEnabled: Boolean = true,
    confirmLabel: String = "OK",
) {
    val rows = listOf(
        listOf<KeypadKey>(KeypadKey.Digit(7), KeypadKey.Digit(8), KeypadKey.Digit(9), KeypadKey.Backspace),
        listOf<KeypadKey>(KeypadKey.Digit(4), KeypadKey.Digit(5), KeypadKey.Digit(6), KeypadKey.ToggleSign),
        listOf<KeypadKey>(KeypadKey.Digit(1), KeypadKey.Digit(2), KeypadKey.Digit(3), KeypadKey.Confirm),
        listOf<KeypadKey>(KeypadKey.Digit(0)),
    )
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    val weight = if (key is KeypadKey.Digit && key.value == 0) 3f else 1f
                    val keyModifier = Modifier
                        .weight(weight)
                        .height(52.dp)
                    when (key) {
                        is KeypadKey.Digit -> FilledTonalButton(
                            onClick = { onKey(key) },
                            enabled = enabled,
                            modifier = keyModifier,
                        ) { Text(key.value.toString(), style = MaterialTheme.typography.titleLarge) }

                        KeypadKey.Backspace -> FilledTonalButton(
                            onClick = { onKey(key) },
                            enabled = enabled,
                            modifier = keyModifier,
                        ) { Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace") }

                        KeypadKey.ToggleSign -> FilledTonalButton(
                            onClick = { onKey(key) },
                            enabled = enabled,
                            modifier = keyModifier,
                        ) { Text("+/-", style = MaterialTheme.typography.titleLarge) }

                        KeypadKey.Confirm -> Button(
                            onClick = { onKey(key) },
                            enabled = confirmEnabled,
                            modifier = keyModifier,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) { Text(confirmLabel, style = MaterialTheme.typography.titleLarge) }
                    }
                }
                if (row.size == 1) {
                    Row(Modifier.weight(1f)) {}
                }
            }
        }
    }
}
