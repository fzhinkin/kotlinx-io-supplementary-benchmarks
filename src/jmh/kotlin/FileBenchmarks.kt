package org.example

import org.openjdk.jmh.annotations.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption


@State(Scope.Benchmark)
abstract class FileBenchmarksBase {
    @Param("8", "128", "1024", "8192")
    var bufferSize: Int = 0

    @Param("1073741824") // 1G by default
    var reopenAt: Int = 0

    protected val filePath: Path = Files.createTempFile("benchmark", ".bin")
    protected val file = filePath.toFile()
    abstract fun reopen()

    abstract fun close()

    @Setup(Level.Iteration)
    fun beforeIteration() {
        reopen()
    }

    @Setup(Level.Iteration)
    fun afterIteration() {
        close()
    }

    @TearDown
    fun delete() {
        file.delete()
    }

    protected fun fillTheFile() {
        FileChannel.open(filePath, StandardOpenOption.WRITE).use {
            val buffer = ByteBuffer.allocateDirect(1024 * 1024)
            var written = 0
            val toWrite = reopenAt + bufferSize
            while (written < toWrite) {
                buffer.clear()
                written += it.write(buffer)
            }
        }
    }
}

@State(Scope.Benchmark)
abstract class ArrayFileBenchmark : FileBenchmarksBase() {
    protected var buffer = ByteArray(0)

    @Setup
    fun setupBuffer() {
        buffer = ByteArray(bufferSize)
    }
}

@State(Scope.Benchmark)
abstract class ByteBufferFileBenchmark : FileBenchmarksBase() {
    protected var buffer: ByteBuffer = ByteBuffer.allocateDirect(0)

    @Setup
    fun setupBuffer() {
        buffer = ByteBuffer.allocateDirect(bufferSize)
    }
}

@State(Scope.Benchmark)
open class FileOutputStreamBenchmark : ArrayFileBenchmark() {
    private var out = FileOutputStream(file).also { it.close() }
    private var bytesWritten = 0

    override fun close() {
        out.close()
    }

    override fun reopen() {
        bytesWritten = 0
        out = FileOutputStream(file)
    }

    @Benchmark
    fun benchmark() {
        out.write(buffer)
        bytesWritten += bufferSize
        if (bytesWritten >= reopenAt) {
            close()
            reopen()
        }
    }
}

@State(Scope.Benchmark)
open class FileInputStreamBenchmark : ArrayFileBenchmark() {
    private var input = FileInputStream(file).also { it.close() }
    private var bytesRead = 0

    override fun close() {
        input.close()
    }

    override fun reopen() {
        bytesRead = 0
        input = FileInputStream(file)
    }

    @Setup
    fun createFile() {
        fillTheFile()
    }

    @Benchmark
    fun benchmark() {
        var read = 0
        while (read < bufferSize) {
            val bytes = input.read(buffer, read, bufferSize - read)
            check(bytes > 0)
            read += bytes
        }
        bytesRead += read
        if (bytesRead >= reopenAt) {
            close()
            reopen()
        }
    }
}

@State(Scope.Benchmark)
open class FileChannelWriteBenchmark : ByteBufferFileBenchmark() {
    private var out = FileChannel.open(filePath).also { it.close() }
    private var bytesWritten = 0

    override fun close() {
        out.close()
    }

    override fun reopen() {
        bytesWritten = 0
        out = FileChannel.open(filePath, StandardOpenOption.WRITE)
    }

    @Benchmark
    fun benchmark() {
        buffer.clear()
        while (buffer.hasRemaining()) {
            out.write(buffer)
        }
        bytesWritten += bufferSize
        if (bytesWritten >= reopenAt) {
            close()
            reopen()
        }
    }
}

@State(Scope.Benchmark)
open class FileChannelReadBenchmark : ByteBufferFileBenchmark() {
    private var input = FileChannel.open(filePath).also { it.close() }
    private var bytesRead = 0

    override fun close() {
        input.close()
    }

    override fun reopen() {
        bytesRead = 0
        input = FileChannel.open(filePath, StandardOpenOption.READ)
    }

    @Setup
    fun createFile() {
        fillTheFile()
    }

    @Benchmark
    fun benchmark() {
        buffer.clear()
        while (buffer.hasRemaining()) {
            val bytes = input.read(buffer)
            check(bytes > 0)
        }
        bytesRead += bufferSize
        if (bytesRead >= reopenAt) {
            close()
            reopen()
        }
    }
}
