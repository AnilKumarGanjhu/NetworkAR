package com.networkar.app.network

import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.math.abs

data class SpeedResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double
)

class SpeedTester {

    companion object {

        private const val PING_HOST = "1.1.1.1"
        private const val PING_COUNT = 4
        private const val PING_TIMEOUT_MS = 1500

        /*
         * Cloudflare speed-test endpoints.
         */
        private const val DOWNLOAD_URL =
            "https://speed.cloudflare.com/__down?bytes=5000000"

        private const val UPLOAD_URL =
            "https://speed.cloudflare.com/__up"

        private const val DOWNLOAD_TIMEOUT_MS = 15_000
        private const val UPLOAD_TIMEOUT_MS = 15_000

        /*
         * 2 MB upload test.
         */
        private const val UPLOAD_SIZE_BYTES = 2_000_000

        /*
         * Bytes -> Megabits.
         */
        private const val BYTES_TO_MEGABITS =
            8.0 / 1_000_000.0
    }

    /**
     * Runs the complete speed test.
     *
     * Measures:
     * - Ping
     * - Jitter
     * - Download
     * - Upload
     */
    suspend fun test(): SpeedResult {

        val pingResults = measurePing()

        val pingMs = calculatePing(pingResults)

        val jitterMs = calculateJitter(pingResults)

        val downloadMbps = measureDownload()

        val uploadMbps = measureUpload()

        return SpeedResult(
            downloadMbps = downloadMbps,
            uploadMbps = uploadMbps,
            pingMs = pingMs,
            jitterMs = jitterMs
        )
    }

    /**
     * Measures network latency.
     *
     * Note:
     * InetAddress.isReachable() is not guaranteed to use
     * ICMP on every Android device/network.
     */
    private fun measurePing(): List<Double> {

        val results = mutableListOf<Double>()

        repeat(PING_COUNT) {

            val start = System.nanoTime()

            val reachable = runCatching {

                InetAddress
                    .getByName(PING_HOST)
                    .isReachable(PING_TIMEOUT_MS)

            }.getOrDefault(false)

            val elapsedMs =
                (System.nanoTime() - start) / 1_000_000.0

            if (reachable) {
                results.add(elapsedMs)
            }
        }

        return results
    }

    /**
     * Calculates average ping.
     */
    private fun calculatePing(
        pingResults: List<Double>
    ): Double {

        if (pingResults.isEmpty()) {
            return 0.0
        }

        return pingResults.average()
    }

    /**
     * Calculates jitter as the average difference
     * between consecutive ping measurements.
     */
    private fun calculateJitter(
        pingResults: List<Double>
    ): Double {

        if (pingResults.size < 2) {
            return 0.0
        }

        return pingResults
            .zipWithNext()
            .map { (first, second) ->
                abs(first - second)
            }
            .average()
    }

    /**
     * Measures download speed.
     *
     * Data is streamed instead of using readBytes(),
     * preventing unnecessary memory usage.
     */
    private fun measureDownload(): Double {

        var connection: HttpURLConnection? = null

        return try {

            connection =
                URL(DOWNLOAD_URL)
                    .openConnection() as HttpURLConnection

            connection.apply {

                requestMethod = "GET"

                connectTimeout =
                    DOWNLOAD_TIMEOUT_MS

                readTimeout =
                    DOWNLOAD_TIMEOUT_MS

                useCaches = false

                instanceFollowRedirects = true
            }

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {
                return 0.0
            }

            var totalBytes = 0L

            val start =
                System.nanoTime()

            BufferedInputStream(
                connection.inputStream
            ).use { input ->

                val buffer =
                    ByteArray(32 * 1024)

                while (true) {

                    val count =
                        input.read(buffer)

                    if (count == -1) {
                        break
                    }

                    totalBytes += count
                }
            }

            val elapsedSeconds =
                (System.nanoTime() - start) /
                    1_000_000_000.0

            if (
                totalBytes <= 0L ||
                elapsedSeconds <= 0.0
            ) {
                0.0
            } else {

                totalBytes *
                    BYTES_TO_MEGABITS /
                    elapsedSeconds
            }

        } catch (_: Exception) {

            0.0

        } finally {

            connection?.disconnect()
        }
    }

    /**
     * Measures upload speed.
     */
    private fun measureUpload(): Double {

        var connection: HttpURLConnection? = null

        return try {

            val payload =
                ByteArray(UPLOAD_SIZE_BYTES)

            connection =
                URL(UPLOAD_URL)
                    .openConnection() as HttpURLConnection

            connection.apply {

                requestMethod = "POST"

                connectTimeout =
                    UPLOAD_TIMEOUT_MS

                readTimeout =
                    UPLOAD_TIMEOUT_MS

                doOutput = true
                doInput = true

                useCaches = false

                instanceFollowRedirects = false

                setRequestProperty(
                    "Content-Type",
                    "application/octet-stream"
                )

                setFixedLengthStreamingMode(
                    UPLOAD_SIZE_BYTES
                )
            }

            val start =
                System.nanoTime()

            connection.outputStream.use { output ->

                output.write(payload)

                output.flush()
            }

            val responseCode =
                connection.responseCode

            val elapsedSeconds =
                (System.nanoTime() - start) /
                    1_000_000_000.0

            if (
                responseCode !in 200..299 ||
                elapsedSeconds <= 0.0
            ) {
                0.0
            } else {

                UPLOAD_SIZE_BYTES *
                    BYTES_TO_MEGABITS /
                    elapsedSeconds
            }

        } catch (_: Exception) {

            0.0

        } finally {

            connection?.disconnect()
        }
    }
}
