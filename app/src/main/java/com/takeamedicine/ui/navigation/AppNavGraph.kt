package com.takeamedicine.ui.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.takeamedicine.R
import com.takeamedicine.data.MedicineEntity
import com.takeamedicine.viewmodel.MedicineViewModel
import java.util.Locale

private const val MedicineListRoute = "medicine_list"

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
            MedicineListScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen() {
    val context = LocalContext.current
    val viewModel: MedicineViewModel = viewModel()
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    var selectedMedicine by remember { mutableStateOf<MedicineEntity?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var medicineToDelete by remember { mutableStateOf<MedicineEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.medicine_list_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedMedicine = null
                showEditor = true
            }) {
                Text("+")
            }
        }
    ) { innerPadding ->
        if (medicines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.empty_medicines))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(medicines, key = MedicineEntity::id) { medicine ->
                    MedicineListItem(
                        medicine = medicine,
                        onClick = {
                            selectedMedicine = medicine
                            showEditor = true
                        },
                        onLongClick = { medicineToDelete = medicine }
                    )
                }
            }
        }
    }

    if (showEditor) {
        ModalBottomSheet(
            onDismissRequest = { showEditor = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            AddEditMedicineScreen(
                medicine = selectedMedicine,
                onDismiss = { showEditor = false },
                onSave = { updatedMedicine, addToSystemAlarm ->
                    if (selectedMedicine == null) {
                        viewModel.insert(updatedMedicine)
                    } else {
                        viewModel.update(updatedMedicine)
                    }
                    if (addToSystemAlarm) {
                        openSystemAlarm(context, updatedMedicine)
                    }
                    showEditor = false
                }
            )
        }
    }

    medicineToDelete?.let { medicine ->
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text(stringResource(R.string.delete_medicine)) },
            text = { Text(stringResource(R.string.delete_medicine_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(medicine)
                    medicineToDelete = null
                }) {
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
            Text(medicine.name)
            Text("${medicine.dosage} ${medicine.unit}")
        }
        Text(
            String.format(
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
private fun AddEditMedicineScreen(
    medicine: MedicineEntity?,
    onDismiss: () -> Unit,
    onSave: (MedicineEntity, Boolean) -> Unit
) {
    var name by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.name.orEmpty()) }
    var dosage by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.dosage.orEmpty()) }
    var unit by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.unit ?: "片") }
    var hour by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.hour ?: 8) }
    var minute by rememberSaveable(medicine?.id) { mutableStateOf(medicine?.minute ?: 0) }
    var addToSystemAlarm by rememberSaveable(medicine?.id) { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var unitExpanded by rememberSaveable { mutableStateOf(false) }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var dosageError by rememberSaveable { mutableStateOf(false) }
    var unitError by rememberSaveable { mutableStateOf(false) }
    var timeError by rememberSaveable { mutableStateOf(false) }
    val units = stringArrayResource(R.array.medicine_units)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopAppBar(
            title = {
                Text(
                    stringResource(
                        if (medicine == null) R.string.add_medicine else R.string.edit_medicine
                    )
                )
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        )
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = false
            },
            label = { Text(stringResource(R.string.medicine_name)) },
            isError = nameError,
            supportingText = {
                if (nameError) Text(stringResource(R.string.required_field, stringResource(R.string.medicine_name)))
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = dosage,
                onValueChange = {
                    dosage = it
                    dosageError = false
                },
                label = { Text(stringResource(R.string.medicine_dosage)) },
                isError = dosageError,
                supportingText = {
                    if (dosageError) Text(stringResource(R.string.invalid_dosage))
                },
                modifier = Modifier.weight(1f)
            )
            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = !unitExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.unit)) },
                    isError = unitError,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = { unitExpanded = false }
                ) {
                    units.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                unit = option
                                unitError = false
                                unitExpanded = false
                            }
                        )
                    }
                }
            }
        }
        if (unitError) {
            Text(stringResource(R.string.invalid_unit))
        }
        OutlinedButton(onClick = { showTimePicker = true }) {
            Text(
                "${stringResource(R.string.reminder_time)}: " +
                    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            )
        }
        if (timeError) Text(stringResource(R.string.invalid_time))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = addToSystemAlarm,
                onCheckedChange = { addToSystemAlarm = it }
            )
            Text(stringResource(R.string.add_to_system_alarm))
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                nameError = name.isBlank()
                dosageError = dosage.toDoubleOrNull()?.let { it <= 0 } ?: true
                unitError = unit.isBlank()
                timeError = hour !in 0..23 || minute !in 0..59
                if (!nameError && !dosageError && !unitError && !timeError) {
                    onSave(
                        MedicineEntity(
                            id = medicine?.id ?: 0,
                            name = name.trim(),
                            dosage = dosage.trim(),
                            unit = unit,
                            hour = hour,
                            minute = minute
                        ),
                        addToSystemAlarm
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

private fun openSystemAlarm(context: android.content.Context, medicine: MedicineEntity) {
    val intent = Intent(AlarmClock.ACTION_SET_ALARM)
        .putExtra(AlarmClock.EXTRA_HOUR, medicine.hour)
        .putExtra(AlarmClock.EXTRA_MINUTES, medicine.minute)
        .putExtra(
            AlarmClock.EXTRA_MESSAGE,
            context.getString(
                R.string.system_alarm_message,
                medicine.name,
                "${medicine.dosage} ${medicine.unit}"
            )
        )
        .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
    if (intent.resolveActivity(context.packageManager) != null) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.system_alarm_unsupported),
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    } else {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.system_alarm_unsupported),
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}
