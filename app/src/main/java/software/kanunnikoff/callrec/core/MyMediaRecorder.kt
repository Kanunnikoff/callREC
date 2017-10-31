package software.kanunnikoff.callrec.core

import android.annotation.TargetApi
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Surface
import software.kanunnikoff.callrec.core.Core.APP_TAG
import java.io.File
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager



/**
 * Created by dmitry on 15/10/2017.
 */
class MyMediaRecorder {
    private var recorder: MediaRecorder? = null
    private var dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), APP_TAG)
    var outputFileAbsolutePath: String = ""
    var duration: Long = 0L
    var date: Long = 0L

    fun init(
            outputFormat: Int = MediaRecorder.OutputFormat.MPEG_4,
            outputFile: String,
            audioEncoder: Int = MediaRecorder.AudioEncoder.AMR_NB,
            audioEncodingBitRate: Int = 16000,
            audioSamplingRate: Int = 96000,   // 96 kHz
            audioChannels: Int = AudioChannels.MONO,
            maxDuration: Int = -1,
            maxFileSize: Long = -1) {

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.d(APP_TAG, "failed to create '$APP_TAG' directory")
                return
            }
        }

        recorder = MediaRecorder()

        recorder?.setAudioSource(MediaRecorder.AudioSource.MIC)

        recorder?.setOutputFormat(outputFormat)
        outputFileAbsolutePath = dir.absolutePath + File.separator + outputFile
        recorder?.setOutputFile(outputFileAbsolutePath)

        recorder?.setAudioEncoder(audioEncoder)
        recorder?.setAudioEncodingBitRate(audioEncodingBitRate)
        recorder?.setAudioSamplingRate(audioSamplingRate)
        recorder?.setAudioChannels(audioChannels)

        recorder?.setMaxDuration(maxDuration)
        recorder?.setMaxFileSize(maxFileSize)

        try {
            recorder?.prepare()
        } catch (cause: Throwable) {
            Log.e(Core.APP_TAG, "can't prepare media recorder: ${cause.localizedMessage}")
        }
    }

    fun start() {
        date = System.currentTimeMillis()
        Core.increaseAudioVolume()
        recorder?.start()
    }

    fun stop() {
        recorder?.stop()
        recorder?.release()
        Core.restoreAudioVolume()
        duration = System.currentTimeMillis() - date
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun pause() {
        recorder?.pause()
    }

    @TargetApi(Build.VERSION_CODES.N)
    fun resume() {
        recorder?.resume()
    }

    fun getSurface(): Surface {
        return recorder!!.surface
    }

    object AudioChannels {
        const val MONO = 1
        const val STEREO = 2
    }
}