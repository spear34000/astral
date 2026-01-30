package pics.spear.astral.nativebridge

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DbModule(context: Context) {
    private val helper = object : SQLiteOpenHelper(
        context,
        File(context.filesDir, "nova/nova.db").apply { parentFile?.mkdirs() }.path,
        null,
        1
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS kv (k TEXT PRIMARY KEY, v TEXT NOT NULL)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    fun exec(sql: String, args: JSONArray): Boolean {
        val db = helper.writableDatabase
        val bindArgs = buildArgs(args)
        db.execSQL(sql, bindArgs)
        return true
    }

    fun query(sql: String, args: JSONArray): JSONArray {
        val db = helper.readableDatabase
        val selArgs = buildSelectionArgs(args)
        db.rawQuery(sql, selArgs).use { c ->
            val arr = JSONArray()
            val cols = c.columnNames
            while (c.moveToNext()) {
                val row = JSONObject()
                for (i in cols.indices) {
                    val name = cols[i]
                    when (c.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                        android.database.Cursor.FIELD_TYPE_INTEGER -> row.put(name, c.getLong(i))
                        android.database.Cursor.FIELD_TYPE_FLOAT -> row.put(name, c.getDouble(i))
                        android.database.Cursor.FIELD_TYPE_STRING -> row.put(name, c.getString(i))
                        android.database.Cursor.FIELD_TYPE_BLOB -> row.put(name, android.util.Base64.encodeToString(c.getBlob(i), android.util.Base64.NO_WRAP))
                        else -> row.put(name, c.getString(i))
                    }
                }
                arr.put(row)
            }
            return arr
        }
    }

    fun kvSet(key: String, value: String): Boolean {
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO kv(k,v) VALUES(?,?) ON CONFLICT(k) DO UPDATE SET v=excluded.v", arrayOf(key, value))
        return true
    }

    fun kvGet(key: String): String {
        val db = helper.readableDatabase
        db.rawQuery("SELECT v FROM kv WHERE k=?", arrayOf(key)).use { c ->
            return if (c.moveToFirst()) c.getString(0) else ""
        }
    }

    private fun buildArgs(args: JSONArray): Array<Any?> =
        Array(args.length()) { i ->
            val v = args.opt(i)
            when (v) {
                JSONObject.NULL -> null
                is Int, is Long, is Double, is Float, is String -> v
                is Boolean -> if (v) 1 else 0
                else -> v?.toString()
            }
        }

    private fun buildSelectionArgs(args: JSONArray): Array<String> =
        Array(args.length()) { i ->
            val v = args.opt(i)
            if (v == null || v == JSONObject.NULL) "" else v.toString()
        }
}



