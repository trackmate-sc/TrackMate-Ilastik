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
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class IlastikRunner
{

	private final static Context context = TMUtils.getContext();

	/**
	 * 
	 * @param <T>
	 * @param projectFilePath
	 * @param input
	 * @param classId
	 * @param probaThreshold
	 * @param logService
	 * @param statusService
	 * @param threshold
	 * @return
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

		System.out.println( "Interval: " + Util.printInterval( interval ) ); // DEBUG
		System.out.println( "Input: " + Util.printInterval( input ) ); // DEBUG

		final RandomAccessibleInterval< T > crop = Views.interval( input, interval );
		final RandomAccessibleInterval< T > zeroMinCrop = Views.zeroMin( crop );

		final ImgPlus< T > cropped = new ImgPlus<>( ImgView.wrap( zeroMinCrop, input.factory() ) );
		MetadataUtil.copyImgPlusMetadata( input, cropped );

		/*
		 * Discover and use Ilastik config.
		 */

		final IlastikOptions ilastikOptions = optionService.getOptions( IlastikOptions.class );
		final File executableFilePath = ilastikOptions.getExecutableFile();
		final int numThreads = ilastikOptions.getNumThreads();
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
		final int t0 = ( int ) interval.min( 2 );
		for ( int t = 0; t < proba.dimension( timeIndex ); t++ )
		{
			final List< Spot > spotsThisFrame;
			final ImgPlus< T > probaThisFrame = ImgPlusViews.hyperSlice( proba, timeIndex, t );
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
