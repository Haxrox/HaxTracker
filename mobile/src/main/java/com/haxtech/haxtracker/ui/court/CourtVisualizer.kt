package com.haxtech.haxtracker.ui.court

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haxtech.haxtracker.core.engine.CourtPositionCalculator
import com.haxtech.haxtracker.core.engine.CourtRenderModel
import com.haxtech.haxtracker.core.engine.CourtSlot
import com.haxtech.haxtracker.core.model.MatchState
import com.haxtech.haxtracker.core.model.Player
import com.haxtech.haxtracker.core.model.Sport
import com.haxtech.haxtracker.core.model.TeamSide
import com.haxtech.haxtracker.ui.theme.*

@Composable
fun CourtVisualizer(
    matchState: MatchState,
    modifier: Modifier = Modifier
) {
    val renderModel = CourtPositionCalculator.calculate(matchState)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Court Header with Server Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${matchState.config.sport.displayName.uppercase()} COURT",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (matchState.servingTeam == TeamSide.TEAM_A) VoltGreen else VoltCyan)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Serving: ${matchState.servingPlayer.name}",
                    color = VoltGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2D Court Canvas and Player Nodes
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.7f)
                .clip(RoundedCornerShape(12.dp))
                .background(CourtBackground)
                .border(2.dp, CourtLines, RoundedCornerShape(12.dp))
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()

            Canvas(modifier = Modifier.fillMaxSize()) {
                val netY = height / 2f
                val centerX = width / 2f
                val kitchenOffset = height * 0.18f

                // Far court kitchen line
                drawLine(color = CourtLines, start = Offset(0f, netY - kitchenOffset), end = Offset(width, netY - kitchenOffset), strokeWidth = 2.dp.toPx())
                // Near court kitchen line
                drawLine(color = CourtLines, start = Offset(0f, netY + kitchenOffset), end = Offset(width, netY + kitchenOffset), strokeWidth = 2.dp.toPx())
                // Far court center line
                drawLine(color = CourtLines, start = Offset(centerX, 0f), end = Offset(centerX, netY - kitchenOffset), strokeWidth = 2.dp.toPx())
                // Near court center line
                drawLine(color = CourtLines, start = Offset(centerX, netY + kitchenOffset), end = Offset(centerX, height), strokeWidth = 2.dp.toPx())
                // Center NET
                drawLine(color = Color.White, start = Offset(0f, netY), end = Offset(width, netY), strokeWidth = 4.dp.toPx())
                drawLine(color = Color.Black, start = Offset(0f, netY), end = Offset(width, netY), strokeWidth = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f))
            }

            val infiniteTransition = rememberInfiniteTransition(label = "server_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f, targetValue = 1.15f,
                animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                label = "pulse_scale"
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Team Row (Team B by default)
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
                        PlayerBadge(renderModel.topLeftPlayer, renderModel.activeServerSlot == CourtSlot.TOP_LEFT, renderModel.activeReceiverSlot == CourtSlot.TOP_LEFT, VoltCyan, pulseScale, matchState.config.sport)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
                        PlayerBadge(renderModel.topRightPlayer, renderModel.activeServerSlot == CourtSlot.TOP_RIGHT, renderModel.activeReceiverSlot == CourtSlot.TOP_RIGHT, VoltCyan, pulseScale, matchState.config.sport)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp)) // Net area
                
                // Bottom Team Row (Team A by default)
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(bottom = 8.dp), contentAlignment = Alignment.BottomCenter) {
                        PlayerBadge(renderModel.bottomLeftPlayer, renderModel.activeServerSlot == CourtSlot.BOTTOM_LEFT, renderModel.activeReceiverSlot == CourtSlot.BOTTOM_LEFT, VoltGreen, pulseScale, matchState.config.sport)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(bottom = 8.dp), contentAlignment = Alignment.BottomCenter) {
                        PlayerBadge(renderModel.bottomRightPlayer, renderModel.activeServerSlot == CourtSlot.BOTTOM_RIGHT, renderModel.activeReceiverSlot == CourtSlot.BOTTOM_RIGHT, VoltGreen, pulseScale, matchState.config.sport)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerBadge(player: Player?, isServer: Boolean, isReceiver: Boolean, teamColor: Color, pulseScale: Float, sport: Sport) {
    if (player == null) return
    val sportIcon = if (sport == Sport.BADMINTON) "🏸" else "🏓"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(2.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp).scale(if (isServer) pulseScale else 1f).clip(CircleShape).background(if (isServer) VoltGold else if (isReceiver) Color(0xFF38BDF8) else teamColor.copy(alpha = 0.85f)).border(width = if (isServer || isReceiver) 2.dp else 1.dp, color = if (isServer || isReceiver) Color.White else Color.Black.copy(alpha = 0.5f), shape = CircleShape)) {
            Text(text = if (isServer) sportIcon else player.shortName, fontSize = if (isServer) 16.sp else 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 4.dp, vertical = 1.dp)) {
            Text(text = player.name.take(9), fontSize = 10.sp, color = if (isServer) VoltGold else TextPrimary, fontWeight = if (isServer) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
        }
    }
}
