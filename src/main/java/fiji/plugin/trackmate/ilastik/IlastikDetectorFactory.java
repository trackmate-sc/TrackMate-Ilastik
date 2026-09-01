/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2025 TrackMate developers.
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
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SMOOTHING_SCALE;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import fiji.plugin.trackmate.detection.SpotGlobalDetectorFactory;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

@Plugin( type = SpotDetectorFactory.class, priority = Priority.LOW - 4. )
public class IlastikDetectorFactory< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetectorFactory< T >
{

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
			+ "It works for 2D and 3D images."
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
			+ "also cite the ilastik paper: <a href=\"https://doi.org/10.1038/s41592-019-0582-9\">Berg, S., Kutra, D., Kroeger, T. et al. ilastik: "
			+ "interactive machine learning for (bio)image analysis. Nat Methods 16, 1226–1232 (2019)</a>"
			+ "</html>";

	public static final String DOC_URL = "https://imagej.net/plugins/trackmate/trackmate-ilastik";

	/*
	 * METHODS
	 */

	@Override
	public SpotGlobalDetector< T > getDetector( final ImgPlus< T > img, final Map< String, Object > settings, final Interval interval )
	{
		final String classifierPath = ( String ) settings.get( KEY_CLASSIFIER_FILEPATH );
		final int classIndex = ( Integer ) settings.get( KEY_CLASS_INDEX );
		final double probaThreshold = ( Double ) settings.get( KEY_PROBA_THRESHOLD );
		// In ImgLib2, dimensions are 0-based.
		final int channel = ( Integer ) settings.get( KEY_TARGET_CHANNEL ) - 1;
		final Object smoothingObj = settings.get( KEY_SMOOTHING_SCALE );
		final double smoothingScale = smoothingObj == null
				? -1.
				: ( ( Number ) smoothingObj ).doubleValue();

		final IlastikDetector< T > detector = new IlastikDetector<>(
				img,
				interval,
				channel,
				classifierPath,
				classIndex,
				probaThreshold,
				smoothingScale );
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
		settings.put( KEY_SMOOTHING_SCALE, -1. );
		return settings;
	}

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return IlastikDetectorConfigurationPanel.ICON;
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

	@Override
	public String getUrl()
	{
		return DOC_URL;
	}

	@Override
	public boolean has2Dsegmentation()
	{
		return true;
	}

	@Override
	public boolean has3Dsegmentation()
	{
		return true;
	}

	@Override
	public IlastikDetectorFactory< T > copy()
	{
		return new IlastikDetectorFactory<>();
	}
}
