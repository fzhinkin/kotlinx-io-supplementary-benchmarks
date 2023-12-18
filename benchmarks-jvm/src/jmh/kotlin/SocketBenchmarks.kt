package org.example

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.random.Random

@State(Scope.Benchmark)
abstract class SocketBenchmarkBase {
    @Param("8", "128", "1024", "8192")
    var bufferSize: Int = 0

    protected val port = Random.nextInt(20000, 60000)
}

@State(Scope.Benchmark)
abstract class ByteBufferSocketBenchmark : SocketBenchmarkBase() {
    protected var buffer = ByteBuffer.allocateDirect(0)

    @Setup
    fun setupBuffer() {
        buffer = ByteBuffer.allocateDirect(bufferSize)
    }
}

@State(Scope.Benchmark)
abstract class ArraySocketBenchmark : SocketBenchmarkBase() {
    protected var buffer = ByteArray(0)

    @Setup
    fun setupBuffer() {
        buffer = ByteArray(bufferSize)
    }
}

@State(Scope.Benchmark)
open class SocketOutputStreamBenchmark : ArraySocketBenchmark() {
    private val server = DiscardServer(port, -1)
    private var serverThread: Thread? = null
    private val clientSocket = Socket()
    private lateinit var out: OutputStream

    @Setup
    fun setup() {
        serverThread = Thread(server)
        serverThread!!.start()
        server.awaitReady()

        clientSocket.connect(InetSocketAddress(port))
        out = clientSocket.getOutputStream()
    }

    @TearDown
    fun teardown() {
        clientSocket.close()
        server.terminate()
    }

    @Benchmark
    fun benchmark() {
        out.write(buffer)
    }
}

@State(Scope.Benchmark)
open class SocketInputStreamBenchmark : ArraySocketBenchmark() {
    private val client = org.example.LoadClient(port, -1)
    private var clientThread: Thread? = null
    private lateinit var clientSocket: Socket
    private val serverSocket = ServerSocket()
    private lateinit var input: InputStream

    @Setup
    fun setup() {
        serverSocket.bind(InetSocketAddress(port))

        clientThread = Thread(client)
        clientThread!!.start()
        clientSocket = serverSocket.accept()
        input = clientSocket.getInputStream()
    }

    @TearDown
    fun teardown() {
        input.close()
        client.terminate()
    }

    @Benchmark
    fun benchmark() {
        var r = 0
        while (r < bufferSize) {
            val bytes = input.read(buffer)
            check(bytes > 0)
            r += bytes
        }
    }
}

@State(Scope.Benchmark)
open class SocketChannelWriteBenchmark : ByteBufferSocketBenchmark() {
    private val server = DiscardServer(port, -1)
    private var serverThread: Thread? = null
    private lateinit var out: SocketChannel

    @Setup
    fun setup() {
        serverThread = Thread(server)
        serverThread!!.start()
        server.awaitReady()

        out = SocketChannel.open(InetSocketAddress(port))
    }

    @TearDown
    fun teardown() {
        out.close()
        server.terminate()
    }

    @Benchmark
    fun benchmark() {
        buffer.clear()
        while (buffer.hasRemaining()) {
            out.write(buffer)
        }
    }
}

@State(Scope.Benchmark)
open class SocketChannelReadBenchmark : ByteBufferSocketBenchmark() {
    private val client = org.example.LoadClient(port, -1)
    private var clientThread: Thread? = null
    private val serverSocket = ServerSocketChannel.open()
    private lateinit var input: SocketChannel

    @Setup
    fun setup() {
        serverSocket.bind(InetSocketAddress(port))
        clientThread = Thread(client)
        clientThread!!.start()
        input = serverSocket.accept()
    }

    @TearDown
    fun teardown() {
        input.close()
        client.terminate()
    }

    @Benchmark
    fun benchmark() {
        buffer.clear()
        while (buffer.hasRemaining()) {
            val bytes = input.read(buffer)
            check(bytes > 0)
        }
    }
}
