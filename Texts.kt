package com.ssc27.tracker

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.random.Random

fun bnDigits(s: String): String = s.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")

fun num(n: Long, lang: String): String = if (lang == "bn") bnDigits(n.toString()) else n.toString()

fun daysLeft(exam: LocalDate): Long = ChronoUnit.DAYS.between(LocalDate.now(), exam)

private val bnMonths = listOf(
    "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
    "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
)

fun fmtDate(d: LocalDate, lang: String): String =
    if (lang == "bn") bnDigits("${d.dayOfMonth}") + " " + bnMonths[d.monthValue - 1] + " " + bnDigits("${d.year}")
    else "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${d.year}"

fun fmtDateTime(ms: Long, lang: String): String {
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val time = "%02d:%02d".format(dt.hour, dt.minute)
    val t = if (lang == "bn") bnDigits(time) else time
    return fmtDate(dt.toLocalDate(), lang) + ", " + t
}

fun relTime(ms: Long, lang: String): String {
    val mins = (System.currentTimeMillis() - ms) / 60000L
    val hours = mins / 60L
    val bn = lang == "bn"
    return when {
        mins < 1L -> if (bn) "এইমাত্র" else "Now"
        mins < 60L -> if (bn) num(mins, lang) + " মিনিট আগে" else if (mins == 1L) "1 minute ago" else "$mins minutes ago"
        hours < 24L -> if (bn) num(hours, lang) + " ঘণ্টা আগে" else if (hours == 1L) "1 hour ago" else "$hours hours ago"
        else -> fmtDateTime(ms, lang)
    }
}

object Texts {
    fun countdownLine(name: String, days: Long, exam: LocalDate, lang: String): String {
        val d = fmtDate(exam, lang)
        val n = num(days, lang)
        return if (lang == "bn") {
            when {
                days > 0L -> "$name! তোমার হাতে আর মাত্র $n দিন বাকি। SSC পরীক্ষা ${d}।"
                days == 0L -> "$name, আজ তোমার পরীক্ষার দিন! শুভকামনা।"
                else -> "পরীক্ষার তারিখ পার হয়ে গেছে।"
            }
        } else {
            when {
                days > 0L -> "$name! Only $n days left. SSC exam is on $d."
                days == 0L -> "$name, today is your exam day! Good luck."
                else -> "Exam date has passed."
            }
        }
    }
}

object Messages {
    fun pick(lang: String, name: String, days: Long, hour: Int, epochDay: Long): String {
        val base = if (lang == "bn") BN else EN
        val pool = base.filter { days > 0L || !it.contains("{days}") }
        val order = pool.indices.shuffled(Random(epochDay))
        val msg = pool[order[hour % pool.size]]
        return msg.replace("{name}", name).replace("{days}", num(if (days < 0L) 0L else days, lang))
    }

    val BN = listOf(
        "{name}, তোমার হাতে আর মাত্র {days} দিন বাকি।",
        "সময় কারও জন্য অপেক্ষা করে না।",
        "প্রতিটি ঘণ্টা লক্ষ্যের দিকে এগিয়ে যাওয়ার সুযোগ।",
        "আজকের পড়া আজই শেষ করার চেষ্টা করো।",
        "SSC 2027 তোমার লক্ষ্য, নিজের লক্ষ্যে স্থির থাকো।",
        "{name}, এখনই পড়তে বসার সেরা সময়।",
        "প্রতিদিন একটু একটু করে এগিয়ে যাও।",
        "তোমার ভবিষ্যৎ গড়ার সময় এখনই।",
        "আজকের পরিশ্রম আগামী দিনের আত্মবিশ্বাস।",
        "{name}, নিজের ওপর বিশ্বাস রাখো।",
        "ছোট ছোট পদক্ষেপই বড় সাফল্যের পথ।",
        "আর {days} দিন, প্রতিদিনের পড়াই তোমাকে এগিয়ে রাখবে।",
        "{name}, এক ঘণ্টা মন দিয়ে পড়লে অনেকটা এগোনো যায়।",
        "কঠিন বিষয়কে ভাগ করে নিলে সহজ হয়ে যায়।",
        "আজ যতটুকু পারো, ততটুকুই দারুণ।",
        "ভুল করলে ক্ষতি নেই, শেখাটাই আসল।",
        "{name}, মাঝে মাঝে পানি খেয়ে নাও, বিরতিও দরকার।",
        "পড়ার ফাঁকে একটু চোখ বিশ্রাম দাও।",
        "নিয়মিত রিভিশনই সফলতার চাবিকাঠি।",
        "{name}, তুমি পারবে, এটা বিশ্বাস করো।",
        "আজকের একটি অধ্যায় কালকের চিন্তা কমাবে।",
        "মনোযোগ ফোন থেকে বইয়ের দিকে ফেরাও।",
        "প্রতিটি অনুশীলন তোমাকে আরও পরিপক্ক করছে।",
        "{name}, তোমার স্বপ্নটা সুন্দর, এর জন্য সময় দাও।",
        "গণিত অনুশীলন করলে ভয় কমে যায়।",
        "আজ ইংরেজির কিছু শব্দ মুখস্থ করে নাও।",
        "বাংলা ব্যাকরণ একটু ঝালাই করে নিতে পারো।",
        "বিজ্ঞানের সূত্রগুলো নিজের হাতে লিখে দেখো।",
        "{name}, ধৈর্য ধরো, ফল আসবেই।",
        "প্রতিদিনের অভ্যাসই তোমাকে সফল করবে।",
        "পরিকল্পনা করে পড়লে সময় বেঁচে যায়।",
        "{name}, আজকের লক্ষ্যটা কী? লিখে ফেলো।",
        "যা পড়লে তা নিজের ভাষায় একবার বলে দেখো।",
        "কঠিন প্রশ্ন আগে নয়, সহজ দিয়ে শুরু করো, তারপর এগোও।",
        "আজকের সময়টা কাজে লাগাও।",
        "{name}, তোমার বাবা-মায়ের দোয়া তোমার সঙ্গে আছে।",
        "ভালো ঘুম ভালো পড়ার সহায়ক।",
        "সকালের পড়া মনে থাকে বেশি।",
        "{name}, একটু হাঁটাহাঁটি করে মাথা ফ্রেশ করে নাও।",
        "লক্ষ্য ঠিক থাকলে পথ খুঁজে পাওয়া যায়।",
        "আজ একটি পুরনো প্রশ্নপত্র সমাধান করে দেখো।",
        "প্রতিটি নতুন শেখা তোমার শক্তি।",
        "{name}, নিজের সঙ্গে নিজের প্রতিযোগিতা করো।",
        "গতকালের চেয়ে আজ একটু ভালো করার চেষ্টা করো।",
        "সফলতা আসে ধারাবাহিকতা থেকে।",
        "{name}, পড়ার টেবিলটা গুছিয়ে নাও, মন বসবে।",
        "বই খোলো, বাকিটা সহজ হয়ে যাবে।",
        "আজকের পড়া শেষ হলে নিজেকে একটু উপহার দাও।",
        "{name}, হাল ছেড়ো না, তুমি অনেকদূর এসেছ।",
        "প্রতিটি দিনই নতুন সুযোগ।",
        "সময়ের কাজ সময়ে করলে চাপ কমে।",
        "{name}, তোমার পরিশ্রম একদিন গর্বের কারণ হবে।",
        "যে বিষয়টা দুর্বল, আজ সেটাকে একটু সময় দাও।",
        "সূত্র আর সংজ্ঞা বারবার পড়লে মনে গেঁথে যায়।",
        "{name}, আরেকটি ঘণ্টা তোমার হাতে, কাজে লাগাও।",
        "লেখা অনুশীলন করলে পরীক্ষায় গুছিয়ে লেখা সহজ হয়।",
        "রচনা আর চিঠির কাঠামো একবার দেখে নাও।",
        "{name}, নিজের প্রতি দয়ালু হও, কিন্তু নিয়মে থাকো।",
        "আজ যা বুঝলে না, কাল শিক্ষককে জিজ্ঞেস করো।",
        "প্রশ্ন করতে লজ্জা পেয়ো না, প্রশ্নই শেখায়।",
        "{name}, বন্ধুদের সঙ্গে মিলে আলোচনা করলে পড়া সহজ হয়।",
        "ইতিহাসের সাল-তারিখ ছোট ছোট নোটে লিখে রাখো।",
        "ভূগোলের মানচিত্র একবার এঁকে দেখো।",
        "{name}, ধর্ম ও নৈতিক শিক্ষার পাঠও অবহেলা কোরো না।",
        "তথ্য ও যোগাযোগ প্রযুক্তির অধ্যায়গুলো একবার ঝালিয়ে নাও।",
        "পদার্থবিজ্ঞানের সমস্যা নিজে নিজে সমাধান করার চেষ্টা করো।",
        "{name}, রসায়নের বিক্রিয়াগুলো ছক করে লিখে ফেলো।",
        "জীববিজ্ঞানের ছবি এঁকে লেবেল করে অনুশীলন করো।",
        "উচ্চতর গণিতে সূত্রের প্রয়োগ বেশি করে অনুশীলন করো।",
        "{name}, আজকের নোটটা লিখে রাখলে কাল কাজে লাগবে।",
        "প্রতিদিনের অগ্রগতি নোটে লিখে রাখো, দেখবে অনেক এগিয়েছ।",
        "তোমার প্রচেষ্টাই তোমার পরিচয় গড়ছে।",
        "{name}, আত্মবিশ্বাস নিয়ে পরীক্ষার হলে যাওয়াই লক্ষ্য।",
        "ধীরে হলেও থেমো না।",
        "প্রতিটি ঘণ্টার সঠিক ব্যবহার বড় পার্থক্য গড়ে।",
        "{name}, মনকে শান্ত রাখো, মন শান্ত থাকলে পড়া ভালো হয়।",
        "গভীর শ্বাস নাও, তারপর আবার শুরু করো।",
        "ক্লান্ত লাগলে ছোট বিরতি নাও, তারপর ফিরে এসো।",
        "{name}, তুমি যা চাও তার জন্য প্রতিদিন একটু করে কাজ করো।",
        "স্বপ্ন দেখো, আর সেই স্বপ্নের জন্য পড়ো।",
        "আজকের কাজ কালকের জন্য ফেলে রেখো না।",
        "{name}, লক্ষ্য পূরণের পথে আজও একটি ধাপ এগিয়ে যাও।",
        "পড়ার সময় পানির বোতল হাতের কাছে রাখো।",
        "পড়ার ঘরে আলো যেন ঠিক থাকে, চোখের যত্ন নাও।",
        "{name}, সোজা হয়ে বসে পড়ো, শরীরও ভালো থাকবে।",
        "একসঙ্গে অনেক কিছু নয়, একটা একটা করে শেষ করো।",
        "ছোট লক্ষ্য পূরণ করলেই বড় লক্ষ্য কাছে আসে।",
        "{name}, আজকের পরিশ্রমের জন্য তুমি নিজেই কৃতজ্ঞ হবে।",
        "প্রতিদিন নিজেকে মনে করিয়ে দাও, আমি পারব।",
        "কঠিন সময় চিরদিন থাকে না, পরিশ্রম থাকে।",
        "{name}, শেষ পর্যন্ত যারা লেগে থাকে তারাই জেতে।",
        "মনে রেখো, তুমি একা নও, অনেকে তোমার জন্য শুভকামনা করছে।",
        "নিজের গতিতে চলো, নিজের সেরাটা দাও।",
        "{name}, এখন পড়ো, পরে গর্ব করো।",
        "আজকের পড়াটাই কালকের আত্মবিশ্বাস।",
        "সময়কে সম্মান করলে সময় তোমাকে সম্মান দেবে।",
        "{name}, শুরু করার জন্য সঠিক সময়ের অপেক্ষা কোরো না, এখনই শুরু করো।",
        "তোমার ভবিষ্যৎ তোমার হাতেই।",
        "সুন্দর ফলাফল আসে সুন্দর অভ্যাস থেকে।",
        "{name}, SSC 2027 এর প্রস্তুতিতে তুমি এগিয়ে যাচ্ছ, চালিয়ে যাও!"
    )

    val EN = listOf(
        "{name}, only {days} days left. Make today count.",
        "Time waits for no one.",
        "Every hour is a chance to move closer to your goal.",
        "Try to finish today's study today.",
        "SSC 2027 is your goal. Stay focused.",
        "{name}, right now is the best time to start studying.",
        "Move forward a little every single day.",
        "The time to build your future is now.",
        "Today's effort is tomorrow's confidence.",
        "{name}, believe in yourself.",
        "Small steps lead to big success.",
        "{days} days to go. Daily study will keep you ahead.",
        "{name}, one focused hour can take you a long way.",
        "Hard topics get easier when you break them into parts.",
        "Whatever you can do today is great.",
        "Mistakes are fine. Learning is what matters.",
        "{name}, drink some water. Breaks matter too.",
        "Give your eyes a short rest between study sessions.",
        "Regular revision is the key to success.",
        "{name}, you can do it. Believe that.",
        "One chapter today means less worry tomorrow.",
        "Turn your attention from the phone to the book.",
        "Every practice makes you stronger.",
        "{name}, your dream is beautiful. Give it your time.",
        "Practising maths makes the fear go away.",
        "Learn a few English words today.",
        "Revise a bit of grammar today.",
        "{name}, patience. Results will come.",
        "Daily habits will make you successful.",
        "Studying with a plan saves time.",
        "{name}, what is today's goal? Write it down.",
        "Try explaining what you read in your own words.",
        "Start with the easy questions, then move on.",
        "{name}, solve one old question paper today.",
        "Every new thing you learn is your strength.",
        "Compete with yesterday's you.",
        "Success comes from consistency.",
        "{name}, tidy your desk and the mind will follow.",
        "Open the book and the rest gets easier.",
        "{name}, with {days} days left, keep going!"
    )
}
