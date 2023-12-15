package org.example

import org.openjdk.jmh.annotations.*
import java.io.Closeable
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

const val DEV_NULL = "/dev/null"
const val DEV_ZERO = "/dev/zero"

@State(Scope.Benchmark)
abstract class VirtualFilesBenchmarkBase<T : Closeable>(protected var io: T) {
    @Param("8", "128", "1024", "8192")
    var bufferSize: Int = 0

    @TearDown
    fun close() {
        io.close()
    }
}

@State(Scope.Benchmark)
abstract class ByteBufferBackedBenchmarks<T : Closeable>(io: T) : VirtualFilesBenchmarkBase<T>(io) {
    protected var buffer: ByteBuffer = ByteBuffer.allocate(0)

    @Setup
    fun setupBuffer() {
        buffer = ByteBuffer.allocateDirect(bufferSize)
    }
}

@State(Scope.Benchmark)
abstract class ArrayBackedBenchmarks<T : Closeable>(io: T) : VirtualFilesBenchmarkBase<T>(io) {
    protected var buffer = ByteArray(0)

    @Setup
    fun setupBuffer() {
        buffer = ByteArray(bufferSize)
    }
}

@State(Scope.Benchmark)
open class FileOutputStreamWriteToDevNull : ArrayBackedBenchmarks<FileOutputStream>(FileOutputStream(DEV_NULL)) {
    @Benchmark
    fun benchmark() {
        io.write(buffer)
    }
}

@State(Scope.Benchmark)
open class FileInputStreamReadFromDevZero : ArrayBackedBenchmarks<FileInputStream>(FileInputStream(DEV_ZERO)) {
    @Benchmark
    fun benchmark() {
        var read = 0
        while (read < bufferSize) {
            val bytes = io.read(buffer, read, bufferSize - read)
            check(bytes > 0)
            read += bytes
        }
    }
}

@State(Scope.Benchmark)
open class FileChannelWriteToDevNull : ByteBufferBackedBenchmarks<FileChannel>(
    FileChannel.open(Paths.get(DEV_NULL), StandardOpenOption.WRITE)
) {
    @Benchmark
    fun benchmark() {
        buffer.clear()
        while (buffer.hasRemaining()) {
            io.write(buffer)
        }
    }
}

@State(Scope.Benchmark)
open class FileChannelReadFromDevZero : ByteBufferBackedBenchmarks<FileChannel>(
    FileChannel.open(Paths.get(DEV_ZERO), StandardOpenOption.READ)
) {
    @Benchmark
    fun benchmark() {
        buffer.clear()
        while (buffer.hasRemaining()) {
            val bytes = io.read(buffer)
            check(bytes > 0)
        }
    }
}
