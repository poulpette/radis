package fr.geobert.radis.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.support.v4.content.CursorLoader
import fr.geobert.radis.data.Statistic
import fr.geobert.radis.tools.fillContentValuesWith

public object StatisticTable {
    private val TAG = "StatisticTable"
    val KEY_STAT_ID = "_id"
    val KEY_STAT_NAME = "stat_name"
    val KEY_STAT_ACCOUNT = "account_id"
    val KEY_STAT_ACCOUNT_NAME = "account_name"
    val KEY_STAT_FILTER = "filter"
    val KEY_STAT_PERIOD_TYPE = "period_type"
    val KEY_STAT_TIMESCALE_TYPE = "timescale_type"
    val KEY_STAT_TYPE = "chart_type"
    val KEY_STAT_X_LAST = "x_last"
    val KEY_STAT_START_DATE = "start_date"
    val KEY_STAT_END_DATE = "end_date"

    val STAT_COLS = arrayOf(KEY_STAT_ID, KEY_STAT_NAME, KEY_STAT_ACCOUNT, KEY_STAT_FILTER, KEY_STAT_PERIOD_TYPE, KEY_STAT_TYPE,
            KEY_STAT_X_LAST, KEY_STAT_START_DATE, KEY_STAT_END_DATE, KEY_STAT_ACCOUNT_NAME, KEY_STAT_TIMESCALE_TYPE)

    public val STAT_TABLE: String = "statistics"

    private val CREATE_TABLE = "create table $STAT_TABLE ($KEY_STAT_ID integer primary key autoincrement, $KEY_STAT_NAME  string not null, $KEY_STAT_ACCOUNT  integer not null, $KEY_STAT_FILTER  integer not null, $KEY_STAT_PERIOD_TYPE  integer not null, $KEY_STAT_TYPE  integer not null, $KEY_STAT_X_LAST  integer, $KEY_STAT_START_DATE  integer,$KEY_STAT_END_DATE  integer, $KEY_STAT_ACCOUNT_NAME  string not null, $KEY_STAT_TIMESCALE_TYPE integer not null)"

    private val ADD_TIMESCALE_COL = "ALTER TABLE $STAT_TABLE ADD COLUMN $KEY_STAT_TIMESCALE_TYPE integer not null DEFAULT -1"

    private val CREATE_TRIGGER_ON_STAT_DELETE = "CREATE TRIGGER IF NOT EXISTS on_account_deleted_for_stat AFTER DELETE ON ${AccountTable.DATABASE_ACCOUNT_TABLE} " +
            "BEGIN DELETE FROM $STAT_TABLE WHERE $KEY_STAT_ACCOUNT = old._id ; END"

    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
        db.execSQL(CREATE_TRIGGER_ON_STAT_DELETE)
    }

    fun upgradeFromV17(db: SQLiteDatabase) {
        onCreate(db)
    }

    fun upgradeFromV19(db: SQLiteDatabase) {
        db.execSQL(CREATE_TRIGGER_ON_STAT_DELETE)
    }

    fun upgradeFromV20(db: SQLiteDatabase, oldVersion: Int) {
        if (oldVersion > 17) {
            db.execSQL(ADD_TIMESCALE_COL)
        }
    }

    private fun fillContentValues(stat: Statistic): ContentValues {
        val args = ContentValues()
        STAT_COLS.map {
            if (it != KEY_STAT_ID)
                fillContentValuesWith(args, it, stat.getValue(it))
        }
        return args
    }

    fun createStatistic(stat: Statistic, ctx: Context): Long {
        val res = ctx.contentResolver?.insert(DbContentProvider.STATS_URI, fillContentValues(stat))
        return res?.lastPathSegment?.toLong() as Long
    }

    fun updateStatistic(stat: Statistic, ctx: Context): Int {
        return ctx.contentResolver?.update(Uri.parse("${DbContentProvider.STATS_URI}/${stat.id}"),
                fillContentValues(stat), null, null) as Int
    }

    fun deleteStatistic(statId: Long, ctx: Context): Boolean {
        val nb = ctx.contentResolver?.delete(Uri.parse("${DbContentProvider.STATS_URI}/$statId"), null, null)
        return if (nb != null) nb > 0 else false
    }

    fun getStatisticLoader(statId: Long, ctx: Context): CursorLoader {
        return CursorLoader(ctx, Uri.parse("${DbContentProvider.STATS_URI}/$statId"), STAT_COLS, null, null, null)
    }
}
