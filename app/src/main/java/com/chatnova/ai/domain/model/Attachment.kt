package com.chatnova.ai.domain.model

data class Attachment(
    val id: String,
    val uriString: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val type: AttachmentType,
    val base64Data: String? = null,
    val extractedText: String? = null
)
