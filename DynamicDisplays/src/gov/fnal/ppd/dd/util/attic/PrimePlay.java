package gov.fnal.ppd.dd.util.attic;

/**
 * Some playing with prime numbers
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class PrimePlay {
	public static void main(String[] args) {
		long prime = Long.parseLong(args[0]);
		long elliott = 0x6c45696c746f0a74L;
		long combine = (prime) ^ (elliott);
		System.out.println(Long.toBinaryString(prime));
		System.out.println(Long.toBinaryString(elliott));
		System.out.println(Long.toBinaryString(combine));
	}
}
