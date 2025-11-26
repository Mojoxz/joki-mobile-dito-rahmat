package com.example.lingoquest;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ListeningActivity extends AppCompatActivity {

    private TextView tvQuestionNumber;
    private TextView tvQuestionText;
    private RadioGroup radioGroupOptions;
    private RadioButton rbOption1, rbOption2, rbOption3, rbOption4;
    private Button btnPlayAudio, btnSubmit, btnNext;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private MediaPlayer mediaPlayer;
    private FirebaseHelper firebaseHelper;
    private String userId, languageId, languageName;
    private int currentLevel = 1;
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private int totalXp = 0;

    private List<Map<String, Object>> questions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listening);

        firebaseHelper = FirebaseHelper.getInstance();
        userId = firebaseHelper.getCurrentUserId();
        languageName = getIntent().getStringExtra("language");

        initViews();
        setupListeners();
        loadLanguageAndQuestions();
    }

    private void initViews() {
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvQuestionText = findViewById(R.id.tv_question_text);
        radioGroupOptions = findViewById(R.id.radio_group_options);
        rbOption1 = findViewById(R.id.rb_option1);
        rbOption2 = findViewById(R.id.rb_option2);
        rbOption3 = findViewById(R.id.rb_option3);
        rbOption4 = findViewById(R.id.rb_option4);
        btnPlayAudio = findViewById(R.id.btn_play_audio);
        btnSubmit = findViewById(R.id.btn_submit);
        btnNext = findViewById(R.id.btn_next);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayAudio.setOnClickListener(v -> playAudio());

        btnSubmit.setOnClickListener(v -> submitAnswer());

        btnNext.setOnClickListener(v -> loadNextQuestion());
    }

    private void loadLanguageAndQuestions() {
        firebaseHelper.getLanguageId(languageName, langId -> {
            if (langId != null) {
                languageId = langId;
                loadUserProgress();
            } else {
                Toast.makeText(this, "Bahasa tidak ditemukan", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadUserProgress() {
        firebaseHelper.getUserListeningProgress(userId, languageId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                Long level = (Long) userData.get("current_level");
                Long xp = (Long) userData.get("total_xp");

                currentLevel = level != null ? level.intValue() : 1;
                totalXp = xp != null ? xp.intValue() : 0;

                loadQuestions();
            }

            @Override
            public void onFailure(String error) {
                currentLevel = 1;
                totalXp = 0;
                loadQuestions();
            }
        });
    }

    private void loadQuestions() {
        firebaseHelper.getListeningQuestions(languageId, currentLevel, new FirebaseHelper.QuestionsCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> questionsList) {
                if (questionsList.isEmpty()) {
                    showCompletionDialog();
                    return;
                }

                questions = questionsList;
                Collections.shuffle(questions);
                currentQuestionIndex = 0;
                correctAnswers = 0;

                displayQuestion();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ListeningActivity.this,
                        "Gagal memuat soal: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            showResultDialog();
            return;
        }

        Map<String, Object> question = questions.get(currentQuestionIndex);

        tvQuestionNumber.setText("Soal " + (currentQuestionIndex + 1) + "/" + questions.size());
        tvQuestionText.setText((String) question.get("question_text"));

        rbOption1.setText((String) question.get("option_1"));
        rbOption2.setText((String) question.get("option_2"));
        rbOption3.setText((String) question.get("option_3"));
        rbOption4.setText((String) question.get("option_4"));

        radioGroupOptions.clearCheck();
        btnSubmit.setEnabled(true);
        btnNext.setVisibility(View.GONE);

        progressBar.setMax(questions.size());
        progressBar.setProgress(currentQuestionIndex);

        // Get audio URL
        String audioUrl = (String) question.get("audio_url");
        prepareAudio(audioUrl);
    }

    private void prepareAudio(String audioUrl) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // In a real app, you would load the audio from Firebase Storage
        // For now, we'll use a local resource or mock it
        btnPlayAudio.setEnabled(true);
    }

    private void playAudio() {
        // Implementasi play audio
        // Dalam implementasi nyata, gunakan MediaPlayer dengan URL dari Firebase Storage

        Toast.makeText(this, "🔊 Memutar audio...", Toast.LENGTH_SHORT).show();

        // Mock: Simulasi audio playing
        btnPlayAudio.setEnabled(false);
        new Handler().postDelayed(() -> {
            btnPlayAudio.setEnabled(true);
            Toast.makeText(ListeningActivity.this, "Audio selesai", Toast.LENGTH_SHORT).show();
        }, 3000);

        /*
        // Implementasi real dengan Firebase Storage:
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.setOnPreparedListener(mp -> mp.start());
            mediaPlayer.setOnCompletionListener(mp -> {
                btnPlayAudio.setEnabled(true);
                Toast.makeText(ListeningActivity.this, "Audio selesai", Toast.LENGTH_SHORT).show();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal memutar audio", Toast.LENGTH_SHORT).show();
        }
        */
    }

    private void submitAnswer() {
        int selectedId = radioGroupOptions.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Pilih jawaban terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> question = questions.get(currentQuestionIndex);
        String correctAnswer = (String) question.get("correct_answer");

        RadioButton selectedRadio = findViewById(selectedId);
        String selectedAnswer = selectedRadio.getText().toString();

        if (selectedAnswer.equals(correctAnswer)) {
            correctAnswers++;
            Long xpReward = (Long) question.get("xp_reward");
            int xp = xpReward != null ? xpReward.intValue() : 10;
            totalXp += xp;

            selectedRadio.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            Toast.makeText(this, "✓ Benar! +" + xp + " XP", Toast.LENGTH_SHORT).show();
        } else {
            selectedRadio.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

            // Show correct answer
            if (rbOption1.getText().toString().equals(correctAnswer)) {
                rbOption1.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (rbOption2.getText().toString().equals(correctAnswer)) {
                rbOption2.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (rbOption3.getText().toString().equals(correctAnswer)) {
                rbOption3.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if (rbOption4.getText().toString().equals(correctAnswer)) {
                rbOption4.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }

            Toast.makeText(this, "✗ Salah! Jawaban: " + correctAnswer, Toast.LENGTH_SHORT).show();
        }

        btnSubmit.setEnabled(false);
        btnNext.setVisibility(View.VISIBLE);
    }

    private void loadNextQuestion() {
        // Reset colors
        rbOption1.setTextColor(getResources().getColor(android.R.color.white));
        rbOption2.setTextColor(getResources().getColor(android.R.color.white));
        rbOption3.setTextColor(getResources().getColor(android.R.color.white));
        rbOption4.setTextColor(getResources().getColor(android.R.color.white));

        currentQuestionIndex++;

        if (currentQuestionIndex >= questions.size()) {
            showResultDialog();
        } else {
            displayQuestion();
        }
    }

    private void showResultDialog() {
        // Update progress
        int newLevel = currentLevel;
        if (correctAnswers >= questions.size() * 0.7) {
            newLevel = currentLevel + 1;
        }

        firebaseHelper.updateUserListeningProgress(userId, languageId, newLevel, totalXp,
                new FirebaseHelper.XpCallback() {
                    @Override
                    public void onSuccess() {
                        firebaseHelper.recordXpGain(userId, totalXp, null);
                    }

                    @Override
                    public void onFailure(String error) {
                        // Handle error
                    }
                });

        new AlertDialog.Builder(this)
                .setTitle("Hasil Listening")
                .setMessage("Skor: " + correctAnswers + "/" + questions.size() + "\n" +
                        "Total XP: " + totalXp + "\n" +
                        (newLevel > currentLevel ? "Level naik ke " + newLevel + "!" : ""))
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showCompletionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Selamat!")
                .setMessage("Anda telah menyelesaikan semua level listening untuk " + languageName)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}