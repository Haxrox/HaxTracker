package com.haxtech.haxtracker.ui.match

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.haxtech.haxtracker.core.model.*
import com.haxtech.haxtracker.ui.court.CourtVisualizer
import com.haxtech.haxtracker.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun MatchScreen(
    viewModel: MatchViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.matchState.collectAsState()
    val showSpectatorDialog = remember { mutableStateOf(false) }
    val showGestureHelp = remember { mutableStateOf(false) }

    var gestureFeedback by remember { mutableStateOf<String?>(null) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    val isFinished = state.isGameFinished || state.isMatchFinished

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(1200)
            gestureFeedback = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
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
                                viewModel.onAction(GameAction.PointTeamA)
                                gestureFeedback = "+1 ${state.config.teamA.name}"
                            } else if (totalDragY > minSwipeDistance) {
                                viewModel.onAction(GameAction.PointTeamB)
                                gestureFeedback = "+1 ${state.config.teamB.name}"
                            }
                        } else {
                            if (totalDragX < -minSwipeDistance) {
                                if (state.canRedo()) {
                                    viewModel.onAction(GameAction.Redo)
                                    gestureFeedback = "⬅️ Point Redone"
                                }
                            } else if (totalDragX > minSwipeDistance) {
                                if (state.canUndo()) {
                                    viewModel.onAction(GameAction.Undo)
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
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Text("←", color = TextPrimary, fontSize = 24.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.config.teamA.name.uppercase()} vs ${state.config.teamB.name.uppercase()}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.config.sport.displayName,
                        color = VoltGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            viewModel.onAction(GameAction.Undo)
                            gestureFeedback = "⬅️ Point Undone"
                        },
                        enabled = state.canUndo()
                    ) {
                        Text("⟲", color = if (state.canUndo()) VoltCoral else TextDisabled, fontSize = 22.sp)
                    }
                    IconButton(
                        onClick = {
                            viewModel.onAction(GameAction.Redo)
                            gestureFeedback = "➡️ Point Redone"
                        },
                        enabled = state.canRedo()
                    ) {
                        Text("⟳", color = if (state.canRedo()) VoltCyan else TextDisabled, fontSize = 22.sp)
                    }
                    IconButton(onClick = { showGestureHelp.value = true }) {
                        Text("🖐", fontSize = 20.sp)
                    }
                    IconButton(onClick = { showSpectatorDialog.value = true }) {
                        Text("📡", fontSize = 20.sp)
                    }
                }
            }

            // Main Score Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // TEAM A
                ScoreTapZone(
                    teamName = state.config.teamA.name,
                    score = state.scoreTeamA,
                    isServing = state.servingTeam == TeamSide.TEAM_A,
                    isWinner = state.gameWinner == TeamSide.TEAM_A,
                    serverNum = if (state.servingTeam == TeamSide.TEAM_A) state.serverNumber.toString() else "",
                    color = VoltGreen,
                    onTap = {
                        if (!isFinished) {
                            viewModel.onAction(GameAction.PointTeamA)
                            gestureFeedback = "+1 ${state.config.teamA.name}"
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(DarkBorder)
                )

                // TEAM B
                ScoreTapZone(
                    teamName = state.config.teamB.name,
                    score = state.scoreTeamB,
                    isServing = state.servingTeam == TeamSide.TEAM_B,
                    isWinner = state.gameWinner == TeamSide.TEAM_B,
                    serverNum = if (state.servingTeam == TeamSide.TEAM_B) state.serverNumber.toString() else "",
                    color = VoltCyan,
                    onTap = {
                        if (!isFinished) {
                            viewModel.onAction(GameAction.PointTeamB)
                            gestureFeedback = "+1 ${state.config.teamB.name}"
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Gesture hint bar above court
            Text(
                text = "⬆️ ${state.config.teamA.name}   ⬇️ ${state.config.teamB.name}   ➡️ Undo   ⬅️ Redo",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGestureHelp.value = true }
                    .padding(vertical = 4.dp)
            )

            // Court Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(DarkSurfaceElevated)
                    .padding(horizontal = 8.dp)
            ) {
                CourtVisualizer(matchState = state)
            }
        }

        // Gesture Feedback Toast Overlay
        AnimatedVisibility(
            visible = gestureFeedback != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(VoltGreen.copy(alpha = 0.95f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = gestureFeedback ?: "",
                    color = PitchBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Overlays
        if (state.isGameFinished && !state.isMatchFinished) {
            GameFinishedDialog(
                matchState = state,
                onNextGame = { viewModel.onAction(GameAction.StartNextGame()) }
            )
        }

        if (state.isMatchFinished) {
            MatchFinishedDialog(
                matchState = state,
                onRestart = { viewModel.onAction(GameAction.ResetMatch()) },
                onExit = onNavigateBack
            )
        }

        if (showSpectatorDialog.value) {
            SpectatorDialog(
                matchState = state,
                onDismiss = { showSpectatorDialog.value = false }
            )
        }

        if (showGestureHelp.value) {
            GestureHelpDialog(
                teamAName = state.config.teamA.name,
                teamBName = state.config.teamB.name,
                onDismiss = { showGestureHelp.value = false }
            )
        }
    }
}

@Composable
private fun ScoreTapZone(
    teamName: String,
    score: Int,
    isServing: Boolean,
    isWinner: Boolean,
    serverNum: String,
    color: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = color, radius = 200.dp),
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isWinner) {
                Text("👑", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                AnimatedVisibility(
                    visible = isServing,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (serverNum.isNotEmpty()) "SERVER $serverNum" else "SERVING",
                            color = color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = score.toString(),
                color = if (isWinner) VoltGold else if (isServing) color else TextPrimary,
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = teamName.uppercase(),
                color = if (isWinner) VoltGold else TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun GameFinishedDialog(
    matchState: MatchState,
    onNextGame: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoltGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏸", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                val winnerName = if (matchState.gameWinner == TeamSide.TEAM_A) matchState.config.teamA.name else matchState.config.teamB.name
                Text(
                    text = "GAME OVER",
                    color = VoltGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "$winnerName Wins Game ${matchState.currentGameIndex + 1}!",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onNextGame,
                    colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = PitchBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Next Game", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MatchFinishedDialog(
    matchState: MatchState,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoltGold),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MATCH COMPLETED",
                    color = VoltGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                val winner = if (matchState.matchWinner == TeamSide.TEAM_A) matchState.config.teamA else matchState.config.teamB
                Text(
                    text = "${winner.name} Wins!",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Score summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "FINAL SCORE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${matchState.config.teamA.name} ${matchState.gameWinsTeamA} - ${matchState.gameWinsTeamB} ${matchState.config.teamB.name}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = PitchBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Play Again", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Home", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpectatorDialog(
    matchState: MatchState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📡 LIVE SPECTATOR ACCESS", color = VoltCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "iOS and Android players can view live scores on the court by scanning this code.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // High Contrast QR representation
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📱 SCAN ON COURT", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "http://haxtracker.local\n/live/${matchState.config.sport.name.lowercase()}",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Live Score: ${matchState.calloutScore}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun GestureHelpDialog(
    teamAName: String,
    teamBName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoltCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🖐 GESTURE CONTROLS",
                    color = VoltCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Swipe anywhere on the screen to control the match:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⬆️ Swipe UP: +1 $teamAName", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("⬇️ Swipe DOWN: +1 $teamBName", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("➡️ Swipe RIGHT: Undo Point", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("⬅️ Swipe LEFT: Redo Point", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = VoltCyan, contentColor = PitchBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GOT IT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
