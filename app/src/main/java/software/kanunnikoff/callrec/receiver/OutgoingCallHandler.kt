package software.kanunnikoff.callrec.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.util.Log
import software.kanunnikoff.callrec.core.Core
import software.kanunnikoff.callrec.core.MyMediaRecorder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by dmitry on 01/10/2017.
 */
class OutgoingCallHandler : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null && intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            Log.i(Core.APP_TAG, "*** new outgoing call")
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

            val outputFormat = MediaRecorder.OutputFormat.DEFAULT   // TODO fetch from Preferences
            val outputFile = "call"   // TODO fetch from Preferences
            val audioEncoder = MediaRecorder.AudioEncoder.DEFAULT   // TODO fetch from Preferences

//            MyMediaRecorder.init(outputFormat, outputFile + "_" + timeStamp, audioEncoder)
        }
    }
}