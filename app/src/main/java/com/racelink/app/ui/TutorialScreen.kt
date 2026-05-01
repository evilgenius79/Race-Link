package com.racelink.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.racelink.app.ui.theme.RaceColors
import kotlinx.coroutines.launch

private data class Slide(
    val title: String,
    val body: String,
    val art: @Composable () -> Unit,
)

@Composable
fun TutorialScreen(onFinish: () -> Unit) {
    val slides = tutorialSlides()
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    ScreenBackground {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "RACE LINK",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onFinish) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                val s = slides[page]
                Column(
                    Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        s.art()
                    }
                    Text(
                        s.title,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        s.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Animated dots
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(slides.size) { i ->
                    val active = i == pagerState.currentPage
                    val w by animateDpAsState(if (active) 22.dp else 8.dp, label = "dotW")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(w)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else RaceColors.OnSurfaceFaint
                            ),
                    )
                }
            }

            // Nav buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                        scope.launch { pagerState.animateScrollToPage(target) }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Back") }

                val isLast = pagerState.currentPage == slides.size - 1
                Button(
                    onClick = {
                        if (isLast) onFinish()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (isLast) "LET'S RACE" else "NEXT",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun tutorialSlides(): List<Slide> = listOf(
    Slide(
        title = "Heads-up drag racing",
        body = "Pair two phones over Bluetooth, fire a synchronized Christmas " +
            "tree, and time a 1/8 or 1/4 mile race over GPS. Built for closed " +
            "courses and private property.",
        art = { ArtLogo() },
    ),
    Slide(
        title = "Pair the phones",
        body = "Both drivers open the app. One taps Host, the other taps Scan " +
            "and picks the host. Once both phones say Connected, either of " +
            "you can set up the race.",
        art = { ArtPair() },
    ),
    Slide(
        title = "Choose the rules",
        body = "Standing or rolling. For rolling, set a target speed and a " +
            "tolerance. Pick a distance: 1/8 mile, 1000 ft, or 1/4 mile. Send " +
            "the request — the other driver accepts or declines.",
        art = { ArtConfig() },
    ),
    Slide(
        title = "Both ready, both in the window",
        body = "Tap I'm ready on each phone. The tree won't arm until both " +
            "cars are inside the speed window AND within tolerance of each " +
            "other. Speed cards turn green when you're in the zone.",
        art = { ArtSpeedCards() },
    ),
    Slide(
        title = "Tree fires together",
        body = "After a small randomized delay, three ambers count down at " +
            "0.5 s, then green fires on BOTH phones at the same instant. " +
            "Floor it.",
        art = { ArtTree() },
    ),
    Slide(
        title = "GPS times the run",
        body = "Each phone measures its own car. First to the finish wins. " +
            "Lay the phone where it can see the sky for accurate GPS. " +
            "Have fun, and please — keep it on a closed course.",
        art = { ArtFinish() },
    ),
)

// ─── art ───────────────────────────────────────────────────────────────────

@Composable private fun ArtLogo() {
    Box(
        Modifier
            .size(180.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, RaceColors.AccentDeep))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(180.dp)
                .background(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent))
                ),
        )
        Text("RL", color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Black)
    }
}

@Composable private fun ArtPair() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PhoneFrame("HOST", MaterialTheme.colorScheme.primary)
        Text(
            "⇄",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
        )
        PhoneFrame("SCAN", RaceColors.Blue)
    }
}

@Composable private fun PhoneFrame(label: String, accent: Color) {
    RaceSurface(Modifier.size(width = 90.dp, height = 160.dp)) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
            }
            Spacer(Modifier.height(10.dp))
            Text(label, color = accent, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun ArtConfig() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Pill("ROLLING • 40 MPH • ±3", MaterialTheme.colorScheme.primary)
        Pill("1/4 MILE", RaceColors.Blue)
    }
}

@Composable private fun Pill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable private fun ArtSpeedCards() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SpeedMini("YOU", "40", RaceColors.Green)
        SpeedMini("OPP", "39", RaceColors.Green)
    }
}

@Composable private fun SpeedMini(label: String, value: String, color: Color) {
    RaceSurface(Modifier.width(110.dp)) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(value, color = color, fontSize = 44.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Text("MPH", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable private fun ArtTree() {
    Box(
        Modifier
            .size(width = 130.dp, height = 260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF050507), Color(0xFF0E0E15)))
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TreeBulb(on = true, color = RaceColors.Amber, size = 32.dp)
            TreeBulb(on = true, color = RaceColors.Amber, size = 32.dp)
            TreeBulb(on = true, color = RaceColors.Amber, size = 32.dp)
            TreeBulb(on = true, color = RaceColors.Green, size = 36.dp)
        }
    }
}

@Composable private fun ArtFinish() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏁", fontSize = 92.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "11.842",
            color = RaceColors.Green,
            fontFamily = FontFamily.Monospace,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
        )
        Text("seconds", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}
