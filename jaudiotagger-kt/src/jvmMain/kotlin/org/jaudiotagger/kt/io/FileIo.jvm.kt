package org.jaudiotagger.kt.io

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlinx.io.files.Path

actual fun openFileIo(path: Path, readOnly: Boolean): FileIo {
    val options = if (readOnly) {
        arrayOf(StandardOpenOption.READ)
    } else {
        arrayOf(StandardOpenOption.READ, StandardOpenOption.WRITE)
    }
    return FileChannelIo(FileChannel.open(java.nio.file.Path.of(path.toString()), *options))
}

/**
 * [FileIo] over any [FileChannel]. Public so callers holding a channel from
 * elsewhere (e.g. one created from an Android file descriptor) can pass it in
 * without going through a filesystem path.
 */
class FileChannelIo(private val channel: FileChannel) : FileIo {

    override var position: Long
        get() = channel.position()
        set(value) {
            channel.position(value)
        }

    override val size: Long
        get() = channel.size()

    override fun read(dest: ByteArray, offset: Int, length: Int): Int =
        channel.read(ByteBuffer.wrap(dest, offset, length))

    override fun write(src: ByteArray, offset: Int, length: Int) {
        val buffer = ByteBuffer.wrap(src, offset, length)
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
    }

    override fun truncate(newSize: Long) {
        channel.truncate(newSize)
    }

    override fun flush() {
        channel.force(false)
    }

    override fun close() {
        channel.close()
    }
}

fun FileChannel.asFileIo(): FileIo = FileChannelIo(this)
