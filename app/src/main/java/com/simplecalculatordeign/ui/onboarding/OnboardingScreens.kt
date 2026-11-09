package com.simplecalculatordeign.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplecalculatordeign.R
import com.simplecalculatordeign.ui.components.GradientButton
import com.simplecalculatordeign.ui.components.GradientTitle
import com.simplecalculatordeign.ui.components.IconBadge
import com.simplecalculatordeign.ui.theme.Brand
import com.simplecalculatordeign.ui.theme.CalcTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Animated brand logo: gradient tile with the calculator glyph. */
@Composable
fun AppLogo(size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .shadow(24.dp, RoundedCornerShape(size * 0.28f), ambientColor = Color(0x884A00E0), spotColor = Color(0x884A00E0))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(Brand.Violet, Brand.DeepPurple, Brand.Blue))),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size * 1.35f),
        )
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val shimmer = rememberInfiniteTransition(label = "splash")
    val rotation by shimmer.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tilt",
    )
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(500)) }
        scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f))
        delay(650)
        onFinished()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha.value)) {
            AppLogo(120.dp, Modifier.scale(scale.value).rotate(rotation))
            Spacer(Modifier.height(28.dp))
            GradientTitle("Simple Calculator Deign", MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Unlimited precision · Scan · Solve",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(36.dp))
            com.simplecalculatordeign.ui.components.LoadingDots()
        }
    }
}

private data class OnboardingPage(val icon: ImageVector, val title: String, val body: String, val brush: Brush)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            Icons.Rounded.AllInclusive,
            "Unlimited precision",
            "Calculate with numbers of any size. Results stay exact — no hidden rounding, no cut-off digits. Copy the full result anytime.",
            Brush.linearGradient(listOf(Brand.Violet, Brand.Blue)),
        ),
        OnboardingPage(
            Icons.Rounded.DocumentScanner,
            "Scan & solve",
            "Point the camera at a receipt, invoice or worksheet. Review the recognised calculations, edit them, and see every step.",
            Brush.linearGradient(listOf(Brand.Cyan, Brand.Blue)),
        ),
        OnboardingPage(
            Icons.Rounded.History,
            "History & smart tools",
            "Every calculation is saved. Plus scientific mode, percentages, discounts, VAT, loan EMI and a unit converter.",
            Brush.linearGradient(listOf(Brand.Amber, Brand.Pink)),
        ),
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text("Skip") }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
            val page = pages[index]
            val float = rememberInfiniteTransition(label = "float$index")
            val dy by float.animateFloat(
                initialValue = -8f, targetValue = 8f,
                animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
                label = "dy",
            )
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.padding(top = (16 + dy).dp)) {
                    Box(
                        Modifier
                            .size(220.dp)
                            .alpha(0.18f)
                            .clip(CircleShape)
                            .background(page.brush)
                    )
                    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        IconBadge(page.icon, page.brush, size = 120.dp)
                    }
                }
                Spacer(Modifier.height(40.dp))
                Text(page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val selected = pager.currentPage == i
                val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "dot")
                val color by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    label = "dotColor",
                )
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        GradientButton(
            text = if (last) "Get started" else "Next",
            onClick = { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.fillMaxWidth(),
            brush = CalcTheme.colors.accentGradient,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Your data stays on your device.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
        )
    }
}
