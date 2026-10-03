package org.ahmetyusufoguz.simplevolleyballstats

import android.app.Application
import org.ahmetyusufoguz.simplevolleyballstats.di.appModule
import org.ahmetyusufoguz.simplevolleyballstats.di.androidModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class VolleyballApp : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@VolleyballApp)
            modules(appModule, androidModule)
        }
    }
}