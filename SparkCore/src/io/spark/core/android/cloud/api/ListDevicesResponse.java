package io.spark.core.android.cloud.api;

import java.util.List;



public class ListDevicesResponse extends SimpleResponse {


	public final List<Device> devices;


	public ListDevicesResponse(boolean ok, List<String> errors, List<Device> devices) {
		super(ok, errors);
		this.devices = devices;
	}

}
