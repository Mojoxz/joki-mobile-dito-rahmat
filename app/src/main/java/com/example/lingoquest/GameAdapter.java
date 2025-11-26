package com.example.lingoquest;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;

public class GameAdapter extends FragmentStateAdapter {

    private final List<String> languages;
    private final GameFragment.OnQuestionAnsweredListener listener;

    public GameAdapter(@NonNull FragmentActivity fragmentActivity, List<String> languages, GameFragment.OnQuestionAnsweredListener listener) {
        super(fragmentActivity);
        this.languages = languages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return GameFragment.newInstance(languages.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }
}