package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.ui.HomeShell
import com.example.ui.theme.FundAppTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val repo = FundRepository.getInstance()
        repo.initStorage(applicationContext)

        setContent {
            val isDarkMode by repo.isDarkMode.collectAsStateWithLifecycle()
            val fontScale by repo.fontScale.collectAsStateWithLifecycle()

            FundAppTheme(
                darkTheme = isDarkMode,
                fontScale = fontScale
            ) {
                HomeShell()
            }
        }
    }
}
