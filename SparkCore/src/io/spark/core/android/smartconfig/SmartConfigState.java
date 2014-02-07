package io.spark.core.android.smartconfig;

import static org.solemnsilence.util.Py.set;

import java.util.Set;

import com.google.common.collect.Sets;


public class SmartConfigState {


	private static final Set<String> smartConfigFoundDeviceIds = set();
	private static final Set<String> claimedButPossiblyUnnamedDeviceIds = set();


	public synchronized static Set<String> getSmartConfigFoundDeviceIds() {
		return Sets.newHashSet(smartConfigFoundDeviceIds);
	}

	public synchronized static void addSmartConfigFoundId(String newId) {
		smartConfigFoundDeviceIds.add(newId);
	}

	public synchronized static void removeSmartConfigFoundDeviceId(String newId) {
		smartConfigFoundDeviceIds.remove(newId);
	}

	public synchronized static Set<String> getClaimedButPossiblyUnnamedDeviceIds() {
		return Sets.newHashSet(claimedButPossiblyUnnamedDeviceIds);
	}

	public synchronized static void addClaimedButPossiblyUnnamedDeviceId(String newId) {
		claimedButPossiblyUnnamedDeviceIds.add(newId);
	}

	public synchronized static void removeClaimedButPossiblyUnnamedDeviceIds(String newId) {
		claimedButPossiblyUnnamedDeviceIds.remove(newId);
	}

	public synchronized static void clearSmartConfigData() {
		claimedButPossiblyUnnamedDeviceIds.clear();
		smartConfigFoundDeviceIds.clear();
	}

}
