package fr.geobert.radis.ui.editor

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v4.view.ViewPager
import android.support.v7.app.ActionBarActivity
import android.util.Log
import android.view.MenuItem
import fr.geobert.radis.BaseActivity
import fr.geobert.radis.R
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.AccountConfig
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.PreferenceTable
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.ui.ConfigFragment
import kotlin.properties.Delegates

public class AccountEditor : BaseActivity(), EditorToolbarTrait {
    public var mRowId: Long = 0
        private set

    private val mViewPager by Delegates.lazy { findViewById(R.id.pager) as ViewPager }

    private val mPagerAdapter = object : FragmentPagerAdapter(getSupportFragmentManager()) {
        private val fragmentsList: Array<Fragment?> = arrayOfNulls(getCount())
        override fun getItem(position: Int): Fragment? {
            val f = fragmentsList.get(position)
            return if (null == f) {
                fragmentsList.set(position, when (position) {
                    0 -> AccountEditFragment()
                    else -> ConfigFragment() : Fragment
                })
                fragmentsList.get(position)
            } else {
                f
            }
        }

        override fun getCount(): Int = 2

        override fun getPageTitle(position: Int): CharSequence =
                when (position) {
                    0 -> getString(R.string.account)
                    else -> getString(R.string.preferences)
                }
    }

    fun getAccountFrag() = mPagerAdapter.getItem(0) as AccountEditFragment
    fun getConfigFrag() = mPagerAdapter.getItem(1) as ConfigFragment
    fun isNewAccount() = NO_ACCOUNT == mRowId

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.multipane_editor)
        initToolbar(this)

        mRowId = if (savedInstanceState != null) {
            savedInstanceState.getSerializable(PARAM_ACCOUNT_ID) as Long
        } else {
            val extra = getIntent().getExtras()
            if (extra != null) {
                extra.getLong(PARAM_ACCOUNT_ID)
            } else {
                NO_ACCOUNT
            }
        }

        if (isNewAccount()) {
            setTitle(R.string.account_creation)
        } else {
            setTitle(R.string.account_edit_title)
        }

        mViewPager.setAdapter(mPagerAdapter)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super<BaseActivity>.onRestoreInstanceState(savedInstanceState)
        getAccountFrag().onRestoreInstanceState(savedInstanceState) // not managed by Android
        getConfigFrag().onRestoreInstanceState(savedInstanceState)  // not managed by Android
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.confirm -> {
                onOkClicked()
                return true
            }
            else -> return super<BaseActivity>.onOptionsItemSelected(item)
        }
    }

    private fun onOkClicked() {
        val errMsg = StringBuilder()
        if (getAccountFrag().isFormValid(errMsg)) {
            setResult(Activity.RESULT_OK)
            val account = getAccountFrag().fillAccount()
            val config = getConfigFrag().fillConfig()
            if (isNewAccount()) {
                val id = AccountTable.createAccount(this, account)
                PreferenceTable.createAccountPrefs(this, config, id)
            } else {
                AccountTable.updateAccount(this, account)
                PreferenceTable.updateAccountPrefs(this, config, mRowId)
            }

            finish()
            this.overridePendingTransition(R.anim.enter_from_right, 0)
        } else {
            Tools.popError(this, errMsg.toString(), null)
        }
    }

    companion object {
        public fun callMeForResult(context: ActionBarActivity, accountId: Long) {
            val intent = Intent(context, javaClass<AccountEditor>())
            intent.putExtra(PARAM_ACCOUNT_ID, accountId)
            context.startActivityForResult(intent, ACCOUNT_EDITOR)
        }

        public val NO_ACCOUNT: Long = 0
        public val ACCOUNT_EDITOR: Int = 1000
        public val PARAM_ACCOUNT_ID: String = "account_id"
        public val GET_ACCOUNT: Int = 400
        public val GET_ACCOUNT_CONFIG: Int = 410
    }
}