package com.hwt.teacher.util

import com.hwt.teacher.data.AssignmentRowView
import com.hwt.teacher.data.ClassEntity
import com.hwt.teacher.data.Completion
import com.hwt.teacher.data.Correction
import com.hwt.teacher.data.EntryRowView
import com.hwt.teacher.data.PersonReport
import java.io.ByteArrayOutputStream

/**
 * 报表导出（FR-8.6/8.8）：xlsx。
 * 单次作业完成情况表套用「作业登记表」模板版式（合并标题 + 六列等宽 + 细黑框全居中）；
 * 个人报表沿用通用版式（标题 + 摘要 + 紫底表头 + 冻结 + 筛选）。
 */
object ReportExporter {

    /**
     * 单次作业完成情况表：套用「作业登记表」模板版式。
     * 标题「班级 备注 日期 作业登记表」，列为 序号|姓名|完成情况|评级|订正情况|备注。
     */
    fun assignmentReport(cls: ClassEntity?, row: AssignmentRowView, entries: List<EntryRowView>): ByteArray {
        val title = listOf(
            cls?.name?.takeIf { it.isNotBlank() },
            cls?.note?.takeIf { it.isNotBlank() },
            DateUtil.monthDay(row.assignment.assignedDate),
            "作业登记表"
        ).filterNotNull().joinToString(" ")
        val columns = listOf("序号", "姓名", "完成情况", "评级", "订正情况", "备注")
        val rows = entries.mapIndexed { i, e ->
            listOf(
                (i + 1).toString(),
                e.student.name,
                Completion.label(e.completion),
                e.grade,
                Correction.label(e.correction),
                ""
            )
        }
        val out = ByteArrayOutputStream()
        XlsxUtil.writeForm(
            out = out,
            sheetName = "Sheet1",
            title = title,
            columns = columns,
            rows = rows,
            numericCols = setOf(0)
        )
        return out.toByteArray()
    }

    fun personReport(className: String, person: PersonReport): ByteArray {
        val titles = listOf(
            "$className · ${person.student.code} ${person.student.student.name}",
            "个人完成率 ${person.rate}%　共 ${person.items.size} 次作业　导出于 ${DateUtil.nowStamp()}"
        )
        val columns = listOf("作业", "日期", "完成情况", "订正情况", "评级")
        val rows = person.items.map { item ->
            listOf(
                item.assignment.title,
                item.assignment.assignedDate,
                Completion.label(item.completion),
                Correction.label(item.correction),
                item.grade
            )
        }
        return build("个人报表", titles, columns, rows, centerFrom = 2)
    }

    private fun build(
        sheetName: String,
        titles: List<String>,
        columns: List<String>,
        rows: List<List<String>>,
        centerFrom: Int
    ): ByteArray {
        val widths = columns.mapIndexed { c, header ->
            val body = rows.mapNotNull { it.getOrNull(c) }
            (listOf(header) + body).maxOf { XlsxUtil.displayWidth(it) }.coerceIn(8, 40)
        }
        val out = ByteArrayOutputStream()
        XlsxUtil.write(
            out = out,
            sheetName = sheetName,
            columns = columns,
            rows = rows,
            widths = widths,
            titles = titles,
            centerFrom = centerFrom
        )
        return out.toByteArray()
    }
}
