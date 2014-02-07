package io.spark.core.android.ui;

import static org.solemnsilence.util.Py.list;
import static org.solemnsilence.util.Py.truthy;
import io.spark.core.android.R;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.cloud.ApiFacade;
import io.spark.core.android.cloud.requestservice.SimpleSparkApiService;
import io.spark.core.android.ui.corelist.CoreListActivity;
import io.spark.core.android.ui.util.Ui;

import org.apache.http.HttpStatus;
import org.solemnsilence.util.EZ;
import org.solemnsilence.util.TLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
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


/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class SignUpActivity extends BaseActivity {


	private static final TLog log = new TLog(SignUpActivity.class);


	// Indicate that we came from the login screen, so don't attempt
	// "launchNextActivityIfReady()"
	public static final String EXTRA_FROM_LOGIN = "io.spark.core.android.extra.FROM_LOGIN";


	// Values for email and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;
	private String savedErrorMessage;
	private boolean waitForDevicesOnLogin = false;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private Button accountAction;

	private LoggedInReceiver loggedInReceiver = new LoggedInReceiver();
	private SignUpReceiver signUpReceiver = new SignUpReceiver();
	private DevicesLoadedReceiver devicesLoadedReceiver = new DevicesLoadedReceiver();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent nextActivityToSkipTo = getNextActivityToSkipTo();

		// NOTE EARLY RETURN HERE
		if (nextActivityToSkipTo != null) {
			if (!isFinishing()) {
				startActivity(nextActivityToSkipTo);
			}
			finish();
			return;
		}

		setContentView(R.layout.activity_sign_up);

		mEmailView = Ui.findView(this, R.id.email);
		mPasswordView = Ui.findView(this, R.id.password);

		Ui.setTextFromHtml(this, R.id.already_have_account, R.string.i_already_have_an_account);
		TextView finePrint = Ui.setTextFromHtml(this, R.id.fine_print, R.string.sign_up_fine_print);
		finePrint.setMovementMethod(LinkMovementMethod.getInstance());

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
		for (View view : list(mPasswordView, mEmailView)) {
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

		findViewById(R.id.already_have_account).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
				finish();
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		broadcastMgr.registerReceiver(signUpReceiver, signUpReceiver.getFilter());
		broadcastMgr.registerReceiver(loggedInReceiver, loggedInReceiver.getFilter());
	}

	@Override
	protected void onStop() {
		broadcastMgr.unregisterReceiver(signUpReceiver);
		broadcastMgr.unregisterReceiver(loggedInReceiver);
		broadcastMgr.unregisterReceiver(devicesLoadedReceiver);
		super.onStop();
	}

	private void scrollAccountForm() {
		// delay this until after the keyboard has popped up so the scroll view
		// is actually able to scroll
		Runnable scrollRunnable = new Runnable() {

			@Override
			public void run() {
				ScrollView scrollArea = Ui.findView(SignUpActivity.this, R.id.scroll_area);
				// using fullScroll() or pageScroll() impacts which child widget
				// gets focus, so that doesn't work here, instead just scroll by
				// an absurdly large number.
				scrollArea.scrollBy(0, 5000);
			}
		};
		// Doing this twice because sometimes it takes a moment for the keyboard
		// to pop up, changing the screen dimensions and allowing scrolling.
		EZ.runOnMainThreadDelayed(scrollRunnable, 150);
		EZ.runOnMainThreadDelayed(scrollRunnable, 750);
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		if (!accountAction.isEnabled()) {
			log.wtf("Sign up being attempted again even though the sign up button isn't enabled?!");
			return;
		}

		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid email address.
		if (!truthy(mEmail)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!Patterns.EMAIL_ADDRESS.matcher(mEmail).matches()) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		}

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
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
			ApiFacade.getInstance(this).signUp(mEmail, mPassword);
		}
	}

	// If we're ready to skip to a new activity, return it here.
	private Intent getNextActivityToSkipTo() {
		if (getIntent().hasExtra(EXTRA_FROM_LOGIN)) {
			// we came here from the login screen, don't bounce back and forth.
			return null;
		}

		Intent intent = null;
		if (isLoggedIn()) {
			log.d("Looks like we're logged in, launching Cores list");
			intent = new Intent(this, CoreListActivity.class)
					.putExtra(CoreListActivity.ARG_ENTERING_FROM_LAUNCH, true);
			if (DeviceState.getKnownDevices().isEmpty()) {
				api.requestAllDevices();
			}

		} else if (prefs.getCompletedFirstLogin()) {
			log.d("We're not logged in now, but we have been before, launching login screen");
			intent = new Intent(this, LoginActivity.class);
		}

		if (intent == null) {
			log.d("User is not logged in and has never logged in, staying on sign up screen");

		} else {
			return intent;
		}

		return null;
	}

	private boolean isLoggedIn() {
		// i.e.: do we have a token?
		return truthy(prefs.getToken());
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	private void showProgress(final boolean show) {
		super.showProgress(R.id.progress_indicator, show);
		accountAction.setEnabled(!show);
	}


	private void onSignUpComplete(boolean success, String error) {
		// Regardless of whether it's a success, wait for the service to try
		// logging in before reporting failure.
		if (!success) {
			savedErrorMessage = error;
			// sign up might have "failed" because the user has an account
			// already
			waitForDevicesOnLogin = true;
		}
	}

	private void onLogInComplete(boolean success, String error) {
		if (!success) {
			waitForDevicesOnLogin = false; // clear this state
			onFailed(savedErrorMessage);
			return;
		}

		if (waitForDevicesOnLogin) {
			broadcastMgr.registerReceiver(devicesLoadedReceiver, devicesLoadedReceiver.getFilter());
			api.requestAllDevices();
			Toast toast = Toast.makeText(this, "Loading your Cores...", Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

		} else {
			moveToCoreList();
		}

	}

	private void moveToCoreList() {
		if (!isFinishing()) {
			Intent intent = new Intent(this, CoreListActivity.class)
					.putExtra(CoreListActivity.ARG_ENTERING_FROM_LAUNCH, true);
			if (DeviceState.getKnownDevices().isEmpty()) {
				intent.putExtra(CoreListActivity.ARG_SKIP_TO_SMART_CONFIG, true);
			}
			startActivity(intent);
		}
		finish();
	}

	private void onFailed(String error) {
		showProgress(false);
		mPasswordView.requestFocus();
		mPasswordView.setError(error);
		savedErrorMessage = null;
	}

	private void onDevicesUpdated(boolean success, String error) {
		if (success) {
			moveToCoreList();
		}
	}


	class SignUpReceiver extends BroadcastReceiver {

		public IntentFilter getFilter() {
			return new IntentFilter(ApiFacade.BROADCAST_SIGN_UP_FINISHED);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			onSignUpComplete((ApiFacade.getResultCode(intent) == HttpStatus.SC_OK),
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
					intent.getStringExtra(SimpleSparkApiService.EXTRA_ERROR_MSG));
		}
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


}
