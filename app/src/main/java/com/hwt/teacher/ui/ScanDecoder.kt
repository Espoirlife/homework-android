package com.hwt.teacher.ui

import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/** CameraX ImageProxy → ZXing 解码（无 GMS 依赖，全设备可用）。 */
object ScanDecoder {
    fun decode(image: ImageProxy): String? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixels = ByteArray(buffer.remaining())
            buffer.get(pixels)
            val width = image.width
            val height = image.height
            val source = PlanarYUVLuminanceSource(pixels, width, height, 0, 0, width, height, false)
            val binary = com.google.zxing.BinaryBitmap(HybridBinarizer(source))
            val reader = MultiFormatReader()
            reader.setHints(
                mapOf(
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
                )
            )
            reader.decode(binary).text
        } catch (e: Exception) {
            null
        }
    }
}
