package io.spark.core.android.ui.tinker;

import static org.solemnsilence.util.Py.list;
import io.spark.core.android.R;
import io.spark.core.android.ui.util.Ui;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class Pin {

	public interface OnAnalogWriteListener {

		public void onAnalogWrite(int value);
	}


	private static final int ANALOG_WRITE_MAX = 255;
	private static final int ANALOG_READ_MAX = 4095;

	public final TextView view;
	private PinAction configuredAction;
	private PinType pinType;
	public final String name;
	ObjectAnimator pinBackgroundAnim;
	Animator endAnimation;

	private View analogReadView;
	private View analogWriteView;
	private View digitalWriteView;
	private View digitalReadView;

	private int analogValue = 0;
	private DigitalValue digitalValue;

	boolean muted = false;


	public Pin(TextView view, PinType pinType, String name) {
		this.view = view;
		this.pinType = pinType;
		this.name = name;
		this.configuredAction = PinAction.NONE;
		reset();
	}

	public int getAnalogValue() {
		return analogValue;
	}

	public void reset() {
		if (analogReadView != null) {
			analogReadView.setVisibility(View.GONE);
			// Reset the values
			ProgressBar barGraph = Ui.findView(analogReadView,
					R.id.tinker_analog_read_progress);
			TextView readValue = Ui.findView(analogReadView,
					R.id.tinker_analog_read_value);
			barGraph.setMax(100);
			barGraph.setProgress(0);
			readValue.setText("0");
			analogReadView = null;
		}
		if (analogWriteView != null) {
			// Reset the values
			analogWriteView.setVisibility(View.GONE);
			final SeekBar seekBar = Ui.findView(analogWriteView,
					R.id.tinker_analog_write_seekbar);
			final TextView value = Ui.findView(analogWriteView,
					R.id.tinker_analog_write_value);
			seekBar.setProgress(0);
			value.setText("0");
			analogWriteView = null;
		}
		if (digitalWriteView != null) {
			digitalWriteView.setVisibility(View.GONE);
			digitalWriteView = null;
		}
		if (digitalReadView != null) {
			digitalReadView.setVisibility(View.GONE);
			digitalReadView = null;
		}
		if (!stopAnimating()) {
			((View) view.getParent()).setBackgroundColor(0);
		}
		muted = false;
		analogValue = 0;
		digitalValue = DigitalValue.NONE;
	}

	public void setConfiguredAction(PinAction action) {
		this.configuredAction = action;
		// Clear out any views
		updatePinColor();
	}

	private void updatePinColor() {
		view.setTextColor(view.getContext().getResources().getColor(android.R.color.white));

		switch (configuredAction) {
			case ANALOG_READ:
				view.setBackgroundResource(R.drawable.tinker_pin_emerald);
				break;
			case ANALOG_WRITE:
				view.setBackgroundResource(R.drawable.tinker_pin_sunflower);
				break;
			case DIGITAL_READ:
				if (digitalValue == DigitalValue.HIGH) {
					view.setBackgroundResource(R.drawable.tinker_pin_read_high);
					view.setTextColor(view.getContext().getResources()
							.getColor(R.color.tinker_pin_text_dark));
				} else {
					view.setBackgroundResource(R.drawable.tinker_pin_cyan);
				}
				break;
			case DIGITAL_WRITE:
				if (digitalValue == DigitalValue.HIGH) {
					view.setBackgroundResource(R.drawable.tinker_pin_write_high);
					view.setTextColor(view.getContext().getResources()
							.getColor(R.color.tinker_pin_text_dark));
				} else {
					view.setBackgroundResource(R.drawable.tinker_pin_alizarin);
				}
				break;
			case NONE:
				view.setBackgroundResource(R.drawable.tinker_pin);
				break;
		}
	}

	public PinAction getConfiguredAction() {
		return configuredAction;
	}

	public boolean isAnalogWriteMode() {
		return (analogWriteView != null && analogWriteView.getVisibility() == View.VISIBLE);
	}

	public void mute() {
		muted = true;
		view.setBackgroundResource(R.drawable.tinker_pin_muted);
		view.setTextColor(view.getContext().getResources().getColor(
				R.color.tinker_pin_text_muted));
		hideExtraViews();
	}

	public void unmute() {
		muted = false;
		updatePinColor();
		showExtraViews();
	}

	private void hideExtraViews() {
		if (analogReadView != null) {
			analogReadView.setVisibility(View.GONE);
		}
		if (analogWriteView != null) {
			analogWriteView.setVisibility(View.GONE);
		}
		if (digitalWriteView != null) {
			digitalWriteView.setVisibility(View.GONE);
		}
		if (digitalReadView != null) {
			digitalReadView.setVisibility(View.GONE);
		}

		if (pinBackgroundAnim != null) {
			pinBackgroundAnim.end();
		}
		View parent = (View) view.getParent();
		parent.setBackgroundColor(0);
	}

	private void showExtraViews() {
		if (analogReadView != null) {
			analogReadView.setVisibility(View.VISIBLE);
		} else if (analogWriteView != null) {
			analogWriteView.setVisibility(View.VISIBLE);
		} else if (digitalWriteView != null) {
			digitalWriteView.setVisibility(View.VISIBLE);
		} else if (digitalReadView != null) {
			digitalReadView.setVisibility(View.VISIBLE);
		}
	}

	public void showAnalogValue(int value) {
		analogValue = value;
		doShowAnalogValue(value);
	}

	private void doShowAnalogValue(int newValue) {
		if (analogWriteView != null) {
			analogWriteView.setVisibility(View.GONE);
			analogWriteView = null;
		}

		ViewGroup parent = (ViewGroup) view.getParent();

		if (pinBackgroundAnim != null) {
			pinBackgroundAnim.cancel();
		}

		if (analogReadView == null) {
			analogReadView = Ui.findView(parent, R.id.tinker_analog_read_main);
		}

		// If the view does not exist, inflate it
		if (analogReadView == null) {
			LayoutInflater inflater = (LayoutInflater) view.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			if (pinType == PinType.A) {
				analogReadView = inflater.inflate(R.layout.tinker_analog_read_left, parent, false);
				parent.addView(analogReadView);
			} else if (pinType == PinType.D) {
				analogReadView = inflater.inflate(R.layout.tinker_analog_read_right, parent, false);
				parent.addView(analogReadView, 0);
			}
		}

		analogReadView.setVisibility(View.VISIBLE);
		// Find the existing views and set the values
		ProgressBar barGraph = Ui.findView(analogReadView,
				R.id.tinker_analog_read_progress);
		TextView readValue = Ui.findView(analogReadView,
				R.id.tinker_analog_read_value);

		if (PinAction.ANALOG_READ.equals(configuredAction)) {
			barGraph.setProgressDrawable(view.getContext().getResources()
					.getDrawable(R.drawable.progress_emerald));
		} else {
			barGraph.setProgressDrawable(view.getContext().getResources()
					.getDrawable(R.drawable.progress_sunflower));
		}

		int max = 1;
		if (configuredAction == PinAction.ANALOG_READ) {
			max = ANALOG_READ_MAX;
		} else if (configuredAction == PinAction.ANALOG_WRITE) {
			max = ANALOG_WRITE_MAX;
		}

		barGraph.setMax(max);
		barGraph.setProgress(newValue);
		readValue.setText(String.valueOf(newValue));
	}

	public void showAnalogWrite(final OnAnalogWriteListener listener) {
		if (analogReadView != null) {
			analogReadView.setVisibility(View.GONE);
			analogReadView = null;
		}

		final ViewGroup parent = (ViewGroup) view.getParent();
		if (analogWriteView == null) {
			analogWriteView = Ui.findView(parent, R.id.tinker_analog_write_main);
		}

		// If the view does not exist, inflate it
		if (analogWriteView == null) {
			LayoutInflater inflater = (LayoutInflater) view.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			if (pinType == PinType.A) {
				analogWriteView = inflater.inflate(R.layout.tinker_analog_write_left,
						parent, false);
				parent.addView(analogWriteView);
			} else if (pinType == PinType.D) {
				analogWriteView = inflater.inflate(R.layout.tinker_analog_write_right, parent,
						false);
				parent.addView(analogWriteView, 0);
			}
		}

		analogWriteView.setVisibility(View.VISIBLE);
		final SeekBar seekBar = Ui.findView(analogWriteView,
				R.id.tinker_analog_write_seekbar);
		final TextView valueText = Ui.findView(analogWriteView,
				R.id.tinker_analog_write_value);
		if (pinBackgroundAnim != null) {
			pinBackgroundAnim.cancel();
			pinBackgroundAnim = null;
		}
		parent.setBackgroundColor(0x4C000000);

		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				int value = seekBar.getProgress();
				parent.setBackgroundColor(0);
				showAnalogWriteValue();
				listener.onAnalogWrite(value);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				valueText.setText(String.valueOf(progress));
			}
		});
	}

	public void showAnalogWriteValue() {
		doShowAnalogValue(analogValue);
	}

	public void showDigitalWrite(DigitalValue newValue) {
		this.digitalValue = newValue;
		ViewGroup parent = (ViewGroup) view.getParent();
		if (digitalWriteView == null) {
			digitalWriteView = Ui.findView(parent, R.id.tinker_digital_write_main);
		}

		// If the view does not exist, inflate it
		if (digitalWriteView == null) {
			LayoutInflater inflater = (LayoutInflater) view.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			digitalWriteView = inflater.inflate(R.layout.tinker_digital_write, parent, false);
			if (pinType == PinType.A) {
				parent.addView(digitalWriteView);
			} else if (pinType == PinType.D) {
				parent.addView(digitalWriteView, 0);
			}
		}

		digitalWriteView.setVisibility(View.VISIBLE);
		final TextView value = Ui.findView(digitalWriteView,
				R.id.tinker_digital_write_value);
		value.setText(newValue.name());
		updatePinColor();
	}

	public void showDigitalRead(DigitalValue newValue) {
		this.digitalValue = newValue;
		ViewGroup parent = (ViewGroup) view.getParent();
		if (digitalReadView == null) {
			digitalReadView = Ui.findView(parent,
					R.id.tinker_digital_write_main);
		}

		// If the view does not exist, inflate it
		if (digitalReadView == null) {
			LayoutInflater inflater = (LayoutInflater) view.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			digitalReadView = inflater.inflate(R.layout.tinker_digital_read, parent, false);
			if (pinType == PinType.A) {
				parent.addView(digitalReadView);
			} else if (pinType == PinType.D) {
				parent.addView(digitalReadView, 0);
			}
		}

		digitalReadView.setVisibility(View.VISIBLE);
		final TextView value = Ui.findView(digitalReadView,
				R.id.tinker_digital_read_value);
		value.setText(newValue.name());
		// fade(value, newValue);
		updatePinColor();
		if (!stopAnimating()) {
			getCancelAnimator().start();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((configuredAction == null) ? 0 : configuredAction.hashCode());
		result = prime * result + ((view == null) ? 0 : view.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pin other = (Pin) obj;
		if (configuredAction != other.configuredAction)
			return false;
		if (view == null) {
			if (other.view != null)
				return false;
		} else if (!view.equals(other.view))
			return false;
		return true;
	}

	public DigitalValue getDigitalValue() {
		return digitalValue;
	}



	public void animateYourself() {
		final ViewGroup parent = (ViewGroup) view.getParent();

		if (pinBackgroundAnim != null) {
			pinBackgroundAnim.end();
			pinBackgroundAnim = null;
		}

		pinBackgroundAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(
				view.getContext(), R.animator.pin_background_start);

		pinBackgroundAnim.setTarget(parent);
		pinBackgroundAnim.setEvaluator(new ArgbEvaluator());
		pinBackgroundAnim.addListener(new AnimatorListener() {

			@Override
			public void onAnimationStart(Animator animation) {
				// NO OP
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
			}

			@Override
			public void onAnimationCancel(Animator animation) {
				endAnimation = getCancelAnimator();
				endAnimation.start();
			}
		});
		pinBackgroundAnim.start();
	}

	Animator getCancelAnimator() {
		ObjectAnimator backToTransparent1 = (ObjectAnimator) AnimatorInflater
				.loadAnimator(view.getContext(), R.animator.pin_background_end);
		ObjectAnimator goDark = (ObjectAnimator) AnimatorInflater
				.loadAnimator(view.getContext(), R.animator.pin_background_go_dark);
		ObjectAnimator backToTransparent2 = (ObjectAnimator) AnimatorInflater
				.loadAnimator(view.getContext(), R.animator.pin_background_end);

		ViewGroup parent = (ViewGroup) view.getParent();
		ArgbEvaluator evaluator = new ArgbEvaluator();
		for (ObjectAnimator animator : list(backToTransparent1, goDark, backToTransparent2)) {
			animator.setTarget(parent);
			animator.setEvaluator(evaluator);
		}

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.setTarget(parent);
		animatorSet.playSequentially(backToTransparent1, goDark, backToTransparent2);
		return animatorSet;
	}

	public boolean stopAnimating() {
		if (pinBackgroundAnim != null) {
			pinBackgroundAnim.cancel();
			pinBackgroundAnim = null;
			return true;
		} else {
			return false;
		}
	}
}
