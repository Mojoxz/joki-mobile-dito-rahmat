package com.example.lingoquest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private FirebaseHelper fbHelper;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Timer dailyTimer, weeklyTimer, realTimeTimer;
    private long dailyEndTime, weeklyEndTime;
    private String userId;
    private int dailyTarget = 10;
    private int weeklyTarget = 50;
    private int dailyProgress = 0;
    private int weeklyProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_tantangan);

        fbHelper = FirebaseHelper.getInstance();

        // Initialize Views
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

        userId = fbHelper.getCurrentUserId();
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        loadUserData();
        loadAchievements();
        loadDailyMission();
        loadWeeklyChallenge();
        loadChallengeModes();
        loadWeeklyRanking();

        setDailyTimer();
        setWeeklyTimer();
        setRealTimeTimer();

        btnSeeMore.setOnClickListener(v -> {
            Intent intent = new Intent(TantanganActivity.this, PeringkatActivity.class);
            startActivity(intent);
        });

        btnDailyMission.setOnClickListener(v -> {
            Intent intent = new Intent(TantanganActivity.this, BelajarActivity.class);
            startActivity(intent);
        });

        btnWeeklyChallenge.setOnClickListener(v -> {
            Intent intent = new Intent(TantanganActivity.this, BelajarActivity.class);
            startActivity(intent);
        });

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
                    return true;
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

        bottomNav.setSelectedItemId(R.id.nav_tantangan);
    }

    private void loadUserData() {
        fbHelper.getUserData(userId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                String username = (String) userData.get("username");
                String avatarUrl = (String) userData.get("avatar_url");

                tvUsername.setText(username != null ? username : "Pengguna");

                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(TantanganActivity.this)
                            .load(avatarUrl)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(ivProfile);
                } else {
                    ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
                }

                // Load level from user stats
                fbHelper.getUserStats(userId, new FirebaseHelper.UserDataCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> statsData) {
                        Long level = (Long) statsData.get("level");
                        tvLevel.setText("Level " + (level != null ? level : 0));
                    }

                    @Override
                    public void onFailure(String error) {
                        tvLevel.setText("Level 0");
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                ivProfile.setImageResource(android.R.drawable.ic_menu_gallery);
                tvUsername.setText("Pengguna");
                tvLevel.setText("Level 0");
            }
        });
    }

    private void loadAchievements() {
        fbHelper.getUserStats(userId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> statsData) {
                Long streak = (Long) statsData.get("streak_days");
                Long correctAnswers = (Long) statsData.get("correct_answers");
                Long level = (Long) statsData.get("level");
                Boolean isWeeklyChampion = (Boolean) statsData.get("is_weekly_champion");

                int streakDays = streak != null ? streak.intValue() : 0;
                int answers = correctAnswers != null ? correctAnswers.intValue() : 0;
                int userLevel = level != null ? level.intValue() : 0;
                boolean champion = isWeeklyChampion != null && isWeeklyChampion;

                llAchievement7Days.setAlpha(streakDays >= 7 ? 1.0f : 0.5f);
                llAchievement100Questions.setAlpha(answers >= 100 ? 1.0f : 0.5f);
                llAchievementLevel20.setAlpha(userLevel >= 20 ? 1.0f : 0.5f);
                llAchievementWeeklyChampion.setAlpha(champion ? 1.0f : 0.5f);
            }

            @Override
            public void onFailure(String error) {
                llAchievement7Days.setAlpha(0.5f);
                llAchievement100Questions.setAlpha(0.5f);
                llAchievementLevel20.setAlpha(0.5f);
                llAchievementWeeklyChampion.setAlpha(0.5f);
            }
        });
    }

    private void loadDailyMission() {
        // Implementation similar to original but using Firebase
        dailyProgress = 0;
        dailyEndTime = getDailyEndTime();

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
    }

    private void awardDailyXP() {
        fbHelper.recordXpGain(userId, 50, new FirebaseHelper.XpCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(TantanganActivity.this, "+50 XP dari Misi Harian!", Toast.LENGTH_SHORT).show();
                loadUserData();
            }

            @Override
            public void onFailure(String error) {
                Log.e("TantanganActivity", "Failed to award daily XP: " + error);
            }
        });
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
        dailyEndTime = getDailyEndTime();
        fbHelper.updateDailyMissionProgress(userId, 0);
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

    private void loadWeeklyChallenge() {
        weeklyProgress = 0;
        weeklyEndTime = getWeeklyEndTime();

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
    }

    private void awardWeeklyXP() {
        fbHelper.recordXpGain(userId, 200, new FirebaseHelper.XpCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(TantanganActivity.this, "+200 XP dari Tantangan Mingguan!", Toast.LENGTH_SHORT).show();
                loadUserData();
            }

            @Override
            public void onFailure(String error) {
                Log.e("TantanganActivity", "Failed to award weekly XP: " + error);
            }
        });
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
        weeklyEndTime = getWeeklyEndTime();
        fbHelper.updateWeeklyProgress(userId, 0);
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
            // Use a dedicated TextView if available
        });
    }

    private void loadChallengeModes() {
        // Challenge modes data would be loaded from Firebase
        tvSpeedRecord.setText("Rekor: 42 soal");
        tvSpeedQuestions.setText("0 soal dikerjakan");
        tvDuelStreak.setText("Rekor: 5 menang");
        tvDuelWins.setText("0 menang berturut");
        tvSurvivalLevel.setText("Level tertinggi: 0");
        tvStoryProgress.setText("0 cerita selesai");
    }

    private void loadWeeklyRanking() {
        fbHelper.getLeaderboard("Mingguan", 3, new FirebaseHelper.LeaderboardCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> leaderboard) {
                if (leaderboard.size() >= 1) {
                    Map<String, Object> rank1 = leaderboard.get(0);
                    String username1 = (String) rank1.get("username");
                    Long xp1 = (Long) rank1.get("total_xp");
                    String avatar1 = (String) rank1.get("avatar_url");

                    tvRank1.setText("1 " + username1 + ", " + (xp1 != null ? xp1 : 0) + " XP");
                    if (avatar1 != null && !avatar1.isEmpty()) {
                        Glide.with(TantanganActivity.this)
                                .load(avatar1)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .into(ivRank1Image);
                    }
                }

                if (leaderboard.size() >= 2) {
                    Map<String, Object> rank2 = leaderboard.get(1);
                    String username2 = (String) rank2.get("username");
                    Long xp2 = (Long) rank2.get("total_xp");
                    String avatar2 = (String) rank2.get("avatar_url");

                    tvRank2.setText("2 " + username2 + ", " + (xp2 != null ? xp2 : 0) + " XP");
                    if (avatar2 != null && !avatar2.isEmpty()) {
                        Glide.with(TantanganActivity.this)
                                .load(avatar2)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .into(ivRank2Image);
                    }
                }

                if (leaderboard.size() >= 3) {
                    Map<String, Object> rank3 = leaderboard.get(2);
                    String username3 = (String) rank3.get("username");
                    Long xp3 = (Long) rank3.get("total_xp");
                    String avatar3 = (String) rank3.get("avatar_url");

                    tvRank3.setText("3 " + username3 + ", " + (xp3 != null ? xp3 : 0) + " XP");
                    if (avatar3 != null && !avatar3.isEmpty()) {
                        Glide.with(TantanganActivity.this)
                                .load(avatar3)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .into(ivRank3Image);
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("TantanganActivity", "Failed to load leaderboard: " + error);
            }
        });
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
    }
}