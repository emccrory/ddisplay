package gov.fnal.ppd.dd.display.attic;

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
