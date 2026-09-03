package com.hwt.teacher.ui

import android.content.Context
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.hwt.teacher.data.ClassEntity
import com.hwt.teacher.data.StudentView
import com.hwt.teacher.util.QrCodec
import com.hwt.teacher.util.QrGenerator
import java.io.FileOutputStream

/** A4 二维码贴纸打印（FR-6.4）：系统打印，A4 纵向，仅输出二维码网格。 */
object QrPrintHelper {

    fun print(
        context: Context,
        cls: ClassEntity,
        students: List<StudentView>,
        perRow: Int,
        marginMm: Int,
        level: String,
        withNo: Boolean
    ) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            private val perPage = QrSheetLayout.perPage(perRow, marginMm, withNo)
            private val pageCount = maxOf(1, (students.size + perPage - 1) / perPage)

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: android.os.Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("qr")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(pageCount)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val doc = android.graphics.pdf.PdfDocument()
                    val requestedRanges = pages ?: arrayOf(PageRange(0, pageCount - 1))
                    val ranges = requestedRanges.mapNotNull { range ->
                        val start = maxOf(0, range.start)
                        val end = minOf(pageCount - 1, range.end)
                        if (start <= end) PageRange(start, end) else null
                    }.toTypedArray()
                    for (range in ranges) {
                        for (p in range.start..range.end) {
                            val pageSize = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                                QrSheetLayout.PAGE_W.toInt(),
                                QrSheetLayout.PAGE_H.toInt(),
                                p + 1
                            ).create()
                            val page = doc.startPage(pageSize)
                            drawPage(page.canvas, cls, students, perRow, marginMm, level, withNo, p)
                            doc.finishPage(page)
                        }
                    }
                    if (destination != null) {
                        doc.writeTo(FileOutputStream(destination.fileDescriptor))
                    }
                    doc.close()
                    callback?.onWriteFinished(ranges)
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        pm.print("二维码贴纸", adapter, null)
    }

    private fun drawPage(
        canvas: android.graphics.Canvas,
        cls: ClassEntity,
        students: List<StudentView>,
        perRow: Int,
        marginMm: Int,
        level: String,
        withNo: Boolean,
        page: Int
    ) {
        canvas.drawColor(Color.WHITE)
        val perPage = QrSheetLayout.perPage(perRow, marginMm, withNo)
        val pageStudents = students.drop(page * perPage).take(perPage)

        val marginPx = QrSheetLayout.marginPx(marginMm)
        val cellW = QrSheetLayout.cellWidth(perRow, marginMm)
        val cellH = QrSheetLayout.cellHeight(perRow, marginMm, withNo)
        val qrSize = QrSheetLayout.qrSize(perRow, marginMm)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(73, 69, 79)
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
            color = Color.rgb(160, 156, 164)
            pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
        }

        val qrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        val qrPx = (qrSize * 4f).toInt().coerceIn(180, 1024)

        pageStudents.forEachIndexed { i, s ->
            val col = i % perRow
            val row = i / perRow
            val left = marginPx + col * (cellW + QrSheetLayout.GAP)
            val top = marginPx + row * (cellH + QrSheetLayout.GAP)
            canvas.drawRect(left, top, left + cellW, top + cellH, cutPaint)

            val qr = QrGenerator.generate(
                QrCodec.encode(cls.id, s.student.id, s.code, s.student.name),
                qrPx,
                level
            )
            val qrLeft = left + (cellW - qrSize) / 2
            val qrTop = top + QrSheetLayout.PAD
            canvas.drawBitmap(qr, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), qrPaint)

            val cx = left + cellW / 2
            val nameY = qrTop + qrSize + QrSheetLayout.NAME_GAP
            canvas.drawText(s.student.name, cx, nameY, namePaint)
            if (withNo) {
                canvas.drawText(s.code, cx, nameY + QrSheetLayout.CODE_GAP, codePaint)
            }
        }
    }
}

/** A4 贴纸网格排版参数（打印与预览共用，单位为 PDF pt）。 */
object QrSheetLayout {
    const val PAGE_W = 595f
    const val PAGE_H = 842f
    const val GAP = 10f
    const val PAD = 6f
    const val NAME_GAP = 12f
    const val CODE_GAP = 11f

    /** 常见家用/办公打印机的不可打印区约 5 mm，低于此值内容会被裁掉。 */
    const val MIN_MARGIN_MM = 5

    val MARGIN_OPTIONS = listOf(5, 8, 10, 15)

    fun safeMarginMm(marginMm: Int): Int = marginMm.coerceAtLeast(MIN_MARGIN_MM)

    fun marginPx(marginMm: Int): Float = safeMarginMm(marginMm) * 72f / 25.4f

    fun textHeight(withNo: Boolean): Float = NAME_GAP + (if (withNo) CODE_GAP else 0f) + 2f

    fun cellWidth(perRow: Int, marginMm: Int): Float =
        (PAGE_W - marginPx(marginMm) * 2 - GAP * (perRow - 1)) / perRow

    fun qrSize(perRow: Int, marginMm: Int): Float = cellWidth(perRow, marginMm) - PAD * 2

    fun cellHeight(perRow: Int, marginMm: Int, withNo: Boolean): Float =
        PAD * 2 + qrSize(perRow, marginMm) + textHeight(withNo)

    /** 纵向按内容实高铺满 A4，而非固定 4 行。 */
    fun rowsPerPage(perRow: Int, marginMm: Int, withNo: Boolean): Int {
        val contentH = PAGE_H - marginPx(marginMm) * 2
        val cellH = cellHeight(perRow, marginMm, withNo)
        return maxOf(1, ((contentH + GAP) / (cellH + GAP)).toInt())
    }

    fun perPage(perRow: Int, marginMm: Int, withNo: Boolean): Int =
        perRow * rowsPerPage(perRow, marginMm, withNo)
}
