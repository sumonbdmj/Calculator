package com.simplecalculatordeign.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.simplecalculatordeign.appContainer
import com.simplecalculatordeign.data.repository.AppSettings
import com.simplecalculatordeign.ui.assistant.AssistantScreen
import com.simplecalculatordeign.ui.calculator.CalculatorScreen
import com.simplecalculatordeign.ui.calculator.CalculatorViewModel
import com.simplecalculatordeign.ui.calculator.ScientificScreen
import com.simplecalculatordeign.ui.components.GradientBackground
import com.simplecalculatordeign.ui.history.HistoryScreen
import com.simplecalculatordeign.ui.onboarding.OnboardingScreen
import com.simplecalculatordeign.ui.onboarding.SplashScreen
import com.simplecalculatordeign.ui.scanner.CameraScreen
import com.simplecalculatordeign.ui.scanner.CropScreen
import com.simplecalculatordeign.ui.scanner.OcrProcessingScreen
import com.simplecalculatordeign.ui.scanner.ScanResultScreen
import com.simplecalculatordeign.ui.scanner.ScannerHomeScreen
import com.simplecalculatordeign.ui.scanner.ScannerViewModel
import com.simplecalculatordeign.ui.scanner.StepByStepScreen
import com.simplecalculatordeign.ui.settings.AboutScreen
import com.simplecalculatordeign.ui.settings.PrivacyPolicyScreen
import com.simplecalculatordeign.ui.settings.SettingsScreen
import com.simplecalculatordeign.ui.tools.DiscountScreen
import com.simplecalculatordeign.ui.tools.LoanScreen
import com.simplecalculatordeign.ui.tools.PercentageScreen
import com.simplecalculatordeign.ui.tools.UnitConverterScreen
import com.simplecalculatordeign.ui.tools.VatScreen
import kotlinx.coroutines.launch

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val CALCULATOR = "calculator"
    const val SCIENTIFIC = "scientific"
    const val SCANNER = "scanner"
    const val CAMERA = "scanner/camera"
    const val CROP = "scanner/crop"
    const val OCR = "scanner/ocr"
    const val SCAN_RESULT = "scanner/result"
    const val STEPS = "steps?expr={expr}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PERCENTAGE = "tools/percentage"
    const val DISCOUNT = "tools/discount"
    const val VAT = "tools/vat"
    const val LOAN = "tools/loan"
    const val UNITS = "tools/units"
    const val ASSISTANT = "assistant"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"

    fun steps(expression: String) = "steps?expr=${Uri.encode(expression)}"
}

private data class TopLevel(val route: String, val label: String, val icon: ImageVector)

private val TOP_LEVEL = listOf(
    TopLevel(Routes.CALCULATOR, "Calculator", Icons.Rounded.Calculate),
    TopLevel(Routes.SCANNER, "Scanner", Icons.Rounded.DocumentScanner),
    TopLevel(Routes.HISTORY, "History", Icons.Rounded.History),
    TopLevel(Routes.SETTINGS, "Settings", Icons.Rounded.Settings),
)

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        // The calculator is the root after splash/onboarding have been popped.
        popUpTo(Routes.CALCULATOR) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppRoot(settings: AppSettings) {
    val navController = rememberNavController()
    val container = LocalContext.current.appContainer
    // Activity-scoped so state survives switching tabs.
    val calculatorViewModel: CalculatorViewModel = viewModel(factory = CalculatorViewModel.factory(container))
    val scannerViewModel: ScannerViewModel = viewModel(factory = ScannerViewModel.factory(container))
    val scope = rememberCoroutineScope()

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = TOP_LEVEL.any { it.route == currentRoute }

    val back: () -> Unit = { navController.popBackStack() }
    val openSteps: (String) -> Unit = { expr -> navController.navigate(Routes.steps(expr)) }
    val useInCalculator: (String) -> Unit = { expr ->
        calculatorViewModel.loadExpression(expr)
        navController.navigateTopLevel(Routes.CALCULATOR)
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f), tonalElevation = 0.dp) {
                        TOP_LEVEL.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = { navController.navigateTopLevel(item.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier
                    .padding(bottom = padding.calculateBottomPadding())
                    .consumeWindowInsets(padding),
                enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(240)) { it / 12 } },
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(onFinished = {
                        val next = if (settings.onboardingDone) Routes.CALCULATOR else Routes.ONBOARDING
                        navController.navigate(next) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    })
                }
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(onDone = {
                        scope.launch { container.settingsRepository.setOnboardingDone() }
                        navController.navigate(Routes.CALCULATOR) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    })
                }
                composable(Routes.CALCULATOR) {
                    CalculatorScreen(
                        viewModel = calculatorViewModel,
                        onNavigate = { route ->
                            when (route) {
                                Routes.CALCULATOR -> Unit
                                Routes.SCANNER -> navController.navigateTopLevel(Routes.SCANNER)
                                else -> navController.navigate(route)
                            }
                        },
                        onShowSteps = openSteps,
                    )
                }
                composable(Routes.SCIENTIFIC) {
                    ScientificScreen(calculatorViewModel, onBack = back, onShowSteps = openSteps)
                }
                composable(Routes.SCANNER) {
                    ScannerHomeScreen(
                        viewModel = scannerViewModel,
                        onOpenCamera = { navController.navigate(Routes.CAMERA) },
                        onImageReady = { navController.navigate(Routes.CROP) },
                        onOpenResult = { navController.navigate(Routes.SCAN_RESULT) },
                    )
                }
                composable(Routes.CAMERA) {
                    CameraScreen(
                        viewModel = scannerViewModel,
                        onBack = back,
                        onCaptured = { navController.navigate(Routes.CROP) { popUpTo(Routes.CAMERA) { inclusive = true } } },
                    )
                }
                composable(Routes.CROP) {
                    CropScreen(
                        viewModel = scannerViewModel,
                        onBack = back,
                        onScan = { navController.navigate(Routes.OCR) },
                    )
                }
                composable(Routes.OCR) {
                    OcrProcessingScreen(
                        viewModel = scannerViewModel,
                        onBack = back,
                        onFinished = { navController.navigate(Routes.SCAN_RESULT) { popUpTo(Routes.OCR) { inclusive = true } } },
                    )
                }
                composable(Routes.SCAN_RESULT) {
                    ScanResultScreen(
                        viewModel = scannerViewModel,
                        onBack = back,
                        onShowSteps = openSteps,
                        onUseInCalculator = useInCalculator,
                        onNewScan = { navController.navigateTopLevel(Routes.SCANNER) },
                    )
                }
                composable(
                    route = Routes.STEPS,
                    arguments = listOf(navArgument("expr") { type = NavType.StringType; defaultValue = "" }),
                ) { entry ->
                    StepByStepScreen(
                        expression = entry.arguments?.getString("expr").orEmpty(),
                        onBack = back,
                        onUseInCalculator = useInCalculator,
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(onReuse = useInCalculator, onShowSteps = openSteps)
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onOpenAbout = { navController.navigate(Routes.ABOUT) },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    )
                }
                composable(Routes.PERCENTAGE) { PercentageScreen(onBack = back) }
                composable(Routes.DISCOUNT) { DiscountScreen(onBack = back) }
                composable(Routes.VAT) { VatScreen(onBack = back) }
                composable(Routes.LOAN) { LoanScreen(onBack = back) }
                composable(Routes.UNITS) {
                    UnitConverterScreen(onBack = back, onUseInCalculator = { number ->
                        calculatorViewModel.insertNumber(number)
                        navController.navigateTopLevel(Routes.CALCULATOR)
                    })
                }
                composable(Routes.ASSISTANT) { AssistantScreen(onBack = back) }
                composable(Routes.ABOUT) { AboutScreen(onBack = back) }
                composable(Routes.PRIVACY) { PrivacyPolicyScreen(onBack = back) }
            }
        }
    }
}
