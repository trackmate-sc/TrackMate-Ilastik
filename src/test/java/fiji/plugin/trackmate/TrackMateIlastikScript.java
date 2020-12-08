package fiji.plugin.trackmate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.ilastik.IlastikDetectorFactory;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

public class TrackMateIlastikScript
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		final ImageJ ij = new ImageJ();
		ij.launch( args );
		
//		final String imagePath = "D:/Projects/NVerttiQuintero/Data/Series014b.tif";
		final String imagePath = "D:/Projects/NVerttiQuintero/Data/Series063a.tif";
		final String classifierPath = "D:/Projects/NVerttiQuintero/Ilastik/NVertti.ilp";

		final Map< String, Object > detectorSettings = new HashMap<>();
		detectorSettings.put( DetectorKeys.KEY_TARGET_CHANNEL, 1 );
		detectorSettings.put( IlastikDetectorFactory.KEY_CLASSIFIER_FILEPATH, classifierPath );
		detectorSettings.put( IlastikDetectorFactory.KEY_CLASS_INDEX, 0 );
		detectorSettings.put( IlastikDetectorFactory.KEY_PROBA_THRESHOLD, 0.5 );

		final ImagePlus imp = IJ.openImage( imagePath );
		imp.show();

		final Settings settings = new Settings();
		settings.setFrom( imp );
		settings.tstart = 10;
		settings.tend = 50;
		settings.detectorFactory = new IlastikDetectorFactory<>();
		settings.detectorSettings = detectorSettings;

		final Model model = new Model();
		final TrackMate trackmate = new TrackMate( model, settings );
		final boolean ok = trackmate.execDetection();
		if ( !ok )
		{
			System.err.println( trackmate.getErrorMessage() );
			return;
		}

		model.getSpots().setVisible( true );
		System.out.println( "Detection completed successfully." );
		System.out.println( model.getSpots().toString() );
		
		

		/*
		 * Display results.
		 */

		final SelectionModel selectionModel = new SelectionModel( model );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp );
		view.setDisplaySettings( TrackMateModelView.KEY_DISPLAY_SPOT_AS_ROIS, true );
		view.render();
		view.refresh();

	}
}
