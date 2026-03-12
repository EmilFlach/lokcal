import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.emilflach.lokcal.App
import com.emilflach.lokcal.data.SqlDriverFactory
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val body = document.body ?: return
    ComposeViewport(body) {
        App(sqlDriverFactory = SqlDriverFactory())
    }
}
