package cl.duoc.tacticaltrack

import android.os.Bundle
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import cl.duoc.tacticaltrack.navigation.TacticalTrackApp
import cl.duoc.tacticaltrack.ui.theme.TacticalTrackTheme

class MainActivity : ComponentActivity() {

    private val pendingVisitId = MutableStateFlow<Long?>(null)
    private fun readPush(intent:Intent?) {
        val raw = intent?.getStringExtra("visitId") ?: return
        pendingVisitId.value=raw.toLongOrNull()?.takeIf {it>0}
        intent.removeExtra("visitId")
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readPush(intent)
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readPush(intent)
        enableEdgeToEdge()

        setContent {
            TacticalTrackTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                val pending by pendingVisitId.collectAsStateWithLifecycle()
                TacticalTrackApp(isExpanded=isExpanded,pendingVisitId=pending,onPushConsumed={pendingVisitId.value=null})
            }
        }
    }
}

