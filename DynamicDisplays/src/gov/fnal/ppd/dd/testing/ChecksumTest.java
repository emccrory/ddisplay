package gov.fnal.ppd.dd.testing;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ChecksumTest {

	public static void main(String[] args) {

		Checksum checksum = new CRC32();

		String one = "ABCDEFGqrstuvwxyz";
		String two = "qrstuvwxyzABCDEFG";

		byte bytes[] = one.getBytes();
		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);

		long c1 = checksum.getValue();

		bytes = two.getBytes();
		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);
		long c2 = checksum.getValue();

		// get the current checksum value
		System.out.println("One: " + c1 + ", two: " + c2);
	}
}
