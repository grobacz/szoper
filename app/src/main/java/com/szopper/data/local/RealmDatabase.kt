package com.szopper.data.local

import com.szopper.domain.model.Product
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealmDatabase @Inject constructor() {
    
    private val configuration = RealmConfiguration.Builder(
        schema = setOf(Product::class)
    )
        .name("szopper.realm")
        .schemaVersion(1)
        .build()
    
    val realm: Realm by lazy {
        Realm.open(configuration)
    }
}