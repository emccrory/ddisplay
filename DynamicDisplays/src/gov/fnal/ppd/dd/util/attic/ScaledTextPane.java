package gov.fnal.ppd.dd.util.attic;

/**
 * @author Stanislav Lapitsky
 * @version 1.0
 */

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;

import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class ScaledTextPane extends JTextPane {
	private static final long	serialVersionUID	= -5493276905046426938L;

	public ScaledTextPane() {
		super();
		final SimpleAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setFontSize(attrs, 16);
		setEditorKit(new ScaledEditorKit());
		StyledDocument doc = (StyledDocument) ScaledTextPane.this.getDocument();
		doc.setCharacterAttributes(0, 1, attrs, true);
		try {
			StyleConstants.setFontFamily(attrs, "Lucida Sans");
			doc.insertString(0, "Lusida Sans font test\n", attrs);

			StyleConstants.setFontFamily(attrs, "Lucida Bright");
			doc.insertString(0, "Lucida Bright font test\n", attrs);

			StyleConstants.setFontFamily(attrs, "Lucida Sans Typewriter");
			doc.insertString(0, "Lucida Sans Typewriter font test\n", attrs);
		} catch (BadLocationException ex) {}

	}

	public void repaint( int x, int y, int width, int height ) {
		super.repaint(0, 0, getWidth(), getHeight());
	}

	private static class ScaledEditorKit extends StyledEditorKit {
		public ViewFactory getViewFactory() {
			return new StyledViewFactory();
		}

		private static class StyledViewFactory implements ViewFactory {

			public View create( Element elem ) {
				String kind = elem.getName();
				if (kind != null) {
					if (kind.equals(AbstractDocument.ContentElementName)) {
						return new LabelView(elem);
					} else if (kind.equals(AbstractDocument.ParagraphElementName)) {
						return new ParagraphView(elem);
					} else if (kind.equals(AbstractDocument.SectionElementName)) {
						return new ScaledView(elem, View.Y_AXIS);
					} else if (kind.equals(StyleConstants.ComponentElementName)) {
						return new ComponentView(elem);
					} else if (kind.equals(StyleConstants.IconElementName)) {
						return new IconView(elem);
					}
				}

				// default to text display
				return new LabelView(elem);
			}

		}
	}

	// -----------------------------------------------------------------
	private static class ScaledView extends BoxView {
		public ScaledView( Element elem, int axis ) {
			super(elem, axis);
		}

		public double getZoomFactor() {
			Double scale = (Double) getDocument().getProperty("ZOOM_FACTOR");
			if (scale != null) {
				return scale.doubleValue();
			}

			return 1;
		}

		public void paint( Graphics g, Shape allocation ) {
			Graphics2D g2d = (Graphics2D) g;
			double zoomFactor = getZoomFactor();
			AffineTransform old = g2d.getTransform();
			g2d.scale(zoomFactor, zoomFactor);
			super.paint(g2d, allocation);
			g2d.setTransform(old);
		}

		public float getMinimumSpan( int axis ) {
			float f = super.getMinimumSpan(axis);
			f *= getZoomFactor();
			return f;
		}

		public float getMaximumSpan( int axis ) {
			float f = super.getMaximumSpan(axis);
			f *= getZoomFactor();
			return f;
		}

		public float getPreferredSpan( int axis ) {
			float f = super.getPreferredSpan(axis);
			f *= getZoomFactor();
			return f;
		}

		protected void layout( int width, int height ) {
			super.layout(new Double(width / getZoomFactor()).intValue(), new Double(height
					* getZoomFactor()).intValue());
		}

		public Shape modelToView( int pos, Shape a, Position.Bias b ) throws BadLocationException {
			double zoomFactor = getZoomFactor();
			Rectangle alloc;
			alloc = a.getBounds();
			Shape s = super.modelToView(pos, alloc, b);
			alloc = s.getBounds();
			alloc.x *= zoomFactor;
			alloc.y *= zoomFactor;
			alloc.width *= zoomFactor;
			alloc.height *= zoomFactor;

			return alloc;
		}

		public int viewToModel( float x, float y, Shape a, Position.Bias[] bias ) {
			double zoomFactor = getZoomFactor();
			Rectangle alloc = a.getBounds();
			x /= zoomFactor;
			y /= zoomFactor;
			alloc.x /= zoomFactor;
			alloc.y /= zoomFactor;
			alloc.width /= zoomFactor;
			alloc.height /= zoomFactor;

			return super.viewToModel(x, y, alloc, bias);
		}
	}
}
