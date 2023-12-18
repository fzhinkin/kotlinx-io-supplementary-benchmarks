package com.example.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.example.DiscardServer
import org.example.LoadClient
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.Exception
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import kotlin.random.Random

@RunWith(Parameterized::class)
public class SocketWriteBenchmarks(val bufferSize: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "bufferSize_{0}")
        fun data(): Array<Int> {
            return arrayOf(8, 128, 1024, 8192)
        }
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val port: Int = Random.nextInt(20000, 60000)
    val server: DiscardServer = DiscardServer(port, -1)
    val serverThread: Thread = Thread(server)

    @Before
    fun setup() {
        serverThread.start()
        server.awaitReady()
    }

    @After
    fun tearDown() {
        server.terminate()
        serverThread.join()
    }

    @Test
    fun outputStream() {
        val buffer = ByteArray(bufferSize)
        Socket().use { socket ->
            socket.connect(InetSocketAddress(port))
            val stream = socket.getOutputStream()

            benchmarkRule.measureRepeated {
                stream.write(buffer)
            }
        }
    }

    @Test
    fun channel() {
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        SocketChannel.open(InetSocketAddress(port)).use { channel ->
            benchmarkRule.measureRepeated {
                buffer.clear()
                while (buffer.hasRemaining()) {
                    channel.write(buffer)
                }
            }
        }
    }
}

@RunWith(Parameterized::class)
public class SocketReadBenchmarks(val bufferSize: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "bufferSize_{0}")
        fun data(): Array<Int> {
            return arrayOf(8, 128, 1024, 8192)
        }
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val port: Int = Random.nextInt(20000, 60000)
    val client: LoadClient = LoadClient(port, -1)
    val clientThread: Thread = Thread(client)

    @After
    fun tearDown() {
        client.terminate()
        try {
            clientThread.join()
        } catch (e: Exception) {
            // do nothing
        }
    }

    @Test
    fun inputStream() {
        val buffer = ByteArray(bufferSize)
        ServerSocket().use { serverSocket ->
            serverSocket.bind(InetSocketAddress(port))
            clientThread.start()
            serverSocket.accept().use { clientSocket ->
                val stream = clientSocket.getInputStream()
                benchmarkRule.measureRepeated {
                    var r = 0
                    while (r < bufferSize) {
                        val bytes = stream.read(buffer)
                        check(bytes > 0)
                        r += bytes
                    }
                }
                client.terminate()
            }
        }
    }

    @Test
    fun channel() {
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        ServerSocketChannel.open().use { serverSocket ->
            serverSocket.bind(InetSocketAddress(port))
            clientThread.start()
            serverSocket.accept().use { channel ->
                benchmarkRule.measureRepeated {
                    buffer.clear()
                    while (buffer.hasRemaining()) {
                        val bytes = channel.read(buffer)
                        check(bytes > 0)
                    }
                }
                client.terminate()
            }
        }
    }
}