import com.emilflach.lokcal.data.KrogerConfig

fun main() {
    // Proves the secrets plugin generated KrogerConfig into the source set.
    println("CLIENT_ID=[${KrogerConfig.CLIENT_ID}]")
    println("CLIENT_SECRET=[${KrogerConfig.CLIENT_SECRET}]")
}
