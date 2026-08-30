package com.chatnova.ai.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.chatnova.ai.domain.model.Attachment
import com.chatnova.ai.domain.model.AttachmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileAttachmentHelper {

    private const val MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB

    suspend fun processUri(context: Context, uri: Uri): Result<Attachment> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "attachment_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("File size exceeds limit of 25MB (${DateUtils.formatFileSize(fileSize)})")
                )
            }

            val mimeType = contentResolver.getType(uri) ?: getMimeTypeFromExtension(fileName)
            val attachmentType = determineAttachmentType(mimeType, fileName)

            var base64Data: String? = null
            var extractedText: String? = null

            // Cache file locally
            val cacheDir = File(context.cacheDir, "attachments").apply { mkdirs() }
            val localFile = File(cacheDir, "${UUID.randomUUID()}_$fileName")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }

            fileSize = localFile.length()

            when (attachmentType) {
                AttachmentType.IMAGE -> {
                    val bytes = localFile.readBytes()
                    base64Data = "data:$mimeType;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
                AttachmentType.DOCUMENT -> {
                    if (isTextReadable(mimeType, fileName)) {
                        extractedText = localFile.readText(Charsets.UTF_8).take(100_000) // Safe token limit
                    }
                }
                AttachmentType.VIDEO, AttachmentType.AUDIO -> {
                    // Multimedia attached metadata
                }
            }

            val attachment = Attachment(
                id = UUID.randomUUID().toString(),
                uriString = Uri.fromFile(localFile).toString(),
                name = fileName,
                mimeType = mimeType,
                sizeBytes = fileSize,
                type = attachmentType,
                base64Data = base64Data,
                extractedText = extractedText
            )

            Result.success(attachment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun determineAttachmentType(mimeType: String, fileName: String): AttachmentType {
        return when {
            mimeType.startsWith("image/") -> AttachmentType.IMAGE
            mimeType.startsWith("video/") -> AttachmentType.VIDEO
            mimeType.startsWith("audio/") -> AttachmentType.AUDIO
            else -> AttachmentType.DOCUMENT
        }
    }

    private fun isTextReadable(mimeType: String, fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return mimeType.startsWith("text/") ||
                mimeType.contains("json") ||
                mimeType.contains("xml") ||
                mimeType.contains("javascript") ||
                ext in listOf("txt", "md", "json", "csv", "xml", "html", "py", "kt", "java", "js", "ts", "cpp", "c", "h", "log", "yaml", "yml")
    }

    private fun getMimeTypeFromExtension(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mov" -> "video/quicktime"
            "webm" -> "video/webm"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "json" -> "application/json"
            "csv" -> "text/csv"
            "md" -> "text/markdown"
            else -> "application/octet-stream"
        }
    }

    fun cleanCache(context: Context): Long {
        val cacheDir = File(context.cacheDir, "attachments")
        var freedBytes = 0L
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                freedBytes += file.length()
                file.delete()
            }
        }
        return freedBytes
    }
}
