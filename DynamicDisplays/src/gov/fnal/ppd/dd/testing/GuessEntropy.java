package gov.fnal.ppd.dd.testing;

import java.util.Random;

/**
 * Unsuccessful attempt to put a number on the entropy of a few random numbers.
 * 
 * Taken from https://www.cs.princeton.edu/courses/archive/spring17/cos126/docs/mid1-s17/ShannonEntropy.java
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class GuessEntropy {

	public static void main(String[] args) {

		Random generator = new Random(System.nanoTime());

		// sequence of n integers are between 1 and m
		int m = 100;

		// number of integers
		int n = 10000;

		// compute frequencies
		// freq[i] = # times integer i appears
		int[] freq = new int[m + 1];
		for (int j = 0; j < n; j++) {
			int value = generator.nextInt(m);
			freq[value]++;
		}

		// compute Shannon entropy
		double entropy = 0.0;
		for (int i = 1; i <= m; i++) {
			double p = 1.0 * freq[i] / n;
			if (freq[i] > 0)
				entropy -= p * Math.log(p) / Math.log(2);
		}

		// print results
		System.out.println(entropy);
	}
}