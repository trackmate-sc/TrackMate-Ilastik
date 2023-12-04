/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2021 - 2023 TrackMate developers.
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

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SMOOTHING_SCALE;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_CLASSIFIER_FILEPATH;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_CLASS_INDEX;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_PROBA_THRESHOLD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.util.DetectionPreview;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import ij.gui.Roi;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class IlastikDetectionPreviewer< T extends RealType< T > & NativeType< T > > extends DetectionPreview
{

	private ImagePlus previousImp;

	private int previousFrame = -1;

	private String previousClassifierFilePath;

	private IlastikRunner< T > ilastikRunner;

	private int previousClassIndex = -1;

	private int previousChannel;

	private Roi previousRoi = null;

	public IlastikDetectionPreviewer(
			final Model model,
			final Settings settings,
			final Supplier< Map< String, Object > > detectionSettingsSupplier,
			final Supplier< Integer > currentFrameSupplier )
	{
		super(
				model,
				settings,
				new IlastikDetectorFactory<>(),
				detectionSettingsSupplier,
				currentFrameSupplier,
				null,
				null,
				null );
	}

	public ImagePlus getLastProbabilityImage()
	{
		final RandomAccessibleInterval< T > img = ilastikRunner.getLastOutput();
		if ( img == null )
			return null;

		final ImagePlus imp = ImageJFunctions.wrap( img, "Probability map" );
		imp.getStack().setSliceLabel( "Frame " + previousFrame, 0 );
		return imp;
	}

	@Override
	protected Pair< Model, Double > runPreviewDetection(
			final Settings settings,
			final int frame,
			final SpotDetectorFactoryBase< ? > detectorFactory,
			final Map< String, Object > detectorSettings,
			final String thresholdKey )
	{
		final Logger logger = getLogger();

		/*
		 * Source image (all time-points, possible multi-channels.
		 */
		final ImgPlus< T > img = TMUtils.rawWraps( settings.imp );

		/*
		 * Unwrap detector settings.
		 */
		final Map< String, Object > dSettings = new HashMap<>( detectorSettings );
		final String classifierPath = ( String ) dSettings.get( KEY_CLASSIFIER_FILEPATH );
		final int classIndex = ( Integer ) dSettings.get( KEY_CLASS_INDEX );
		final double probaThreshold = ( Double ) dSettings.get( KEY_PROBA_THRESHOLD );
		// In ImgLib2, dimensions are 0-based.
		final int channel = ( Integer ) dSettings.get( KEY_TARGET_CHANNEL ) - 1;
		final Object smoothingObj = dSettings.get( KEY_SMOOTHING_SCALE );
		final double smoothingScale = smoothingObj == null
				? -1.
				: ( ( Number ) smoothingObj ).doubleValue();

		/*
		 * Interval with one time-frame.
		 */
		final Settings lSettings = new Settings( settings.imp );
		lSettings.tstart = frame;
		lSettings.tend = frame;
		lSettings.zstart = settings.zstart;
		lSettings.zend = settings.zend;
		final Interval interval = TMUtils.getIntervalWithTime( img, lSettings );

		/*
		 * First test to make sure we can read the classifier file.
		 */
		final Object obj = dSettings.get( KEY_CLASSIFIER_FILEPATH );
		if ( obj == null )
		{
			logger.error( "The path to the ilastik project file is not set." );
			return null;
		}

		final String classifierFilePath = ( String ) obj;
		final StringBuilder errorHolder = new StringBuilder();
		if ( !IOUtils.canReadFile( classifierFilePath, errorHolder ) )
		{
			logger.error( "Problem with the ilastik project file: " + errorHolder.toString() );
			return null;
		}

		/*
		 * Shall we recompute probabilities?
		 */
		boolean recomputeProba = false;
		if ( settings.imp != previousImp )
			recomputeProba = true;
		previousImp = settings.imp;

		if ( frame != previousFrame )
			recomputeProba = true;
		previousFrame = frame;

		final Roi currentRoi = settings.imp.getRoi();
		if ( !areEqual( currentRoi, previousRoi ) )
			recomputeProba = true;
		previousRoi = currentRoi == null
				? null
				: ( Roi ) currentRoi.clone();
		settings.setRoi( currentRoi );

		if ( !classifierFilePath.equals( previousClassifierFilePath ) )
			recomputeProba = true;
		previousClassifierFilePath = classifierFilePath;

		if ( classIndex != previousClassIndex )
			recomputeProba = true;
		previousClassIndex = classIndex;

		if ( channel != previousChannel )
			recomputeProba = true;
		previousChannel = channel;

		/*
		 * Recompute probabilities.
		 */
		if ( recomputeProba || ilastikRunner == null )
		{
			ilastikRunner = new IlastikRunner<>( classifierPath );
			final RandomAccessibleInterval< T > probabilities = ilastikRunner.computeProbabilities( img, channel, interval, classIndex );
			if ( probabilities == null )
			{
				logger.error( "Problem computing probabilities: " + ilastikRunner.getErrorMessage() );
				return null;
			}
		}

		/*
		 * Create spots from probabilities.
		 */
		logger.log( "Creating spots from probabilities." );
		final boolean simplify = true;
		final SpotCollection sc = ilastikRunner.getSpotsFromLastProbabilities( probaThreshold, simplify, smoothingScale );
		if ( sc == null )
		{
			logger.error( "Problem creating spots: " + ilastikRunner.getErrorMessage() );
			return null;
		}

		/*
		 * Possible prune spots.
		 */
		final SpotCollection spots;
		final Roi roi = settings.getRoi();
		if ( roi != null )
		{
			spots = new SpotCollection();
			final double[] calibration = TMUtils.getSpatialCalibration( settings.imp );
			for ( int t = settings.tstart; t <= settings.tend; t++ )
			{
				final List< Spot > spotsThisFrame = new ArrayList<>();
				final Iterable< Spot > spotsIt = sc.iterable( t, false );
				if ( spotsIt == null )
					continue;

				for ( final Spot spot : spotsIt )
				{
					if ( roi.contains(
							( int ) Math.round( spot.getFeature( Spot.POSITION_X ) / calibration[ 0 ] ),
							( int ) Math.round( spot.getFeature( Spot.POSITION_Y ) / calibration[ 1 ] ) ) )
					{
						spotsThisFrame.add( spot );
					}
				}
				spots.put( t, spotsThisFrame );
			}
		}
		else
		{
			spots = sc;
		}

		final Model model = new Model();
		model.setSpots( spots, false );
		model.getSpots().filter( new FeatureFilter( Spot.QUALITY, probaThreshold, true ) );

		return new ValuePair< Model, Double >( model, Double.NaN );
	}

	private static boolean areEqual( final Roi roi1, final Roi roi2 )
	{
		if ( roi1 == null && roi2 == null )
			return true;

		if ( roi1 == null || roi2 == null )
			return false;

		// Check if the types are the same
		if ( roi1.getType() != roi2.getType() )
			return false;

		// Check if the number of coordinates is the same
		if ( roi1.getPolygon().npoints != roi2.getPolygon().npoints )
			return false;

		// Check if the coordinates are the same
		for ( int i = 0; i < roi1.getPolygon().npoints; i++ )
		{
			if ( roi1.getPolygon().xpoints[ i ] != roi2.getPolygon().xpoints[ i ] ||
					roi1.getPolygon().ypoints[ i ] != roi2.getPolygon().ypoints[ i ] )
				return false;
		}

		return true;
	}

}
