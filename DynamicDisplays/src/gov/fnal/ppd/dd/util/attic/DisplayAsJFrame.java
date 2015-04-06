package gov.fnal.ppd.dd.util.attic;

import gov.fnal.ppd.dd.changer.ChannelCatalog;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.display.DisplayImpl;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class DisplayAsJFrame extends DisplayImpl {
	private static int		x	= 0, y = 0;

	private JFrame			f;

	private JTextPane		tp	= new JTextPane();

	private JLabel			dNum, cName, cNum, cDescr, errorMessage;

	public static boolean	webTesting;

	public DisplayAsJFrame(String ipName, int screenNumber, int number, String location, Color color, SignageType type) {
		super(ipName, screenNumber, number, number, location, color, type);
		initComponents();
	}

	private void initComponents() {
		JPanel p = new JPanel(new BorderLayout());
		if (getContent() == null)
			throw new IllegalArgumentException("No content defined!");

		// p.setOpaque(true);
		// p.setBackground(getPreferredHighlightColor());
		dNum = new JLabel("Display " + getVirtualDisplayNumber() + " (" + getCategory() + ")");
		dNum.setFont(new Font("SansSerif", Font.BOLD, 20));
		cName = new JLabel((this.getContent() != null ? this.getContent().getName() : "n/a"));
		cName.setFont(new Font("SansSerif", Font.BOLD, 15));
		errorMessage = new JLabel("");
		errorMessage.setFont(new Font("SansSerif", Font.BOLD, 15));

		Box b = Box.createVerticalBox();
		b.add(dNum);
		b.add(cName);
		b.add(errorMessage);

		if (this.getContent() instanceof Channel) {
			cNum = new JLabel("Channel " + (this.getContent() != null ? ((Channel) this.getContent()).getNumber() : "n/a"));
			b.add(cNum);
			cDescr = new JLabel((this.getContent() != null ? ((Channel) this.getContent()).getDescription() : "n/a"));
			cDescr.setFont(new Font("Serif", Font.ITALIC, 10));
			b.add(cDescr);
		}

		if (webTesting) {
			Box bb = Box.createHorizontalBox();
			bb.add(b);
			bb.add(createWebBox());
			bb.setBorder(BorderFactory.createLineBorder(Color.blue));
			p.add(bb, BorderLayout.CENTER);
			bb.setPreferredSize(new Dimension(820, 420));
		} else
			p.add(b, BorderLayout.CENTER);

		JLabel color = new JLabel(("" + getPreferredHighlightColor()).replace(getPreferredHighlightColor().getClass()
				.getCanonicalName(), ""));
		color.setOpaque(true);
		color.setBackground(getPreferredHighlightColor());
		p.add(color, BorderLayout.SOUTH);

		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getPreferredHighlightColor(), 10),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		f = new JFrame("Place holder for Display " + getVirtualDisplayNumber());
		if (webTesting)
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		else
			f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		f.setContentPane(p);
		f.pack();

		f.setLocation(x, y);
		x += 180;
		if (x > 900) {
			x = 0;
			y += 135;
		}

		f.setVisible(true);
	}

	private Component createWebBox() {
		try {
			tp.setPage(new URL(getContent().getURI().toString()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		JPanel p = new JPanel();
		p.add(tp);
		JScrollPane sp = new JScrollPane(p);
		tp.setMaximumSize(new Dimension(600, 400));
		sp.setMaximumSize(new Dimension(600, 400));
		tp.setBorder(BorderFactory.createLineBorder(Color.red));
		return sp;
	}

	protected boolean localSetContent() {
		if (webTesting)
			try {
				tp.setPage(new URL(getContent().getURI().toString()));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		cName.setText(this.getContent().getName());
		if (this.getContent() instanceof Channel) {
			cNum.setText("Channel " + ((Channel) this.getContent()).getNumber());
		} else {
			cNum.setText("Generic Signage Content");
		}
		cDescr.setText((this.getContent()).getDescription());
		errorMessage.setText("");
		return true;
	}

	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);

		localSetContent();
	}

	protected void error(String string) {
		errorMessage.setText(string);
	}

	public final static void main(String[] args) {
		webTesting = true;
		ChannelCatalog list = ChannelCatalogFactory.getInstance();
		DisplayAsJFrame display = new DisplayAsJFrame("IPName", 0, 0, "Location", Color.green, SignageType.XOC);

		Map<String, SignageContent> pub = list.getPublicChannels();
		for (String key : pub.keySet()) {
			display.setContent(pub.get(key));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public String getMessagingName() {
		// TODO Auto-generated method stub
		return null;
	}
}
