package com.ncb.drugtestcompanion

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ncb.drugtestcompanion.ui.auth.LoginScreen
import com.ncb.drugtestcompanion.ui.capture.CaptureScreen
import com.ncb.drugtestcompanion.ui.dashboard.DashboardScreen
import com.ncb.drugtestcompanion.ui.history.HistoryScreen
import com.ncb.drugtestcompanion.ui.home.HomeScreen
import com.ncb.drugtestcompanion.ui.mltest.MlTestScreen
import com.ncb.drugtestcompanion.ui.newtest.NewTestScreen
import com.ncb.drugtestcompanion.ui.theme.DrugTestCompanionTheme
import com.ncb.drugtestcompanion.viewmodel.CaptureViewModel
import com.ncb.drugtestcompanion.viewmodel.LanguageViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val languageViewModel: LanguageViewModel = hiltViewModel()
            val currentLanguage by languageViewModel.currentLanguage.collectAsState()

            val localizedConfiguration = remember(currentLanguage.code) {
                try {
                    val locale = Locale(currentLanguage.code)
                    Locale.setDefault(locale)

                    val config = Configuration(resources.configuration)
                    config.setLocale(locale)
                    config.setLayoutDirection(locale)

                    @Suppress("DEPRECATION")
                    resources.updateConfiguration(config, resources.displayMetrics)
                    config
                } catch (_: Throwable) {
                    resources.configuration
                }
            }

            val testString = try {
                resources.getString(R.string.language_test_key)
            } catch (e: Throwable) {
                "ERR: ${e.message}"
            }

            val appliedLocaleCode = try {
                if (!localizedConfiguration.locales.isEmpty) {
                    localizedConfiguration.locales.get(0).language
                } else {
                    currentLanguage.code
                }
            } catch (_: Throwable) {
                currentLanguage.code
            }

            Log.d("LANGUAGE_DEBUG", "selectedLanguage=${currentLanguage.code}")
            Log.d("LANGUAGE_DEBUG", "storedLanguage=${currentLanguage.code}")
            Log.d("LANGUAGE_DEBUG", "appliedLocale=$appliedLocaleCode")
            Log.d("LANGUAGE_DEBUG", "localizedString=$testString")

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration
            ) {
                DrugTestCompanionTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                HomeScreen(
                                    currentLanguage = currentLanguage,
                                    onSelectLanguage = { languageViewModel.selectLanguage(it) },
                                    onNavigateToLogin = { navController.navigate("login") },
                                    onNavigateToMlTest = { navController.navigate("ml_test") }
                                )
                            }

                            composable("ml_test") {
                                MlTestScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("login") {
                                LoginScreen(
                                    onLoginSuccess = { officerId ->
                                        navController.navigate("dashboard/$officerId") {
                                            popUpTo("home") { inclusive = false }
                                        }
                                    },
                                    onBackToHome = { navController.popBackStack() }
                                )
                            }

                            composable("dashboard/{officerId}") { backStackEntry ->
                                val officerId = backStackEntry.arguments?.getString("officerId") ?: "OP-LOCAL"
                                DashboardScreen(
                                    officerId = officerId,
                                    currentLanguage = currentLanguage,
                                    onSelectLanguage = { languageViewModel.selectLanguage(it) },
                                    onStartNewCase = { navController.navigate("kit_selection") },
                                    onViewHistory = { navController.navigate("history") },
                                    onSignOut = {
                                        navController.navigate("home") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("kit_selection") {
                                NewTestScreen(
                                    onNavigateToCapture = { kitId ->
                                        navController.navigate("capture/$kitId")
                                    },
                                    onNavigateToHistory = {
                                        navController.navigate("history")
                                    }
                                )
                            }

                            composable("capture/{kitId}") { backStackEntry ->
                                val kitId = backStackEntry.arguments?.getString("kitId") ?: "KIT_A"
                                val captureViewModel: CaptureViewModel = hiltViewModel()
                                LaunchedEffect(kitId) {
                                    captureViewModel.selectKitVariant(kitId)
                                }
                                CaptureScreen(
                                    viewModel = captureViewModel
                                )
                            }

                            composable("history") {
                                HistoryScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
