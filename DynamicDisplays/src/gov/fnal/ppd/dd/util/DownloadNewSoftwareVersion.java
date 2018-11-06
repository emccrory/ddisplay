package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.SOFTWARE_FILE_ZIP;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_PROTOCOL;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Download a new version of the software from the Dynamic Displays server.
 * 
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class DownloadNewSoftwareVersion {

	// Note: The URL contains the slash ("/") always, but the filename uses File.separator (although I think Java corrects for this
	// internally)
	private final String	zipFile			= SOFTWARE_FILE_ZIP;
	private final String	location		= WEB_PROTOCOL + "://" + WEB_SERVER_NAME + "/software/" + zipFile;
	private final String	operatingFolder	= ".." + File.separator + "..";
	private final String	outputFolder	= operatingFolder + File.separator + "roc-dynamicdisplays" + File.separator
			+ "DynamicDisplays" + File.separator;
	private final String	zipFilePath		= operatingFolder + File.separator + zipFile;

	public static void main(String[] args) {
		DownloadNewSoftwareVersion d = new DownloadNewSoftwareVersion(null);
		if (d.hasSucceeded())
			System.exit(0);
	}

	boolean succeeded = false;

	public DownloadNewSoftwareVersion(String version) {
		succeeded = download(version) && renameTargetFolder() && unpack();
	}

	private boolean download(String version) {
		String actualLocation = location;
		if (version != null) {
			actualLocation = location.replace(".zip", "_" + adjust(version) + ".zip");
		} else {
			actualLocation = actualLocation.replace("/software", "");
		}
		println(getClass(), "Getting the file from " + actualLocation);
		try (InputStream in = new URL(actualLocation).openStream()) {
			Files.copy(in, Paths.get(zipFilePath), StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		println(getClass(), "ZIP file retrieval Failed");
		return false;
	}

	private static String adjust(String v) {
		return v.replace('.', '_');
	}

	private boolean unpack() {
		println(getClass(), "Unpacking ZIP file " + zipFilePath + " to " + outputFolder);

		String osName = System.getProperty("os.name").toUpperCase();
		boolean isUnix = osName.contains("LINUX") || osName.contains("UNIX");

		// This code stolen from https://howtodoinjava.com/java/io/unzip-file-with-subdirectories/
		// It seems to be a LOT slower than the operating systems' "unzip" command(s). Whatever; we have the time.

		// Open the file
		try (ZipFile file = new ZipFile(zipFilePath)) {
			FileSystem fileSystem = FileSystems.getDefault();
			// Get file entries
			Enumeration<? extends ZipEntry> entries = file.entries();

			// We will unzip files in this folder
			String uncompressedFolder = outputFolder;
			// Already created - Files.createDirectory(fileSystem.getPath(uncompressedDirectory));

			// Iterate over entries
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				// If directory then create a new directory in uncompressed folder
				if (entry.isDirectory()) {
					Files.createDirectories(fileSystem.getPath(uncompressedFolder + entry.getName()));
					println(getClass(), "Created folder:        " + uncompressedFolder + entry.getName());
				}
				// Else create the file
				else {
					// FIXME - File protections on Linux
					// I cannot figure out how the Linux command unzip preserves the file protections - all the Google searches I
					// have done to date say that zip does not store file protections, but this is clearly wrong. So there must
					// be some way to restore those protections when we write the files here, but (alas), I cannot figure it out.
					// So for now, I will ASSUME that all files that end in ".sh" or "driver" (as in geckodriver) will be
					// executable.

					InputStream is = file.getInputStream(entry);
					BufferedInputStream bis = new BufferedInputStream(is);
					String uncompressedFileName = uncompressedFolder + entry.getName();
					Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
					Files.createFile(uncompressedFilePath);
					FileOutputStream fileOutput = new FileOutputStream(uncompressedFileName);
					while (bis.available() > 0) {
						fileOutput.write(bis.read());
					}
					fileOutput.close();
					if (isUnix && (uncompressedFileName.endsWith(".sh") || uncompressedFileName.endsWith("driver"))) {
						File f = new File(uncompressedFileName);
						f.setExecutable(true);
						println(getClass(), "Wrote executable file: " + entry.getName());

					} else
						println(getClass(), "Wrote plain file:      " + entry.getName());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			println(getClass(), "Unpacking Failed");
			return false;
		}
		return true;
	}

	private boolean renameTargetFolder() {
		String targetLocation = operatingFolder + File.separator + "roc-dynamicdisplays";
		println(getClass(), "Renaming " + targetLocation);

		// File (or directory) with old name
		File file = new File(targetLocation);
		int index = 0;
		String folderName = "";
		while (file.exists()) {
			index++;
			String zero = "00";
			if (index > 99)
				zero = "";
			else if (index > 9)
				zero = "0";
			folderName = operatingFolder + File.separator + "roc-dynamicdisplays-old" + zero + index;
			file = null;
			file = new File(folderName);
		}

		if (index > 0) {
			File fileOrig = new File(targetLocation);

			// There is a problem in translating this to Windows, based on the way "..\.." is interpreted So we have to build a
			// work-around. 

			// These replace calls will do nothing in Linux, but change the absolute path to the right thing in Windows
			String adjustedTargetLoc = fileOrig.getAbsolutePath().replace("\\roc-dynamicdisplays\\DynamicDisplays\\..\\..", "");
			String adjustedNewLoc = file.getAbsolutePath().replace("\\roc-dynamicdisplays\\DynamicDisplays\\..\\..", "");
			fileOrig = new File(adjustedTargetLoc);
			file = new File(adjustedNewLoc);

			println(getClass(), "Renaming " + fileOrig.getAbsolutePath() + " to " + file.getAbsolutePath());

			try {
				Files.move(fileOrig.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ex) {
				printlnErr(getClass(), "Renaming has failed");
				return false;
			}

		}
		// File (or directory) with new name
		File file2 = new File(outputFolder);
		println(getClass(), "Creating folder " + outputFolder);
		return file2.mkdirs();
	}

	public boolean hasSucceeded() {
		return succeeded;
	}

}
