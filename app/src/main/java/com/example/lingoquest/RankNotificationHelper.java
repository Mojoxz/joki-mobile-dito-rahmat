package com.example.lingoquest;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RankNotificationHelper {
    private static final String TAG = "RankNotification";
    private static final String CHANNEL_ID = "rank_notification_channel";
    private static final String CHANNEL_NAME = "Notifikasi Peringkat";
    private static final int NOTIFICATION_ID = 1001;

    private Context context;
    private NotificationManager notificationManager;

    public RankNotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifikasi untuk update peringkat dan motivasi belajar");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleRankNotification() {
        // Jadwalkan notifikasi harian pada jam 9 pagi dan 6 sore
        PeriodicWorkRequest morningWork = new PeriodicWorkRequest.Builder(
                RankNotificationWorker.class,
                24, TimeUnit.HOURS
        ).setInitialDelay(calculateDelayToNextHour(9), TimeUnit.MILLISECONDS)
                .build();

        PeriodicWorkRequest eveningWork = new PeriodicWorkRequest.Builder(
                RankNotificationWorker.class,
                24, TimeUnit.HOURS
        ).setInitialDelay(calculateDelayToNextHour(18), TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueue(morningWork);
        WorkManager.getInstance(context).enqueue(eveningWork);
    }

    private long calculateDelayToNextHour(int targetHour) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.util.Calendar target = (java.util.Calendar) calendar.clone();

        target.set(java.util.Calendar.HOUR_OF_DAY, targetHour);
        target.set(java.util.Calendar.MINUTE, 0);
        target.set(java.util.Calendar.SECOND, 0);

        if (target.before(calendar)) {
            target.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        return target.getTimeInMillis() - calendar.getTimeInMillis();
    }

    public void showRankNotification(int currentRank, int previousRank, String username) {
        String title;
        String message;

        if (currentRank < previousRank) {
            // Naik peringkat
            title = "🎉 Selamat " + username + "!";
            message = "Peringkatmu naik dari #" + previousRank + " ke #" + currentRank + "! Pertahankan!";
        } else if (currentRank > previousRank) {
            // Turun peringkat
            title = "⚠️ Peringkatmu Turun!";
            message = "Dari #" + previousRank + " ke #" + currentRank + ". Ayo belajar lagi untuk naik!";
        } else {
            // Stuck di peringkat yang sama
            title = "💪 Tingkatkan Peringkatmu!";
            message = getMotivationalMessage(currentRank);
        }

        showNotification(title, message);
    }

    public void showMotivationalNotification(int currentRank, int daysStuck) {
        String title;
        String message;

        if (daysStuck >= 3) {
            title = "🔥 Waktunya Bangkit!";
            message = "Peringkatmu stuck di #" + currentRank + " selama " + daysStuck + " hari. Ayo mulai belajar!";
        } else if (currentRank > 10) {
            title = "🎯 Target Top 10!";
            message = "Kamu di peringkat #" + currentRank + ". Yuk belajar untuk masuk Top 10!";
        } else if (currentRank > 5) {
            title = "⭐ Hampir Top 5!";
            message = "Peringkat #" + currentRank + " sudah bagus! Tingkatkan lagi untuk Top 5!";
        } else {
            title = "🏆 Kejar Juara 1!";
            message = "Kamu di peringkat #" + currentRank + ". Sedikit lagi juara 1!";
        }

        showNotification(title, message);
    }

    private String getMotivationalMessage(int rank) {
        String[] messages = {
                "Peringkatmu masih di #" + rank + ". Ayo mulai belajar hari ini!",
                "Stuck di #" + rank + "? Saatnya belajar dan naik peringkat!",
                "Jangan biarkan peringkat #" + rank + " menghalangimu. Ayo belajar!",
                "Peringkat #" + rank + " menunggumu untuk naik. Mulai belajar sekarang!",
                "Sudah berapa lama di #" + rank + "? Waktunya bergerak naik!",
                "Kompetitor sudah belajar! Jangan sampai peringkatmu turun dari #" + rank + "!",
                "Peringkat #" + rank + " bukan tempatmu. Ayo tunjukkan kemampuanmu!"
        };
        return messages[new Random().nextInt(messages.length)];
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(context, PeringkatActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    // Worker class untuk background task
    public static class RankNotificationWorker extends Worker {
        public RankNotificationWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public Result doWork() {
            Context context = getApplicationContext();
            android.content.SharedPreferences prefs = context.getSharedPreferences("LingoQuestPrefs", Context.MODE_PRIVATE);
            String userId = prefs.getString("user_id", null);

            if (userId == null) {
                return Result.failure();
            }

            // Ambil data peringkat dari Firestore
            FirebaseHelper firebaseHelper = FirebaseHelper.getInstance();
            firebaseHelper.getLeaderboard("Harian", 100, new FirebaseHelper.LeaderboardCallback() {
                @Override
                public void onSuccess(java.util.List<java.util.Map<String, Object>> leaderboard) {
                    int currentRank = -1;
                    for (int i = 0; i < leaderboard.size(); i++) {
                        String uid = (String) leaderboard.get(i).get("user_id");
                        if (uid != null && uid.equals(userId)) {
                            currentRank = i + 1;
                            break;
                        }
                    }

                    if (currentRank != -1) {
                        final int finalCurrentRank = currentRank; // Make it final
                        int previousRank = prefs.getInt("previous_rank", currentRank);
                        final int finalPreviousRank = previousRank; // Make it final
                        int daysStuck = prefs.getInt("days_stuck", 0);

                        if (currentRank == previousRank) {
                            daysStuck++;
                            prefs.edit().putInt("days_stuck", daysStuck).apply();
                        } else {
                            daysStuck = 0;
                            prefs.edit().putInt("days_stuck", 0).apply();
                        }

                        prefs.edit().putInt("previous_rank", currentRank).apply();

                        RankNotificationHelper notificationHelper = new RankNotificationHelper(context);
                        final int finalDaysStuck = daysStuck; // Make it final

                        if (daysStuck >= 2) {
                            notificationHelper.showMotivationalNotification(finalCurrentRank, finalDaysStuck);
                        } else if (currentRank != previousRank) {
                            firebaseHelper.getUserData(userId, new FirebaseHelper.UserDataCallback() {
                                @Override
                                public void onSuccess(java.util.Map<String, Object> userData) {
                                    String username = (String) userData.get("username");
                                    notificationHelper.showRankNotification(finalCurrentRank, finalPreviousRank, username != null ? username : "");
                                }

                                @Override
                                public void onFailure(String error) {
                                    notificationHelper.showRankNotification(finalCurrentRank, finalPreviousRank, "");
                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailure(String error) {
                    Log.e("RankNotificationWorker", "Failed to get leaderboard: " + error);
                }
            });

            return Result.success();
        }
    }
}