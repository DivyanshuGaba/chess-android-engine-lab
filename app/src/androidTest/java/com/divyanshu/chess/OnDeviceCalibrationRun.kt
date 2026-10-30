package com.divyanshu.chess

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.test.platform.app.InstrumentationRegistry
import com.divyanshu.chess.domain.ai.AdvancedAi
import com.divyanshu.chess.domain.ai.ChessAi
import com.divyanshu.chess.domain.ai.HARD_MOVE_TIME_MS
import com.divyanshu.chess.domain.ai.Searcher
import com.divyanshu.chess.domain.model.Game
import com.divyanshu.chess.domain.model.GameStatus
import com.divyanshu.chess.domain.model.Move
import com.divyanshu.chess.domain.model.Side
import com.divyanshu.chess.engine.EngineCrashedException
import com.divyanshu.chess.engine.EngineTimeoutException
import com.divyanshu.chess.engine.IllegalEngineMoveException
import com.divyanshu.chess.engine.SearchLimits
import com.divyanshu.chess.engine.SharedEngine
import com.divyanshu.chess.engine.StockfishAi
import com.divyanshu.chess.engine.StockfishLocator
import com.divyanshu.chess.engine.StockfishProfile
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Plays the app's own built-in Hard engine against a Stockfish UCI_Elo rung, on the phone, from the same
 * seeded openings as `engine_lab`, and writes games in the lab's CSV format so the usual report applies.
 * It only runs with `-e calibrate true -e elo <rung>`; `-e pairs <n>` sets the number of opening pairs.
 */
class OnDeviceCalibrationRun {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private class Side(val name: String, val ai: ChessAi)

    private class Stats {
        var moves = 0
        var depthSum = 0
        var depthSamples = 0
        var nodes = 0L
        var engineSeconds = 0.0
        var wallSeconds = 0.0
    }

    private class Opening(val id: String, val fen: String, val moves: List<Move>)

    private class Finished(val result: String, val termination: String, val error: String = "")

    @Test
    fun built_in_hard_against_a_stockfish_rung_on_this_phone() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("start with -e calibrate true -e elo <rung>", args.getString("calibrate") == "true")
        val elo = checkNotNull(args.getString("elo")?.toIntOrNull()) { "-e elo <rung> is required" }
        val pairs = args.getString("pairs")?.toIntOrNull() ?: 30
        val experiment = args.getString("experiment") ?: "device-vs-stockfish-$elo"

        val shared = SharedEngine(StockfishLocator(context))
        assumeTrue("Stockfish is not available on this device", shared.isStockfishAvailable)

        // A partial wake lock keeps the CPU running at full speed if the screen turns off during a long run.
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "chess:calibration").apply { acquire(WAKE_LOCK_MS) }
        try {
            playAll(shared, args, elo, pairs, experiment)
        } finally {
            wakeLock.release()
        }
    }

    private suspend fun playAll(shared: SharedEngine, args: android.os.Bundle, elo: Int, pairs: Int, experiment: String) {
        val stockfish = Side("baseline", StockfishAi(shared, StockfishProfile(elo, SearchLimits(movetimeMs = HARD_MOVE_TIME_MS))))
        val builtIn = Side("candidate", AdvancedAi(Searcher.Limits(movetimeMs = HARD_MOVE_TIME_MS)))
        val openings = loadOpenings().take(pairs)

        val directory = File(context.filesDir, "calibration").apply { mkdirs() }
        val csv = File(directory, "$experiment.csv")
        val done = if (csv.exists()) csv.readLines().drop(1).mapNotNull { it.split(',').getOrNull(2)?.toIntOrNull() }.toSet() else emptySet()
        if (!csv.exists()) csv.writeText(HEADER.joinToString(",") + "\n")

        File(directory, "$experiment.meta.json").writeText(
            JSONObject()
                .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
                .put("android", Build.VERSION.RELEASE)
                .put("sdk", Build.VERSION.SDK_INT)
                .put("abis", Build.SUPPORTED_ABIS.joinToString())
                .put("started_at", Instant.now().toString())
                .put("pairs_requested", pairs)
                .toString(2),
        )

        for ((index, opening) in openings.withIndex()) {
            if (index in done) continue
            for (candidateWhite in listOf(true, false)) {
                val (white, black) = if (candidateWhite) builtIn to stockfish else stockfish to builtIn
                shared.withEngine(emptyMap()) { it.newGame() }
                csv.appendText(playGame(experiment, index, opening, white, black, candidateWhite) + "\n")
            }
        }
        shared.shutdown()
    }

    private fun loadOpenings(): List<Opening> {
        val text = InstrumentationRegistry.getInstrumentation().context.assets.open("openings.json").bufferedReader().use { it.readText() }
        val list = JSONObject(text).getJSONArray("openings")
        return (0 until list.length()).map {
            val item = list.getJSONObject(it)
            val moves = item.getJSONArray("moves")
            Opening(item.getString("id"), item.getString("fen"), (0 until moves.length()).map { m -> Move.fromUci(moves.getString(m)) })
        }
    }

    private suspend fun playGame(
        experiment: String,
        pair: Int,
        opening: Opening,
        white: Side,
        black: Side,
        candidateWhite: Boolean,
    ): String {
        val startedAt = System.nanoTime()
        var game = Game.fromMoves(opening.moves)
        val stats = mapOf(white.name to Stats(), black.name to Stats())
        val scores = mutableListOf<Int>()
        var outcome: Finished? = null

        while (outcome == null) {
            game.result?.let {
                val termination = when (game.status) {
                    GameStatus.CHECKMATE -> "checkmate"
                    GameStatus.STALEMATE -> "stalemate"
                    GameStatus.DRAW_FIFTY_MOVE -> "fifty_moves"
                    GameStatus.DRAW_REPETITION -> "threefold_repetition"
                    else -> "insufficient_material"
                }
                outcome = Finished(it.label, termination)
            }
            if (outcome != null) break
            if (game.moves.size >= MAX_PLIES) {
                outcome = Finished("1/2-1/2", "max_plies")
                break
            }
            when {
                scores.size >= ADJUDICATION_PLIES && scores.takeLast(ADJUDICATION_PLIES).all { it >= ADJUDICATION_CP } ->
                    outcome = Finished("1-0", "adjudicated_win")
                scores.size >= ADJUDICATION_PLIES && scores.takeLast(ADJUDICATION_PLIES).all { it <= -ADJUDICATION_CP } ->
                    outcome = Finished("0-1", "adjudicated_win")
            }
            if (outcome != null) break

            val mover = if (game.state.sideToMove == com.divyanshu.chess.domain.model.Side.WHITE) white else black
            val moveStarted = System.nanoTime()
            val result = try {
                mover.ai.search(game.state)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val kind = when (e) {
                    is IllegalEngineMoveException -> "illegal_move"
                    is EngineTimeoutException -> "timeout"
                    is EngineCrashedException -> "crash"
                    else -> "crash"
                }
                val moverIsWhite = mover === white
                outcome = Finished(if (moverIsWhite) "0-1" else "1-0", kind, e.message.orEmpty().replace(',', ';'))
                break
            }
            val wallSeconds = (System.nanoTime() - moveStarted) / 1e9
            val move = result.move
            if (move == null || move !in game.legalMoves) {
                outcome = Finished(if (mover === white) "0-1" else "1-0", "illegal_move", "returned ${move?.uci}")
                break
            }
            val side = stats.getValue(mover.name)
            side.moves++
            side.wallSeconds += wallSeconds
            result.info?.let { info ->
                side.depthSum += info.depth
                side.depthSamples++
                side.nodes += info.nodes
                side.engineSeconds += info.timeMs / 1000.0
                val white = info.mateIn?.let { if (it > 0) MATE_CP else -MATE_CP } ?: info.scoreCp
                if (white != null) scores += white else scores.clear()
            } ?: scores.clear()
            game = game.play(move)
        }

        val finished = outcome!!
        val candidateScore = when (finished.result) {
            "1-0" -> if (candidateWhite) 1.0 else 0.0
            "0-1" -> if (candidateWhite) 0.0 else 1.0
            else -> 0.5
        }
        val b = stats.getValue("baseline")
        val c = stats.getValue("candidate")
        return listOf(
            experiment, "%05d-%s".format(pair, if (candidateWhite) "cw" else "bw"), pair, opening.id, opening.fen,
            white.name, black.name, finished.result, candidateScore, finished.termination, game.moves.size,
            game.moves.joinToString(" ") { it.uci }, finished.error,
            b.moves, b.depthSum, b.depthSamples, b.nodes, "%.4f".format(b.engineSeconds), "%.4f".format(b.wallSeconds), 0,
            c.moves, c.depthSum, c.depthSamples, c.nodes, "%.4f".format(c.engineSeconds), "%.4f".format(c.wallSeconds), 0,
            "Stockfish 19 and built-in advanced on device", "device; ${HARD_MOVE_TIME_MS} ms per move each",
            "%.3f".format((System.nanoTime() - startedAt) / 1e9), Instant.now().toString(),
        ).joinToString(",")
    }

    private companion object {
        const val WAKE_LOCK_MS = 3 * 60 * 60 * 1000L
        const val MAX_PLIES = 400
        const val ADJUDICATION_CP = 1000
        const val ADJUDICATION_PLIES = 6
        const val MATE_CP = 10_000

        val HEADER = listOf(
            "experiment_id", "game_id", "pair_id", "opening_id", "opening_fen", "white", "black", "result",
            "candidate_score", "termination", "plies", "moves_uci", "error",
            "b_moves", "b_depth_sum", "b_depth_samples", "b_nodes", "b_engine_time_s", "b_wall_time_s", "b_peak_rss_mb",
            "c_moves", "c_depth_sum", "c_depth_samples", "c_nodes", "c_engine_time_s", "c_wall_time_s", "c_peak_rss_mb",
            "engine_version", "time_control", "duration_s", "finished_at",
        )
    }
}
