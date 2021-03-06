package fr.geobert.radis

import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.util.Log
import fr.geobert.radis.data.AccountManager
import fr.geobert.radis.tools.UpdateDisplayInterface
import kotlin.properties.Delegates

public abstract class BaseFragment : Fragment(), UpdateDisplayInterface, Toolbar.OnMenuItemClickListener {
    protected val mActivity: MainActivity by lazy(LazyThreadSafetyMode.NONE) { activity as MainActivity }
    protected val mAccountManager: AccountManager by lazy(LazyThreadSafetyMode.NONE) { mActivity.mAccountManager }

    open public fun onOperationEditorResult(requestCode: Int, resultCode: Int, data: Intent?) {
    }

    open public fun onAccountChanged(itemId: Long): Boolean = false

    public fun getName(): String {
        return this.javaClass.name
    }

    protected fun setIcon(id: Int) {
        mActivity.mToolbar.post { mActivity.mToolbar.setNavigationIcon(id) }
    }

    protected fun setMenu(id: Int) {
        mActivity.mToolbar.menu.clear()
        mActivity.mToolbar.inflateMenu(id)
        mActivity.mToolbar.setOnMenuItemClickListener(this)
    }

    public abstract fun setupIcon();
}
