package com.hwt.teacher.data

/** 完成状态 / 订正状态 / 评级 的稳定字符串常量与文案、标签。 */
object Completion {
    const val MISS = "miss"
    const val DONE = "done"
    const val PARTIAL = "partial"
    val ALL = listOf(MISS, DONE, PARTIAL)

    fun label(v: String?): String = when (v) {
        DONE -> "已完成"
        PARTIAL -> "部分完成"
        else -> "未完成"
    }

    fun mark(v: String?): String = when (v) {
        DONE -> "\u2713"
        PARTIAL -> "半"
        else -> "\u2717"
    }

    fun next(v: String?): String {
        val i = ALL.indexOf(v ?: MISS)
        return ALL[(i + 1) % ALL.size]
    }

    fun counted(v: String?): Boolean = v == DONE || v == PARTIAL
}

object Correction {
    const val PENDING = "pending"
    const val FIXED = "fixed"
    val ALL = listOf(PENDING, FIXED)

    fun label(v: String?): String = when (v) {
        FIXED -> "订正完成"
        else -> "讲解后订正"
    }

    fun mark(v: String?): String = when (v) {
        FIXED -> "\u2713"
        else -> "待"
    }

    fun next(v: String?): String {
        val i = ALL.indexOf(v ?: PENDING)
        return ALL[(i + 1) % ALL.size]
    }
}

object Grade {
    const val NONE = ""
    val ALL = listOf(NONE, "A", "B", "C")

    fun label(v: String?): String = if (v.isNullOrEmpty()) "未评" else v

    fun mark(v: String?): String = if (v.isNullOrEmpty()) "—" else v

    fun next(v: String?): String {
        val i = ALL.indexOf(v ?: NONE)
        return ALL[(i + 1) % ALL.size]
    }
}

object Marks {
    const val GROUP_COMPLETION = "completion"
    const val GROUP_CORRECTION = "correction"
    const val GROUP_GRADE = "grade"

    fun groupLabel(key: String): String = when (key) {
        GROUP_COMPLETION -> "完成情况"
        GROUP_CORRECTION -> "订正情况"
        else -> "评级"
    }
}
