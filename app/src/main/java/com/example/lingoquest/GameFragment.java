package com.example.lingoquest;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

public class GameFragment extends Fragment {

    private TextView tvQuestion, tvLevel, tvProgress, tvCoins, tvSelectedAnswer, tvDailyMission, tvLearningMission;
    private Button btnOption1, btnOption2, btnOption3, btnOption4;
    private ImageView ivImage;
    private ProgressBar progressDailyMission, progressLearning;
    private DatabaseHelper db;
    private int userId, languageId;
    private String targetLanguage;
    private int wordsCompleted = 0; // Progres harian
    private int weeklyCompleted = 0; // Progres mingguan
    private int timeSpent = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;
    private OnQuestionAnsweredListener listener;

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
        db = new DatabaseHelper(requireContext());
        if (getArguments() != null) {
            targetLanguage = getArguments().getString("language");
        }

        SharedPreferences prefs = requireContext().getSharedPreferences("LingoQuestPrefs", Context.MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);
        if (userId == -1) {
            return;
        }

        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor langCursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_LANGUAGE_ID + " FROM " + DatabaseHelper.TABLE_LANGUAGES +
                        " WHERE " + DatabaseHelper.COLUMN_LANGUAGE_NAME + " = ?",
                new String[]{targetLanguage});
        if (langCursor.moveToFirst()) {
            languageId = langCursor.getInt(langCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE_ID));
        } else {
            languageId = 1;
        }
        langCursor.close();

        loadProgress();
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

        int currentLevel = getCurrentLevel();
        int totalXp = 0;
        int coins = 0;
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor progressCursor = readableDb.rawQuery(
                "SELECT total_xp FROM user_game_progress WHERE user_id = ? AND language_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(languageId)});
        if (progressCursor.moveToFirst()) {
            totalXp = progressCursor.getInt(progressCursor.getColumnIndexOrThrow("total_xp"));
        }
        progressCursor.close();

        Cursor statsCursor = readableDb.rawQuery(
                "SELECT points FROM user_stats WHERE user_id = ?",
                new String[]{String.valueOf(userId)});
        if (statsCursor.moveToFirst()) {
            coins = statsCursor.getInt(statsCursor.getColumnIndexOrThrow("points"));
        }
        statsCursor.close();

        tvLevel.setText("Level " + currentLevel + ": Menerjemahkan Kata");
        tvProgress.setText(wordsCompleted + "/5");
        tvCoins.setText("💰 " + coins);
        tvDailyMission.setText("Selesaikan 5 Kata");
        tvLearningMission.setText("Belajar 10 Menit (" + (timeSpent / 60) + "/10 Menit)");

        progressDailyMission.setProgress(wordsCompleted);
        progressLearning.setProgress(timeSpent / 6);

        startLearningTimer();
        loadQuestion(currentLevel);
        setupButtons();

        return view;
    }
    private int getTotalXp() {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor cursor = readableDb.rawQuery(
                "SELECT total_xp FROM user_game_progress WHERE user_id = ? AND language_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(languageId)});
        int totalXp = 0;
        if (cursor.moveToFirst()) {
            totalXp = cursor.getInt(cursor.getColumnIndexOrThrow("total_xp"));
        }
        cursor.close();
        return totalXp;
    }

    private void loadQuestion(int level) {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor cursor = readableDb.rawQuery(
                "SELECT * FROM game_questions WHERE language_id = ? AND question_level = ?",
                new String[]{String.valueOf(languageId), String.valueOf(level)});
        if (cursor.moveToFirst()) {
            String questionText = cursor.getString(cursor.getColumnIndexOrThrow("question_text"));
            String correctAnswer = cursor.getString(cursor.getColumnIndexOrThrow("correct_answer"));
            String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("image_url"));
            List<String> options = new ArrayList<>();
            options.add(cursor.getString(cursor.getColumnIndexOrThrow("option_1")));
            options.add(cursor.getString(cursor.getColumnIndexOrThrow("option_2")));
            options.add(cursor.getString(cursor.getColumnIndexOrThrow("option_3")));
            options.add(cursor.getString(cursor.getColumnIndexOrThrow("option_4")));

            tvQuestion.setText(questionText);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("@drawable")) {
                    String drawableName = imageUrl.substring(10); // Hapus "@drawable/"
                    int resId = getResources().getIdentifier(drawableName, "drawable", requireContext().getPackageName());
                    if (resId != 0) {
                        ivImage.setImageResource(resId);
                    } else {
                        ivImage.setImageResource(R.drawable.bg_image);
                    }
                } else {
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.bg_image).error(R.drawable.bg_image).into(ivImage);
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
        } else {
            // Cek apakah ada soal di level berikutnya
            Cursor nextLevelCursor = readableDb.rawQuery(
                    "SELECT * FROM game_questions WHERE language_id = ? AND question_level = ?",
                    new String[]{String.valueOf(languageId), String.valueOf(level + 1)});
            if (nextLevelCursor.moveToFirst()) {
                db.updateUserGameProgress(userId, languageId, level + 1, getTotalXp());
                loadQuestion(level + 1);
            } else {
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
            nextLevelCursor.close();
        }
        cursor.close();
    }

    private void setupButtons() {
        View.OnClickListener onClickListener = v -> {
            Button clickedButton = (Button) v;
            String selectedAnswer = clickedButton.getText().toString();
            tvSelectedAnswer.setText("Jawab: " + selectedAnswer);

            SQLiteDatabase readableDb = db.getReadableDatabase();
            Cursor cursor = readableDb.rawQuery(
                    "SELECT correct_answer FROM game_questions WHERE language_id = ? AND question_level = ?",
                    new String[]{String.valueOf(languageId), String.valueOf(getCurrentLevel())});
            String correctAnswer = "";
            if (cursor.moveToFirst()) {
                correctAnswer = cursor.getString(cursor.getColumnIndexOrThrow("correct_answer"));
            }
            cursor.close();

            boolean isCorrect = selectedAnswer.equals(correctAnswer);
            if (listener != null) {
                listener.onQuestionAnswered(isCorrect);
            }

            if (isCorrect) {
                Toast.makeText(requireContext(), "Benar! +10 XP", Toast.LENGTH_SHORT).show();
                db.recordXpGain(userId, 10); // Catat XP ke xp_history

                // Perbarui total_xp dan cek kenaikan level di user_game_progress
                int currentLevel = getCurrentLevel();
                int totalXp = getTotalXp() + 10;
                if (totalXp >= currentLevel * 100) { // Naik level setiap 100 XP
                    currentLevel++;
                }
                db.updateUserGameProgress(userId, languageId, currentLevel, totalXp);

                // Tambah progres harian
                wordsCompleted++;
                tvProgress.setText(wordsCompleted + "/5");
                progressDailyMission.setProgress(wordsCompleted);

                // Tambah progres mingguan
                weeklyCompleted++;
                saveProgress(); // Simpan progres harian dan mingguan ke database

                // Tambah jumlah kata yang dipelajari (correct_answers)
                SQLiteDatabase writableDb = db.getWritableDatabase();
                writableDb.execSQL(
                        "UPDATE " + DatabaseHelper.TABLE_USER_STATS + " SET " + DatabaseHelper.COLUMN_CORRECT_ANSWERS + " = " + DatabaseHelper.COLUMN_CORRECT_ANSWERS + " + 1 " +
                                "WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                        new Object[]{userId}
                );
                writableDb.close();

                // Cek jika misi harian selesai
                if (wordsCompleted >= 10) { // Ubah target menjadi 5 soal sesuai UI
                    Toast.makeText(requireContext(), "Misi Harian Selesai! +50 XP", Toast.LENGTH_SHORT).show();
                    db.recordXpGain(userId, 50); // Catat XP misi harian
                    totalXp += 50;
                    if (totalXp >= currentLevel * 100) {
                        currentLevel++;
                    }
                    db.updateUserGameProgress(userId, languageId, currentLevel, totalXp);
                    updateMissionProgress(true, 50);
                    wordsCompleted = 0;
                    tvProgress.setText("0/5");
                    progressDailyMission.setProgress(0);
                } else {
                    updateMissionProgress(false, 0);
                }

                // Cek jika misi mingguan selesai
                if (weeklyCompleted >= 50) {
                    Toast.makeText(requireContext(), "Misi Mingguan Selesai! +200 XP", Toast.LENGTH_SHORT).show();
                    db.recordXpGain(userId, 200); // Catat XP misi mingguan
                    totalXp += 200;
                    if (totalXp >= currentLevel * 100) {
                        currentLevel++;
                    }
                    db.updateUserGameProgress(userId, languageId, currentLevel, totalXp);
                    updateWeeklyProgress(true, 200);
                    weeklyCompleted = 0;
                } else {
                    updateWeeklyProgress(false, 0);
                }

                loadQuestion(currentLevel);
                tvSelectedAnswer.setText("");
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
                    db.recordXpGain(userId, 30); // Catat XP belajar ke xp_history
                    updateLearningProgress(30);
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
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor cursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_DAILY_PROGRESS + " FROM " + DatabaseHelper.TABLE_DAILY_MISSIONS +
                        " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            wordsCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DAILY_PROGRESS));
        } else {
            wordsCompleted = 0;
        }
        cursor.close();

        Cursor weeklyCursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_WEEKLY_PROGRESS + " FROM " + DatabaseHelper.TABLE_WEEKLY_CHALLENGES +
                        " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        if (weeklyCursor.moveToFirst()) {
            weeklyCompleted = weeklyCursor.getInt(weeklyCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEEKLY_PROGRESS));
        } else {
            weeklyCompleted = 0;
        }
        weeklyCursor.close();

        Cursor learningCursor = readableDb.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_WEEKLY_PROGRESS + " FROM " + DatabaseHelper.TABLE_WEEKLY_CHALLENGES +
                        " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        if (learningCursor.moveToFirst()) {
            timeSpent = learningCursor.getInt(learningCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEEKLY_PROGRESS));
        } else {
            timeSpent = 0;
        }
        learningCursor.close();
    }

    private void saveProgress() {
        SQLiteDatabase writableDb = db.getWritableDatabase();

        // Cek dan simpan progres harian
        Cursor cursor = writableDb.rawQuery(
                "SELECT * FROM " + DatabaseHelper.TABLE_DAILY_MISSIONS + " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        if (cursor.getCount() == 0) {
            // Insert jika belum ada
            writableDb.execSQL(
                    "INSERT INTO " + DatabaseHelper.TABLE_DAILY_MISSIONS + " (" + DatabaseHelper.COLUMN_USER_ID + ", " +
                            DatabaseHelper.COLUMN_DAILY_PROGRESS + ", " + DatabaseHelper.COLUMN_END_TIME + ", " +
                            DatabaseHelper.COLUMN_LAST_UPDATED + ") VALUES (?, ?, ?, ?)",
                    new Object[]{userId, wordsCompleted, System.currentTimeMillis() + 86400000,
                            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date())}
            );
        } else {
            // Update jika sudah ada
            writableDb.execSQL(
                    "UPDATE " + DatabaseHelper.TABLE_DAILY_MISSIONS + " SET " + DatabaseHelper.COLUMN_DAILY_PROGRESS + " = ? " +
                            "WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                    new Object[]{wordsCompleted, userId}
            );
        }
        cursor.close();

        // Cek dan simpan progres mingguan
        Cursor weeklyCursor = writableDb.rawQuery(
                "SELECT * FROM " + DatabaseHelper.TABLE_WEEKLY_CHALLENGES + " WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        if (weeklyCursor.getCount() == 0) {
            // Insert jika belum ada
            writableDb.execSQL(
                    "INSERT INTO " + DatabaseHelper.TABLE_WEEKLY_CHALLENGES + " (" + DatabaseHelper.COLUMN_USER_ID + ", " +
                            DatabaseHelper.COLUMN_WEEKLY_PROGRESS + ", " + DatabaseHelper.COLUMN_END_TIME + ") VALUES (?, ?, ?)",
                    new Object[]{userId, weeklyCompleted, System.currentTimeMillis() + 604800000}
            );
        } else {
            // Update jika sudah ada
            writableDb.execSQL(
                    "UPDATE " + DatabaseHelper.TABLE_WEEKLY_CHALLENGES + " SET " + DatabaseHelper.COLUMN_WEEKLY_PROGRESS + " = ? " +
                            "WHERE " + DatabaseHelper.COLUMN_USER_ID + " = ?",
                    new Object[]{weeklyCompleted, userId}
            );
        }
        weeklyCursor.close();
    }

    private void updateMissionProgress(boolean missionCompleted, int xpGain) {
        if (missionCompleted) {
            SQLiteDatabase writableDb = db.getWritableDatabase();
            writableDb.execSQL(
                    "UPDATE user_stats SET points = points + ? WHERE user_id = ?",
                    new Object[]{xpGain, userId}
            );
        }
        saveProgress();
    }

    private void updateWeeklyProgress(boolean missionCompleted, int xpGain) {
        if (missionCompleted) {
            SQLiteDatabase writableDb = db.getWritableDatabase();
            writableDb.execSQL(
                    "UPDATE user_stats SET points = points + ? WHERE user_id = ?",
                    new Object[]{xpGain, userId}
            );
            writableDb.close();
        }
        saveProgress();
    }

    private void updateLearningProgress(int xpGain) {
        SQLiteDatabase writableDb = db.getWritableDatabase();
        writableDb.execSQL(
                "UPDATE user_stats SET points = points + ? WHERE user_id = ?",
                new Object[]{xpGain, userId}
        );
        saveProgress();
    }

    private int getCurrentLevel() {
        SQLiteDatabase readableDb = db.getReadableDatabase();
        Cursor cursor = readableDb.rawQuery(
                "SELECT current_level FROM user_game_progress WHERE user_id = ? AND language_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(languageId)});
        int level = 1;
        if (cursor.moveToFirst()) {
            level = cursor.getInt(cursor.getColumnIndexOrThrow("current_level"));
        }
        cursor.close();
        return level;
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(timerRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timerRunnable);
        if (db != null) {
        }
    }
}