package com.haxtech.haxtracker.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.haxtech.haxtracker.core.engine.CourtPositionCalculator
import com.haxtech.haxtracker.core.engine.CourtSlot
import com.haxtech.haxtracker.core.engine.GameEngine
import com.haxtech.haxtracker.core.model.*
import com.haxtech.haxtracker.wear.presentation.theme.*
import com.haxtech.haxtracker.wear.sync.WatchSyncRepository
import kotlinx.coroutines.delay
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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

    LaunchedEffect(Unit) {
        WatchSyncRepository.checkInitialSyncState(context)
    }

    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    var showGestureHelp by remember { mutableStateOf(value = false) }
    var showSettings by remember { mutableStateOf(value = false) }

    val handleAction: (GameAction) -> Unit = { action ->
        WatchSyncRepository.sendActionToPhone(context, action) { fallbackAction ->
            val localUpdated = GameEngine.process(matchState, fallbackAction)
            WatchSyncRepository.updateLocalState(localUpdated)
        }
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
        AppScaffold {
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
                                            if (matchState.canUndo()) {
                                                handleAction(GameAction.Undo)
                                                gestureFeedback = "⬅️ Point Undone"
                                            }
                                        } else if (totalDragX > minSwipeDistance) {
                                            handleAction(GameAction.SwitchSidesManual)
                                            gestureFeedback = "➡️ Switched Sides"
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
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Header Bar: Status & Sport Title
                        HeaderSection(
                            matchState = matchState,
                            isConnectedToPhone = isConnectedToPhone
                        )

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
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 2.dp)
                        )

                        // 4. Footer Bar: Gesture Hints & Settings
                        GestureHintFooter(
                            teamAName = matchState.config.teamA.name,
                            teamBName = matchState.config.teamB.name,
                            onOpenSettings = { showSettings = true },
                            onToggleHelp = { showGestureHelp = true }
                        )
                    }

                    // Gesture Toast Feedback Overlay
                    AnimatedVisibility(
                        visible = gestureFeedback != null,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp)
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

                    // Modals
                    if (matchState.isGameFinished && !matchState.isMatchFinished) {
                        GameFinishedCard(
                            matchState = matchState,
                            onNextGame = { handleAction(GameAction.StartNextGame()) }
                        )
                    }

                    if (matchState.isMatchFinished) {
                        MatchFinishedCard(
                            matchState = matchState,
                            onRestart = { handleAction(GameAction.ResetMatch()) }
                        )
                    }

                    if (showGestureHelp) {
                        GestureHelpDialog(
                            teamAName = matchState.config.teamA.name,
                            teamBName = matchState.config.teamB.name,
                            onDismiss = { showGestureHelp = false }
                        )
                    }

                    if (showSettings) {
                        SettingsDialog(
                            currentConfig = matchState.config,
                            onSelectConfig = { newConfig ->
                                handleAction(GameAction.ResetMatch(newConfig))
                                showSettings = false
                            },
                            onOpenHelp = { showGestureHelp = true },
                            onDismiss = { showSettings = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    matchState: MatchState,
    isConnectedToPhone: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isConnectedToPhone) VoltGreen.copy(alpha = 0.2f) else DarkSurfaceElevated)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (isConnectedToPhone) "📱 SYNC" else "⌚ LOCAL",
                    color = if (isConnectedToPhone) VoltGreen else TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            val sportIcon = if (matchState.config.sport == Sport.BADMINTON) "🏸" else "🎾"
            Text(
                text = "$sportIcon ${matchState.config.sport.displayName.uppercase()}",
                color = VoltGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "G${matchState.currentGameIndex + 1}",
                    color = TextSecondary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (matchState.isMatchPoint) {
            Text(
                text = "MATCH POINT",
                color = VoltCoral,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        } else if (matchState.isGamePoint) {
            Text(
                text = "GAME POINT",
                color = VoltGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Team A Score Box (Tap Zone)
        TeamScoreBox(
            teamName = matchState.config.teamA.name,
            score = matchState.scoreTeamA,
            isServing = matchState.servingTeam == TeamSide.TEAM_A,
            serverNum = if (matchState.servingTeam == TeamSide.TEAM_A) matchState.serverNumber else 1,
            showServerNum = (matchState.config.sport == Sport.PICKLEBALL && matchState.config.format == MatchFormat.DOUBLES),
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // Team B Score Box (Tap Zone)
        TeamScoreBox(
            teamName = matchState.config.teamB.name,
            score = matchState.scoreTeamB,
            isServing = matchState.servingTeam == TeamSide.TEAM_B,
            serverNum = if (matchState.servingTeam == TeamSide.TEAM_B) matchState.serverNumber else 1,
            showServerNum = (matchState.config.sport == Sport.PICKLEBALL && matchState.config.format == MatchFormat.DOUBLES),
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
    serverNum: Int,
    showServerNum: Boolean,
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
            .padding(vertical = 2.dp, horizontal = 2.dp)
    ) {
        if (isServing) {
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
                    text = if (showServerNum) "S$serverNum" else "SERVE",
                    color = accentColor,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Spacer(modifier = Modifier.height(9.dp))
        }

        Text(
            text = score.toString(),
            color = if (isServing) accentColor else TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )

        Text(
            text = teamName.uppercase(),
            color = if (isServing) accentColor else TextSecondary,
            fontSize = 7.sp,
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
            .padding(3.dp)
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
                .size(18.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .border(width = if (isServer || isReceiver) 1.dp else 0.dp, color = Color.White, shape = CircleShape)
        ) {
            Text(
                text = if (isServer) "🎾" else player.shortName.take(2),
                fontSize = if (isServer) 8.sp else 7.sp,
                fontWeight = FontWeight.Bold,
                color = PitchBlack
            )
        }
        Text(
            text = player.name.take(6),
            fontSize = 7.sp,
            color = if (isServer) VoltGold else TextPrimary,
            fontWeight = if (isServer) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun GestureHintFooter(
    teamAName: String,
    teamBName: String,
    onOpenSettings: () -> Unit,
    onToggleHelp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⬆️ $teamAName  ⬇️ $teamBName  ⬅️ Undo",
            color = TextSecondary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable(onClick = onToggleHelp)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", color = VoltCyan, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GestureHelpDialog(
    teamAName: String,
    teamBName: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack.copy(alpha = 0.92f))
            .clickable(onClick = onDismiss)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, VoltCyan, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Text("🖐 GESTURES", color = VoltCyan, fontSize = 10.sp, fontWeight = FontWeight.Black)

            Text("⬆️ Swipe UP / Tap Left: +1 $teamAName", color = TextPrimary, fontSize = 9.sp)
            Text("⬇️ Swipe DOWN / Tap Right: +1 $teamBName", color = TextPrimary, fontSize = 9.sp)
            Text("⬅️ Swipe LEFT: Undo Point", color = TextPrimary, fontSize = 9.sp)
            Text("➡️ Swipe RIGHT: Switch Sides", color = TextPrimary, fontSize = 9.sp)

            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VoltCyan, contentColor = PitchBlack),
                modifier = Modifier.height(26.dp)
            ) {
                Text("GOT IT", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack.copy(alpha = 0.94f))
            .clickable(onClick = onDismiss)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, VoltGold, RoundedCornerShape(14.dp))
                .padding(8.dp)
        ) {
            Text("⚙ MATCH OPTIONS", color = VoltGold, fontSize = 10.sp, fontWeight = FontWeight.Black)

            ConfigOptionButton(
                label = "🖐 Gesture Guide",
                isSelected = false,
                onClick = {
                    onDismiss()
                    onOpenHelp()
                }
            )

            ConfigOptionButton(
                label = "🏸 Badminton Doubles",
                isSelected = (currentConfig.sport == Sport.BADMINTON && currentConfig.format == MatchFormat.DOUBLES),
                onClick = { onSelectConfig(MatchConfig.defaultBadmintonDoubles()) }
            )

            ConfigOptionButton(
                label = "🏸 Badminton Singles",
                isSelected = (currentConfig.sport == Sport.BADMINTON && currentConfig.format == MatchFormat.SINGLES),
                onClick = { onSelectConfig(MatchConfig.defaultBadmintonSingles()) }
            )

            ConfigOptionButton(
                label = "🎾 Pickleball Doubles",
                isSelected = (currentConfig.sport == Sport.PICKLEBALL && currentConfig.format == MatchFormat.DOUBLES),
                onClick = { onSelectConfig(MatchConfig.defaultPickleballDoubles()) }
            )

            ConfigOptionButton(
                label = "🎾 Pickleball Singles",
                isSelected = (currentConfig.sport == Sport.PICKLEBALL && currentConfig.format == MatchFormat.SINGLES),
                onClick = { onSelectConfig(MatchConfig.defaultPickleballSingles()) }
            )

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                modifier = Modifier.height(22.dp)
            ) {
                Text("Close", fontSize = 8.sp)
            }
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
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) VoltGreen.copy(alpha = 0.2f) else DarkSurfaceElevated)
            .border(
                1.dp,
                if (isSelected) VoltGreen else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) VoltGreen else TextPrimary,
            fontSize = 8.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun GameFinishedCard(
    matchState: MatchState,
    onNextGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack.copy(alpha = 0.9f))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, VoltGreen, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Text("🏸 GAME OVER", color = VoltGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(3.dp))

            val winnerName = if (matchState.gameWinner == TeamSide.TEAM_A) matchState.config.teamA.name else matchState.config.teamB.name
            Text(
                text = "$winnerName Wins Game ${matchState.currentGameIndex + 1}!",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onNextGame,
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = PitchBlack),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            ) {
                Text("Start Game ${matchState.currentGameIndex + 2}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MatchFinishedCard(
    matchState: MatchState,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack.copy(alpha = 0.9f))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, VoltGold, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Text("🏆 MATCH OVER", color = VoltGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(3.dp))

            val winnerName = if (matchState.matchWinner == TeamSide.TEAM_A) matchState.config.teamA.name else matchState.config.teamB.name
            Text(
                text = "$winnerName Wins Match!",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${matchState.scoreTeamA} - ${matchState.scoreTeamB}",
                color = TextSecondary,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = PitchBlack),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            ) {
                Text("Play Again", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun DefaultPreview() {
    WearApp()
}
