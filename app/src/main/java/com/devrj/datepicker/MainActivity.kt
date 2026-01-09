package com.devrj.datepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.devrj.datepicker.ui.theme.DatePickerTheme
import com.devrj.draggabledatepicker.ui.DraggableDateRangePicker
import com.inc.adv.draggabledaterangepicker.datePickerState.rememberDraggableDateRangePickerState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DatePickerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DraggableDateRangePicker(
                        modifier = Modifier.padding(innerPadding),
                        state = rememberDraggableDateRangePickerState(),
                        startYear = 2026,
                        endYear = 2026
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DatePickerTheme {
        Greeting("Android")
    }
}