package com.takeamedicine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.takeamedicine.data.MedicineEntity
import com.takeamedicine.data.MedicineRepository
import com.takeamedicine.data.MedicineDatabase
import com.takeamedicine.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MedicineRepository(
        MedicineDatabase.getInstance(application).medicineDao()
    )

    val medicines: StateFlow<List<MedicineEntity>> = repository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun getById(id: Long) = repository.getById(id)

    fun insert(medicine: MedicineEntity) {
        viewModelScope.launch {
            val id = repository.insert(medicine)
            if (medicine.enabled) {
                ReminderScheduler.schedule(getApplication(), medicine.copy(id = id))
            }
        }
    }

    fun update(medicine: MedicineEntity) {
        viewModelScope.launch {
            repository.update(medicine)
            if (medicine.enabled) {
                ReminderScheduler.schedule(getApplication(), medicine)
            } else {
                ReminderScheduler.cancel(getApplication(), medicine)
            }
        }
    }

    fun delete(medicine: MedicineEntity) {
        viewModelScope.launch {
            repository.delete(medicine)
            ReminderScheduler.cancel(getApplication(), medicine)
        }
    }
}
