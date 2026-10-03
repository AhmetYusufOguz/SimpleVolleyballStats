package org.ahmetyusufoguz.simplevolleyballstats.data.local

import android.content.Context
import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.ahmetyusufoguz.simplevolleyballstats.database.VolleyballDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = VolleyballDatabase.Schema,
            context = context,
            name = "volleyball.db",
            callback = object : AndroidSqliteDriver.Callback(VolleyballDatabase.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Foreign key desteğini aç
                    db.execSQL("PRAGMA foreign_keys = ON;")
                    Log.d("DB", "volleyball.db açıldı. Foreign keys aktif.")
                }

                override fun onConfigure(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
        return driver
    }
}