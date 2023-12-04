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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ilastik.ilastik4ij.ui.IlastikOptions;
import org.ilastik.ilastik4ij.workflow.PixelClassificationCommand;
import org.ilastik.ilastik4ij.workflow.WorkflowCommand;
import org.scijava.Context;
import org.scijava.options.OptionsService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5ObjectInformation;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.MaskUtils;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.ops.MetadataUtil;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.ImgView;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public class IlastikRunner< T extends RealType< T > & NativeType< T > > implements MultiThreaded
{

	private String errorMessage;

	private int numThreads;

	/**
	 * Caches the probabilities computed from the last call to
	 * {@link #computeProbabilities(ImgPlus, int, Interval, long)},
	 */
	private ImgPlus< T > lastOutput;

	/**
	 * The spatial calibration of the last image input to
	 * {@link #computeProbabilities(ImgPlus, int, Interval, long)}.
	 */
	private double[] lastCalibration;

	/**
	 * Stores whether the ilastik model is built on a single channel or on
	 * multiple channels.
	 */
	private final int modelNChannel;

	private final PixelClassificationCommand< T > classifier;

	private Interval lastExtendedInterval;

	public IlastikRunner( final String projectFilePath )
	{
		this.modelNChannel = getModelNChannel( projectFilePath );
		setNumThreads();

		// The ilastik classifier.
		final File projectFile = new File( projectFilePath );
		this.classifier = new PixelClassificationCommand<>();
		final Context context = TMUtils.getContext();
		classifier.setContext( context );
		classifier.projectFileName = projectFile;
		classifier.pixelClassificationType = WorkflowCommand.ROLE_PROBABILITIES;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public RandomAccessibleInterval< T > computeProbabilities(
			final ImgPlus< T > img,
			final int channel,
			final Interval interval,
			final long classId )
	{
		errorMessage = null;
		lastCalibration = TMUtils.getSpatialCalibration( img );

		/*
		 * Investigate whether the ilastik model is built on a single channel or
		 * on multiple channels.
		 */

		final ImgPlus< T > input;
		final Interval extendedInterval;
		if ( modelNChannel > 1 )
		{
			/*
			 * The model was trained on images with more that one channel. In
			 * that case we assume that the image to perform inference has the
			 * same number of channel with the same order, and we pass it whole
			 * to ilastik.
			 */
			input = img;
			final long[] min = new long[ input.numDimensions() ];
			final long[] max = new long[ input.numDimensions() ];
			// Source interval is always x, y, z (if any), t.
			int axisidSource = 0;
			int axisidTarget = 0;
			// X
			min[ axisidSource ] = interval.min( axisidTarget );
			max[ axisidSource ] = interval.max( axisidTarget );
			// Y
			axisidSource++;
			axisidTarget++;
			min[ axisidSource ] = interval.min( axisidTarget );
			max[ axisidSource ] = interval.max( axisidTarget );
			// Z.
			if ( img.dimensionIndex( Axes.Z ) >= 0 )
			{
				axisidSource++;
				axisidTarget++;
				min[ axisidSource ] = interval.min( axisidTarget );
				max[ axisidSource ] = interval.max( axisidTarget );
			}
			// C - not present in the source interval.
			final int caxis = img.dimensionIndex( Axes.CHANNEL );
			if ( caxis >= 0 )
			{
				// If we do not have channel axis here, we are screwed anyway.
				axisidSource++;
				min[ axisidSource ] = img.min( caxis );
				max[ axisidSource ] = img.max( caxis );
			}
			// T
			axisidSource++;
			axisidTarget++;
			min[ axisidSource ] = interval.min( axisidTarget );
			max[ axisidSource ] = interval.max( axisidTarget );

			extendedInterval = FinalInterval.wrap( min, max );
		}
		else
		{
			/*
			 * The model was trained on images with one channel. In that case we
			 * make it possible to apply it on one of the channel of the
			 * possibly multi-channel input image. We extract the channel
			 * specified by the user and pass it to ilastik.
			 */
			input = prepareImg( img, channel );
			extendedInterval = interval;
		}
		this.lastExtendedInterval = extendedInterval;

		/*
		 * Properly set the image to process: crop it.
		 */

		final RandomAccessibleInterval< T > crop = Views.interval( input, extendedInterval );
		final RandomAccessibleInterval< T > zeroMinCrop = Views.zeroMin( crop );

		final ImgPlus< T > cropped = new ImgPlus<>( ImgView.wrap( zeroMinCrop, input.factory() ) );
		MetadataUtil.copyImgPlusMetadata( input, cropped );

		final Context context = TMUtils.getContext();
		classifier.inputImage = new DefaultDataset( context, cropped );
		classifier.run();

		final ImgPlus< T > output = classifier.predictions;
		ImgPlus< T > proba = ImgPlusViews.hyperSlice( output, output.dimensionIndex( Axes.CHANNEL ), classId );

		// Do we have an unwarranted Z dim that was added?
		final int zIndex = proba.dimensionIndex( Axes.Z );
		if ( DetectionUtils.is2D( img ) && zIndex >= 0 )
			proba = ImgPlusViews.hyperSlice( proba, zIndex, 0 );

		MetadataUtil.copyImgPlusMetadata( cropped, proba );

		this.lastOutput = proba;
		return proba;
	}

	public SpotCollection getSpotsFromLastProbabilities( final double threshold, final boolean simplify, final double smoothingScale )
	{
		errorMessage = null;
		if ( lastOutput == null )
		{
			errorMessage = "Probabilities have not been computed yet.";
			return null;
		}
		return getSpots( lastOutput, lastCalibration, threshold, simplify, smoothingScale );
	}

	/**
	 * Exposes the last probability image calculated.
	 *
	 * @return the last probability image calculated. May be <code>null</code>
	 */
	public ImgPlus< T > getLastOutput()
	{
		return lastOutput;
	}

	public SpotCollection getSpots( final ImgPlus< T > proba, final double[] calibration, final double threshold, final boolean simplify, final double smoothingScale )
	{
		// What was the time origin of the input?
		// Time is always the last dim in the interval.
		final int t0 = ( int ) lastExtendedInterval.min( lastExtendedInterval.numDimensions() - 1 );

		final SpotCollection spots = new SpotCollection();
		final int timeIndex = proba.dimensionIndex( Axes.TIME );
		for ( int t = 0; t < proba.dimension( timeIndex ); t++ )
		{
			final ImgPlus< T > probaThisFrame = TMUtils.hyperSlice( proba, 0, t );
			final List< Spot > spotsThisFrame = MaskUtils.fromThresholdWithROI(
					probaThisFrame,
					probaThisFrame,
					calibration,
					threshold,
					simplify,
					smoothingScale,
					numThreads,
					probaThisFrame );

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
					final double newPos = pos + lastExtendedInterval.min( d ) * calibration[ d ];
					spot.putFeature( Spot.POSITION_FEATURES[ d ], newPos );
				}
			}

			spots.put( t + t0, spotsThisFrame );
		}
		return spots;
	}

	@Override
	public void setNumThreads()
	{
		// Discover and use Ilastik config.
		final Context context = TMUtils.getContext();
		final OptionsService optionService = context.getService( OptionsService.class );
		final IlastikOptions ilastikOptions = optionService.getOptions( IlastikOptions.class );
		this.numThreads = ilastikOptions.numThreads <= 0
				? Runtime.getRuntime().availableProcessors()
				: ilastikOptions.numThreads;
	}
	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	private static final int getModelNChannel( final String path )
	{
		final IHDF5Reader reader = HDF5Factory.openForReading( new File( path ) );
		final HDF5ObjectInformation info = reader.object().getObjectInformation( HDF_PATH_AXISTAGS );
		if ( !info.exists() )
			return 1; // assume there is only 1 channel

		final String str = reader.readString( HDF_PATH_AXISTAGS );
		@SuppressWarnings( "unchecked" )
		final Map< String, List< Map< String, String > > > map = createJSon().fromJson( str, Map.class );
		final List< Map< String, String > > axesList = map.get( "axes" );
		int channelAxis = -1;
		for ( int i = 0; i < axesList.size(); i++ )
		{
			final Map< String, String > axesAttributes = axesList.get( i );
			final String axisStr = axesAttributes.get( AXIS_KEY_KEY );
			if ( CHANNEL_AXIS_NAME.equals( axisStr ) )
			{
				channelAxis = i;
				break;
			}
		}
		if ( channelAxis < 0 )
			return 1;
		final int[] shape = reader.readIntArray( HDF_PATH_SHAPE );
		return shape[ channelAxis ];
	}

	private static final Gson createJSon()
	{
		return new GsonBuilder()
				.registerTypeAdapter( Map.class, new IlastikAxisMapAdapter() )
				.create();
	}

	private static class IlastikAxisMapAdapter implements JsonDeserializer< Map< String, List< Map< String, String > > > >
	{

		@Override
		public Map< String, List< Map< String, String > > > deserialize( final JsonElement json,
				final java.lang.reflect.Type typeOfT, final JsonDeserializationContext context )
				throws JsonParseException
		{
			final JsonObject obj = json.getAsJsonObject();
			final JsonElement str = obj.get( AXES_KEY );
			final List< Map< String, String > > deserialize = context.deserialize( str, List.class );
			return Collections.singletonMap( AXES_KEY, deserialize );
		}
	}

	/**
	 * Return 1-channel, all time-points, all-Zs if any.
	 *
	 * @return an {@link ImgPlus}.
	 */
	private static < T extends Type< T > > ImgPlus< T > prepareImg( final ImgPlus< T > img, final int channel )
	{
		final int cDim = img.dimensionIndex( Axes.CHANNEL );
		if ( cDim < 0 )
			return img;
		else
			return ImgPlusViews.hyperSlice( img, cDim, channel );
	}

	public static List< String > getClassLabels( final String path )
	{
		final File file = new File( path );
		if ( !file.exists() || !file.canRead() )
			return null; // Model file not found.
		final IHDF5Reader reader = HDF5Factory.openForReading( file );
		final HDF5ObjectInformation info = reader.object().getObjectInformation( HDF_PATH_LABELNAMES );
		if ( !info.exists() )
			return null; // We failed to read.

		final String[] arr = reader.readStringArray( HDF_PATH_LABELNAMES );
		return Arrays.asList( arr );
	}

	private static final String HDF_PATH_LABELNAMES = "/PixelClassification/LabelNames";

	private static final String AXES_KEY = "axes";

	private static final String AXIS_KEY_KEY = "key";

	private static final String CHANNEL_AXIS_NAME = "c";

	private static final String HDF_PATH_AXISTAGS = "/Input Data/infos/lane0000/Raw Data/axistags";

	private static final String HDF_PATH_SHAPE = "/Input Data/infos/lane0000/Raw Data/shape";

}
