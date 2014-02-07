package io.spark.core.android.ui.smartconfig;

import io.spark.core.android.R;
import io.spark.core.android.app.DeviceState;
import io.spark.core.android.ui.BaseFragment;
import io.spark.core.android.ui.corelist.CoreListActivity;
import io.spark.core.android.ui.util.Ui;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class NoCoresFoundFragment extends BaseFragment {


	@Override
	public int getContentViewLayoutId() {
		return R.layout.fragment_no_cores_found;
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		Button tryAgain = Ui.findView(this, R.id.try_again_button);
		tryAgain.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getActivity().startActivity(new Intent(getActivity(), SmartConfigActivity.class));
				getActivity().finish();
			}
		});

		Button continueAnyway = Ui.findView(this, R.id.continue_anyway_button);
		if (DeviceState.getKnownDevices().isEmpty()) {
			continueAnyway.setVisibility(View.GONE);
		} else {
			continueAnyway.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					NavUtils.navigateUpTo(getActivity(),
							new Intent(getActivity(), CoreListActivity.class));
				}
			});
		}
	}
}
