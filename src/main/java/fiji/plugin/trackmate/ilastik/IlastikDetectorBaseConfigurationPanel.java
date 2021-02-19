package fiji.plugin.trackmate.ilastik;

import java.net.URL;

import javax.swing.ImageIcon;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.components.ConfigurationPanel;

public abstract class IlastikDetectorBaseConfigurationPanel extends ConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	protected static final ImageIcon ICON = new ImageIcon( getResource( "images/TrackMate-Ilastik-logo-32px.png" ) );

	protected final Settings settings;

	protected final Model model;

	public IlastikDetectorBaseConfigurationPanel( final Settings settings, final Model model )
	{
		this.settings = settings;
		this.model = model;
	}

	protected abstract IlastikDetectorFactory< ? > getDetectorFactory();

	protected static URL getResource( final String name )
	{
		return IlastikDetectorFactory.class.getClassLoader().getResource( name );
	}
}
