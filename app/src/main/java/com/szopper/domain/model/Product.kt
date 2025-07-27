package com.szopper.domain.model

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Product : RealmObject {
    @PrimaryKey
    var id: ObjectId = ObjectId()
    var name: String = ""
    var isBought: Boolean = false
    var position: Int = 0
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()
}
