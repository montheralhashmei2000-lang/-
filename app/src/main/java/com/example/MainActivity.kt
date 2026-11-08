package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.fund.data.FundRepository
import com.example.fund.ui.HomeShell
import com.example.ui.theme.FundAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val repo = FundRepository.getInstance()
        repo.initStorage(applicationContext)

        setContent {
            val isDarkMode by repo.isDarkMode.collectAsState()
            val fontScale by repo.fontScale.collectAsState()

            FundAppTheme(
                darkTheme = isDarkMode,
                fontScale = fontScale
            ) {
                HomeShell()
            }
        }
    }
}
