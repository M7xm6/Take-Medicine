package com.takeamedicine

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.takeamedicine.reminder.ReminderScheduler
import kotlinx.coroutines.launch
import com.takeamedicine.ui.navigation.AppNavGraph
import com.takeamedicine.ui.theme.TakeAMedicineTheme

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    getString(com.takeamedicine.R.string.notification_permission_denied),
                    Toast.LENGTH_LONG
                ).show()
                openNotificationSettings()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        requestExactAlarmPermission()
        lifecycleScope.launch {
            ReminderScheduler.rescheduleAll(applicationContext)
        }
        setContent {
            TakeAMedicineTheme {
                AppNavGraph()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )
                if (intent.resolveActivity(packageManager) != null) {
                    try {
                        startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(
                            this,
                            getString(com.takeamedicine.R.string.exact_alarm_inexact),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        if (intent.resolveActivity(packageManager) != null) {
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // The permission warning remains visible if settings are unavailable.
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun AppPreview() {
    TakeAMedicineTheme {
        AppNavGraph()
    }
}