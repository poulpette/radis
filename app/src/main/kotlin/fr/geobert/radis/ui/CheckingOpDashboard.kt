package fr.geobert.radis.ui

import android.widget.LinearLayout
import fr.geobert.radis.R
import android.widget.EditText
import android.widget.TextView
import fr.geobert.radis.tools.TargetSumWatcher
import fr.geobert.radis.tools.getSumSeparator
import fr.geobert.radis.MainActivity
import fr.geobert.radis.db.OperationTable
import fr.geobert.radis.data.Operation
import fr.geobert.radis.tools.Tools
import fr.geobert.radis.tools.formatSum

public class CheckingOpDashboard(val activity: MainActivity, layout: LinearLayout) {
    val targetedSumEdt = layout.findViewById(R.id.targeted_sum) as EditText
    val sumWatcher = TargetSumWatcher(getSumSeparator(), targetedSumEdt, this)
    val statusLbl = layout.findViewById(R.id.checking_op_status) as TextView

    {
        sumWatcher.setAutoNegate(false)
        targetedSumEdt.setOnFocusChangeListener {(view, b) ->
            if (b) {
                sumWatcher.setAutoNegate(false)
                (view as EditText).selectAll()
            }
        }

        updateDisplay()
    }

    fun updateDisplay() {
        val accountManager = activity.getAccountManager()
        val target = Tools.extractSumFromStr(targetedSumEdt.getText().toString())
        val checkedSum = accountManager.getCurrentAccountCheckedSum()
        val total = accountManager.getCurrentAccountStartSum() - checkedSum
        val diff = target - total
        statusLbl.setText("%s\n%s".format((total / 100.0).formatSum(), (diff / 100.0).formatSum()))
    }

    fun onCheckedChanged(op: Operation, b: Boolean) {
        OperationTable.updateOpCheckedStatus(activity, op, b)
        op.mIsChecked = b
        updateDisplay()
    }

    fun onResume() {
        targetedSumEdt.addTextChangedListener(sumWatcher)
    }

    fun onPause() {
        targetedSumEdt.removeTextChangedListener(sumWatcher);
    }
}