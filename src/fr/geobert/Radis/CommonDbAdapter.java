package fr.geobert.Radis;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CommonDbAdapter {
	private static final String TAG = "CommonDbAdapter";
	protected static final String DATABASE_NAME = "radisDb";
	protected static final int DATABASE_VERSION = 1;

	protected static final String DATABASE_ACCOUNT_TABLE = "accounts";
	protected static final String DATABASE_MODES_TABLE = "modes";
	protected static final String DATABASE_THIRD_PARTIES_TABLE = "third_parties";
	protected static final String DATABASE_TAGS_TABLE = "tags";

	public static final String KEY_ACCOUNT_NAME = "account_name";
	public static final String KEY_ACCOUNT_DESC = "account_desc";
	public static final String KEY_ACCOUNT_START_SUM = "account_start_sum";
	public static final String KEY_ACCOUNT_CUR_SUM = "account_current_sum";
	public static final String KEY_ACCOUNT_OP_SUM = "account_operations_sum";
	public static final String KEY_ACCOUNT_CURRENCY = "account_currency";
	public static final String KEY_ACCOUNT_ROWID = "_id";

	private static final String DATABASE_ACCOUNT_CREATE = "create table "
			+ DATABASE_ACCOUNT_TABLE + "(" + KEY_ACCOUNT_ROWID
			+ " integer primary key autoincrement, " + KEY_ACCOUNT_NAME
			+ " text not null, " + KEY_ACCOUNT_DESC + " text not null, "
			+ KEY_ACCOUNT_START_SUM + " real not null, " + KEY_ACCOUNT_OP_SUM
			+ " real not null, " + KEY_ACCOUNT_CUR_SUM + " real not null, "
			+ KEY_ACCOUNT_CURRENCY + " text not null);";

	public static final String KEY_THIRD_PARTY_ROWID = "_id";
	public static final String KEY_THIRD_PARTY_NAME = "third_party_name";

	private static final String DATABASE_THIRD_PARTIES_CREATE = "create table "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ " integer primary key autoincrement, " + KEY_THIRD_PARTY_NAME
			+ " text not null);";

	public static final String KEY_TAG_ROWID = "_id";
	public static final String KEY_TAG_NAME = "tag_name";

	private static final String DATABASE_TAGS_CREATE = "create table "
			+ DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID
			+ " integer primary key autoincrement, " + KEY_TAG_NAME
			+ " text not null);";

	public static final String KEY_MODE_ROWID = "_id";
	public static final String KEY_MODE_NAME = "mode_name";

	private static final String DATABASE_MODES_CREATE = "create table "
			+ DATABASE_MODES_TABLE + "(" + KEY_MODE_ROWID
			+ " integer primary key autoincrement, " + KEY_MODE_NAME
			+ " text not null);";

	public static final String KEY_OP_DATE = "op_date";
	public static final String KEY_OP_THIRD_PARTY = "op_third_party";
	public static final String KEY_OP_TAG = "op_tag";
	public static final String KEY_OP_MODE = "op_mode";
	public static final String KEY_OP_SUM = "op_sum";
	public static final String KEY_OP_SCHEDULED_ID = "op_scheduled_id";
	public static final String KEY_OP_ROWID = "_id";
	
	public static final String OPS_ACCOUNT_TABLE = "ops_of_account_%d";
	protected static final String DATABASE_OP_CREATE = "create table " + OPS_ACCOUNT_TABLE + "("
			+ KEY_OP_ROWID + " integer primary key autoincrement, "
			+ KEY_OP_THIRD_PARTY + " integer not null, " + KEY_OP_TAG
			+ " integer not null, " + KEY_OP_SUM + " real not null, "
			+ KEY_OP_MODE + " integer not null, " + KEY_OP_DATE
			+ " integer not null, " + KEY_OP_SCHEDULED_ID
			+ " integer, FOREIGN KEY (" + KEY_OP_THIRD_PARTY + ") REFERENCES "
			+ DATABASE_THIRD_PARTIES_TABLE + "(" + KEY_THIRD_PARTY_ROWID
			+ "), FOREIGN KEY (" + KEY_OP_TAG + ") REFERENCES "
			+ DATABASE_TAGS_TABLE + "(" + KEY_TAG_ROWID + "), FOREIGN KEY ("
			+ KEY_OP_MODE + ") REFERENCES " + DATABASE_MODES_TABLE + "("
			+ KEY_MODE_ROWID + "));";

	protected static final String DATABASE_OP_DROP = "drop table if exists " + OPS_ACCOUNT_TABLE + ";";
	
	protected DatabaseHelper mDbHelper;
	protected SQLiteDatabase mDb;
	protected final Context mCtx;

	public CommonDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	protected static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_ACCOUNT_CREATE);
			//db.execSQL(DATABASE_OP_CREATE);
			db.execSQL(DATABASE_THIRD_PARTIES_CREATE);
			db.execSQL(DATABASE_MODES_CREATE);
			db.execSQL(DATABASE_TAGS_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			// String old_name = DATABASE_TABLE + "_"
			// + Integer.toString(oldVersion);
			// db.execSQL("ALTER TABLE " + DATABASE_TABLE + " RENAME TO "
			// + old_name + ";");
			// db.execSQL("CREATE TABLE " + DATABASE_TABLE + "AS SELECT * FROM "
			// + old_name + ";");
			// db.execSQL("DROP TABLE " + old_name + ";");
		}
	}

	/**
	 * Open the notes database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException
	 *             if the database could be neither opened or created
	 */
	public CommonDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}
}
