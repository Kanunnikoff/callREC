package software.kanunnikoff.callrec.service

import android.app.IntentService
import android.content.Intent
import software.kanunnikoff.callrec.R
import software.kanunnikoff.callrec.core.Core

/**
 * Created by dmitry on 23/10/2017.
 */

class MyIntentService : IntentService("MyIntentService") {

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    override fun onHandleIntent(intent: Intent?) {
        startForeground(1, Core.getNotification(resources.getString(R.string.app_name), resources.getString(R.string.notification_body)))

        while (true) {
            Thread.sleep(1000)
        }
    }
}
