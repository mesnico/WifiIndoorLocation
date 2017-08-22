package com.unipi.nicola.indoorlocator.UI;

/**
 * Created by Nicola on 20/08/2017.
 */

import android.content.Context;
import android.util.AttributeSet;

public class CustomSummaryEditTextPreference extends android.preference.EditTextPreference{
    public CustomSummaryEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomSummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSummaryEditTextPreference(Context context) {
        super(context);
    }

    @Override
    public CharSequence getSummary() {
        String summary = super.getSummary().toString();
        return String.format(summary, getText());
    }
}
