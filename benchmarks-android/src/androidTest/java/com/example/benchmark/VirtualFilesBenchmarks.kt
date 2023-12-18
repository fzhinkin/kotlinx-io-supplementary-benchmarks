package com.example.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

const val DEV_NULL = "/dev/null"
const val DEV_ZERO = "/dev/zero"

@RunWith(Parameterized::class)
public class VirtualFilesBenchmarks(val bufferSize: Int) {

    companion object {
        @JvmStatic
        @Parameters(name = "bufferSize_{0}")
        fun data(): Array<Int> {
            return arrayOf(8, 128, 1024, 8192)
        }
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun inputStreamReadDevZero() {
        val buffer = ByteArray(bufferSize)
        FileInputStream(DEV_ZERO).use { io ->
            benchmarkRule.measureRepeated {
                var read = 0
                while (read < bufferSize) {
                    val bytes = io.read(buffer, read, bufferSize - read)
                    check(bytes > 0)
                    read += bytes
                }
            }
        }
    }

    @Test
    fun outputStreamWriteDevNull() {
        val buffer = ByteArray(bufferSize)
        FileOutputStream(DEV_NULL).use { io ->
            benchmarkRule.measureRepeated {
                io.write(buffer)
            }
        }
    }

    @Test
    fun fileChannelReadDevZero() {
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        FileChannel.open(Paths.get(DEV_ZERO), StandardOpenOption.READ).use { io ->
            benchmarkRule.measureRepeated {
                buffer.clear()
                while (buffer.hasRemaining()) {
                    val bytes = io.read(buffer)
                    check(bytes > 0)
                }
            }
        }
    }

    @Test
    fun fileChannelWriteDevNull() {
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        FileChannel.open(Paths.get(DEV_NULL), StandardOpenOption.WRITE).use { io ->
            benchmarkRule.measureRepeated {
                buffer.clear()
                while (buffer.hasRemaining()) {
                    io.write(buffer)
                }
            }
        }
    }
}
