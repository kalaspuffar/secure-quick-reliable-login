package org.ea.sqrl.activites;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.base.CommonBaseActivity;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Daniel Persson
 */
public class LanguageActivity extends CommonBaseActivity {
    private static final String TAG = "LanguageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);

        RecyclerView lstLanguageSelector = findViewById(R.id.lstLanguageSelector);

        List<Language> languages = new ArrayList<>();
        languages.add(new Language(R.string.language_default, ""));
        languages.add(new Language(R.string.language_arabic, "ar"));
        languages.add(new Language(R.string.language_catalan, "ca"));
        languages.add(new Language(R.string.language_chinese, "zh"));
        languages.add(new Language(R.string.language_czech, "cs"));
        languages.add(new Language(R.string.language_dutch, "nl"));
        languages.add(new Language(R.string.language_english, "en"));
        languages.add(new Language(R.string.language_french, "fr"));
        languages.add(new Language(R.string.language_german, "de"));
        languages.add(new Language(R.string.language_hebrew, "he"));
        languages.add(new Language(R.string.language_hungarian, "hu"));
        languages.add(new Language(R.string.language_japanese, "ja"));
        languages.add(new Language(R.string.language_norwegian, "no"));
        languages.add(new Language(R.string.language_russian, "ru"));
        languages.add(new Language(R.string.language_slovenian, "sl"));
        languages.add(new Language(R.string.language_swedish, "sv"));
        languages.add(new Language(R.string.language_spanish, "es"));



        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        String prefLanguage = sharedPreferences.getString("language", "");

        int i = 0;
        for(Language lang : languages) {
            if(prefLanguage.equals(lang.getCode())) break;
            i++;
        }

        LanguageAdapter adapter = new LanguageAdapter(languages, i);
        // Attach the adapter to the recyclerview to populate items
        lstLanguageSelector.setAdapter(adapter);
        // Set layout manager to position the items
        lstLanguageSelector.setLayoutManager(new LinearLayoutManager(this));

        ImageView moreIndicator = findViewById(R.id.more_indicator);
        lstLanguageSelector.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if(!lstLanguageSelector.canScrollVertically(1)) {
                moreIndicator.setVisibility(View.INVISIBLE);
            } else {
                moreIndicator.setVisibility(View.VISIBLE);
            }
        });
    }

    private class Language {
        private int name;
        private String code;

        public Language(int name, String code) {
            this.name = name;
            this.code = code;
        }

        public int getName() {
            return name;
        }

        public String getCode() {
            return code;
        }
    }

    private class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
        private int focusedItem;
        private List<Language> languageList;

        // Pass in the contact array into the constructor
        public LanguageAdapter(List<Language> languageList, int selected) {
            this.languageList = languageList;
            this.focusedItem = selected;
        }

        @Override
        public LanguageAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);

            // Inflate the custom layout
            View languageView = inflater.inflate(R.layout.language_item, parent, false);

            // Return a new holder instance
            ViewHolder viewHolder = new ViewHolder(languageView);
            return viewHolder;
        }

        // Involves populating data into the item through holder
        @Override
        public void onBindViewHolder(LanguageAdapter.ViewHolder viewHolder, int position) {
            // Get the data model based on position
            Language language = languageList.get(position);

            // Set item views based on your views and data model
            TextView languageTextView = viewHolder.languageTextView;
            languageTextView.setText(language.getName());

            viewHolder.languageImageView.setImageResource(
                    focusedItem == position ?
                        android.R.drawable.btn_star_big_on :
                        android.R.drawable.btn_star_big_off
            );
        }

        // Returns the total count of items in the list
        @Override
        public int getItemCount() {
            return languageList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            // Your holder should contain a member variable
            // for any view that will be set as you render a row
            public TextView languageTextView;
            public ImageView languageImageView;

            // We also create a constructor that accepts the entire item row
            // and does the view lookups to find each subview
            public ViewHolder(View itemView) {
                // Stores the itemView in a public final member variable that can be used
                // to access the context from any ViewHolder instance.
                super(itemView);

                languageTextView = itemView.findViewById(R.id.languageText);
                languageImageView = itemView.findViewById(R.id.languageStar);

                itemView.setOnClickListener(v -> {
                    if (getAdapterPosition() == RecyclerView.NO_POSITION) return;

                    notifyItemChanged(focusedItem);
                    focusedItem = getLayoutPosition();
                    notifyItemChanged(focusedItem);

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(LanguageActivity.this.getApplicationContext());
                    String selectedLang = languageList.get(focusedItem).getCode();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("language", selectedLang);
                    editor.apply();

                    LanguageActivity.this.finish();
                });
            }
        }
    }
}
