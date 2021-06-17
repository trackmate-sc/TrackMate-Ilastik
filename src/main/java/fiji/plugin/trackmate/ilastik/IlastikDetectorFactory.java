/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2020 - 2021 The Institut Pasteur.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.ilastik;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readStringAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeTargetChannel;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class )
public class IlastikDetectorFactory< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetectorFactory< T >
{

	/*
	 * CONSTANTS
	 */

	/**
	 * The key to the parameter that stores the path to the Ilastik file.
	 */
	public static final String KEY_CLASSIFIER_FILEPATH = "CLASSIFIER_FILEPATH";

	/**
	 * The key to the parameter that stores the probability / score threshold.
	 * Values are {@link Double}s from 0 to 1.
	 */
	public static final String KEY_PROBA_THRESHOLD = "PROBA_THRESHOLD";

	public static final Double DEFAULT_PROBA_THRESHOLD = Double.valueOf( 0.5 );

	/**
	 * The key to the parameter that stores the index of the class to use to
	 * create objects. Values are positive integers.
	 */
	public static final String KEY_CLASS_INDEX = "CLASS_INDEX";

	public static final Integer DEFAULT_CLASS_INDEX = Integer.valueOf( 0 );

	/** A string key identifying this factory. */
	public static final String DETECTOR_KEY = "ILASTIK_DETECTOR";

	/** The pretty name of the target detector. */
	public static final String NAME = "ilastik detector";

	/** An html information text. */
	public static final String INFO_TEXT = "<html>"
			+ "This detector relies on ilastik to detect objects."
			+ "<p>"
			+ "It only works for 2D images."
			+ "And for this detector to work, the 'ilastik' update site "
			+ "must be activated in your Fiji installation. "
			+ "You also need to properly configure the Ilastik Fiji plugin."
			+ "You have to specify the ilastik software path, max RAM and max"
			+ "number of threads to use in the ilastik option menu: <i>Plugins "
			+ "> ilastik > Configure ilastik executable location</i>.  "
			+ "<p>"
			+ "This detector allows you to apply an ilastik pixel classifier to "
			+ "the source image. It will give the probability map for each class "
			+ "that will be thresholded to yield objects. Spots are created with "
			+ "these objects, with a quality equal to the maximal value of the "
			+ "probability image in the cell. "
			+ "<p>"
			+ "If you use this detector for your work, please be so kind as to "
			+ "also cite the ilastik paper: <i>Berg, S., Kutra, D., Kroeger, T. et al. ilastik: "
			+ "interactive machine learning for (bio)image analysis. Nat Methods 16, 1226–1232 (2019)</i>"
			+ "</html>";

	/*
	 * FIELDS
	 */

	/** The image to operate on. Multiple frames, single channel. */
	protected ImgPlus< T > img;

	protected Map< String, Object > settings;

	protected String errorMessage;

	/*
	 * METHODS
	 */

	@Override
	public SpotGlobalDetector< T > getDetector( final Interval interval )
	{
		final double[] calibration = TMUtils.getSpatialCalibration( img );
		final String classifierPath = ( String ) settings.get( KEY_CLASSIFIER_FILEPATH );
		final ImgPlus< T > imFrame = prepareImg();
		final int classIndex = ( Integer ) settings.get( KEY_CLASS_INDEX );
		final double probaThreshold = ( Double ) settings.get( KEY_PROBA_THRESHOLD );

		final IlastikDetector< T > detector = new IlastikDetector<>(
				imFrame,
				interval,
				calibration,
				classifierPath,
				classIndex,
				probaThreshold );
		return detector;
	}

	@Override
	public boolean forbidMultithreading()
	{
		/*
		 * We want to run one frame after another, because the inference for one
		 * frame takes all the resources anyway.
		 */
		return true;
	}

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = writeTargetChannel( settings, element, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_CLASSIFIER_FILEPATH, String.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_CLASS_INDEX, Integer.class, errorHolder );
		ok = ok && writeAttribute( settings, element, KEY_PROBA_THRESHOLD, Double.class, errorHolder );

		if ( !ok )
			errorMessage = errorHolder.toString();

		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok && readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		ok = ok && readStringAttribute( element, settings, KEY_CLASSIFIER_FILEPATH, errorHolder );
		ok = ok && readIntegerAttribute( element, settings, KEY_CLASS_INDEX, errorHolder );
		ok = ok && readDoubleAttribute( element, settings, KEY_PROBA_THRESHOLD, errorHolder );

		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new IlastikDetectorConfigurationPanel( settings, model );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap<>();
		settings.put( KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL );
		settings.put( KEY_CLASSIFIER_FILEPATH, "" );
		settings.put( KEY_CLASS_INDEX, DEFAULT_CLASS_INDEX );
		settings.put( KEY_PROBA_THRESHOLD, DEFAULT_PROBA_THRESHOLD );
		return settings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CLASSIFIER_FILEPATH, String.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CLASS_INDEX, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_PROBA_THRESHOLD, Double.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList<>();
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_CLASSIFIER_FILEPATH );
		mandatoryKeys.add( KEY_CLASS_INDEX );
		mandatoryKeys.add( KEY_PROBA_THRESHOLD );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		if ( !ok )
			errorMessage = errorHolder.toString();

		// Extra test to make sure we can read the classifier file.
		if ( ok )
		{
			final Object obj = settings.get( KEY_CLASSIFIER_FILEPATH );
			if ( obj == null )
			{
				errorMessage = "The path to the ilastik file is not set.";
				return false;
			}

			if ( !IOUtils.canReadFile( ( String ) obj, errorHolder ) )
			{
				errorMessage = "Problem with ilastik file: " + errorHolder.toString();
				return false;
			}
		}

		return ok;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return DETECTOR_KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	/**
	 * Return 1-channel, all time-points, all-Zs if any.
	 * 
	 * @return an {@link ImgPlus}.
	 */
	protected ImgPlus< T > prepareImg()
	{
		final int cDim = img.dimensionIndex( Axes.CHANNEL );
		final ImgPlus< T > imFrame;
		if ( cDim < 0 )
		{
			imFrame = img;
		}
		else
		{
			// In ImgLib2, dimensions are 0-based.
			final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
			imFrame = ImgPlusViews.hyperSlice( img, cDim, channel );
		}
		return imFrame;
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}
}
