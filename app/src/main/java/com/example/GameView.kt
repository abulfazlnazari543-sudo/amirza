package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Beautiful Persian traditional palette colors
val PersianTeal = Color(0xFF008080)
val PersianTurquoise = Color(0xFF14B8A6)
val PersianGold = Color(0xFFD97706)
val WarmAmirzaBrown = Color(0xFF78350F)
val SoftCreamBackground = Color(0xFFFFFBEB)
val AntiqueWood = Color(0xFF451A03)
val BrightGold = Color(0xFFFBBF24)
val CrimsonRed = Color(0xFFDC2626)

// Particle model for victory sparkles
data class VisualSparkle(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val currentX: Float,
    val currentY: Float,
    val size: Float,
    val color: Color,
    val alpha: Float
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameView(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Particles system state
    var sparkles = remember { mutableStateListOf<VisualSparkle>() }
    var particleIdCounter = remember { { Random.nextInt(100000) } }

    // Clear feedback message automatically after delay
    LaunchedEffect(uiState.feedbackMessage) {
        if (uiState.feedbackMessage != null) {
            delay(2500)
            viewModel.clearFeedback()
        }
    }

    // Trigger visual sparkles upon success
    LaunchedEffect(uiState.particlesTrigger) {
        if (uiState.particlesTrigger > 0) {
            val random = Random(System.currentTimeMillis())
            repeat(35) { i ->
                val angle = random.nextDouble() * 2 * Math.PI
                val speed = random.nextFloat() * 10f + 5f
                val sparkle = VisualSparkle(
                    id = particleIdCounter(),
                    startX = 500f, // Center relative
                    startY = 400f,
                    currentX = 0f,
                    currentY = 0f,
                    size = random.nextFloat() * 15f + 8f,
                    color = listOf(BrightGold, PersianTurquoise, PersianGold, Color.White).random(),
                    alpha = 1f
                )
                sparkles.add(sparkle)

                scope.launch {
                    var x = 0f
                    var y = 0f
                    var vx = cos(angle).toFloat() * speed
                    var vy = sin(angle).toFloat() * speed - 5f // drift upwards
                    var a = 1f
                    repeat(25) {
                        delay(20)
                        x += vx
                        y += vy
                        vy += 0.3f // gravity
                        a -= 0.04f
                        val index = sparkles.indexOfFirst { s -> s.id == sparkle.id }
                        if (index != -1) {
                            sparkles[index] = sparkles[index].copy(
                                currentX = x,
                                currentY = y,
                                alpha = a.coerceIn(0f, 1f)
                            )
                        }
                    }
                    sparkles.removeAll { s -> s.id == sparkle.id }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B)  // Slate 800
                    )
                )
            )
    ) {
        // Background decoration circles (traditional look)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = PersianTeal.copy(alpha = 0.05f),
                radius = 400f,
                center = Offset(size.width * 0.1f, size.height * 0.2f)
            )
            drawCircle(
                color = PersianGold.copy(alpha = 0.04f),
                radius = 600f,
                center = Offset(size.width * 0.9f, size.height * 0.8f)
            )
        }

        // Screen Routing
        AnimatedContent(
            targetState = uiState.currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) with
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                GameScreen.MAIN_MENU -> MainMenuView(
                    viewModel = viewModel,
                    uiState = uiState
                )
                GameScreen.PLAYING -> PlayingView(
                    viewModel = viewModel,
                    uiState = uiState,
                    sparkles = sparkles
                )
                GameScreen.LEVEL_SELECT -> LevelMapView(
                    viewModel = viewModel,
                    uiState = uiState
                )
                GameScreen.VICTORY -> VictoryView(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }

        // Overlay Feedback Message Banner (Traditional Banner style)
        val message = uiState.feedbackMessage
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically { -150 } + fadeIn(),
            exit = slideOutVertically { -150 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .zIndex(99f)
        ) {
            message?.let { msg ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.feedbackSuccess) PersianTeal else CrimsonRed
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .border(1.5.dp, BrightGold, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.feedbackSuccess) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. MAIN MENU VIEW
// -------------------------------------------------------------
@Composable
fun MainMenuView(
    viewModel: GameViewModel,
    uiState: GameState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Welcome text & Brand logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, BrightGold, CircleShape)
                    .background(Brush.radialGradient(listOf(PersianTurquoise, PersianTeal)))
                    .shadow(4.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "آ",
                    fontSize = 58.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.offset(y = (-4).dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "بازی کلمه‌چین",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = BrightGold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Text(
                text = "آمیرزا ابوالفضل نظری",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PersianTurquoise,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Traditional Persian Tile Ornament visual
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PersianTeal.copy(alpha = 0.15f)),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "خوش آمدید، جناب ابوالفضل نظری عزیز! 🌸\nبرای سنجش پازل کلمات آماده‌اید؟ بازی شبیه آمیرزا را آغاز نمایید.",
                    color = SoftCreamBackground,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Large CTA Menu Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 60.dp)
        ) {
            val pulsingScale by rememberInfiniteTransition(label = "").animateFloat(
                initialValue = 0.96f,
                targetValue = 1.04f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulsingButton"
            )

            Button(
                onClick = { viewModel.setScreen(GameScreen.PLAYING) },
                colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(64.dp)
                    .graphicsLayer {
                        scaleX = pulsingScale
                        scaleY = pulsingScale
                    }
                    .border(2.dp, SoftCreamBackground, RoundedCornerShape(32.dp))
                    .shadow(12.dp, RoundedCornerShape(32.dp))
                    .testTag("start_game_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AntiqueWood,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شروع بازی کلمات",
                        color = AntiqueWood,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Button(
                onClick = { viewModel.setScreen(GameScreen.LEVEL_SELECT) },
                colors = ButtonDefaults.buttonColors(containerColor = PersianTeal),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp)
                    .border(1.5.dp, PersianTurquoise, RoundedCornerShape(32.dp))
                    .testTag("level_map_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "نقشه مراحل بازی",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.resetGame() },
                border = BorderStroke(1.5.dp, CrimsonRed.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(44.dp)
                    .testTag("reset_progress_button")
            ) {
                Text(
                    text = "شروع مجدد کل مراحل",
                    color = CrimsonRed.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. LEVEL MAP VIEW
// -------------------------------------------------------------
@Composable
fun LevelMapView(
    viewModel: GameViewModel,
    uiState: GameState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setScreen(GameScreen.MAIN_MENU) },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .testTag("back_to_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "بازگشت به منلی",
                    tint = Color.White
                )
            }

            Text(
                text = "نقشه مراحل بازی",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = BrightGold,
                fontFamily = FontFamily.SansSerif
            )

            // Safe empty placeholder
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "مرحله‌ای را برای بازی تفضیلی انتخاب کنید:",
            color = SoftCreamBackground.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(GameData.levels.size) { index ->
                val isUnlocked = index <= uiState.highestUnlockedLevel
                val isCurrent = index == uiState.currentLevelIndex

                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable(enabled = isUnlocked) {
                            viewModel.selectLevel(index)
                        }
                        .border(
                            width = if (isCurrent) 3.dp else 1.dp,
                            color = if (isCurrent) BrightGold else if (isUnlocked) PersianTurquoise else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .shadow(
                            elevation = if (isCurrent) 8.dp else 2.dp,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("level_card_$index"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) {
                            PersianTeal.copy(alpha = 0.9f)
                        } else if (isUnlocked) {
                            AntiqueWood.copy(alpha = 0.7f)
                        } else {
                            Color.White.copy(alpha = 0.05f)
                        }
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isUnlocked) {
                                Text(
                                    text = (index + 1).toPersianDigits(),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isCurrent) Color.White else BrightGold,
                                    fontFamily = FontFamily.SansSerif
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (index < uiState.highestUnlockedLevel) "حل‌شده ✅" else "آماده بازی 🎯",
                                    fontSize = 10.sp,
                                    color = SoftCreamBackground.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.SansSerif
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "قفل",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. PLAYING VIEW (CORE GAME SCREEN WITH GESTURE WHEEL & TAPS)
// -------------------------------------------------------------
@Composable
fun PlayingView(
    viewModel: GameViewModel,
    uiState: GameState,
    sparkles: List<VisualSparkle>
) {
    val level = GameData.levels.getOrNull(uiState.currentLevelIndex) ?: return
    var listBonusWordsOpen by remember { mutableStateOf(false) }

    // Shuffler spin anim
    var shuffleRotation by remember { mutableStateOf(0f) }
    val animateRotation by animateFloatAsState(
        targetValue = shuffleRotation,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f),
        label = "ShuffleRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP STATUS BAR (Coins, Back, Title, Level Number)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setScreen(GameScreen.MAIN_MENU) },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .testTag("back_to_home")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "خانه",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "مرحله ${(uiState.currentLevelIndex + 1).toPersianDigits()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = BrightGold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "آمیرزا ابوالفضل نظری",
                    fontSize = 11.sp,
                    color = SoftCreamBackground.copy(alpha = 0.6f),
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Coin counter bubble
            Row(
                modifier = Modifier
                    .background(AntiqueWood.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .border(1.dp, BrightGold, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.coins.toPersianDigits(),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "💰",
                    fontSize = 14.sp
                )
            }
        }

        // WORD PUZZLE DISPLAY BOARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw sparkles particle explosions
            Canvas(modifier = Modifier.fillMaxSize()) {
                sparkles.forEach { sparkle ->
                    drawCircle(
                        color = sparkle.color,
                        radius = sparkle.size,
                        center = Offset(
                            x = size.width / 2 + sparkle.currentX,
                            y = size.height / 2 + sparkle.currentY
                        ),
                        alpha = sparkle.alpha
                    )
                }
            }

            // Display words arranged gracefully
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.targetWords.forEachIndexed { groupIdx, targetState ->
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        targetState.word.forEachIndexed { charIdx, char ->
                            val isCharRevealed = targetState.revealedIndices.contains(charIdx) || targetState.isSolved
                            val scale by animateFloatAsState(
                                targetValue = if (isCharRevealed) 1f else 0.9f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                                label = "CharRevealed"
                            )

                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(38.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = if (targetState.isSolved) {
                                                listOf(PersianTeal, Color(0xFF0F766E)) // Teal solved tiles
                                            } else {
                                                listOf(SoftCreamBackground, Color(0xFFFDE68A)) // Wooden/yellow tiles
                                            }
                                        )
                                    )
                                    .border(
                                        width = if (targetState.isSolved) 1.5.dp else 1.dp,
                                        color = if (targetState.isSolved) BrightGold else WarmAmirzaBrown.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCharRevealed) {
                                    Text(
                                        text = char.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (targetState.isSolved) Color.White else AntiqueWood,
                                        fontFamily = FontFamily.SansSerif,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    // Empty slot dot
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(AntiqueWood.copy(alpha = 0.3f), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FLOATING WORD CANDIDATE PREVIEW
        androidx.compose.animation.AnimatedVisibility(
            visible = uiState.candidateWord.isNotEmpty(),
            enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AntiqueWood),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clickable { viewModel.clearCandidate() }
                        .shadow(6.dp, RoundedCornerShape(20.dp)),
                    border = BorderStroke(2.dp, BrightGold)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.candidateWord,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "پاک کردن",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // TOOL TRAY AND GESTURE WHEEL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hint Button
            Button(
                onClick = { viewModel.useHint() },
                colors = ButtonDefaults.buttonColors(containerColor = PersianTeal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(width = 90.dp, height = 48.dp)
                    .border(1.dp, PersianTurquoise, RoundedCornerShape(16.dp))
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .testTag("hint_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "💡 راهنما",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "۱۰۰🪙",
                        color = BrightGold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // Shuffle Button
            IconButton(
                onClick = {
                    shuffleRotation += 360f
                    viewModel.shuffleLetters()
                },
                modifier = Modifier
                    .size(48.dp)
                    .rotate(animateRotation)
                    .background(AntiqueWood, CircleShape)
                    .shadow(4.dp, CircleShape)
                    .border(1.dp, BrightGold, CircleShape)
                    .testTag("shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "هم‌زدن",
                    tint = Color.White
                )
            }

            // Extra Words Button
            Button(
                onClick = { listBonusWordsOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(width = 90.dp, height = 48.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(16.dp))
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .testTag("bonus_words_btn")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎁 جوایز",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "(${uiState.bonusWordsFound.size.toPersianDigits()}) کلمه",
                        color = SoftCreamBackground.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        // TOUCH GESTURE WORD LETTERS WHEEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LetterWheel(
                letters = uiState.currentLetters,
                viewModel = viewModel,
                candidateWord = uiState.candidateWord
            )
        }
    }

    // BONUS WORDS DIALOG BOX
    if (listBonusWordsOpen) {
        AlertDialog(
            onDismissRequest = { listBonusWordsOpen = false },
            title = {
                Text(
                    text = "کلمات فرعی پیدا شده",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "کلماتی که حدس زدید اما در جدول اصلی نیستند (هر کلمه ۱۵+ سکه جایزه داشت):",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.bonusWordsFound.isEmpty()) {
                        Text(
                            text = "هنوز کلمه اضافی پیدا نکرده‌اید! 🔎",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.DarkGray
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.bonusWordsFound.forEach { bWord ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PersianTeal.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text(
                                        text = bWord,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PersianTeal,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { listBonusWordsOpen = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PersianTeal)
                ) {
                    Text("بستن 닫기", fontFamily = FontFamily.SansSerif)
                }
            }
        )
    }

    // LEVEL COMPLETE DIALOG CELEBRATION
    if (uiState.showLevelCompleteDialog) {
        AlertDialog(
            onDismissRequest = { /* forces click button */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            containerColor = Color(0xFFFFFBEB), // Vintage luxury background
            title = {
                Text(
                    text = "کار بی نقص! ✨",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = PersianTeal,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎉 مرحله با موفقیت تمام شد 🎉",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AntiqueWood,
                        fontFamily = FontFamily.SansSerif
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "جناب ابوالفضل نظری، شما تمام کلمات این مرحله را به درستی پیدا کردید.",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .background(PersianGold.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .border(1.dp, PersianGold, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎁 جایزه اتمام مرحله: ۵۰+ سکه طلایی",
                            fontWeight = FontWeight.Bold,
                            color = PersianGold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.nextLevel() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("next_level_button"),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "مرحله بعدی 🚀",
                        color = AntiqueWood,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        )
    }
}

// -------------------------------------------------------------
// LETTERS SELECTION WHEEL (SWIPE ENGINE & TAPS COMBINED)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LetterWheel(
    letters: List<Char>,
    viewModel: GameViewModel,
    candidateWord: String
) {
    var sizeW by remember { mutableStateOf(300f) }
    var sizeH by remember { mutableStateOf(300f) }

    val density = LocalDensity.current
    val hitRadiusPx = with(density) { 34.dp.toPx() } // Hit test trigger

    // Current finger coordinate relative to canvas
    var fingerOffset by remember { mutableStateOf<Offset?>(null) }
    
    // Ordered indices currently linked via the swipe path
    val swipedIndexes = remember { mutableStateListOf<Int>() }

    // Sequential manual click buffer for fallback clicks
    val sequentialClickIndexes = remember { mutableStateListOf<Int>() }

    // Triggers whenever letters change to reset index mapping
    LaunchedEffect(letters) {
        swipedIndexes.clear()
        sequentialClickIndexes.clear()
    }

    // Geometry calculation helpers
    val letterCount = letters.size
    val cx = sizeW / 2
    val cy = sizeH / 2
    val radius = Math.min(sizeW, sizeH) / 2 * 0.70f

    // Calculate Cartesian coordinate slots for elements
    val wheelPositions = remember(letters, sizeW, sizeH) {
        List(letterCount) { i ->
            val angle = (i * (2 * Math.PI) / letterCount) - Math.PI / 2
            Offset(
                x = (cx + radius * cos(angle)).toFloat(),
                y = (cy + radius * sin(angle)).toFloat()
            )
        }
    }

    // Clean tracking hit-test function
    fun performHitTest(touchPoint: Offset) {
        wheelPositions.forEachIndexed { idx, center ->
            val distance = sqrt((touchPoint.x - center.x).pow(2) + (touchPoint.y - center.y).pow(2))
            if (distance <= hitRadiusPx) {
                if (!swipedIndexes.contains(idx)) {
                    swipedIndexes.add(idx)
                    viewModel.buildCandidateFromIndices(swipedIndexes)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(280.dp)
            .onGloballyPositioned { layoutCoordinates ->
                sizeW = layoutCoordinates.size.width.toFloat()
                sizeH = layoutCoordinates.size.height.toFloat()
            }
            .clip(CircleShape)
            // Combined Swipe engine using multi-touch detect pointer
            .pointerInput(letters, wheelPositions) {
                detectDragGestures(
                    onDragStart = { offset ->
                        sequentialClickIndexes.clear()
                        swipedIndexes.clear()
                        fingerOffset = offset
                        performHitTest(offset)
                    },
                    onDrag = { change, _ ->
                        val pos = change.position
                        fingerOffset = pos
                        performHitTest(pos)
                    },
                    onDragEnd = {
                        if (swipedIndexes.isNotEmpty()) {
                            viewModel.submitCandidate()
                        }
                        swipedIndexes.clear()
                        fingerOffset = null
                    },
                    onDragCancel = {
                        swipedIndexes.clear()
                        fingerOffset = null
                        viewModel.clearCandidate()
                    }
                )
            }
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AntiqueWood,
                        Color(0xFF2D1002) // Deep circular tray look
                    )
                )
            )
            .border(4.dp, BrightGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Trace connection stroke path canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw lines between connected swiped letters
            if (swipedIndexes.size > 1) {
                val path = Path().apply {
                    val firstPt = wheelPositions.getOrNull(swipedIndexes.first()) ?: Offset.Zero
                    moveTo(firstPt.x, firstPt.y)
                    for (i in 1 until swipedIndexes.size) {
                        val nextPt = wheelPositions.getOrNull(swipedIndexes[i]) ?: Offset.Zero
                        lineTo(nextPt.x, nextPt.y)
                    }
                }
                drawPath(
                    path = path,
                    color = PersianTurquoise,
                    style = Stroke(
                        width = 14f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Draw line to current finger drag position for active swipe look
            if (swipedIndexes.isNotEmpty() && fingerOffset != null) {
                val lastPt = wheelPositions.getOrNull(swipedIndexes.last()) ?: Offset.Zero
                drawLine(
                    color = PersianTurquoise.copy(alpha = 0.6f),
                    start = lastPt,
                    end = fingerOffset!!,
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw letter buttons
        letters.forEachIndexed { index, char ->
            val pos = wheelPositions.getOrNull(index) ?: Offset.Zero
            val isSelected = swipedIndexes.contains(index) || sequentialClickIndexes.contains(index)
            
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                label = "LetterBubbleScale"
            )

            // Letter circle item position overlay
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (pos.x - with(density) { 26.dp.toPx() }).toInt(),
                            y = (pos.y - with(density) { 26.dp.toPx() }).toInt()
                        )
                    }
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    // Support TAPs for lag-free alternate clicking!
                    .clickable {
                        // Switch on Taps falling action
                        if (sequentialClickIndexes.contains(index)) {
                            // Already selected in click path? Remove it
                            sequentialClickIndexes.remove(index)
                        } else {
                            // Add to path
                            sequentialClickIndexes.add(index)
                        }
                        viewModel.buildCandidateFromIndices(sequentialClickIndexes)
                    }
                    .background(
                        Brush.verticalGradient(
                            colors = if (isSelected) {
                                listOf(PersianTurquoise, PersianTeal)
                            } else {
                                listOf(Color(0xFFFFFBEB), Color(0xFFFCD34D)) // Saffron Golden look
                            }
                        )
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Color.White else AntiqueWood,
                        shape = CircleShape
                    )
                    .shadow(elevation = 4.dp, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.White else AntiqueWood,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Center wheel controller (Clear / Verify for tapping mode support)
        if (sequentialClickIndexes.isNotEmpty() && swipedIndexes.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, BrightGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "ثبت",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightGold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .clickable {
                                viewModel.submitCandidate()
                                sequentialClickIndexes.clear()
                            }
                            .padding(4.dp)
                    )
                    Divider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.width(40.dp))
                    Text(
                        text = "پاک",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .clickable {
                                sequentialClickIndexes.clear()
                                viewModel.clearCandidate()
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. VICTORY GRAND VIEW (EPIC HONOR CREDITS TO ABOLFAZL NAZARI)
// -------------------------------------------------------------
@Composable
fun VictoryView(
    viewModel: GameViewModel,
    uiState: GameState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // High Trophy visual
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏆",
                    fontSize = 84.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "جناب ابوالفضل نظری قهرمان!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = BrightGold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )

            Text(
                text = "پایان بازی آمیرزا",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = PersianTurquoise,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        // Honor Message box
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AntiqueWood.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BrightGold, RoundedCornerShape(24.dp))
                .shadow(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تبریک صمیمانه! 🎖️",
                    color = BrightGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "آفرین بر ابوالفضل نظری هنرمند و توانا! شما تمام ۱۰ مرحله سخت و جذاب کلمه‌بازی آمیرزا نسخه سفارشی را با امتیاز کامل فتح کردید.",
                    color = SoftCreamBackground,
                    fontSize = 14.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "دمت گرم و سرت خوش باد! ✨",
                    color = PersianTurquoise,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Reset progress play again button
        Button(
            onClick = {
                viewModel.resetGame()
            },
            colors = ButtonDefaults.buttonColors(containerColor = PersianTeal),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
                .height(56.dp)
                .border(2.dp, Color.White, RoundedCornerShape(28.dp))
                .shadow(6.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "شروع مجدد تمام مراحل بازی 🔄",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

// Simple FlowRow helper for layout
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable () -> Unit
) {
    // Basic FlowRow wrapper in Compose
    @OptIn(ExperimentalLayoutApi::class)
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = { content() }
    )
}
