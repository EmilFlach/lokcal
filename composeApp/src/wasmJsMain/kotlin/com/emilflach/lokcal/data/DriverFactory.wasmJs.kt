package com.emilflach.lokcal.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

@OptIn(ExperimentalWasmJsInterop::class)
fun initializeWorker(): String = js(""" "./sqljs.worker.js" """)
actual class SqlDriverFactory {
    actual suspend fun createDriver(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver {
        return WebWorkerDriver(
            Worker(
                initializeWorker()
            )
        ).also { schema.create(it).await() }
    }
}


