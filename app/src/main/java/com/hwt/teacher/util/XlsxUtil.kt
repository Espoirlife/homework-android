package com.hwt.teacher.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 轻量 XLSX 读写（避免 Apache POI 的体积与兼容性问题）。
 * 读取：仅第一个工作表，支持 sharedStrings / inlineStr / 数值 / 布尔单元格。
 * 写入：内联字符串（inlineStr），列宽按字符数（中文按 2 字符计）。
 */
object XlsxUtil {

    fun readFirstSheet(input: InputStream): List<List<String>> {
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val shared = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetTarget = resolveFirstSheet(entries)
        if (sheetTarget == null) return emptyList()
        return parseSheet(entries[sheetTarget], shared)
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val list = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var inSi = false
        var inT = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> { inSi = true; sb.setLength(0) }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> if (inT) sb.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "t" -> inT = false
                    "si" -> { inSi = false; list.add(sb.toString()) }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun resolveFirstSheet(entries: Map<String, ByteArray>): String? {
        val wb = entries["xl/workbook.xml"] ?: return null
        var rid: String? = null
        val p = Xml.newPullParser()
        p.setInput(wb.inputStream(), "UTF-8")
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT && rid == null) {
            if (event == XmlPullParser.START_TAG && p.name == "sheet") {
                rid = p.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
            }
            event = p.next()
        }
        if (rid == null) return null
        val rels = entries["xl/_rels/workbook.xml.rels"] ?: return null
        val rp = Xml.newPullParser()
        rp.setInput(rels.inputStream(), "UTF-8")
        var target: String? = null
        var event2 = rp.eventType
        while (event2 != XmlPullParser.END_DOCUMENT && target == null) {
            if (event2 == XmlPullParser.START_TAG && rp.name == "Relationship" && rp.getAttributeValue(null, "Id") == rid) {
                target = rp.getAttributeValue(null, "Target")
            }
            event2 = rp.next()
        }
        return when {
            target == null -> null
            else -> {
                val t = target.replace('\\', '/')
                when {
                    t.startsWith("/") -> "xl" + t.removePrefix("/xl").removePrefix("/")
                    t.startsWith("xl/") -> t
                    t.startsWith("worksheets/") -> "xl/$t"
                    else -> "xl/$t"
                }
            }
        }
    }

    private fun parseSheet(bytes: ByteArray?, shared: List<String>): List<List<String>> {
        if (bytes == null) return emptyList()
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var event = parser.eventType
        var currentRow = mutableListOf<String>()
        var colIdx = -1
        var inCell = false
        var inV = false
        var inT = false
        var cellType = ""
        val sb = StringBuilder()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> currentRow = mutableListOf()
                    "c" -> {
                        inCell = true
                        cellType = parser.getAttributeValue(null, "t") ?: ""
                        val ref = parser.getAttributeValue(null, "r")
                        colIdx = if (ref != null) {
                            val colLetters = ref.takeWhile { it.isLetter() }.uppercase()
                            if (colLetters.isEmpty()) -1
                            else colLetters.fold(0) { acc, ch -> acc * 26 + (ch.code - 'A'.code + 1) } - 1
                        } else -1
                        sb.setLength(0)
                    }
                    "v" -> if (inCell) inV = true
                    "t" -> if (inCell) inT = true
                }
                XmlPullParser.TEXT -> if (inV || inT) sb.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v" -> inV = false
                    "t" -> inT = false
                    "c" -> {
                        if (inCell) {
                            val raw = sb.toString()
                            val v = when (cellType) {
                                "s" -> shared.getOrElse(raw.toIntOrNull() ?: -1) { "" }
                                "inlineStr" -> raw
                                "b" -> if (raw == "1") "是" else "否"
                                else -> raw
                            }
                            if (colIdx >= 0) {
                                while (currentRow.size < colIdx) currentRow.add("")
                                if (currentRow.size == colIdx) currentRow.add(v) else currentRow[colIdx] = v
                            } else {
                                currentRow.add(v)
                            }
                            inCell = false
                        }
                    }
                    "row" -> rows.add(currentRow)
                }
            }
            event = parser.next()
        }
        return rows
    }

    fun write(
        out: OutputStream,
        sheetName: String,
        columns: List<String>,
        rows: List<List<String>>,
        widths: List<Int>? = null,
        titles: List<String> = emptyList(),
        centerFrom: Int = -1
    ) {
        val shared = LinkedHashMap<String, Int>()
        fun idx(s: String): Int = shared.getOrPut(s) { shared.size }
        val colWidths = widths ?: columns.mapIndexed { i, h ->
            val all = listOf(h) + rows.map { it.getOrElse(i) { "" } }
            all.map { displayWidth(it) }.maxOrNull()?.coerceIn(6, 40) ?: 10
        }
        val sheetXml = buildSheet(titles, columns, rows, colWidths, centerFrom, ::idx)
        pack(out, sheetName, sheetXml, buildSharedStrings(shared.keys.toList()))
    }

    /**
     * 登记表版式（对齐「作业登记表」模板）：
     * 第 1 行合并居中标题（微软雅黑 16 加粗），第 2 行表头，第 3 行起数据；
     * 表头与数据同款（华文中宋 14、细黑框、水平垂直居中），全列等宽。
     */
    fun writeForm(
        out: OutputStream,
        sheetName: String,
        title: String,
        columns: List<String>,
        rows: List<List<String>>,
        numericCols: Set<Int> = emptySet()
    ) {
        val shared = LinkedHashMap<String, Int>()
        fun idx(s: String): Int = shared.getOrPut(s) { shared.size }
        val sheetXml = buildFormSheet(title, columns, rows, numericCols, ::idx)
        pack(out, sheetName, sheetXml, buildSharedStrings(shared.keys.toList()))
    }

    private fun pack(out: OutputStream, sheetName: String, sheetXml: String, sharedXml: String) {
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(CONTENT_TYPES.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(RELS.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("docProps/core.xml"))
            zip.write(CORE_XML.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("docProps/app.xml"))
            zip.write(APP_XML.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(workbookXml(sheetName).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(WORKBOOK_RELS.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(STYLES.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedXml.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray())
            zip.closeEntry()
        }
    }

    private fun buildFormSheet(
        title: String,
        columns: List<String>,
        rows: List<List<String>>,
        numericCols: Set<Int>,
        idx: (String) -> Int
    ): String {
        val sb = StringBuilder()
        val colCount = columns.size.coerceAtLeast(1)
        val lastCol = colName(colCount - 1)
        val totalRows = 2 + rows.size
        val spans = " spans=\"1:$colCount\""
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("""<dimension ref="A1:$lastCol$totalRows"/>""")
        sb.append("""<sheetViews><sheetView tabSelected="1" workbookViewId="0"/></sheetViews>""")
        sb.append("""<sheetFormatPr defaultColWidth="8.72727272727273" defaultRowHeight="14"/>""")
        sb.append("""<cols><col min="1" max="$colCount" width="$FORM_COL_WIDTH" customWidth="1"/></cols>""")
        sb.append("<sheetData>")

        sb.append("""<row r="1" ht="22.5" customHeight="1"$spans>""")
        sb.append("""<c r="A1" s="$S_FORM_TITLE" t="s"><v>${idx(title)}</v></c>""")
        for (c in 1 until colCount) sb.append("""<c r="${colName(c)}1" s="$S_FORM_TITLE"/>""")
        sb.append("</row>")

        sb.append("""<row r="2" ht="19.5"$spans>""")
        columns.forEachIndexed { i, h ->
            sb.append("""<c r="${colName(i)}2" s="$S_FORM_CELL" t="s"><v>${idx(h)}</v></c>""")
        }
        sb.append("</row>")

        rows.forEachIndexed { ri, row ->
            val r = ri + 3
            sb.append("""<row r="$r" ht="19.5"$spans>""")
            for (ci in 0 until colCount) {
                val v = row.getOrNull(ci) ?: ""
                val ref = "${colName(ci)}$r"
                when {
                    v.isEmpty() -> sb.append("""<c r="$ref" s="$S_FORM_CELL"/>""")
                    ci in numericCols && v.toIntOrNull() != null ->
                        sb.append("""<c r="$ref" s="$S_FORM_CELL"><v>$v</v></c>""")
                    else ->
                        sb.append("""<c r="$ref" s="$S_FORM_CELL" t="s"><v>${idx(v)}</v></c>""")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")
        sb.append("""<mergeCells count="1"><mergeCell ref="A1:${lastCol}1"/></mergeCells>""")
        sb.append("""<pageMargins left="0.75" right="0.75" top="1" bottom="1" header="0.5" footer="0.5"/>""")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun buildSheet(
        titles: List<String>,
        columns: List<String>,
        rows: List<List<String>>,
        colWidths: List<Int>,
        centerFrom: Int,
        idx: (String) -> Int
    ): String {
        val sb = StringBuilder()
        val colCount = columns.size.coerceAtLeast(1)
        val lastCol = colName(colCount - 1)
        val headerRow = titles.size + 1
        val totalRows = titles.size + 1 + rows.size
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("""<dimension ref="A1:$lastCol$totalRows"/>""")
        sb.append("""<sheetViews><sheetView workbookViewId="0"><pane ySplit="$headerRow" topLeftCell="A${headerRow + 1}" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        sb.append("""<sheetFormatPr defaultRowHeight="16.5"/>""")
        sb.append("<cols>")
        colWidths.forEachIndexed { i, w ->
            sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
        }
        sb.append("</cols><sheetData>")

        titles.forEachIndexed { ti, t ->
            val style = if (ti == 0) S_TITLE else S_SUBTITLE
            val h = if (ti == 0) """ ht="22" customHeight="1"""" else ""
            sb.append("""<row r="${ti + 1}"$h>""")
            sb.append("""<c r="A${ti + 1}" s="$style" t="s"><v>${idx(t)}</v></c>""")
            for (c in 1 until colCount) {
                sb.append("""<c r="${colName(c)}${ti + 1}" s="$style"/>""")
            }
            sb.append("</row>")
        }

        sb.append("""<row r="$headerRow" ht="20" customHeight="1">""")
        columns.forEachIndexed { i, h ->
            sb.append("""<c r="${colName(i)}$headerRow" s="$S_HEADER" t="s"><v>${idx(h)}</v></c>""")
        }
        sb.append("</row>")

        rows.forEachIndexed { ri, row ->
            val r = headerRow + 1 + ri
            sb.append("""<row r="$r">""")
            for (ci in 0 until colCount) {
                val v = row.getOrNull(ci) ?: ""
                val style = if (centerFrom >= 0 && ci >= centerFrom) S_CELL_CENTER else S_CELL
                if (v.isEmpty()) {
                    sb.append("""<c r="${colName(ci)}$r" s="$style"/>""")
                } else {
                    sb.append("""<c r="${colName(ci)}$r" s="$style" t="s"><v>${idx(v)}</v></c>""")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")
        if (rows.isNotEmpty()) {
            sb.append("""<autoFilter ref="A$headerRow:$lastCol$totalRows"/>""")
        }
        sb.append("""<pageMargins left="0.5" right="0.5" top="0.6" bottom="0.6" header="0.3" footer="0.3"/>""")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun buildSharedStrings(list: List<String>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${list.size}" uniqueCount="${list.size}">""")
        list.forEach { s ->
            val text = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            sb.append("""<si><t xml:space="preserve">$text</t></si>""")
        }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun colName(i: Int): String {
        var n = i + 1
        var s = ""
        while (n > 0) {
            val rem = (n - 1) % 26
            s = ('A' + rem) + s
            n = (n - 1) / 26
        }
        return s
    }

    fun displayWidth(s: String): Int {
        var w = 0
        for (ch in s) w += if (ch.code > 0xFF) 2 else 1
        return w + 2
    }

    private val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>"""

    private val RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""

    private val CORE_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<dc:creator>HwtTeacher</dc:creator>
<cp:lastModifiedBy>HwtTeacher</cp:lastModifiedBy>
</cp:coreProperties>"""

    private val APP_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties">
<Application>HwtTeacher</Application>
</Properties>"""

    private const val S_TITLE = 1
    private const val S_SUBTITLE = 2
    private const val S_HEADER = 3
    private const val S_CELL = 4
    private const val S_CELL_CENTER = 5
    private const val S_FORM_TITLE = 6
    private const val S_FORM_CELL = 7

    /** 模板列宽（13.64 字符，约合模板 A:F 等宽设置）。 */
    private const val FORM_COL_WIDTH = "13.6363636363636"

    private val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="6">
<font><sz val="11"/><color theme="1"/><name val="等线"/><family val="2"/></font>
<font><b/><sz val="14"/><color rgb="FF1D1B20"/><name val="等线"/><family val="2"/></font>
<font><sz val="10"/><color rgb="FF49454F"/><name val="等线"/><family val="2"/></font>
<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="等线"/><family val="2"/></font>
<font><b/><sz val="16"/><color rgb="FF000000"/><name val="微软雅黑"/><charset val="134"/></font>
<font><sz val="14"/><color rgb="FF000000"/><name val="华文中宋"/><charset val="134"/></font>
</fonts>
<fills count="3">
<fill><patternFill patternType="none"/></fill>
<fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FF6750A4"/><bgColor indexed="64"/></patternFill></fill>
</fills>
<borders count="3">
<border><left/><right/><top/><bottom/><diagonal/></border>
<border>
<left style="thin"><color rgb="FFCAC4D0"/></left>
<right style="thin"><color rgb="FFCAC4D0"/></right>
<top style="thin"><color rgb="FFCAC4D0"/></top>
<bottom style="thin"><color rgb="FFCAC4D0"/></bottom>
<diagonal/>
</border>
<border>
<left style="thin"><color rgb="FF000000"/></left>
<right style="thin"><color rgb="FF000000"/></right>
<top style="thin"><color rgb="FF000000"/></top>
<bottom style="thin"><color rgb="FF000000"/></bottom>
<diagonal/>
</border>
</borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="8">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="3" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="49" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="49" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="4" fillId="0" borderId="2" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
<xf numFmtId="0" fontId="5" fillId="0" borderId="2" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    private fun workbookXml(name: String) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="${name.replace("&", "&amp;")}" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""
}
