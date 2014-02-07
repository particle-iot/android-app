package io.spark.core.android.ui.assets;

import static org.solemnsilence.util.Py.map;

import java.util.Map;

import android.content.Context;
import android.graphics.Typeface;


public class Typefaces {

	// NOTE: this is tightly coupled to the filenames in assets/fonts
	public static enum Style {
		BOLD("gotham_bold.otf"),
		BOLD_ITALIC("gotham_bold_ita.otf"),
		BOOK("gotham_book.otf"),
		BOOK_ITALIC("gotham_book_ita.otf"),
		LIGHT("gotham_light.otf"),
		LIGHT_ITALIC("gotham_light_ita.otf"),
		MEDIUM("gotham_medium.otf"),
		MEDIUM_ITALIC("gotham_medium_ita.otf");

		public final String fileName;

		private Style(String name) {
			fileName = name;
		}
	}


	private static final Map<Style, Typeface> typefaces = map();


	public static Typeface getTypeface(Context ctx, Style style) {
		Typeface face = typefaces.get(style);
		if (face == null) {
			face = Typeface.createFromAsset(ctx.getAssets(), "fonts/" + style.fileName);
			typefaces.put(style, face);
		}
		return face;
	}

}
