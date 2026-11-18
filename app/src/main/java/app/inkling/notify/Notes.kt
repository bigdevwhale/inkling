package app.inkling.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.inkling.MainActivity
import app.inkling.R
import app.inkling.domain.Lang
import app.inkling.domain.tr

/** Notifications: the candle burning (a countdown), the candle burnt down (with a bell), and the gentle reminders. */
object Notes {
    const val CANDLE = 10; const val DONE = 11; const val CLASS = 12; const val EVENING = 13
    private const val CH_CANDLE = "candle"; private const val CH_NUDGE = "nudge"; private const val CH_DONE = "done"
    private val bell: AudioAttributes get() = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    /** When the bell last rang (elapsed ms), so the screen and the notification don't ring it twice. */
    @Volatile private var rang = -60_000L
    private fun justRang() = SystemClock.elapsedRealtime() - rang < 5000

    fun allowed(ctx: Context) = android.os.Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_CANDLE, tr("Свеча", "Candle"), NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_NUDGE, tr("Напоминания академии", "Academy reminders"), NotificationManager.IMPORTANCE_DEFAULT))
        // by name, not by id: the channel remembers the sound, and ids change from build to build
        nm.createNotificationChannel(NotificationChannel(CH_DONE, tr("Свеча догорела", "The candle has burnt down"), NotificationManager.IMPORTANCE_HIGH)
            .apply { setSound(Uri.parse("android.resource://${ctx.packageName}/raw/chime"), bell) })
    }

    /** The bell, with the app open: it follows the phone's notification volume, so a silent phone stays silent. */
    fun ring(ctx: Context) {
        if (justRang()) return
        rang = SystemClock.elapsedRealtime()
        runCatching {
            MediaPlayer.create(ctx.applicationContext, R.raw.chime, bell, ctx.getSystemService(AudioManager::class.java).generateAudioSessionId())
                ?.apply { setOnCompletionListener { it.release() }; start() }
        }
    }

    /** The candle has burnt down while nobody looked: a note, with the bell when [chime]. */
    fun done(ctx: Context, chime: Boolean) {
        val quiet = justRang()
        if (chime && !quiet) rang = SystemClock.elapsedRealtime()
        nudge(ctx, DONE, tr("Свеча догорела", "The candle has burnt down"), tr("Пора на перемену. Загляните в башню – запишем, что успели.", "Time for a break. Look into the tower – let's write down what you did."), "candle",
            if (chime) CH_DONE else CH_NUDGE, quiet)
    }
    private fun open(ctx: Context, what: String = "") = PendingIntent.getActivity(ctx, what.hashCode(), Intent(ctx, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("go", what), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun alarm(ctx: Context, chime: Boolean) = PendingIntent.getBroadcast(ctx, 7, Intent(ctx, CandleDone::class.java).putExtra("chime", chime),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** The candle in the shade: a countdown to [end]; an alarm says when it has burnt down, with the bell when [chime]. */
    @android.annotation.SuppressLint("MissingPermission") // allowed() checks it
    fun candle(ctx: Context, subject: String, end: Long, chime: Boolean) {
        if (!allowed(ctx)) return
        channels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_CANDLE).setSmallIcon(R.drawable.ic_stat).setContentTitle(tr("Свеча горит", "The candle is burning"))
            .setContentText(subject).setUsesChronometer(true).setChronometerCountDown(true).setWhen(end).setShowWhen(true)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open(ctx, "candle")).build()
        NotificationManagerCompat.from(ctx).notify(CANDLE, n)
        // through doze too: the phone lies face down for the whole candle. To the second where the system lets us
        // (always before Android 12); otherwise it may come some minutes late, and the bell on the candle's screen is the one in time
        val am = ctx.getSystemService(AlarmManager::class.java)
        if (android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, alarm(ctx, chime))
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, alarm(ctx, chime))
    }

    fun stopCandle(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(CANDLE)
        ctx.getSystemService(AlarmManager::class.java).cancel(alarm(ctx, false))
    }

    @android.annotation.SuppressLint("MissingPermission") // allowed() checks it
    fun nudge(ctx: Context, id: Int, title: String, text: String, what: String = "", channel: String = CH_NUDGE, quiet: Boolean = false) {
        if (!allowed(ctx)) return
        channels(ctx)
        val n = NotificationCompat.Builder(ctx, channel).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open(ctx, what)).setAutoCancel(true).setSilent(quiet).build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

/** The candle's time is up. */
class CandleDone : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        Lang.ru = ctx.resources.configuration.locales[0].language == "ru"
        NotificationManagerCompat.from(ctx).cancel(Notes.CANDLE)
        Notes.done(ctx, i.getBooleanExtra("chime", true))
    }
}
