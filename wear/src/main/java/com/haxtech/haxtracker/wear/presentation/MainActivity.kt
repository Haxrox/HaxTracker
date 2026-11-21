package com.haxtech.haxtracker.wear.presentation

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.ScrollIndicatorDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.haxtech.haxtracker.core.engine.CourtPositionCalculator
import com.haxtech.haxtracker.core.engine.CourtSlot
import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.*
import com.haxtech.haxtracker.wear.R
import com.haxtech.haxtracker.wear.audio.ScoreTtsAnnouncer
import com.haxtech.haxtracker.wear.presentation.theme.*
import com.haxtech.haxtracker.wear.sync.WatchSyncRepository
import kotlinx.coroutines.delay
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val matchState by WatchSyncRepository.matchState.collectAsState()
    val isConnectedToPhone by WatchSyncRepository.isConnectedToPhone.collectAsState()

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000L)
        showSplash = false
    }

    val announcer = remember { ScoreTtsAnnouncer(context) }
    DisposableEffect(Unit) {
        onDispose { announcer.shutdown() }
    }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var lastVibratedScoreA by remember { mutableIntStateOf(matchState.scoreTeamA) }
    var lastVibratedScoreB by remember { mutableIntStateOf(matchState.scoreTeamB) }
    var lastVibratedGameIndex by remember { mutableIntStateOf(matchState.currentGameIndex) }

    LaunchedEffect(matchState.scoreTeamA, matchState.scoreTeamB, matchState.currentGameIndex) {
        if (matchState.scoreTeamA != lastVibratedScoreA || 
            matchState.scoreTeamB != lastVibratedScoreB || 
            matchState.currentGameIndex != lastVibratedGameIndex) {
            
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                
                if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                    announcer.speak(matchState.spokenAnnouncement)
                }
            }
            
            lastVibratedScoreA = matchState.scoreTeamA
            lastVibratedScoreB = matchState.scoreTeamB
            lastVibratedGameIndex = matchState.currentGameIndex
        }
    }

    LaunchedEffect(Unit) {
        WatchSyncRepository.checkInitialSyncState(context)
    }

    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    var showGestureHelp by remember { mutableStateOf(value = false) }
    var showSettings by remember { mutableStateOf(value = false) }

    val handleAction: (GameAction) -> Unit = { action ->
        // 1. Update local state immediately for responsiveness
        val localUpdated = GameEngine.process(matchState, action)
        WatchSyncRepository.updateLocalState(context, localUpdated)

        // 2. Send action to phone for authority
        WatchSyncRepository.sendActionToPhone(context, action)
    }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(1200)
            gestureFeedback = null
        }
    }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    val isFinished = matchState.isGameFinished || matchState.isMatchFinished

    HaxTrackerTheme {
        AnimatedContent(
            targetState = showSplash,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "SplashToMainTransition"
        ) { isSplash ->
            if (isSplash) {
                SplashScreen()
            } else {
                AppScaffold(
                    containerColor = PitchBlack
                ) {
                    when {
                        showSettings -> {
                            BackHandler { showSettings = false }
                            SettingsDialog(
                                currentConfig = matchState.config,
                                onSelectConfig = { newConfig ->
                                    handleAction(GameAction.ResetMatch(newConfig))
                                    showSettings = false
                                },
                                onOpenHelp = {
                                    showSettings = false
                                    showGestureHelp = true
                                },
                                onDismiss = { showSettings = false }
                            )
                        }
                        showGestureHelp -> {
                            BackHandler { showGestureHelp = false }
                            GestureHelpDialog(
                                teamAName = matchState.config.teamA.name,
                                teamBName = matchState.config.teamB.name,
                                onDismiss = { showGestureHelp = false }
                            )
                        }
                        matchState.isMatchFinished -> {
                            MatchFinishedDialog(
                                matchState = matchState,
                                onRestart = { handleAction(GameAction.ResetMatch()) }
                            )
                        }
                        matchState.isGameFinished -> {
                            GameFinishedDialog(
                                matchState = matchState,
                                onNextGame = { handleAction(GameAction.StartNextGame()) }
                            )
                        }
                        else -> {
                            ScreenScaffold { contentPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PitchBlack)
                                        .padding(contentPadding)
                                        .pointerInput(isFinished) {
                                            if (isFinished) return@pointerInput
                                            detectDragGestures(
                                                onDragStart = {
                                                    totalDragX = 0f
                                                    totalDragY = 0f
                                                },
                                                onDragEnd = {
                                                    val minSwipeDistance = 30f
                                                    if (abs(totalDragY) > abs(totalDragX)) {
                                                        if (totalDragY < -minSwipeDistance) {
                                                            handleAction(GameAction.PointTeamA)
                                                            gestureFeedback = "+1 ${matchState.config.teamA.name}"
                                                        } else if (totalDragY > minSwipeDistance) {
                                                            handleAction(GameAction.PointTeamB)
                                                            gestureFeedback = "+1 ${matchState.config.teamB.name}"
                                                        }
                                                    } else {
                                                        if (totalDragX < -minSwipeDistance) {
                                                            if (matchState.canRedo()) {
                                                                handleAction(GameAction.Redo)
                                                                gestureFeedback = "⬅️ Point Redone"
                                                            }
                                                        } else if (totalDragX > minSwipeDistance) {
                                                            if (matchState.canUndo()) {
                                                                handleAction(GameAction.Undo)
                                                                gestureFeedback = "➡️ Point Undone"
                                                            }
                                                        }
                                                    }
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                totalDragX += dragAmount.x
                                                totalDragY += dragAmount.y
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // 1. Header Bar: Status & Sport Title
                                        HeaderSection(matchState = matchState)

                                        // 2. Scoreboard Section
                                        CompactScoreBoard(
                                            matchState = matchState,
                                            onAction = handleAction,
                                            onGestureFeedback = { feedback -> gestureFeedback = feedback }
                                        )

                                        // 3. 2D Mini Court Section (Combined on the same screen)
                                        MiniCourtSection(
                                            matchState = matchState,
                                            modifier = Modifier
                                                .fillMaxWidth(0.88f)
                                                .weight(1f)
                                                .padding(vertical = 1.dp)
                                        )

                                        // 4. Footer Bar: Gesture Hints & Settings
                                        GestureHintFooter(
                                            onOpenSettings = { showSettings = true },
                                            onNewMatch = { handleAction(GameAction.ResetMatch()) },
                                            onSwitchSides = { handleAction(GameAction.SwitchSidesManual) }
                                        )
                                    }

                                    // Gesture Toast Feedback Overlay
                                    AnimatedVisibility(
                                        visible = gestureFeedback != null,
                                        enter = fadeIn() + scaleIn(),
                                        exit = fadeOut() + scaleOut(),
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(VoltGreen.copy(alpha = 0.95f))
                                                .padding(horizontal = 10.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = gestureFeedback ?: "",
                                                color = PitchBlack,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    matchState: MatchState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val sportIcon = if (matchState.config.sport == Sport.BADMINTON) "🏸" else "🎾"
            Text(
                text = "$sportIcon ${matchState.config.sport.displayName.uppercase()}",
                color = VoltGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "G${matchState.currentGameIndex + 1}",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CompactScoreBoard(
    matchState: MatchState,
    onAction: (GameAction) -> Unit,
    onGestureFeedback: (String) -> Unit
) {
    val isFinished = matchState.isGameFinished || matchState.isMatchFinished

    Row(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Team A Score Box (Tap Zone)
        val serverLabelA = if (matchState.servingTeam == TeamSide.TEAM_A) {
            val label = if (matchState.config.sport == Sport.PICKLEBALL && matchState.config.format == MatchFormat.DOUBLES) "S${matchState.serverNumber}" else "Serve"
            val id = if (matchState.servingPlayer.id == matchState.config.teamA.player1.id) "A1" else "A2"
            "$label: $id"
        } else null

        TeamScoreBox(
            teamName = matchState.config.teamA.name,
            score = matchState.scoreTeamA,
            isServing = matchState.servingTeam == TeamSide.TEAM_A,
            serverLabel = serverLabelA,
            accentColor = VoltGreen,
            onTap = {
                if (!isFinished) {
                    onAction(GameAction.PointTeamA)
                    onGestureFeedback("+1 ${matchState.config.teamA.name}")
                }
            },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = ":",
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 1.dp)
        )

        // Team B Score Box (Tap Zone)
        val serverLabelB = if (matchState.servingTeam == TeamSide.TEAM_B) {
            val label = if (matchState.config.sport == Sport.PICKLEBALL && matchState.config.format == MatchFormat.DOUBLES) "S${matchState.serverNumber}" else "Serve"
            val id = if (matchState.servingPlayer.id == matchState.config.teamB.player1.id) "B1" else "B2"
            "$label: $id"
        } else null

        TeamScoreBox(
            teamName = matchState.config.teamB.name,
            score = matchState.scoreTeamB,
            isServing = matchState.servingTeam == TeamSide.TEAM_B,
            serverLabel = serverLabelB,
            accentColor = VoltCyan,
            onTap = {
                if (!isFinished) {
                    onAction(GameAction.PointTeamB)
                    onGestureFeedback("+1 ${matchState.config.teamB.name}")
                }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TeamScoreBox(
    teamName: String,
    score: Int,
    isServing: Boolean,
    serverLabel: String?,
    accentColor: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isServing) accentColor.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onTap)
            .padding(vertical = 1.dp, horizontal = 2.dp)
    ) {
        if (isServing && serverLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = serverLabel,
                    color = accentColor,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = score.toString(),
            color = if (isServing) accentColor else TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Text(
            text = teamName.uppercase(),
            color = if (isServing) accentColor else TextSecondary,
            fontSize = 6.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MiniCourtSection(
    matchState: MatchState,
    modifier: Modifier = Modifier
) {
    val renderModel = CourtPositionCalculator.calculate(matchState)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F2027))
            .border(1.dp, VoltCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(2.dp)
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val netY = height / 2f
            val centerX = width / 2f

            // Net line
            drawLine(color = Color.White, start = Offset(0f, netY), end = Offset(width, netY), strokeWidth = 1.5.dp.toPx())
            // Center line
            drawLine(color = Color.White.copy(alpha = 0.35f), start = Offset(centerX, 0f), end = Offset(centerX, height), strokeWidth = 1.dp.toPx())
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Far Court (Top Team)
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    MiniPlayerBadge(renderModel.topLeftPlayer, renderModel.activeServerSlot == CourtSlot.TOP_LEFT, renderModel.activeReceiverSlot == CourtSlot.TOP_LEFT, VoltCyan)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    MiniPlayerBadge(renderModel.topRightPlayer, renderModel.activeServerSlot == CourtSlot.TOP_RIGHT, renderModel.activeReceiverSlot == CourtSlot.TOP_RIGHT, VoltCyan)
                }
            }

            // Near Court (Bottom Team)
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    MiniPlayerBadge(renderModel.bottomLeftPlayer, renderModel.activeServerSlot == CourtSlot.BOTTOM_LEFT, renderModel.activeReceiverSlot == CourtSlot.BOTTOM_LEFT, VoltGreen)
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    MiniPlayerBadge(renderModel.bottomRightPlayer, renderModel.activeServerSlot == CourtSlot.BOTTOM_RIGHT, renderModel.activeReceiverSlot == CourtSlot.BOTTOM_RIGHT, VoltGreen)
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerBadge(
    player: Player?,
    isServer: Boolean,
    isReceiver: Boolean,
    teamColor: Color
) {
    if (player == null) return
    val badgeColor = if (isServer) VoltGold else if (isReceiver) Color(0xFF38BDF8) else teamColor

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .border(width = if (isServer || isReceiver) 1.dp else 0.dp, color = Color.White, shape = CircleShape)
        ) {
            Text(
                text = if (isServer) "🏸" else player.shortName,
                fontSize = if (isServer) 7.sp else 6.sp,
                fontWeight = FontWeight.Bold,
                color = PitchBlack
            )
        }
        Text(
            text = player.name.take(5),
            fontSize = 6.sp,
            color = if (isServer) VoltGold else TextPrimary,
            fontWeight = if (isServer) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun GestureHintFooter(
    onOpenSettings: () -> Unit,
    onNewMatch: () -> Unit,
    onSwitchSides: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .clickable(onClick = onNewMatch),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = VoltGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .clickable(onClick = onSwitchSides),
            contentAlignment = Alignment.Center
        ) {
            Text("⇄", color = VoltCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", color = VoltCyan, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GestureHelpDialog(
    teamAName: String,
    teamBName: String,
    onDismiss: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = null
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack)
        ) {
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .rotaryScrollable(
                        behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                        focusRequester = focusRequester
                    )
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                item {
                    Text(
                        text = "🖐 GESTURES",
                        color = VoltCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    GestureHelpItem(
                        action = "Swipe UP / Tap Left",
                        result = "+1 $teamAName"
                    )
                }

                item {
                    GestureHelpItem(
                        action = "Swipe DOWN / Tap Right",
                        result = "+1 $teamBName"
                    )
                }

                item {
                    GestureHelpItem(
                        action = "Swipe RIGHT",
                        result = "Undo Point"
                    )
                }

                item {
                    GestureHelpItem(
                        action = "Swipe LEFT",
                        result = "Redo Point"
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = VoltCyan, contentColor = PitchBlack),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(32.dp)
                    ) {
                        Text("GOT IT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            ScrollIndicator(
                state = listState,
                colors = ScrollIndicatorDefaults.colors(
                    indicatorColor = VoltCyan,
                    trackColor = DarkBorder
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun GestureHelpItem(
    action: String,
    result: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 6.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(action, color = TextSecondary, fontSize = 8.5.sp, textAlign = TextAlign.Center)
            Text(result, color = TextPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SettingsDialog(
    currentConfig: MatchConfig,
    onSelectConfig: (MatchConfig) -> Unit,
    onOpenHelp: () -> Unit,
    onDismiss: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = null
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack)
        ) {
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .rotaryScrollable(
                        behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                        focusRequester = focusRequester
                    )
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                item {
                    Text(
                        text = "⚙ MATCH OPTIONS",
                        color = VoltGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    ConfigOptionButton(
                        label = "🖐 Gesture Guide",
                        isSelected = false,
                        onClick = {
                            onDismiss()
                            onOpenHelp()
                        }
                    )
                }

                item {
                    ConfigOptionButton(
                        label = "🏸 Badminton Doubles",
                        isSelected = (currentConfig.sport == Sport.BADMINTON && currentConfig.format == MatchFormat.DOUBLES),
                        onClick = { onSelectConfig(MatchConfig.defaultBadmintonDoubles()) }
                    )
                }

                item {
                    ConfigOptionButton(
                        label = "🏸 Badminton Singles",
                        isSelected = (currentConfig.sport == Sport.BADMINTON && currentConfig.format == MatchFormat.SINGLES),
                        onClick = { onSelectConfig(MatchConfig.defaultBadmintonSingles()) }
                    )
                }

                item {
                    ConfigOptionButton(
                        label = "🎾 Pickleball Doubles",
                        isSelected = (currentConfig.sport == Sport.PICKLEBALL && currentConfig.format == MatchFormat.DOUBLES),
                        onClick = { onSelectConfig(MatchConfig.defaultPickleballDoubles()) }
                    )
                }

                item {
                    ConfigOptionButton(
                        label = "🎾 Pickleball Singles",
                        isSelected = (currentConfig.sport == Sport.PICKLEBALL && currentConfig.format == MatchFormat.SINGLES),
                        onClick = { onSelectConfig(MatchConfig.defaultPickleballSingles()) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(32.dp)
                    ) {
                        Text("Close", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            ScrollIndicator(
                state = listState,
                colors = ScrollIndicatorDefaults.colors(
                    indicatorColor = VoltCyan,
                    trackColor = DarkBorder
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun ConfigOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) VoltGreen.copy(alpha = 0.2f) else DarkSurfaceElevated)
            .border(
                1.dp,
                if (isSelected) VoltGreen else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) VoltGreen else TextPrimary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GameFinishedDialog(
    matchState: MatchState,
    onNextGame: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = null
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack)
        ) {
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .rotaryScrollable(
                        behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                        focusRequester = focusRequester
                    )
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                item {
                    Text(
                        text = "🏸 GAME OVER",
                        color = VoltGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    val winnerName = if (matchState.gameWinner == TeamSide.TEAM_A) matchState.config.teamA.name else matchState.config.teamB.name
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, VoltGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$winnerName Wins!",
                                color = VoltGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Game ${matchState.currentGameIndex + 1} Complete",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onNextGame,
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = PitchBlack),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(34.dp)
                    ) {
                        Text(
                            text = "Start Game ${matchState.currentGameIndex + 2}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            ScrollIndicator(
                state = listState,
                colors = ScrollIndicatorDefaults.colors(
                    indicatorColor = VoltGreen,
                    trackColor = DarkBorder
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun MatchFinishedDialog(
    matchState: MatchState,
    onRestart: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = null
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlack)
        ) {
            ScalingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .rotaryScrollable(
                        behavior = RotaryScrollableDefaults.behavior(scrollableState = listState),
                        focusRequester = focusRequester
                    )
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                item {
                    Text(
                        text = "🏆 MATCH OVER",
                        color = VoltGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                item {
                    val winnerName = if (matchState.matchWinner == TeamSide.TEAM_A) matchState.config.teamA.name else matchState.config.teamB.name
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, VoltGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$winnerName Wins Match!",
                                color = VoltGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${matchState.scoreTeamA} - ${matchState.scoreTeamB}",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = PitchBlack),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(34.dp)
                    ) {
                        Text(
                            text = "Play Again",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            ScrollIndicator(
                state = listState,
                colors = ScrollIndicatorDefaults.colors(
                    indicatorColor = VoltGold,
                    trackColor = DarkBorder
                ),
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@WearPreviewDevices
@Composable
fun DefaultPreview() {
    WearApp()
}

@WearPreviewDevices
@Composable
fun SettingsPreview() {
    HaxTrackerTheme {
        AppScaffold {
            SettingsDialog(
                currentConfig = MatchConfig.defaultBadmintonDoubles(),
                onSelectConfig = {},
                onOpenHelp = {},
                onDismiss = {}
            )
        }
    }
}

@WearPreviewDevices
@Composable
fun GestureHelpPreview() {
    HaxTrackerTheme {
        AppScaffold {
            GestureHelpDialog(
                teamAName = "Team Alpha",
                teamBName = "Team Beta",
                onDismiss = {}
            )
        }
    }
}

@WearPreviewDevices
@Composable
fun GameFinishedPreview() {
    val base = GameEngine.newMatch(MatchConfig.defaultBadmintonDoubles())
    HaxTrackerTheme {
        AppScaffold {
            GameFinishedDialog(
                matchState = base.copy(
                    isGameFinished = true,
                    gameWinner = TeamSide.TEAM_A
                ),
                onNextGame = {}
            )
        }
    }
}

@WearPreviewDevices
@Composable
fun MatchFinishedPreview() {
    val base = GameEngine.newMatch(MatchConfig.defaultBadmintonDoubles())
    HaxTrackerTheme {
        AppScaffold {
            MatchFinishedDialog(
                matchState = base.copy(
                    isMatchFinished = true,
                    matchWinner = TeamSide.TEAM_A,
                    scoreTeamA = 21,
                    scoreTeamB = 18
                ),
                onRestart = {}
            )
        }
    }
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier
) {
    val iconPainter = rememberAppIconPainter()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = iconPainter,
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
private fun rememberAppIconPainter(): Painter {
    val context = LocalContext.current
    return remember(context) {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
            ?: ContextCompat.getDrawable(context, R.mipmap.ic_launcher)

        if (drawable != null) {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 48
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 48
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            BitmapPainter(bitmap.asImageBitmap())
        } else {
            ColorPainter(Color.Transparent)
        }
    }
}

@WearPreviewDevices
@Composable
fun SplashScreenPreview() {
    HaxTrackerTheme {
        SplashScreen()
    }
}
