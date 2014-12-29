package io.spark.core.android.ui.tinker;

public enum DigitalValue {

	HIGH(1),
	LOW(0),
	NONE(-1);


	private final int intValue;

	private DigitalValue(int intValue) {
		this.intValue = intValue;
	}


	public int asInt() {
		return intValue;
	}


	public static DigitalValue fromInt(int value) {
		switch (value) {
			case 1:
				return HIGH;
			case 0:
				return LOW;
			default:
				return NONE;
		}
	}

}
