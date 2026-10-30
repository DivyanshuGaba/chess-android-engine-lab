package com.divyanshu.chess.engine

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.channels.Channel

sealed class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause)

class EngineUnavailableException(message: String) : EngineException(message)

class EngineCrashedException(message: String, cause: Throwable? = null) : EngineException(message, cause)

class EngineTimeoutException(message: String) : EngineException(message)

class IllegalEngineMoveException(val move: String, val fen: String) :
    EngineException("Engine returned illegal move $move for $fen")

/** The two ends of a UCI conversation: text lines out, text lines in. */
interface UciTransport {
    val isAlive: Boolean

    fun send(line: String)

    /** Suspends until the next line arrives; throws [EngineCrashedException] once the engine has gone. */
    suspend fun readLine(): String

    /** Ends the conversation politely, then forcefully if the engine does not exit. */
    fun close()

    fun kill()
}

/** Runs an engine executable as a child process and talks to it over stdin and stdout. */
class ProcessUciTransport(command: List<String>, workingDir: File? = null) : UciTransport {

    private val process: Process = try {
        ProcessBuilder(command).directory(workingDir).start()
    } catch (e: IOException) {
        throw EngineUnavailableException("Could not start ${command.first()}: ${e.message}")
    }
    private val writer = process.outputStream.bufferedWriter()
    private val inbox = Channel<String>(Channel.UNLIMITED)

    init {
        thread(isDaemon = true, name = "uci-stdout") {
            try {
                process.inputStream.bufferedReader().useLines { lines -> lines.forEach { inbox.trySend(it) } }
            } catch (_: IOException) {
                // The pipe closes when the engine exits; the channel close below reports it.
            } finally {
                inbox.close()
            }
        }
        thread(isDaemon = true, name = "uci-stderr") {
            try {
                process.errorStream.bufferedReader().useLines { lines -> lines.forEach { _ -> } }
            } catch (_: IOException) {
            }
        }
    }

    override val isAlive: Boolean get() = process.isAlive

    override fun send(line: String) {
        try {
            synchronized(writer) {
                writer.write(line)
                writer.newLine()
                writer.flush()
            }
        } catch (e: IOException) {
            throw EngineCrashedException("Engine pipe closed while sending '$line'", e)
        }
    }

    override suspend fun readLine(): String {
        val result = inbox.receiveCatching()
        return result.getOrNull() ?: throw EngineCrashedException("Engine closed its output")
    }

    override fun close() {
        runCatching { send("quit") }
        if (!process.waitFor(QUIT_GRACE_MS, TimeUnit.MILLISECONDS)) process.destroyForcibly()
        inbox.close()
    }

    override fun kill() {
        process.destroyForcibly()
        inbox.close()
    }

    private companion object {
        const val QUIT_GRACE_MS = 1_000L
    }
}
