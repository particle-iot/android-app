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


public class LoginActivity extends BaseActivity {

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private Button accountAction;

	private String email;
	private String password;

	private NetConnectionHelper netConnectionHelper;

	private LoggedInReceiver loginReceiver = new LoggedInReceiver();
	private DevicesLoadedReceiver devicesLoadedReceiver = new DevicesLoadedReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_log_in);

		mEmailView = Ui.findView(this, R.id.email);
		mPasswordView = Ui.findView(this, R.id.password);

		mEmailView.setText(prefs.getUsername());

		netConnectionHelper = new NetConnectionHelper(this);

		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		accountAction = Ui.findView(this, R.id.sign_up_button);
		accountAction.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});


		// set up touch listeners on form fields, to auto scroll when the
		// keyboard pops up
		for (View view : list(mEmailView, mPasswordView)) {
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


		TextView noAccountYet = Ui.setTextFromHtml(this, R.id.no_account_yet,
				R.string.i_dont_have_an_account);
		noAccountYet.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				launchSignUpActivity();
			}
		});
		TextView forgotPassword = Ui.setTextFromHtml(this, R.id.forgot_password,
				R.string.action_forgot_password);
		forgotPassword.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				openUri(R.string.uri_forgot_password);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		broadcastMgr.registerReceiver(loginReceiver, loginReceiver.getFilter());
	}

	@Override
	protected void onStop() {
		broadcastMgr.unregisterReceiver(devicesLoadedReceiver);
		broadcastMgr.unregisterReceiver(loginReceiver);
		super.onStop();
	}

	private void scrollAccountForm() {
		// delay this until after the keyboard has popped up so the scroll view
		// is actually able to scroll
		Runnable scrollRunnable = new Runnable() {

			@Override
			public void run() {
				ScrollView scrollArea = Ui.findView(LoginActivity.this, R.id.scroll_area);
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

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		email = mEmailView.getText().toString();
		password = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid email address.
		if (!truthy(email)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		// Check for a valid password.
		if (TextUtils.isEmpty(password)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (password.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			showProgress(true);
			api.logIn(email, password);
		}
	}


	private void showProgress(boolean show) {
		super.showProgress(R.id.progress_indicator, show);
		accountAction.setEnabled(!show);
	}

	private void onLogInComplete(boolean success, int statusCode, String error) {
		if (success) {
			broadcastMgr.registerReceiver(devicesLoadedReceiver, devicesLoadedReceiver.getFilter());
			api.requestAllDevices();
			Toast toast = Toast.makeText(this, "Loading your Cores...", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

		} else {
			showProgress(false);
			if (!netConnectionHelper.isConnectedViaWifi()) {
				getErrorsDelegate().showCloudUnreachableDialog();

			} else if (statusCode == 400) {
				mPasswordView.setError(getString(R.string.error_incorrect_password));
				mPasswordView.requestFocus();

			} else {
				getErrorsDelegate().showHttpErrorDialog(statusCode);
			}
		}
	}

	private void onDevicesUpdated(boolean success, String error) {
		showProgress(false);
		if (!isFinishing()) {
			startActivity(new Intent(this, CoreListActivity.class)
					.putExtra(CoreListActivity.ARG_ENTERING_FROM_LAUNCH, true));
		}
		finish();
	}


	void launchSignUpActivity() {
		// the value here doesn't really matter, it's just a flag.
		startActivity(new Intent(this, SignUpActivity.class)
				.putExtra(SignUpActivity.EXTRA_FROM_LOGIN, ""));
		finish();
	}


	class DevicesLoadedReceiver extends BroadcastReceiver {

		public IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_DEVICES_UPDATED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			onDevicesUpdated((ApiFacade.getResultCode(intent) == HttpStatus.SC_OK),
					intent.getStringExtra(SimpleSparkApiService.EXTRA_ERROR_MSG));
		}
	}


	class LoggedInReceiver extends BroadcastReceiver {

		public IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_LOG_IN_FINISHED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			onLogInComplete((ApiFacade.getResultCode(intent) == HttpStatus.SC_OK),
					ApiFacade.getResultCode(intent),
					intent.getStringExtra(SimpleSparkApiService.EXTRA_ERROR_MSG));
		}
	}


	static final TLog log = new TLog(LoginActivity.class);

}
