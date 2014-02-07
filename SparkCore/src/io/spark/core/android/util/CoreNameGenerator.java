package io.spark.core.android.util;

import java.util.Random;
import java.util.Set;


public class CoreNameGenerator {

	private static final Random random = new Random();

	private static final String[] TROCHEES = new String[] { "aardvark", "bacon", "badger", "banjo",
			"bobcat", "boomer", "captain", "chicken", "cowboy", "cracker", "cranky", "crazy",
			"dentist", "doctor", "dozen", "easter", "ferret", "gerbil", "hacker", "hamster",
			"hindu", "hobo", "hoosier", "hunter", "jester", "jetpack", "kitty", "laser", "lawyer",
			"mighty", "monkey", "morphing", "mutant", "narwhal", "ninja", "normal", "penguin",
			"pirate", "pizza", "plumber", "power", "puppy", "ranger", "raptor", "robot", "scraper",
			"scrapple", "station", "tasty", "trochee", "turkey", "turtle", "vampire", "wombat",
			"zombie" };


	public static String generateUniqueName(Set<String> existingNames) {
		String uniqueName = null;
		while (uniqueName == null) {
			String part1 = getRandomName();
			String part2 = getRandomName();
			String candidate = part1 + "_" + part2;
			if (!existingNames.contains(candidate) && !part1.equals(part2)) {
				uniqueName = candidate;
			}
		}
		return uniqueName;
	}

	private static String getRandomName() {
		int randomIndex = random.nextInt(TROCHEES.length);
		return TROCHEES[randomIndex];
	}
}
