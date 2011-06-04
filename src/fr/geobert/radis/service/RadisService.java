package fr.geobert.radis.service;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.PowerManager;
import android.util.Log;
import fr.geobert.radis.RadisConfiguration;
import fr.geobert.radis.ScheduledOperation;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.tools.PrefsManager;
import fr.geobert.radis.tools.Tools;

public class RadisService extends IntentService {
	public static final String LOCK_NAME_STATIC = "fr.geobert.radis.StaticLock";
	private static PowerManager.WakeLock lockStatic = null;
	private CommonDbAdapter mDbHelper;

	public RadisService() {
		super("RadisService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		try {
			mDbHelper = CommonDbAdapter.getInstance(getApplicationContext());
			assert null != mDbHelper;
			mDbHelper.open();
			processScheduledOps();
			// mDbHelper.close();
		} finally {
			getLock(this).release();
			// mDbHelper.close();
		}
		stopSelf();
	}

	private void keepGreatestDate(HashMap<Long, Long> greatestDatePerAccount,
			final long accountId, final long curOpDate) {
		Long date = greatestDatePerAccount.get(accountId);
		if (null == date) {
			date = Long.valueOf(0);
		}
		if (curOpDate > date) {
			greatestDatePerAccount.put(accountId, curOpDate);
		}
	}

	private void processScheduledOps() {
		Cursor c = mDbHelper.fetchAllScheduledOps();
		if (c.isFirst()) {
			GregorianCalendar today = new GregorianCalendar();
			Tools.clearTimeOfCalendar(today);
			final long todayInMillis = today.getTimeInMillis();

			GregorianCalendar insertionDate = new GregorianCalendar();
			int insertionDayOfMonth = PrefsManager.getInstance(this)
					.getInt(RadisConfiguration.KEY_INSERTION_DATE, 25)
					.intValue();
			final int maxDayOfCurMonth = today
					.getActualMaximum(Calendar.DAY_OF_MONTH);
			insertionDayOfMonth = insertionDayOfMonth > maxDayOfCurMonth ? maxDayOfCurMonth
					: insertionDayOfMonth;
			insertionDate.set(Calendar.DAY_OF_MONTH, insertionDayOfMonth);
			Tools.clearTimeOfCalendar(insertionDate);
			final long insertionDateInMillis = insertionDate.getTimeInMillis();
			final int insertionMonthLimit = insertionDate.get(Calendar.MONTH) + 1;

			HashMap<Long, Long> sumsPerAccount = new LinkedHashMap<Long, Long>();
			HashMap<Long, Long> greatestDatePerAccount = new LinkedHashMap<Long, Long>();

			do {
				final long opRowId = c.getLong(c.getColumnIndex("_id"));
				ScheduledOperation op = new ScheduledOperation(c);
				final Long accountId = Long.valueOf(op.mAccountId);
				long sum = 0;
				boolean needUpdate = false;

				// insert all scheduled of the past until current month
				while (op.getMonth() <= today.get(Calendar.MONTH) && !op.isObsolete()) {
					sum = sum + insertSchOp(op, opRowId);
					needUpdate = true;
				}
				if (todayInMillis >= insertionDateInMillis) {
					while (op.getMonth() <= insertionMonthLimit && !op.isObsolete()) {
						keepGreatestDate(greatestDatePerAccount, accountId,
								op.getDate());
						sum = sum + insertSchOp(op, opRowId);
						needUpdate = true;
					}
				}
				if (needUpdate) {
					mDbHelper.updateScheduledOp(opRowId, op, false);
					Long curSum = sumsPerAccount.get(accountId);
					if (curSum == null) {
						curSum = Long.valueOf(0);
					}
					sumsPerAccount.put(accountId, curSum + sum);
					keepGreatestDate(greatestDatePerAccount, accountId,
							op.getDate());
				}
			} while (c.moveToNext());
			boolean needUpdate = false;
			Long[] accountIds = sumsPerAccount.keySet().toArray(
					new Long[sumsPerAccount.size()]);
			for (HashMap.Entry<Long, Long> e : sumsPerAccount.entrySet()) {
				needUpdate = true;
				updateAccountSum(e.getValue().longValue(), e.getKey()
						.longValue(), greatestDatePerAccount.get(e.getKey()));
			}
			if (needUpdate) {
				Intent i = new Intent(Tools.INTENT_OP_INSERTED);
				i.putExtra("accountIds", accountIds);
				sendOrderedBroadcast(i, null);
			}
		}
		c.close();
	}

	private void updateAccountSum(final long sumToAdd, final long accountId,
			final long date) {
		Cursor accountCursor = mDbHelper.fetchAccount(accountId);
		long curSum = accountCursor.getLong(accountCursor
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_OP_SUM));
		mDbHelper.updateOpSum(accountId, curSum + sumToAdd);
		final long curDate = accountCursor.getLong(accountCursor
				.getColumnIndex(CommonDbAdapter.KEY_ACCOUNT_CUR_SUM_DATE));
		if (date > curDate) {
			mDbHelper.updateCurrentSum(accountId, date);
		} else {
			mDbHelper.updateCurrentSum(accountId, 0);
		}
		accountCursor.close();
	}

	private long insertSchOp(ScheduledOperation op, final long opRowId) {
		final long accountId = op.mAccountId;
		op.mScheduledId = opRowId;
		mDbHelper.createOp(op, accountId);
		ScheduledOperation.addPeriodicityToDate(op);
		Log.d("Radis", String.format("inserted op %s", op.mThirdParty));
		return op.mSum;
	}

	public static void acquireStaticLock(Context context) {
		getLock(context).acquire();
	}

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);

			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					LOCK_NAME_STATIC);
			lockStatic.setReferenceCounted(true);
		}

		return lockStatic;
	}

}
