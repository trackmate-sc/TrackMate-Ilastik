/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2022 TrackMate developers.
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
package fiji.plugin.trackmate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.ilastik.IlastikDetectorFactory;
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

		final Settings settings = new Settings( imp );
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
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp, DisplaySettingsIO.readUserDefault() );
		view.render();
		view.refresh();
	}
}
