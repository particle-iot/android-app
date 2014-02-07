package io.spark.core.android.cloud.api;

import static org.solemnsilence.util.Py.truthy;
import android.os.Parcel;
import android.os.Parcelable;


public class TinkerResponse implements Parcelable {

	public static final int RESPONSE_TYPE_DIGITAL = 1;
	public static final int RESPONSE_TYPE_ANALOG = 2;

	public static final int REQUEST_TYPE_READ = 3;
	public static final int REQUEST_TYPE_WRITE = 4;


	public final int requestType;
	public final String coreId;
	public final String pin;
	public final int responseValue;
	public final int responseType;
	public final boolean errorMakingRequest;


	public TinkerResponse(int requestType, String coreId, String pin, int responseType,
			int responseValue, boolean requestError) {
		this.requestType = requestType;
		this.coreId = coreId;
		this.pin = pin;
		this.responseType = responseType;
		this.responseValue = responseValue;
		this.errorMakingRequest = requestError;
	}

	public TinkerResponse(Parcel in) {
		this.requestType = in.readInt();
		this.coreId = in.readString();
		this.pin = in.readString();
		this.responseType = in.readInt();
		this.responseValue = in.readInt();
		this.errorMakingRequest = truthy(in.readInt());
	}

	@Override
	public String toString() {
		return "TinkerResponse [requestType=" + requestType + ", coreId=" + coreId + ", pin=" + pin
				+ ", responseValue=" + responseValue + ", responseType=" + responseType
				+ ", requestError=" + errorMakingRequest + "]";
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(requestType);
		dest.writeString(coreId);
		dest.writeString(pin);
		dest.writeInt(responseType);
		dest.writeInt(responseValue);
		dest.writeInt((errorMakingRequest) ? 1 : 0);
	}


	public static final Parcelable.Creator<TinkerResponse> CREATOR = new Parcelable.Creator<TinkerResponse>() {

		public TinkerResponse createFromParcel(Parcel in) {
			return new TinkerResponse(in);
		}

		public TinkerResponse[] newArray(int size) {
			return new TinkerResponse[size];
		}
	};

}
