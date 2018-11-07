package com.omniremotes.remoteverify.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.omniremotes.remoteverify.R;

public class PairingDialog extends Dialog {
    public PairingDialog(Context context){
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
    }

    private void initUI(){
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        View view = layoutInflater.inflate(R.layout.pairing_dialog_layout,null);
        setContentView(view);
        TextView titleTextView = view.findViewById(R.id.dialog_title);
        titleTextView.setText("Pairing Test");
        Window window = getWindow();
        if(window==null){
            return;
        }
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        DisplayMetrics d = getContext().getResources().getDisplayMetrics();
        layoutParams.width = (int) (d.widthPixels * 1.0);
        layoutParams.height = (int) (d.heightPixels*0.5);
        window.setAttributes(layoutParams);
    }
}
