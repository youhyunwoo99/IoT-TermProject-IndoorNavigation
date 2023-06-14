package com.navigation;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;

public class MyLocationDialogFragment extends DialogFragment {
    private String location;
    private TextView location_tv;
    private MyLocationDialogListener listener;

    public interface MyLocationDialogListener {
        void onDialogResult(boolean result);
    }

    public MyLocationDialogFragment(String location, MyLocationDialogListener listener){
        this.location = location;
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_location_dialog, container, false);
        Button yesBtn = view.findViewById(R.id.btn_yes);
        Button noBtn = view.findViewById(R.id.btn_no);
        location_tv = view.findViewById(R.id.location);
        location_tv.setText(location);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDialogResult(true);
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDialogResult(false);
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
        return view;
    }
}