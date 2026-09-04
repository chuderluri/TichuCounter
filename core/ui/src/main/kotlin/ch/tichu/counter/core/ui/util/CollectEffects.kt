package ch.tichu.counter.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> CollectEffects(effects: Flow<T>, handler: suspend (T) -> Unit) {
    val currentHandler by rememberUpdatedState(handler)
    LaunchedEffect(effects) {
        effects.collect { currentHandler(it) }
    }
}
