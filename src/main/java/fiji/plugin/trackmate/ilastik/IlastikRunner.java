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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor.PixelPredictionType;
import org.ilastik.ilastik4ij.executors.PixelClassification;
import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.options.OptionsService;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.MetadataUtil;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class IlastikRunner
{

	private final static Context context = TMUtils.getContext();

	/**
	 * @return a new {@link SpotCollection}
	 * @throws IOException
	 *             if the Ilastik file cannot be found.
	 */
	public static < T extends RealType< T > & NativeType< T > > SpotCollection run(
			final ImgPlus< T > input,
			final Interval interval,
			final double[] calibration,
			final String projectFilePath,
			final long classId,
			final double probaThreshold ) throws IOException
	{

		final LogService logService = context.getService( LogService.class );
		final StatusService statusService = context.getService( StatusService.class );
		final OptionsService optionService = context.getService( OptionsService.class );

		/*
		 * Properly set the image to process: crop it.
		 */

		final RandomAccessibleInterval< T > crop = Views.interval( input, interval );
		final RandomAccessibleInterval< T > zeroMinCrop = Views.zeroMin( crop );

		final ImgPlus< T > cropped = new ImgPlus<>( ImgView.wrap( zeroMinCrop, input.factory() ) );
		MetadataUtil.copyImgPlusMetadata( input, cropped );

		/*
		 * Discover and use Ilastik config.
		 */

		final IlastikOptions ilastikOptions = optionService.getOptions( IlastikOptions.class );
		final File executableFilePath = ilastikOptions.getExecutableFile();
		final int numThreads = ilastikOptions.getNumThreads() < 0 ? Runtime.getRuntime().availableProcessors() : ilastikOptions.getNumThreads();
		final int maxRamMb = ilastikOptions.getMaxRamMb();

		/*
		 * Run Ilastik.
		 */

		final File projectFile = new File( projectFilePath );
		final PixelClassification classifier = new PixelClassification(
				executableFilePath,
				projectFile,
				logService,
				statusService,
				numThreads,
				maxRamMb );
		final PixelPredictionType predictionType = PixelPredictionType.Probabilities;


		final ImgPlus< T > output = classifier.classifyPixels( cropped, predictionType );
		final ImgPlus< T > proba = ImgPlusViews.hyperSlice( output, output.dimensionIndex( Axes.CHANNEL ), classId );

		/*
		 * Create ROIs from proba.
		 */

		final SpotCollection spots = new SpotCollection();
		final int timeIndex = proba.dimensionIndex( Axes.TIME );
		final int t0 = interval.numDimensions() > 2 ? ( int ) interval.min( 2 ) : 0;
		for ( int t = 0; t < proba.dimension( timeIndex ); t++ )
		{
			final List< Spot > spotsThisFrame;
			final ImgPlus< T > probaThisFrame = TMUtils.hyperSlice( proba, 0, t );

			if ( DetectionUtils.is2D( probaThisFrame ) )
			{
				/*
				 * 2D: we compute and store the contour.
				 */
				final boolean simplify = true;
				spotsThisFrame = MaskUtils.fromThresholdWithROI(
						probaThisFrame,
						probaThisFrame,
						calibration, 
						probaThreshold, 
						simplify, 
						numThreads, 
						probaThisFrame );
			}
			else
			{
				/*
				 * 3D: We create spots of the same volume that of the region.
				 */
				spotsThisFrame = MaskUtils.fromThreshold(
						probaThisFrame,
						probaThisFrame,
						calibration,
						probaThreshold,
						numThreads,
						probaThisFrame );
			}

			/*
			 * Shift the spots (before this step, they have the top-left corner
			 * of the interval as (0, 0) coordinates).
			 */
			final int maxD = DetectionUtils.is2D( probaThisFrame ) ? 2 : 3;
			for ( final Spot spot : spotsThisFrame )
			{
				for ( int d = 0; d < maxD; d++ )
				{
					final double pos = spot.getDoublePosition( d );
					final double newPos = pos + interval.min( d ) * calibration[ d ];
					spot.putFeature( Spot.POSITION_FEATURES[ d ], newPos );
				}
			}

			spots.put( t + t0, spotsThisFrame );
		}
		return spots;
	}
}
