package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private FirebaseHelper fbHelper;
    private String userId;
    private Toolbar toolbar;
    private ImageButton btnBack;
    private ImageView ivAvatar, ivLanguageIcon;
    private TextView tvUsername, tvLevelDisplay, tvXp, tvLanguage;
    private ProgressBar progressLevel;
    private ViewPager2 viewPager;
    private GameAdapter gameAdapter;
    private String selectedLanguage;
    private String languageId;
    private int gameLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        fbHelper = FirebaseHelper.getInstance();
        userId = fbHelper.getCurrentUserId();

        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_game);

        selectedLanguage = getIntent().getStringExtra("language");
        if (selectedLanguage == null) {
            selectedLanguage = "Bahasa Inggris";
        }

        // Get language ID from Firebase
        fbHelper.getLanguageId(selectedLanguage, langId -> {
            languageId = langId;
            if (languageId == null) {
                languageId = "default_lang_id";
            }
            loadGameLevel();
        });

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

    private void loadGameLevel() {
        fbHelper.getUserGameProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                Long level = (Long) userData.get("current_level");
                gameLevel = level != null ? level.intValue() : 1;
                updateGameProgress();
            }

            @Override
            public void onFailure(String error) {
                gameLevel = 1;
                updateGameProgress();
            }
        });
    }

    private void loadUserData() {
        fbHelper.getUserData(userId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                String username = (String) userData.get("username");
                String avatarUrl = (String) userData.get("avatar_url");

                tvUsername.setText(username != null ? username : "Pengguna");

                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(GameActivity.this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar)
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.default_avatar);
                }
            }

            @Override
            public void onFailure(String error) {
                ivAvatar.setImageResource(R.drawable.default_avatar);
                tvUsername.setText("Pengguna");
            }
        });
    }

    private void loadLanguageIcon() {
        // Language icons can be stored in Firebase Storage or as URLs in Firestore
        // For now, using default icon
        ivLanguageIcon.setImageResource(R.drawable.bahasa);
    }

    private void updateGameProgress() {
        fbHelper.getUserGameProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> progressData) {
                Long level = (Long) progressData.get("current_level");
                Long totalXp = (Long) progressData.get("total_xp");

                gameLevel = level != null ? level.intValue() : 1;
                int xp = totalXp != null ? totalXp.intValue() : 0;

                // Get user stats for points
                fbHelper.getUserStats(userId, new FirebaseHelper.UserDataCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> statsData) {
                        Long points = (Long) statsData.get("points");
                        int totalPoints = points != null ? points.intValue() : 0;

                        int maxGameLevel = 10;
                        int progress = (gameLevel * 100) / maxGameLevel;
                        tvLevelDisplay.setText("Level " + gameLevel);
                        progressLevel.setProgress(progress);
                        tvXp.setText(totalPoints + " XP");
                    }

                    @Override
                    public void onFailure(String error) {
                        tvXp.setText("0 XP");
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                gameLevel = 1;
                tvLevelDisplay.setText("Level 1");
                progressLevel.setProgress(10);
                tvXp.setText("0 XP");
            }
        });
    }

    private void onQuestionAnswered(boolean isCorrect) {
        if (isCorrect) {
            int xpReward = 10;
            gameLevel++;
            updateUserProgress(xpReward);
        }
        updateGameProgress();
    }

    private void updateUserProgress(int xpEarned) {
        fbHelper.getUserGameProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> progressData) {
                Long currentLevelLong = (Long) progressData.get("current_level");
                Long totalXpLong = (Long) progressData.get("total_xp");

                int currentLevel = currentLevelLong != null ? currentLevelLong.intValue() : 1;
                int totalXp = totalXpLong != null ? totalXpLong.intValue() : 0;

                totalXp += xpEarned;
                int maxXpPerLevel = 100;
                if (totalXp >= currentLevel * maxXpPerLevel) {
                    currentLevel = totalXp / maxXpPerLevel + 1;
                }

                if (gameLevel > currentLevel) {
                    currentLevel = gameLevel;
                }

                int finalLevel = currentLevel;
                int finalXp = totalXp;

                // Update game progress
                fbHelper.updateUserGameProgress(userId, languageId, finalLevel, finalXp,
                        new FirebaseHelper.XpCallback() {
                            @Override
                            public void onSuccess() {
                                // Record XP gain
                                fbHelper.recordXpGain(userId, xpEarned, null);
                            }

                            @Override
                            public void onFailure(String error) {
                                // Handle error
                            }
                        });
            }

            @Override
            public void onFailure(String error) {
                // Handle error
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}