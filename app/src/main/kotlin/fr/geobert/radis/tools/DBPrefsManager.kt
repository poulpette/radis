package fr.geobert.radis.tools

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.FragmentActivity
import android.support.v4.app.LoaderManager
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.util.Log
import fr.geobert.radis.R
import fr.geobert.radis.db.DbContentProvider
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.ui.ConfigFragment
import java.lang
import java.util.HashMap
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

public class DBPrefsManager : LoaderManager.LoaderCallbacks<Cursor> {
    private var mCurrentCtx: Context? = null
    private var mCache: HashMap<String, String>? = null
    private val FILL_CACHE = 100

    private var mCbk: () -> Unit by Delegates.notNull()

    // async
    public fun fillCache(ctx: FragmentActivity, cbk: () -> Unit) {
        if (mCache == null) {
            mCbk = cbk
            ctx.getSupportLoaderManager().initLoader<Cursor>(FILL_CACHE, Bundle(), this)
        } else {
            cbk()
        }
    }

    // sync
    public fun fillCache(ctx: Context) {
        if (mCache == null) {
            val data = ctx.getContentResolver().query(DbContentProvider.PREFS_URI, PreferenceTable.PREFS_COLS, null, null, null)
            mCache = HashMap<String, String>()
            if (data != null) {
                if (data.moveToFirst()) {
                    do {
                        mCache!!.put(data.getString(0), data.getString(1))
                    } while (data.moveToNext())
                }
                data.close()
            }
        }
    }

    public fun getString(key: String): String {
        return mCache!!.get(key)
    }

    public fun getString(key: String, defValue: String): String {
        return mCache!!.get(key) ?: defValue
    }

    public fun getBoolean(key: String): Boolean? {
        val v = mCache!!.get(key)
        try {
            val b = lang.Boolean.valueOf(v)
            return b
        } catch (e: Exception) {
            return null
        }

    }

    public fun getBoolean(key: String, defValue: Boolean): Boolean {
        val v = mCache!!.get(key)
        try {
            return (if (v != null) lang.Boolean.valueOf(v) else null) ?: defValue
        } catch (e: Exception) {
            return defValue
        }

    }

    public fun getInt(key: String): Int? {
        val v = mCache!!.get(key)
        try {
            val b = Integer.valueOf(v)
            return b
        } catch (e: Exception) {
            return null
        }

    }

    public fun getInt(key: String, defValue: Int): Int {
        val v = mCache!!.get(key)
        try {
            return (if (v != null) Integer.valueOf(v) else null) ?: defValue
        } catch (e: Exception) {
            return defValue
        }

    }

    public fun getLong(key: String): Long? {
        val v = mCache!!.get(key)
        try {
            val b = lang.Long.valueOf(v)
            return b
        } catch (e: Exception) {
            return null
        }

    }

    public fun getLong(key: String, defValue: Long): Long {
        val v = mCache!!.get(key)
        try {
            return (if (v != null) lang.Long.valueOf(v) else null) ?: defValue
        } catch (e: Exception) {
            return defValue
        }

    }

    public fun put(key: String, value: Any?) {
        Log.d("PrefBug", "DBPrefsManager put $key = $value")
        if (!key.endsWith("_for_account") && !key.startsWith("override_")) {
            val cr = mCurrentCtx!!.getContentResolver()
            val values = ContentValues()
            values.put(PreferenceTable.KEY_PREFS_NAME, key)
            values.put(PreferenceTable.KEY_PREFS_VALUE, value.toString())
            values.put(PreferenceTable.KEY_PREFS_ACCOUNT, 0)
            values.put(PreferenceTable.KEY_PREFS_IS_ACTIVE, 1)

            if (mCache!!.get(key) == null) {
                cr.insert(DbContentProvider.PREFS_URI, values)
            } else {
                cr.update(DbContentProvider.PREFS_URI, values, PreferenceTable.KEY_PREFS_NAME + "=?", array(key))
            }
        }
        mCache!!.put(key, value.toString())
    }

    private fun deletePref(key: String) {
        mCurrentCtx!!.getContentResolver().delete(DbContentProvider.PREFS_URI, PreferenceTable.KEY_PREFS_NAME + "=?", array(key))
    }

    private fun deleteAllPrefs() {
        val i = mCurrentCtx!!.getContentResolver().delete(DbContentProvider.PREFS_URI, null, null)
        Log.d("PrefBug", "delete All Prefs : $i")
    }

    public fun resetAll() {
        //clearAccountRelated()
        deleteAllPrefs()
        mCache?.clear()
        val editor = PreferenceManager.getDefaultSharedPreferences (mCurrentCtx!!).edit() //mCurrentCtx!!.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE).edit()
        editor.clear()
        editor.commit()
    }

    override fun onCreateLoader(id: Int, args: Bundle): Loader<Cursor>? = when (id) {
        FILL_CACHE -> CursorLoader(mCurrentCtx, DbContentProvider.PREFS_URI, PreferenceTable.PREFS_COLS, null, null, null)
        else -> null
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        when (loader.getId()) {
            FILL_CACHE -> {
                mCache = HashMap<String, String>()
                if (data.moveToFirst()) {
                    do {
                        mCache!!.put(data.getString(0), data.getString(1))
                    } while (data.moveToNext())
                }
                mCbk()
            }
            else -> {
            }
        }

    }

    override fun onLoaderReset(arg0: Loader<Cursor>) {
        // nothing to do
    }

    companion object {
        public val SHARED_PREF_NAME: String = "radis_prefs"
        private var mInstance: DBPrefsManager? = null

        platformStatic public fun getInstance(ctx: Context): DBPrefsManager {
            if (null == mInstance) {
                mInstance = DBPrefsManager()
                PreferenceManager.setDefaultValues(ctx, R.xml.preferences, false)
            }
            mInstance!!.mCurrentCtx = ctx
            return mInstance as DBPrefsManager
        }
    }
}