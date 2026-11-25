package com.ssc27.tracker

object S {
    private val m: Map<String, Pair<String, String>> = mapOf(
        "tagline" to ("Your Success Starts Today!" to "Your Success Starts Today!"),
        "welcome" to ("Welcome to SSC 27 Study Tracker" to "Welcome to SSC 27 Study Tracker"),
        "welcomeSub" to ("তোমার SSC প্রস্তুতির প্রতিটি দিনকে মূল্যবান করে তুলতে এই অ্যাপ।" to "This app helps make every day of your SSC preparation count."),
        "yourName" to ("তোমার নাম" to "Your Name"),
        "examDate" to ("SSC পরীক্ষার তারিখ" to "SSC Exam Date"),
        "start" to ("শুরু করো" to "Get Started"),
        "greet" to ("আসসালামু আলাইকুম, %s!" to "Hello, %s!"),
        "countdown" to ("SSC পরীক্ষার কাউন্টডাউন" to "SSC EXAM COUNTDOWN"),
        "daysLeft" to ("দিন বাকি" to "DAYS LEFT"),
        "examDay" to ("আজ পরীক্ষার দিন!" to "Exam Day!"),
        "passed" to ("পরীক্ষার তারিখ পার হয়ে গেছে" to "Exam Date Passed"),
        "newNote" to ("নতুন নোট" to "New Note"),
        "recent" to ("সাম্প্রতিক নোট" to "Recent Notes"),
        "total" to ("মোট নোট" to "Total Notes"),
        "notifStatus" to ("নোটিফিকেশন" to "Notifications"),
        "on" to ("চালু" to "ON"),
        "off" to ("বন্ধ" to "OFF"),
        "home" to ("হোম" to "Home"),
        "notes" to ("নোট" to "Notes"),
        "settings" to ("সেটিংস" to "Settings"),
        "search" to ("নোট খোঁজো" to "Search notes"),
        "title" to ("শিরোনাম" to "Title"),
        "desc" to ("বিস্তারিত লেখো..." to "Write here..."),
        "delete" to ("মুছে ফেলো" to "Delete"),
        "cancel" to ("বাতিল" to "Cancel"),
        "ok" to ("ঠিক আছে" to "OK"),
        "confirmDel" to ("এই নোটটি মুছে ফেলবে?" to "Delete this note?"),
        "noNotes" to ("কোনো নোট নেই" to "No notes yet"),
        "copy" to ("কপি" to "Copy"),
        "copied" to ("কপি হয়েছে" to "Copied"),
        "changeName" to ("নাম" to "Name"),
        "save" to ("সংরক্ষণ" to "Save"),
        "changeDate" to ("পরীক্ষার তারিখ পরিবর্তন" to "Change exam date"),
        "cdNotif" to ("কাউন্টডাউন নোটিফিকেশন" to "Countdown notification"),
        "moNotif" to ("মোটিভেশন নোটিফিকেশন" to "Motivation notification"),
        "nightNotif" to ("রাতের নোটিফিকেশন (রাত ১০টা – ভোর ৬টা)" to "Night notification (10 PM – 6 AM)"),
        "interval" to ("নোটিফিকেশনের ব্যবধান (ঘণ্টা)" to "Notification interval (hours)"),
        "language" to ("ভাষা" to "Language"),
        "dark" to ("ডার্ক মোড" to "Dark mode"),
        "about" to ("অ্যাপ সম্পর্কে" to "About app"),
        "aboutText" to ("SSC 27 Study Tracker: নোটপ্যাড, পরীক্ষার কাউন্টডাউন আর নিয়মিত মোটিভেশন রিমাইন্ডার।" to "SSC 27 Study Tracker: notepad, exam countdown and regular motivation reminders."),
        "privacy" to ("গোপনীয়তা নীতি" to "Privacy policy"),
        "privacyText" to ("তোমার সব নোট ও সেটিংস শুধু এই ফোনেই থাকে। কোনো লগইন নেই, কোনো তথ্য ইন্টারনেটে পাঠানো হয় না।" to "All your notes and settings stay on this phone only. No login, and nothing is sent online."),
        "version" to ("অ্যাপ ভার্সন" to "App version"),
        "deleteAll" to ("সব নোট মুছে ফেলো" to "Delete all notes"),
        "deleteAllQ" to ("সব নোট মুছে ফেলবে? এটি আর ফেরানো যাবে না।" to "Delete all notes? This cannot be undone."),
        "permMissing" to ("নোটিফিকেশনের অনুমতি দেওয়া নেই। ফোনের সেটিংস থেকে অনুমতি দাও।" to "Notification permission is not granted. Please allow it in phone settings."),
        "allowNow" to ("অনুমতি চাও" to "Ask permission"),
        "battery" to ("ব্যাটারি সেভার বা ব্যাকগ্রাউন্ড সীমাবদ্ধতার কারণে কিছু ফোনে নোটিফিকেশন ঠিক ৬০ মিনিটে নাও আসতে পারে। ফোনের সেটিংসে এই অ্যাপের ব্যাটারি অপ্টিমাইজেশন বন্ধ করলে ভালো কাজ করবে।" to "On some phones, battery saver or background limits may delay notifications, so they may not arrive at exactly 60 minutes. Turning off battery optimization for this app helps."),
        "batteryBtn" to ("ব্যাটারি সেটিংস খোলো" to "Open battery settings"),
        "test" to ("এখনই টেস্ট নোটিফিকেশন পাঠাও" to "Send a test notification now")
    )

    fun t(k: String, lang: String): String {
        val p = m[k] ?: return k
        return if (lang == "bn") p.first else p.second
    }
}
