package com.eaydin79.notes;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

public class ConfirmDialog {

    private final AlertDialog alertDialog;
    private static final long HOLD_DURATION = 1000;
    private boolean isHolding = false;

    @SuppressLint("InflateParams")
    public ConfirmDialog(Context context, @NonNull Runnable confirmAction) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.layout_confirm, null, false);
        alertDialog = new AlertDialog.Builder(context).setTitle(R.string.confirm_dialog_title).setView(view).create();
        alertDialog.show();

        ProgressBar progressConfirm = view.findViewById(R.id.progressConfirm);
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressConfirm, "progress", 0, 100).setDuration(HOLD_DURATION);
        progressAnimator.setInterpolator(new LinearInterpolator());

        Runnable runnableConfirm = () -> {
            if (!isHolding) return;
            alertDialog.dismiss();
            confirmAction.run();
        };

        view.findViewById(R.id.txtCancel).setOnClickListener(v -> alertDialog.dismiss());
        view.findViewById(R.id.txtConfirm).setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isHolding = true;
                    v.postDelayed(runnableConfirm, HOLD_DURATION);
                    v.setPressed(true);
                    progressAnimator.start();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    isHolding = false;
                    v.removeCallbacks(runnableConfirm);
                    v.setPressed(false);
                    progressAnimator.cancel();
                    ObjectAnimator.ofInt(progressConfirm, "progress", progressConfirm.getProgress(), 0).setDuration(200).start();
                    v.performClick();
                    return true;
            }
            return false;
        });
    }

}
