package com.example.lingoquest;

import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private FirebaseHelper firebaseHelper;
    private LinearLayout llLanguageList;
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

        setContentView(R.layout.activity_belajar);

        userId = firebaseHelper.getCurrentUserId();

        ivAvatar = findViewById(R.id.avatar);
        tvTimer = findViewById(R.id.timer);
        btnStartChallenge = findViewById(R.id.btn_start_challenge);
        nestedScrollView = findViewById(R.id.nested_scroll_view);
        llLanguageList = findViewById(R.id.language_list);

        loadUserData();
        setChallengeTimer();
        loadLearnedLanguages();

        setupLanguageClickListeners();

        findViewById(R.id.layout_continue_learning).setOnClickListener(v -> showContinueLearningDialog());
        findViewById(R.id.layout_new_practice).setOnClickListener(v -> showNewPracticeDialog());

        // TAMBAHAN BARU: Listener untuk Listening dan Reading
        findViewById(R.id.layout_listening_practice).setOnClickListener(v -> showListeningDialog());
        findViewById(R.id.layout_reading_practice).setOnClickListener(v -> showReadingDialog());

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

    // TAMBAHAN BARU: Method untuk Listening Dialog
    private void showListeningDialog() {
        String[] languages = {"Bahasa Inggris", "Bahasa Jepang", "Bahasa Korea", "Bahasa Mandarin"};

        new AlertDialog.Builder(this)
                .setTitle("Pilih Bahasa - Listening")
                .setMessage("Pilih bahasa yang ingin Anda pelajari dengan latihan listening")
                .setItems(languages, (dialog, which) -> {
                    String selectedLanguage = languages[which];
                    startListeningActivity(selectedLanguage);
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // TAMBAHAN BARU: Method untuk Reading Dialog
    private void showReadingDialog() {
        String[] languages = {"Bahasa Inggris", "Bahasa Jepang", "Bahasa Korea", "Bahasa Mandarin"};

        new AlertDialog.Builder(this)
                .setTitle("Pilih Bahasa - Reading")
                .setMessage("Pilih bahasa yang ingin Anda pelajari dengan latihan reading")
                .setItems(languages, (dialog, which) -> {
                    String selectedLanguage = languages[which];
                    startReadingActivity(selectedLanguage);
                })
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // TAMBAHAN BARU: Method untuk start Listening Activity
    private void startListeningActivity(String language) {
        Intent intent = new Intent(this, ListeningActivity.class);
        intent.putExtra("language", language);
        startActivity(intent);
    }

    // TAMBAHAN BARU: Method untuk start Reading Activity
    private void startReadingActivity(String language) {
        Intent intent = new Intent(this, ReadingActivity.class);
        intent.putExtra("language", language);
        startActivity(intent);
    }

    private void getLanguageLevel(String languageName, LanguageLevelCallback callback) {
        if (userId == null) {
            callback.onResult(0);
            return;
        }

        firebaseHelper.getLanguageId(languageName, languageId -> {
            if (languageId == null) {
                callback.onResult(0);
                return;
            }

            firebaseHelper.getUserGameProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
                @Override
                public void onSuccess(Map<String, Object> userData) {
                    Long level = (Long) userData.get("current_level");
                    callback.onResult(level != null ? level.intValue() : 0);
                }

                @Override
                public void onFailure(String error) {
                    callback.onResult(0);
                }
            });
        });
    }

    interface LanguageLevelCallback {
        void onResult(int level);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLearnedLanguages();
    }

    private void loadUserData() {
        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        firebaseHelper.getUserData(userId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                String avatarUrl = (String) userData.get("avatar_url");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(BelajarActivity.this)
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
    }

    private void setChallengeTimer() {
        // Menggunakan data dari weekly challenges
        if (userId != null) {
            firebaseHelper.getInstance().getClass();
            // Untuk sementara set timer default, Anda bisa sesuaikan dengan Firebase collection
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
        if (userId == null) return;

        String[] languages = {"Bahasa Inggris", "Bahasa Jepang", "Bahasa Korea", "Bahasa Mandarin"};

        for (String languageName : languages) {
            firebaseHelper.getLanguageId(languageName, languageId -> {
                if (languageId != null) {
                    firebaseHelper.getUserGameProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
                        @Override
                        public void onSuccess(Map<String, Object> userData) {
                            Long levelLong = (Long) userData.get("current_level");
                            Long totalXpLong = (Long) userData.get("total_xp");

                            int level = levelLong != null ? levelLong.intValue() : 0;
                            int totalXp = totalXpLong != null ? totalXpLong.intValue() : 0;

                            int maxLevel = 10;
                            int progress = (level * 100) / maxLevel;

                            runOnUiThread(() -> updateLanguageUI(languageName, level, progress, totalXp));
                        }

                        @Override
                        public void onFailure(String error) {
                            // Language not started yet
                            runOnUiThread(() -> updateLanguageUI(languageName, 0, 0, 0));
                        }
                    });
                }
            });
        }
    }

    private void getTotalQuestionsForLanguage(String languageName, TotalQuestionsCallback callback) {
        firebaseHelper.getLanguageId(languageName, languageId -> {
            if (languageId == null) {
                callback.onResult(10);
                return;
            }

            // Get all questions for this language across all levels
            firebaseHelper.getGameQuestions(languageId, 1, new FirebaseHelper.QuestionsCallback() {
                @Override
                public void onSuccess(List<Map<String, Object>> questions) {
                    callback.onResult(questions.size() > 0 ? questions.size() * 10 : 10); // Assuming 10 levels
                }

                @Override
                public void onFailure(String error) {
                    callback.onResult(10);
                }
            });
        });
    }

    interface TotalQuestionsCallback {
        void onResult(int total);
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

        getTotalQuestionsForLanguage(languageName, totalQuestions -> {
            int completedQuestions = totalXp / 10;
            int progressPercentage = (totalQuestions > 0) ? (completedQuestions * 100) / totalQuestions : 0;

            if (progressPercentage > 100) progressPercentage = 100;

            int finalProgress = progressPercentage;
            runOnUiThread(() -> {
                if (level == 0) {
                    tvLevel.setText("Belum Mulai");
                    progressBar.setProgress(0);
                    tvProgress.setText("0 XP");
                } else {
                    tvLevel.setText("Level " + level);
                    progressBar.setProgress(finalProgress);
                    tvProgress.setText(totalXp + " XP");
                }
            });
        });
    }

    private void showContinueLearningDialog() {
        getLearnedLanguages(learnedLanguages -> {
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
        });
    }

    private void showNewPracticeDialog() {
        getUnlearnedLanguages(unlearnedLanguages -> {
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
        });
    }

    private void getLearnedLanguages(LanguageListCallback callback) {
        List<String> learnedLanguages = new ArrayList<>();
        if (userId == null) {
            callback.onResult(learnedLanguages);
            return;
        }

        String[] allLanguages = {"Bahasa Inggris", "Bahasa Jepang", "Bahasa Korea", "Bahasa Mandarin"};
        final int[] checkCount = {0};

        for (String languageName : allLanguages) {
            firebaseHelper.getLanguageId(languageName, languageId -> {
                if (languageId != null) {
                    firebaseHelper.getUserGameProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
                        @Override
                        public void onSuccess(Map<String, Object> userData) {
                            learnedLanguages.add(languageName);
                            checkCount[0]++;
                            if (checkCount[0] == allLanguages.length) {
                                callback.onResult(learnedLanguages);
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            checkCount[0]++;
                            if (checkCount[0] == allLanguages.length) {
                                callback.onResult(learnedLanguages);
                            }
                        }
                    });
                } else {
                    checkCount[0]++;
                    if (checkCount[0] == allLanguages.length) {
                        callback.onResult(learnedLanguages);
                    }
                }
            });
        }
    }

    private void getUnlearnedLanguages(LanguageListCallback callback) {
        getLearnedLanguages(learnedLanguages -> {
            List<String> allLanguages = List.of("Bahasa Inggris", "Bahasa Jepang", "Bahasa Korea", "Bahasa Mandarin");
            List<String> unlearnedLanguages = new ArrayList<>();

            for (String language : allLanguages) {
                if (!learnedLanguages.contains(language)) {
                    unlearnedLanguages.add(language);
                }
            }
            callback.onResult(unlearnedLanguages);
        });
    }

    interface LanguageListCallback {
        void onResult(List<String> languages);
    }

    private void addLanguageToUser(String languageName) {
        if (userId == null) return;

        firebaseHelper.getLanguageId(languageName, languageId -> {
            if (languageId != null) {
                // Initialize game progress for this language
                firebaseHelper.updateUserGameProgress(userId, languageId, 1, 0, new FirebaseHelper.XpCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> loadLearnedLanguages());
                    }

                    @Override
                    public void onFailure(String error) {
                        // Handle error
                    }
                });
            }
        });
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
        return firebaseHelper.isUserLoggedIn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class Challenge {
        private long endTime;

        public Challenge(long endTime) {
            this.endTime = endTime;
        }

        public long getEndTime() { return endTime; }
    }
}