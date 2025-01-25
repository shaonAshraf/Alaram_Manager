package com.bjit.alarmmanager

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var btnSetAlarm: Button
    lateinit var btnEditAlarm: Button
    lateinit var btnDeleteAlarm: Button
    lateinit var timePicker: TimePicker
    lateinit var textViewAlarm: TextView
    var alarmRequestCode = 0
    var currentAlarmTime: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "Alarm App"
        timePicker = findViewById(R.id.timePicker)
        btnSetAlarm = findViewById(R.id.buttonAlarm)
        btnEditAlarm = findViewById(R.id.buttonEdit)
        btnDeleteAlarm = findViewById(R.id.buttonDelete)
        textViewAlarm = findViewById(R.id.textViewAlarm)

        // Check for Do Not Disturb permission
        checkDoNotDisturbPermission()

        // Set alarm button click listener
        btnSetAlarm.setOnClickListener {
            val calendar: Calendar = Calendar.getInstance()
            if (Build.VERSION.SDK_INT >= 23) {
                calendar.set(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    timePicker.hour,
                    timePicker.minute,
                    0
                )
            } else {
                calendar.set(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    timePicker.currentHour,
                    timePicker.currentMinute, 0
                )
            }
            setAlarm(calendar.timeInMillis, alarmRequestCode++)
            currentAlarmTime = calendar.timeInMillis
            textViewAlarm.text = "Alarm set for: ${calendar.time}"
        }

        // Edit alarm button click listener
        btnEditAlarm.setOnClickListener {
            currentAlarmTime?.let {
                val calendar = Calendar.getInstance().apply { timeInMillis = it }
                if (Build.VERSION.SDK_INT >= 23) {
                    timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
                    timePicker.minute = calendar.get(Calendar.MINUTE)
                } else {
                    timePicker.currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    timePicker.currentMinute = calendar.get(Calendar.MINUTE)
                }
            }
        }

        // Delete alarm button click listener
        btnDeleteAlarm.setOnClickListener {
            currentAlarmTime?.let {
                cancelAlarm(alarmRequestCode - 1)
                textViewAlarm.text = "No alarm set"
                currentAlarmTime = null
            }
        }
    }

    // Method to check and request Do Not Disturb permission
    private fun checkDoNotDisturbPermission() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    // Method to set an alarm respecting Do Not Disturb mode
    private fun setAlarm(timeInMillis: Long, requestCode: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
            Toast.makeText(this, "Do Not Disturb mode is enabled. Alarm will not ring.", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MyAlarm::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setRepeating(
            AlarmManager.RTC,
            timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Toast.makeText(this, "Alarm is set", Toast.LENGTH_SHORT).show()
    }

    // Method to cancel an alarm
    private fun cancelAlarm(requestCode: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MyAlarm::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Alarm is canceled", Toast.LENGTH_SHORT).show()
    }

    // Method to set a snooze alarm
    private fun setSnoozeAlarm(snoozeTimeInMillis: Long, requestCode: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MyAlarm::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            snoozeTimeInMillis,
            pendingIntent
        )
        Toast.makeText(this, "Snooze alarm is set", Toast.LENGTH_SHORT).show()
    }

    // Method to set a custom alarm with user-defined parameters
    private fun setCustomAlarm(timeInMillis: Long, intervalMillis: Long, requestCode: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MyAlarm::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setRepeating(
            AlarmManager.RTC,
            timeInMillis,
            intervalMillis,
            pendingIntent
        )
        Toast.makeText(this, "Custom alarm is set", Toast.LENGTH_SHORT).show()
    }

    // BroadcastReceiver to handle alarm actions
    private class MyAlarm : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("Alarm Bell", "Alarm just fired")

            // Set snooze alarm for 10 minutes later
            val snoozeTimeInMillis = System.currentTimeMillis() + 10 * 60 * 1000
            val mainActivity = context as MainActivity
            mainActivity.setSnoozeAlarm(snoozeTimeInMillis, mainActivity.alarmRequestCode++)
        }
    }
}