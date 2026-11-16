package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fund.data.FundRepository
import com.example.fund.ui.HomeShell
import com.example.fund.ui.components.FundLogoImage
import com.example.ui.theme.FundAppTheme
import com.example.ui.theme.FundPrimary

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val repo = FundRepository.getInstance()
        val appCtx = applicationContext

        setContent {
            var isStorageReady by remember { mutableStateOf(repo.isStorageInitialized()) }
            LaunchedEffect(Unit) {
                if (!isStorageReady) {
                    repo.initStorageAsync(appCtx)
                    isStorageReady = true
                }
            }

            val isDarkMode by repo.isDarkMode.collectAsStateWithLifecycle()
            val fontScale by repo.fontScale.collectAsStateWithLifecycle()
            val appFont by repo.appFontFamily.collectAsStateWithLifecycle()
            val screenFormat by repo.screenFormat.collectAsStateWithLifecycle()
            val printFormat by repo.printFormat.collectAsStateWithLifecycle()

            FundAppTheme(
                darkTheme = isDarkMode,
                fontScale = fontScale,
                appFont = appFont,
                screenFormat = screenFormat,
                printFormat = printFormat
            ) {
                if (!isStorageReady) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                FundLogoImage(
                                    customLogoBase64 = "",
                                    modifier = Modifier.size(72.dp)
                                )
                                Text(
                                    text = "جاري تهيئة الخزنة المشفرة وقاعدة البيانات...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FundPrimary
                                )
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = FundPrimary
                                )
                            }
                        }
                    }
                } else {
                    HomeShell()
                }
            }
        }
    }
}

