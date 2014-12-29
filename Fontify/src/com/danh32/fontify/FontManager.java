package com.danh32.fontify;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.danh32.fontify.R;

public class FontManager {
	private static FontManager sInstance;
	private Map<String, Typeface> mCache;
	private static int sDefaultStyle = R.attr.fontifyStyle;

	private FontManager() {
		mCache = new HashMap<String, Typeface>();
	}

	public static FontManager getInstance() {
		if (sInstance == null) {
			sInstance = new FontManager();
		}

		return sInstance;
	}

	public void setFont(TextView tv, AttributeSet attrs) {
		String fontName = getFontName(tv.getContext(), attrs);
		setFont(tv, fontName);
	}
	
	public void setFont(TextView tv, String fontName) {
		if (fontName == null) return;
		
		Typeface typeface = mCache.get(fontName);
		if (typeface == null) {
            try {
                typeface = Typeface.createFromAsset(tv.getContext().getAssets(), fontName);
            } catch (Exception e) {
                Log.w("FontManager", String.format("Font %s not available. Please add missing font to assets/fonts directory", fontName));
                typeface = Typeface.DEFAULT;
            }
			mCache.put(fontName, typeface);
		}
		
		tv.setTypeface(typeface);
	}
	
	public static String getFontName(Context c, AttributeSet attrs) {
		TypedArray arr = c.obtainStyledAttributes(
			attrs,
			R.styleable.Fontify,
			sDefaultStyle,
			0
		);

		String fontName = arr.getString(R.styleable.Fontify_font);
		arr.recycle();
		return fontName;
	}
}
