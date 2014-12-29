package io.spark.core.android.cloud.api;

import java.util.List;


public class SimpleResponse {

	public final boolean ok;
	public final List<String> errors;

	public SimpleResponse(boolean ok, List<String> errors) {
		super();
		this.ok = ok;
		this.errors = errors;
	}

	@Override
	public String toString() {
		return "SimpleResponse [ok=" + ok + ", errors=" + errors + "]";
	}

}
