package com.takeamedicine.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(
    private val medicineDao: MedicineDao
) {
    fun getAll(): Flow<List<MedicineEntity>> = medicineDao.getAll()

    fun getById(id: Long): Flow<MedicineEntity?> = medicineDao.getById(id)

    suspend fun insert(medicine: MedicineEntity): Long = medicineDao.insert(medicine)

    suspend fun update(medicine: MedicineEntity) = medicineDao.update(medicine)

    suspend fun delete(medicine: MedicineEntity) = medicineDao.delete(medicine)
}
