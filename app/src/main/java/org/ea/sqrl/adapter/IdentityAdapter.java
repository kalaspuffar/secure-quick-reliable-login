package org.ea.sqrl.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.ea.sqrl.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IdentityAdapter implements SpinnerAdapter {

    private class Identity {
        long id;
        String name;

        Identity(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private List<Identity> identityList = new ArrayList<>();

    public IdentityAdapter(Map<Long, String> identities) {
        for(Map.Entry<Long, String> entry : identities.entrySet()) {
            identityList.add(new Identity(entry.getKey(), entry.getValue()));
        }
    }

    private View createTextView(String text, Context context) {
        TextView view = (TextView)LayoutInflater.from(context).inflate(R.layout.simple_spinner_item, null);
        view.setText(text);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createTextView(identityList.get(position).name, parent.getContext());
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {}

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {}

    @Override
    public int getCount() {
        return identityList.size();
    }

    @Override
    public Object getItem(int position) {
        return identityList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return identityList.get(position).id;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createTextView(identityList.get(position).name, parent.getContext());
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
