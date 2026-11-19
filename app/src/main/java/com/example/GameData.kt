package com.example

import java.io.Serializable

data class Level(
    val id: Int,
    val letters: List<Char>,
    val targetWords: List<String>,
    val bonusWords: List<String> = emptyList()
) : Serializable

object GameData {
    val levels = listOf(
        // Level 1: "نظری" - Themed around Nazari (نظری)
        Level(
            id = 1,
            letters = listOf('ن', 'ظ', 'ر', 'ی'),
            targetWords = listOf("نی", "نظر", "نظری"),
            bonusWords = listOf("ری", "زین", "رن", "نر")
        ),
        // Level 2: "فضل" - Virtue (فضل) from Abolfazl
        Level(
            id = 2,
            letters = listOf('ف', 'ض', 'ل'),
            targetWords = listOf("لف", "فل", "فضل"),
            bonusWords = listOf("ضل")
        ),
        // Level 3: "باوفا" - Loyal (باوفا) - incorporating 'ب', 'ا', 'و', 'ف'
        Level(
            id = 3,
            letters = listOf('ب', 'ا', 'و', 'ف', 'ا'),
            targetWords = listOf("بو", "آب", "وفا", "بوف", "باوفا"),
            bonusWords = listOf("وب", "با", "ابوا")
        ),
        // Level 4: "راضی" - Pleased (راضی)
        Level(
            id = 4,
            letters = listOf('ر', 'ا', 'ض', 'ی'),
            targetWords = listOf("ری", "یا", "یار", "ریا", "راضی"),
            bonusWords = listOf("آری", "راض")
        ),
        // Level 5: "ناظر" - Observer (ناظر) - related to Nazari
        Level(
            id = 5,
            letters = listOf('ن', 'ا', 'ظ', 'ر', 'ی'),
            targetWords = listOf("ران", "نظر", "ناظر", "نظیر", "نظری", "ایران"),
            bonusWords = listOf("یار", "رای", "آری", "آنی", "نای", "رن", "نظ")
        ),
        // Level 6: "طلایی" - Golden (طلایی)
        Level(
            id = 6,
            letters = listOf('ط', 'ل', 'ا', 'ی', 'ی'),
            targetWords = listOf("طی", "طلا", "طال", "طلایی"),
            bonusWords = listOf("یل", "لیا", "ایست")
        ),
        // Level 7: "افول" - Decline (افول)
        Level(
            id = 7,
            letters = listOf('ا', 'ب', 'و', 'ل', 'ف'),
            targetWords = listOf("آب", "بو", "لف", "بوف", "الف", "فال", "افول", "بلوا"),
            bonusWords = listOf("لبو", "فلو", "ابوا", "وب")
        ),
        // Level 8: "فاضل" - Erudite (فاضل) - from Abolfazl
        Level(
            id = 8,
            letters = listOf('ف', 'ا', 'ض', 'ل', 'ی'),
            targetWords = listOf("لف", "فال", "فضل", "فاضل", "فضلی", "فیاض"),
            bonusWords = listOf("ایل", "ضیا", "یاور")
        ),
        // Level 9: "بوفالو" - Buffalo (بوفالو)
        Level(
            id = 9,
            letters = listOf('ب', 'و', 'ف', 'ا', 'ل', 'و'),
            targetWords = listOf("بو", "وب", "بوف", "فال", "افول", "بلوا", "بوفالو"),
            bonusWords = listOf("لبو", "فلو", "ابوا", "لاف")
        ),
        // Level 10: "ابوالفضل" - The Ultimate Level dedicated to Abolfazl (ابوالفضل)
        Level(
            id = 10,
            letters = listOf('ا', 'ب', 'و', 'ا', 'ل', 'ف', 'ض', 'ل'),
            targetWords = listOf("فضا", "فضل", "بوف", "الف", "فال", "فاضل", "افول", "بلوا", "ابوالفضل"),
            bonusWords = listOf("لبو", "بوفالو", "لولا", "بالا", "وب", "لاف")
        )
    )
}

fun Int.toPersianDigits(): String {
    val persianDigits = arrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return this.toString().map { char ->
        if (char.isDigit()) persianDigits[char - '0'] else char
    }.joinToString("")
}
