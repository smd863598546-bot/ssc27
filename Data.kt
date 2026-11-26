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
        get() = p.getBoolean("dark", true)
        set(v) { p.edit().putBoolean("dark", v).apply() }
    var accent: Int
        get() = p.getInt("accent", 0xFF4CAF50.toInt())
        set(v) { p.edit().putInt("accent", v).apply() }
    var fontSize: Int
        get() = p.getInt("fontSize", 18)
        set(v) { p.edit().putInt("fontSize", v).apply() }
    var sort: Int
        get() = p.getInt("sort", 1)
        set(v) { p.edit().putInt("sort", v).apply() }
    var titleOnly: Boolean
        get() = p.getBoolean("titleOnly", false)
        set(v) { p.edit().putBoolean("titleOnly", v).apply() }

    fun resetLook() {
        p.edit().remove("accent").remove("fontSize").remove("sort").remove("titleOnly").remove("dark").apply()
    }
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

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val created: Long,
    val updated: Long,
    val color: Int = 0,
    val folder: String = "",
    val trashed: Boolean = false,
    val checklist: Boolean = false
)

class NotesDb(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "notes.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, body TEXT, created INTEGER, updated INTEGER, " +
                "color INTEGER DEFAULT 0, folder TEXT DEFAULT '', trashed INTEGER DEFAULT 0, checklist INTEGER DEFAULT 0)"
        )
        db.execSQL("CREATE TABLE folders(name TEXT PRIMARY KEY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE notes ADD COLUMN folder TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE notes ADD COLUMN trashed INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE notes ADD COLUMN checklist INTEGER DEFAULT 0")
            db.execSQL("CREATE TABLE IF NOT EXISTS folders(name TEXT PRIMARY KEY)")
        }
    }

    fun all(q: String = "", trashed: Boolean = false): List<Note> {
        val out = ArrayList<Note>()
        val like = "%$q%"
        readableDatabase.rawQuery(
            "SELECT id,title,body,created,updated,color,folder,trashed,checklist FROM notes " +
                "WHERE trashed=? AND (title LIKE ? OR body LIKE ?) ORDER BY updated DESC",
            arrayOf(if (trashed) "1" else "0", like, like)
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Note(
                        c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3), c.getLong(4),
                        c.getInt(5), c.getString(6) ?: "", c.getInt(7) == 1, c.getInt(8) == 1
                    )
                )
            }
        }
        return out
    }

    fun save(id: Long, title: String, body: String, color: Int, folder: String, checklist: Boolean): Long {
        val now = System.currentTimeMillis()
        val v = ContentValues()
        v.put("title", title)
        v.put("body", body)
        v.put("updated", now)
        v.put("color", color)
        v.put("folder", folder)
        v.put("checklist", if (checklist) 1 else 0)
        if (id == 0L) {
            v.put("created", now)
            v.put("trashed", 0)
            return writableDatabase.insert("notes", null, v)
        }
        writableDatabase.update("notes", v, "id=?", arrayOf(id.toString()))
        return id
    }

    fun trash(id: Long) {
        val v = ContentValues()
        v.put("trashed", 1)
        writableDatabase.update("notes", v, "id=?", arrayOf(id.toString()))
    }

    fun restore(id: Long) {
        val v = ContentValues()
        v.put("trashed", 0)
        writableDatabase.update("notes", v, "id=?", arrayOf(id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("notes", "id=?", arrayOf(id.toString()))
    }

    fun emptyTrash() {
        writableDatabase.delete("notes", "trashed=1", null)
    }

    fun deleteAll() {
        writableDatabase.delete("notes", null, null)
    }

    fun folders(): List<String> {
        val out = ArrayList<String>()
        readableDatabase.rawQuery("SELECT name FROM folders ORDER BY name", null).use { c ->
            while (c.moveToNext()) out.add(c.getString(0) ?: "")
        }
        return out
    }

    fun addFolder(name: String) {
        val v = ContentValues()
        v.put("name", name)
        writableDatabase.insertWithOnConflict("folders", null, v, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun deleteFolder(name: String) {
        writableDatabase.delete("folders", "name=?", arrayOf(name))
        val v = ContentValues()
        v.put("folder", "")
        writableDatabase.update("notes", v, "folder=?", arrayOf(name))
    }
}
