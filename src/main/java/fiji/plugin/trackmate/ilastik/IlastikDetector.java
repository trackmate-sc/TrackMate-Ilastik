package fiji.plugin.trackmate.ilastik;

import java.io.IOException;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class IlastikDetector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >
{

	private final static String BASE_ERROR_MESSAGE = "IlastikDetector: ";

	protected final ImgPlus< T > img;

	protected final Interval interval;

	protected final double[] calibration;

	protected final String classifierPath;

	protected final int classIndex;

	protected final double probaThreshold;

	protected String baseErrorMessage;

	protected String errorMessage;

	protected long processingTime;

	protected SpotCollection spots;

	public IlastikDetector(
			final ImgPlus< T > img,
			final Interval interval,
			final double[] calibration,
			final String classifierPath,
			final int classIndex,
			final double probaThreshold )
	{
		this.img = img;
		this.interval = DetectionUtils.squeeze( interval );
		this.calibration = calibration;
		this.classifierPath = classifierPath;
		this.classIndex = classIndex;
		this.probaThreshold = probaThreshold;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Run Ilastik.
		 */
		
		try
		{
			spots = IlastikRunner.run(
					img,
					interval,
					calibration,
					classifierPath,
					classIndex,
					probaThreshold );
		}
		catch ( final IOException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Problem accessing the Ilastik executable or the project file:\n" + e.getMessage();
			e.printStackTrace();
			return false;
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
	}

	@Override
	public SpotCollection getResult()
	{
		return spots;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if ( img.dimensionIndex( Axes.Z ) >= 0 )
		{
			errorMessage = baseErrorMessage + "Image must be 2D over time, got and image with multiple Z.";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}
}