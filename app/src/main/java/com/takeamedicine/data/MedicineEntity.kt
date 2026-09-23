package com.takeamedicine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val unit: String = "片",
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true
)
