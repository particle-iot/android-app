package io.spark.core.android.cloud.api;

import static org.solemnsilence.util.Py.truthy;


/**
 * Class representing a single Spark device (i.e.: a Core)
 */
public class Device {

	public final String id;
	public final String name;
	public final int color;

	private Device(String id, String name, int color) {
		this.id = id;
		this.name = name;
		this.color = color;
	}


	public static Builder newBuilder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return newBuilder()
				.setColor(color)
				.setId(id)
				.setName(name);
	}


	public static class Builder {

		private String id;
		private String name;
		private int color;

		private Builder() {
		}

		public Device build() {
			return new Device(id, name, color);
		}

		public Builder fillInFalseyValuesFromOther(Device other) {
			this.id = (truthy(this.id)) ? this.id : other.id;
			this.name = (truthy(this.name)) ? this.name : other.name;
			this.color = (truthy(this.color)) ? this.color : other.color;
			return this;
		}


		public String getId() {
			return id;
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public String getName() {
			return name;
		}

		public Builder setName(String name) {
			this.name = name;
			return this;
		}

		public int getColor() {
			return color;
		}

		public Builder setColor(int color) {
			this.color = color;
			return this;
		}
	}


	@Override
	public String toString() {
		return "Device [id=" + id + ", name=" + name + ", color=" + color + "]";
	}

	// NOTE: device ID is the ONLY field being factored in to test equality
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Device other = (Device) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

}
