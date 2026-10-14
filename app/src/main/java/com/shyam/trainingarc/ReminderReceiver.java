
package com.shyam.trainingarc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "study_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String subject = intent.getStringExtra("subject");
        String pdfName = intent.getStringExtra("pdfName");

        if (subject == null || subject.trim().isEmpty()) {
            subject = "Study Session";
        }

        if (pdfName == null || pdfName.trim().isEmpty()) {
            pdfName = "Open your study materials";
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);

        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription("Shyam Training Arc study reminders");
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
            builder.setPriority(Notification.PRIORITY_HIGH);
        }

        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("⚔ Study Time: " + subject)
                .setContentText(pdfName)
                .setStyle(new Notification.BigTextStyle()
                        .bigText("Your training mission is ready. "
                                + "Subject: " + subject
                                + ". Material: " + pdfName))
                .setAutoCancel(true)
                .build();

        manager.notify(
                (subject + pdfName).hashCode() & 0x7fffffff,
                notification);
    }
}
  
