package com.hwt.teacher.util

import java.util.UUID

object Ids {
    fun new(): String = UUID.randomUUID().toString()
}

object DateUtil {
    private val DATE_FMT = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
    private val MONTH_DAY_FMT = java.text.SimpleDateFormat("MM月dd日", java.util.Locale.CHINA)
    private val MONTH_DAY_SLASH_FMT = java.text.SimpleDateFormat("MM/dd", java.util.Locale.CHINA)
    private val STAMP_FMT = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
    private val BACKUP_FMT = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.CHINA)
    private val ISO_FMT = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.CHINA)

    fun today(): String = DATE_FMT.format(java.util.Date())

    fun monthDay(dateStr: String): String = try {
        MONTH_DAY_FMT.format(DATE_FMT.parse(dateStr)!!)
    } catch (e: Exception) { dateStr }

    fun monthDaySlash(dateStr: String): String = try {
        MONTH_DAY_SLASH_FMT.format(DATE_FMT.parse(dateStr)!!)
    } catch (e: Exception) { dateStr }

    fun nowStamp(): String = STAMP_FMT.format(java.util.Date())

    fun stamp(epoch: Long?): String = epoch?.let { STAMP_FMT.format(java.util.Date(it)) } ?: "—"

    fun backupName(): String = "hwt-backup-${BACKUP_FMT.format(java.util.Date())}.json"

    fun toIso(epoch: Long?): String? = epoch?.let { ISO_FMT.format(java.util.Date(it)) }

    fun fromIso(iso: String?): Long? = try {
        iso?.let { ISO_FMT.parse(it)?.time }
    } catch (e: Exception) { null }
}
