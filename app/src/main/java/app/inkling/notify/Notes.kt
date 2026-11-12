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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.inkling.MainActivity
import app.inkling.R
import app.inkling.domain.Lang
import app.inkling.domain.tr

/** Notifications: the candle burning (a countdown), the candle burnt down, and the gentle reminders. */
object Notes {
    const val CANDLE = 10; const val DONE = 11; const val CLASS = 12; const val EVENING = 13
    private const val CH_CANDLE = "candle"; private const val CH_NUDGE = "nudge"

    fun allowed(ctx: Context) = android.os.Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_CANDLE, tr("Свеча", "Candle"), NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_NUDGE, tr("Напоминания академии", "Academy reminders"), NotificationManager.IMPORTANCE_DEFAULT))
    }
    private fun open(ctx: Context, what: String = "") = PendingIntent.getActivity(ctx, what.hashCode(), Intent(ctx, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("go", what), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** The candle in the shade: a countdown to [end]; an alarm says when it has burnt down. */
    @android.annotation.SuppressLint("MissingPermission") // allowed() checks it
    fun candle(ctx: Context, subject: String, end: Long) {
        if (!allowed(ctx)) return
        channels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_CANDLE).setSmallIcon(R.drawable.ic_stat).setContentTitle(tr("Свеча горит", "The candle is burning"))
            .setContentText(subject).setUsesChronometer(true).setChronometerCountDown(true).setWhen(end).setShowWhen(true)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open(ctx, "candle")).build()
        NotificationManagerCompat.from(ctx).notify(CANDLE, n)
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.set(AlarmManager.RTC_WAKEUP, end, PendingIntent.getBroadcast(ctx, 7, Intent(ctx, CandleDone::class.java), PendingIntent.FLAG_IMMUTABLE))
    }

    fun stopCandle(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(CANDLE)
        ctx.getSystemService(AlarmManager::class.java).cancel(PendingIntent.getBroadcast(ctx, 7, Intent(ctx, CandleDone::class.java), PendingIntent.FLAG_IMMUTABLE))
    }

    @android.annotation.SuppressLint("MissingPermission") // allowed() checks it
    fun nudge(ctx: Context, id: Int, title: String, text: String, what: String = "") {
        if (!allowed(ctx)) return
        channels(ctx)
        val n = NotificationCompat.Builder(ctx, CH_NUDGE).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open(ctx, what)).setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}

/** The candle's time is up. */
class CandleDone : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        Lang.ru = ctx.resources.configuration.locales[0].language == "ru"
        NotificationManagerCompat.from(ctx).cancel(Notes.CANDLE)
        Notes.nudge(ctx, Notes.DONE, tr("Свеча догорела", "The candle has burnt down"), tr("Пора на перемену. Загляните в башню – запишем, что успели.", "Time for a break. Look into the tower – let's write down what you did."), "candle")
    }
}
