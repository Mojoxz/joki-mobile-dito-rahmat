package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import com.bumptech.glide.Glide;
import androidx.core.view.WindowCompat;
import android.view.WindowManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView tvLevel, tvPoints, tvStreak, tvTimer;
    private ImageView ivAvatar;
    private Button btnStartChallenge;
    private NestedScrollView nestedScrollView;
    private Timer timer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long challengeEndTime;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        ivAvatar = findViewById(R.id.avatar);
        tvLevel = findViewById(R.id.level_value);
        tvPoints = findViewById(R.id.points_value);
        tvStreak = findViewById(R.id.streak_value);
        tvTimer = findViewById(R.id.timer);
        btnStartChallenge = findViewById(R.id.btn_start_challenge);
        nestedScrollView = findViewById(R.id.nested_scroll_view);

        loadUserData();
        setChallengeTimer();

        findViewById(R.id.layout_english).setOnClickListener(v -> startGameActivity("Bahasa Inggris"));
        findViewById(R.id.layout_japanese).setOnClickListener(v -> startGameActivity("Bahasa Jepang"));
        findViewById(R.id.layout_korean).setOnClickListener(v -> startGameActivity("Bahasa Korea"));
        findViewById(R.id.layout_mandarin).setOnClickListener(v -> startGameActivity("Bahasa Mandarin"));

        findViewById(R.id.see_more_languages).setOnClickListener(v -> {
            nestedScrollView.smoothScrollTo(0, findViewById(R.id.language_section).getBottom());
            loadMoreLanguages();
        });

        btnStartChallenge.setOnClickListener(v -> startChallenge());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            NavigationItem navItem = NavigationItem.fromItemId(item.getItemId());
            if (navItem == null) return false;
            switch (navItem) {
                case NAV_HOME:
                    return true;
                case NAV_BELAJAR:
                    startActivity(new Intent(MainActivity.this, BelajarActivity.class));
                    return true;
                case NAV_TANTANGAN:
                    startActivity(new Intent(MainActivity.this, TantanganActivity.class));
                    return true;
                case NAV_PERINGKAT:
                    startActivity(new Intent(MainActivity.this, PeringkatActivity.class));
                    return true;
                case NAV_PROFIL:
                    startActivity(new Intent(MainActivity.this, ProfilActivity.class));
                    return true;
                default:
                    return false;
            }
        });
    }

    private void loadUserData() {
        User user = getUserDataFromDatabase();
        if (user != null) {
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getAvatarUrl())
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.default_avatar);
            }

            tvLevel.setText(String.valueOf(user.getLevel()));
            tvPoints.setText(String.valueOf(user.getPoints()));
            tvStreak.setText(user.getStreak() + " Hari");
        }
    }

    private void setChallengeTimer() {
        Challenge challenge = getChallengeDataFromDatabase();
        if (challenge != null) {
            challengeEndTime = challenge.getEndTime();
            if (challengeEndTime > System.currentTimeMillis()) {
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        updateTimer();
                    }
                }, 0, 1000);
            } else {
                tvTimer.setText("⏰ 00:00:00");
            }
        } else {
            tvTimer.setText("⏰ Tidak ada tantangan aktif");
        }
    }

    private void updateTimer() {
        handler.post(() -> {
            long timeLeft = challengeEndTime - System.currentTimeMillis();
            if (timeLeft > 0) {
                int hours = (int) (timeLeft / (1000 * 60 * 60));
                int minutes = (int) (timeLeft / (1000 * 60) % 60);
                int seconds = (int) (timeLeft / 1000 % 60);
                tvTimer.setText(String.format("⏰ %02d:%02d:%02d", hours, minutes, seconds));
            } else {
                tvTimer.setText("⏰ 00:00:00");
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        });
    }

    private void startGameActivity(String language) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("language", language);
        startActivity(intent);
    }

    private void loadMoreLanguages() {
        // Implementasi untuk menampilkan lebih banyak bahasa
    }

    private void startChallenge() {
        Intent intent = new Intent(this, TantanganActivity.class);
        startActivity(intent);
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        return prefs.getInt("user_id", -1) != -1;
    }

    private User getUserDataFromDatabase() {
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) return null;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT u." + DatabaseHelper.COLUMN_AVATAR_URL + ", s." + DatabaseHelper.COLUMN_LEVEL + ", s." +
                DatabaseHelper.COLUMN_POINTS + ", s." + DatabaseHelper.COLUMN_STREAK_DAYS +
                " FROM " + DatabaseHelper.TABLE_USERS + " u" +
                " JOIN " + DatabaseHelper.TABLE_USER_STATS + " s ON u." + DatabaseHelper.COLUMN_USER_ID + " = s." + DatabaseHelper.COLUMN_USER_ID +
                " WHERE u." + DatabaseHelper.COLUMN_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        User user = null;
        if (cursor.moveToFirst()) {
            String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AVATAR_URL));
            int level = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LEVEL));
            int points = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POINTS));
            int streak = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STREAK_DAYS));
            user = new User(avatarUrl, level, points, streak);
        }
        cursor.close();
        return user;
    }

    private Challenge getChallengeDataFromDatabase() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_END_TIME +
                " FROM " + DatabaseHelper.TABLE_DAILY_CHALLENGES +
                " WHERE " + DatabaseHelper.COLUMN_END_TIME + " > strftime('%s', 'now') LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        Challenge challenge = null;
        if (cursor.moveToFirst()) {
            long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_END_TIME)) * 1000;
            challenge = new Challenge(endTime);
        }
        cursor.close();
        return challenge;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private static class User {
        private String avatarUrl;
        private int level;
        private int points;
        private int streak;

        public User(String avatarUrl, int level, int points, int streak) {
            this.avatarUrl = avatarUrl;
            this.level = level;
            this.points = points;
            this.streak = streak;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public int getLevel() {
            return level;
        }

        public int getPoints() {
            return points;
        }

        public int getStreak() {
            return streak;
        }
    }

    private static class Challenge {
        private long endTime;

        public Challenge(long endTime) {
            this.endTime = endTime;
        }

        public long getEndTime() {
            return endTime;
        }
    }
}