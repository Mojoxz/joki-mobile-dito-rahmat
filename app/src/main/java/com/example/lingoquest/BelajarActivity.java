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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import com.bumptech.glide.Glide;
import androidx.core.view.WindowCompat;
import android.view.WindowManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class BelajarActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvTimer;
    private Button btnStartChallenge;
    private NestedScrollView nestedScrollView;
    private Timer timer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long challengeEndTime;
    private DatabaseHelper dbHelper;
    private LinearLayout llLanguageList;

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

        setContentView(R.layout.activity_belajar);

        dbHelper = new DatabaseHelper(this);

        ivAvatar = findViewById(R.id.avatar);
        tvTimer = findViewById(R.id.timer);
        btnStartChallenge = findViewById(R.id.btn_start_challenge);
        nestedScrollView = findViewById(R.id.nested_scroll_view);
        llLanguageList = findViewById(R.id.language_list);

        loadUserData();
        setChallengeTimer();
        loadLearnedLanguages();

        // Atur listener untuk layout bahasa
        setupLanguageClickListeners();

        findViewById(R.id.layout_continue_learning).setOnClickListener(v -> showContinueLearningDialog());
        findViewById(R.id.layout_new_practice).setOnClickListener(v -> showNewPracticeDialog());
        findViewById(R.id.see_more_languages).setOnClickListener(v -> {
            nestedScrollView.smoothScrollTo(0, llLanguageList.getBottom());
            loadMoreLanguages();
        });

        btnStartChallenge.setOnClickListener(v -> startChallenge());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_belajar);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            NavigationItem navItem = NavigationItem.fromItemId(item.getItemId());
            if (navItem == null) return false;
            switch (navItem) {
                case NAV_HOME:
                    startActivity(new Intent(BelajarActivity.this, MainActivity.class));
                    return true;
                case NAV_BELAJAR:
                    return true;
                case NAV_TANTANGAN:
                    startActivity(new Intent(BelajarActivity.this, TantanganActivity.class));
                    return true;
                case NAV_PERINGKAT:
                    startActivity(new Intent(BelajarActivity.this, PeringkatActivity.class));
                    return true;
                case NAV_PROFIL:
                    startActivity(new Intent(BelajarActivity.this, ProfilActivity.class));
                    return true;
                default:
                    return false;
            }
        });
    }

    private void setupLanguageClickListeners() {
        String[] languages = {"Bahasa Inggris", "Bahasa Jepang", "Bahasa Korea", "Bahasa Mandarin"};
        int[] layoutIds = {R.id.layout_english, R.id.layout_japanese, R.id.layout_korean, R.id.layout_mandarin};

        for (int i = 0; i < languages.length; i++) {
            final String languageName = languages[i];
            findViewById(layoutIds[i]).setOnClickListener(v -> startGameActivity(languageName));
        }
    }

    // Tambahkan metode untuk mendapatkan level bahasa
    private int getLanguageLevel(String languageName) {
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) return 0;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT ugp." + DatabaseHelper.COLUMN_CURRENT_LEVEL +
                " FROM " + DatabaseHelper.TABLE_LANGUAGES + " l" +
                " JOIN " + DatabaseHelper.TABLE_USER_LANGUAGES + " ul ON l." + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ul." + DatabaseHelper.COLUMN_LANGUAGE_ID +
                " LEFT JOIN " + DatabaseHelper.TABLE_USER_GAME_PROGRESS + " ugp ON ul." + DatabaseHelper.COLUMN_USER_ID + " = ugp." + DatabaseHelper.COLUMN_USER_ID +
                " AND ul." + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ugp." + DatabaseHelper.COLUMN_LANGUAGE_ID +
                " WHERE ul." + DatabaseHelper.COLUMN_USER_ID + " = ? AND l." + DatabaseHelper.COLUMN_LANGUAGE_NAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), languageName});

        int level = 0;
        if (cursor.moveToFirst()) {
            level = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CURRENT_LEVEL));
        }
        cursor.close();
        return level;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLearnedLanguages();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT " + DatabaseHelper.COLUMN_AVATAR_URL +
                " FROM " + DatabaseHelper.TABLE_USERS +
                " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            String avatarUrl = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AVATAR_URL));
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            ivAvatar.setImageResource(R.drawable.default_avatar);
        }
        cursor.close();
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

    private void loadLearnedLanguages() {
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) return;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT l." + DatabaseHelper.COLUMN_LANGUAGE_NAME +
                ", ugp." + DatabaseHelper.COLUMN_CURRENT_LEVEL + ", ugp." + DatabaseHelper.COLUMN_TOTAL_XP +
                " FROM " + DatabaseHelper.TABLE_LANGUAGES + " l" +
                " JOIN " + DatabaseHelper.TABLE_USER_LANGUAGES + " ul ON l." + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ul." + DatabaseHelper.COLUMN_LANGUAGE_ID +
                " LEFT JOIN " + DatabaseHelper.TABLE_USER_GAME_PROGRESS + " ugp ON ul." + DatabaseHelper.COLUMN_USER_ID + " = ugp." + DatabaseHelper.COLUMN_USER_ID +
                " AND ul." + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ugp." + DatabaseHelper.COLUMN_LANGUAGE_ID +
                " WHERE ul." + DatabaseHelper.COLUMN_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        while (cursor.moveToNext()) {
            String languageName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE_NAME));
            int level = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CURRENT_LEVEL));
            int totalXp = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_XP));

            int maxLevel = 10;
            int progress = (level * 100) / maxLevel;

            updateLanguageUI(languageName, level, progress, totalXp);
        }
        cursor.close();
    }

    private int getTotalQuestionsForLanguage(String languageName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int languageId = dbHelper.getLanguageId(languageName);
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_GAME_QUESTIONS +
                " WHERE " + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(languageId)});
        int totalQuestions = 0;
        if (cursor.moveToFirst()) {
            totalQuestions = cursor.getInt(0);
        }
        cursor.close();
        return totalQuestions > 0 ? totalQuestions : 10; // Default 10 jika tidak ada data
    }

    private void updateLanguageUI(String languageName, int level, int progress, int totalXp) {
        int layoutId;
        int progressBarId;
        int levelTextId;
        int progressTextId;

        switch (languageName) {
            case "Bahasa Inggris":
                layoutId = R.id.layout_english;
                progressBarId = R.id.progress_english;
                levelTextId = R.id.level_english;
                progressTextId = R.id.progress_text_english;
                break;
            case "Bahasa Jepang":
                layoutId = R.id.layout_japanese;
                progressBarId = R.id.progress_japanese;
                levelTextId = R.id.level_japanese;
                progressTextId = R.id.progress_text_japanese;
                break;
            case "Bahasa Korea":
                layoutId = R.id.layout_korean;
                progressBarId = R.id.progress_korean;
                levelTextId = R.id.level_korean;
                progressTextId = R.id.progress_text_korean;
                break;
            case "Bahasa Mandarin":
                layoutId = R.id.layout_mandarin;
                progressBarId = R.id.progress_mandarin;
                levelTextId = R.id.level_mandarin;
                progressTextId = R.id.progress_text_mandarin;
                break;
            default:
                return;
        }

        TextView tvLevel = findViewById(levelTextId);
        ProgressBar progressBar = findViewById(progressBarId);
        TextView tvProgress = findViewById(progressTextId);
        LinearLayout layout = findViewById(layoutId);

        // Hitung total soal untuk bahasa tertentu
        int totalQuestions = getTotalQuestionsForLanguage(languageName);
        int completedQuestions = totalXp / 10; // Asumsi setiap soal memberikan 10 XP
        int progressPercentage = (totalQuestions > 0) ? (completedQuestions * 100) / totalQuestions : 0;

        // Batasi progress agar tidak melebihi 100%
        if (progressPercentage > 100) progressPercentage = 100;

        if (level == 0) {
            tvLevel.setText("Belum Mulai");
            progressBar.setProgress(0);
            tvProgress.setText("0 XP");
        } else {
            tvLevel.setText("Level " + level);
            progressBar.setProgress(progressPercentage);
            tvProgress.setText(totalXp + " XP");
        }
    }
    private void showContinueLearningDialog() {
        List<String> learnedLanguages = getLearnedLanguages();
        if (learnedLanguages.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Lanjutkan Belajar")
                    .setMessage("Anda belum mempelajari bahasa apa pun. Mulai latihan baru terlebih dahulu!")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        String[] languagesArray = learnedLanguages.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Lanjutkan Belajar")
                .setItems(languagesArray, (dialog, which) -> {
                    String selectedLanguage = languagesArray[which];
                    startGameActivity(selectedLanguage);
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showNewPracticeDialog() {
        List<String> unlearnedLanguages = getUnlearnedLanguages();
        if (unlearnedLanguages.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Latihan Baru")
                    .setMessage("Anda sudah mempelajari semua bahasa yang tersedia!")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        String[] languagesArray = unlearnedLanguages.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Latihan Baru")
                .setItems(languagesArray, (dialog, which) -> {
                    String selectedLanguage = languagesArray[which];
                    addLanguageToUser(selectedLanguage);
                    startGameActivity(selectedLanguage);
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private List<String> getLearnedLanguages() {
        List<String> learnedLanguages = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) return learnedLanguages;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT l." + DatabaseHelper.COLUMN_LANGUAGE_NAME +
                " FROM " + DatabaseHelper.TABLE_LANGUAGES + " l" +
                " JOIN " + DatabaseHelper.TABLE_USER_LANGUAGES + " ul ON l." + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ul." + DatabaseHelper.COLUMN_LANGUAGE_ID +
                " WHERE ul." + DatabaseHelper.COLUMN_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        while (cursor.moveToNext()) {
            String languageName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE_NAME));
            learnedLanguages.add(languageName);
        }
        cursor.close();
        return learnedLanguages;
    }

    private List<String> getUnlearnedLanguages() {
        List<String> allLanguages = new ArrayList<>();
        List<String> learnedLanguages = getLearnedLanguages();
        List<String> unlearnedLanguages = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_LANGUAGE_NAME + " FROM " + DatabaseHelper.TABLE_LANGUAGES, null);
        while (cursor.moveToNext()) {
            String languageName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE_NAME));
            allLanguages.add(languageName);
        }
        cursor.close();

        for (String language : allLanguages) {
            if (!learnedLanguages.contains(language)) {
                unlearnedLanguages.add(language);
            }
        }
        return unlearnedLanguages;
    }

    private void addLanguageToUser(String languageName) {
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        if (userId == -1) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int languageId = dbHelper.getLanguageId(languageName);
        if (languageId != -1) {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_USER_ID, userId);
            values.put(DatabaseHelper.COLUMN_LANGUAGE_ID, languageId);
            values.put(DatabaseHelper.COLUMN_PROGRESS, 0);
            db.insert(DatabaseHelper.TABLE_USER_LANGUAGES, null, values);

            ContentValues progressValues = new ContentValues();
            progressValues.put(DatabaseHelper.COLUMN_USER_ID, userId);
            progressValues.put(DatabaseHelper.COLUMN_LANGUAGE_ID, languageId);
            progressValues.put(DatabaseHelper.COLUMN_CURRENT_LEVEL, 1);
            progressValues.put(DatabaseHelper.COLUMN_TOTAL_XP, 0);
            db.insert(DatabaseHelper.TABLE_USER_GAME_PROGRESS, null, progressValues);
        }
    }

    private void startGameActivity(String language) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("language", language);
        startActivity(intent);
    }

    private void loadMoreLanguages() {
        // Implementasi untuk menampilkan lebih banyak bahasa (opsional)
    }

    private void startChallenge() {
        Intent intent = new Intent(this, TantanganActivity.class);
        startActivity(intent);
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        return prefs.getInt("user_id", -1) != -1;
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

        public String getAvatarUrl() { return avatarUrl; }
        public int getLevel() { return level; }
        public int getPoints() { return points; }
        public int getStreak() { return streak; }
    }

    private static class Challenge {
        private long endTime;

        public Challenge(long endTime) {
            this.endTime = endTime;
        }

        public long getEndTime() { return endTime; }
    }
}