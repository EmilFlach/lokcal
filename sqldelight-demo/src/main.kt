import com.emilflach.lokcal.Database

fun main() {
    // Proves the sqldelight plugin generated the Database interface and it compiles.
    println("Generated SQLDelight Database: ${Database::class.qualifiedName}")
}
