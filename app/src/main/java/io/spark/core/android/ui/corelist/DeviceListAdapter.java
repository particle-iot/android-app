package io.spark.core.android.ui.corelist;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import io.spark.core.android.R;
import io.spark.core.android.cloud.api.Device;
import io.spark.core.android.ui.util.Ui;


public class DeviceListAdapter extends ArrayAdapter<Device> {

	Device selectedForPopupActions;

	public DeviceListAdapter(Context ctx) {
		super(ctx, R.layout.core_row);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.core_row, parent, false);

			ViewHolder holder = new ViewHolder();
			holder.activeStripe = Ui.findView(convertView, R.id.active_core_stripe);
			holder.coreName = Ui.findView(convertView, R.id.core_name);

			convertView.setTag(holder);
		}

		ViewHolder holder = (ViewHolder) convertView.getTag();
		final Device device = getItem(position);

		holder.coreName.setText(device.name);

		Drawable[] compoundDrawables = holder.coreName.getCompoundDrawables();
		GradientDrawable dotDrawable = (GradientDrawable) compoundDrawables[0];
		dotDrawable.setColor(device.color);

		holder.activeStripe.setBackgroundColor(device.color);
		if (convertView.isActivated()) {
			holder.activeStripe.setVisibility(View.VISIBLE);
		} else {
			holder.activeStripe.setVisibility(View.INVISIBLE);
		}

		return convertView;
	}


	static class ViewHolder {

		TextView coreName;
		View activeStripe;
	}


}
