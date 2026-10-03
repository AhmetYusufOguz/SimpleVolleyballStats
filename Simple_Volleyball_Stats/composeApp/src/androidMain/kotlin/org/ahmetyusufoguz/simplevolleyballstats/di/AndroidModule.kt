package org.ahmetyusufoguz.simplevolleyballstats.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.ahmetyusufoguz.simplevolleyballstats.data.local.createDataStore
import org.ahmetyusufoguz.simplevolleyballstats.database.VolleyballDatabase

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<SqlDriver> {
        val context = androidContext()
        AndroidSqliteDriver(
            schema = VolleyballDatabase.Schema,
            context = context,
            name = "svs_database_v2.db",
            callback = object : AndroidSqliteDriver.Callback(VolleyballDatabase.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys = ON;")
                }
            }
        )
    }

    single { org.ahmetyusufoguz.simplevolleyballstats.domain.service.ShareManager(androidContext()) }

    single { createDataStore(androidContext()) }
}