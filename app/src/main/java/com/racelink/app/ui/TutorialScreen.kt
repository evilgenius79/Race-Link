package com.racelink.app.ui

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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "RACE LINK",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onFinish) { Text("Skip") }
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
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    s.body,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // Dots
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(slides.size) { i ->
                val active = i == pagerState.currentPage
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(if (active) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.3f)
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
                modifier = Modifier.weight(1f),
            ) { Text("Back") }

            val isLast = pagerState.currentPage == slides.size - 1
            Button(
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (isLast) "Let's race" else "Next") }
        }
    }
}

@Composable
private fun tutorialSlides(): List<Slide> = listOf(
    Slide(
        title = "Heads-up drag racing",
        body = "Race Link pairs two phones over Bluetooth, fires a synchronized " +
            "Christmas tree, and uses GPS to time a 1/8 or 1/4 mile race. " +
            "Run it on track, on a closed road, or on a long private driveway.",
        art = { ArtLogo() },
    ),
    Slide(
        title = "Pair the two phones",
        body = "Both drivers open the app. One taps Host, the other taps Scan and " +
            "picks the host from the list. Once both phones say \"Connected\", " +
            "either of you can set up the race.",
        art = { ArtPair() },
    ),
    Slide(
        title = "Choose the rules",
        body = "Standing or rolling start. For rolling, pick a target speed " +
            "(e.g. 40 mph) and a tolerance (e.g. ±3 mph). Pick a distance: " +
            "1/8 mile, 1000 ft, or 1/4 mile. Send the request — the other " +
            "driver accepts or declines.",
        art = { ArtConfig() },
    ),
    Slide(
        title = "Both ready, both in the window",
        body = "Tap \"I'm ready\" on each phone. For a rolling start, the tree " +
            "won't arm until both cars are inside the speed window AND within " +
            "tolerance of each other. The speed cards turn green when you're " +
            "in the zone.",
        art = { ArtSpeedCards() },
    ),
    Slide(
        title = "Tree fires together",
        body = "After a small randomized delay, three ambers count down at " +
            "0.5 s, then the green fires on BOTH phones at the same instant. " +
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

// ---- art ----------------------------------------------------------------

@Composable private fun ArtLogo() {
    Box(
        Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text("RL", color = Color.White, fontWeight = FontWeight.Black, fontSize = 64.sp)
    }
}

@Composable private fun ArtPair() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PhoneFrame("HOST")
        Text("⇄", color = MaterialTheme.colorScheme.secondary, fontSize = 36.sp, fontWeight = FontWeight.Black)
        PhoneFrame("SCAN")
    }
}

@Composable private fun PhoneFrame(label: String) {
    Box(
        Modifier
            .width(80.dp).height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1F)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun ArtConfig() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Pill("Rolling • 40 mph • ±3", MaterialTheme.colorScheme.secondary)
        Pill("1/4 mile", Color(0xFFB0BEC5))
    }
}

@Composable private fun Pill(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) { Text(text, color = Color.Black, fontWeight = FontWeight.Bold) }
}

@Composable private fun ArtSpeedCards() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SpeedMini("YOU", "40", Color(0xFF4CAF50))
        SpeedMini("OPP", "39", Color(0xFF4CAF50))
    }
}

@Composable private fun SpeedMini(label: String, value: String, color: Color) {
    Column(
        Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1F))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 44.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Text("mph", color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable private fun ArtTree() {
    Box(
        Modifier
            .width(110.dp).height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF111111)),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Bulb(Color(0xFFFFC107))
            Bulb(Color(0xFFFFC107))
            Bulb(Color(0xFFFFC107))
            Bulb(Color(0xFF4CAF50))
        }
    }
}

@Composable private fun Bulb(c: Color) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(c))
}

@Composable private fun ArtFinish() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🏁", fontSize = 80.sp)
        Spacer(Modifier.height(8.dp))
        Text("11.842 s", color = Color(0xFF8BC34A), fontFamily = FontFamily.Monospace, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
