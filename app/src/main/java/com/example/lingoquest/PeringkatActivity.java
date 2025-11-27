package com.example.lingoquest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeringkatActivity extends AppCompatActivity {

    private static final String TAG = "PeringkatActivity";
    private FirebaseHelper firebaseHelper;
    private String userId;
    private TabLayout tabLayout;
    private LinearLayout userItems4To10;
    private TextView userRankText, userXpText, userName1, userName2, userName3;
    private TextView userXp1, userXp2, userXp3;
    private ImageView userProfile1, userProfile2, userProfile3;
    private ProgressBar userProgressBar;
    private ImageView userProfileImage;
    private BottomNavigationView bottomNavigationView;
    private String currentFilter = "Harian";
    private RankNotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_peringkat);

        firebaseHelper = FirebaseHelper.getInstance();
        notificationHelper = new RankNotificationHelper(this);

        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", Context.MODE_PRIVATE);
        userId = prefs.getString("user_id", null);

        if (userId == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Schedule rank notifications
        notificationHelper.scheduleRankNotification();

        initializeViews();
        setupBottomNavigation();
        setupTabLayout();
        loadCurrentUserProfileImage();
        loadLeaderboard();
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tab_layout);
        userItems4To10 = findViewById(R.id.user_items_4_10);
        userRankText = findViewById(R.id.user_rank_text);
        userXpText = findViewById(R.id.user_xp_text);
        userProgressBar = findViewById(R.id.user_progress_bar);
        userProfileImage = findViewById(R.id.user_profile_image);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Top 3 users
        userName1 = findViewById(R.id.name1);
        userName2 = findViewById(R.id.name2);
        userName3 = findViewById(R.id.name3);
        userXp1 = findViewById(R.id.level_xp1);
        userXp2 = findViewById(R.id.level_xp2);
        userXp3 = findViewById(R.id.level_xp3);
        userProfile1 = findViewById(R.id.profile_image1);
        userProfile2 = findViewById(R.id.profile_image2);
        userProfile3 = findViewById(R.id.profile_image3);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_peringkat);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            NavigationItem navItem = NavigationItem.fromItemId(item.getItemId());
            if (navItem == null) return false;

            switch (navItem) {
                case NAV_HOME:
                    startActivity(new Intent(this, MainActivity.class));
                    return true;
                case NAV_BELAJAR:
                    startActivity(new Intent(this, BelajarActivity.class));
                    return true;
                case NAV_TANTANGAN:
                    startActivity(new Intent(this, TantanganActivity.class));
                    return true;
                case NAV_PERINGKAT:
                    return true;
                case NAV_PROFIL:
                    startActivity(new Intent(this, ProfilActivity.class));
                    return true;
                default:
                    return false;
            }
        });
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getText().toString();
                loadLeaderboard();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadLeaderboard() {
        userItems4To10.removeAllViews();

        // Tampilkan loading
        Toast.makeText(this, "Memuat peringkat...", Toast.LENGTH_SHORT).show();

        int limit = currentFilter.equals("Harian") ? 50 : 100;

        firebaseHelper.getLeaderboard(currentFilter, limit, new FirebaseHelper.LeaderboardCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> leaderboard) {
                if (leaderboard == null || leaderboard.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(PeringkatActivity.this,
                                "Belum ada data peringkat",
                                Toast.LENGTH_SHORT).show();
                        displayEmptyLeaderboard();
                    });
                    return;
                }

                runOnUiThread(() -> displayLeaderboard(leaderboard));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(PeringkatActivity.this,
                            "Gagal memuat peringkat: " + error,
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Leaderboard error: " + error);
                    displayEmptyLeaderboard();
                });
            }
        });
    }

    private void displayEmptyLeaderboard() {
        // Reset top 3
        userName1.setText("-");
        userName2.setText("-");
        userName3.setText("-");
        userXp1.setText("0 XP");
        userXp2.setText("0 XP");
        userXp3.setText("0 XP");
        userProfile1.setImageResource(R.drawable.default_avatar);
        userProfile2.setImageResource(R.drawable.default_avatar);
        userProfile3.setImageResource(R.drawable.default_avatar);

        // Reset user rank
        userRankText.setText("Peringkat: -");
        userXpText.setText("0 XP");
        userProgressBar.setProgress(0);
    }

    private void displayLeaderboard(List<Map<String, Object>> leaderboard) {
        // Display top 3
        if (leaderboard.size() > 0) updateTopUser(1, leaderboard.get(0));
        if (leaderboard.size() > 1) updateTopUser(2, leaderboard.get(1));
        if (leaderboard.size() > 2) updateTopUser(3, leaderboard.get(2));

        // Display ranks 4-10
        for (int i = 3; i < Math.min(10, leaderboard.size()); i++) {
            addUserItem(leaderboard.get(i), i + 1);
        }

        // Update current user rank
        updateUserRank(leaderboard);

        // Check for rank changes and show notification
        checkRankChanges(leaderboard);
    }

    private void updateTopUser(int rank, Map<String, Object> userData) {
        String username = (String) userData.get("username");
        Long totalXp = (Long) userData.get("total_xp");
        String avatarUrl = (String) userData.get("avatar_url");

        TextView nameView = null;
        TextView xpView = null;
        ImageView profileView = null;

        switch (rank) {
            case 1:
                nameView = userName1;
                xpView = userXp1;
                profileView = userProfile1;
                break;
            case 2:
                nameView = userName2;
                xpView = userXp2;
                profileView = userProfile2;
                break;
            case 3:
                nameView = userName3;
                xpView = userXp3;
                profileView = userProfile3;
                break;
        }

        if (nameView != null && xpView != null && profileView != null) {
            nameView.setText(username != null ? username : "User " + rank);

            double xpInK = totalXp != null ? totalXp / 1000.0 : 0;
            xpView.setText(String.format("%.1fK XP", xpInK));

            loadProfileImage(profileView, avatarUrl);
        }
    }

    private void addUserItem(Map<String, Object> userData, int rank) {
        ConstraintLayout itemView = new ConstraintLayout(this);
        itemView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        itemView.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        // Rank badge
        TextView rankText = new TextView(this);
        rankText.setId(View.generateViewId());
        ConstraintLayout.LayoutParams rankParams = new ConstraintLayout.LayoutParams(
                dpToPx(40), dpToPx(40));
        rankText.setLayoutParams(rankParams);
        rankText.setBackgroundResource(R.drawable.circle_purple);
        rankText.setText("#" + rank);
        rankText.setTextColor(getResources().getColor(android.R.color.white));
        rankText.setGravity(android.view.Gravity.CENTER);
        rankText.setTextSize(14);
        itemView.addView(rankText);

        // Profile image
        ImageView profileImage = new ImageView(this);
        profileImage.setId(View.generateViewId());
        ConstraintLayout.LayoutParams imageParams = new ConstraintLayout.LayoutParams(
                dpToPx(50), dpToPx(50));
        profileImage.setLayoutParams(imageParams);
        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        String avatarUrl = (String) userData.get("avatar_url");
        loadProfileImage(profileImage, avatarUrl);
        itemView.addView(profileImage);

        // Username
        TextView nameText = new TextView(this);
        nameText.setId(View.generateViewId());
        ConstraintLayout.LayoutParams nameParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        nameText.setLayoutParams(nameParams);
        String username = (String) userData.get("username");
        nameText.setText(username != null ? username : "User " + rank);
        nameText.setTextSize(16);
        nameText.setTextColor(getResources().getColor(android.R.color.black));
        itemView.addView(nameText);

        // XP text
        TextView xpText = new TextView(this);
        xpText.setId(View.generateViewId());
        ConstraintLayout.LayoutParams xpParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        xpText.setLayoutParams(xpParams);
        Long totalXp = (Long) userData.get("total_xp");
        double xpInK = totalXp != null ? totalXp / 1000.0 : 0;
        xpText.setText(String.format("%.1fK XP", xpInK));
        xpText.setTextSize(14);
        xpText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        itemView.addView(xpText);

        // Apply constraints
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(itemView);

        // Rank badge constraints
        constraintSet.connect(rankText.getId(), ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
        constraintSet.connect(rankText.getId(), ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.TOP, dpToPx(8));
        constraintSet.connect(rankText.getId(), ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(8));

        // Profile image constraints
        constraintSet.connect(profileImage.getId(), ConstraintSet.START,
                rankText.getId(), ConstraintSet.END, dpToPx(12));
        constraintSet.connect(profileImage.getId(), ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.TOP, dpToPx(4));
        constraintSet.connect(profileImage.getId(), ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, dpToPx(4));

        // Username constraints
        constraintSet.connect(nameText.getId(), ConstraintSet.START,
                profileImage.getId(), ConstraintSet.END, dpToPx(12));
        constraintSet.connect(nameText.getId(), ConstraintSet.TOP,
                profileImage.getId(), ConstraintSet.TOP, 0);

        // XP text constraints
        constraintSet.connect(xpText.getId(), ConstraintSet.START,
                profileImage.getId(), ConstraintSet.END, dpToPx(12));
        constraintSet.connect(xpText.getId(), ConstraintSet.TOP,
                nameText.getId(), ConstraintSet.BOTTOM, dpToPx(2));

        constraintSet.applyTo(itemView);

        userItems4To10.addView(itemView);
    }

    private void updateUserRank(List<Map<String, Object>> leaderboard) {
        int userRank = -1;
        long userXp = 0;
        long topXp = 0;

        if (leaderboard.size() > 0) {
            Long firstXp = (Long) leaderboard.get(0).get("total_xp");
            topXp = firstXp != null ? firstXp : 0;
        }

        for (int i = 0; i < leaderboard.size(); i++) {
            String uid = (String) leaderboard.get(i).get("user_id");
            if (uid != null && uid.equals(userId)) {
                userRank = i + 1;
                Long xp = (Long) leaderboard.get(i).get("total_xp");
                userXp = xp != null ? xp : 0;
                break;
            }
        }

        updateUserRankUI(userRank, userXp, topXp);
    }

    private void updateUserRankUI(int userRank, long userXp, long topXp) {
        if (userRank != -1) {
            userRankText.setText("Peringkat Anda #" + userRank);
            double xpInK = userXp / 1000.0;
            userXpText.setText(String.format("%.1fK XP", xpInK));

            if (userRank == 1 || topXp == 0) {
                userProgressBar.setProgress(100);
            } else {
                float progress = (float) userXp / topXp * 100;
                progress = Math.max(0, Math.min(100, progress));
                userProgressBar.setProgress((int) progress);
            }
        } else {
            userRankText.setText("Belum masuk peringkat");
            userXpText.setText("0 XP");
            userProgressBar.setProgress(0);
        }
    }

    private void checkRankChanges(List<Map<String, Object>> leaderboard) {
        int currentRank = -1;
        for (int i = 0; i < leaderboard.size(); i++) {
            String uid = (String) leaderboard.get(i).get("user_id");
            if (uid != null && uid.equals(userId)) {
                currentRank = i + 1;
                break;
            }
        }

        if (currentRank == -1) return;

        SharedPreferences prefs = getSharedPreferences("LingoQuestPrefs", MODE_PRIVATE);
        int previousRank = prefs.getInt("previous_rank_" + currentFilter, currentRank);
        int daysStuck = prefs.getInt("days_stuck_" + currentFilter, 0);

        if (currentRank == previousRank) {
            daysStuck++;
            if (daysStuck >= 2) {
                firebaseHelper.getUserData(userId, new FirebaseHelper.UserDataCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> userData) {
                        // Notification akan ditangani oleh RankNotificationWorker
                    }

                    @Override
                    public void onFailure(String error) {}
                });
            }
        } else {
            daysStuck = 0;
        }

        prefs.edit()
                .putInt("previous_rank_" + currentFilter, currentRank)
                .putInt("days_stuck_" + currentFilter, daysStuck)
                .apply();
    }

    private void loadCurrentUserProfileImage() {
        firebaseHelper.getUserData(userId, new FirebaseHelper.UserDataCallback() {
            @Override
            public void onSuccess(Map<String, Object> userData) {
                String avatarUrl = (String) userData.get("avatar_url");
                runOnUiThread(() -> loadProfileImage(userProfileImage, avatarUrl));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> userProfileImage.setImageResource(R.drawable.default_avatar));
            }
        });
    }

    private void loadProfileImage(ImageView imageView, String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.default_avatar);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}