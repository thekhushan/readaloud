package com.readaloud.app.extraction

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OcrTextExtractor(
    private val grouper: MessageGrouper = MessageGrouper()
) {
    suspend fun extractFromAccessibilityScreenshot(service: AccessibilityService): List<String> {
        val bitmap = captureBitmap(service) ?: return emptyList()
        return try {
            recognize(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun captureBitmap(service: AccessibilityService): Bitmap? = suspendCoroutine { continuation ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            continuation.resume(null)
            return@suspendCoroutine
        }
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                    val hardwareBuffer = screenshot.hardwareBuffer
                    val wrapped = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                    val bitmap = wrapped?.copy(Bitmap.Config.ARGB_8888, false)
                    wrapped?.recycle()
                    hardwareBuffer.close()
                    continuation.resume(bitmap)
                }

                override fun onFailure(errorCode: Int) {
                    continuation.resume(null)
                }
            }
        )
    }

    private suspend fun recognize(bitmap: Bitmap): List<String> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        try {
            val latinText = runCatching { latin.process(image).await() }.getOrNull()
            val devanagariText = runCatching { devanagari.process(image).await() }.getOrNull()
            val combined = listOfNotNull(latinText, devanagariText)
                .flatMap { result -> linesFrom(result) }
                .joinToString("\n")
            grouper.fromOcrText(combined)
        } finally {
            latin.close()
            devanagari.close()
        }
    }

    private fun linesFrom(text: Text): List<String> {
        return text.textBlocks
            .flatMap { block -> block.lines }
            .sortedWith(compareBy<Text.Line> { it.boundingBox?.top ?: 0 }.thenBy { it.boundingBox?.left ?: 0 })
            .map { it.text }
    }
}
