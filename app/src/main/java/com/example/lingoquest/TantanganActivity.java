package com.example.lingoquest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TantanganActivity extends AppCompatActivity {

    private ImageView ivProfile, ivRank1Image, ivRank2Image, ivRank3Image;
    private TextView tvUsername, tvLevel, tvDailyTimer, tvWeeklyTimer;
    private ProgressBar pbDailyMission, pbWeeklyChallenge;
    private TextView tvDailyProgress, tvWeeklyProgress, tvSpeedRecord, tvSpeedQuestions, tvDuelStreak, tvDuelWins,
            tvSurvivalLevel, tvStoryProgress;
    private LinearLayout llAchievement7Days, llAchievement100Questions, llAchievementLevel20, llAchievementWeeklyChampion;
    private TextView tvRank1, tvRank2, tvRank3;
    private Button btnSeeMore, btnDailyMission, btnWeeklyChallenge;
    private LinearLayout dailyRewardContainer, weeklyRewardContainer;
    private DatabaseHelper dbHelper;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Timer dailyTimer, weeklyTimer, realTimeTimer;
    private long dailyEndTime, weeklyEndTime;
    private int userId;
    private int dailyTarget = 10; // Target misi harian
    private int weeklyTarget = 50; // Target tantangan mingguan
    private int dailyProgress = 0;
    private int weeklyProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_tantangan);

        // Inisialisasi DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Inisialisasi Views
        ivProfile = findViewById(R.id.profile_image);
        ivRank1Image = findViewById(R.id.rank1_image);
        ivRank2Image = findViewById(R.id.rank2_image);
        ivRank3Image = findViewById(R.id.rank3_image);
        tvUsername = findViewById(R.id.username);
        tvLevel = findViewById(R.id.level);
        tvDailyTimer = findViewById(R.id.daily_timer);
        tvWeeklyTimer = findViewById(R.id.weekly_timer);
        pbDailyMission = findViewById(R.id.daily_progress_bar);
        pbWeeklyChallenge = findViewById(R.id.weekly_progress_bar);
        tvDailyProgress = findViewById(R.id.daily_progress_text);
        tvWeeklyProgress = findViewById(R.id.weekly_progress_text);
        llAchievement7Days = findViewById(R.id.achievement_7days);
        llAchievement100Questions = findViewById(R.id.achievement_100questions);
        llAchievementLevel20 = findViewById(R.id.achievement_level20);
        llAchievementWeeklyChampion = findViewById(R.id.achievement_weekly_champion);
        tvSpeedRecord = findViewById(R.id.speed_record);
        tvSpeedQuestions = findViewById(R.id.speed_questions);
        tvDuelStreak = findViewById(R.id.duel_streak);
        tvDuelWins = findViewById(R.id.duel_wins);
        tvSurvivalLevel = findViewById(R.id.survival_level);
        tvStoryProgress = findViewById(R.id.story_progress);
        tvRank1 = findViewById(R.id.rank1_text);
        tvRank2 = findViewById(R.id.rank2_text);
        tvRank3 = findViewById(R.id.rank3_text);
        btnSeeMore = findViewById(R.id.see_more_button);
        btnDailyMission = findViewById(R.id.daily_mission_button);
        btnWeeklyChallenge = findViewById(R.id.weekly_challenge_button);
        dailyRewardContainer = findViewById(R.id.daily_reward_container);
        weeklyRewardContainer = findViewById(R.id.weekly_reward_container);

        // Ambil userId dari SharedPreferences
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Muat data pengguna untuk header
        loadUserData();

        // Muat pencapaian terbaru
        loadAchievements();

        // Muat misi harian dan tantangan mingguan
        loadDailyMission();
        loadWeeklyChallenge();

        // Muat mode tantangan
        loadChallengeModes();

        // Muat peringkat mingguan
        loadWeeklyRanking();

        // Atur timer untuk misi harian, mingguan, dan waktu real-time
        setDailyTimer();
        setWeeklyTimer();
        setRealTimeTimer();

        // Listener untuk tombol "Lihat Selengkapnya"
        btnSeeMore.setOnClickListener(v -> {
            Intent intent = new Intent(TantanganActivity.this, PeringkatActivity.class);
            startActivity(intent);
        });

        // Listener untuk tombol "Lanjutkan" (Misi Harian) - Arahkan ke BelajarActivity
        btnDailyMission.setOnClickListener(v -> {
            Intent intent = new Intent(TantanganActivity.this, BelajarActivity.class);
            startActivity(intent);
        });

        // Listener untuk tombol "Kerjakan" (Tantangan Mingguan) - Arahkan ke BelajarActivity
        btnWeeklyChallenge.setOnClickListener(v -> {
            Intent intent = new Intent(TantanganActivity.this, BelajarActivity.class);
            startActivity(intent);
        });

        // Atur Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            NavigationItem navItem = NavigationItem.fromItemId(item.getItemId());
            if (navItem == null) return false;
            switch (navItem) {
                case NAV_HOME:
                    startActivity(new Intent(TantanganActivity.this, MainActivity.class));
                    return true;
                case NAV_BELAJAR:
                    startActivity(new Intent(TantanganActivity.this, BelajarActivity.class));
                    return true;
                case NAV_TANTANGAN:
                    return true; // Sudah di TantanganActivity
                case NAV_PERINGKAT:
                    startActivity(new Intent(TantanganActivity.this, PeringkatActivity.class));
                    return true;
                case NAV_PROFIL:
                    startActivity(new Intent(TantanganActivity.this, ProfilActivity.class));
                    return true;
                default:
                    return false;
            }
        });

        // Tandai tab Tantangan sebagai aktif
        bottomNav.setSelectedItemId(R.id.nav_tantangan);
    }

    // **Header: Foto Profil, Nama, dan Level Pengguna**
    private void loadUserData() {
        SQLiteDatabase readableDb = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_USERNAME + ", " + DatabaseHelper.COLUMN_AVATAR_URL + ", " + DatabaseHelper.COLUMN_LEVEL +
                " FROM " + DatabaseHelper.TABLE_USERS + " u" +
                " JOIN " + DatabaseHelper.TABLE_USER_STATS + " s ON u." + DatabaseHelper.COLUMN_USER_ID + " = s." + DatabaseHelper.COLUMN_USER_ID +
                " WHERE u." + DatabaseHelper.COLUMN_USER_ID + " = ?";
        Cursor cursor = readableDb.rawQuery(query, new String[]{String.valueOf(userId)});

        User user = null;
        if (cursor.moveToFirst()) {
            String username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
            String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AVATAR_URL));
            int level = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LEVEL));
            user = new User(username, avatarUrl, level);
        }
        cursor.close();

        if (user != null) {
            loadProfileImage(ivProfile, user.getAvatarUrl());
            tvUsername.setText(user.getUsername());
            tvLevel.setText("Level " + user.getLevel());
        } else {
            loadProfileImage(ivProfile, null);
            tvUsername.setText("Pengguna");
            tvLevel.setText("Level 0");
        }
    }

    private void loadProfileImage(ImageView imageView, String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                File file = new File(avatarUrl);
                if (file.exists()) {
                    imageView.setImageURI(Uri.fromFile(file));
                } else {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            } catch (Exception e) {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                Log.e("TantanganActivity", "Error loading image: " + e.getMessage());
            }
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    // **Pencapaian Terbaru**
    private void loadAchievements() {
        SQLiteDatabase readableDb = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_STREAK_DAYS + ", " + DatabaseHelper.COLUMN_CORRECT_ANSWERS + ", " +
                DatabaseHelper.COLUMN_LEVEL + ", " + DatabaseHelper.COLUMN_IS_WEEKLY_CHAMPION +
                " FROM " + DatabaseHelper.TABLE_USER_STATS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
        Cursor cursor = readableDb.rawQuery(query, new String[]{String.valueOf(userId)});

        UserStats stats = null;
        if (cursor.moveToFirst()) {
            int streak = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STREAK_DAYS));
            int correctAnswers = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CORRECT_ANSWERS));
            int level = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LEVEL));
            boolean isWeeklyChampion = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_WEEKLY_CHAMPION)) == 1;
            stats = new UserStats(streak, correctAnswers, level, isWeeklyChampion);
        }
        cursor.close();

        if (stats != null) {
            if (stats.getStreak() >= 7) llAchievement7Days.setAlpha(1.0f);
            else llAchievement7Days.setAlpha(0.5f);
            if (stats.getCorrectAnswers() >= 100) llAchievement100Questions.setAlpha(1.0f);
            else llAchievement100Questions.setAlpha(0.5f);
            if (stats.getLevel() >= 20) llAchievementLevel20.setAlpha(1.0f);
            else llAchievementLevel20.setAlpha(0.5f);
            if (stats.isWeeklyChampion()) llAchievementWeeklyChampion.setAlpha(1.0f);
            else llAchievementWeeklyChampion.setAlpha(0.5f);
        }
    }

    // **Misi Harian**
    private void loadDailyMission() {
        SQLiteDatabase readableDb = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_END_TIME + ", " + DatabaseHelper.COLUMN_DAILY_PROGRESS +
                " FROM " + DatabaseHelper.TABLE_DAILY_MISSIONS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? LIMIT 1";
        Cursor cursor = readableDb.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_END_TIME)) * 1000;
            dailyProgress = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DAILY_PROGRESS));
            long currentTime = System.currentTimeMillis();
            if (endTime < currentTime) {
                resetDailyMission();
                dailyProgress = 0;
            }
        } else {
            resetDailyMission();
            dailyProgress = 0;
        }
        cursor.close();

        pbDailyMission.setMax(dailyTarget);
        pbDailyMission.setProgress(dailyProgress);
        tvDailyProgress.setText(dailyProgress + "/" + dailyTarget + " soal");
        if (dailyProgress >= dailyTarget) {
            awardDailyXP();
            resetDailyMission();
            dailyProgress = 0;
            pbDailyMission.setProgress(0);
            tvDailyProgress.setText("0/" + dailyTarget + " soal");
            dailyRewardContainer.setVisibility(View.VISIBLE);
        } else {
            dailyRewardContainer.setVisibility(View.GONE);
        }
        dailyEndTime = getDailyEndTime();
    }

    private void awardDailyXP() {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        String query = "UPDATE " + DatabaseHelper.TABLE_USER_STATS + " SET " + DatabaseHelper.COLUMN_POINTS + " = " + DatabaseHelper.COLUMN_POINTS + " + 50 WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
        writableDb.execSQL(query, new String[]{String.valueOf(userId)});
        Toast.makeText(this, "+50 XP dari Misi Harian!", Toast.LENGTH_SHORT).show();
        loadUserData(); // Perbarui level dan XP di UI
    }

    private long getDailyEndTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void resetDailyMission() {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        writableDb.execSQL("DELETE FROM " + DatabaseHelper.TABLE_DAILY_MISSIONS + " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        long newEndTime = getDailyEndTime();
        writableDb.execSQL("INSERT INTO " + DatabaseHelper.TABLE_DAILY_MISSIONS + " (" + DatabaseHelper.COLUMN_USER_ID + ", " + DatabaseHelper.COLUMN_END_TIME + ", " + DatabaseHelper.COLUMN_DAILY_PROGRESS + ") VALUES (?, ?, 0)",
                new Object[]{userId, newEndTime / 1000});
        dailyEndTime = newEndTime;
    }

    private void setDailyTimer() {
        if (dailyEndTime > 0) {
            dailyTimer = new Timer();
            dailyTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateDailyTimer();
                }
            }, 0, 1000);
        }
    }

    private void updateDailyTimer() {
        handler.post(() -> {
            long timeLeft = dailyEndTime - System.currentTimeMillis();
            if (timeLeft > 0) {
                int hours = (int) (timeLeft / (1000 * 60 * 60));
                int minutes = (int) (timeLeft / (1000 * 60) % 60);
                int seconds = (int) (timeLeft / 1000 % 60);
                tvDailyTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            } else {
                tvDailyTimer.setText("00:00:00");
                resetDailyMission();
                dailyProgress = 0;
                loadDailyMission();
                if (dailyTimer != null) {
                    dailyTimer.cancel();
                    setDailyTimer();
                }
            }
        });
    }

    // **Tantangan Mingguan**
    private void loadWeeklyChallenge() {
        SQLiteDatabase readableDb = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_END_TIME + ", " + DatabaseHelper.COLUMN_WEEKLY_PROGRESS +
                " FROM " + DatabaseHelper.TABLE_WEEKLY_CHALLENGES +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? LIMIT 1";
        Cursor cursor = readableDb.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_END_TIME)) * 1000;
            weeklyProgress = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEEKLY_PROGRESS));
            long currentTime = System.currentTimeMillis();
            if (endTime < currentTime) {
                resetWeeklyChallenge();
                weeklyProgress = 0;
            }
        } else {
            resetWeeklyChallenge();
            weeklyProgress = 0;
        }
        cursor.close();

        pbWeeklyChallenge.setMax(weeklyTarget);
        pbWeeklyChallenge.setProgress(weeklyProgress);
        tvWeeklyProgress.setText(weeklyProgress + "/" + weeklyTarget + " soal");
        if (weeklyProgress >= weeklyTarget) {
            awardWeeklyXP();
            resetWeeklyChallenge();
            weeklyProgress = 0;
            pbWeeklyChallenge.setProgress(0);
            tvWeeklyProgress.setText("0/" + weeklyTarget + " soal");
            weeklyRewardContainer.setVisibility(View.VISIBLE);
        } else {
            weeklyRewardContainer.setVisibility(View.GONE);
        }
        weeklyEndTime = getWeeklyEndTime();
    }

    private void awardWeeklyXP() {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        String query = "UPDATE " + DatabaseHelper.TABLE_USER_STATS + " SET " + DatabaseHelper.COLUMN_POINTS + " = " + DatabaseHelper.COLUMN_POINTS + " + 200 WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
        writableDb.execSQL(query, new String[]{String.valueOf(userId)});
        Toast.makeText(this, "+200 XP dari Tantangan Mingguan!", Toast.LENGTH_SHORT).show();
        loadUserData(); // Perbarui level dan XP di UI
    }

    private long getWeeklyEndTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }

    private void resetWeeklyChallenge() {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        writableDb.execSQL("DELETE FROM " + DatabaseHelper.TABLE_WEEKLY_CHALLENGES + " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        long newEndTime = getWeeklyEndTime();
        writableDb.execSQL("INSERT INTO " + DatabaseHelper.TABLE_WEEKLY_CHALLENGES + " (" + DatabaseHelper.COLUMN_USER_ID + ", " + DatabaseHelper.COLUMN_END_TIME + ", " + DatabaseHelper.COLUMN_WEEKLY_PROGRESS + ") VALUES (?, ?, 0)",
                new Object[]{userId, newEndTime / 1000});
        weeklyEndTime = newEndTime;
    }

    private void setWeeklyTimer() {
        if (weeklyEndTime > 0) {
            weeklyTimer = new Timer();
            weeklyTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateWeeklyTimer();
                }
            }, 0, 1000);
        }
    }

    private void updateWeeklyTimer() {
        handler.post(() -> {
            long timeLeft = weeklyEndTime - System.currentTimeMillis();
            if (timeLeft > 0) {
                int days = (int) (timeLeft / (1000 * 60 * 60 * 24));
                tvWeeklyTimer.setText(days + " hari tersisa");
            } else {
                tvWeeklyTimer.setText("0 hari tersisa");
                resetWeeklyChallenge();
                weeklyProgress = 0;
                loadWeeklyChallenge();
                if (weeklyTimer != null) {
                    weeklyTimer.cancel();
                    setWeeklyTimer();
                }
            }
        });
    }

    // **Waktu Real-Time di Kanan**
    private void setRealTimeTimer() {
        realTimeTimer = new Timer();
        realTimeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateRealTime();
            }
        }, 0, 1000);
    }

    private void updateRealTime() {
        handler.post(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String currentTime = sdf.format(new Date());
            tvWeeklyTimer.setText(currentTime); // Ganti dengan TextView khusus jika ada
        });
    }

    // **Mode Tantangan**
    private void loadChallengeModes() {
        SQLiteDatabase readableDb = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_CHALLENGE_MODES +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
        Cursor cursor = readableDb.rawQuery(query, new String[]{String.valueOf(userId)});

        ChallengeModes modes = null;
        if (cursor.moveToFirst()) {
            int speedTarget = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SPEED_TARGET));
            int speedQuestions = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SPEED_QUESTIONS));
            tvSpeedRecord.setText("Rekor: " + speedTarget + " soal");
            tvSpeedQuestions.setText(speedQuestions + " soal dikerjakan");
            if (speedQuestions >= speedTarget) {
                updateSpeedTarget(speedTarget * 2);
            }

            int duelTarget = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DUEL_TARGET));
            int duelWins = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DUEL_WINS));
            tvDuelStreak.setText("Rekor: " + duelTarget + " menang");
            tvDuelWins.setText(duelWins + " menang berturut");
            if (duelWins >= duelTarget) {
                updateDuelTarget(duelTarget + 2);
            }

            int survivalTarget = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SURVIVAL_TARGET));
            int survivalLevel = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SURVIVAL_LEVEL));
            tvSurvivalLevel.setText("Level tertinggi: " + survivalLevel);
            if (survivalLevel >= survivalTarget) {
                updateSurvivalTarget(survivalTarget * 2);
            }

            int storyProgress = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STORY_PROGRESS));
            tvStoryProgress.setText(storyProgress + " cerita selesai");
            if (storyProgress >= cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STORY_TARGET))) {
                updateStoryTarget(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STORY_TARGET)) + 1);
            }
        }
        cursor.close();
    }

    private void updateSpeedTarget(int newTarget) {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        writableDb.execSQL("UPDATE " + DatabaseHelper.TABLE_CHALLENGE_MODES + " SET " + DatabaseHelper.COLUMN_SPEED_TARGET + " = ? WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new Object[]{newTarget, userId});
    }

    private void updateDuelTarget(int newTarget) {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        writableDb.execSQL("UPDATE " + DatabaseHelper.TABLE_CHALLENGE_MODES + " SET " + DatabaseHelper.COLUMN_DUEL_TARGET + " = ? WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new Object[]{newTarget, userId});
    }

    private void updateSurvivalTarget(int newTarget) {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        writableDb.execSQL("UPDATE " + DatabaseHelper.TABLE_CHALLENGE_MODES + " SET " + DatabaseHelper.COLUMN_SURVIVAL_TARGET + " = ? WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new Object[]{newTarget, userId});
    }

    private void updateStoryTarget(int newTarget) {
        SQLiteDatabase writableDb = dbHelper.getWritableDatabase();
        writableDb.execSQL("UPDATE " + DatabaseHelper.TABLE_CHALLENGE_MODES + " SET " + DatabaseHelper.COLUMN_STORY_TARGET + " = ? WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new Object[]{newTarget, userId});
    }

    // **Peringkat Mingguan**
    private void loadWeeklyRanking() {
        Cursor cursor = dbHelper.getXpByPeriod("Mingguan", 3);
        List<UserRanking> rankings = new ArrayList<>();

        while (cursor.moveToNext()) {
            String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow("avatar_url"));
            int xp = cursor.isNull(cursor.getColumnIndexOrThrow("total_xp")) ? 0 : cursor.getInt(cursor.getColumnIndexOrThrow("total_xp"));
            rankings.add(new UserRanking(username, avatarUrl, xp));
        }
        cursor.close();

        if (rankings.size() >= 1) {
            tvRank1.setText("1 " + rankings.get(0).getUsername() + ", " + rankings.get(0).getXp() + " XP");
            loadProfileImage(ivRank1Image, rankings.get(0).getAvatarUrl());
        }
        if (rankings.size() >= 2) {
            tvRank2.setText("2 " + rankings.get(1).getUsername() + ", " + rankings.get(1).getXp() + " XP");
            loadProfileImage(ivRank2Image, rankings.get(1).getAvatarUrl());
        }
        if (rankings.size() >= 3) {
            tvRank3.setText("3 " + rankings.get(2).getUsername() + ", " + rankings.get(2).getXp() + " XP");
            loadProfileImage(ivRank3Image, rankings.get(2).getAvatarUrl());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dailyTimer != null) {
            dailyTimer.cancel();
        }
        if (weeklyTimer != null) {
            weeklyTimer.cancel();
        }
        if (realTimeTimer != null) {
            realTimeTimer.cancel();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    // **Kelas Pendukung**
    private static class User {
        private String username, avatarUrl;
        private int level;

        public User(String username, String avatarUrl, int level) {
            this.username = username;
            this.avatarUrl = avatarUrl;
            this.level = level;
        }

        public String getUsername() { return username; }
        public String getAvatarUrl() { return avatarUrl; }
        public int getLevel() { return level; }
    }

    private static class UserStats {
        private int streak, correctAnswers, level;
        private boolean isWeeklyChampion;

        public UserStats(int streak, int correctAnswers, int level, boolean isWeeklyChampion) {
            this.streak = streak;
            this.correctAnswers = correctAnswers;
            this.level = level;
            this.isWeeklyChampion = isWeeklyChampion;
        }

        public int getStreak() { return streak; }
        public int getCorrectAnswers() { return correctAnswers; }
        public int getLevel() { return level; }
        public boolean isWeeklyChampion() { return isWeeklyChampion; }
    }

    private static class DailyMission {
        private long endTime;
        private int progress;

        public DailyMission(long endTime, int progress) {
            this.endTime = endTime;
            this.progress = progress;
        }

        public long getEndTime() { return endTime; }
        public int getProgress() { return progress; }
    }

    private static class WeeklyChallenge {
        private long endTime;
        private int progress;

        public WeeklyChallenge(long endTime, int progress) {
            this.endTime = endTime;
            this.progress = progress;
        }

        public long getEndTime() { return endTime; }
        public int getProgress() { return progress; }
    }

    private static class ChallengeModes {
        private int speedTarget, speedQuestions, duelTarget, duelWins, survivalTarget, survivalLevel, storyTarget, storyProgress;

        public ChallengeModes(int speedTarget, int speedQuestions, int duelTarget, int duelWins, int survivalTarget, int survivalLevel, int storyTarget, int storyProgress) {
            this.speedTarget = speedTarget;
            this.speedQuestions = speedQuestions;
            this.duelTarget = duelTarget;
            this.duelWins = duelWins;
            this.survivalTarget = survivalTarget;
            this.survivalLevel = survivalLevel;
            this.storyTarget = storyTarget;
            this.storyProgress = storyProgress;
        }

        public int getSpeedTarget() { return speedTarget; }
        public int getSpeedQuestions() { return speedQuestions; }
        public int getDuelTarget() { return duelTarget; }
        public int getDuelWins() { return duelWins; }
        public int getSurvivalTarget() { return survivalTarget; }
        public int getSurvivalLevel() { return survivalLevel; }
        public int getStoryTarget() { return storyTarget; }
        public int getStoryProgress() { return storyProgress; }
    }

    private static class UserRanking {
        private String username, avatarUrl;
        private int xp;

        public UserRanking(String username, String avatarUrl, int xp) {
            this.username = username;
            this.avatarUrl = avatarUrl;
            this.xp = xp;
        }

        public String getUsername() { return username; }
        public String getAvatarUrl() { return avatarUrl; }
        public int getXp() { return xp; }
    }
}