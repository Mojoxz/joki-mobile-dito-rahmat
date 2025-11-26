package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private int userId;
    private Toolbar toolbar;
    private ImageButton btnBack;
    private ImageView ivAvatar, ivLanguageIcon;
    private TextView tvUsername, tvLevelDisplay, tvXp, tvLanguage;
    private ProgressBar progressLevel;
    private ViewPager2 viewPager;
    private GameAdapter gameAdapter;
    private String selectedLanguage;
    private int languageId;
    private int gameLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_game);

        db = new DatabaseHelper(this);

        selectedLanguage = getIntent().getStringExtra("language");
        if (selectedLanguage == null) {
            selectedLanguage = "Bahasa Inggris";
        }

        languageId = db.getLanguageId(selectedLanguage);
        if (languageId == -1) {
            languageId = 1;
        }

        gameLevel = getCurrentLevel();

        toolbar = findViewById(R.id.toolbar);
        btnBack = findViewById(R.id.btnBack);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvUsername = findViewById(R.id.tvUsername);
        tvLevelDisplay = findViewById(R.id.tvLevelDisplay);
        progressLevel = findViewById(R.id.progressLevel);
        tvXp = findViewById(R.id.tvXp);
        ivLanguageIcon = findViewById(R.id.ivLanguageIcon);
        tvLanguage = findViewById(R.id.tvLanguage);
        viewPager = findViewById(R.id.viewPager);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        btnBack.setOnClickListener(v -> finish());

        loadUserData();
        updateGameProgress();

        tvLanguage.setText(selectedLanguage);
        loadLanguageIcon();

        List<String> languages = new ArrayList<>();
        languages.add(selectedLanguage);
        gameAdapter = new GameAdapter(this, languages, this::onQuestionAnswered);
        viewPager.setAdapter(gameAdapter);
        viewPager.setCurrentItem(0, false);
        viewPager.setUserInputEnabled(false);
    }

    private void loadUserData() {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor userCursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_USERNAME + ", " + DatabaseHelper.COLUMN_AVATAR_URL +
                        " FROM " + DatabaseHelper.TABLE_USERS + " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        if (userCursor.moveToFirst()) {
            String username = userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME));
            String avatarUrl = userCursor.getString(userCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AVATAR_URL));
            tvUsername.setText(username);
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(this).load(avatarUrl).placeholder(R.drawable.default_avatar).error(R.drawable.default_avatar).into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            ivAvatar.setImageResource(R.drawable.default_avatar);
            tvUsername.setText("Pengguna");
        }
        userCursor.close();
        readableDb.close();
    }

    private void loadLanguageIcon() {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor iconCursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_ICON_URL + " FROM " + DatabaseHelper.TABLE_LANGUAGES +
                        " WHERE " + DatabaseHelper.COLUMN_LANGUAGE_NAME + " = ?",
                new String[]{selectedLanguage});
        if (iconCursor.moveToFirst()) {
            String iconUrl = iconCursor.getString(iconCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ICON_URL));
            if (iconUrl != null && !iconUrl.isEmpty()) {
                Glide.with(this).load(iconUrl).placeholder(R.drawable.bahasa).error(R.drawable.bahasa).into(ivLanguageIcon);
            } else {
                ivLanguageIcon.setImageResource(R.drawable.bahasa);
            }
        } else {
            ivLanguageIcon.setImageResource(R.drawable.bahasa);
        }
        iconCursor.close();
        readableDb.close();
    }

    private void updateGameProgress() {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor progressCursor = readableDb.rawQuery(
                "SELECT ugp." + DatabaseHelper.COLUMN_CURRENT_LEVEL + ", ugp." + DatabaseHelper.COLUMN_TOTAL_XP +
                        ", us." + DatabaseHelper.COLUMN_POINTS +
                        " FROM " + DatabaseHelper.TABLE_USER_GAME_PROGRESS + " ugp" +
                        " JOIN " + DatabaseHelper.TABLE_USER_STATS + " us ON ugp." + DatabaseHelper.COLUMN_USER_ID + " = us." + DatabaseHelper.COLUMN_USER_ID +
                        " WHERE ugp." + DatabaseHelper.COLUMN_USER_ID + " = ? AND ugp." + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(languageId)});
        int totalXp = 0;
        int points = 0;
        if (progressCursor.moveToFirst()) {
            gameLevel = progressCursor.getInt(progressCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CURRENT_LEVEL));
            totalXp = progressCursor.getInt(progressCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_XP));
            points = progressCursor.getInt(progressCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_POINTS));
        }
        progressCursor.close();
        readableDb.close();

        int maxGameLevel = 10;
        int progress = (gameLevel * 100) / maxGameLevel;
        tvLevelDisplay.setText("Level " + gameLevel);
        progressLevel.setProgress(progress);
        tvXp.setText(points + " XP");
    }

    private void onQuestionAnswered(boolean isCorrect) {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor questionCursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_XP_REWARD +
                        " FROM " + DatabaseHelper.TABLE_GAME_QUESTIONS +
                        " WHERE " + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ? AND " + DatabaseHelper.COLUMN_QUESTION_LEVEL + " <= ? LIMIT 1",
                new String[]{String.valueOf(languageId), String.valueOf(gameLevel)});
        int xpReward = 10;
        if (questionCursor.moveToFirst()) {
            xpReward = questionCursor.getInt(questionCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_XP_REWARD));
        }
        questionCursor.close();
        readableDb.close();

        if (isCorrect) {
            gameLevel++;
            updateUserProgress(xpReward);
        }

        updateGameProgress();
    }

    private void updateUserProgress(int xpEarned) {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor progressCursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_CURRENT_LEVEL + ", " + DatabaseHelper.COLUMN_TOTAL_XP +
                        " FROM " + DatabaseHelper.TABLE_USER_GAME_PROGRESS +
                        " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(languageId)});
        int currentLevel = 1;
        int totalXp = 0;
        if (progressCursor.moveToFirst()) {
            currentLevel = progressCursor.getInt(progressCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CURRENT_LEVEL));
            totalXp = progressCursor.getInt(progressCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOTAL_XP));
        }
        progressCursor.close();
        readableDb.close();

        totalXp += xpEarned;
        int maxXpPerLevel = 100;
        if (totalXp >= currentLevel * maxXpPerLevel) {
            currentLevel = totalXp / maxXpPerLevel + 1;
        }

        if (gameLevel > currentLevel) {
            currentLevel = gameLevel;
        }

        db.updateUserGameProgress(userId, languageId, currentLevel, totalXp);

        SQLiteDatabase writableDb = db.getWritableDatabase();
        writableDb.execSQL(
                "UPDATE user_stats SET points = points + ? WHERE user_id = ?",
                new Object[]{xpEarned, userId}
        );
        writableDb.close();
    }

    private int getCurrentLevel() {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor cursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_CURRENT_LEVEL +
                        " FROM " + DatabaseHelper.TABLE_USER_GAME_PROGRESS +
                        " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ? AND " + DatabaseHelper.COLUMN_LANGUAGE_ID + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(languageId)});
        int level = 1;
        if (cursor.moveToFirst()) {
            level = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CURRENT_LEVEL));
        }
        cursor.close();
        readableDb.close();
        return level;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }
}