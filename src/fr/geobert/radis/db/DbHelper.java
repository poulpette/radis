package fr.geobert.radis.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

public class DbHelper extends SQLiteOpenHelper {
	private static final String TAG = "DbHelper";
	protected static final String DATABASE_NAME = "radisDb";
	protected static final int DATABASE_VERSION = 12;

	private Context mCtx;
	
	public DbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mCtx = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		AccountTable.onCreate(db);
		OperationTable.onCreate(db);
		InfoTables.onCreate(db);
		OperationTable.createMeta(db);
		PreferenceTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		switch (oldVersion) {
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
			ScheduledOperationTable.onUpgrade(db, oldVersion, newVersion);
			OperationTable.onUpgrade(db, oldVersion, newVersion);
		case 6:
			AccountTable.onUpgrade(db, oldVersion, newVersion);
			ScheduledOperationTable.onUpgrade(db, oldVersion, newVersion);
			OperationTable.onUpgrade(db, oldVersion, newVersion);
		case 7:
		case 8:
			InfoTables.onUpgrade(db, oldVersion, newVersion);
		case 9:
			AccountTable.onUpgrade(db, oldVersion, newVersion);
		case 10:
			PreferenceTable.onUpgrade(mCtx, db, oldVersion, newVersion);
		case 11:
			OperationTable.onUpgrade(db, oldVersion, newVersion);
			ScheduledOperationTable.onUpgrade(db, oldVersion, newVersion);
		default:
			AccountTable.onUpgrade(db, oldVersion, newVersion);
		} 
		
	}
	
	public void trashDatabase() {
		close();
		mCtx.deleteDatabase(DATABASE_NAME);
	}

	public boolean backupDatabase() {
		close();
		try {
			File sd = Environment.getExternalStorageDirectory();
			File data = Environment.getDataDirectory();
			if (sd.canWrite()) {
				String currentDBPath = "data/fr.geobert.radis/databases/radisDb";
				String backupDBDir = "/radis/";
				String backupDBPath = "/radis/radisDb";
				File currentDB = new File(data, currentDBPath);
				File backupDir = new File(sd, backupDBDir);
				backupDir.mkdirs();
				File backupDB = new File(sd, backupDBPath);

				if (currentDB.exists()) {
					FileInputStream srcFIS = new FileInputStream(currentDB);
					FileOutputStream dstFOS = new FileOutputStream(backupDB);
					FileChannel src = srcFIS.getChannel();
					FileChannel dst = dstFOS.getChannel();
					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();
					srcFIS.close();
					dstFOS.close();
				}
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean restoreDatabase() {
		close();
		try {
			File sd = Environment.getExternalStorageDirectory();

			String backupDBPath = "/radis/radisDb";
			File currentDB = mCtx.getDatabasePath(DATABASE_NAME);
			File backupDB = new File(sd, backupDBPath);

			if (backupDB.exists()) {
				FileInputStream dstFIS = new FileInputStream(currentDB);
				FileOutputStream srcFOS = new FileOutputStream(backupDB);
				FileChannel dst = dstFIS.getChannel();
				FileChannel src = srcFOS.getChannel();

				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
				srcFOS.close();
				dstFIS.close();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
