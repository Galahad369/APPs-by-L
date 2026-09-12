# Greater Art — Detailed Animation Implementation Draft (v1.9.17+)

**Based on actual codebase investigation (2026-09-12)**

---

## Animation 1: WaveformTimeline.kt — Per-Bar Staggered `animateFloatAsState`

### File & Location
- **File**: `greater-art/app/src/main/java/com/local/listentomusic/ui/NowPlayingScreen.kt`
- **Function**: `WaveformTimeline` (lines 842-932)
- **Canvas**: Lines 885-906 (inside `Slider.track`)

### Current State (lines 885-898)
```kotlin
Canvas(Modifier.fillMaxWidth().height(44.dp)) {
    val bars = displayPeaks?.size ?: 60
    val spacing = size.width / bars
    repeat(bars) { index ->
        val wave = displayPeaks?.getOrNull(index) ?: 0.025f
        val barHeight = size.height * (0.06f + wave * 0.90f)
        val x = spacing * (index + 0.5f)
        drawLine(
            color = if ((index + 1f) / bars <= fraction) active else inactive,
            start = Offset(x, (size.height - barHeight) / 2f),
            end = Offset(x, (size.height + barHeight) / 2f),
            strokeWidth = 2.25.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
```

### Implementation Spec

#### 1. File Path
`greater-art/app/src/main/java/com/local/listentomusic/ui/NowPlayingScreen.kt`

#### 2. Composable
`WaveformTimeline` (private, lines 842-932)

#### 3. State Management
| Concern | Choice | Remember Key |
|---------|--------|--------------|
| Bar height | `animateFloatAsState` per bar | `remember(key1 = bars.size, key2 = isPlaying)` |
| Stagger delay | `delayMillis = index * STAGGER_MS` in `tween` | `key = index` on `Box` |
| Recomposition isolation | Each bar in own `Box(key = index)` | `key(index)` on `Box` |

#### 4. Code Diff (lines 885-906)
```diff
        track = {
-            Canvas(Modifier.fillMaxWidth().height(44.dp)) {
-                val bars = displayPeaks?.size ?: 60
-                val spacing = size.width / bars
-                repeat(bars) { index ->
-                    val wave = displayPeaks?.getOrNull(index) ?: 0.025f
-                    val barHeight = size.height * (0.06f + wave * 0.90f)
-                    val x = spacing * (index + 0.5f)
-                    drawLine(
-                        color = if ((index + 1f) / bars <= fraction) active else inactive,
-                        start = Offset(x, (size.height - barHeight) / 2f),
-                        end = Offset(x, (size.height + barHeight) / 2f),
-                        strokeWidth = 2.25.dp.toPx(),
-                        cap = StrokeCap.Round,
-                    )
-                }
+            // PERF: Extract to inline composable to limit recomposition
+            AnimatedWaveformBars(
+                displayPeaks = displayPeaks,
+                fraction = fraction,
+                active = active,
+                inactive = inactive,
+                isPlaying = playback.isPlaying,
+                modifier = Modifier.fillMaxWidth().height(44.dp)
+            )
         }
```

#### 5. New Composable: `AnimatedWaveformBars`
```kotlin
@Composable
private fun AnimatedWaveformBars(
    displayPeaks: FloatArray?,
    fraction: Float,
    active: Color,
    inactive: Color,
    isPlaying: Boolean,
    modifier: Modifier
) {
    val bars = displayPeaks?.size ?: 60
    val staggerMs = 16L
    
    // PERF: Extract Canvas to isolate redraw from Slider recomposition
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .graphicsLayer {  // Isolate canvas from parent recomposition
        }) {
        val bars = displayPeaks?.size ?: 60
        val spacing = size.width / bars
        val centerY = size.height / 2f
        
        // PERF: Key by size + play state. New list = new animation set.
        val animatedHeights by remember(key1 = displayPeaks?.size ?: 60, key2 = playback.isPlaying) {
            derivedStateOf {
                (displayPeaks ?: FloatArray(60) { 0.025f }).mapIndexed { index, wave ->
                    val baseHeight = (0.06f + wave * 0.90f)
                    val targetHeight = if (playback.isPlaying) {
                        // Scale by fraction for active portion
                        baseHeight * (if ((index + 1f) / bars <= fraction) 1.0f else 0.3f)
                    } else {
                        baseHeight * 0.3f  // Idle state: reduced height
                    }
                }
        }
        
        repeat(bars) { index ->
            Box(key = index) {  // RECOMPOSITION SCOPE: each bar keyed
                val animatedHeight by animateFloatAsState(
                    targetValue = animatedHeights[index],
                    animationSpec = tween(
                        durationMillis = 120,
                        delayMillis = index * 16L,  // Stagger: 16ms per bar
                        easing = FastOutSlowInEasing
                    ),
                    label = "waveformBar_$index"
                )
                
                val x = spacing * (index + 0.5f)
                val barHeight = animatedHeight * size.height
                val color = if ((index + 1f) / bars <= fraction) active else inactive
                
                drawLine(
                    color = color,
                    start = Offset(x, centerY - barHeight / 2f),
                    end = Offset(x, centerY + barHeight / 2f),
                    strokeWidth = 2.25.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
```

---

## Animation 2: SeekBar Thumb — Drag Scale + AnimatedVisibility Timestamp

### File & Location
- **File**: `greater-art/app/src/main/java/com/local/listentomusic/ui/NowPlayingScreen.kt`
- **Function**: `WaveformTimeline` (lines 842-932)
- **Slider**: Lines 871-907 (Slider with custom `track` and `thumb = {}`)

### Current State (lines 871-884)
```kotlin
Slider(
    value = fraction,
    onValueChange = { seeking = true; seekFraction = it.coerceIn(0f, 1f) },
    onValueChangeFinished = { ... },
    valueRange = 0f..1f,
    enabled = hasDuration,
    modifier = Modifier.fillMaxWidth().height(56.dp),
    thumb = {},
    track = { ... }
)
```

### Implementation Spec

#### 1. File Path
`greater-art/app/src/main/java/com/local/listentomusic/ui/NowPlayingScreen.kt`

#### 2. Composable
`WaveformTimeline` (private, lines 842-932)

#### 3. State Management
| Concern | Choice | Remember Key |
|---------|--------|--------------|
| Thumb scale | `animateFloatAsState` | `remember { mutableStateOf(false) }` for `isDragging` |
| Timestamp visibility | `AnimatedVisibility` | `key = seeking` |
| Magnetism snap | `LaunchedEffect` | `key = dragX` |

#### 3. Code Diff (lines 871-907)
```diff
    Slider(
        value = fraction,
        onValueChange = { seeking = true; seekFraction = it.coerceIn(0f, 1f) },
        onValueChangeFinished = {
            if (hasDuration) {
                onSeek((playback.durationMs.toDouble() * seekFraction).toLong())
            }
            seeking = false
        },
        valueRange = 0f..1f,
        enabled = hasDuration,
        modifier = Modifier.fillMaxWidth().height(56.dp)
            .pointerInput(Unit) {  // ADD: Drag gesture for thumb animation
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        seekFraction = (seekFraction + change.positionChange().x / size.width).coerceIn(0f, 1f)
                    }
                )
            },
-        thumb = {},
+        thumb = {
+            val thumbScale by animateFloatAsState(
+                targetValue = if (isDragging) 1.4f else 1.0f,
+                animationSpec = tween(120, easing = FastOutSlowInEasing),
+                label = "seekThumbScale"
+            )
+            val thumbOffsetY by animateFloatAsState(
+                targetValue = if (isDragging) -12.dp.toPx() else 0f,
+                animationSpec = tween(120, easing = FastOutSlowInEasing),
+                label = "seekThumbOffset"
+            )
+            Box(
+                modifier = Modifier
+                    .graphicsLayer {
+                        scaleX = thumbScale
+                        scaleY = thumbScale
+                        translationY = thumbOffsetY
+                    }
+                    .size(24.dp)
+                    .background(active, CircleShape)
+            )
+        },
        track = { ... }
    )
```

#### 4. Timestamp Toast (add after Slider)
```kotlin
// ADD: Timestamp toast with AnimatedVisibility
AnimatedVisibility(
    visible = seeking,
    enter = expandVertically(expandFrom = Alignment.TopCenter) + fadeIn(80),
    exit = shrinkVertically + fadeOut(60)
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 56.dp + 8.dp)
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        val seekTime = (playback.durationMs.toDouble() * seekFraction).toLong()
        Text(
            text = formatDuration(seekTime),
            style = MaterialTheme.typography.labelMedium,
            color = active,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(active.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
        )
    }
}
```

---

## Animation 3: LiquidMetalSurface.kt CoverArt — Pulse + Beat Sync

### File & Location
- **File**: `greater-art/app/src/main/java/com/local/listentomusic/ui/components/LiquidMetalSurface.kt`
- **Function**: `LiquidMetalSurface` (lines 34-86)
- **Usage in NowPlayingScreen**: Lines 480-510 (AudioPlayer artwork)

### Current State (lines 55-78)
```kotlin
Box(
    modifier = modifier.clip(shape).background(base),
    contentAlignment = contentAlignment,
) {
    Canvas(Modifier.matchParentSize()) {
        val focus = size.width * travel
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.04f),
                    Color.White.copy(alpha = 0.19f),
                    teal.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.04f),
                    Color.Black.copy(alpha = 0.14f),
                ),
                start = Offset(focus - size.width * 0.72f, size.height),
                end = Offset(focus + size.width * 0.72f, 0f),
            ),
        ),
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        content()
    }
}
```

### Implementation Spec

#### 1. File Path
`greater-art/app/src/main/java/com/local/listentomusic/ui/components/LiquidMetalSurface.kt`

#### 2. Composable
`LiquidMetalSurface` (lines 34-86)

#### 3. State Management
| Concern | Choice | Remember Key |
|---------|--------|--------------|
| Pulse scale | `animateScaleAsState` | `remember(key1 = isPlaying, key2 = playbackSpeed)` |
| Beat sync | `Animatable` + `LaunchedEffect` | `key = isPlaying` |
| Recomposition scope | `graphicsLayer` on content | N/A |

#### 3. Code Diff (lines 33-86)
```diff
@Composable
fun LiquidMetalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    baseColor: Color? = null,
    accentColor: Color? = null,
+   isPlaying: Boolean = false,        // ADD
+   playbackSpeed: Float = 1f,          // ADD
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "liquidMetal")
    val travel by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "metalReflection",
    )
    val base = baseColor ?: MaterialTheme.colorScheme.surface
    val teal = accentColor ?: MaterialTheme.colorScheme.secondary
    
    // ADD: Pulse state
    val pulseScale by animateScaleAsState(
        targetValue = if (isPlaying) 1.02f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "coverPulse"
    )
    
    // ADD: Beat sync animation
    val beatAnimatable = remember(isPlaying) { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val beatInterval = (60_000f / (120f * playbackSpeed)).toLong()  // 120 BPM base
            while (isPlaying) {
                delay(beatInterval)
                if (!isPlaying) break
                beatAnimatable.animateTo(1f, tween(80, easing = FastOutSlowInEasing)).await()
                beatAnimatable.snapTo(0f)
            }
        } else {
            beatAnimatable.stop()
        }
    }
    val beatPulse by beatAnimatable
    
    Box(
        modifier = modifier.clip(shape).background(base),
        contentAlignment = contentAlignment,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val focus = size.width * travel
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f),
                        Color.White.copy(alpha = 0.19f),
                        teal.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.04f),
                        Color.Black.copy(alpha = 0.14f),
                    ),
                    start = Offset(focus - size.width * 0.72f, size.height),
                    end = Offset(focus + size.width * 0.72f, 0f),
                ),
            ),
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Box(
                modifier = Modifier
                    .graphicsLayer {  // ISOLATE: Pulse/beat only affect content, not LiquidMetal
                        scaleX = pulseScale * (1f + beatPulse * 0.02f)
                        scaleY = pulseScale * (1f + beatPulse * 0.02f)
                    }
                    .matchParentSize()
            ) {
                content()
            }
        }
    }
}
```

---

## Animation 4: WaveformTimeline.kt — "Breath" Staggered Phase

### File & Location
- **File**: `greater-art/app/src/main/java/com/local/listentomusic/ui/NowPlayingScreen.kt`
- **Function**: `WaveformTimeline` (lines 842-932)
- **Same Canvas as Animation 1** — extends Animation 1

### Implementation Spec

#### 1. File Path
`greater-art/app/src/main/java/com/local/listentomusic/ui/NowPlayingScreen.kt`

#### 2. Composable
`WaveformTimeline` (lines 842-932) — extends `AnimatedWaveformBars`

#### 3. State Management
| Concern | Choice | Remember Key |
|---------|--------|--------------|
| Breath progress | `Animatable` loop | `remember(isPlaying) { Animatable(0f) }` |
| Phase offset | `delayMillis = index * PHASE_STAGGER_MS` | `key = index` |
| Recomposition | Same `AnimatedWaveformBars` | `key1 = bars.size, key2 = isPlaying` |

#### 3. Code Addition to `AnimatedWaveformBars`
```kotlin
// ADD: Breath animation state
val breathProgress = remember(isPlaying) { Animatable(0f) }
LaunchedEffect(isPlaying) {
    if (isPlaying) {
        breathProgress.animateTo(
            1f,
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    } else {
        breathProgress.stop()
    }
}
val breathProgressValue = breathProgress.value

// In draw loop, replace animatedHeight calculation:
val breathPhase = (index * 0.15f) % 1f  // Stagger: 0.15 = ~5 bars per cycle
val localProgress = (breathProgressValue + breathPhase) % 1f
val breathMultiplier = 0.7f + 0.3f * cos(localProgress * 2 * PI)  // 0.7-1.0 range

val targetHeight = if (playback.isPlaying) {
    baseHeight * breathMultiplier * (if ((index + 1f) / bars <= fraction) 1.0f else 0.3f)
} else {
    baseHeight * 0.3f
}
```

---

## Animation 5: Seek Scrub Thumbnail Peel + Magnetism

### File & Location
- **File**: `greater-art/app/src/main/java/com/local/listentomusic/ui/NowPlayingScreen.kt`
- **Function**: `WaveformTimeline` (lines 842-932)
- **Slider**: Lines 871-907

### Implementation Spec

#### 1. File Path
`greater-art/app/src/main/java/com/local/listentomusic/ui/NowPlayingScreen.kt`

#### 2. Composable
`WaveformTimeline` (private, lines 842-932)

#### 3. State Management
| Concern | Choice | Remember Key |
|---------|--------|--------------|
| Peel visibility | `AnimatedVisibility` | `visible = isDragging` |
| Peel position | `dragX` state | `mutableStateOf(0f)` |
| Magnetism | `LaunchedEffect` | `key = dragX` |

#### 3. Code Diff (add after Slider in WaveformTimeline)
```diff
    Slider(...)
+    // ADD: Thumbnail peel + magnetism
+    var dragX by remember { mutableStateOf(0f) }
+    var snapTarget by remember { mutableStateOf<Float?>(null) }
+    val isDragging = seeking
+    
+    // Magnetism: snap to keyframes/chapters
+    LaunchedEffect(dragX) {
+        val keyframes = remember {  // Keyframe positions 0..1
+            (1..9).map { it / 10f } +  // 10% intervals
+            playback.durationMs > 0L let {  // Chapter markers if available
+                // Add chapter positions if available
+            }
+        }.flatten()
+        val nearest = keyframes.minByOrNull { abs(it - dragX) }
+        if (nearest != null && abs(nearest - dragX) < 0.02f) {
+            snapTarget.value = nearest
+        } else {
+            snapTarget.value = null
+        }
+    }
+    
+    // Thumbnail peel
+    AnimatedVisibility(
+        visible = isDragging,
+        enter = expandVertically(expandFrom = Alignment.TopCenter) + fadeIn(100),
+        exit = shrinkVertically + fadeOut(50)
+    ) {
+        ThumbnailPeel(
+            x = if (snapTarget != null) snapTarget! * size.width else dragX,
+            time = formatDuration((playback.durationMs.toDouble() * 
+                (if (snapTarget != null) snapTarget! else dragX)).toLong())
+        )
+    }
```

#### 4. New Composable: `ThumbnailPeel`
```kotlin
@Composable
private fun ThumbnailPeel(x: Float, time: String) {
    val thumbnailSize = 80.dp
    val stemHeight = 16.dp
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .wrapContentSize()
            .offset(x = x - thumbnailSize.value / 2f)
    ) {
        // Thumbnail frame
        Box(
            modifier = Modifier
                .size(thumbnailSize)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.secondary), RoundedCornerShape(8.dp))
        ) {
            // TODO: Load actual thumbnail via onLoadThumbnail
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        
        // Stem + timestamp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(stemHeight))
            Container(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .background(active.copy(alpha = 0.95f), RoundedCornerShape(6.dp)),
                padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
```

---

## Summary: Files to Modify

| # | Animation | File | Lines | Complexity |
|---|-----------|------|-------|------------|
| 1 | Waveform bars | `NowPlayingScreen.kt` | 842-932 | Medium |
| 2 | Seek thumb + timestamp | `NowPlayingScreen.kt` | 871-907 | Medium |
| 3 | Cover art pulse | `LiquidMetalSurface.kt` | 34-86 | Low |
| 4 | Waveform breath | `NowPlayingScreen.kt` | 842-932 | Low (extends #1) |
| 5 | Thumbnail peel | `NowPlayingScreen.kt` | 871-907 | Medium |

---

## Verification Commands
```bash
cd greater-art
$env:JAVA_HOME='C:/Program Files/Android/openjdk/jdk-21.0.8'
$env:ANDROID_HOME='<user-home>/AppData/Local/Android/Sdk'
$env:GRADLE_USER_HOME='<user-home>/.gradle'
./gradlew testDebugUnitTest lintDebug assembleDebug --offline --no-daemon

# Test animation recomposition
./gradlew testDebugUnitTest --tests "*WaveformTimeline*"
./gradlew testDebugUnitTest --tests "*LiquidMetalSurface*"
./gradlew testDebugUnitTest --tests "*SeekFeedback*"
```

---

*Status: Detailed implementation draft complete. Ready for implementation approval.*