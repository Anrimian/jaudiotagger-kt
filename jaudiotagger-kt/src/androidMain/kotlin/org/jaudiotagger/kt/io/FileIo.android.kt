package org.jaudiotagger.kt.io

import android.system.Os
import java.io.FileDescriptor
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
    return FileChannelIo(FileChannel.open(java.nio.file.Paths.get(path.toString()), *options))
}

/**
 * [FileIo] over a [FileChannel], for callers that already hold a channel.
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

/**
 * [FileIo] over a raw [FileDescriptor] using POSIX calls — the MediaStore/SAF
 * path: obtain a descriptor with `contentResolver.openFileDescriptor(uri, "rw")`
 * and edit tags directly, no temp copies:
 *
 * ```kotlin
 * contentResolver.openFileDescriptor(uri, "rw")!!.use { pfd ->
 *     FileDescriptorIo(pfd.fileDescriptor).use { io ->
 *         val file = AudioTagger.read(io, AudioFormat.MP3)
 *         file.tag.set(FieldKey.TITLE, "New title")
 *         AudioTagger.write(io, file.tag, AudioFormat.MP3)
 *     }
 * }
 * ```
 *
 * Closing this object does not close the descriptor; the owner
 * (e.g. the ParcelFileDescriptor) stays responsible for it.
 */
class FileDescriptorIo(private val fd: FileDescriptor) : FileIo {

    override var position: Long = 0

    override val size: Long
        get() = Os.fstat(fd).st_size

    override fun read(dest: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val read = Os.pread(fd, dest, offset, length, position)
        if (read == 0) return -1
        position += read
        return read
    }

    override fun write(src: ByteArray, offset: Int, length: Int) {
        var written = 0
        while (written < length) {
            val n = Os.pwrite(fd, src, offset + written, length - written, position)
            if (n <= 0) throw kotlinx.io.IOException("pwrite returned $n")
            written += n
            position += n
        }
    }

    override fun truncate(newSize: Long) {
        Os.ftruncate(fd, newSize)
    }

    override fun flush() {
        Os.fsync(fd)
    }

    override fun close() {
        // deliberately not closing: the descriptor belongs to the caller
    }
}
