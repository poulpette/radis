package fr.geobert.radis

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Spinner
import com.crashlytics.android.Crashlytics
import fr.geobert.radis.data.Account
import fr.geobert.radis.data.Operation
import fr.geobert.radis.db.AccountTable
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.service.InstallRadisServiceReceiver
import fr.geobert.radis.service.OnRefreshReceiver
import fr.geobert.radis.tools.*
import fr.geobert.radis.ui.ConfigEditor
import fr.geobert.radis.ui.OperationListFragment
import fr.geobert.radis.ui.ScheduledOpListFragment
import fr.geobert.radis.ui.StatisticsListFragment
import fr.geobert.radis.ui.drawer.NavDrawerItem
import fr.geobert.radis.ui.drawer.NavDrawerListAdapter
import fr.geobert.radis.ui.editor.AccountEditor
import fr.geobert.radis.ui.editor.OperationEditor
import fr.geobert.radis.ui.editor.ScheduledOperationEditor
import hirondelle.date4j.DateTime
import io.fabric.sdk.android.Fabric
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.ArrayList
import kotlin.platform.platformStatic
import kotlin.properties.Delegates

public class MainActivity : BaseActivity(), UpdateDisplayInterface {
    private val mDrawerLayout by Delegates.lazy { findViewById(R.id.drawer_layout) as DrawerLayout }
    private val mDrawerList by Delegates.lazy { findViewById(R.id.left_drawer) as ListView }
    private val mOnRefreshReceiver by Delegates.lazy { OnRefreshReceiver(this) }
    private val handler: FragmentHandler by Delegates.lazy { FragmentHandler(this) }
    public val mAccountSpinner: Spinner by Delegates.lazy { findViewById(R.id.account_spinner) as Spinner }

    // ActionBarDrawerToggle ties together the the proper interactions
    // between the navigation drawer and the action bar app icon.
    private val mDrawerToggle: ActionBarDrawerToggle by Delegates.lazy {
        object : ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                mToolbar,
                R.string.navigation_drawer_open, /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */) {

            override fun onDrawerClosed(drawerView: View?) {
                invalidateOptionsMenu() // calls onPrepareOptionsMenu()
            }

            override fun onDrawerOpened(drawerView: View?) {
                invalidateOptionsMenu() // calls onPrepareOptionsMenu()
            }

        }
    }

    private var mActiveFragment: BaseFragment? = null
    private var mFirstStart = true
    private var mPrevFragment: BaseFragment? = null
    private var mActiveFragmentId = -1
    private var mPrevFragmentId: Int = 0


    protected fun findOrCreateFragment(c: Class<out BaseFragment>, fragmentId: Int): Fragment? {
        var fragment: Fragment?
        val fragmentManager = getSupportFragmentManager()
        fragment = fragmentManager.findFragmentByTag(c.getName())
        when (fragment) {
            null -> {
                try {
                    return updateFragmentRefs(c.newInstance(), fragmentId)
                } catch (e: InstantiationException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                return null
            }
            else -> {
                updateFragmentRefs(fragment, fragmentId)
                mDrawerLayout.closeDrawer(mDrawerList)
                fragmentManager.popBackStack(c.getName(), 0)
                return null
            }
        }
    }

    protected fun updateFragmentRefs(fragment: Fragment, id: Int): Fragment {
        val f = fragment as BaseFragment
        mPrevFragment = mActiveFragment
        mActiveFragment = f
        mPrevFragmentId = mActiveFragmentId
        mActiveFragmentId = id
        mDrawerList.setItemChecked(mActiveFragmentId, true)
        return fragment
    }

    private inner class FragmentHandler(private var activity: MainActivity) : PauseHandler() {

        override fun processMessage(act: Activity, message: Message) {
            var fragment: Fragment? = null
            val fragmentManager = activity.getSupportFragmentManager()
            when (message.what) {
                OP_LIST -> fragment = findOrCreateFragment(javaClass<OperationListFragment>(), message.what)
                SCH_OP_LIST -> fragment = findOrCreateFragment(javaClass<ScheduledOpListFragment>(), message.what)
                STATISTICS -> fragment = findOrCreateFragment(javaClass<StatisticsListFragment>(), message.what)
                CREATE_ACCOUNT -> {
                    AccountEditor.callMeForResult(activity, AccountEditor.NO_ACCOUNT)
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                EDIT_ACCOUNT -> {
                    AccountEditor.callMeForResult(activity, getCurrentAccountId())
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                DELETE_ACCOUNT -> {
                    val account = mAccountManager.getCurrentAccount(activity)
                    OperationListFragment.DeleteAccountConfirmationDialog.newInstance(account.id, account.name).
                            show(fragmentManager, "delAccount")
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                PREFERENCES -> {
                    val i = Intent(activity, javaClass<ConfigEditor>())
                    activity.startActivity(i)
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                SAVE_ACCOUNT -> {
                    Tools.AdvancedDialog.newInstance(SAVE_ACCOUNT, activity).show(fragmentManager, "backup")
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                RESTORE_ACCOUNT -> {
                    Tools.AdvancedDialog.newInstance(RESTORE_ACCOUNT, activity).show(fragmentManager, "restore")
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                PROCESS_SCH -> {
                    Tools.AdvancedDialog.newInstance(PROCESS_SCH, activity).show(fragmentManager, "process_scheduling")
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                RECOMPUTE_ACCOUNT -> {
                    AccountTable.consolidateSums(activity, activity.getCurrentAccountId())
                    MainActivity.refreshAccountList(activity)
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                EXPORT_CSV -> {
                    exportCSV()
                    mDrawerList.setItemChecked(mActiveFragmentId, true)
                }
                else -> Log.d(TAG, "Undeclared fragment")
            }

            val tmp = fragment
            if (tmp != null) {
                val f = tmp as BaseFragment
                fragmentManager.beginTransaction().setCustomAnimations(R.anim.enter_from_right, R.anim.zoom_exit,
                        R.anim.enter_from_left, R.anim.zoom_exit).replace(R.id.content_frame, f,
                        f.getName()).addToBackStack(f.getName()).commit()
            }
            mDrawerLayout.closeDrawer(mDrawerList)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onCreate(savedInstanceState)
        initShortDate(this)

        if (!BuildConfig.DEBUG)
            Fabric.with(this, Crashlytics())

        setContentView(R.layout.activity_main)
        Tools.checkDebugMode(this)

        mToolbar.setTitle("")

        registerReceiver(mOnRefreshReceiver, IntentFilter(Tools.INTENT_REFRESH_NEEDED))
        registerReceiver(mOnRefreshReceiver, IntentFilter(INTENT_UPDATE_ACC_LIST))
        initAccountStuff()
        initDrawer()
        installRadisTimer()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super<BaseActivity>.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super<BaseActivity>.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
            finish()
        } else {
            mActiveFragment = mPrevFragment
            mActiveFragmentId = mPrevFragmentId
            mDrawerList.setItemChecked(mActiveFragmentId, true)
            super<BaseActivity>.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return mDrawerToggle.onOptionsItemSelected(item) || super<BaseActivity>.onOptionsItemSelected(item)
    }

    private fun setUpDrawerToggle() {
        // Defer code dependent on restoration of previous instance state.
        // NB: required for the drawer indicator to show up!
        mDrawerLayout.setDrawerListener(mDrawerToggle)
        mDrawerLayout.post(object : Runnable {
            override fun run() {
                mDrawerToggle.syncState()
            }
        })
    }

    public fun displayFragment(fragmentId: Int, id: Long) {
        if (fragmentId != mActiveFragmentId || mActiveFragment == null) {
            val msg = Message()
            msg.what = fragmentId
            msg.obj = id
            handler.sendMessage(msg)
        }
    }

    override fun onPause() {
        super<BaseActivity>.onPause()
        handler.pause()
    }

    override fun onResume() {
        super<BaseActivity>.onResume()
        // nothing to do here, required by Android
    }

    override fun onResumeFragments() {
        Log.d(TAG, "onResumeFragments:$mActiveFragment")
        mActiveFragment?.setupIcon()
        DBPrefsManager.getInstance(this).fillCache(this, {
            Log.d(TAG, "pref cache ok")
            consolidateDbIfNeeded()
            mAccountManager.fetchAllAccounts(false, {
                Log.d(TAG, "all accounts fetched")
                processAccountList(true)
                super<BaseActivity>.onResumeFragments()
            })
            handler.resume(this)
        })
    }

    override fun onDestroy() {
        super<BaseActivity>.onDestroy()
        unregisterReceiver(mOnRefreshReceiver)
    }

    public fun onAccountEditFinished(requestCode: Int, result: Int) {
        Log.d(TAG, "onAccountEditFinished: $result")
        if (result == Activity.RESULT_OK) {
            mAccountManager.fetchAllAccounts(true, {
                mAccountManager.refreshConfig(this, mAccountManager.getCurrentAccountId(this)) // need to be done before setQuickAddVisibility
                val f = mActiveFragment
                if (mActiveFragmentId == OP_LIST && f is OperationListFragment) {
                    f.refreshQuickAdd()
                }
                processAccountList(requestCode == AccountEditor.ACCOUNT_CREATOR)
            })
        } else if (result == Activity.RESULT_CANCELED) {
            if (mAccountManager.mAccountAdapter.getCount() == 0) {
                finish()
            }
        }
    }

    private fun processAccountList(create: Boolean) {
        Log.d(TAG, "processAccountList: count:${mAccountManager.mAccountAdapter.getCount()}, create:$create")
        if (mAccountManager.mAccountAdapter.getCount() == 0) {
            // no account, open create account
            AccountEditor.callMeForResult(this, AccountEditor.NO_ACCOUNT, true)
        } else {
            if (mActiveFragmentId == -1) {
                displayFragment(OP_LIST, (-1).toLong())
            } else {
                displayFragment(mActiveFragmentId, getCurrentAccountId())
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult : " + requestCode);
        super<BaseActivity>.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AccountEditor.ACCOUNT_EDITOR, AccountEditor.ACCOUNT_CREATOR -> onAccountEditFinished(requestCode, resultCode)
            ScheduledOperationEditor.ACTIVITY_SCH_OP_CREATE, ScheduledOperationEditor.ACTIVITY_SCH_OP_EDIT,
            ScheduledOperationEditor.ACTIVITY_SCH_OP_CONVERT -> {
                if (mActiveFragment == null) {
                    findOrCreateFragment(if (mActiveFragmentId == OP_LIST) javaClass<OperationListFragment>() else
                        javaClass<ScheduledOpListFragment>(), mActiveFragmentId)
                }
                mActiveFragment?.onOperationEditorResult(requestCode, resultCode, data)
            }
            OperationEditor.OPERATION_EDITOR, OperationEditor.OPERATION_CREATOR -> {
                if (mActiveFragment == null) {
                    findOrCreateFragment(javaClass<OperationListFragment>(), OP_LIST)
                }
                mActiveFragment?.onOperationEditorResult(requestCode, resultCode, data)
                mAccountManager.backupCurAccountId()
                updateAccountList()
            }
            else -> {
            }
        }
    }

    private fun consolidateDbIfNeeded() {
        val prefs = DBPrefsManager.getInstance(this)
        val needConsolidate = prefs.getBoolean(fr.geobert.radis.service.RadisService.CONSOLIDATE_DB, false)
        if (needConsolidate) {
            fr.geobert.radis.service.RadisService.acquireStaticLock(this)
            this.startService(Intent(this, javaClass<fr.geobert.radis.service.RadisService>()))
        }
    }

    private fun initAccountStuff() {
        mAccountSpinner.setAdapter(mAccountManager.mAccountAdapter)
        this.setActionBarListNavCbk(object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<out Adapter>?) {
            }

            override fun onItemSelected(p0: AdapterView<out Adapter>?, p1: View?, p2: Int, itemId: Long) {
                val frag = mActiveFragment
                if (frag != null && frag.isAdded()) {
                    frag.onAccountChanged(itemId)
                }
            }
        })
    }

    public fun updateAccountList() {
        mAccountManager.fetchAllAccounts(true, {
            onFetchAllAccountCbk()
        })
    }

    private fun onFetchAllAccountCbk() {
        processAccountList(false)
    }

    private fun initDrawer() {
        setUpDrawerToggle()

        val navDrawerItems = ArrayList<NavDrawerItem>()

        navDrawerItems.add(NavDrawerItem(getString(R.string.operations)))
        navDrawerItems.add(NavDrawerItem(getString(R.string.op_list), R.drawable.op_list_48))
        navDrawerItems.add(NavDrawerItem(getString(R.string.scheduled_ops), R.drawable.sched_48))
        navDrawerItems.add(NavDrawerItem(getString(R.string.statistics), R.drawable.stat_48))

        navDrawerItems.add(NavDrawerItem(getString(R.string.accounts)))
        navDrawerItems.add(NavDrawerItem(getString(R.string.create_account), R.drawable.new_account_48))
        navDrawerItems.add(NavDrawerItem(getString(R.string.account_edit), R.drawable.edit_48))
        navDrawerItems.add(NavDrawerItem(getString(R.string.delete_account), R.drawable.trash_48))

        navDrawerItems.add(NavDrawerItem(getString(R.string.advanced)))
        navDrawerItems.add(NavDrawerItem(getString(R.string.preferences), 0)) // TODO icon
        navDrawerItems.add(NavDrawerItem(getString(R.string.backup_db), 0))
        navDrawerItems.add(NavDrawerItem(getString(R.string.restore_db), 0))
        navDrawerItems.add(NavDrawerItem(getString(R.string.process_scheduled_transactions), 0))
        navDrawerItems.add(NavDrawerItem(getString(R.string.recompute_account_sums), 0))
        navDrawerItems.add(NavDrawerItem(getString(R.string.export_csv), 0))

        mDrawerList.setAdapter(NavDrawerListAdapter(getApplicationContext(), navDrawerItems))
        mDrawerList.setOnItemClickListener(object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                displayFragment(i, getCurrentAccountId())
            }
        })
    }

    private fun installRadisTimer() {
        if (mFirstStart) {
            val i = Intent(this, javaClass<InstallRadisServiceReceiver>())
            i.setAction(Tools.INTENT_RADIS_STARTED)
            sendBroadcast(i) // install radis timer
            fr.geobert.radis.service.RadisService.callMe(this) // call service once
            mFirstStart = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super<BaseActivity>.onSaveInstanceState(outState)
        outState.putInt("activeFragId", mActiveFragmentId)
        outState.putInt("prevFragId", mPrevFragmentId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super<BaseActivity>.onRestoreInstanceState(savedInstanceState)
        mActiveFragmentId = savedInstanceState.getInt("activeFragId")
        mPrevFragmentId = savedInstanceState.getInt("prevFragId")
        DBPrefsManager.getInstance(this).fillCache(this, {
            initAccountStuff()
            if (mAccountManager.mAccountAdapter.isEmpty()) {
                updateDisplay(null)
            }
        })
    }

    public fun setActionBarListNavCbk(callback: AdapterView.OnItemSelectedListener?) {
        mAccountSpinner.setOnItemSelectedListener(callback)
    }

    override fun updateDisplay(intent: Intent?) {
        Log.d(TAG, "updateDisplay")
        mAccountManager.fetchAllAccounts(true, {
            processAccountList(true)
            val f = mActiveFragment
            if (f != null && f.isAdded()) {
                f.updateDisplay(intent)
            }
        })
    }

    public fun getCurrentAccountId(): Long {
        return mAccountManager.getCurrentAccountId(this)
    }

    private fun exportCSV() {
        val today = DateTime.now(TIME_ZONE)
        val filename = "${today.format("YYYYMMDD|_|ssmmhh")}_radis.csv"
        var writer: BufferedWriter? = null
        try {
            val sd = Environment.getExternalStorageDirectory()
            if (sd.canWrite()) {
                val backupDBDir = "/radis/"
                val backupDir = File(sd, backupDBDir)
                backupDir.mkdirs()
                val file = File(sd, "$backupDBDir$filename")
                writer = BufferedWriter(FileWriter(file))
                processExportCSV(writer)
                val path = file.getAbsolutePath()
                ExportCSVSucceedDialog.newInstance(path.substring(0, path.lastIndexOf(File.separator) + 1)).
                        show(getSupportFragmentManager(), "csv_export")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            writer?.close()
        }
    }

    private fun processExportCSV(writer: Writer) {
        val accountsCursor = AccountTable.fetchAllAccounts(this)
        val accounts = accountsCursor.map { Account(it) }
        accountsCursor.close()
        writer.write("account;date;third party;amount;tag;mode;notes\n")
        accounts.forEach {
            val opCursor = OperationTable.fetchAllOps(this, it.id)
            val operations = opCursor.map { Operation(it) }
            val accountName = it.name
            operations.forEach {
                writer.write("${accountName};${it.getDate().formatDate()};${it.mThirdParty};${it.getSumStr()};${it.mTag};${it.mMode};${it.mNotes}\n")
            }
        }
        writer.flush()
    }

    public class ExportCSVSucceedDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            super.onCreateDialog(savedInstanceState)
            val builder = AlertDialog.Builder(getActivity())
            val path = getArguments().getString("path")
            builder.setMessage(R.string.export_csv_success).setCancelable(false).
                    setPositiveButton(R.string.open, { d, i ->
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.setDataAndType(Uri.parse(path), "text/csv")
                        startActivity(intent)
                    }).
                    setNegativeButton(R.string.ok, { d, i ->

                    })
            return builder.create()
        }

        companion object {
            public fun newInstance(path: String): ExportCSVSucceedDialog {
                val f = ExportCSVSucceedDialog()
                val args = Bundle()
                args.putString("path", path)
                f.setArguments(args)
                return f
            }
        }
    }

    private val TAG = "MainActivity"

    companion object {
        public val INTENT_UPDATE_OP_LIST: String = "fr.geobert.radis.UPDATE_OP_LIST"
        public val INTENT_UPDATE_ACC_LIST: String = "fr.geobert.radis.UPDATE_ACC_LIST"

        // used for FragmentHandler
        public val OP_LIST: Int = 1
        public val SCH_OP_LIST: Int = 2
        public val STATISTICS: Int = 3

        public val CREATE_ACCOUNT: Int = 5
        public val EDIT_ACCOUNT: Int = 6
        public val DELETE_ACCOUNT: Int = 7

        public val PREFERENCES: Int = 9
        public val SAVE_ACCOUNT: Int = 10
        public val RESTORE_ACCOUNT: Int = 11
        public val PROCESS_SCH: Int = 12
        public val RECOMPUTE_ACCOUNT: Int = 13
        public val EXPORT_CSV: Int = 14

        platformStatic public fun refreshAccountList(ctx: Context) {
            val intent = Intent(INTENT_UPDATE_ACC_LIST)
            ctx.sendBroadcast(intent)
        }
    }
}
