package com.hwt.teacher.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrGenerator {

    fun generate(content: String, sizePx: Int, level: String = "M"): Bitmap {
        val ecc = when (level) {
            "L" -> ErrorCorrectionLevel.L
            "Q" -> ErrorCorrectionLevel.Q
            "H" -> ErrorCorrectionLevel.H
            else -> ErrorCorrectionLevel.M
        }
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ecc,
            EncodeHintType.MARGIN to 0
        )
        val matrix: BitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val w = matrix.width
        val h = matrix.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.RGB_565)
    }
}

/** 二维码内容协议（附录 A）：HWT|班级ID|学生ID|学号|姓名 */
object QrCodec {

    fun encode(classId: String, studentId: String, code: String, name: String): String =
        "HWT|$classId|$studentId|$code|$name"

    data class Parsed(
        val classId: String,
        val studentId: String?,
        val code: String,
        val name: String
    )

    /** 解析并定位；返回 null 表示非本产品二维码。 */
    fun parse(raw: String): Parsed? {
        val parts = raw.split("|")
        if (parts.isEmpty() || parts[0] != "HWT" || parts.size < 4) return null
        return if (parts.size >= 5) {
            Parsed(parts[1], parts[2], parts[3], parts.drop(4).joinToString("|"))
        } else {
            // 旧版四段码：HWT|班级ID|学号|姓名
            Parsed(parts[1], null, parts[2], parts.drop(3).joinToString("|"))
        }
    }
}
