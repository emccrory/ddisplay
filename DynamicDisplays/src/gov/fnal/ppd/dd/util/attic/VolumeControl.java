package gov.fnal.ppd.dd.util.attic;

import java.util.LinkedList;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * Copied from http://stackoverflow.com/questions/953598/audio-volume-control-increase-or-decrease-in-java
 * 
 * Control the volume of the machine.
 * 
 * *********************************************************************************************************************************
 * This class does not work on Linux (yet). It seems that the answer is, "sound support in Java/Linux is spotty at best."
 * *************************************************************************************************************************
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public final class VolumeControl {

	private VolumeControl() {
	}

	private static LinkedList<Line>	speakers	= new LinkedList<Line>();

	private final static void findSpeakers() {
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		for (Mixer.Info mixerInfo : mixers) {
			if (mixerInfo.getName().equals("PulseAudio Mixer") || mixerInfo.getName().equals("Port Intel [hw:0]")
					|| mixerInfo.getName().equals("default [default]")) {
				System.out.println("Skipping Mixer info: " + mixerInfo.getName());
				continue;
			}
			System.out.println("Using Mixer info: " + mixerInfo.getName());

			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			Line.Info[] lines = mixer.getSourceLineInfo();

			for (Line.Info info : lines) {
				try {
					Line line = mixer.getLine(info);
					speakers.add(line);
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException iaEx) {
					iaEx.printStackTrace();
				}
			}
		}
	}

	static {
		findSpeakers();
		System.out.println("Found " + speakers.size() + " speakers");
	}

	/**
	 * @return the current volume level
	 */
	public static float getVolume() {
		float retval = 0.0f;
		System.out.println("Getting volume level");
		for (Line line : speakers) {
			try {
				line.open();
				FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				float r = control.getValue();
				if (r > retval)
					retval = r;
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return retval;
	}

	/**
	 * @param level
	 *            the new volume level
	 */
	public static void setVolume(final float level) {
		System.out.println("setting volume to " + level);
		for (Line line : speakers) {
			try {
				line.open();
				FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				control.setValue(limit(control, level));
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				System.err.println("Line name: " + line);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Mute the audio
	 */
	public static void setMute() {
		System.out.println("Muting volume");
		for (Line line : speakers) {
			try {
				line.open();
				FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
				control.setValue(limit(control, control.getMinimum()));
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	private static float limit(FloatControl control, float level) {
		System.out.println("Maximum: " + control.getMaximum());
		System.out.println("Minimum: " + control.getMinimum());
		return Math.min(control.getMaximum(), Math.max(control.getMinimum(), level));
	}

	private static void setVolume(SourceDataLine source, int volume) {
		try {
			FloatControl gainControl = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
			BooleanControl muteControl = (BooleanControl) source.getControl(BooleanControl.Type.MUTE);
			if (volume == 0) {
				muteControl.setValue(true);
			} else {
				muteControl.setValue(false);
				gainControl.setValue((float) (Math.log(volume / 100d) / Math.log(10.0) * 20.0));
			}
		} catch (Exception e) {
			System.err.println("unable to set the volume to the provided source");
			e.printStackTrace();
		}
	}

	public static void mainn(String[] args) {
		setVolume(10);
		try {
			Thread.sleep(10000);
			setVolume(0);
			Thread.sleep(10000);
			setVolume(100);
			Thread.sleep(10000);
			setMute();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] argv) throws Exception {
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();
		System.out.println("There are " + mixers.length + " mixer info objects");
		for (Mixer.Info mixerInfo : mixers) {
			System.out.println("mixer name: " + mixerInfo.getName());
			Mixer mixer = AudioSystem.getMixer(mixerInfo);
			Line.Info[] lineInfos = mixer.getTargetLineInfo(); // target, not source
			for (Line.Info lineInfo : lineInfos) {
				System.out.println("  Line.Info: " + lineInfo);
				Line line = null;
				boolean opened = true;
				try {
					line = mixer.getLine(lineInfo);
					opened = line.isOpen() || line instanceof Clip;
					if (!opened) {
						line.open();
					}
					FloatControl volCtrl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
					System.out.println("\n\n    volCtrl.getValue() = " + volCtrl.getValue() + "\n\n");
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException iaEx) {
					iaEx.printStackTrace();
					System.out.println("    " + iaEx);
				} finally {
					if (line != null && !opened) {
						line.close();
					}
				}
			}
		}

	}
}