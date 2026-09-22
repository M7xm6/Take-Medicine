package com.takeamedicine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.takeamedicine.ui.navigation.AppNavGraph
import com.takeamedicine.ui.theme.TakeAMedicineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TakeAMedicineTheme {
                AppNavGraph()
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