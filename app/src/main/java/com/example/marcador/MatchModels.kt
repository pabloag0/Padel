package com.example.marcador

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class CourtPosition(val label: String) {
    TOP_LEFT("Arriba izquierda"),
    TOP_RIGHT("Arriba derecha"),
    BOTTOM_LEFT("Abajo izquierda"),
    BOTTOM_RIGHT("Abajo derecha")
}

data class PlayerAssignment(
    val position: CourtPosition,
    val name: String
)

data class MatchSetup(
    val playerCount: Int = 4,
    val topLeft: String = "",
    val topRight: String = "",
    val bottomLeft: String = "",
    val bottomRight: String = ""
) {
    fun update(position: CourtPosition, value: String): MatchSetup = when (position) {
        CourtPosition.TOP_LEFT -> copy(topLeft = value)
        CourtPosition.TOP_RIGHT -> copy(topRight = value)
        CourtPosition.BOTTOM_LEFT -> copy(bottomLeft = value)
        CourtPosition.BOTTOM_RIGHT -> copy(bottomRight = value)
    }

    fun assignments(): List<PlayerAssignment> = listOf(
        PlayerAssignment(CourtPosition.TOP_LEFT, topLeft.trim()),
        PlayerAssignment(CourtPosition.TOP_RIGHT, topRight.trim()),
        PlayerAssignment(CourtPosition.BOTTOM_LEFT, bottomLeft.trim()),
        PlayerAssignment(CourtPosition.BOTTOM_RIGHT, bottomRight.trim())
    ).filter { it.name.isNotEmpty() }

    fun teamAPlayers(): List<String> = listOf(topLeft.trim(), topRight.trim()).filter { it.isNotEmpty() }
    fun teamBPlayers(): List<String> = listOf(bottomLeft.trim(), bottomRight.trim()).filter { it.isNotEmpty() }

    fun teamALabel(): String = teamAPlayers().ifEmpty { listOf("Equipo 1") }.joinToString(" / ")
    fun teamBLabel(): String = teamBPlayers().ifEmpty { listOf("Equipo 2") }.joinToString(" / ")

    fun initialScoreboardState(): ScoreboardState {
        if (playerCount != 4) return ScoreboardState()
        val firstTeam = if (Random.nextBoolean()) TeamSide.A else TeamSide.B
        val firstTeamPositions = firstTeam.courtPositions()
        val secondTeamPositions = firstTeam.opponent().courtPositions()
        val firstServer = firstTeamPositions.random()
        val secondServer = secondTeamPositions.random()
        val serviceOrder = listOf(
            firstServer,
            secondServer,
            firstServer.partnerPosition(),
            secondServer.partnerPosition()
        )
        return ScoreboardState(
            currentServer = firstServer,
            serviceOrder = serviceOrder
        )
    }

    fun validate(): String? {
        val totalPlayers = assignments().size
        if (playerCount == 2) {
            if (totalPlayers != 2) return "Para un partido de 2 jugadores debes rellenar exactamente dos posiciones."
            if (teamAPlayers().size != 1 || teamBPlayers().size != 1) {
                return "En un partido de 2 jugadores debe haber un jugador arriba y otro abajo."
            }
        } else {
            if (totalPlayers != 4) return "Para un partido de 4 jugadores debes rellenar las cuatro posiciones."
            if (teamAPlayers().size != 2 || teamBPlayers().size != 2) {
                return "En un partido de 4 jugadores debe haber dos jugadores arriba y dos abajo."
            }
        }
        return null
    }
}

private fun TeamSide.courtPositions(): List<CourtPosition> = when (this) {
    TeamSide.A -> listOf(CourtPosition.TOP_LEFT, CourtPosition.TOP_RIGHT)
    TeamSide.B -> listOf(CourtPosition.BOTTOM_LEFT, CourtPosition.BOTTOM_RIGHT)
}

private fun CourtPosition.partnerPosition(): CourtPosition = when (this) {
    CourtPosition.TOP_LEFT -> CourtPosition.TOP_RIGHT
    CourtPosition.TOP_RIGHT -> CourtPosition.TOP_LEFT
    CourtPosition.BOTTOM_LEFT -> CourtPosition.BOTTOM_RIGHT
    CourtPosition.BOTTOM_RIGHT -> CourtPosition.BOTTOM_LEFT
}

fun ScoreboardState.withInitialServiceIfMissing(setup: MatchSetup): ScoreboardState {
    if (setup.playerCount != 4 || currentServer != null || serviceOrder.isNotEmpty()) return this
    val initialState = setup.initialScoreboardState()
    return copy(
        currentServer = initialState.currentServer,
        serviceOrder = initialState.serviceOrder
    )
}

data class PlayerMatchStats(
    val position: CourtPosition,
    val name: String,
    val winners: Int,
    val errors: Int,
    val forcedErrorsCaused: Int,
    val serveFaults: Int,
    val doubleFaults: Int,
    val pointsWon: Int,
    val score: Int
)

private data class MutablePlayerStats(
    var winners: Int = 0,
    var errors: Int = 0,
    var forcedErrorsCaused: Int = 0,
    var serveFaults: Int = 0,
    var doubleFaults: Int = 0,
    var pointsWon: Int = 0
) {
    fun score(): Int = pointsWon + winners * 3 + forcedErrorsCaused * 2 - errors - serveFaults - doubleFaults * 2
}

data class MatchRecord(
    val playedAtMillis: Long,
    val playedAtLabel: String,
    val playerCount: Int,
    val scoreSummary: String,
    val gamesA: Int = 0,
    val gamesB: Int = 0,
    val setsA: Int = 0,
    val setsB: Int = 0,
    val pointA: String = "00",
    val pointB: String = "00",
    val setScores: List<SetScore> = emptyList(),
    val currentServer: CourtPosition? = null,
    val serviceOrder: List<CourtPosition> = emptyList(),
    val firstServeFault: Boolean = false,
    val events: List<MatchEvent> = emptyList(),
    val teamALabel: String,
    val teamBLabel: String,
    val participants: List<PlayerAssignment>
) {
    fun teamADetailLabel(): String = teamALabel.replace(" / ", "/")
    fun teamBDetailLabel(): String = teamBLabel.replace(" / ", "/")
    fun isFinished(): Boolean = setsA >= 2 || setsB >= 2

    fun detailSetScores(): List<SetScore?> {
        val visibleScores = setScores.take(3).toMutableList<SetScore?>()
        if (!isFinished() && visibleScores.size < 3) {
            visibleScores.add(SetScore(gamesA, gamesB))
        }
        while (visibleScores.size < 3) {
            visibleScores.add(null)
        }
        return visibleScores
    }

    fun toMatchSetup(): MatchSetup {
        var setup = MatchSetup(playerCount = playerCount)
        participants.forEach { participant ->
            setup = setup.update(participant.position, participant.name)
        }
        return setup
    }

    fun toScoreboardState(): ScoreboardState = ScoreboardState(
        pointStateA = pointA.toPointState(),
        pointStateB = pointB.toPointState(),
        gamesA = gamesA,
        gamesB = gamesB,
        setsA = setsA,
        setsB = setsB,
        setScores = setScores,
        currentServer = currentServer,
        serviceOrder = serviceOrder,
        firstServeFault = firstServeFault,
        events = events
    )

    fun playerStats(): List<PlayerMatchStats> {
        val statsByPosition = participants.associate { it.position to MutablePlayerStats() }.toMutableMap()
        events.forEach { event ->
            val actorStats = statsByPosition[event.actor]
            when (event.type) {
                PointEventType.WINNER -> {
                    actorStats?.winners = (actorStats?.winners ?: 0) + 1
                    actorStats?.pointsWon = (actorStats?.pointsWon ?: 0) + 1
                }
                PointEventType.RALLY_ERROR -> {
                    actorStats?.forcedErrorsCaused = (actorStats?.forcedErrorsCaused ?: 0) + 1
                    actorStats?.pointsWon = (actorStats?.pointsWon ?: 0) + 1
                    event.target?.let { target ->
                        val targetStats = statsByPosition[target]
                        targetStats?.errors = (targetStats?.errors ?: 0) + 1
                    }
                }
                PointEventType.SERVE_FAULT -> {
                    actorStats?.serveFaults = (actorStats?.serveFaults ?: 0) + 1
                }
                PointEventType.DOUBLE_FAULT -> {
                    actorStats?.serveFaults = (actorStats?.serveFaults ?: 0) + 1
                    actorStats?.doubleFaults = (actorStats?.doubleFaults ?: 0) + 1
                }
                PointEventType.GENERIC -> {
                    // Ignorar para estadísticas individuales, ya que no hay un actor concreto
                }
            }
        }
        return participants.map { participant ->
            val stats = statsByPosition.getValue(participant.position)
            PlayerMatchStats(
                position = participant.position,
                name = participant.name,
                winners = stats.winners,
                errors = stats.errors,
                forcedErrorsCaused = stats.forcedErrorsCaused,
                serveFaults = stats.serveFaults,
                doubleFaults = stats.doubleFaults,
                pointsWon = stats.pointsWon,
                score = stats.score()
            )
        }.sortedWith(compareByDescending<PlayerMatchStats> { it.score }.thenBy { it.name })
    }
}

private fun String.toPointState(): Int = when (uppercase()) {
    "15" -> 1
    "30" -> 2
    "40" -> 3
    "AD" -> 4
    else -> 0
}

private data class ScoreBreakdown(
    val gamesA: Int = 0,
    val gamesB: Int = 0,
    val setsA: Int = 0,
    val setsB: Int = 0,
    val pointA: String = "00",
    val pointB: String = "00",
    val setScores: List<SetScore> = emptyList(),
    val currentServer: CourtPosition? = null,
    val serviceOrder: List<CourtPosition> = emptyList(),
    val firstServeFault: Boolean = false,
    val events: List<MatchEvent> = emptyList()
)

fun ScoreboardState.toSummary(): String {
    return "Sets $setsA-$setsB | Juegos $gamesA-$gamesB | Punto ${pointLabelA()}-${pointLabelB()}"
}

fun ScoreboardState.isMatchFinished(): Boolean = setsA >= 2 || setsB >= 2

private fun ScoreboardState.toBreakdown(): ScoreBreakdown = ScoreBreakdown(
    gamesA = gamesA,
    gamesB = gamesB,
    setsA = setsA,
    setsB = setsB,
    pointA = pointLabelA(),
    pointB = pointLabelB(),
    setScores = setScores,
    currentServer = currentServer,
    serviceOrder = serviceOrder,
    firstServeFault = firstServeFault,
    events = events
)

private fun String.toScoreBreakdown(): ScoreBreakdown {
    val sets = Regex("""Sets\s+(\d+)-(\d+)""").find(this)
    val games = Regex("""Juegos\s+(\d+)-(\d+)""").find(this)
    val points = Regex("""Punto\s+([^-\s|]+)-([^-\s|]+)""").find(this)
    return ScoreBreakdown(
        gamesA = games?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
        gamesB = games?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0,
        setsA = sets?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
        setsB = sets?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0,
        pointA = points?.groupValues?.getOrNull(1) ?: "00",
        pointB = points?.groupValues?.getOrNull(2) ?: "00"
    )
}

private fun JSONObject.optSetScores(): List<SetScore> {
    val array = optJSONArray("setScores") ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                SetScore(
                    gamesA = item.optInt("gamesA", 0),
                    gamesB = item.optInt("gamesB", 0)
                )
            )
        }
    }
}

private fun List<SetScore>.toSetScoresJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { setScore ->
        array.put(
            JSONObject().apply {
                put("gamesA", setScore.gamesA)
                put("gamesB", setScore.gamesB)
            }
        )
    }
    return array
}

private fun JSONObject.optCourtPosition(name: String): CourtPosition? {
    val raw = optString(name, "")
    return runCatching { CourtPosition.valueOf(raw) }.getOrNull()
}

private fun JSONObject.optTeamSide(name: String): TeamSide? {
    val raw = optString(name, "")
    return runCatching { TeamSide.valueOf(raw) }.getOrNull()
}

private fun JSONObject.optCourtPositionList(name: String): List<CourtPosition> {
    val array = optJSONArray(name) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            runCatching { CourtPosition.valueOf(array.getString(index)) }
                .getOrNull()
                ?.let(::add)
        }
    }
}

private fun List<CourtPosition>.toCourtPositionsJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { position -> array.put(position.name) }
    return array
}

private fun JSONObject.optMatchEvents(): List<MatchEvent> {
    val array = optJSONArray("events") ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val type = runCatching { PointEventType.valueOf(item.getString("type")) }.getOrNull()
            val actor = item.optCourtPosition("actor")
            if (type != null) {
                add(
                    MatchEvent(
                        type = type,
                        actor = actor,
                        target = item.optCourtPosition("target"),
                        pointWinner = item.optTeamSide("pointWinner"),
                        server = item.optCourtPosition("server")
                    )
                )
            }
        }
    }
}

private fun List<MatchEvent>.toMatchEventsJsonArray(): JSONArray {
    val array = JSONArray()
    forEach { event ->
        array.put(
            JSONObject().apply {
                put("type", event.type.name)
                event.actor?.let { put("actor", it.name) }
                event.target?.let { put("target", it.name) }
                event.pointWinner?.let { put("pointWinner", it.name) }
                event.server?.let { put("server", it.name) }
            }
        )
    }
    return array
}

class MatchHistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("match_history", Context.MODE_PRIVATE)

    fun loadMatches(): List<MatchRecord> {
        val raw = prefs.getString("matches", "[]") ?: "[]"
        val jsonArray = JSONArray(raw)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(index)
                val participantsArray = item.getJSONArray("participants")
                val participants = buildList {
                    for (p in 0 until participantsArray.length()) {
                        val participant = participantsArray.getJSONObject(p)
                        add(
                            PlayerAssignment(
                                position = CourtPosition.valueOf(participant.getString("position")),
                                name = participant.getString("name")
                            )
                        )
                    }
                }
                add(
                    run {
                        val scoreSummary = item.optString("scoreSummary", "")
                        val parsedScore = scoreSummary.toScoreBreakdown()
                        MatchRecord(
                            playedAtMillis = item.getLong("playedAtMillis"),
                            playedAtLabel = item.getString("playedAtLabel"),
                            playerCount = item.getInt("playerCount"),
                            scoreSummary = scoreSummary,
                            gamesA = item.optInt("gamesA", parsedScore.gamesA),
                            gamesB = item.optInt("gamesB", parsedScore.gamesB),
                            setsA = item.optInt("setsA", parsedScore.setsA),
                            setsB = item.optInt("setsB", parsedScore.setsB),
                            pointA = item.optString("pointA", parsedScore.pointA),
                            pointB = item.optString("pointB", parsedScore.pointB),
                            setScores = item.optSetScores(),
                            currentServer = item.optCourtPosition("currentServer"),
                            serviceOrder = item.optCourtPositionList("serviceOrder"),
                            firstServeFault = item.optBoolean("firstServeFault", false),
                            events = item.optMatchEvents(),
                            teamALabel = item.getString("teamALabel"),
                            teamBLabel = item.getString("teamBLabel"),
                            participants = participants
                        )
                    }
                )
            }
        }.sortedByDescending { it.playedAtMillis }
    }

    fun saveMatch(
        setup: MatchSetup,
        scoreboard: ScoreboardState,
        replacePlayedAtMillis: Long? = null
    ) {
        val current = loadMatches().toMutableList()
        val existingMatch = replacePlayedAtMillis?.let { playedAtMillis ->
            current.firstOrNull { it.playedAtMillis == playedAtMillis }
        }
        val now = existingMatch?.playedAtMillis ?: System.currentTimeMillis()
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val scoreBreakdown = scoreboard.toBreakdown()
        val record = MatchRecord(
            playedAtMillis = now,
            playedAtLabel = existingMatch?.playedAtLabel ?: formatter.format(Date(now)),
            playerCount = setup.playerCount,
            scoreSummary = scoreboard.toSummary(),
            gamesA = scoreBreakdown.gamesA,
            gamesB = scoreBreakdown.gamesB,
            setsA = scoreBreakdown.setsA,
            setsB = scoreBreakdown.setsB,
            pointA = scoreBreakdown.pointA,
            pointB = scoreBreakdown.pointB,
            setScores = scoreBreakdown.setScores,
            currentServer = scoreBreakdown.currentServer,
            serviceOrder = scoreBreakdown.serviceOrder,
            firstServeFault = scoreBreakdown.firstServeFault,
            events = scoreBreakdown.events,
            teamALabel = setup.teamALabel(),
            teamBLabel = setup.teamBLabel(),
            participants = setup.assignments()
        )
        val replaceIndex = replacePlayedAtMillis?.let { playedAtMillis ->
            current.indexOfFirst { it.playedAtMillis == playedAtMillis }
        } ?: -1
        if (replaceIndex >= 0) {
            current[replaceIndex] = record
        } else {
            current.add(0, record)
        }
        val array = JSONArray()
        current.forEach { match ->
            val obj = JSONObject()
            obj.put("playedAtMillis", match.playedAtMillis)
            obj.put("playedAtLabel", match.playedAtLabel)
            obj.put("playerCount", match.playerCount)
            obj.put("scoreSummary", match.scoreSummary)
            obj.put("gamesA", match.gamesA)
            obj.put("gamesB", match.gamesB)
            obj.put("setsA", match.setsA)
            obj.put("setsB", match.setsB)
            obj.put("pointA", match.pointA)
            obj.put("pointB", match.pointB)
            obj.put("setScores", match.setScores.toSetScoresJsonArray())
            match.currentServer?.let { obj.put("currentServer", it.name) }
            obj.put("serviceOrder", match.serviceOrder.toCourtPositionsJsonArray())
            obj.put("firstServeFault", match.firstServeFault)
            obj.put("events", match.events.toMatchEventsJsonArray())
            obj.put("teamALabel", match.teamALabel)
            obj.put("teamBLabel", match.teamBLabel)
            val participants = JSONArray()
            match.participants.forEach { participant ->
                participants.put(
                    JSONObject().apply {
                        put("position", participant.position.name)
                        put("name", participant.name)
                    }
                )
            }
            obj.put("participants", participants)
            array.put(obj)
        }
        prefs.edit().putString("matches", array.toString()).apply()
    }

    fun deleteMatches(playedAtMillis: Set<Long>) {
        if (playedAtMillis.isEmpty()) return
        val remainingMatches = loadMatches().filterNot { it.playedAtMillis in playedAtMillis }
        val array = JSONArray()
        remainingMatches.forEach { match ->
            val obj = JSONObject()
            obj.put("playedAtMillis", match.playedAtMillis)
            obj.put("playedAtLabel", match.playedAtLabel)
            obj.put("playerCount", match.playerCount)
            obj.put("scoreSummary", match.scoreSummary)
            obj.put("gamesA", match.gamesA)
            obj.put("gamesB", match.gamesB)
            obj.put("setsA", match.setsA)
            obj.put("setsB", match.setsB)
            obj.put("pointA", match.pointA)
            obj.put("pointB", match.pointB)
            obj.put("setScores", match.setScores.toSetScoresJsonArray())
            match.currentServer?.let { obj.put("currentServer", it.name) }
            obj.put("serviceOrder", match.serviceOrder.toCourtPositionsJsonArray())
            obj.put("firstServeFault", match.firstServeFault)
            obj.put("events", match.events.toMatchEventsJsonArray())
            obj.put("teamALabel", match.teamALabel)
            obj.put("teamBLabel", match.teamBLabel)
            val participants = JSONArray()
            match.participants.forEach { participant ->
                participants.put(
                    JSONObject().apply {
                        put("position", participant.position.name)
                        put("name", participant.name)
                    }
                )
            }
            obj.put("participants", participants)
            array.put(obj)
        }
        prefs.edit().putString("matches", array.toString()).apply()
    }
}
