package fr.geobert.radis.editor;

import java.text.ParseException;
import java.util.HashMap;

import org.acra.ErrorReporter;

import fr.geobert.radis.InfoAdapter;
import fr.geobert.radis.Operation;
import fr.geobert.radis.R;
import fr.geobert.radis.R.id;
import fr.geobert.radis.R.string;
import fr.geobert.radis.db.CommonDbAdapter;
import fr.geobert.radis.db.OperationsDbAdapter;
import fr.geobert.radis.tools.CorrectCommaWatcher;
import fr.geobert.radis.tools.Formater;
import fr.geobert.radis.tools.Tools;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;

public abstract class CommonOpEditor extends Activity {
	protected static final int THIRD_PARTIES_DIALOG_ID = 1;
	protected static final int TAGS_DIALOG_ID = 2;
	protected static final int MODES_DIALOG_ID = 3;
	protected static final int EDIT_THIRD_PARTY_DIALOG_ID = 4;
	protected static final int EDIT_TAG_DIALOG_ID = 5;
	protected static final int EDIT_MODE_DIALOG_ID = 6;
	protected static final int DELETE_THIRD_PARTY_DIALOG_ID = 7;
	protected static final int DELETE_TAG_DIALOG_ID = 8;
	protected static final int DELETE_MODE_DIALOG_ID = 9;

	protected Operation mCurrentOp;
	protected OperationsDbAdapter mDbHelper;
	protected AutoCompleteTextView mOpThirdPartyText;
	protected EditText mOpSumText;
	protected AutoCompleteTextView mOpModeText;
	protected AutoCompleteTextView mOpTagText;
	protected DatePicker mDatePicker;
	protected EditText mNotesText;
	protected Long mRowId;

	protected HashMap<String, InfoManager> mInfoManagersMap;
	protected CorrectCommaWatcher mSumTextWatcher;
	protected boolean mOnRestore = false;
	public String mCurrentInfoTable;
	protected double mPreviousSum = 0.0;
	
	// abstract methods
	protected abstract void setView();

	protected abstract void initDbHelper();

	protected abstract void populateFields();

	
	protected abstract void fetchOrCreateCurrentOp();

	// default and common behaviors
	protected void saveOpAndExit() throws ParseException {
		finish();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!Formater.isInit()) {
			Formater.init();
		}

		setView();
		initDbHelper();
		init(savedInstanceState);
	}

	protected void init(Bundle savedInstanceState) {
		Bundle extras = getIntent().getExtras();
		mRowId = (savedInstanceState == null) ? null
				: (Long) savedInstanceState.getSerializable(Tools.EXTRAS_OP_ID);
		if (mRowId == null) {
			mRowId = extras != null ? extras.getLong(Tools.EXTRAS_OP_ID) : null;
			if (mRowId == -1) {
				mRowId = null;
			}
		}

		mOpThirdPartyText = (AutoCompleteTextView) findViewById(R.id.edit_op_third_party);
		mOpThirdPartyText.setAdapter(new InfoAdapter(this, mDbHelper,
				OperationsDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
				OperationsDbAdapter.KEY_THIRD_PARTY_NAME));
		mOpModeText = (AutoCompleteTextView) findViewById(R.id.edit_op_mode);
		mOpModeText.setAdapter(new InfoAdapter(this, mDbHelper,
				OperationsDbAdapter.DATABASE_MODES_TABLE,
				OperationsDbAdapter.KEY_MODE_NAME));
		mOpSumText = (EditText) findViewById(R.id.edit_op_sum);
		mSumTextWatcher = new CorrectCommaWatcher(Formater.SUM_FORMAT
				.getDecimalFormatSymbols().getDecimalSeparator(), mOpSumText);
		mOpTagText = (AutoCompleteTextView) findViewById(R.id.edit_op_tag);
		mOpTagText.setAdapter(new InfoAdapter(this, mDbHelper,
				OperationsDbAdapter.DATABASE_TAGS_TABLE,
				OperationsDbAdapter.KEY_TAG_NAME));
		mDatePicker = (DatePicker) findViewById(R.id.edit_op_date);
		mNotesText = (EditText) findViewById(R.id.edit_op_notes);
		mInfoManagersMap = new HashMap<String, InfoManager>();
	}

	private void invertSign() throws ParseException {
		mSumTextWatcher.setAutoNegate(false);
		Double sum = Formater.SUM_FORMAT.parse(mOpSumText.getText().toString())
				.doubleValue();
		if (sum != null) {
			sum = -sum;
		}
		mOpSumText.setText(Formater.SUM_FORMAT.format(sum));
	}

	protected boolean isFormValid(StringBuilder errMsg) throws ParseException {
		fillOperationWithInputs(mCurrentOp);
		boolean res = true;
		String sumStr = mOpSumText.getText().toString();
		if (sumStr.length() == 0) {
			if (errMsg.length() > 0) {
				errMsg.append("\n");
			}
			errMsg.append(getString(R.string.empty_amount));
			res = false;
		}
		return res;
	}

	private Dialog createInfoListDialog(String table, String colName,
			String title, int editId, int deletiId) {
		InfoManager i = new InfoManager(this, mDbHelper, title, table, colName,
				editId, deletiId);
		mInfoManagersMap.put(table, i);
		return i.getListDialog();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case THIRD_PARTIES_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_THIRD_PARTIES_TABLE,
					OperationsDbAdapter.KEY_THIRD_PARTY_NAME,
					getString(R.string.third_parties),
					EDIT_THIRD_PARTY_DIALOG_ID, DELETE_THIRD_PARTY_DIALOG_ID);
		case TAGS_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_TAGS_TABLE,
					OperationsDbAdapter.KEY_TAG_NAME, getString(R.string.tags),
					EDIT_TAG_DIALOG_ID, DELETE_TAG_DIALOG_ID);
		case MODES_DIALOG_ID:
			return createInfoListDialog(
					OperationsDbAdapter.DATABASE_MODES_TABLE,
					OperationsDbAdapter.KEY_MODE_NAME,
					getString(R.string.modes), EDIT_MODE_DIALOG_ID,
					DELETE_MODE_DIALOG_ID);
		case EDIT_THIRD_PARTY_DIALOG_ID:
		case EDIT_TAG_DIALOG_ID:
		case EDIT_MODE_DIALOG_ID:
			InfoManager i = mInfoManagersMap.get(mCurrentInfoTable);
			Dialog d = i.getEditDialog();
			i.initEditDialog(d);
			return d;
		case DELETE_THIRD_PARTY_DIALOG_ID:
		case DELETE_TAG_DIALOG_ID:
		case DELETE_MODE_DIALOG_ID:
			return Tools.createDeleteConfirmationDialog(this,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mInfoManagersMap.get(mCurrentInfoTable)
									.deleteInfo();
						}
					});
		default:
			return Tools.onDefaultCreateDialog(this, id, mDbHelper);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case EDIT_THIRD_PARTY_DIALOG_ID:
		case EDIT_TAG_DIALOG_ID:
		case EDIT_MODE_DIALOG_ID:
			mInfoManagersMap.get(mCurrentInfoTable).initEditDialog(dialog);
			break;
		case THIRD_PARTIES_DIALOG_ID:
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_THIRD_PARTIES_TABLE)
					.onPrepareDialog((AlertDialog) dialog);
			break;
		case TAGS_DIALOG_ID:
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_TAGS_TABLE)
					.onPrepareDialog((AlertDialog) dialog);
			break;
		case MODES_DIALOG_ID:
			mInfoManagersMap.get(CommonDbAdapter.DATABASE_MODES_TABLE)
					.onPrepareDialog((AlertDialog) dialog);
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!mOnRestore) {
			fetchOrCreateCurrentOp();
			populateFields();
		} else {
			mOnRestore = false;
		}
		initListeners();
	}

	protected void populateCommonFields(Operation op) {
		Tools.setTextWithoutComplete(mOpThirdPartyText, op.mThirdParty);
		Tools.setTextWithoutComplete(mOpModeText, op.mMode);
		Tools.setTextWithoutComplete(mOpTagText, op.mTag);
		mDatePicker.updateDate(op.getYear(), op.getMonth(), op.getDay());
		mPreviousSum = op.mSum;
		mNotesText.setText(op.mNotes);
		Tools.setSumTextGravity(mOpSumText);
		if (mCurrentOp.mSum == 0.0) {
			mOpSumText.setText("");
			mSumTextWatcher.setAutoNegate(true);
		} else {
			mOpSumText.setText(mCurrentOp.getSumStr());
		}
	}

	protected void initListeners() {
		mOpSumText.addTextChangedListener(mSumTextWatcher);
		findViewById(R.id.edit_op_third_parties_list).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showDialog(THIRD_PARTIES_DIALOG_ID);
					}
				});

		findViewById(R.id.edit_op_tags_list).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showDialog(TAGS_DIALOG_ID);
					}
				});

		findViewById(R.id.edit_op_modes_list).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showDialog(MODES_DIALOG_ID);
					}
				});

		findViewById(R.id.edit_op_sign).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						try {
							invertSign();
						} catch (ParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});

		findViewById(R.id.cancel_op).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						setResult(RESULT_CANCELED);
						finish();
					}
				});

		findViewById(R.id.confirm_op).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						try {
							StringBuilder errMsg = new StringBuilder();

							if (isFormValid(errMsg)) {
								saveOpAndExit();
							} else {
								Tools.popError(CommonOpEditor.this,
										errMsg.toString(), null);
							}
						} catch (ParseException e) {
							Tools.popError(CommonOpEditor.this, e.getMessage(),
									null);
						}
					}
				});
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (Tools.onKeyLongPress(keyCode, event, this)) {
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	protected void fillOperationWithInputs(Operation op) throws ParseException {
		op.mThirdParty = mOpThirdPartyText.getText().toString();
		op.mMode = mOpModeText.getText().toString();
		op.mTag = mOpTagText.getText().toString();
		op.setSumStr(mOpSumText.getText().toString());
		op.mNotes = mNotesText.getText().toString();

		DatePicker dp = mDatePicker;
		dp.clearChildFocus(getCurrentFocus());
		op.setDay(dp.getDayOfMonth());
		op.setMonth(dp.getMonth());
		op.setYear(dp.getYear());
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mRowId != null) {
			outState.putLong("rowId", mRowId.longValue());
		}

		try {
			Operation op = mCurrentOp;
			fillOperationWithInputs(op);
			outState.putParcelable("currentOp", op);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		outState.putDouble("previousSum", mPreviousSum);
		mOnRestore = true;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		long rowId = savedInstanceState.getLong("rowId");
		mRowId = rowId != 0 ? Long.valueOf(rowId) : null;
		Operation op = savedInstanceState.getParcelable("currentOp");
		mCurrentOp = op;
		if (null == op) {
			ErrorReporter.getInstance().handleException(
					new NullPointerException("op was not correctly restored"));
		}
		populateFields();
		mOnRestore = true;
		mPreviousSum = savedInstanceState.getDouble("previousSum");
	}
}