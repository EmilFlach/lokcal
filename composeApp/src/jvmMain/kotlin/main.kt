import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.emilflach.lokcal.App
import com.emilflach.lokcal.data.SqlDriverFactory
import io.github.vinceglb.filekit.FileKit
import java.awt.Dimension

fun main() = application {
    FileKit.init(appId = "Lokcal")
    Window(
        title = "Lokcal",
        alwaysOnTop = true,
        state = rememberWindowState(
            position = WindowPosition(500.dp, 50.dp),
            width = 450.dp,
            height = 1000.dp
        ),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        App(sqlDriverFactory = SqlDriverFactory())
    }
}

