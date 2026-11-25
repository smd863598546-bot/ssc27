package com.ssc27.tracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

class Prefs(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("ssc27", Context.MODE_PRIVATE)

    var name: String
        get() = p.getString("name", "সিয়াম") ?: "সিয়াম"
        set(v) { p.edit().putString("name", v).apply() }
    var examDate: LocalDate
        get() = try { LocalDate.parse(p.getString("exam", "2027-03-14")) } catch (e: Exception) { LocalDate.of(2027, 3, 14) }
        set(v) { p.edit().putString("exam", v.toString()).apply() }
    var lang: String
        get() = p.getString("lang", "bn") ?: "bn"
        set(v) { p.edit().putString("lang", v).apply() }
    var dark: Boolean
        get() = p.getBoolean("dark", false)
        set(v) { p.edit().putBoolean("dark", v).apply() }
    var setupDone: Boolean
        get() = p.getBoolean("setup", false)
        set(v) { p.edit().putBoolean("setup", v).apply() }
    var countdownOn: Boolean
        get() = p.getBoolean("cd", true)
        set(v) { p.edit().putBoolean("cd", v).apply() }
    var motivationOn: Boolean
        get() = p.getBoolean("mo", true)
        set(v) { p.edit().putBoolean("mo", v).apply() }
    var nightOn: Boolean
        get() = p.getBoolean("night", true)
        set(v) { p.edit().putBoolean("night", v).apply() }
    var interval: Int
        get() = p.getInt("interval", 1)
        set(v) { p.edit().putInt("interval", v).apply() }
}

data class Note(val id: Long, val title: String, val body: String, val created: Long, val updated: Long)

class NotesDb(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "notes.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, body TEXT, created INTEGER, updated INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun all(q: String = ""): List<Note> {
        val out = ArrayList<Note>()
        val like = "%$q%"
        readableDatabase.rawQuery(
            "SELECT id,title,body,created,updated FROM notes WHERE title LIKE ? OR body LIKE ? ORDER BY updated DESC",
            arrayOf(like, like)
        ).use { c ->
            while (c.moveToNext()) {
                out.add(Note(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3), c.getLong(4)))
            }
        }
        return out
    }

    fun count(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM notes", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun save(id: Long, title: String, body: String): Long {
        val now = System.currentTimeMillis()
        val v = ContentValues()
        v.put("title", title)
        v.put("body", body)
        v.put("updated", now)
        if (id == 0L) {
            v.put("created", now)
            return writableDatabase.insert("notes", null, v)
        }
        writableDatabase.update("notes", v, "id=?", arrayOf(id.toString()))
        return id
    }

    fun delete(id: Long) {
        writableDatabase.delete("notes", "id=?", arrayOf(id.toString()))
    }

    fun deleteAll() {
        writableDatabase.delete("notes", null, null)
    }
}
