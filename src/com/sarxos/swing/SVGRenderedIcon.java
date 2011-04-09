package com.sarxos.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.swing.ImageIcon;

import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGRoot;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;


/**
 * Icon instance with SVG object as image resource
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class SVGRenderedIcon extends SVGIcon {

	protected double rotation = 0;

	/**
	 * SVG icon
	 */
	public SVGRenderedIcon() {
		super();
		initialize();
	}

	/**
	 * SVG icon
	 * 
	 * @param svguri - URI to the SVG resource
	 */
	public SVGRenderedIcon(URI svguri) {
		super();
		initialize();
		setSvgURI(svguri);
	}

	/**
	 * SVG icon
	 * 
	 * @param svgpath - SVG resource path
	 */
	public SVGRenderedIcon(String svgpath) {
		super();
		initialize();

		/*
		 * NOTE! Poprzez class loadera pobieramy strumien do resoursow svg, a
		 * nastepnie poprzez universum SVG ladujemy go jako URI do salamandra.
		 */
		ClassLoader cl = getClass().getClassLoader();
		InputStream is = cl.getResourceAsStream(svgpath);
		SVGUniverse universe = SVGCache.getSVGUniverse();

		URI svguri = null;
		try {
			svguri = universe.loadSVG(is, svgpath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		/*
		 * NOTE! Pobieramy diagram (czyli czysty obrazek svg) i znajdujac jego
		 * korzen wyciagamy z niego oryginalna wysokosc i szerokosc.
		 */
		SVGDiagram diagram = universe.getDiagram(svguri);
		SVGRoot root = diagram.getRoot();

		int w = (int) root.getDeviceWidth();
		int h = (int) root.getDeviceHeight();

		Dimension dim = new Dimension(w, h);

		setPreferredSize(dim);
		setSvgURI(svguri);
	}

	/**
	 * SVG icon initialization.
	 */
	protected void initialize() {
		setAntiAlias(true);
		setInterpolation(SVGIcon.INTERP_BICUBIC);
		setScaleToFit(true);
		setClipToViewbox(false);
	}

	/**
	 * Rysowanie ikony.<br>
	 * 
	 * @param component - komponent na którym ma byæ rysowana ikona
	 * @param g - obiekt Graphics
	 * @param x - x-owa pozycja rysowania
	 * @param y - y-owa pozycja rysowania
	 * @see com.kitfox.svg.app.beans.SVGIcon#paintIcon(java.awt.Component,
	 *      java.awt.Graphics, int, int)
	 */
	@Override
	public void paintIcon(Component component, Graphics g, int x, int y) {

		Graphics2D g2 = (Graphics2D) g;

		Object aliasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		if (aliasing == null) {

			/*
			 * NOTE! Zapobiega NPE gdy dostarczymy obiekt Graphics spoza j¹dra
			 * wirtualnej maszyny (np. z i-texta).
			 */

			aliasing = RenderingHints.VALUE_ANTIALIAS_DEFAULT;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aliasing);
		}

		if (rotation != 0) {

			double width = getIconWidth();
			double height = getIconHeight();

			AffineTransform fitToRotate = new AffineTransform();

			// rotacja o 90 stopni
			fitToRotate.rotate(
					rotation,
					width / 2,
					height / 2
			);

			// skalowanie do nowego rozmiaru
			double sx = height / width;
			double sy = width / height;
			fitToRotate.scale(sx, sy);

			// przesuniecie do naroznika
			fitToRotate.translate(
					sy * (width - height) / 2,
					sx * (height - width) / 2
			);

			AffineTransform oldTr = g2.getTransform();

			g2.setTransform(fitToRotate);
			super.paintIcon(component, g, x, y);
			g2.setTransform(oldTr);

		} else {
			super.paintIcon(component, g, x, y);
		}
	}

	/**
	 * Zwraca obiekt ImageIcon reprezentuj¹cy obraz SVG.<br>
	 * 
	 * @return ImageIcon
	 */
	public ImageIcon getImageIcon() {
		ImageIcon iim = new ImageIcon();
		iim.setImage(getImage());
		return iim;
	}

	/**
	 * Zwraca zbuforowany obraz ikony.<br>
	 * 
	 * @return BufferedImage
	 */
	public BufferedImage getImage() {

		int width = getIconWidth();
		int height = getIconHeight();

		BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = buffer.createGraphics();

		Shape oldClip = g2.getClip();

		g2.setClip(0, 0, width, height);
		paintIcon(null, g2, 0, 0);
		g2.setClip(oldClip);
		g2.dispose();

		return buffer;
	}

	/**
	 * Return image with specified size.
	 * 
	 * @param size
	 * @return Will return instance of BufferedImage
	 */
	public BufferedImage getImage(Dimension size) {
		Dimension oldDimension = getPreferredSize();
		setPreferredSize(size);
		BufferedImage buff = getImage();
		setPreferredSize(oldDimension);
		return buff;
	}

	/**
	 * Will return volatile image.
	 * 
	 * @return VolatileImage
	 */
	public VolatileImage getVolatileImage() {

		int w = getIconWidth();
		int h = getIconHeight();

		BufferedImage buffer = new BufferedImage(1, 1, BufferedImage.TYPE_BYTE_BINARY);
		Graphics2D g2 = buffer.createGraphics();
		GraphicsConfiguration gc = g2.getDeviceConfiguration();
		VolatileImage img = gc.createCompatibleVolatileImage(w, h, Transparency.TRANSLUCENT);

		g2 = img.createGraphics();

		Shape oc = g2.getClip();

		g2.setClip(0, 0, w, h);
		paintIcon(null, g2, 0, 0);
		g2.setClip(oc);
		g2.dispose();

		return img;
	}

	/**
	 * Will return volatile image with desired size.
	 * 
	 * @param size
	 * @return Return volatile image
	 */
	public VolatileImage getVolatileImage(Dimension size) {
		Dimension oldDimension = getPreferredSize();
		setPreferredSize(size);
		VolatileImage buff = getVolatileImage();
		setPreferredSize(oldDimension);
		return buff;
	}

	/**
	 * Set icon rotation.
	 * 
	 * @param rotation
	 */
	public void setRotation(double rotation) {
		this.rotation = rotation;
	}

	/**
	 * @return Return icon rotation.
	 */
	public double getRotation() {
		return rotation;
	}

	// @Override
	// public void setPreferredSize(Dimension preferredSize) {
	// super.setPreferredSize(preferredSize);
	// }
}
