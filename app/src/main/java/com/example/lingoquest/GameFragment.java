package com.example.lingoquest;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GameFragment extends Fragment {

    private TextView tvQuestion, tvLevel, tvProgress, tvCoins, tvSelectedAnswer, tvDailyMission, tvLearningMission;
    private Button btnOption1, btnOption2, btnOption3, btnOption4;
    private ImageView ivImage;
    private ProgressBar progressDailyMission, progressLearning;
    private FirebaseHelper fbHelper;
    private String userId, languageId;
    private String targetLanguage;
    private int wordsCompleted = 0;
    private int weeklyCompleted = 0;
    private int timeSpent = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private OnQuestionAnsweredListener listener;
    private List<Map<String, Object>> questionsList = new ArrayList<>();
    private int currentQuestionIndex = 0;

    public interface OnQuestionAnsweredListener {
        void onQuestionAnswered(boolean isCorrect);
    }

    public GameFragment() {
    }

    public static GameFragment newInstance(String language, OnQuestionAnsweredListener listener) {
        GameFragment fragment = new GameFragment();
        Bundle args = new Bundle();
        args.putString("language", language);
        fragment.setArguments(args);
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fbHelper = FirebaseHelper.getInstance();

        if (getArguments() != null) {
            targetLanguage = getArguments().getString("language");
        }

        userId = fbHelper.getCurrentUserId();
        if (userId == null) {
            return;
        }

        fbHelper.getLanguageId(targetLanguage, langId -> {
            languageId = langId != null ? langId : "default_lang_id";
            loadProgress();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_game, container, false);

        tvQuestion = view.findViewById(R.id.tvQuestion);
        tvLevel = view.findViewById(R.id.tvLevel);
        tvProgress = view.findViewById(R.id.tvProgress);
        tvCoins = view.findViewById(R.id.tvCoins);
        tvSelectedAnswer = view.findViewById(R.id.tvSelectedAnswer);
        btnOption1 = view.findViewById(R.id.btnOption1);
        btnOption2 = view.findViewById(R.id.btnOption2);
        btnOption3 = view.findViewById(R.id.btnOption3);
        btnOption4 = view.findViewById(R.id.btnOption4);
        ivImage = view.findViewById(R.id.ivImage);
        progressDailyMission = view.findViewById(R.id.progressDailyMission);
        progressLearning = view.findViewById(R.id.progressLearning);
        tvDailyMission = view.findViewById(R.id.tvDailyMission);
        tvLearningMission = view.findViewById(R.id.tvLearningMission);

        loadInitialData();
        startLearningTimer();
        setupButtons();

        return view;
    }

    private void loadInitialData() {
        getCurrentLevel(currentLevel -> {
            tvLevel.setText("Level " + currentLevel + ": Menerjemahkan Kata");

            getTotalXp(totalXp -> {
                fbHelper.getUserStats(userId, new FirebaseHelper.UserDataCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> statsData) {
                        Long points = (Long) statsData.get("points");
                        int coins = points != null ? points.intValue() : 0;
                        tvCoins.setText("💰 " + coins);
                    }

                    @Override
                    public void onFailure(String error) {
                        tvCoins.setText("💰 0");
                    }
                });
            });

            tvProgress.setText(wordsCompleted + "/5");
            tvDailyMission.setText("Selesaikan 5 Kata");
            tvLearningMission.setText("Belajar 10 Menit (" + (timeSpent / 60) + "/10 Menit)");

            progressDailyMission.setProgress(wordsCompleted);
            progressLearning.setProgress(timeSpent / 6);

            loadQuestion(currentLevel);
        });
    }

    private void getTotalXp(XpLoadCallback callback) {
        fbHelper.getUserGameProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> progressData) {
                Long totalXp = (Long) progressData.get("total_xp");
                callback.onXpLoaded(totalXp != null ? totalXp.intValue() : 0);
            }

            @Override
            public void onFailure(String error) {
                callback.onXpLoaded(0);
            }
        });
    }

    private void loadQuestion(int level) {
        fbHelper.getGameQuestions(languageId, level, new FirebaseHelper.QuestionsCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> questions) {
                if (!questions.isEmpty()) {
                    questionsList = questions;
                    currentQuestionIndex = 0;
                    displayQuestion(questionsList.get(currentQuestionIndex));
                } else {
                    // Try next level
                    fbHelper.getGameQuestions(languageId, level + 1, new FirebaseHelper.QuestionsCallback() {
                        @Override
                        public void onSuccess(List<Map<String, Object>> nextQuestions) {
                            if (!nextQuestions.isEmpty()) {
                                getTotalXp(totalXp -> {
                                    fbHelper.updateUserGameProgress(userId, languageId, level + 1, totalXp, null);
                                    loadQuestion(level + 1);
                                });
                            } else {
                                showCompletionMessage();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            showCompletionMessage();
                        }
                    });
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(requireContext(), "Error loading questions", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayQuestion(Map<String, Object> question) {
        String questionText = (String) question.get("question_text");
        String correctAnswer = (String) question.get("correct_answer");
        String imageUrl = (String) question.get("image_url");

        List<String> options = new ArrayList<>();
        options.add((String) question.get("option_1"));
        options.add((String) question.get("option_2"));
        options.add((String) question.get("option_3"));
        options.add((String) question.get("option_4"));

        tvQuestion.setText(questionText);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("@drawable")) {
                String drawableName = imageUrl.substring(10);
                int resId = getResources().getIdentifier(drawableName, "drawable", requireContext().getPackageName());
                if (resId != 0) {
                    ivImage.setImageResource(resId);
                } else {
                    ivImage.setImageResource(R.drawable.bg_image);
                }
            } else {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_image)
                        .error(R.drawable.bg_image)
                        .into(ivImage);
            }
        } else {
            ivImage.setImageResource(R.drawable.bg_image);
        }

        Collections.shuffle(options);
        btnOption1.setText(options.get(0));
        btnOption2.setText(options.get(1));
        btnOption3.setText(options.get(2));
        btnOption4.setText(options.get(3));

        btnOption1.setVisibility(View.VISIBLE);
        btnOption2.setVisibility(View.VISIBLE);
        btnOption3.setVisibility(View.VISIBLE);
        btnOption4.setVisibility(View.VISIBLE);
    }

    private void showCompletionMessage() {
        tvQuestion.setText("Semua soal telah selesai!");
        btnOption1.setText("Kembali");
        btnOption1.setVisibility(View.VISIBLE);
        btnOption1.setOnClickListener(v -> requireActivity().finish());
        btnOption2.setVisibility(View.GONE);
        btnOption3.setVisibility(View.GONE);
        btnOption4.setVisibility(View.GONE);
        tvSelectedAnswer.setText("");
        ivImage.setImageResource(R.drawable.bg_image);
    }

    private void setupButtons() {
        View.OnClickListener onClickListener = v -> {
            Button clickedButton = (Button) v;
            String selectedAnswer = clickedButton.getText().toString();
            tvSelectedAnswer.setText("Jawab: " + selectedAnswer);

            if (questionsList.isEmpty()) return;

            Map<String, Object> currentQuestion = questionsList.get(currentQuestionIndex);
            String correctAnswer = (String) currentQuestion.get("correct_answer");

            boolean isCorrect = selectedAnswer.equals(correctAnswer);
            if (listener != null) {
                listener.onQuestionAnswered(isCorrect);
            }

            if (isCorrect) {
                Toast.makeText(requireContext(), "Benar! +10 XP", Toast.LENGTH_SHORT).show();
                fbHelper.recordXpGain(userId, 10, null);

                getCurrentLevel(currentLevel -> {
                    getTotalXp(totalXp -> {
                        int newTotalXp = totalXp + 10;
                        int newLevel = currentLevel;
                        if (newTotalXp >= currentLevel * 100) {
                            newLevel++;
                        }

                        int finalNewLevel = newLevel;
                        fbHelper.updateUserGameProgress(userId, languageId, finalNewLevel, newTotalXp, null);

                        wordsCompleted++;
                        tvProgress.setText(wordsCompleted + "/5");
                        progressDailyMission.setProgress(wordsCompleted);

                        weeklyCompleted++;
                        saveProgress();

                        if (wordsCompleted >= 10) {
                            Toast.makeText(requireContext(), "Misi Harian Selesai! +50 XP", Toast.LENGTH_SHORT).show();
                            fbHelper.recordXpGain(userId, 50, null);
                            int bonusXp = newTotalXp + 50;
                            int bonusLevel = finalNewLevel;
                            if (bonusXp >= finalNewLevel * 100) {
                                bonusLevel++;
                            }
                            fbHelper.updateUserGameProgress(userId, languageId, bonusLevel, bonusXp, null);
                            wordsCompleted = 0;
                            tvProgress.setText("0/5");
                            progressDailyMission.setProgress(0);
                        }

                        if (weeklyCompleted >= 50) {
                            Toast.makeText(requireContext(), "Misi Mingguan Selesai! +200 XP", Toast.LENGTH_SHORT).show();
                            fbHelper.recordXpGain(userId, 200, null);
                            weeklyCompleted = 0;
                        }

                        loadQuestion(finalNewLevel);
                        tvSelectedAnswer.setText("");
                    });
                });
            } else {
                Toast.makeText(requireContext(), "Salah, coba lagi!", Toast.LENGTH_SHORT).show();
            }
        };

        btnOption1.setOnClickListener(onClickListener);
        btnOption2.setOnClickListener(onClickListener);
        btnOption3.setOnClickListener(onClickListener);
        btnOption4.setOnClickListener(onClickListener);
    }

    private void startLearningTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timeSpent++;
                progressLearning.setProgress(timeSpent / 6);
                tvLearningMission.setText("Belajar 10 Menit (" + (timeSpent / 60) + "/10 Menit)");
                if (timeSpent >= 600) {
                    Toast.makeText(requireContext(), "Target Belajar 10 Menit Tercapai! +30 XP", Toast.LENGTH_SHORT).show();
                    fbHelper.recordXpGain(userId, 30, null);
                    timeSpent = 0;
                    progressLearning.setProgress(0);
                    tvLearningMission.setText("Belajar 10 Menit (0/10 Menit)");
                }
                saveProgress();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    private void loadProgress() {
        // Load daily progress from Firebase
        // This would typically be stored in daily_missions collection
        wordsCompleted = 0;
        weeklyCompleted = 0;
        timeSpent = 0;
    }

    private void saveProgress() {
        // Save progress to Firebase
        fbHelper.updateDailyMissionProgress(userId, wordsCompleted);
        fbHelper.updateWeeklyProgress(userId, weeklyCompleted);
    }

    private void getCurrentLevel(LevelCallback callback) {
        fbHelper.getUserGameProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> progressData) {
                Long level = (Long) progressData.get("current_level");
                callback.onLevelLoaded(level != null ? level.intValue() : 1);
            }

            @Override
            public void onFailure(String error) {
                callback.onLevelLoaded(1);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (timerRunnable != null) {
            handler.post(timerRunnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timerRunnable);
    }

    // Callback interfaces
    interface LevelCallback {
        void onLevelLoaded(int level);
    }

    interface XpLoadCallback {
        void onXpLoaded(int xp);
    }
}