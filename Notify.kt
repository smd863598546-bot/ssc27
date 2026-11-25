package com.ssc27.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class HourlyWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        Notifier.post(applicationContext, false)
        return Result.success()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Notifier.schedule(context)
    }
}

object Notifier {
    private const val CH = "study_reminders"
    private const val WORK = "hourly_reminder"
    private const val NOTIF_ID = 1001

    fun createChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH, "Study reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun granted(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun post(ctx: Context, force: Boolean) {
        val p = Prefs(ctx)
        if (!force) {
            if (!p.countdownOn && !p.motivationOn) return
            val h = LocalTime.now().hour
            if (!p.nightOn && (h >= 22 || h < 6)) return
        }
        if (!granted(ctx)) return
        createChannel(ctx)

        val days = daysLeft(p.examDate)
        val parts = ArrayList<String>()
        if (p.countdownOn || force) parts.add(Texts.countdownLine(p.name, days, p.examDate, p.lang))
        if (p.motivationOn || force) {
            parts.add(Messages.pick(p.lang, p.name, days, LocalTime.now().hour, LocalDate.now().toEpochDay()))
        }
        val body = parts.joinToString(" ")

        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("SSC 27 Study Tracker")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        // Same id every time: a new notification replaces the old one, so no duplicates.
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n)
    }

    fun schedule(ctx: Context) {
        val p = Prefs(ctx)
        val wm = WorkManager.getInstance(ctx.applicationContext)
        if (p.countdownOn || p.motivationOn) {
            val req = PeriodicWorkRequestBuilder<HourlyWorker>(p.interval.toLong(), TimeUnit.HOURS).build()
            wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
        } else {
            wm.cancelUniqueWork(WORK)
        }
    }
}
