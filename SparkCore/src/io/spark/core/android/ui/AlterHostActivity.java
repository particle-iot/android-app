package io.spark.core.android.ui;

import static org.solemnsilence.util.Py.list;
import static org.solemnsilence.util.Py.truthy;
import io.spark.core.android.R;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.cloud.requestservice.SimpleSparkApiService;
import io.spark.core.android.ui.corelist.CoreListActivity;
import io.spark.core.android.ui.util.Ui;
import io.spark.core.android.util.NetConnectionHelper;

import org.apache.http.HttpStatus;
import org.solemnsilence.util.EZ;
import org.solemnsilence.util.TLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AlterHostActivity extends BaseActivity {

	// UI references.
	private EditText mHostServerAddress;
	private EditText mHostServerPort;
	private Button alterHostAction;

	private String hostaddress;
	private String hostport;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_alter_host);

		mHostServerAddress = Ui.findView(this, R.id.host_server);
		mHostServerPort = Ui.findView(this, R.id.host_port);

		alterHostAction = Ui.findView(this, R.id.alter_host_button);
		alterHostAction.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				// ToDo: write back into DB;
				finish();
			}
		});

		/*
		TextView noAccountYet = Ui.setTextFromHtml(this, R.id.no_account_yet,
				R.string.i_dont_have_an_account);
		noAccountYet.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(AlterHostActivity.this, SignUpActivity.class).putExtra(
						SignUpActivity.EXTRA_FROM_ALTERHOST, ""));
				finish();
			}
		});

		TextView haveAccount = Ui.setTextFromHtml(this,
				R.id.already_have_account, R.string.i_already_have_an_account);
		haveAccount.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(AlterHostActivity.this,
						LoginActivity.class));
				finish();
			}
		});
		*/

		// set up touch listeners on form fields, to auto scroll when the
		// keyboard pops up
		for (View view : list(mHostServerAddress, mHostServerPort)) {
			view.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						scrollAccountForm();
					}
					return false;
				}
			});

		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void scrollAccountForm() {
		// delay this until after the keyboard has popped up so the scroll view
		// is actually able to scroll
		Runnable scrollRunnable = new Runnable() {

			@Override
			public void run() {
				ScrollView scrollArea = Ui.findView(AlterHostActivity.this,
						R.id.scroll_area);
				// using fullScroll() or pageScroll() impacts which child widget
				// gets focus, so that doesn't work here, instead just scroll by
				// an absurdly large number.
				scrollArea.scrollBy(0, 5000);
			}
		};
		// Doing this twice because sometimes it takes a moment for the keyboard
		// to pop up, changing the screen dimensions and allowing scrolling
		EZ.runOnMainThreadDelayed(scrollRunnable, 150);
		EZ.runOnMainThreadDelayed(scrollRunnable, 750);
	}

	private void showProgress(boolean show) {
		super.showProgress(R.id.progress_indicator, show);
		alterHostAction.setEnabled(!show);
	}

	static final TLog log = new TLog(AlterHostActivity.class);
}
