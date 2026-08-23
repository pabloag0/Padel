package com.example.marcador

enum class AppMode {
    LOCAL,
    BLUETOOTH
}

enum class TeamSide {
    A,
    B
}

enum class PointEventType {
    RALLY_ERROR,
    WINNER,
    SERVE_FAULT,
    DOUBLE_FAULT
}

data class SetScore(
    val gamesA: Int,
    val gamesB: Int
)

data class MatchEvent(
    val type: PointEventType,
    val actor: CourtPosition,
    val target: CourtPosition? = null,
    val pointWinner: TeamSide? = null,
    val server: CourtPosition? = null
)

data class ScoreboardState(
    val pointStateA: Int = 0,
    val pointStateB: Int = 0,
    val gamesA: Int = 0,
    val gamesB: Int = 0,
    val setsA: Int = 0,
    val setsB: Int = 0,
    val setScores: List<SetScore> = emptyList(),
    val currentServer: CourtPosition? = null,
    val serviceOrder: List<CourtPosition> = emptyList(),
    val firstServeFault: Boolean = false,
    val events: List<MatchEvent> = emptyList()
)

fun ScoreboardState.pointLabelA(): String = pointLabel(pointStateA)
fun ScoreboardState.pointLabelB(): String = pointLabel(pointStateB)

fun CourtPosition.teamSide(): TeamSide = when (this) {
    CourtPosition.TOP_LEFT,
    CourtPosition.TOP_RIGHT -> TeamSide.A
    CourtPosition.BOTTOM_LEFT,
    CourtPosition.BOTTOM_RIGHT -> TeamSide.B
}

fun TeamSide.opponent(): TeamSide = if (this == TeamSide.A) TeamSide.B else TeamSide.A

fun TeamSide.bluetoothScoreMessage(): String = if (this == TeamSide.A) "0\n" else "1\n"

fun ScoreboardState.bluetoothStateMessage(): String = "SET:$pointStateA,$pointStateB,$gamesA,$gamesB,$setsA,$setsB\n"
private fun pointLabel(pointState: Int): String = when (pointState) {
    0 -> "00"
    1 -> "15"
    2 -> "30"
    3 -> "40"
    4 -> "AD"
    else -> "00"
}

fun ScoreboardState.pointToTeamA(): ScoreboardState {
    if (pointStateA == 4) return winGameForA()
    if (pointStateB == 4) return copy(pointStateA = 3, pointStateB = 3, firstServeFault = false)
    if (pointStateA == 3 && pointStateB == 3) return copy(pointStateA = 4, firstServeFault = false)
    if (pointStateA < 3) return copy(pointStateA = pointStateA + 1, firstServeFault = false)
    if (pointStateA == 3 && pointStateB < 3) return winGameForA()
    return this
}

fun ScoreboardState.pointToTeamB(): ScoreboardState {
    if (pointStateB == 4) return winGameForB()
    if (pointStateA == 4) return copy(pointStateA = 3, pointStateB = 3, firstServeFault = false)
    if (pointStateA == 3 && pointStateB == 3) return copy(pointStateB = 4, firstServeFault = false)
    if (pointStateB < 3) return copy(pointStateB = pointStateB + 1, firstServeFault = false)
    if (pointStateB == 3 && pointStateA < 3) return winGameForB()
    return this
}

fun ScoreboardState.recordWinner(winner: CourtPosition): ScoreboardState {
    val scoringTeam = winner.teamSide()
    val event = MatchEvent(
        type = PointEventType.WINNER,
        actor = winner,
        pointWinner = scoringTeam,
        server = currentServer
    )
    return pointToTeam(scoringTeam).copy(events = events + event)
}

fun ScoreboardState.recordRallyError(hitter: CourtPosition, failedPlayer: CourtPosition): ScoreboardState {
    if (hitter.teamSide() == failedPlayer.teamSide()) return this
    val scoringTeam = hitter.teamSide()
    val event = MatchEvent(
        type = PointEventType.RALLY_ERROR,
        actor = hitter,
        target = failedPlayer,
        pointWinner = scoringTeam,
        server = currentServer
    )
    return pointToTeam(scoringTeam).copy(events = events + event)
}

fun ScoreboardState.recordServeFault(server: CourtPosition): ScoreboardState {
    if (currentServer != null && server != currentServer) return this
    val servingTeam = server.teamSide()
    return if (firstServeFault) {
        val scoringTeam = servingTeam.opponent()
        val event = MatchEvent(
            type = PointEventType.DOUBLE_FAULT,
            actor = server,
            pointWinner = scoringTeam,
            server = currentServer
        )
        pointToTeam(scoringTeam).copy(events = events + event, firstServeFault = false)
    } else {
        val event = MatchEvent(
            type = PointEventType.SERVE_FAULT,
            actor = server,
            server = currentServer
        )
        copy(firstServeFault = true, events = events + event)
    }
}

private fun ScoreboardState.pointToTeam(teamSide: TeamSide): ScoreboardState = when (teamSide) {
    TeamSide.A -> pointToTeamA()
    TeamSide.B -> pointToTeamB()
}

private fun ScoreboardState.winGameForA(): ScoreboardState = copy(
    pointStateA = 0,
    pointStateB = 0,
    gamesA = gamesA + 1,
    firstServeFault = false
).checkSetWin().advanceServer()

private fun ScoreboardState.winGameForB(): ScoreboardState = copy(
    pointStateA = 0,
    pointStateB = 0,
    gamesB = gamesB + 1,
    firstServeFault = false
).checkSetWin().advanceServer()

private fun ScoreboardState.checkSetWin(): ScoreboardState {
    return when {
        (gamesA >= 6 && gamesA - gamesB >= 2) || gamesA == 7 -> copy(
            pointStateA = 0,
            pointStateB = 0,
            gamesA = 0,
            gamesB = 0,
            setsA = setsA + 1,
            setScores = setScores + SetScore(gamesA, gamesB)
        )
        (gamesB >= 6 && gamesB - gamesA >= 2) || gamesB == 7 -> copy(
            pointStateA = 0,
            pointStateB = 0,
            gamesA = 0,
            gamesB = 0,
            setsB = setsB + 1,
            setScores = setScores + SetScore(gamesA, gamesB)
        )
        else -> this
    }
}

private fun ScoreboardState.advanceServer(): ScoreboardState {
    if (serviceOrder.isEmpty()) return this
    val currentIndex = serviceOrder.indexOf(currentServer).takeIf { it >= 0 } ?: 0
    val nextIndex = (currentIndex + 1) % serviceOrder.size
    return copy(currentServer = serviceOrder[nextIndex], firstServeFault = false)
}
