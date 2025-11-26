package com.example.lingoquest;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseDataInitializer {
    private static final String TAG = "FirebaseInit";
    private FirebaseFirestore db;

    public FirebaseDataInitializer() {
        db = FirebaseFirestore.getInstance();
    }

    public void initializeLanguages() {
        String[][] languages = {
                {"Bahasa Inggris", "@drawable/inggris"},
                {"Bahasa Jepang", "@drawable/jepang"},
                {"Bahasa Korea", "@drawable/korea"},
                {"Bahasa Mandarin", "@drawable/mandarin"}
        };

        for (String[] lang : languages) {
            Map<String, Object> language = new HashMap<>();
            language.put("language_name", lang[0]);
            language.put("icon_url", lang[1]);

            db.collection("languages")
                    .add(language)
                    .addOnSuccessListener(docRef ->
                            Log.d(TAG, "Language added: " + lang[0] + " with ID: " + docRef.getId()))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error adding language: " + lang[0], e));
        }
    }

    public void initializeQuestions() {
        // Bahasa Inggris Questions
        addEnglishQuestions();

        // Bahasa Jepang Questions
        addJapaneseQuestions();

        // Bahasa Mandarin Questions
        addMandarinQuestions();

        // Bahasa Korea Questions
        addKoreanQuestions();
    }

    private void addEnglishQuestions() {
        Object[][] questions = {
                {1, "Apa arti kata \"pintu\"?", "Window", "Table", "Door", "Roof", "Door", 10, "@drawable/soal_pintu"},
                {2, "Apa arti kata \"anjing\"?", "Dog", "Cat", "Bird", "Fish", "Dog", 10, "@drawable/soal_anjing"},
                {3, "Apa arti kata \"kursi\"?", "Chair", "Bed", "Closet", "Desk", "Chair", 10, "@drawable/soal_kursi"},
                {4, "Apa arti kata \"buku\"?", "Pencil", "Book", "Bag", "Paper", "Book", 10, "@drawable/soal_buku"},
                {5, "Apa arti kata \"langit\"?", "Ground", "Sun", "Sky", "Moon", "Sky", 10, "@drawable/soal_langit"},
                {6, "Apa arti kata \"rumah\"?", "House", "Car", "Shop", "Fence", "House", 10, "@drawable/soal_rumah"},
                {7, "Apa arti kata \"apel\"?", "Orange", "Banana", "Apple", "Mango", "Apple", 10, "@drawable/soal_apple"},
                {8, "Apa arti kata \"sekolah\"?", "Class", "School", "Office", "Library", "School", 10, "@drawable/soal_sekolah"},
                {9, "Apa arti kata \"kucing\"?", "Fish", "Dog", "Cow", "Cat", "Cat", 10, "@drawable/soal_kucing"},
                {10, "Apa arti kata \"jalan\"?", "Road", "Wall", "Hill", "Tree", "Road", 10, "@drawable/soal_jalan"}
        };

        getLanguageIdAndAddQuestions("Bahasa Inggris", questions);
    }

    private void addJapaneseQuestions() {
        Object[][] questions = {
                {1, "Apa arti kata \"api\"?", "水 (Mizu)", "火 (Hi)", "木 (Ki)", "金 (Kin)", "火 (Hi)", 10, "@drawable/soal_api"},
                {1, "Apa arti kata \"gunung\"?", "山 (Yama)", "川 (Kawa)", "森 (Mori)", "空 (Sora)", "山 (Yama)", 10, "@drawable/soal_gunung"},
                {1, "Apa arti kata \"mobil\"?", "電車 (Densha)", "車 (Kuruma)", "自転車 (Jitensha)", "飛行機 (Hikouki)", "車 (Kuruma)", 10, "@drawable/soal_mobil"},
                {1, "Apa arti kata \"kucing\"?", "犬 (Inu)", "猫 (Neko)", "馬 (Uma)", "鳥 (Tori)", "猫 (Neko)", 10, "@drawable/soal_kucing"},
                {1, "Apa arti kata \"hujan\"?", "雪 (Yuki)", "風 (Kaze)", "雨 (Ame)", "空 (Sora)", "雨 (Ame)", 10, "@drawable/soal_hujan"},
                {1, "Apa arti kata \"matahari\"?", "月 (Tsuki)", "星 (Hoshi)", "太陽 (Taiyō)", "空 (Sora)", "太陽 (Taiyō)", 10, "@drawable/soal_matahari"},
                {1, "Apa arti kata \"bunga\"?", "草 (Kusa)", "木 (Ki)", "花 (Hana)", "葉 (Ha)", "花 (Hana)", 10, "@drawable/soal_bunga"},
                {1, "Apa arti kata \"air laut\"?", "湖 (Mizuumi)", "海 (Umi)", "川 (Kawa)", "雨 (Ame)", "海 (Umi)", 10, "@drawable/soal_air_laut"},
                {1, "Apa arti kata \"tangan\"?", "足 (Ashi)", "手 (Te)", "目 (Me)", "耳 (Mimi)", "手 (Te)", 10, "@drawable/soal_tangan"},
                {1, "Apa arti kata \"bulan (langit)\"?", "月 (Tsuki)", "太陽 (Taiyō)", "火 (Hi)", "花 (Hana)", "月 (Tsuki)", 10, "@drawable/soal_bulan"}
        };

        getLanguageIdAndAddQuestions("Bahasa Jepang", questions);
    }

    private void addMandarinQuestions() {
        Object[][] questions = {
                {1, "Apa arti kata \"matahari\"?", "月亮 (yuèliàng)", "星星 (xīngxing)", "太阳 (tàiyáng)", "火 (huǒ)", "太阳 (tàiyáng)", 10, "@drawable/soal_matahari"},
                {1, "Apa arti kata \"air\"?", "水 (shuǐ)", "火 (huǒ)", "风 (fēng)", "云 (yún)", "水 (shuǐ)", 10, "@drawable/soal_air"},
                {1, "Apa arti kata \"pohon\"?", "森林 (sēnlín)", "树 (shù)", "草 (cǎo)", "花 (huā)", "树 (shù)", 10, "@drawable/soal_pohon"},
                {1, "Apa arti kata \"burung\"?", "鸡 (jī)", "鸭 (yā)", "鸟 (niǎo)", "虎 (hǔ)", "鸟 (niǎo)", 10, "@drawable/soal_burung"},
                {1, "Apa arti kata \"kucing\"?", "狗 (gǒu)", "狼 (láng)", "猫 (māo)", "熊 (xióng)", "猫 (māo)", 10, "@drawable/soal_kucing"},
                {1, "Apa arti kata \"rumah\"?", "家 (jiā)", "学校 (xuéxiào)", "商店 (shāngdiàn)", "医院 (yīyuàn)", "家 (jiā)", 10, "@drawable/soal_rumah"},
                {1, "Apa arti kata \"bunga\"?", "草 (cǎo)", "树 (shù)", "花 (huā)", "木 (mù)", "花 (huā)", 10, "@drawable/soal_bunga"},
                {1, "Apa arti kata \"telinga\"?", "鼻子 (bízi)", "眼睛 (yǎnjing)", "耳朵 (ěrduo)", "嘴巴 (zuǐba)", "耳朵 (ěrduo)", 10, "@drawable/soal_telinga"},
                {1, "Apa arti kata \"laut\"?", "湖 (hú)", "河 (hé)", "海 (hǎi)", "雨 (yǔ)", "海 (hǎi)", 10, "@drawable/soal_laut"},
                {1, "Apa arti kata \"jalan\"?", "路 (lù)", "门 (mén)", "桌子 (zhuōzi)", "椅子 (yǐzi)", "路 (lù)", 10, "@drawable/soal_jalan"}
        };

        getLanguageIdAndAddQuestions("Bahasa Mandarin", questions);
    }

    private void addKoreanQuestions() {
        Object[][] questions = {
                {1, "Apa arti kata \"air\"?", "불 (bul)", "물 (mul)", "바람 (baram)", "땅 (ttang)", "물 (mul)", 10, "@drawable/soal_air"},
                {1, "Apa arti kata \"mata\"?", "손 (son)", "코 (ko)", "눈 (nun)", "귀 (gwi)", "눈 (nun)", 10, "@drawable/soal_mata"},
                {1, "Apa arti kata \"bunga\"?", "꽃 (kkot)", "잎 (ip)", "나무 (namu)", "풀 (pul)", "꽃 (kkot)", 10, "@drawable/soal_bunga"},
                {1, "Apa arti kata \"anjing\"?", "고양이 (goyang-i)", "강아지 (gangaji)", "돼지 (dwaeji)", "곰 (gom)", "강아지 (gangaji)", 10, "@drawable/soal_anjing"},
                {1, "Apa arti kata \"mobil\"?", "비행기 (bihaenggi)", "자전거 (jajeongeo)", "자동차 (jadongcha)", "배 (bae)", "자동차 (jadongcha)", 10, "@drawable/soal_mobil"},
                {1, "Apa arti kata \"rumah\"?", "집 (jip)", "학교 (hakgyo)", "문 (mun)", "방 (bang)", "집 (jip)", 10, "@drawable/soal_rumah"},
                {1, "Apa arti kata \"bulan\"?", "태양 (taeyang)", "별 (byeol)", "달 (dal)", "하늘 (haneul)", "달 (dal)", 10, "@drawable/soal_bulan"},
                {1, "Apa arti kata \"jalan\"?", "길 (gil)", "집 (jip)", "공원 (gongwon)", "나무 (namu)", "길 (gil)", 10, "@drawable/soal_jalan"},
                {1, "Apa arti kata \"matahari\"?", "별 (byeol)", "달 (dal)", "태양 (taeyang)", "구름 (gureum)", "태양 (taeyang)", 10, "@drawable/soal_matahari"},
                {1, "Apa arti kata \"kursi\"?", "탁자 (takja)", "의자 (uija)", "문 (mun)", "책 (chaek)", "의자 (uija)", 10, "@drawable/soal_kursi"}
        };

        getLanguageIdAndAddQuestions("Bahasa Korea", questions);
    }

    private void getLanguageIdAndAddQuestions(String languageName, Object[][] questions) {
        db.collection("languages")
                .whereEqualTo("language_name", languageName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String languageId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        for (Object[] q : questions) {
                            Map<String, Object> question = new HashMap<>();
                            question.put("language_id", languageId);
                            question.put("question_level", q[0]);
                            question.put("question_text", q[1]);
                            question.put("option_1", q[2]);
                            question.put("option_2", q[3]);
                            question.put("option_3", q[4]);
                            question.put("option_4", q[5]);
                            question.put("correct_answer", q[6]);
                            question.put("xp_reward", q[7]);
                            question.put("image_url", q[8]);

                            db.collection("game_questions")
                                    .add(question)
                                    .addOnSuccessListener(docRef ->
                                            Log.d(TAG, "Question added for " + languageName))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error adding question", e));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error getting language: " + languageName, e));
    }

    // ==================== LISTENING QUESTIONS ====================

    public void initializeListeningQuestions() {
        addEnglishListeningQuestions();
        addJapaneseListeningQuestions();
        addKoreanListeningQuestions();
        addMandarinListeningQuestions();
    }

    private void addEnglishListeningQuestions() {
        Object[][] questions = {
                {1, "What did you hear?", "audio_url_here", "Hello", "Goodbye", "Thank you", "Sorry", "Hello", 10},
                {1, "What greeting did you hear?", "audio_url_here", "Good morning", "Good night", "Good afternoon", "Good evening", "Good morning", 10},
                {1, "What number did you hear?", "audio_url_here", "One", "Two", "Three", "Four", "Two", 10},
                {1, "What color was mentioned?", "audio_url_here", "Red", "Blue", "Green", "Yellow", "Blue", 10},
                {1, "What animal did you hear?", "audio_url_here", "Dog", "Cat", "Bird", "Fish", "Cat", 10}
        };

        getLanguageIdAndAddListeningQuestions("Bahasa Inggris", questions);
    }

    private void addJapaneseListeningQuestions() {
        Object[][] questions = {
                {1, "日本語で何と言いましたか？", "audio_url_here", "こんにちは (Konnichiwa)", "さようなら (Sayounara)", "ありがとう (Arigatou)", "すみません (Sumimasen)", "こんにちは (Konnichiwa)", 10},
                {1, "何の数字を聞きましたか？", "audio_url_here", "一 (Ichi)", "二 (Ni)", "三 (San)", "四 (Shi)", "二 (Ni)", 10},
                {1, "何色と言いましたか？", "audio_url_here", "赤 (Aka)", "青 (Ao)", "緑 (Midori)", "黄色 (Kiiro)", "青 (Ao)", 10}
        };

        getLanguageIdAndAddListeningQuestions("Bahasa Jepang", questions);
    }

    private void addKoreanListeningQuestions() {
        Object[][] questions = {
                {1, "무엇을 들었습니까?", "audio_url_here", "안녕하세요 (Annyeonghaseyo)", "안녕히 가세요 (Annyeonghi gaseyo)", "감사합니다 (Gamsahamnida)", "미안합니다 (Mianhamnida)", "안녕하세요 (Annyeonghaseyo)", 10},
                {1, "어떤 숫자를 들었습니까?", "audio_url_here", "하나 (Hana)", "둘 (Dul)", "셋 (Set)", "넷 (Net)", "둘 (Dul)", 10}
        };

        getLanguageIdAndAddListeningQuestions("Bahasa Korea", questions);
    }

    private void addMandarinListeningQuestions() {
        Object[][] questions = {
                {1, "你听到了什么？", "audio_url_here", "你好 (Nǐ hǎo)", "再见 (Zàijiàn)", "谢谢 (Xièxie)", "对不起 (Duìbùqǐ)", "你好 (Nǐ hǎo)", 10},
                {1, "你听到了什么数字？", "audio_url_here", "一 (Yī)", "二 (Èr)", "三 (Sān)", "四 (Sì)", "二 (Èr)", 10}
        };

        getLanguageIdAndAddListeningQuestions("Bahasa Mandarin", questions);
    }

    private void getLanguageIdAndAddListeningQuestions(String languageName, Object[][] questions) {
        db.collection("languages")
                .whereEqualTo("language_name", languageName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String languageId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        for (Object[] q : questions) {
                            Map<String, Object> question = new HashMap<>();
                            question.put("language_id", languageId);
                            question.put("question_level", q[0]);
                            question.put("question_text", q[1]);
                            question.put("audio_url", q[2]);
                            question.put("option_1", q[3]);
                            question.put("option_2", q[4]);
                            question.put("option_3", q[5]);
                            question.put("option_4", q[6]);
                            question.put("correct_answer", q[7]);
                            question.put("xp_reward", q[8]);

                            db.collection("listening_questions")
                                    .add(question)
                                    .addOnSuccessListener(docRef ->
                                            Log.d(TAG, "Listening question added for " + languageName))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error adding listening question", e));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error getting language: " + languageName, e));
    }

    // ==================== READING QUESTIONS ====================

    public void initializeReadingQuestions() {
        addEnglishReadingQuestions();
        addJapaneseReadingQuestions();
        addKoreanReadingQuestions();
        addMandarinReadingQuestions();
    }

    private void addEnglishReadingQuestions() {
        Object[][] questions = {
                {1, "My name is John. I am a student. I study English every day.",
                        "What is John?",
                        "A teacher", "A student", "A doctor", "A worker", "A student", 15},

                {1, "The cat is sleeping on the sofa. It is very comfortable.",
                        "Where is the cat?",
                        "On the bed", "On the sofa", "On the floor", "On the table", "On the sofa", 15},

                {1, "I wake up at 7 AM every morning. Then I have breakfast and go to school.",
                        "What time does the person wake up?",
                        "6 AM", "7 AM", "8 AM", "9 AM", "7 AM", 15},

                {1, "My favorite color is blue. I like blue because it reminds me of the ocean.",
                        "What is the person's favorite color?",
                        "Red", "Green", "Blue", "Yellow", "Blue", 15}
        };

        getLanguageIdAndAddReadingQuestions("Bahasa Inggris", questions);
    }

    private void addJapaneseReadingQuestions() {
        Object[][] questions = {
                {1, "私の名前は田中です。私は学生です。毎日日本語を勉強しています。",
                        "田中さんは何ですか？",
                        "先生", "学生", "医者", "会社員", "学生", 15},

                {1, "猫がソファで寝ています。とても気持ちよさそうです。",
                        "猫はどこにいますか？",
                        "ベッドの上", "ソファの上", "床の上", "テーブルの上", "ソファの上", 15}
        };

        getLanguageIdAndAddReadingQuestions("Bahasa Jepang", questions);
    }

    private void addKoreanReadingQuestions() {
        Object[][] questions = {
                {1, "저는 김민수입니다. 저는 학생입니다. 매일 한국어를 공부합니다.",
                        "김민수는 무엇입니까?",
                        "선생님", "학생", "의사", "회사원", "학생", 15},

                {1, "고양이가 소파에서 자고 있습니다. 매우 편안해 보입니다.",
                        "고양이는 어디에 있습니까?",
                        "침대 위", "소파 위", "바닥 위", "테이블 위", "소파 위", 15}
        };

        getLanguageIdAndAddReadingQuestions("Bahasa Korea", questions);
    }

    private void addMandarinReadingQuestions() {
        Object[][] questions = {
                {1, "我叫李明。我是学生。我每天学习中文。",
                        "李明是什么？",
                        "老师", "学生", "医生", "工人", "学生", 15},

                {1, "猫在沙发上睡觉。它看起来很舒服。",
                        "猫在哪里？",
                        "床上", "沙发上", "地板上", "桌子上", "沙发上", 15}
        };

        getLanguageIdAndAddReadingQuestions("Bahasa Mandarin", questions);
    }

    private void getLanguageIdAndAddReadingQuestions(String languageName, Object[][] questions) {
        db.collection("languages")
                .whereEqualTo("language_name", languageName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String languageId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        for (Object[] q : questions) {
                            Map<String, Object> question = new HashMap<>();
                            question.put("language_id", languageId);
                            question.put("question_level", q[0]);
                            question.put("reading_text", q[1]);
                            question.put("question_text", q[2]);
                            question.put("option_1", q[3]);
                            question.put("option_2", q[4]);
                            question.put("option_3", q[5]);
                            question.put("option_4", q[6]);
                            question.put("correct_answer", q[7]);
                            question.put("xp_reward", q[8]);

                            db.collection("reading_questions")
                                    .add(question)
                                    .addOnSuccessListener(docRef ->
                                            Log.d(TAG, "Reading question added for " + languageName))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error adding reading question", e));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error getting language: " + languageName, e));
    }

    // ==================== ADMIN ACCOUNT ====================

    public void initializeAdminAccount() {
        // Check if admin exists first
        db.collection("users")
                .whereEqualTo("email", "admin@lingoquest.com")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Create admin via FirebaseHelper
                        FirebaseHelper helper = FirebaseHelper.getInstance();
                        helper.registerUser("admin@lingoquest.com", "admin123", "admin",
                                null, new FirebaseHelper.AuthCallback() {
                                    @Override
                                    public void onSuccess(String userId) {
                                        // Update to admin
                                        db.collection("users").document(userId)
                                                .update("is_admin", true)
                                                .addOnSuccessListener(aVoid ->
                                                        Log.d(TAG, "Admin account created"))
                                                .addOnFailureListener(e ->
                                                        Log.e(TAG, "Error setting admin flag", e));
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        Log.e(TAG, "Error creating admin: " + error);
                                    }
                                });
                    } else {
                        Log.d(TAG, "Admin account already exists");
                    }
                });
    }

    // ==================== INITIALIZE ALL DATA ====================

    public void initializeAllData() {
        initializeLanguages();

        // Wait a bit for languages to be created, then add questions
        new android.os.Handler().postDelayed(() -> {
            initializeQuestions();
            initializeListeningQuestions();
            initializeReadingQuestions();
        }, 2000);

        initializeAdminAccount();
    }
}