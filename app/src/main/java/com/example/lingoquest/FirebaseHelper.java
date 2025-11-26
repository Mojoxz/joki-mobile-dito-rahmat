package com.example.lingoquest;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // Collection names
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_USER_STATS = "user_stats";
    private static final String COLLECTION_LANGUAGES = "languages";
    private static final String COLLECTION_USER_LANGUAGES = "user_languages";
    private static final String COLLECTION_GAME_QUESTIONS = "game_questions";
    private static final String COLLECTION_USER_GAME_PROGRESS = "user_game_progress";
    private static final String COLLECTION_DAILY_MISSIONS = "daily_missions";
    private static final String COLLECTION_WEEKLY_CHALLENGES = "weekly_challenges";
    private static final String COLLECTION_CHALLENGE_MODES = "challenge_modes";
    private static final String COLLECTION_XP_HISTORY = "xp_history";
    private static final String COLLECTION_LEADERBOARD = "leaderboard";
    private static final String COLLECTION_LISTENING_QUESTIONS = "listening_questions";
    private static final String COLLECTION_READING_QUESTIONS = "reading_questions";
    private static final String COLLECTION_USER_LISTENING_PROGRESS = "user_listening_progress";
    private static final String COLLECTION_USER_READING_PROGRESS = "user_reading_progress";

    private static FirebaseHelper instance;

    private FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    // ==================== Authentication ====================

    public interface AuthCallback {
        void onSuccess(String userId);
        void onFailure(String error);
    }

    public void registerUser(String email, String password, String username,
                             String avatarUrl, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            createUserDocument(userId, username, email, avatarUrl, callback);
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Registration failed");
                    }
                });
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            callback.onSuccess(user.getUid());
                        }
                    } else {
                        callback.onFailure(task.getException() != null ?
                                task.getException().getMessage() : "Login failed");
                    }
                });
    }

    public void logoutUser() {
        mAuth.signOut();
    }

    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    // ==================== User Management ====================

    private void createUserDocument(String userId, String username, String email,
                                    String avatarUrl, AuthCallback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("avatar_url", avatarUrl);
        user.put("is_admin", false);
        user.put("created_at", FieldValue.serverTimestamp());

        db.collection(COLLECTION_USERS).document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Initialize related collections
                    initializeUserStats(userId);
                    initializeChallengeModes(userId);
                    initializeDailyMissions(userId);
                    initializeWeeklyChallenges(userId);
                    initializeUserLanguages(userId);
                    callback.onSuccess(userId);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                });
    }

    private void initializeUserStats(String userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("level", 1);
        stats.put("points", 0);
        stats.put("streak_days", 0);
        stats.put("last_active", FieldValue.serverTimestamp());
        stats.put("correct_answers", 0);
        stats.put("is_weekly_champion", false);
        stats.put("weekly_xp", 0);

        db.collection(COLLECTION_USER_STATS).document(userId)
                .set(stats)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User stats initialized"))
                .addOnFailureListener(e -> Log.e(TAG, "Error initializing user stats", e));
    }

    private void initializeChallengeModes(String userId) {
        Map<String, Object> modes = new HashMap<>();
        modes.put("speed_target", 42);
        modes.put("speed_questions", 0);
        modes.put("duel_target", 5);
        modes.put("duel_wins", 0);
        modes.put("survival_target", 8);
        modes.put("survival_level", 0);
        modes.put("story_target", 2);
        modes.put("story_progress", 0);

        db.collection(COLLECTION_CHALLENGE_MODES).document(userId)
                .set(modes);
    }

    private void initializeDailyMissions(String userId) {
        Map<String, Object> mission = new HashMap<>();
        mission.put("progress", 0);
        mission.put("end_time", System.currentTimeMillis() + 86400000); // 1 day
        mission.put("last_updated", FieldValue.serverTimestamp());

        db.collection(COLLECTION_DAILY_MISSIONS).document(userId)
                .set(mission);
    }

    private void initializeWeeklyChallenges(String userId) {
        Map<String, Object> challenge = new HashMap<>();
        challenge.put("progress", 0);
        challenge.put("end_time", System.currentTimeMillis() + 604800000); // 1 week

        db.collection(COLLECTION_WEEKLY_CHALLENGES).document(userId)
                .set(challenge);
    }

    private void initializeUserLanguages(String userId) {
        String[] languages = {"Bahasa Inggris", "Bahasa Jepang", "Bahasa Korea", "Bahasa Mandarin"};

        for (String lang : languages) {
            getLanguageId(lang, languageId -> {
                if (languageId != null) {
                    // Add to user_languages
                    Map<String, Object> userLang = new HashMap<>();
                    userLang.put("user_id", userId);
                    userLang.put("language_id", languageId);
                    userLang.put("progress", 0);

                    db.collection(COLLECTION_USER_LANGUAGES)
                            .add(userLang);

                    // Initialize game progress
                    Map<String, Object> progress = new HashMap<>();
                    progress.put("user_id", userId);
                    progress.put("language_id", languageId);
                    progress.put("current_level", 1);
                    progress.put("total_xp", 0);

                    db.collection(COLLECTION_USER_GAME_PROGRESS)
                            .add(progress);
                }
            });
        }
    }

    // ==================== User Data ====================

    public interface UserDataCallback {
        void onSuccess(Map<String, Object> userData);
        void onFailure(String error);
    }

    public void getUserData(String userId, UserDataCallback callback) {
        db.collection(COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess(document.getData());
                    } else {
                        callback.onFailure("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getUserStats(String userId, UserDataCallback callback) {
        db.collection(COLLECTION_USER_STATS).document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        callback.onSuccess(document.getData());
                    } else {
                        callback.onFailure("Stats not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateUserAvatar(String userId, Uri imageUri, AvatarUploadCallback callback) {
        StorageReference avatarRef = storage.getReference()
                .child("avatars/" + userId + ".jpg");

        avatarRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String avatarUrl = uri.toString();

                        // Update user document
                        db.collection(COLLECTION_USERS).document(userId)
                                .update("avatar_url", avatarUrl)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(avatarUrl))
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    });
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface AvatarUploadCallback {
        void onSuccess(String avatarUrl);
        void onFailure(String error);
    }

    // ==================== XP Management ====================

    public void recordXpGain(String userId, int xpGained, XpCallback callback) {
        // Add to XP history
        Map<String, Object> xpRecord = new HashMap<>();
        xpRecord.put("user_id", userId);
        xpRecord.put("xp_gained", xpGained);
        xpRecord.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_XP_HISTORY)
                .add(xpRecord)
                .addOnSuccessListener(docRef -> {
                    // Update user stats
                    DocumentReference statsRef = db.collection(COLLECTION_USER_STATS).document(userId);

                    db.runTransaction(transaction -> {
                        DocumentSnapshot snapshot = transaction.get(statsRef);
                        long currentPoints = snapshot.getLong("points") != null ?
                                snapshot.getLong("points") : 0;
                        long currentWeeklyXp = snapshot.getLong("weekly_xp") != null ?
                                snapshot.getLong("weekly_xp") : 0;

                        transaction.update(statsRef, "points", currentPoints + xpGained);
                        transaction.update(statsRef, "weekly_xp", currentWeeklyXp + xpGained);

                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess();
                    }).addOnFailureListener(e -> {
                        if (callback != null) callback.onFailure(e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public interface XpCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public void getTotalXp(String userId, TotalXpCallback callback) {
        db.collection(COLLECTION_USER_STATS).document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long points = document.getLong("points");
                        callback.onSuccess(points != null ? points.intValue() : 0);
                    } else {
                        callback.onSuccess(0);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface TotalXpCallback {
        void onSuccess(int totalXp);
        void onFailure(String error);
    }

    // ==================== Game Questions ====================

    public interface QuestionsCallback {
        void onSuccess(List<Map<String, Object>> questions);
        void onFailure(String error);
    }

    public void getGameQuestions(String languageId, int level, QuestionsCallback callback) {
        db.collection(COLLECTION_GAME_QUESTIONS)
                .whereEqualTo("language_id", languageId)
                .whereEqualTo("question_level", level)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> questions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> question = doc.getData();
                        question.put("question_id", doc.getId());
                        questions.add(question);
                    }
                    callback.onSuccess(questions);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void addGameQuestion(Map<String, Object> questionData, AddQuestionCallback callback) {
        db.collection(COLLECTION_GAME_QUESTIONS)
                .add(questionData)
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface AddQuestionCallback {
        void onSuccess(String questionId);
        void onFailure(String error);
    }

    // ==================== Language Management ====================

    public interface LanguageIdCallback {
        void onResult(String languageId);
    }

    public void getLanguageId(String languageName, LanguageIdCallback callback) {
        db.collection(COLLECTION_LANGUAGES)
                .whereEqualTo("language_name", languageName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onResult(queryDocumentSnapshots.getDocuments().get(0).getId());
                    } else {
                        callback.onResult(null);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(null));
    }

    // ==================== User Game Progress ====================

    public void updateUserGameProgress(String userId, String languageId,
                                       int currentLevel, int totalXp, XpCallback callback) {
        db.collection(COLLECTION_USER_GAME_PROGRESS)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("language_id", languageId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("current_level", currentLevel);
                        updates.put("total_xp", totalXp);

                        db.collection(COLLECTION_USER_GAME_PROGRESS).document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                    }
                });
    }

    public void getUserGameProgress(String userId, String languageId, UserDataCallback callback) {
        db.collection(COLLECTION_USER_GAME_PROGRESS)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("language_id", languageId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(queryDocumentSnapshots.getDocuments().get(0).getData());
                    } else {
                        callback.onFailure("Progress not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ==================== Leaderboard ====================

    public interface LeaderboardCallback {
        void onSuccess(List<Map<String, Object>> leaderboard);
        void onFailure(String error);
    }

    public void getLeaderboard(String period, int limit, LeaderboardCallback callback) {
        long timeFilter = 0;
        switch (period) {
            case "Harian":
                timeFilter = System.currentTimeMillis() - 86400000; // 1 day
                break;
            case "Mingguan":
                timeFilter = System.currentTimeMillis() - 604800000; // 7 days
                break;
            case "Bulanan":
                timeFilter = System.currentTimeMillis() - 2592000000L; // 30 days
                break;
        }

        final long finalTimeFilter = timeFilter;

        db.collection(COLLECTION_USER_STATS)
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> leaderboard = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String userId = doc.getId();

                        // Get user data
                        db.collection(COLLECTION_USERS).document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        Map<String, Object> entry = new HashMap<>();
                                        entry.put("user_id", userId);
                                        entry.put("username", userDoc.getString("username"));
                                        entry.put("avatar_url", userDoc.getString("avatar_url"));
                                        entry.put("total_xp", doc.getLong("points"));
                                        leaderboard.add(entry);

                                        if (leaderboard.size() == queryDocumentSnapshots.size()) {
                                            callback.onSuccess(leaderboard);
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // ==================== Admin Check ====================

    public void isAdmin(String userId, AdminCheckCallback callback) {
        db.collection(COLLECTION_USERS).document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Boolean isAdmin = document.getBoolean("is_admin");
                        callback.onResult(isAdmin != null && isAdmin);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
    }

    // ==================== Missions ====================

    public void updateDailyMissionProgress(String userId, int progress) {
        db.collection(COLLECTION_DAILY_MISSIONS).document(userId)
                .update("progress", progress, "last_updated", FieldValue.serverTimestamp());
    }

    public void updateWeeklyProgress(String userId, int progress) {
        db.collection(COLLECTION_WEEKLY_CHALLENGES).document(userId)
                .update("progress", progress);
    }

    public void isDailyMissionCompleted(String userId, CompletionCallback callback) {
        db.collection(COLLECTION_DAILY_MISSIONS).document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long progress = document.getLong("progress");
                        callback.onResult(progress != null && progress >= 10);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public void isWeeklyMissionCompleted(String userId, CompletionCallback callback) {
        db.collection(COLLECTION_WEEKLY_CHALLENGES).document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long progress = document.getLong("progress");
                        callback.onResult(progress != null && progress >= 50);
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public interface CompletionCallback {
        void onResult(boolean isCompleted);
    }

    // ==================== Weekly XP Reset ====================

    public void resetWeeklyXp() {
        db.collection(COLLECTION_USER_STATS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().update("weekly_xp", 0);
                    }
                    Log.d(TAG, "Weekly XP reset for all users");
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error resetting weekly XP", e));
    }

    // ==================== Listening Questions ====================

    public void getListeningQuestions(String languageId, int level, QuestionsCallback callback) {
        db.collection(COLLECTION_LISTENING_QUESTIONS)
                .whereEqualTo("language_id", languageId)
                .whereEqualTo("question_level", level)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> questions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> question = doc.getData();
                        question.put("question_id", doc.getId());
                        questions.add(question);
                    }
                    callback.onSuccess(questions);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void addListeningQuestion(Map<String, Object> questionData, AddQuestionCallback callback) {
        db.collection(COLLECTION_LISTENING_QUESTIONS)
                .add(questionData)
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

// ==================== Reading Questions ====================

    public void getReadingQuestions(String languageId, int level, QuestionsCallback callback) {
        db.collection(COLLECTION_READING_QUESTIONS)
                .whereEqualTo("language_id", languageId)
                .whereEqualTo("question_level", level)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> questions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> question = doc.getData();
                        question.put("question_id", doc.getId());
                        questions.add(question);
                    }
                    callback.onSuccess(questions);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void addReadingQuestion(Map<String, Object> questionData, AddQuestionCallback callback) {
        db.collection(COLLECTION_READING_QUESTIONS)
                .add(questionData)
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

// ==================== User Progress ====================

    public void updateUserListeningProgress(String userId, String languageId,
                                            int currentLevel, int totalXp, XpCallback callback) {
        db.collection(COLLECTION_USER_LISTENING_PROGRESS)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("language_id", languageId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("current_level", currentLevel);
                        updates.put("total_xp", totalXp);

                        db.collection(COLLECTION_USER_LISTENING_PROGRESS).document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                    } else {
                        // Create new progress
                        Map<String, Object> progress = new HashMap<>();
                        progress.put("user_id", userId);
                        progress.put("language_id", languageId);
                        progress.put("current_level", currentLevel);
                        progress.put("total_xp", totalXp);

                        db.collection(COLLECTION_USER_LISTENING_PROGRESS)
                                .add(progress)
                                .addOnSuccessListener(docRef -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                    }
                });
    }

    public void updateUserReadingProgress(String userId, String languageId,
                                          int currentLevel, int totalXp, XpCallback callback) {
        db.collection(COLLECTION_USER_READING_PROGRESS)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("language_id", languageId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("current_level", currentLevel);
                        updates.put("total_xp", totalXp);

                        db.collection(COLLECTION_USER_READING_PROGRESS).document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                    } else {
                        // Create new progress
                        Map<String, Object> progress = new HashMap<>();
                        progress.put("user_id", userId);
                        progress.put("language_id", languageId);
                        progress.put("current_level", currentLevel);
                        progress.put("total_xp", totalXp);

                        db.collection(COLLECTION_USER_READING_PROGRESS)
                                .add(progress)
                                .addOnSuccessListener(docRef -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                    }
                });
    }

    public void getUserListeningProgress(String userId, String languageId, UserDataCallback callback) {
        db.collection(COLLECTION_USER_LISTENING_PROGRESS)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("language_id", languageId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(queryDocumentSnapshots.getDocuments().get(0).getData());
                    } else {
                        callback.onFailure("Progress not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getUserReadingProgress(String userId, String languageId, UserDataCallback callback) {
        db.collection(COLLECTION_USER_READING_PROGRESS)
                .whereEqualTo("user_id", userId)
                .whereEqualTo("language_id", languageId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(queryDocumentSnapshots.getDocuments().get(0).getData());
                    } else {
                        callback.onFailure("Progress not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}

