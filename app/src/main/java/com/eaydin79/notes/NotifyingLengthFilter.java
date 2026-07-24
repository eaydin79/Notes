package com.eaydin79.notes;

import android.text.InputFilter;
import android.text.Spanned;

public class NotifyingLengthFilter implements InputFilter {

    private final int maxLength;
    private final Runnable onTruncated;
    public NotifyingLengthFilter(int maxLength, Runnable onTruncated) {
        this.maxLength = maxLength;
        this.onTruncated = onTruncated;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        int keep = maxLength - (dest.length() - (dend-dstart));
        if (keep <= 0) {
            onTruncated.run();
            return "";
        }
        if (keep < end-start) {
            onTruncated.run();
            return source.subSequence(start, start+keep);
        }
        return null;
    }

}
