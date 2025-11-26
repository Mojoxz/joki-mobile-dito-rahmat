package com.example.lingoquest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "lingoquest.db";
    private static final int DATABASE_VERSION = 4; // Diperbarui dari 3 ke 4

    // Tabel Users
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_AVATAR_URL = "avatar_url";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_IS_ADMIN = "is_admin";

    // Tabel User_Stats
    public static final String TABLE_USER_STATS = "user_stats";
    public static final String COLUMN_LEVEL = "level";
    public static final String COLUMN_POINTS = "points";
    public static final String COLUMN_STREAK_DAYS = "streak_days";
    public static final String COLUMN_LAST_ACTIVE = "last_active";
    public static final String COLUMN_CORRECT_ANSWERS = "correct_answers";
    public static final String COLUMN_IS_WEEKLY_CHAMPION = "is_weekly_champion";
    public static final String COLUMN_WEEKLY_XP = "weekly_xp";

    // Tabel Languages
    public static final String TABLE_LANGUAGES = "languages";
    public static final String COLUMN_LANGUAGE_ID = "language_id";
    public static final String COLUMN_LANGUAGE_NAME = "language_name";
    public static final String COLUMN_ICON_URL = "icon_url";

    // Tabel User_Languages
    public static final String TABLE_USER_LANGUAGES = "user_languages";
    public static final String COLUMN_PROGRESS = "progress";

    // Tabel Daily_Challenges
    public static final String TABLE_DAILY_CHALLENGES = "daily_challenges";
    public static final String COLUMN_CHALLENGE_ID = "challenge_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_POINTS_REWARD = "points_reward";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_END_TIME = "end_time";

    // Tabel User_Challenges
    public static final String TABLE_USER_CHALLENGES = "user_challenges";
    public static final String COLUMN_COMPLETED = "completed";
    public static final String COLUMN_COMPLETION_TIME = "completion_time";

    // Tabel Game_Questions
    public static final String TABLE_GAME_QUESTIONS = "game_questions";
    public static final String COLUMN_QUESTION_ID = "question_id";
    public static final String COLUMN_QUESTION_LEVEL = "question_level";
    public static final String COLUMN_QUESTION_TEXT = "question_text";
    public static final String COLUMN_OPTION_1 = "option_1";
    public static final String COLUMN_OPTION_2 = "option_2";
    public static final String COLUMN_OPTION_3 = "option_3";
    public static final String COLUMN_OPTION_4 = "option_4";
    public static final String COLUMN_CORRECT_ANSWER = "correct_answer";
    public static final String COLUMN_XP_REWARD = "xp_reward";
    public static final String COLUMN_IMAGE_URL = "image_url";

    // Tabel User_Game_Progress
    public static final String TABLE_USER_GAME_PROGRESS = "user_game_progress";
    public static final String COLUMN_CURRENT_LEVEL = "current_level";
    public static final String COLUMN_TOTAL_XP = "total_xp";

    // Tabel Daily_Missions
    public static final String TABLE_DAILY_MISSIONS = "daily_missions";
    public static final String COLUMN_DAILY_PROGRESS = "progress";
    public static final String COLUMN_LAST_UPDATED = "last_updated"; // Kolom baru untuk timestamp

    // Tabel Weekly_Challenges
    public static final String TABLE_WEEKLY_CHALLENGES = "weekly_challenges";
    public static final String COLUMN_WEEKLY_PROGRESS = "progress";

    // Tabel Challenge_Modes
    public static final String TABLE_CHALLENGE_MODES = "challenge_modes";
    public static final String COLUMN_SPEED_TARGET = "speed_target";
    public static final String COLUMN_SPEED_QUESTIONS = "speed_questions";
    public static final String COLUMN_DUEL_TARGET = "duel_target";
    public static final String COLUMN_DUEL_WINS = "duel_wins";
    public static final String COLUMN_SURVIVAL_TARGET = "survival_target";
    public static final String COLUMN_SURVIVAL_LEVEL = "survival_level";
    public static final String COLUMN_STORY_TARGET = "story_target";
    public static final String COLUMN_STORY_PROGRESS = "story_progress";

    // Tabel XP_History
    public static final String TABLE_XP_HISTORY = "xp_history";
    public static final String COLUMN_XP_GAINED = "xp_gained";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Buat tabel Users dengan kolom is_admin
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT NOT NULL, " +
                COLUMN_EMAIL + " TEXT NOT NULL UNIQUE, " +
                COLUMN_PASSWORD + " TEXT NOT NULL, " +
                COLUMN_AVATAR_URL + " TEXT, " +
                COLUMN_IS_ADMIN + " INTEGER DEFAULT 0, " +
                COLUMN_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createUsersTable);

        // Buat tabel User_Stats dengan kolom tambahan
        String createUserStatsTable = "CREATE TABLE " + TABLE_USER_STATS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_LEVEL + " INTEGER DEFAULT 1, " +
                COLUMN_POINTS + " INTEGER DEFAULT 0, " +
                COLUMN_STREAK_DAYS + " INTEGER DEFAULT 0, " +
                COLUMN_LAST_ACTIVE + " DATETIME, " +
                COLUMN_CORRECT_ANSWERS + " INTEGER DEFAULT 0, " +
                COLUMN_IS_WEEKLY_CHAMPION + " INTEGER DEFAULT 0, " +
                COLUMN_WEEKLY_XP + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createUserStatsTable);

        // Buat tabel Languages
        String createLanguagesTable = "CREATE TABLE " + TABLE_LANGUAGES + " (" +
                COLUMN_LANGUAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LANGUAGE_NAME + " TEXT NOT NULL, " +
                COLUMN_ICON_URL + " TEXT)";
        db.execSQL(createLanguagesTable);

        // Buat tabel User_Languages
        String createUserLanguagesTable = "CREATE TABLE " + TABLE_USER_LANGUAGES + " (" +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_LANGUAGE_ID + " INTEGER, " +
                COLUMN_PROGRESS + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COLUMN_USER_ID + ", " + COLUMN_LANGUAGE_ID + "), " +
                "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "), " +
                "FOREIGN KEY (" + COLUMN_LANGUAGE_ID + ") REFERENCES " + TABLE_LANGUAGES + "(" + COLUMN_LANGUAGE_ID + "))";
        db.execSQL(createUserLanguagesTable);

        // Buat tabel Daily_Challenges
        String createDailyChallengesTable = "CREATE TABLE " + TABLE_DAILY_CHALLENGES + " (" +
                COLUMN_CHALLENGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LANGUAGE_ID + " INTEGER, " +
                COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_POINTS_REWARD + " INTEGER NOT NULL, " +
                COLUMN_START_TIME + " DATETIME NOT NULL, " +
                COLUMN_END_TIME + " DATETIME NOT NULL, " +
                "FOREIGN KEY (" + COLUMN_LANGUAGE_ID + ") REFERENCES " + TABLE_LANGUAGES + "(" + COLUMN_LANGUAGE_ID + "))";
        db.execSQL(createDailyChallengesTable);

        // Buat tabel User_Challenges
        String createUserChallengesTable = "CREATE TABLE " + TABLE_USER_CHALLENGES + " (" +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_CHALLENGE_ID + " INTEGER, " +
                COLUMN_COMPLETED + " INTEGER DEFAULT 0, " +
                COLUMN_COMPLETION_TIME + " DATETIME, " +
                "PRIMARY KEY (" + COLUMN_USER_ID + ", " + COLUMN_CHALLENGE_ID + "), " +
                "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "), " +
                "FOREIGN KEY (" + COLUMN_CHALLENGE_ID + ") REFERENCES " + TABLE_DAILY_CHALLENGES + "(" + COLUMN_CHALLENGE_ID + "))";
        db.execSQL(createUserChallengesTable);

        // Buat tabel Game_Questions
        String createGameQuestionsTable = "CREATE TABLE " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_QUESTION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_LANGUAGE_ID + " INTEGER, " +
                COLUMN_QUESTION_LEVEL + " INTEGER NOT NULL, " +
                COLUMN_QUESTION_TEXT + " TEXT NOT NULL, " +
                COLUMN_OPTION_1 + " TEXT NOT NULL, " +
                COLUMN_OPTION_2 + " TEXT NOT NULL, " +
                COLUMN_OPTION_3 + " TEXT NOT NULL, " +
                COLUMN_OPTION_4 + " TEXT NOT NULL, " +
                COLUMN_CORRECT_ANSWER + " TEXT NOT NULL, " +
                COLUMN_XP_REWARD + " INTEGER NOT NULL, " +
                COLUMN_IMAGE_URL + " TEXT, " +
                "FOREIGN KEY (" + COLUMN_LANGUAGE_ID + ") REFERENCES " + TABLE_LANGUAGES + "(" + COLUMN_LANGUAGE_ID + "))";
        db.execSQL(createGameQuestionsTable);

        // Buat tabel User_Game_Progress
        String createUserGameProgressTable = "CREATE TABLE " + TABLE_USER_GAME_PROGRESS + " (" +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_LANGUAGE_ID + " INTEGER, " +
                COLUMN_CURRENT_LEVEL + " INTEGER DEFAULT 1, " +
                COLUMN_TOTAL_XP + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COLUMN_USER_ID + ", " + COLUMN_LANGUAGE_ID + "), " +
                "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "), " +
                "FOREIGN KEY (" + COLUMN_LANGUAGE_ID + ") REFERENCES " + TABLE_LANGUAGES + "(" + COLUMN_LANGUAGE_ID + "))";
        db.execSQL(createUserGameProgressTable);

        // Buat tabel Daily_Missions dengan kolom last_updated
        String createDailyMissionsTable = "CREATE TABLE " + TABLE_DAILY_MISSIONS + " (" +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_END_TIME + " INTEGER, " +
                COLUMN_DAILY_PROGRESS + " INTEGER DEFAULT 0, " +
                COLUMN_LAST_UPDATED + " DATETIME, " + // Kolom baru
                "PRIMARY KEY (" + COLUMN_USER_ID + "), " +
                "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createDailyMissionsTable);

        // Buat tabel Weekly_Challenges
        String createWeeklyChallengesTable = "CREATE TABLE " + TABLE_WEEKLY_CHALLENGES + " (" +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_END_TIME + " INTEGER, " +
                COLUMN_WEEKLY_PROGRESS + " INTEGER DEFAULT 0, " +
                "PRIMARY KEY (" + COLUMN_USER_ID + "), " +
                "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createWeeklyChallengesTable);

        // Buat tabel Challenge_Modes
        String createChallengeModesTable = "CREATE TABLE " + TABLE_CHALLENGE_MODES + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY, " +
                COLUMN_SPEED_TARGET + " INTEGER DEFAULT 42, " +
                COLUMN_SPEED_QUESTIONS + " INTEGER DEFAULT 0, " +
                COLUMN_DUEL_TARGET + " INTEGER DEFAULT 5, " +
                COLUMN_DUEL_WINS + " INTEGER DEFAULT 0, " +
                COLUMN_SURVIVAL_TARGET + " INTEGER DEFAULT 8, " +
                COLUMN_SURVIVAL_LEVEL + " INTEGER DEFAULT 0, " +
                COLUMN_STORY_TARGET + " INTEGER DEFAULT 2, " +
                COLUMN_STORY_PROGRESS + " INTEGER DEFAULT 0, " +
                "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createChallengeModesTable);

        // Buat tabel XP_History
        String createXpHistoryTable = "CREATE TABLE " + TABLE_XP_HISTORY + " (" +
                COLUMN_USER_ID + " INTEGER, " +
                COLUMN_XP_GAINED + " INTEGER, " +
                COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createXpHistoryTable);

        // Inisialisasi tabel Languages
        db.execSQL("INSERT INTO " + TABLE_LANGUAGES + " (" + COLUMN_LANGUAGE_NAME + ", " + COLUMN_ICON_URL + ") VALUES ('Bahasa Inggris', '@drawable/inggris')");
        db.execSQL("INSERT INTO " + TABLE_LANGUAGES + " (" + COLUMN_LANGUAGE_NAME + ", " + COLUMN_ICON_URL + ") VALUES ('Bahasa Jepang', '@drawable/jepang')");
        db.execSQL("INSERT INTO " + TABLE_LANGUAGES + " (" + COLUMN_LANGUAGE_NAME + ", " + COLUMN_ICON_URL + ") VALUES ('Bahasa Korea', '@drawable/korea')");
        db.execSQL("INSERT INTO " + TABLE_LANGUAGES + " (" + COLUMN_LANGUAGE_NAME + ", " + COLUMN_ICON_URL + ") VALUES ('Bahasa Mandarin', '@drawable/mandarin')");

        // Inisialisasi tabel Game_Questions dengan lebih banyak data
        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 1, 'Apa arti kata \"pintu\"?', 'Window', 'Table', 'Door', 'Roof', 'Door', 10, '@drawable/soal_pintu')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 2, 'Apa arti kata \"anjing\"?', 'Dog', 'Cat', 'Bird', 'Fish', 'Dog', 10, '@drawable/soal_anjing')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 3, 'Apa arti kata \"kursi\"?', 'Chair', 'Bed', 'Closet', 'Desk', 'Chair', 10, '@drawable/soal_kursi')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 4, 'Apa arti kata \"buku\"?', 'Pencil', 'Book', 'Bag', 'Paper', 'Book', 10, '@drawable/soal_buku')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 5, 'Apa arti kata \"langit\"?', 'Ground', 'Sun', 'Sky', 'Moon', 'Sky', 10, '@drawable/soal_langit')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 6, 'Apa arti kata \"rumah\"?', 'House', 'Car', 'Shop', 'Fence', 'House', 10, '@drawable/soal_rumah')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 7, 'Apa arti kata \"apel\"?', 'Orange', 'Banana', 'Apple', 'Mango', 'Apple', 10, '@drawable/soal_apple')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 8, 'Apa arti kata \"sekolah\"?', 'Class', 'School', 'Office', 'Library', 'School', 10, '@drawable/soal_sekolah')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 9, 'Apa arti kata \"kucing\"?', 'Fish', 'Dog', 'Cow', 'Cat', 'Cat', 10, '@drawable/soal_kucing')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(1, 10, 'Apa arti kata \"jalan\"?', 'Road', 'Wall', 'Hill', 'Tree', 'Road', 10, '@drawable/soal_jalan')");


        //bahasa jepang
        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"api\"?', '水 (Mizu)', '火 (Hi)', '木 (Ki)', '金 (Kin)', '火 (Hi)', 10, '@drawable/soal_api')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"gunung\"?', '山 (Yama)', '川 (Kawa)', '森 (Mori)', '空 (Sora)', '山 (Yama)', 10, '@drawable/soal_gunung')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"mobil\"?', '電車 (Densha)', '車 (Kuruma)', '自転車 (Jitensha)', '飛行機 (Hikouki)', '車 (Kuruma)', 10, '@drawable/soal_mobil')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"kucing\"?', '犬 (Inu)', '猫 (Neko)', '馬 (Uma)', '鳥 (Tori)', '猫 (Neko)', 10, '@drawable/soal_kucing')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"hujan\"?', '雪 (Yuki)', '風 (Kaze)', '雨 (Ame)', '空 (Sora)', '雨 (Ame)', 10, '@drawable/soal_hujan')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"matahari\"?', '月 (Tsuki)', '星 (Hoshi)', '太陽 (Taiyō)', '空 (Sora)', '太陽 (Taiyō)', 10, '@drawable/soal_matahari')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"bunga\"?', '草 (Kusa)', '木 (Ki)', '花 (Hana)', '葉 (Ha)', '花 (Hana)', 10, '@drawable/soal_bunga')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"air laut\"?', '湖 (Mizuumi)', '海 (Umi)', '川 (Kawa)', '雨 (Ame)', '海 (Umi)', 10, '@drawable/soal_air_laut')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"tangan\"?', '足 (Ashi)', '手 (Te)', '目 (Me)', '耳 (Mimi)', '手 (Te)', 10, '@drawable/soal_tangan')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(2, 1, 'Apa arti kata \"bulan (langit)\"?', '月 (Tsuki)', '太陽 (Taiyō)', '火 (Hi)', '花 (Hana)', '月 (Tsuki)', 10, '@drawable/soal_bulan')");

        //Bahasa Mandarin
        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"matahari\"?', '月亮 (yuèliàng)', '星星 (xīngxing)', '太阳 (tàiyáng)', '火 (huǒ)', '太阳 (tàiyáng)', 10, '@drawable/soal_matahari')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"air\"?', '水 (shuǐ)', '火 (huǒ)', '风 (fēng)', '云 (yún)', '水 (shuǐ)', 10, '@drawable/soal_air')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"pohon\"?', '森林 (sēnlín)', '树 (shù)', '草 (cǎo)', '花 (huā)', '树 (shù)', 10, '@drawable/soal_pohon')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"burung\"?', '鸡 (jī)', '鸭 (yā)', '鸟 (niǎo)', '虎 (hǔ)', '鸟 (niǎo)', 10, '@drawable/soal_burung')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"kucing\"?', '狗 (gǒu)', '狼 (láng)', '猫 (māo)', '熊 (xióng)', '猫 (māo)', 10, '@drawable/soal_kucing')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"rumah\"?', '家 (jiā)', '学校 (xuéxiào)', '商店 (shāngdiàn)', '医院 (yīyuàn)', '家 (jiā)', 10, '@drawable/soal_rumah')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"bunga\"?', '草 (cǎo)', '树 (shù)', '花 (huā)', '木 (mù)', '花 (huā)', 10, '@drawable/soal_bunga')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"telinga\"?', '鼻子 (bízi)', '眼睛 (yǎnjing)', '耳朵 (ěrduo)', '嘴巴 (zuǐba)', '耳朵 (ěrduo)', 10, '@drawable/soal_telinga')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"laut\"?', '湖 (hú)', '河 (hé)', '海 (hǎi)', '雨 (yǔ)', '海 (hǎi)', 10, '@drawable/soal_laut')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(3, 1, 'Apa arti kata \"jalan\"?', '路 (lù)', '门 (mén)', '桌子 (zhuōzi)', '椅子 (yǐzi)', '路 (lù)', 10, '@drawable/soal_jalan')");

        //Bahasa Korea
        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"air\"?', '불 (bul)', '물 (mul)', '바람 (baram)', '땅 (ttang)', '물 (mul)', 10, '@drawable/soal_air')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"mata\"?', '손 (son)', '코 (ko)', '눈 (nun)', '귀 (gwi)', '눈 (nun)', 10, '@drawable/soal_mata')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"bunga\"?', '꽃 (kkot)', '잎 (ip)', '나무 (namu)', '풀 (pul)', '꽃 (kkot)', 10, '@drawable/soal_bunga')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"anjing\"?', '고양이 (goyang-i)', '강아지 (gangaji)', '돼지 (dwaeji)', '곰 (gom)', '강아지 (gangaji)', 10, '@drawable/soal_anjing')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"mobil\"?', '비행기 (bihaenggi)', '자전거 (jajeongeo)', '자동차 (jadongcha)', '배 (bae)', '자동차 (jadongcha)', 10, '@drawable/soal_mobil')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"rumah\"?', '집 (jip)', '학교 (hakgyo)', '문 (mun)', '방 (bang)', '집 (jip)', 10, '@drawable/soal_rumah')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"bulan\"?', '태양 (taeyang)', '별 (byeol)', '달 (dal)', '하늘 (haneul)', '달 (dal)', 10, '@drawable/soal_bulan')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"jalan\"?', '길 (gil)', '집 (jip)', '공원 (gongwon)', '나무 (namu)', '길 (gil)', 10, '@drawable/soal_jalan')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"matahari\"?', '별 (byeol)', '달 (dal)', '태양 (taeyang)', '구름 (gureum)', '태양 (taeyang)', 10, '@drawable/soal_matahari')");

        db.execSQL("INSERT INTO " + TABLE_GAME_QUESTIONS + " (" +
                COLUMN_LANGUAGE_ID + ", " + COLUMN_QUESTION_LEVEL + ", " + COLUMN_QUESTION_TEXT + ", " +
                COLUMN_OPTION_1 + ", " + COLUMN_OPTION_2 + ", " + COLUMN_OPTION_3 + ", " + COLUMN_OPTION_4 + ", " +
                COLUMN_CORRECT_ANSWER + ", " + COLUMN_XP_REWARD + ", " + COLUMN_IMAGE_URL + ") VALUES " +
                "(4, 1, 'Apa arti kata \"kursi\"?', '탁자 (takja)', '의자 (uija)', '문 (mun)', '책 (chaek)', '의자 (uija)', 10, '@drawable/soal_kursi')");

        // Inisialisasi akun admin
        ContentValues adminValues = new ContentValues();
        adminValues.put(COLUMN_USERNAME, "admin");
        adminValues.put(COLUMN_EMAIL, "admin@lingoquest.com");
        adminValues.put(COLUMN_PASSWORD, "admin123");
        adminValues.put(COLUMN_IS_ADMIN, 1);
        db.insert(TABLE_USERS, null, adminValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 3) {
            // Tambahkan tabel xp_history jika versi database kurang dari 3
            String createXpHistoryTable = "CREATE TABLE " + TABLE_XP_HISTORY + " (" +
                    COLUMN_USER_ID + " INTEGER, " +
                    COLUMN_XP_GAINED + " INTEGER, " +
                    COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
            db.execSQL(createXpHistoryTable);
        }

        if (oldVersion < 4) {
            // Tambahkan kolom last_updated ke TABLE_DAILY_MISSIONS
            db.execSQL("ALTER TABLE " + TABLE_DAILY_MISSIONS + " ADD COLUMN " + COLUMN_LAST_UPDATED + " DATETIME");
        }
    }

    // Metode untuk mencatat XP ke xp_history
    public void recordXpGain(int userId, int xpGained) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_XP_GAINED, xpGained);
        long result = db.insert(TABLE_XP_HISTORY, null, values);
        Log.d("DatabaseHelper", "XP recorded: userId=" + userId + ", xpGained=" + xpGained + ", result=" + result);

        // Update total points di user_stats
        int currentPoints = getTotalXp(userId);
        ContentValues statsValues = new ContentValues();
        statsValues.put(COLUMN_POINTS, currentPoints + xpGained);
        int rowsAffected = db.update(TABLE_USER_STATS, statsValues, COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        Log.d("DatabaseHelper", "user_stats updated: userId=" + userId + ", newPoints=" + (currentPoints + xpGained) + ", rowsAffected=" + rowsAffected);

        // Update weekly_xp di user_stats
        int currentWeeklyXp = getWeeklyXp(userId);
        ContentValues weeklyValues = new ContentValues();
        weeklyValues.put(COLUMN_WEEKLY_XP, currentWeeklyXp + xpGained);
        rowsAffected = db.update(TABLE_USER_STATS, weeklyValues, COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        Log.d("DatabaseHelper", "weekly_xp updated: userId=" + userId + ", newWeeklyXp=" + (currentWeeklyXp + xpGained) + ", rowsAffected=" + rowsAffected);

    }

    // Metode untuk mengambil total XP pengguna
    public int getTotalXp(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_POINTS + " FROM " + TABLE_USER_STATS +
                " WHERE " + COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        int totalXp = 0;
        if (cursor.moveToFirst()) {
            totalXp = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_POINTS));
        }
        cursor.close();
        return totalXp;
    }

    // Metode untuk mengambil weekly_xp pengguna
    public int getWeeklyXp(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_WEEKLY_XP + " FROM " + TABLE_USER_STATS +
                " WHERE " + COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        int weeklyXp = 0;
        if (cursor.moveToFirst()) {
            weeklyXp = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEEKLY_XP));
        }
        cursor.close();
        return weeklyXp;
    }

    // Metode untuk mengambil total XP berdasarkan periode waktu
    public Cursor getXpByPeriod(String period, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query;
        switch (period) {
            case "Harian":
                query = "SELECT u.user_id, u.username, u.avatar_url, SUM(xh.xp_gained) as total_xp " +
                        "FROM " + TABLE_USERS + " u " +
                        "LEFT JOIN " + TABLE_XP_HISTORY + " xh ON u.user_id = xh.user_id " +
                        "WHERE xh.timestamp >= datetime('now', '-1 day') " +
                        "GROUP BY u.user_id, u.username, u.avatar_url " +
                        "ORDER BY total_xp DESC LIMIT ?";
                break;
            case "Mingguan":
                query = "SELECT u.user_id, u.username, u.avatar_url, SUM(xh.xp_gained) as total_xp " +
                        "FROM " + TABLE_USERS + " u " +
                        "LEFT JOIN " + TABLE_XP_HISTORY + " xh ON u.user_id = xh.user_id " +
                        "WHERE xh.timestamp >= datetime('now', '-7 days') " +
                        "GROUP BY u.user_id, u.username, u.avatar_url " +
                        "ORDER BY total_xp DESC LIMIT ?";
                break;
            case "Bulanan":
                query = "SELECT u.user_id, u.username, u.avatar_url, SUM(xh.xp_gained) as total_xp " +
                        "FROM " + TABLE_USERS + " u " +
                        "LEFT JOIN " + TABLE_XP_HISTORY + " xh ON u.user_id = xh.user_id " +
                        "WHERE xh.timestamp >= datetime('now', '-30 days') " +
                        "GROUP BY u.user_id, u.username, u.avatar_url " +
                        "ORDER BY total_xp DESC LIMIT ?";
                break;
            default:
                query = "SELECT u.user_id, u.username, u.avatar_url, SUM(xh.xp_gained) as total_xp " +
                        "FROM " + TABLE_USERS + " u " +
                        "LEFT JOIN " + TABLE_XP_HISTORY + " xh ON u.user_id = xh.user_id " +
                        "GROUP BY u.user_id, u.username, u.avatar_url " +
                        "ORDER BY total_xp DESC LIMIT ?";
        }
        return db.rawQuery(query, new String[]{String.valueOf(limit)});
    }

    // Metode untuk reset weekly_xp semua pengguna
    public void resetWeeklyXp() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_WEEKLY_XP, 0);
        db.update(TABLE_USER_STATS, values, null, null);
        Log.d("DatabaseHelper", "Weekly XP reset for all users");
    }

    // Metode untuk memeriksa apakah misi harian selesai
    public boolean isDailyMissionCompleted(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_DAILY_PROGRESS + " FROM " + TABLE_DAILY_MISSIONS +
                        " WHERE " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        boolean completed = false;
        if (cursor.moveToFirst()) {
            int progress = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DAILY_PROGRESS));
            completed = progress >= 10; // Target 10 soal
        }
        cursor.close();
        return completed;
    }

    // Metode untuk memeriksa apakah misi mingguan selesai
    public boolean isWeeklyMissionCompleted(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_WEEKLY_PROGRESS + " FROM " + TABLE_WEEKLY_CHALLENGES +
                        " WHERE " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        boolean completed = false;
        if (cursor.moveToFirst()) {
            int progress = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_WEEKLY_PROGRESS));
            completed = progress >= 50; // Target 50 soal
        }
        cursor.close();
        return completed;
    }

    public long saveUser(String username, String email, String password, String avatarUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_AVATAR_URL, avatarUrl);
        values.put(COLUMN_IS_ADMIN, 0);

        long userId = db.insert(TABLE_USERS, null, values);

        if (userId != -1) {
            ContentValues statsValues = new ContentValues();
            statsValues.put(COLUMN_USER_ID, userId);
            statsValues.put(COLUMN_LEVEL, 1);
            statsValues.put(COLUMN_POINTS, 0);
            statsValues.put(COLUMN_STREAK_DAYS, 0);
            statsValues.put(COLUMN_CORRECT_ANSWERS, 0);
            statsValues.put(COLUMN_IS_WEEKLY_CHAMPION, 0);
            statsValues.put(COLUMN_WEEKLY_XP, 0);
            db.insert(TABLE_USER_STATS, null, statsValues);

            ContentValues challengeModesValues = new ContentValues();
            challengeModesValues.put(COLUMN_USER_ID, userId);
            challengeModesValues.put(COLUMN_SPEED_TARGET, 42);
            challengeModesValues.put(COLUMN_SPEED_QUESTIONS, 0);
            challengeModesValues.put(COLUMN_DUEL_TARGET, 5);
            challengeModesValues.put(COLUMN_DUEL_WINS, 0);
            challengeModesValues.put(COLUMN_SURVIVAL_TARGET, 8);
            challengeModesValues.put(COLUMN_SURVIVAL_LEVEL, 0);
            challengeModesValues.put(COLUMN_STORY_TARGET, 2);
            challengeModesValues.put(COLUMN_STORY_PROGRESS, 0);
            db.insert(TABLE_CHALLENGE_MODES, null, challengeModesValues);

            // Inisialisasi dengan semua bahasa untuk user baru
            String[] defaultLanguages = {"Bahasa Inggris", "Bahasa Jepang", "Bahasa Korea", "Bahasa Mandarin"};
            for (String languageName : defaultLanguages) {
                int languageId = getLanguageId(languageName);
                if (languageId != -1) {
                    ContentValues userLangValues = new ContentValues();
                    userLangValues.put(COLUMN_USER_ID, userId);
                    userLangValues.put(COLUMN_LANGUAGE_ID, languageId);
                    userLangValues.put(COLUMN_PROGRESS, 0);
                    db.insert(TABLE_USER_LANGUAGES, null, userLangValues);

                    ContentValues progressValues = new ContentValues();
                    progressValues.put(COLUMN_USER_ID, userId);
                    progressValues.put(COLUMN_LANGUAGE_ID, languageId);
                    progressValues.put(COLUMN_CURRENT_LEVEL, 1);
                    progressValues.put(COLUMN_TOTAL_XP, 0);
                    db.insert(TABLE_USER_GAME_PROGRESS, null, progressValues);
                }
            }

            // Inisialisasi daily_missions dan weekly_challenges
            ContentValues dailyMissionValues = new ContentValues();
            dailyMissionValues.put(COLUMN_USER_ID, userId);
            dailyMissionValues.put(COLUMN_END_TIME, System.currentTimeMillis() + 86400000); // 1 hari ke depan
            dailyMissionValues.put(COLUMN_DAILY_PROGRESS, 0);
            dailyMissionValues.put(COLUMN_LAST_UPDATED, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
            db.insert(TABLE_DAILY_MISSIONS, null, dailyMissionValues);

            ContentValues weeklyChallengeValues = new ContentValues();
            weeklyChallengeValues.put(COLUMN_USER_ID, userId);
            weeklyChallengeValues.put(COLUMN_END_TIME, System.currentTimeMillis() + 604800000); // 1 minggu ke depan
            weeklyChallengeValues.put(COLUMN_WEEKLY_PROGRESS, 0);
            db.insert(TABLE_WEEKLY_CHALLENGES, null, weeklyChallengeValues);
        }
        return userId;
    }

    public String saveImageToInternalStorage(Context context, Uri imageUri, int userId) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;
            File dir = context.getFilesDir();
            File file = new File(dir, "profile_" + userId + ".jpg");
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error saving image: " + e.getMessage());
            return null;
        }
    }

    public void updateUserAvatar(int userId, String avatarPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AVATAR_URL, avatarPath);
        db.update(TABLE_USERS, values, COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});

    }

    public long saveGameQuestion(int languageId, int level, String questionText, String option1, String option2,
                                 String option3, String option4, String correctAnswer, int xpReward, String imageUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (languageId <= 0 || level <= 0 || questionText == null || correctAnswer == null ||
                option1 == null || option2 == null || option3 == null || option4 == null || xpReward < 0) {
            Log.w("DatabaseHelper", "Invalid input for saveGameQuestion: languageId=" + languageId + ", level=" + level);
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_LANGUAGE_ID, languageId);
        values.put(COLUMN_QUESTION_LEVEL, level);
        values.put(COLUMN_QUESTION_TEXT, questionText);
        values.put(COLUMN_OPTION_1, option1);
        values.put(COLUMN_OPTION_2, option2);
        values.put(COLUMN_OPTION_3, option3);
        values.put(COLUMN_OPTION_4, option4);
        values.put(COLUMN_CORRECT_ANSWER, correctAnswer);
        values.put(COLUMN_XP_REWARD, xpReward);
        values.put(COLUMN_IMAGE_URL, imageUrl);

        long result = db.insert(TABLE_GAME_QUESTIONS, null, values);
        return result;
    }

    public Cursor getGameQuestions(int languageId, int level) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (languageId <= 0 || level <= 0) {
            Log.w("DatabaseHelper", "Invalid languageId or level: " + languageId + ", " + level);
            return null;
        }
        return db.rawQuery("SELECT * FROM " + TABLE_GAME_QUESTIONS +
                        " WHERE " + COLUMN_LANGUAGE_ID + " = ? AND " + COLUMN_QUESTION_LEVEL + " = ?",
                new String[]{String.valueOf(languageId), String.valueOf(level)});
    }

    public void updateUserGameProgress(int userId, int languageId, int currentLevel, int totalXp) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (userId <= 0 || languageId <= 0 || currentLevel < 0 || totalXp < 0) {
            Log.w("DatabaseHelper", "Invalid input for updateUserGameProgress: userId=" + userId + ", languageId=" + languageId);
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_CURRENT_LEVEL, currentLevel);
        values.put(COLUMN_TOTAL_XP, totalXp);

        int rowsAffected = db.update(TABLE_USER_GAME_PROGRESS, values,
                COLUMN_USER_ID + " = ? AND " + COLUMN_LANGUAGE_ID + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(languageId)});

        if (rowsAffected == 0) {
            values.put(COLUMN_USER_ID, userId);
            values.put(COLUMN_LANGUAGE_ID, languageId);
            db.insert(TABLE_USER_GAME_PROGRESS, null, values);
        }
    }

    public Cursor getUserGameProgress(int userId, int languageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USER_GAME_PROGRESS +
                        " WHERE " + COLUMN_USER_ID + " = ? AND " + COLUMN_LANGUAGE_ID + " = ?",
                new String[]{String.valueOf(userId), String.valueOf(languageId)});
    }

    public boolean isAdmin(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_IS_ADMIN + " FROM " + TABLE_USERS +
                " WHERE " + COLUMN_USER_ID + " = ?", new String[]{String.valueOf(userId)});
        boolean isAdmin = false;
        if (cursor.moveToFirst()) {
            isAdmin = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ADMIN)) == 1;
        }
        cursor.close();
        return isAdmin;
    }

    public int getLanguageId(String languageName) {
        SQLiteDatabase db = this.getReadableDatabase();
        if (languageName == null || languageName.trim().isEmpty()) {
            Log.w("DatabaseHelper", "Invalid languageName: " + languageName);
            return -1;
        }
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_LANGUAGE_ID + " FROM " + TABLE_LANGUAGES +
                " WHERE " + COLUMN_LANGUAGE_NAME + " = ?", new String[]{languageName});
        int languageId = -1;
        if (cursor.moveToFirst()) {
            languageId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LANGUAGE_ID));
        } else {
            Log.w("DatabaseHelper", "Language not found: " + languageName);
        }
        cursor.close();
        return languageId;
    }

    public Cursor getUserData(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
    }

    public Cursor getLeaderboardData(String filter) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT u.user_id, u.username, u.avatar_url, us.points, ugp.current_level " +
                "FROM users u " +
                "JOIN user_stats us ON u.user_id = us.user_id " +
                "JOIN user_game_progress ugp ON u.user_id = ugp.user_id " +
                "WHERE us.last_active IS NOT NULL";
        return db.rawQuery(query, null);
    }
}