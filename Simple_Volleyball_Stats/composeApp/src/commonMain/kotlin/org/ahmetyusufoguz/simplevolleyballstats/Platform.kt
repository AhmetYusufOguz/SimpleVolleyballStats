package org.ahmetyusufoguz.simplevolleyballstats

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform