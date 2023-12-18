package com.example.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption

const val REOPEN_AT = 16 * 1024 * 1024

@RunWith(Parameterized::class)
public class FilesBenchmarks(val bufferSize: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "bufferSize_{0}")
        fun data(): Array<Int> {
            return arrayOf(8, 128, 1024, 8192)
        }
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    lateinit var tempFile: File

    @Before
    fun setup() {
        tempFile = Files.createTempFile("benchmark", ".bin").toFile()
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    fun fillFile() {
        FileChannel.open(tempFile.toPath(), StandardOpenOption.WRITE).use {
            val buffer = ByteBuffer.allocateDirect(1024 * 1024)
            var written = 0
            val toWrite = REOPEN_AT + bufferSize
            while (written < toWrite) {
                buffer.clear()
                written += it.write(buffer)
            }
        }
    }

    @Test
    fun fileOutputStream() {
        val buffer = ByteArray(bufferSize)
        var stream = FileOutputStream(tempFile)
        var totalWritten = 0
        try {
            benchmarkRule.measureRepeated {
                stream.write(buffer)
                totalWritten += bufferSize
                if (totalWritten >= REOPEN_AT) {
                    stream.close()
                    stream = FileOutputStream(tempFile)
                    totalWritten = 0
                }
            }
        } finally {
            stream.closeQuietly()
        }
    }

    @Test
    fun fileInputStream() {
        fillFile()
        val buffer = ByteArray(bufferSize)
        var stream = FileInputStream(tempFile)
        var readTotal = 0

        try {
            benchmarkRule.measureRepeated {
                var read = 0
                while (read < bufferSize) {
                    val bytes = stream.read(buffer, read, bufferSize - read)
                    check(bytes > 0)
                    read += bytes
                }
                readTotal += read
                if (readTotal >= REOPEN_AT) {
                    stream.close()
                    stream = FileInputStream(tempFile)
                    readTotal = 0
                }
            }
        } finally {
            stream.closeQuietly()
        }
    }

    @Test
    fun fileChannelWrite() {
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        var channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.WRITE)
        var totalWritten = 0

        try {
            benchmarkRule.measureRepeated {
                buffer.clear()
                while (buffer.hasRemaining()) {
                    channel.write(buffer)
                }
                totalWritten += bufferSize
                if (totalWritten >= REOPEN_AT) {
                    channel.close()
                    channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.WRITE)
                    totalWritten = 0
                }
            }
        } finally {
            channel.closeQuietly()
        }
    }

    @Test
    fun fileChannelRead() {
        fillFile()
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        var channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.READ)
        var totalRead = 0

        try {
            benchmarkRule.measureRepeated {
                buffer.clear()
                while (buffer.hasRemaining()) {
                    val bytes = channel.read(buffer)
                    check(bytes > 0)
                }
                totalRead += bufferSize
                if (totalRead >= REOPEN_AT) {
                    channel.close()
                    channel = FileChannel.open(tempFile.toPath(), StandardOpenOption.READ)
                    totalRead = 0
                }
            }
        } finally {
            channel.closeQuietly()
        }
    }
}

private fun Closeable.closeQuietly() {
    try {
        this.close()
    } catch (e: IOException) {
        // do nothing
    }
}