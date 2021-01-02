package fiji.plugin.trackmate;

import java.io.IOException;

import net.imagej.Dataset;
import net.imagej.ImageJ;

public class TrackMateIlastikTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );
		final String path = "D:\\Projects\\NVerttiQuintero\\Data\\Series014b.tif";
//		"D:/Projects/NVerttiQuintero/Data/Series014b.tif"
		final Dataset dataset = ( Dataset ) ij.io().open( path );
		ij.ui().show( dataset );

		new TrackMatePlugIn().run( null );
	}

}
