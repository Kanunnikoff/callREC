package software.kanunnikoff.callrec.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.billingclient.api.BillingClient
import software.kanunnikoff.callrec.ui.MainActivity

/**
 * Created by dmitry on 28/10/2017.
 */
class BillingBroadcastReceiver(val activity: MainActivity): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (activity.mBillingManager != null && activity.mBillingManager!!.billingClientResponseCode == BillingClient.BillingResponse.OK) {
            activity.mBillingManager!!.queryPurchases()
        }
    }
}