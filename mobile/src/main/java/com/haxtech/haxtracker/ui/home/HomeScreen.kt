package com.haxtech.haxtracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haxtech.haxtracker.core.model.MatchConfig
import com.haxtech.haxtracker.core.model.MatchFormat
import com.haxtech.haxtracker.core.model.Player
import com.haxtech.haxtracker.core.model.Sport
import com.haxtech.haxtracker.core.model.Team
import com.haxtech.haxtracker.core.model.TeamSide
import com.haxtech.haxtracker.ui.theme.DarkBorder
import com.haxtech.haxtracker.ui.theme.DarkSurface
import com.haxtech.haxtracker.ui.theme.DarkSurfaceElevated
import com.haxtech.haxtracker.ui.theme.PitchBlack
import com.haxtech.haxtracker.ui.theme.TextPrimary
import com.haxtech.haxtracker.ui.theme.TextSecondary
import com.haxtech.haxtracker.ui.theme.VoltCoral
import com.haxtech.haxtracker.ui.theme.VoltCyan
import com.haxtech.haxtracker.ui.theme.VoltGold
import com.haxtech.haxtracker.ui.theme.VoltGreen

@Composable
fun HomeScreen(
    onStartMatch: (MatchConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // App Branding Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "HAXTRACKER",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Mid-Game Score & Court Position System",
                    fontSize = 12.sp,
                    color = VoltGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(VoltGreen.copy(alpha = 0.15f))
                    .border(1.dp, VoltGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Match Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "INSTANT QUICK MATCH (< 5 SECONDS)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Quick Match Card: Pickleball Doubles
        QuickLaunchCard(
            sportEmoji = "🏓",
            title = "Pickleball Doubles",
            subtitle = "First to 11 • Side-Out Rules • USAPA Rotation",
            badge = "POPULAR",
            badgeColor = VoltGreen,
            accentColor = VoltGreen,
            onClick = {
                onStartMatch(MatchConfig.defaultPickleballDoubles())
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Quick Match Card: Badminton Doubles
        QuickLaunchCard(
            sportEmoji = "🏸",
            title = "Badminton Doubles",
            subtitle = "First to 21 • Rally Points • BWF Service Courts",
            badge = "CLASSIC",
            badgeColor = VoltCyan,
            accentColor = VoltCyan,
            onClick = {
                onStartMatch(MatchConfig.defaultBadmintonDoubles())
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Quick Match Row: Singles Options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SinglesLaunchCard(
                emoji = "🏓",
                title = "Pickleball Singles",
                targetScore = "To 11",
                accentColor = VoltGold,
                modifier = Modifier.weight(1f),
                onClick = {
                    val a1 = Player("a1", "Player A")
                    val b1 = Player("b1", "Player B")
                    onStartMatch(
                        MatchConfig(
                            sport = Sport.PICKLEBALL,
                            format = MatchFormat.SINGLES,
                            teamA = Team(TeamSide.TEAM_A, "Player A", a1),
                            teamB = Team(TeamSide.TEAM_B, "Player B", b1),
                            winningScore = 11,
                            winByTwo = true
                        )
                    )
                }
            )

            SinglesLaunchCard(
                emoji = "🏸",
                title = "Badminton Singles",
                targetScore = "To 21",
                accentColor = VoltCoral,
                modifier = Modifier.weight(1f),
                onClick = {
                    val a1 = Player("a1", "Player A")
                    val b1 = Player("b1", "Player B")
                    onStartMatch(
                        MatchConfig(
                            sport = Sport.BADMINTON,
                            format = MatchFormat.SINGLES,
                            teamA = Team(TeamSide.TEAM_A, "Player A", a1),
                            teamB = Team(TeamSide.TEAM_B, "Player B", b1),
                            winningScore = 21,
                            winByTwo = true,
                            maxScoreCap = 30
                        )
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mid-Game Features Highlight Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "MID-GAME NO-LOOK CONTROLS",
                    color = VoltGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeatureRow(icon = "🔊", title = "Hardware Volume Rocker", desc = "Click Volume Up for Team B point, Volume Down for Team A point through your pocket.")
                Spacer(modifier = Modifier.height(8.dp))
                FeatureRow(icon = "🗣", title = "Voice Score Calls", desc = "Built-in Text-to-Speech announces '8-6-2' and 'Side-Out' across the court.")
                Spacer(modifier = Modifier.height(8.dp))
                FeatureRow(icon = "🗺", title = "2D Court Visualizer", desc = "Live diagram shows where every player stands and arrows the active serve.")
            }
        }
    }
}

@Composable
private fun QuickLaunchCard(
    sportEmoji: String,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.16f),
                        DarkSurface
                    )
                )
            )
            .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sportEmoji, fontSize = 34.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 9.sp,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = PitchBlack, fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SinglesLaunchCard(
    emoji: String,
    title: String,
    targetScore: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 24.sp)
                Text(
                    text = targetScore,
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
