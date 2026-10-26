package com.auraflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.auraflow.data.reminder.ReminderWorker

class AuraFlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createReminderChannel()
    }

    private fun createReminderChannel() {
        val channel = NotificationChannel(
            ReminderWorker.CHANNEL_ID,
            getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = getString(R.string.reminder_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
