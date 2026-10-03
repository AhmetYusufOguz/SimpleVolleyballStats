package org.ahmetyusufoguz.simplevolleyballstats.data.local

import app.cash. sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.ahmetyusufoguz.simplevolleyballstats.database.VolleyballDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = VolleyballDatabase.Schema,
            name = "volleyball.db"
        )
    }
}