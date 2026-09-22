package com.takeamedicine.ui.navigation

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.takeamedicine.R
import com.takeamedicine.data.MedicineEntity
import com.takeamedicine.viewmodel.MedicineViewModel
import java.util.Locale

private const val MedicineListRoute = "medicine_list"
private const val AddMedicineRoute = "add_medicine"
private const val EditMedicineRoute = "edit_medicine/{medicineId}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MedicineListRoute,
        modifier = modifier
    ) {
        composable(MedicineListRoute) {
            MedicineListScreen(
                onAddMedicine = { navController.navigate(AddMedicineRoute) },
                onEditMedicine = { id -> navController.navigate("edit_medicine/$id") }
            )
        }
        composable(AddMedicineRoute) {
            AddEditMedicineScreen(
                medicineId = null,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = EditMedicineRoute,
            arguments = listOf(navArgument("medicineId") { type = NavType.LongType })
        ) { backStackEntry ->
            AddEditMedicineScreen(
                medicineId = backStackEntry.arguments?.getLong("medicineId"),
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(
    onAddMedicine: () -> Unit,
    onEditMedicine: (Long) -> Unit
) {
    val viewModel: MedicineViewModel = viewModel()
    val medicines = viewModel.medicines.collectAsStateWithLifecycle()
    var medicineToDelete by remember { mutableStateOf<MedicineEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.medicine_list_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedicine) {
                Text("+")
            }
        }
    ) { innerPadding ->
        if (medicines.value.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("还没有添加药品")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(medicines.value, key = MedicineEntity::id) { medicine ->
                        MedicineListItem(
                            medicine = medicine,
                            onClick = { onEditMedicine(medicine.id) },
                            onLongClick = { medicineToDelete = medicine }
                        )
                    }
            }
        }
    }

    medicineToDelete?.let { medicine ->
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text(stringResource(R.string.delete_medicine)) },
            text = { Text(stringResource(R.string.delete_medicine_message)) },
            confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.delete(medicine)
                            medicineToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
            },
            dismissButton = {
                    TextButton(onClick = { medicineToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MedicineListItem(
    medicine: MedicineEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = medicine.name)
            Text(text = medicine.dosage)
        }
        Text(
            text = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                medicine.hour,
                medicine.minute
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    medicineId: Long?,
    onBack: () -> Unit
) {
    val viewModel: MedicineViewModel = viewModel()
    var name by rememberSaveable(medicineId) { mutableStateOf("") }
    var dosage by rememberSaveable(medicineId) { mutableStateOf("") }
    var hour by rememberSaveable(medicineId) { mutableStateOf(8) }
    var minute by rememberSaveable(medicineId) { mutableStateOf(0) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var dosageError by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable(medicineId) { mutableStateOf(false) }

    LaunchedEffect(medicineId) {
        if (medicineId != null) {
            viewModel.getById(medicineId).collect { medicine ->
                if (!loaded && medicine != null) {
                    name = medicine.name
                    dosage = medicine.dosage
                    hour = medicine.hour
                    minute = medicine.minute
                    loaded = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (medicineId == null) {
                                R.string.add_medicine
                            } else {
                                R.string.edit_medicine
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text(stringResource(R.string.medicine_name)) },
                isError = nameError,
                supportingText = {
                    if (nameError) {
                        Text(stringResource(R.string.required_field, stringResource(R.string.medicine_name)))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dosage,
                onValueChange = {
                    dosage = it
                    dosageError = false
                },
                label = { Text(stringResource(R.string.medicine_dosage)) },
                isError = dosageError,
                supportingText = {
                    if (dosageError) {
                        Text(stringResource(R.string.required_field, stringResource(R.string.medicine_dosage)))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { showTimePicker = true }) {
                Text(
                    stringResource(
                        R.string.reminder_time
                    ) + ": " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    nameError = name.isBlank()
                    dosageError = dosage.isBlank()
                    if (!nameError && !dosageError) {
                        val medicine = MedicineEntity(
                            id = medicineId ?: 0,
                            name = name.trim(),
                            dosage = dosage.trim(),
                            hour = hour,
                            minute = minute
                        )
                        if (medicineId == null) {
                            viewModel.insert(medicine)
                        } else {
                            viewModel.update(medicine)
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = timePickerState)
                Button(
                    onClick = {
                        hour = timePickerState.hour
                        minute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
