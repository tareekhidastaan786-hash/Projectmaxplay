package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: MediaViewModel,
    onSplashFinished: () -> Unit
) {
    // Continuous Shimmer / Glowing Light Animation
    val infiniteTransition = rememberInfiniteTransition(label = "SplashShimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    // Glowing Light Brush across "video & music" subtitle
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF707075),
            Color(0xFFFFFFFF),
            Color(0xFF707075)
        ),
        start = androidx.compose.ui.geometry.Offset(translateAnim - 300f, 0f),
        end = androidx.compose.ui.geometry.Offset(translateAnim, 0f)
    )

    // Asynchronous background scan while splash animation plays
    LaunchedEffect(Unit) {
        viewModel.refreshMedia()
        delay(2200) // Smooth display duration
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top App Logo
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.maxplay_logo),
                    contentDescription = "MaxPlay Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Title: MaxPlay
            Text(
                text = "MaxPlay",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle: "video & music" with Glowing Light Shimmer
            Text(
                text = "video & music",
                style = TextStyle(
                    brush = shimmerBrush,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
