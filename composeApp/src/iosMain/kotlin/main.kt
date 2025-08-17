import androidx.compose.ui.window.ComposeUIViewController
import com.emilflach.lokcal.App
import com.emilflach.lokcal.data.SqlDriverFactory
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App(sqlDriverFactory = SqlDriverFactory()) }
