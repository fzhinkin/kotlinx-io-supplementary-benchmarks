package org.example

import java.io.IOException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import kotlin.concurrent.Volatile
import kotlin.math.max

/**
 * Connects to a server listening on local port #[port] and uploads data until disconnected.
 */
class LoadClient(private val port: Int, private val sndbufSize: Int) : Runnable {
    @Volatile
    private var terminated = false

    @Volatile
    private var channel: SocketChannel? = null

    override fun run() {
        val buffer = ByteBuffer.allocateDirect(max(org.example.DEFAULT_BUFFER_SIZE_8MB, sndbufSize))
        try {
            channel = SocketChannel.open()
            val localChannel = channel!!
            if (sndbufSize > 0) {
                localChannel.setOption(StandardSocketOptions.SO_SNDBUF, sndbufSize)
            }
            localChannel.connect(InetSocketAddress(port))
            while (true) {
                buffer.clear()
                localChannel.write(buffer)
            }
        } catch (e: IOException) {
            if (!terminated) {
                throw RuntimeException(e)
            }
        } finally {
            try {
                channel!!.close()
            } catch (e: IOException) {
                // do nothing
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
