package com.divyanshu.chess.lab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabJsonTest {

    private val sample = """
        {
          "generated_at": "2026-10-29T12:00:00Z",
          "experiments": [{
            "id": "exp-demo", "title": "Demo", "description": "d", "status": "preliminary",
            "engine_version": "Stockfish 19", "time_control": "nodes 1000", "pairs": 10, "games": 20,
            "baseline": {"name": "A", "limits": "nodes 1000", "options": {"Hash": 16}},
            "candidate": {"name": "B", "limits": "nodes 2000", "options": {}},
            "wins": 9, "draws": 6, "losses": 5, "score_pct": 55.0,
            "elo_diff": 34.9, "elo_ci_low": -50.0, "elo_ci_high": 120.0, "ci_method": "pentanomial",
            "crashes": 0, "illegal_moves": 0, "timeouts": 0,
            "baseline_stats": {"avg_nps": 1500000, "avg_depth": 9.5},
            "candidate_stats": {},
            "created_at": "2026-10-29", "reproduce": "python -m engine_lab run exp-demo"
          }]
        }
    """.trimIndent()

    @Test
    fun `benchmark json is parsed field by field`() {
        val report = LabJson.parseBenchmarks(sample)
        val result = report.experiments.single()
        assertEquals("exp-demo", result.id)
        assertEquals(20, result.games)
        assertEquals(Triple(9, 6, 5), Triple(result.wins, result.draws, result.losses))
        assertEquals(34.9, result.eloDiff!!, 0.001)
        assertEquals(mapOf("Hash" to "16"), result.baseline.options)
        assertEquals(1_500_000L, result.baselineStats.avgNps)
        assertNull(result.candidateStats.avgNps)
    }

    @Test
    fun `missing confidence interval stays null`() {
        val noCi = sample.replace("\"elo_ci_low\": -50.0,", "").replace("\"elo_diff\": 34.9,", "")
        val result = LabJson.parseBenchmarks(noCi).experiments.single()
        assertNull(result.eloDiff)
        assertNull(result.eloCiLow)
    }

    @Test
    fun `test summaries are parsed`() {
        val report = LabJson.parseTests(
            """{"generated_at":"t","suites":[{"name":"JVM","total":10,"failures":1,"skipped":2}]}""",
        )
        assertEquals(TestSuiteResult("JVM", 10, 1, 2), report.suites.single())
    }
}
