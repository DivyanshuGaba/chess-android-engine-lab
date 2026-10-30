package com.divyanshu.chess.lab

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class EngineConfigSummary(val name: String, val limits: String, val options: Map<String, String>)

data class EngineRunStats(val avgNps: Long?, val avgDepth: Double?, val avgMoveMs: Double?, val peakRssMb: Double?)

/** One completed benchmark. Wins, draws and losses are from the candidate's point of view. */
data class ExperimentResult(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val stockfishVersion: String,
    val timeControl: String,
    val pairs: Int,
    val games: Int,
    val baseline: EngineConfigSummary,
    val candidate: EngineConfigSummary,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val scorePct: Double,
    val eloDiff: Double?,
    val eloCiLow: Double?,
    val eloCiHigh: Double?,
    val ciMethod: String,
    val crashes: Int,
    val illegalMoves: Int,
    val timeouts: Int,
    val baselineStats: EngineRunStats,
    val candidateStats: EngineRunStats,
    val createdAt: String,
    val reproduce: String,
)

data class BenchmarkReport(val generatedAt: String, val experiments: List<ExperimentResult>)

data class TestSuiteResult(val name: String, val total: Int, val failures: Int, val skipped: Int)

data class TestReport(val generatedAt: String, val suites: List<TestSuiteResult>)

interface LabDataSource {
    suspend fun benchmarks(): BenchmarkReport?

    suspend fun tests(): TestReport?
}

/** Reads the JSON that `engine_lab` and `tools/summarize_tests.py` generate; nothing is invented here. */
object LabJson {

    fun parseBenchmarks(text: String): BenchmarkReport {
        val root = JSONObject(text)
        val list = root.getJSONArray("experiments")
        return BenchmarkReport(
            generatedAt = root.optString("generated_at"),
            experiments = (0 until list.length()).map { parseExperiment(list.getJSONObject(it)) },
        )
    }

    fun parseTests(text: String): TestReport {
        val root = JSONObject(text)
        val list = root.getJSONArray("suites")
        return TestReport(
            generatedAt = root.optString("generated_at"),
            suites = (0 until list.length()).map {
                val suite = list.getJSONObject(it)
                TestSuiteResult(
                    name = suite.getString("name"),
                    total = suite.getInt("total"),
                    failures = suite.getInt("failures"),
                    skipped = suite.optInt("skipped", 0),
                )
            },
        )
    }

    private fun parseExperiment(json: JSONObject) = ExperimentResult(
        id = json.getString("id"),
        title = json.optString("title", json.getString("id")),
        description = json.optString("description"),
        status = json.optString("status", "preliminary"),
        stockfishVersion = json.optString("engine_version"),
        timeControl = json.optString("time_control"),
        pairs = json.optInt("pairs"),
        games = json.getInt("games"),
        baseline = parseConfig(json.getJSONObject("baseline")),
        candidate = parseConfig(json.getJSONObject("candidate")),
        wins = json.getInt("wins"),
        draws = json.getInt("draws"),
        losses = json.getInt("losses"),
        scorePct = json.getDouble("score_pct"),
        eloDiff = json.optDoubleOrNull("elo_diff"),
        eloCiLow = json.optDoubleOrNull("elo_ci_low"),
        eloCiHigh = json.optDoubleOrNull("elo_ci_high"),
        ciMethod = json.optString("ci_method"),
        crashes = json.optInt("crashes"),
        illegalMoves = json.optInt("illegal_moves"),
        timeouts = json.optInt("timeouts"),
        baselineStats = parseStats(json.optJSONObject("baseline_stats")),
        candidateStats = parseStats(json.optJSONObject("candidate_stats")),
        createdAt = json.optString("created_at"),
        reproduce = json.optString("reproduce"),
    )

    private fun parseConfig(json: JSONObject): EngineConfigSummary {
        val options = json.optJSONObject("options")
        return EngineConfigSummary(
            name = json.optString("name"),
            limits = json.optString("limits"),
            options = options?.keys()?.asSequence()?.associateWith { options.get(it).toString() }.orEmpty(),
        )
    }

    private fun parseStats(json: JSONObject?) = EngineRunStats(
        avgNps = json?.optDoubleOrNull("avg_nps")?.toLong(),
        avgDepth = json?.optDoubleOrNull("avg_depth"),
        avgMoveMs = json?.optDoubleOrNull("avg_move_ms"),
        peakRssMb = json?.optDoubleOrNull("peak_rss_mb"),
    )

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null
}

class AssetLabDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LabDataSource {

    override suspend fun benchmarks(): BenchmarkReport? = read("lab/results.json")?.let(LabJson::parseBenchmarks)

    override suspend fun tests(): TestReport? = read("lab/tests.json")?.let(LabJson::parseTests)

    private suspend fun read(path: String): String? = withContext(Dispatchers.IO) {
        try {
            context.assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: FileNotFoundException) {
            null
        }
    }
}
