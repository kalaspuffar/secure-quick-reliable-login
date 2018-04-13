package org.ea.sqrl.activites;

import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;

import org.ea.sqrl.R;
import org.ea.sqrl.processors.SQRLStorage;

/**
 * This activity is used to inform the user about the different features, techniques use cases
 * of SQRL. This knowledge base should give the user a good understanding of the client application.
 * Using a scrollable page adapter we can show multiple pages of text to read.
 *
 * @author Daniel Persson
 */
public class IntroductionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction);

        final SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        final ViewPager mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.tab_dots);
        tabLayout.setupWithViewPager(mViewPager, true);

        Button btnClose = findViewById(R.id.btnCloseIntroduction);
        btnClose.setOnClickListener(v -> new Thread(() -> {
            IntroductionActivity.this.finish();
        }).start());
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_introduction, container, false);
            TextView textView = rootView.findViewById(R.id.section_label);
            textView.setMovementMethod(LinkMovementMethod.getInstance());

            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 0:
                    textView.setText(getString(R.string.introduction_startpage));
                    break;
                case 1:
                    textView.setText(getString(R.string.introduction_nutshell));
                    break;
                case 2:
                    textView.setText(getString(R.string.introduction_password));
                    break;
                case 3:
                    textView.setText(getString(R.string.introduction_rescue_code));
                    break;
                case 4:
                    textView.setText(getString(R.string.introduction_backup));
                    break;
                default:
                    textView.setText("Well this is embarrassing, nothing to see where. You should not even be able to see this. EASTER EGG! :)");
                    break;
            }
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 5;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SQRLStorage.getInstance().clear();
    }
}
