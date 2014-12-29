#Fontify

Fontify is an Android library project providing drop-in replacements for all Android TextView subclasses, allowing developers to apply custom fonts via xml layouts and/or styles.

##Set Up:
1) Clone Fontify locally  
2) Import library project into Eclipse  
3) Add Fontify to your app as a library project  

##Usage:
###Getting fonts in place:
1) Put your custom font file in Android's assets folder (assets/fonts/helvetica.ttf)  
2) Put the path to your file in your strings.xml for ease of use:  

	<string name="FONT_HELVETICA">fonts/helvetica.ttf</string>

###Applying fonts:
####In styles/themes:
1) Declare your style in res/values/styles.xml:  

    <style name="CustomTextViewStyle">
        <item name="font">@string/FONT_HELVETICA</item>
    </style>
2) Use the Fontify subclass of your TextView:

	<com.danh32.fontify.TextView 
	    android:layout_width="match_parent"
		android:layout_height="wrap_content"
		style="@style/CustomTextViewStyle" />

3) You can also declare a default Fontify font as part of your Application theme so that every Fontified element will use the same font:

a. Declare the style as in (1) above

b. While still in res/values/styles.xml, add the following attribute to your Application theme:

    <!-- Application theme. Android Studio default example - your theme names may differ -->
    <style name="AppTheme" parent="AppBaseTheme">
        <item name="fontifyStyle">@style/CustomTextViewStyle</item>
    </style>

c. In your layouts, use the Fontify subclass of your element as in (2) above, but omit the style attribute if you want the default font from the theme to apply

	  <!-- This element will use the default CustomTextViewStyle from AppTheme
	       since no specific style with the font attribute is specified -->
 	  <com.danh32.fontify.TextView
	      android:layout_width="match_parent"
		    android:layout_height="wrap_content"
	  />

####In layouts:  
1) Add new XML NameSpace to root element of layout:  

    xmlns:fontify="http://schemas.android.com/apk/res-auto"  
2) Use the Fontify subclass of your TextView:  

	<com.danh32.fontify.TextView 
	    android:layout_width="match_parent"
		android:layout_height="wrap_content"
		fontify:font="@string/FONT_HELVETICA" />
		
####In Java:
Use the textView.setFont(String fontPath) or textView.setFont(int resId) method:  

	import com.danh32.fontify.TextView;

	public class CustomFontActivity extends Activity {
		@Override
		onCreate (Bundle savedInstanceState) {
			super(savedInstanceState);
		
			TextView tv = new TextView(this);
			tv.setFont(R.string.FONT_HELVETICA);
		}	
	}

##License
	Copyright 2013 Daniel Hill

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
