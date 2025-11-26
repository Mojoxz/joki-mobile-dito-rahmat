package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import com.bumptech.glide.Glide;
import androidx.core.view.WindowCompat;
import android.view.WindowManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Map;
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
    private FirebaseHelper firebaseHelper;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseHelper = FirebaseHelper.getInstance();

        if (!isUserLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);

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
        if (userId == null) return;

        // Load user data dari Firebase
        firebaseHelper.getUserData(userId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                String avatarUrl = (String) userData.get("avatar_url");

                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(MainActivity.this)
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
            }
        });

        // Load user stats dari Firebase
        firebaseHelper.getUserStats(userId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> stats) {
                Long level = (Long) stats.get("level");
                Long points = (Long) stats.get("points");
                Long streakDays = (Long) stats.get("streak_days");

                tvLevel.setText(String.valueOf(level != null ? level : 1));
                tvPoints.setText(String.valueOf(points != null ? points : 0));
                tvStreak.setText((streakDays != null ? streakDays : 0) + " Hari");
            }

            @Override
            public void onFailure(String error) {
                tvLevel.setText("1");
                tvPoints.setText("0");
                tvStreak.setText("0 Hari");
            }
        });
    }

    private void setChallengeTimer() {
        if (userId == null) return;

        // Load daily mission data dari Firebase
        firebaseHelper.getUserData(userId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                // Get daily mission untuk timer
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("daily_missions")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                Long endTime = document.getLong("end_time");
                                if (endTime != null) {
                                    challengeEndTime = endTime;
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
                            } else {
                                tvTimer.setText("⏰ Tidak ada tantangan aktif");
                            }
                        })
                        .addOnFailureListener(e -> {
                            tvTimer.setText("⏰ Tidak ada tantangan aktif");
                        });
            }

            @Override
            public void onFailure(String error) {
                tvTimer.setText("⏰ Tidak ada tantangan aktif");
            }
        });
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
        String userId = prefs.getString("user_id", null);
        return userId != null && firebaseHelper.isUserLoggedIn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}