package software.kanunnikoff.callrec.core

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.view.Gravity
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.preference.PreferenceManager
import android.app.NotificationManager
import android.app.NotificationChannel
import android.media.AudioManager
import android.os.Build
import software.kanunnikoff.callrec.R
import software.kanunnikoff.callrec.model.Record
import software.kanunnikoff.callrec.ui.MainActivity
import software.kanunnikoff.callrec.ui.NotificationActivity


/**
 * Created by dmitry on 15/10/2017.
 */
@SuppressLint("StaticFieldLeak")
object Core {
    const val APP_TAG = "callREC"
    const val STOP_RECORDING_ACTION = "stop_recording"
    private const val RECORD_NUMBER = "record_number"
    private const val COLUMN_FILE_NAME_PREFIX = "pref_key_file_name_prefix"
    private const val COLUMN_RECORD_TITLE_PREFIX = "pref_key_record_title_prefix"
    private const val COLUMN_OUTPUT_FORMAT = "pref_key_output_format"
    private const val COLUMN_AUDIO_ENCODER = "pref_key_audio_encoder"
    private const val COLUMN_AUDIO_ENCODING_BIT_RATE = "pref_key_audio_encoding_bit_rate"
    private const val COLUMN_AUDIO_SAMPLING_RATE = "pref_key_audio_sampling_rate"
    private const val COLUMN_AUDIO_CHANNELS = "pref_key_audio_channels"
    private const val IS_FIRST_LAUNCH = "is_first_launch"
    private const val NOTIFICATION_CHANNEL_ID = "screenREC_notification_channel"
    private const val IS_PREMIUM_PURCHASED = "is_premium_purchased"

    const val PREMIUM_SKU_ID = "premium"
    const val START_STOP_RECORDING_DELAY = 5000L

    var sqliteStorage: RecordsSqliteStorage? = null
    var context: Context? = null

    var audioManagerStreamVolume = -1

    fun init(context: Context) {
        Core.context = context
        sqliteStorage = RecordsSqliteStorage(context)
    }

    fun showToast(message: String) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    fun getNotification(header: String, body: String): Notification {
        val intent = Intent(context, NotificationActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder: Notification.Builder

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, context!!.resources.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = "screenREC notification channel"
            notificationManager.createNotificationChannel(notificationChannel)

            notificationBuilder = Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(header)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setUsesChronometer(true)
        } else {
            notificationBuilder = Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(header)
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setUsesChronometer(true)
        }

        return notificationBuilder.build()
    }

    fun receiveAllRecords(start: Long, callback: Callback<Record>) {
        callback.onResult(sqliteStorage!!.getAllRecords(start))
    }

    fun receiveFavoredRecords(start: Long, callback: Callback<Record>) {
        callback.onResult(sqliteStorage!!.getFavoredRecords(start))
    }

    fun favoriteRecord(record: Record) {
        record.isFavored = 1
        sqliteStorage?.favoriteRecord(record)
    }

    fun unFavoriteRecord(record: Record) {
        record.isFavored = 0
        sqliteStorage?.favoriteRecord(record)
    }

    fun insertRecord(record: Record): Long {
        return sqliteStorage!!.insertRecord(record)
    }

    fun deleteRecord(record: Record) {
        sqliteStorage?.deleteRecord(record)
        File(record.outputFile).delete()
    }

    fun renameRecord(record: Record, title: String) {
        sqliteStorage!!.renameRecord(record, title)
    }

    fun loadBitmap(url: String, imageView: ImageView) {
        Glide.with(context).load(url).into(imageView)
    }

    fun loadBitmap(bitmap: Bitmap, imageView: ImageView) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        Glide.with(context)
                .load(stream.toByteArray())
                .asBitmap()
                .into(imageView)
    }

    fun loadBitmap(thumbnail: ByteArray, imageView: ImageView) {
        Glide.with(context)
                .load(thumbnail)
                .asBitmap()
                .into(imageView)
    }

    var isPremiumPurchased: Boolean
        get() = context!!.getSharedPreferences(APP_TAG, Context.MODE_PRIVATE).getBoolean(IS_PREMIUM_PURCHASED, false)
        set(value) {
            val editor = context!!.getSharedPreferences(APP_TAG, Context.MODE_PRIVATE).edit()
            editor.putBoolean(IS_PREMIUM_PURCHASED, value)
            editor.apply()
        }

    var recordNumber: Int
        get() = context!!.getSharedPreferences(APP_TAG, Context.MODE_PRIVATE).getInt(RECORD_NUMBER, 1)
        set(value) {
            val editor = context!!.getSharedPreferences(APP_TAG, Context.MODE_PRIVATE).edit()
            editor.putInt(RECORD_NUMBER, value)
            editor.apply()
        }

    var isFirstLaunch: Boolean
        get() = context!!.getSharedPreferences(APP_TAG, Context.MODE_PRIVATE).getBoolean(IS_FIRST_LAUNCH, true)
        set(value) {
            val editor = context!!.getSharedPreferences(APP_TAG, Context.MODE_PRIVATE).edit()
            editor.putBoolean(IS_FIRST_LAUNCH, value)
            editor.apply()
        }

    fun getFileNamePrefix(): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(COLUMN_FILE_NAME_PREFIX, "")
    }

    fun getRecordTitlePrefix(): String {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(COLUMN_RECORD_TITLE_PREFIX, "")
    }

    fun getOutputFormat(): Int {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(COLUMN_OUTPUT_FORMAT, "-1").toInt()
    }

    fun setOutputFormat(format: Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(COLUMN_OUTPUT_FORMAT, format.toString())
        editor.apply()
    }

    fun getAudioEncoder(): Int {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(COLUMN_AUDIO_ENCODER, "-1").toInt()
    }

    fun setAudioEncoder(encoder: Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(COLUMN_AUDIO_ENCODER, encoder.toString())
        editor.apply()
    }

    fun getAudioEncodingBitRate(): Int {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(COLUMN_AUDIO_ENCODING_BIT_RATE, "-1").toInt()
    }

    fun setAudioEncodingBitRate(rate: Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(COLUMN_AUDIO_ENCODING_BIT_RATE, rate.toString())
        editor.apply()
    }

    fun getAudioSamplingRate(): Int {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(COLUMN_AUDIO_SAMPLING_RATE, "-1").toInt()
    }

    fun setAudioSamplingRate(rate: Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(COLUMN_AUDIO_SAMPLING_RATE, rate.toString())
        editor.apply()
    }

    fun getAudioChannels(): Int {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString(COLUMN_AUDIO_CHANNELS, "-1").toInt()
    }

    fun setAudioChannels(channels: Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(COLUMN_AUDIO_CHANNELS, channels.toString())
        editor.apply()
    }

    fun outputFormatString(): String {
        return when (getOutputFormat()) {
            MediaRecorder.OutputFormat.THREE_GPP -> "3GPP"
            MediaRecorder.OutputFormat.MPEG_4 -> "MPEG4"
            else -> "Default"
        }
    }

    fun audioEncoderString(): String {
        return when (getAudioEncoder()) {
            MediaRecorder.AudioEncoder.AMR_NB -> "AMR (Narrowband)"
            MediaRecorder.AudioEncoder.AMR_WB -> "AMR (Wideband)"
            MediaRecorder.AudioEncoder.AAC -> "AAC Low Complexity (AAC-LC)"
            MediaRecorder.AudioEncoder.HE_AAC -> "High Efficiency AAC (HE-AAC)"
            MediaRecorder.AudioEncoder.AAC_ELD -> "Enhanced Low Delay AAC (AAC-ELD)"
            MediaRecorder.AudioEncoder.VORBIS -> "Ogg Vorbis"
            else -> "Default"
        }
    }

    fun audioChannelsString(): String {
        return when (getAudioChannels()) {
            MyMediaRecorder.AudioChannels.MONO -> context!!.resources.getString(R.string.audio_channels_mono)
            else -> context!!.resources.getString(R.string.audio_channels_stereo)
        }
    }

    fun formatDate(date: Long): String {
        var result = if (android.text.format.DateFormat.is24HourFormat(context)) {
            SimpleDateFormat("dd, MMMM yyyy, HH:mm:ss", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("dd, MMMM yyyy, hh:mm:ss a", Locale.getDefault()).format(date)
        }

        for ((k, v) in rusMonths) {
            result = result.replace(k, v)
        }

        return result
    }

    private val rusMonths = mapOf(
            "января" to "Январь",
            "февраля" to "Февраль",
            "марта" to "Март",
            "апреля" to "Апрель",
            "мая" to "Май",
            "июня" to "Июнь",
            "июля" to "Июль",
            "августа" to "Август",
            "сентября" to "Сентябрь",
            "октября" to "Октябрь",
            "ноября" to "Ноябрь",
            "декабря" to "Декабрь"
    )

    fun formatDateForFileName(date: Date): String {
        return if (android.text.format.DateFormat.is24HourFormat(context)) {
            SimpleDateFormat("_yyyyMMdd_HHmmss", Locale.getDefault()).format(date)
        } else {
            SimpleDateFormat("_yyyyMMdd_hhmmss_a", Locale.getDefault()).format(date)
        }
    }

    fun formatDuration(duration: Long): String {
        val hour = duration / 3600
        val minute = duration % 3600 / 60
        val second = duration % 3600 % 60

        var result = ""

        if (hour > 0) {
            result += hour.toString() + context?.getString(R.string.h) + " "
        }

        if (minute > 0) {
            result += minute.toString() + context?.getString(R.string.m) + " "
        }

        result += second.toString() + context?.getString(R.string.s)

        return result
    }

    fun increaseAudioVolume() {
        val audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManagerStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0)
    }

    fun restoreAudioVolume() {
        val audioManager = context!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManagerStreamVolume, 0)
    }
}