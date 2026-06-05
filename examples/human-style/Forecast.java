package weather;

import java.util.Random;
import java.util.Scanner;

public class Forecast {

	// quick + dirty CLI, will tidy up once the upstream API settles
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.print("city? ");
		String city = sc.nextLine().trim();
		if (city.isEmpty()) {
			System.out.println("no city given, bailing out");
			return;
		}

		double[] hourly = fakeReadings(city.hashCode());
		double sum = 0;
		for (double t : hourly) {
			sum += t;
		}
		double avg = sum / hourly.length;

		// FIXME: thresholds are guesses, move them to config
		String mood = avg > 25 ? "scorching" : avg < 5 ? "freezing" : "fine";
		System.out.printf("%s: avg %.1f (%s)%n", city, avg, mood);
	}

	static double[] fakeReadings(int seed) {
		Random rng = new Random(seed);
		double[] readings = new double[24];
		for (int h = 0; h < readings.length; h++) {
			readings[h] = 18 + rng.nextGaussian() * 6;
		}
		return readings;
	}
}
