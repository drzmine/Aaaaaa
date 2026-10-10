package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import coil.compose.AsyncImage
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// --- Local game-play entities ---
data class LiveRat(
    val id: Long,
    var x: Float,
    var y: Float,
    val size: Float,
    var passed: Boolean = false,
    var isEaten: Boolean = false,
    val speedOffset: Float = 0f,
    var backSpeed: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var life: Float, // 1.0 down to 0.0
    val maxLife: Float = 1.0f
)

data class ScorePopup(
    val text: String,
    var x: Float,
    var y: Float,
    var life: Float, // 1.0 down to 0.0
    var color: Color = Color(0xFFFFEA00)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val isLevelConfirmed by viewModel.isLevelConfirmed.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Top high scores
    val easyHighScore by viewModel.easyHighScore.collectAsState()
    val hardHighScore by viewModel.hardHighScore.collectAsState()
    val superHighScore by viewModel.superHighScore.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentScreen) {
            Screen.MENU -> {
                MainMenuScreen(
                    viewModel = viewModel,
                    selectedLevel = selectedLevel,
                    isLevelConfirmed = isLevelConfirmed,
                    profile = profile,
                    easyScore = easyHighScore,
                    hardScore = hardHighScore,
                    superScore = superHighScore
                )
            }
            Screen.GAME -> {
                GamePlayScreen(
                    viewModel = viewModel,
                    settings = settings,
                    selectedLevel = selectedLevel,
                    profile = profile
                )
            }
            Screen.PROFILE -> {
                ProfileScreen(
                    viewModel = viewModel,
                    profile = profile,
                    easyScore = easyHighScore,
                    hardScore = hardHighScore,
                    superScore = superHighScore
                )
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    settings = settings
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. MAIN MENU SCREEN
// -------------------------------------------------------------
@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    selectedLevel: GameLevel,
    isLevelConfirmed: Boolean,
    profile: ProfileData,
    easyScore: Int,
    hardScore: Int,
    superScore: Int
) {
    val scope = rememberCoroutineScope()
    val animScale = remember { Animatable(0.95f) }

    LaunchedEffect(Unit) {
        animScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    // Get the profile avatar or picture to display
    val avatarEmoji = when (profile.avatarIndex) {
        1 -> "👧"
        2 -> "🥷"
        3 -> "🤖"
        4 -> "🦁"
        else -> "👦"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFEF7FF),
                        Color(0xFFF3EDF7)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .scale(animScale.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                        .shadow(2.dp, CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF6750A4)
                    )
                }

                // App Title
                Text(
                    text = "RUN BOY RUN",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = 4.sp
                    ),
                    color = Color(0xFF6750A4)
                )

                // Profile button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color(0xFF6750A4), CircleShape)
                        .clickable { viewModel.navigateTo(Screen.PROFILE) }
                        .shadow(2.dp, CircleShape)
                        .testTag("profile_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.photoUri != null) {
                        AsyncImage(
                            model = profile.photoUri,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = avatarEmoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }

            // HERO HERO BANNER CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Custom Sunset background loaded safely
                    Image(
                        painter = painterResource(id = R.drawable.img_game_background),
                        contentDescription = "Game Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay to make text pop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )

                    // Hero Text
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Hello, ${profile.name}!",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "🏃", fontSize = 18.sp)
                        }
                        Text(
                            text = "Save the boy from dogs! Jump over rats!",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // LEVEL CHOOSER
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "select difficulty level".uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 3 Levels Cards Side-by-Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GameLevel.values().forEach { level ->
                        val isSelected = selectedLevel == level
                        val (title, color, emoji) = when (level) {
                            GameLevel.EASY -> Triple("EASY", Color(0xFF4CAF50), "🟢")
                            GameLevel.HARD -> Triple("HARD", Color(0xFFFF9800), "🔥")
                            GameLevel.SUPER -> Triple("SUPER", Color(0xFFE91E63), "⚡")
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(95.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) color.copy(alpha = 0.15f) else Color.White)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) color else Color(0xFFE7E0EC),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { viewModel.selectLevel(level) }
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (isSelected) color else Color.DarkGray
                                )
                                val currentScore = when (level) {
                                    GameLevel.EASY -> easyScore
                                    GameLevel.HARD -> hardScore
                                    GameLevel.SUPER -> superScore
                                }
                                Text(
                                    text = "Best: $currentScore",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // PLAY BUTTON
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isLevelConfirmed,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    val playButtonColor = when (selectedLevel) {
                        GameLevel.EASY -> Color(0xFF4CAF50)
                        GameLevel.HARD -> Color(0xFFFF9800)
                        GameLevel.SUPER -> Color(0xFFE91E63)
                    }

                    Button(
                        onClick = { viewModel.navigateTo(Screen.GAME) },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(60.dp)
                            .shadow(8.dp, RoundedCornerShape(30.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = playButtonColor),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PLAY MATCH (${selectedLevel.name})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. ACTIVE GAME PLAY SCREEN (Custom 2D Scrolling Canvas)
// -------------------------------------------------------------
@Composable
fun GamePlayScreen(
    viewModel: GameViewModel,
    settings: GameSettings,
    selectedLevel: GameLevel,
    profile: ProfileData
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Screen navigation back or restart
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }

    // High score for current level
    val levelHighScore = when (selectedLevel) {
        GameLevel.EASY -> viewModel.easyHighScore.collectAsState().value
        GameLevel.HARD -> viewModel.hardHighScore.collectAsState().value
        GameLevel.SUPER -> viewModel.superHighScore.collectAsState().value
    }

    // Canvas sizes & assets
    val bgImage = ImageBitmap.imageResource(id = R.drawable.img_game_background)

    // Boy avatar based on selection
    val boyEmoji = when (profile.avatarIndex) {
        1 -> "👧" // Girl
        2 -> "🥷" // Ninja
        3 -> "🤖" // Robot
        4 -> "🦁" // Lion
        else -> "👦" // Boy
    }

    // Theme assets and colors based on setting index
    val (groundColor, skyColor, trackColor) = when (settings.selectedTheme) {
        1 -> Triple(Color(0xFF0F051D), Color(0xFF080112), Color(0xFF00FFCC)) // Cyber grid theme
        2 -> Triple(Color(0xFFE5C158), Color(0xFFFFF2AF), Color(0xFFC49A28)) // Desert theme
        else -> Triple(Color(0xFF5D4037), Color(0xFFE3F2FD), Color(0xFF4CAF50)) // Forest theme
    }

    // --- GAME PHYSICS STATE ---
    var backgroundOffset by remember { mutableStateOf(0f) }
    var groundOffset by remember { mutableStateOf(0f) }

    // Boy physics
    var boyY by remember { mutableStateOf(0f) }
    var boyVelocityY by remember { mutableStateOf(0f) }
    var isJumping by remember { mutableStateOf(false) }
    var runningFrameTime by remember { mutableStateOf(0f) }
    var boyTripped by remember { mutableStateOf(false) }

    // Dog tracking
    var dogsX by remember { mutableStateOf(0f) }
    var dogsRunningTime by remember { mutableStateOf(0f) }

    // Rats list
    val rats = remember { mutableStateListOf<LiveRat>() }
    var ratIdCounter by remember { mutableStateOf(0L) }
    var nextSpawnTime by remember { mutableStateOf(1f) } // spawn countdown timer

    // FX Particles and text popups
    val particles = remember { mutableStateListOf<Particle>() }
    val scorePopups = remember { mutableStateListOf<ScorePopup>() }

    // Reset scores & variables on init
    LaunchedEffect(Unit) {
        viewModel.resetScoreSession()
        score = 0
        boyY = 0f
        boyVelocityY = 0f
        isJumping = false
        boyTripped = false
        dogsX = -100f // Starts a bit left
        rats.clear()
        particles.clear()
        scorePopups.clear()
        nextSpawnTime = 1.0f
    }

    // --- MAIN TICK LOOP (60 FPS) ---
    val isGameActive = !isPaused && !isGameOver
    var lastTime by remember { mutableStateOf(0L) }

    LaunchedEffect(key1 = isGameActive) {
        if (isGameActive) {
            lastTime = System.currentTimeMillis()
            while (true) {
                withFrameMillis { frameTime ->
                    val delta = ((frameTime - lastTime) / 1000f).coerceAtMost(0.05f)
                    lastTime = frameTime

                    // Get screen measurements (let's assume we read them from canvas density sizes)
                    // We will define default logic speeds scaled by deltaTime
                    val baseSpeed = when (selectedLevel) {
                        GameLevel.EASY -> 300f
                        GameLevel.HARD -> 480f
                        GameLevel.SUPER -> 650f
                    }

                    // Theme specific speed modifier
                    val speedModifier = 1.0f
                    val currentSpeed = if (boyTripped) 0f else baseSpeed * speedModifier

                    // 1. Scroll Backgrounds
                    backgroundOffset = (backgroundOffset + currentSpeed * 0.4f * delta) % 1200f
                    groundOffset = (groundOffset + currentSpeed * delta) % 1200f

                    // 2. Boy running frame cycle
                    runningFrameTime += delta * 12f

                    // 3. Boy Jump Physics
                    val gravity = 2200f
                    val groundLevelY = 0f // We treat ground Y as delta relative to draw height
                    if (isJumping) {
                        boyVelocityY += gravity * delta
                        boyY += boyVelocityY * delta
                        if (boyY >= groundLevelY) {
                            boyY = groundLevelY
                            boyVelocityY = 0f
                            isJumping = false
                        }
                    }

                    // Create running dust particles at boy's feet
                    if (!isJumping && !boyTripped && settings.particlesEnabled && Random.nextFloat() < 0.25f) {
                        particles.add(
                            Particle(
                                x = 120f + Random.nextFloat() * 40f, // Approx boy X position
                                y = groundLevelY + 10f + Random.nextFloat() * 10f,
                                vx = -100f - Random.nextFloat() * 150f,
                                vy = -40f - Random.nextFloat() * 60f,
                                color = if (settings.selectedTheme == 1) Color(0xFF00FFCC).copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.6f),
                                size = 6f + Random.nextFloat() * 8f,
                                life = 1f,
                                maxLife = 0.4f
                            )
                        )
                    }

                    // 4. Dogs Physics & animation
                    dogsRunningTime += delta * 15f
                    val dogsTargetX = if (boyTripped) 120f else 25f // dogs close the gap on trip
                    val approachSpeed = if (boyTripped) 180f else 80f
                    if (dogsX < dogsTargetX) {
                        dogsX = (dogsX + approachSpeed * delta).coerceAtMost(dogsTargetX)
                    } else if (dogsX > dogsTargetX) {
                        dogsX = (dogsX - approachSpeed * delta).coerceAtLeast(dogsTargetX)
                    }

                    // Check if dogs caught the boy
                    if (boyTripped && dogsX >= 110f) {
                        isGameOver = true
                        viewModel.updateScore(score)
                    }

                    // 5. Rats Spawning Logic
                    nextSpawnTime -= delta
                    if (nextSpawnTime <= 0f && !boyTripped) {
                        val size = 35f
                        val speedExtra = Random.nextFloat() * 100f
                        rats.add(
                            LiveRat(
                                id = ratIdCounter++,
                                x = 1000f, // Will be set relative to Canvas size right edge
                                y = groundLevelY,
                                size = size,
                                speedOffset = speedExtra
                            )
                        )
                        // Next spawn countdown
                        val (minSpawn, maxSpawn) = when (selectedLevel) {
                            GameLevel.EASY -> Pair(2.2f, 4.0f)
                            GameLevel.HARD -> Pair(1.4f, 2.6f)
                            GameLevel.SUPER -> Pair(0.8f, 1.6f)
                        }
                        nextSpawnTime = Random.nextFloat() * (maxSpawn - minSpawn) + minSpawn
                    }

                    // 6. Update existing Rats
                    val iterator = rats.iterator()
                    while (iterator.hasNext()) {
                        val rat = iterator.next()

                        if (!rat.passed) {
                            // Move left towards boy
                            rat.x -= (currentSpeed + 120f + rat.speedOffset) * delta

                            // Check boy collision
                            // Boy is roughly at x=130f, height/size=50f. Let's do accurate offset logic
                            val boyRealY = boyY // relative offset
                            val boySize = 50f
                            val ratSize = rat.size

                            // Bounding boxes check
                            val boyLeft = 110f
                            val boyRight = 150f
                            val boyTop = boyRealY - boySize
                            val boyBottom = boyRealY

                            val ratLeft = rat.x
                            val ratRight = rat.x + ratSize
                            val ratTop = rat.y - ratSize
                            val ratBottom = rat.y

                            // Collision check: overlapping boxes
                            if (!boyTripped && !isGameOver &&
                                boyLeft < ratRight && boyRight > ratLeft &&
                                boyTop < ratBottom && boyBottom > ratTop
                            ) {
                                boyTripped = true // Boy trips, dogs run forward
                            }

                            // Jumped over check
                            if (!boyTripped && rat.x + rat.size < 110f) {
                                rat.passed = true
                                // Give it high backwards tumble velocity
                                rat.backSpeed = baseSpeed * 1.8f
                            }
                        } else if (!rat.isEaten) {
                            // Rat flies backwards past the boy towards the dogs
                            rat.x -= (rat.backSpeed) * delta // Move further left to dogs (x ~ 20f-50f)

                            // Check dog eating collision
                            val dogEatenThresh = dogsX + 45f
                            if (rat.x <= dogEatenThresh) {
                                rat.isEaten = true
                                score += 10
                                viewModel.updateScore(score)

                                // Trigger eating score popup at dogs
                                scorePopups.add(
                                    ScorePopup(
                                        text = "+10",
                                        x = dogsX + 30f,
                                        y = groundLevelY - 40f,
                                        life = 1f,
                                        color = Color(0xFFFFEA00)
                                    )
                                )

                                // Spawn eating star/splat particles
                                if (settings.particlesEnabled) {
                                    repeat(10) {
                                        particles.add(
                                            Particle(
                                                x = dogsX + 30f,
                                                y = groundLevelY - 15f,
                                                vx = (Random.nextFloat() - 0.5f) * 200f,
                                                vy = -100f - Random.nextFloat() * 200f,
                                                color = Color(0xFFFFEA00),
                                                size = 4f + Random.nextFloat() * 6f,
                                                life = 1f,
                                                maxLife = 0.5f
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Remove eaten or offscreen rats
                    rats.removeAll { it.isEaten || it.x < -100f }

                    // 7. Update Particles
                    val pIterator = particles.iterator()
                    while (pIterator.hasNext()) {
                        val p = pIterator.next()
                        p.x += p.vx * delta
                        p.y += p.vy * delta
                        p.vy += 250f * delta // gravity on particles
                        p.life -= delta / p.maxLife
                        if (p.life <= 0f) {
                            pIterator.remove()
                        }
                    }

                    // 8. Update Score Popups
                    val sIterator = scorePopups.iterator()
                    while (sIterator.hasNext()) {
                        val pop = sIterator.next()
                        pop.y -= 80f * delta // float up
                        pop.life -= delta * 1.8f // fade out quickly
                        if (pop.life <= 0f) {
                            sIterator.remove()
                        }
                    }
                }
            }
        }
    }

    // --- RENDER GAME SCENE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                // TAP TO JUMP!
                if (isGameActive && !isJumping && !boyTripped) {
                    isJumping = true
                    boyVelocityY = -750f // Powerful jump force
                }
            }
    ) {
        // Core game Canvas viewport
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val viewWidth = maxWidth
            val viewHeight = maxHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val groundY = canvasH * 0.72f // ground is positioned at 72% screen height

                // --- 1. SKY DRAW ---
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            skyColor,
                            skyColor.copy(alpha = 0.7f)
                        )
                    )
                )

                // --- 2. SEAMLESS SCROLLING SUNSET BACKGROUNDS ---
                // We tile the parallax image safely
                val bgW = bgImage.width
                val bgH = bgImage.height
                val scaleFactor = (canvasH * 0.65f) / bgH
                val drawBgW = bgW * scaleFactor
                val drawBgH = canvasH * 0.65f

                // Draw parallax bg repeating twice
                val offset1 = -(backgroundOffset % drawBgW)
                drawImage(
                    image = bgImage,
                    dstOffset = IntOffset(offset1.toInt(), 0),
                    dstSize = IntSize(drawBgW.toInt(), drawBgH.toInt()),
                    alpha = 0.75f
                )
                drawImage(
                    image = bgImage,
                    dstOffset = IntOffset((offset1 + drawBgW).toInt(), 0),
                    dstSize = IntSize(drawBgW.toInt(), drawBgH.toInt()),
                    alpha = 0.75f
                )

                // --- 3. MOUNTAIN SILHOUETTES OR PARALLAX LAYER ---
                // In retro theme, we can draw neat neon mountains or city skylines!
                if (settings.selectedTheme == 1) {
                    val path = Path().apply {
                        moveTo(0f, groundY)
                        lineTo(canvasW * 0.15f, groundY - 180f)
                        lineTo(canvasW * 0.3f, groundY)
                        lineTo(canvasW * 0.45f, groundY - 260f)
                        lineTo(canvasW * 0.6f, groundY)
                        lineTo(canvasW * 0.75f, groundY - 150f)
                        lineTo(canvasW * 0.9f, groundY - 210f)
                        lineTo(canvasW, groundY)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF1E0E3D),
                        style = Fill
                    )
                }

                // --- 4. GROUND DRAW ---
                // Grass / dirt background
                drawRect(
                    color = groundColor,
                    topLeft = Offset(0f, groundY),
                    size = Size(canvasW, canvasH - groundY)
                )

                // High-fidelity running lane grid line (Sleek track)
                val trackHeight = 12.dp.toPx()
                drawRect(
                    color = trackColor,
                    topLeft = Offset(0f, groundY),
                    size = Size(canvasW, trackHeight)
                )

                // Subtle grid lines if cyber theme
                if (settings.selectedTheme == 1) {
                    for (i in 0..10) {
                        val lineX = ((i * (canvasW / 8)) - groundOffset) % canvasW
                        drawLine(
                            color = Color(0xFF00FFCC).copy(alpha = 0.3f),
                            start = Offset(lineX, groundY),
                            end = Offset(lineX + 120f, canvasH),
                            strokeWidth = 3f
                        )
                    }
                }

                // --- 5. DRAW PLAYERS (BOY & DOGS & RATS) ---
                // Setup scaled values
                val scale = canvasW / 480f // Logical virtual coordinate scale
                val boyPhysX = 130f * scale
                val boyPhysY = groundY + (boyY * scale)

                // 5a. Particles
                particles.forEach { p ->
                    drawCircle(
                        color = p.color.copy(alpha = p.life),
                        radius = p.size * scale * p.life,
                        center = Offset(p.x * scale, groundY + (p.y * scale))
                    )
                }

                // 5b. The Boy (👦)
                // Draw rotating/jumping animation
                val boyAngle = if (isJumping) (boyY * 1.5f) % 360f else if (boyTripped) -75f else 0f
                drawIntoCanvas { canvas ->
                    val textPaint = android.graphics.Paint().apply {
                        textSize = 42.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(boyPhysX, boyPhysY - 15.dp.toPx())
                    canvas.nativeCanvas.rotate(boyAngle)
                    canvas.nativeCanvas.drawText(boyEmoji, 0f, 12.dp.toPx(), textPaint)
                    canvas.nativeCanvas.restore()
                }

                // Draw boy energy bubble/trail if Super Level
                if (selectedLevel == GameLevel.SUPER && !boyTripped) {
                    drawCircle(
                        color = Color(0xFFE91E63).copy(alpha = 0.2f),
                        radius = 28.dp.toPx() + (sin(runningFrameTime * 0.5f) * 4f),
                        center = Offset(boyPhysX, boyPhysY - 15.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )
                }

                // 5c. The Dogs (🐕🐕)
                val dogPhysX = dogsX * scale
                val dogBobY = if (!boyTripped) sin(dogsRunningTime) * 6f else 0f
                val dogPhysY = groundY + (dogBobY * scale)

                drawIntoCanvas { canvas ->
                    val textPaint = android.graphics.Paint().apply {
                        textSize = 34.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    // Draw packing dogs
                    canvas.nativeCanvas.save()
                    canvas.nativeCanvas.translate(dogPhysX, dogPhysY - 14.dp.toPx())
                    canvas.nativeCanvas.drawText("🐕", 0f, 10.dp.toPx(), textPaint)
                    // Draw second puppy slightly back
                    canvas.nativeCanvas.drawText("🐕", -22.dp.toPx(), 12.dp.toPx(), textPaint)
                    canvas.nativeCanvas.restore()
                }

                // 5d. Rats (🐀)
                rats.forEach { rat ->
                    val ratPhysX = rat.x * scale
                    val ratPhysY = groundY + (rat.y * scale)
                    val ratAngle = if (rat.passed) (runningFrameTime * 15f) % 360f else 0f

                    drawIntoCanvas { canvas ->
                        val textPaint = android.graphics.Paint().apply {
                            textSize = 28.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.save()
                        canvas.nativeCanvas.translate(ratPhysX, ratPhysY - 10.dp.toPx())
                        if (rat.passed) {
                            canvas.nativeCanvas.rotate(ratAngle) // tumbles back backwards!
                        } else {
                            // flip horizontally to face left towards the boy
                            canvas.nativeCanvas.scale(-1f, 1f)
                        }
                        canvas.nativeCanvas.drawText("🐀", 0f, 8.dp.toPx(), textPaint)
                        canvas.nativeCanvas.restore()
                    }
                }

                // 5e. Score Popups
                scorePopups.forEach { pop ->
                    drawIntoCanvas { canvas ->
                        val textPaint = android.graphics.Paint().apply {
                            textSize = 22.dp.toPx()
                            color = pop.color.toArgb()
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        textPaint.alpha = (pop.life * 255).toInt().coerceIn(0, 255)
                        canvas.nativeCanvas.drawText(
                            pop.text,
                            pop.x * scale,
                            groundY + (pop.y * scale),
                            textPaint
                        )
                    }
                }
            }

            // --- HUD OVERLAY BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 40.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause trigger
                IconButton(
                    onClick = { isPaused = true },
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .shadow(2.dp, CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White)
                }

                // Score display card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    modifier = Modifier.shadow(2.dp, RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🐀 Eaten: ", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Text(
                            text = "$score",
                            color = Color(0xFFFFEA00),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // High score badge
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🏆 Best: $levelHighScore",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Hint instructions overlay
            if (score == 0 && rats.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 80.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "👆 TAP TO JUMP OVER THE RAT!",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // --- PAUSE DIALOG PANEL ---
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "GAME PAUSED",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6750A4)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Take a breath, runner!",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Resume button
                        Button(
                            onClick = { isPaused = false },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resume Game", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Restart Match
                        Button(
                            onClick = {
                                // restart physics completely
                                score = 0
                                boyY = 0f
                                boyVelocityY = 0f
                                isJumping = false
                                boyTripped = false
                                dogsX = -100f
                                rats.clear()
                                particles.clear()
                                scorePopups.clear()
                                isPaused = false
                                isGameOver = false
                                nextSpawnTime = 1.0f
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF625B71)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restart Match", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Exit to menu
                        TextButton(
                            onClick = { viewModel.navigateTo(Screen.MENU) }
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exit to Main Menu", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- GAME OVER PANEL ---
        if (isGameOver) {
            val isNewHighScore = score > levelHighScore

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🐾 CAUGHT BY DOGS!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE91E63)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The rats tripped you up!",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Score display
                        Text("FINAL SCORE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color(0xFF6750A4))

                        if (isNewHighScore) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFFEA00).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFFEA00), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎉 NEW HIGH SCORE!", color = Color(0xFFD81B60), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        } else {
                            Text("Best High Score: $levelHighScore", fontSize = 13.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Play again button
                        Button(
                            onClick = {
                                // restart physics completely
                                score = 0
                                boyY = 0f
                                boyVelocityY = 0f
                                isJumping = false
                                boyTripped = false
                                dogsX = -100f
                                rats.clear()
                                particles.clear()
                                scorePopups.clear()
                                isGameOver = false
                                nextSpawnTime = 1.0f
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Again", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Menu button
                        Button(
                            onClick = { viewModel.navigateTo(Screen.MENU) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7E0EC)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Main Menu", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. PROFILE SCREEN (Configure Name, Avatar Emoji, pick photo)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: GameViewModel,
    profile: ProfileData,
    easyScore: Int,
    hardScore: Int,
    superScore: Int
) {
    var nameText by remember { mutableStateOf(profile.name) }
    var selectedAvatarIdx by remember { mutableStateOf(profile.avatarIndex) }

    // Built-in game avatar symbols
    val avatarOptions = listOf(
        Pair("👦", "Gully Boy"),
        Pair("👧", "Pink Dash"),
        Pair("🥷", "Shadow"),
        Pair("🤖", "RoboX"),
        Pair("🦁", "Simba")
    )

    // Image Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateProfile(nameText, selectedAvatarIdx, uri.toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.updateProfile(nameText, selectedAvatarIdx, profile.photoUri)
                        viewModel.navigateTo(Screen.MENU)
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF6750A4))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Runner Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            }

            // Profile Picture Circle & Upload triggers
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEADDFF))
                    .border(3.dp, Color(0xFF6750A4), CircleShape)
                    .clickable { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profile.photoUri != null) {
                    AsyncImage(
                        model = profile.photoUri,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val currentEmoji = avatarOptions.getOrNull(selectedAvatarIdx)?.first ?: "👦"
                    Text(text = currentEmoji, fontSize = 56.sp)
                }

                // Add small camera overlay icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6750A4))
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick custom photo from gallery", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Name edit textfield
            OutlinedTextField(
                value = nameText,
                onValueChange = { nameText = it },
                label = { Text("Runner Nickname") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input"),
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    viewModel.updateProfile(nameText, selectedAvatarIdx, profile.photoUri)
                }),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selector for standard beautiful runner emojis
            Text(
                text = "CHOOSE RUNNER SPIRIT",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                avatarOptions.forEachIndexed { idx, pair ->
                    val isSelected = selectedAvatarIdx == idx && profile.photoUri == null
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF6750A4).copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF6750A4) else Color(0xFFE7E0EC),
                                shape = CircleShape
                            )
                            .clickable {
                                selectedAvatarIdx = idx
                                // Reset custom photo if they select a built-in avatar icon
                                viewModel.updateProfile(nameText, idx, null)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = pair.first, fontSize = 24.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // High scores panel card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personal Level Records", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3 level high scores listed elegantly
                    listOf(
                        Triple("🟢 Easy Runner Record", easyScore, Color(0xFF4CAF50)),
                        Triple("🔥 Hard Runner Record", hardScore, Color(0xFFFF9800)),
                        Triple("⚡ Super Runner Record", superScore, Color(0xFFE91E63))
                    ).forEach { (lbl, valScore, clr) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lbl, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "$valScore pts",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = clr
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save profile changes button
            Button(
                onClick = {
                    viewModel.updateProfile(nameText, selectedAvatarIdx, profile.photoUri)
                    viewModel.navigateTo(Screen.MENU)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
            ) {
                Text("SAVE PROFILE DATA", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// -------------------------------------------------------------
// 4. SETTINGS SCREEN
// -------------------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    settings: GameSettings
) {
    var soundChecked by remember { mutableStateOf(settings.soundEnabled) }
    var musicChecked by remember { mutableStateOf(settings.musicEnabled) }
    var particlesChecked by remember { mutableStateOf(settings.particlesEnabled) }
    var selectedThemeIdx by remember { mutableStateOf(settings.selectedTheme) }

    var showConfirmReset by remember { mutableStateOf(false) }

    val themes = listOf(
        Triple("Sunset Forest 🌲", Color(0xFF4CAF50), 0),
        Triple("Cyber Neon Grid 🪐", Color(0xFF00FFCC), 1),
        Triple("Dusty Desert 🏜️", Color(0xFFE5C158), 2)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.updateSettings(soundChecked, musicChecked, particlesChecked, selectedThemeIdx)
                        viewModel.navigateTo(Screen.MENU)
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF6750A4))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Game Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
            }

            // TOGGLES SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Audio & Visual Options".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Sound switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Sound Effects", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Jump & eat sound alerts", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = soundChecked,
                            onCheckedChange = { soundChecked = it }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE7E0EC))

                    // Music Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Background Ambient Sounds", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Calm environmental hum", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = musicChecked,
                            onCheckedChange = { musicChecked = it }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE7E0EC))

                    // Particles Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Trail & Eat Particles", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Rich gold sparkles & dust puffs", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Switch(
                            checked = particlesChecked,
                            onCheckedChange = { particlesChecked = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // THEME SELECTION PANEL
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Runner Track Themes".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    themes.forEach { (name, color, index) ->
                        val isSelected = selectedThemeIdx == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedThemeIdx = index }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.DarkGray
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF6750A4))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Danger / Reset stats area
            Button(
                onClick = { showConfirmReset = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDAD9)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFBA1A1A))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All High Scores", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Settings configuration
            Button(
                onClick = {
                    viewModel.updateSettings(soundChecked, musicChecked, particlesChecked, selectedThemeIdx)
                    viewModel.navigateTo(Screen.MENU)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
            ) {
                Text("APPLY SETTINGS CHANGES", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // RESET CONFIRMATION ALERT DIALOG
    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = { Text("Reset Personal High Scores?") },
            text = { Text("This will permanently clear your runner high score records for all levels. This action is irreversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllHighScores()
                        showConfirmReset = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                ) {
                    Text("Yes, Clear Records", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
