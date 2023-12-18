package org.example

import java.io.IOException
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.Volatile
import kotlin.math.max

const val DEFAULT_BUFFER_SIZE_8MB = 8 * 1024 * 1024

/**
 * Listens on local port #[port] until a single client connected and then
 * discards all data coming from it.
 */
class DiscardServer(private val port: Int, private val recvbuf: Int) : Runnable {
    @Volatile
    private var channel: ServerSocketChannel? = null

    @Volatile
    private var terminated = false

    private val latch = CountDownLatch(1)

    fun awaitReady() {
        latch.await()
    }

    override fun run() {
        val buffer = ByteBuffer.allocateDirect(max(DEFAULT_BUFFER_SIZE_8MB, recvbuf))
        try {
            channel = ServerSocketChannel.open(StandardProtocolFamily.INET)
            val localChannel = channel!!
            localChannel.bind(InetSocketAddress(port))
            if (recvbuf > 0) {
                localChannel.setOption(StandardSocketOptions.SO_RCVBUF, recvbuf)
            }
            localChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true)
            latch.countDown()
            run(localChannel, buffer)
        } catch (e: Exception) {
            if (!terminated) {
                throw RuntimeException(e)
            }
        } finally {
            if (channel != null && channel!!.isOpen) {
                try {
                    channel!!.close()
                } catch (e: IOException) {
                    // do nothing
                }
            }
        }
    }

    private fun run(localChannel: ServerSocketChannel, buffer: ByteBuffer) {
        val client = localChannel.accept()
        while (true) {
            val bytesRead = client.read(buffer)
            buffer.clear()
            if (bytesRead == -1) {
                client.close()
                break
            }
        }
    }

    fun terminate() {
        terminated = true
        try {
            channel!!.close()
        } catch (e: IOException) {
            // do nothing
        }
    }
}
