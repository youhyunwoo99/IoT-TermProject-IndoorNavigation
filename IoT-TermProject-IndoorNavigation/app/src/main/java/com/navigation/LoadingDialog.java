package com.navigation;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;

public class LoadingDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage("현재 위치 탐색 중...");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        // 뒤로가기 버튼 이벤트를 직접 처리
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                // 뒤로가기 버튼을 눌렀을 때의 이벤트를 캐치
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;  // 이벤트를 여기에서 처리하고 더 이상 전파하지 않음
                }
                return false;  // 다른 키 이벤트는 시스템에게 전달
            }
        });
        return dialog;
    }
}
