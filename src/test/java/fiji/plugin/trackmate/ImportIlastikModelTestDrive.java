package fiji.plugin.trackmate;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import net.imglib2.util.Util;

public class ImportIlastikModelTestDrive
{

	private static final String AXES_KEY = "axes";

	private static final String AXIS_KEY_KEY = "key";

	private static final String CHANNEL_AXIS_NAME = "c";

	private static final String HDF_PATH_AXISTAGS = "/Input Data/infos/lane0000/Raw Data/axistags";

	private static final String HDF_PATH_SHAPE = "/Input Data/infos/lane0000/Raw Data/shape";

	public static void main( final String[] args )
	{
		final String path = "D:\\Projects\\LLeBlanc\\Data\\MyProject_2ch_2cl.ilp";
		final IHDF5Reader reader = HDF5Factory.openForReading( new File( path ) );

		final HDF5ObjectInformation info = reader.object().getObjectInformation( HDF_PATH_AXISTAGS );
		if ( !info.isDataSet() )
		{
			System.out.println( "Could not find axes model metadata in the project file. Skipping." );
			return;
		}

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
		{
			System.out.println( "Could not find the axis for channel in the model. Skipping." );
			return;
		}
		System.out.println( "Channel axis is at index " + channelAxis );

		final int[] shape = reader.readIntArray( HDF_PATH_SHAPE );
		System.out.println( "Shape: " + Util.printCoordinates( shape ) );
		System.out.println( "Number of channels in the model: " + shape[ channelAxis ] );
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
		public Map< String, List< Map< String, String > > > deserialize(
				final JsonElement json,
				final Type typeOfT,
				final JsonDeserializationContext context ) throws JsonParseException
		{
			final JsonObject obj = json.getAsJsonObject();
			final JsonElement str = obj.get( AXES_KEY );
			final List< Map< String, String > > deserialize = context.deserialize( str, List.class );
			return Collections.singletonMap( AXES_KEY, deserialize );
		}
	}
}
