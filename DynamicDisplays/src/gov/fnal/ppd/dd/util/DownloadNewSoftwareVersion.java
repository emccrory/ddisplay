package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.SOFTWARE_FILE_ZIP;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_PROTOCOL;
import static gov.fnal.ppd.dd.GlobalVariables.WEB_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.okToUpdateSoftware;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
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

	private final String	zipFile		= SOFTWARE_FILE_ZIP;
	private final String	location	= WEB_PROTOCOL + "://" + WEB_SERVER_NAME + "/software/" + zipFile;
	private final String	baseFolder	= ".." + File.separator + ".." + File.separator;
	// private final String outputFolder = "roc-dynamicdisplays" + File.separator + "DynamicDisplays" + File.separator;
	private final String	tempFolder	= "roc-dynamicdisplays-new" + File.separator + "DynamicDisplays" + File.separator;
	private final String	zipFilePath	= zipFile;

	public static void main(String[] args) {
		// This test should download the "default" latest version of the software. The logic of what version actually
		// should be downloaded is not in this class.

		if (!okToUpdateSoftware()) {
			System.out.println("\n\n********** You should not run this test from a development machine - too risky **********");
			System.exit(0);
		}

		DownloadNewSoftwareVersion d = new DownloadNewSoftwareVersion(null);
		if (d.hasSucceeded()) {
			System.out.println("Successfully updated software");
			System.exit(0);
		}
		System.err.println("**********\nFailed to updated software\n**********");
		System.exit(-1);
	}

	boolean			succeeded	= false;
	final String	osName		= System.getProperty("os.name").toUpperCase();
	final boolean	isUnix		= osName.contains("LINUX") || osName.contains("UNIX");
	final boolean	isWindows	= osName.contains("WINDOWS");

	public DownloadNewSoftwareVersion(String version) {
		// Note that the Linux (and Mac) updates have to be a little different than the Windows updates. Windows
		// does not let you rename a folder if there is anything running from it or from its sub folders. Since
		// the JVM here is running from the sub-folder that we want to replace, we MUST give some of the update
		// work to a DOS script so we can exit the JVM.

		succeeded = setWorkingDirectory() && download(version) && unpack();
		if (succeeded) {
			if (!isWindows) {
				succeeded &= renameOriginalFolder() & renameNewFolder();
			} else {
				printlnErr(getClass(), "The software has been unpacked.  Assuming that the controlling "
						+ "Windows/DOS BATCH file will complete the installation.  EXIT(99)");
			}
		} else {
			printlnErr(getClass(), "\n\n\t\t\tUpdate failed!");
		}
	}

	private boolean setWorkingDirectory() {
		try {
			File ff = new File(baseFolder);
			System.setProperty("user.dir", ff.getCanonicalPath());

			println(getClass(), "Working directory has been changed to " + System.getProperty("user.dir"));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		println(getClass(), "Working directory: " + System.getProperty("user.dir"));
		return true;
	}

	private boolean download(String version) {
		String actualLocation = location;
		if (version != null) {
			actualLocation = location.replace(".zip", "_" + adjust(version) + ".zip");
		} else {
			actualLocation = actualLocation.replace("/software", "");
		}

		String z = null;
		try (InputStream in = new URL(actualLocation).openStream()) {
			z = System.getProperty("user.dir") + File.separator + zipFilePath;
			println(getClass(), "Getting the new version of the software from " + actualLocation + " and putting it here: " + z);

			Path p = Paths.get(z);
			Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (Exception e) {
			System.err.println("The attempted path for the output is " + z);
			e.printStackTrace();
		}
		println(getClass(), "ZIP file retrieval Failed");
		return false;
	}

	private static String adjust(String v) {
		return v.replace('.', '_');
	}

	private boolean unpack() {
		println(getClass(), "Unpacking ZIP file " + zipFilePath + " to " + tempFolder);

		// This code stolen from https://howtodoinjava.com/java/io/unzip-file-with-subdirectories/
		// since there is no guarantee that this node has unzip, or where it is (especially Windows). It seems to be a LOT slower
		// than the operating system "unzip" command(s). For example, it goes at about 8 MB/min to unzip individual files on my
		// current (late 2018) Lenovo/Windows laptop. Whatever. We have the time.

		// Open the file
		try (ZipFile file = new ZipFile(System.getProperty("user.dir") + File.separator + zipFilePath)) {
			FileSystem fileSystem = FileSystems.getDefault();
			// Get file entries
			Enumeration<? extends ZipEntry> entries = file.entries();

			// We will unzip files in this folder
			String uncompressedFolder = tempFolder;
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
					// have done to date say that zip does not store file protections, but this is clearly wrong since doing the
					// shell command unzip on Linux makes the protections right. So there must be some way to restore those
					// protections when we write the files here, but (alas), I cannot figure it out. So for now, I will ASSUME that
					// all files that end in ".sh" or "driver" (as in geckodriver) will be executable.

					InputStream is = file.getInputStream(entry);
					BufferedInputStream bis = new BufferedInputStream(is);
					String uncompressedFileName = System.getProperty("user.dir") + File.separator + uncompressedFolder
							+ entry.getName();
					Path uncompressedFilePath = fileSystem.getPath(uncompressedFileName);
					Path p = Files.createFile(uncompressedFilePath);
					try (FileOutputStream fileOutput = new FileOutputStream(p.toString())) {
						while (bis.available() > 0) {
							fileOutput.write(bis.read());
						}
						fileOutput.close();
					} catch (FileNotFoundException e) {
						printlnErr(getClass(), " File=" + p);
						e.printStackTrace();
						bis.close();
						return false;
					}
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
			printlnErr(getClass(), "Unpacking FAILED for file '" + zipFilePath + "'");
			return false;
		}
		return true;
	}

	private boolean renameOriginalFolder() {
		// Does not work under Windows!

		String targetLocation = "roc-dynamicdisplays";

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
			folderName = "roc-dynamicdisplays-old" + zero + index;
			file = null;
			file = new File(folderName);
			file = new File(file.getAbsolutePath()); // Seems to be necessary for Windows. ?!?!
		}

		if (index > 0) {
			File fileOrig = new File(targetLocation);
			println(getClass(), "Permissions of " + file.getAbsolutePath());
			println(getClass(), "Renaming " + fileOrig.getAbsolutePath() + " to " + file.getAbsolutePath());

			try {
				// This fails in Windows because this JVM has something inside this folder open (this execution of the JVM,
				// preventing Windows from completing the rename. The work-around (then) must be in the controlling DOS
				// script.
				Files.move(fileOrig.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ex) {
				printlnErr(getClass(), "Renaming has failed");
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}

	private boolean renameNewFolder() {
		// Does not work in Windows!

		File fileNew = new File(tempFolder);
		File fileOld = new File("roc-dynamicdisplays");

		println(getClass(), "Renaming " + fileNew.getAbsolutePath() + " to " + fileOld.getAbsolutePath());

		try {
			Files.move(fileNew.toPath(), fileOld.toPath());
		} catch (IOException ex) {
			printlnErr(getClass(), "Renaming has failed");
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean hasSucceeded() {
		return succeeded;
	}

}
