package io.spark.core.android.ui.assets;

import static org.solemnsilence.util.Py.map;

import java.util.Map;

import android.content.Context;
import android.graphics.Typeface;

public class Typefaces {
// **** to avoid having to alter font file names in font_names.xml AND Typefaces.java *********************
//	// NOTE: this is tightly coupled to the filenames in assets/fonts
//	public static enum Style {
//  		BOLD("gotham_bold.otf"),
//		BOLD_ITALIC("gotham_bold_ita.otf"),
//		BOOK("gotham_book.otf"),
//		BOOK_ITALIC("gotham_book_ita.otf"),
//		LIGHT("gotham_light.otf"),
//		LIGHT_ITALIC("gotham_light_ita.otf"),
//		MEDIUM("gotham_medium.otf"),
//  		MEDIUM_ITALIC("gotham_medium_ita.otf");
//		
//		public final String fileName;
//
//		private Style(String name) {
//			fileName = name;
//		}
//	}
//
//	private static final Map<Style, Typeface> typefaces = map();
//
//	public static Typeface getTypeface(Context ctx, Style style) {
//		Typeface face = typefaces.get(style);
//		if (face == null) {
//			face = Typeface.createFromAsset(ctx.getAssets(), "fonts/" + style.fileName);
//			typefaces.put(style, face);
//		}
//		return face;
//	}
// ********************************************************************************************************
//
// instead go for the resource IDs
	
	private static final Map<String, Typeface> typefaces = map();

	public static Typeface getTypeface(Context ctx, int fontID) {
		Typeface face = typefaces.get(ctx.getString(fontID));
		if (face == null) {
			face = Typeface.createFromAsset(ctx.getAssets(), ctx.getString(fontID));
			typefaces.put(ctx.getString(fontID), face);
		}
		return face;
	}
	
// or even for the font file names
	
	public static Typeface getTypeface(Context ctx, String fontName) {
		Typeface face = typefaces.get(fontName);
		if (face == null) {
			face = Typeface.createFromAsset(ctx.getAssets(), fontName);
			typefaces.put(fontName, face);
		}
		return face;
	}
}
