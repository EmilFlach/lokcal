package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ParticleBackground(
    particleCount: Int,
    modifier: Modifier = Modifier,
    tiltX: Float = 0f, // tilt around Y-axis (left/right)
    tiltY: Float = 0f  // tilt around X-axis (forward/back)
) {
    val density = LocalDensity.current
    val particleSize = with(density) { 3.dp.toPx() }

    // Animation time for subtle motion
    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            withFrameNanos { t ->
                if (start == 0L) start = t
                time = ((t - start) / 1_000_000_000.0f)
            }
        }
    }

    // We'll use layout id to get dimensions before generating particles
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    // Generate particles based on available dimensions
    val particles = remember(particleCount, canvasWidth, canvasHeight) {
        if (canvasWidth > 0 && canvasHeight > 0) {
            generateSandParticles(particleCount, particleSize, canvasWidth)
        } else {
            emptyList()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Update dimensions when they change
        if (canvasWidth != size.width || canvasHeight != size.height) {
            canvasWidth = size.width
            canvasHeight = size.height
        }

        // Then draw individual particles on top for texture

        // Sort by vertical position so lower particles draw last (brighter overlap)
        val sortedParticles = particles.sortedBy { it.fillPosition }

        // Draw particles with enhanced glow and subtle sand-like motion
        sortedParticles.forEach { particle ->
            val baseY = canvasHeight * particle.fillPosition

            // Procedural drift using time and particle properties, plus tilt bias
            val depth = particle.fillPosition // 0 bottom..1 crest (top of fill)
            val amp = 0.6f + 0.8f * depth  // lower amplitude to avoid wavy look
            val seed = particle.x * 0.013f + particle.size * 0.11f
            // Multi-frequency, de-synced noise-like drift (less wavy than a single sine)
            val n1x = sin(time * (0.21f + (seed % 0.17f)) + seed * 1.3f)
            val n2x = sin(time * (0.51f + (seed % 0.23f)) * 0.7f + seed * 2.1f)
            val n3x = sin(time * (0.91f + (seed % 0.19f)) * 0.33f + seed * 3.7f)
            val noiseX = (0.62f * n1x + 0.28f * n2x + 0.10f * n3x)

            val n1y = sin(time * (0.18f + (seed % 0.13f)) + seed * 0.9f)
            val n2y = sin(time * (0.44f + (seed % 0.29f)) * 0.6f + seed * 1.7f)
            val n3y = sin(time * (0.77f + (seed % 0.21f)) * 0.28f + seed * 2.9f)
            val noiseY = (0.64f * n1y + 0.26f * n2y + 0.10f * n3y)

            val driftX = noiseX * amp + tiltX * 12f * (0.4f + 0.6f * depth)
            val driftY = noiseY * 0.6f * amp - tiltY * 8f * (0.3f + 0.7f * depth)

            // Allow particles to reach the very top of the filled region (no artificial crest limit)
            val cyUnclamped = baseY + driftY
            val cy = cyUnclamped.coerceIn(0f, canvasHeight)
            val cx = (particle.x + driftX).coerceIn(0f, canvasWidth)

            if (cx >= 0 && cx <= canvasWidth && cy >= 0 && cy <= canvasHeight) {
                // Outer glow (calmer)
                drawCircle(
                    color = particle.color,
                    radius = particle.size * 2.0f,
                    center = Offset(cx, cy),
                    alpha = (particle.alpha * 0.1f).coerceIn(0f, 0.20f)
                )
                // Middle glow
                drawCircle(
                    color = particle.color,
                    radius = particle.size * 1.35f,
                    center = Offset(cx, cy),
                    alpha = (particle.alpha * 0.20f).coerceIn(0f, 0.30f)
                )
                // Core with starry-night sparkle: base twinkle + occasional sparkle pulse
                val baseTwinkle = 0.93f + 0.07f * (sin(time * (1.1f + (seed % 0.55f)) + seed * 3.7f) * 0.5f + 0.5f)
                // Sparkle pulse: random subset of particles get a slow pulse occasionally
                val pulsePhase = time * (0.6f * particle.sparkleSpeed) + particle.sparkleSeed
                val pulseWave = (sin(pulsePhase) * 0.5f + 0.5f) // 0..1
                val pulseGate = if ((sin(time * 0.15f + particle.sparkleSeed * 0.37f) * 0.5f + 0.5f) < particle.sparkleChance) 1f else 0f
                val sparkleMul = 1f + 4f * pulseWave * pulseGate // up to +40% when pulsing
                val coreAlpha = (particle.alpha * 0.3f * baseTwinkle * sparkleMul).coerceIn(0f, 0.7f)
                drawCircle(
                    color = particle.color,
                    radius = particle.size,
                    center = Offset(cx, cy),
                    alpha = coreAlpha
                )
            }
        }
    }
}

data class Particle(
    val x: Float,
    val size: Float,
    val color: Color,
    val alpha: Float,
    val fillPosition: Float, // 0.0 (top) to 1.0 (bottom)
    val sparkleSeed: Float,   // stable random to desync sparkle
    val sparkleChance: Float, // probability weight to enter sparkle pulses
    val sparkleSpeed: Float   // speed multiplier for sparkle pulses
)

private fun generateSandParticles(count: Int, particleSize: Float, canvasWidth: Float): List<Particle> {
    if (count <= 0) return emptyList()

    // Simplified: place particles uniformly at random across entire canvas.
    // No rows/cols/neighbor logic to avoid any banding.
    val particles = ArrayList<Particle>(count)
    repeat(count) {
        val x = Random.nextFloat() * canvasWidth
        val yNorm = Random.nextFloat().coerceIn(0.001f, 0.999f) // 0=top, 1=bottom in our current drawing usage
        // Slight depth-based variation, but very subtle to keep uniform look
        val size = (particleSize * (0.9f + 0.4f * Random.nextFloat())).coerceAtLeast(0.8f)
        val alpha = (0.08f + 0.28f * Random.nextFloat()).coerceIn(0.08f, 0.4f)
        particles.add(
            Particle(
                x = x,
                size = size,
                color = generateSandColor(),
                alpha = alpha,
                fillPosition = yNorm,
                sparkleSeed = Random.nextFloat() * 1000f,
                sparkleChance = 0.10f + 0.20f * Random.nextFloat(),
                sparkleSpeed = 0.6f + 1.2f * Random.nextFloat()
            )
        )
    }
    return particles
}


private fun generateSandColor(): Color {
    // Sand colors range from light yellows to light browns
    return Color(
        red = Random.nextFloat() * 0.15f + 0.85f,
        green = Random.nextFloat() * 0.2f + 0.7f,
        blue = Random.nextFloat() * 0.2f + 0.3f,
        alpha = 1f
    )
}

// We don't need a custom pow function as it's already available in kotlin.math
// for Float.pow(Float) and Float.pow(Int)