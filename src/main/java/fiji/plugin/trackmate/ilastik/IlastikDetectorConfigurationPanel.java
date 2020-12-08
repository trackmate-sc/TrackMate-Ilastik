package fiji.plugin.trackmate.ilastik;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_CLASSIFIER_FILEPATH;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_CLASS_INDEX;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_PROBA_THRESHOLD;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.JLabelLogger;

public class IlastikDetectorConfigurationPanel extends IlastikDetectorBaseConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final NumberFormat THRESHOLD_FORMAT = new DecimalFormat( "#.##" );

	private static final String TITLE = IlastikDetectorFactory.NAME;

	private static final FileFilter fileFilter = new FileNameExtensionFilter( "Ilastik project files.", "ilp" );

	private final JSlider sliderChannel;

	private final JSlider sliderClassId;

	private final JButton btnPreview;

	private final Logger localLogger;

	private final JTextField modelFileTextField;

	private final JButton btnBrowse;

	private final JFormattedTextField ftfProbaThreshold;

	/**
	 * Create the panel.
	 */
	public IlastikDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model );

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 144, 0, 32 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 84, 0, 27, 0, 0, 0, 37, 23 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		setLayout( gridBagLayout );

		final JLabel lblSettingsForDetector = new JLabel( "Settings for detector:" );
		lblSettingsForDetector.setFont( FONT );
		final GridBagConstraints gbc_lblSettingsForDetector = new GridBagConstraints();
		gbc_lblSettingsForDetector.gridwidth = 3;
		gbc_lblSettingsForDetector.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblSettingsForDetector.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblSettingsForDetector.gridx = 0;
		gbc_lblSettingsForDetector.gridy = 0;
		add( lblSettingsForDetector, gbc_lblSettingsForDetector );

		final JLabel lblStardistDetector = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lblStardistDetector.setFont( BIG_FONT );
		lblStardistDetector.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbc_lblStardistDetector = new GridBagConstraints();
		gbc_lblStardistDetector.gridwidth = 3;
		gbc_lblStardistDetector.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblStardistDetector.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblStardistDetector.gridx = 0;
		gbc_lblStardistDetector.gridy = 1;
		add( lblStardistDetector, gbc_lblStardistDetector );

		/*
		 * Help text.
		 */
		final JLabel lblHelptext = new JLabel( IlastikDetectorFactory.INFO_TEXT
				.replace( "<br>", "" )
				.replace( "<p>", "<p align=\"justify\">" )
				.replace( "<html>", "<html><p align=\"justify\">" ) );
		lblHelptext.setFont( FONT.deriveFont( Font.ITALIC ) );
		final GridBagConstraints gbc_lblHelptext = new GridBagConstraints();
		gbc_lblHelptext.anchor = GridBagConstraints.NORTH;
		gbc_lblHelptext.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblHelptext.gridwidth = 3;
		gbc_lblHelptext.insets = new Insets( 5, 10, 5, 10 );
		gbc_lblHelptext.gridx = 0;
		gbc_lblHelptext.gridy = 2;
		add( lblHelptext, gbc_lblHelptext );

		/*
		 * Channel selector.
		 */

		final JLabel lblSegmentInChannel = new JLabel( "Segment in channel:" );
		lblSegmentInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbc_lblSegmentInChannel = new GridBagConstraints();
		gbc_lblSegmentInChannel.anchor = GridBagConstraints.EAST;
		gbc_lblSegmentInChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblSegmentInChannel.gridx = 0;
		gbc_lblSegmentInChannel.gridy = 3;
		add( lblSegmentInChannel, gbc_lblSegmentInChannel );

		sliderChannel = new JSlider();
		final GridBagConstraints gbc_sliderChannel = new GridBagConstraints();
		gbc_sliderChannel.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbc_sliderChannel.gridx = 1;
		gbc_sliderChannel.gridy = 3;
		add( sliderChannel, gbc_sliderChannel );

		final JLabel labelChannel = new JLabel( "1" );
		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbc_labelChannel = new GridBagConstraints();
		gbc_labelChannel.insets = new Insets( 5, 5, 5, 0 );
		gbc_labelChannel.gridx = 2;
		gbc_labelChannel.gridy = 3;
		add( labelChannel, gbc_labelChannel );

		sliderChannel.addChangeListener( l -> labelChannel.setText( "" + sliderChannel.getValue() ) );

		/*
		 * Model file.
		 */

		final JLabel lblCusstomModelFile = new JLabel( "Ilastik file:" );
		lblCusstomModelFile.setFont( FONT );
		final GridBagConstraints gbc_lblCusstomModelFile = new GridBagConstraints();
		gbc_lblCusstomModelFile.anchor = GridBagConstraints.SOUTHWEST;
		gbc_lblCusstomModelFile.insets = new Insets( 0, 5, 5, 5 );
		gbc_lblCusstomModelFile.gridx = 0;
		gbc_lblCusstomModelFile.gridy = 4;
		add( lblCusstomModelFile, gbc_lblCusstomModelFile );

		btnBrowse = new JButton( "Browse" );
		btnBrowse.setFont( FONT );
		final GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
		gbc_btnBrowse.insets = new Insets( 5, 0, 5, 5 );
		gbc_btnBrowse.anchor = GridBagConstraints.SOUTHEAST;
		gbc_btnBrowse.gridwidth = 2;
		gbc_btnBrowse.gridx = 1;
		gbc_btnBrowse.gridy = 4;
		add( btnBrowse, gbc_btnBrowse );

		modelFileTextField = new JTextField( "" );
		modelFileTextField.setFont( SMALL_FONT );
		final GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 3;
		gbc_textField.insets = new Insets( 0, 5, 5, 0 );
		gbc_textField.fill = GridBagConstraints.BOTH;
		gbc_textField.gridx = 0;
		gbc_textField.gridy = 5;
		add( modelFileTextField, gbc_textField );
		modelFileTextField.setColumns( 10 );

		/*
		 * Proba threshold.
		 */

		/*
		 * Class index.
		 */

		final JLabel lblClassId = new JLabel( "Class index:" );
		lblClassId.setFont( SMALL_FONT );
		final GridBagConstraints gbc_lblOverlapThreshold = new GridBagConstraints();
		gbc_lblOverlapThreshold.anchor = GridBagConstraints.EAST;
		gbc_lblOverlapThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblOverlapThreshold.gridx = 0;
		gbc_lblOverlapThreshold.gridy = 6;
		add( lblClassId, gbc_lblOverlapThreshold );

		sliderClassId = new JSlider();
		final GridBagConstraints gbc_sliderClassId = new GridBagConstraints();
		gbc_sliderClassId.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderClassId.insets = new Insets( 0, 0, 5, 5 );
		gbc_sliderClassId.gridx = 1;
		gbc_sliderClassId.gridy = 6;
		add( sliderClassId, gbc_sliderClassId );

		final JLabel labelClassId = new JLabel( "1" );
		labelClassId.setHorizontalAlignment( SwingConstants.CENTER );
		labelClassId.setFont( new Font( "Arial", Font.PLAIN, 10 ) );
		final GridBagConstraints gbc_labelClassId = new GridBagConstraints();
		gbc_labelClassId.insets = new Insets( 0, 0, 5, 0 );
		gbc_labelClassId.gridx = 2;
		gbc_labelClassId.gridy = 6;
		add( labelClassId, gbc_labelClassId );

		sliderClassId.addChangeListener( l -> labelClassId.setText( "" + sliderClassId.getValue() ) );
		sliderClassId.setMaximum( 10 );
		sliderClassId.setMinimum( 1 );
		sliderClassId.setValue( 1 );

		/*
		 * Preview.
		 */

		final JLabel lblScoreTreshold = new JLabel( "Treshold on probability:" );
		lblScoreTreshold.setFont( SMALL_FONT );
		final GridBagConstraints gbc_lblScoreTreshold = new GridBagConstraints();
		gbc_lblScoreTreshold.anchor = GridBagConstraints.EAST;
		gbc_lblScoreTreshold.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblScoreTreshold.gridx = 0;
		gbc_lblScoreTreshold.gridy = 7;
		add( lblScoreTreshold, gbc_lblScoreTreshold );

		ftfProbaThreshold = new JFormattedTextField( THRESHOLD_FORMAT );
		ftfProbaThreshold.setFont( FONT );
		ftfProbaThreshold.setMinimumSize( new Dimension( 60, 26 ) );
		ftfProbaThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbc_score = new GridBagConstraints();
		gbc_score.fill = GridBagConstraints.HORIZONTAL;
		gbc_score.insets = new Insets( 5, 5, 5, 5 );
		gbc_score.gridx = 1;
		gbc_score.gridy = 7;
		add( ftfProbaThreshold, gbc_score );

		btnPreview = new JButton( "Preview", ICON_PREVIEW );
		btnPreview.setFont( FONT );
		final GridBagConstraints gbc_btnPreview = new GridBagConstraints();
		gbc_btnPreview.gridwidth = 2;
		gbc_btnPreview.anchor = GridBagConstraints.SOUTHEAST;
		gbc_btnPreview.insets = new Insets( 5, 5, 5, 5 );
		gbc_btnPreview.gridx = 1;
		gbc_btnPreview.gridy = 8;
		add( btnPreview, gbc_btnPreview );

		/*
		 * Logger.
		 */

		final JLabelLogger labelLogger = new JLabelLogger();
		final GridBagConstraints gbc_labelLogger = new GridBagConstraints();
		gbc_labelLogger.anchor = GridBagConstraints.EAST;
		gbc_labelLogger.gridwidth = 3;
		gbc_labelLogger.gridx = 0;
		gbc_labelLogger.gridy = 9;
		add( labelLogger, gbc_labelLogger );
		localLogger = labelLogger.getLogger();

		btnPreview.addActionListener( l -> preview( btnPreview, localLogger ) );

		/*
		 * Listeners and specificities.
		 */

		/*
		 * Deal with channels: the slider and channel labels are only visible if
		 * we find more than one channel.
		 */
		if ( null != settings.imp )
		{
			final int n_channels = settings.imp.getNChannels();
			sliderChannel.setMaximum( n_channels );
			sliderChannel.setMinimum( 1 );
			sliderChannel.setValue( settings.imp.getChannel() );

			if ( n_channels <= 1 )
			{
				labelChannel.setVisible( false );
				lblSegmentInChannel.setVisible( false );
				sliderChannel.setVisible( false );
			}
			else
			{
				labelChannel.setVisible( true );
				lblSegmentInChannel.setVisible( true );
				sliderChannel.setVisible( true );
			}
		}

		/*
		 * We don't know yet how many classes we have. Let's put 10 for now.
		 */
		btnBrowse.addActionListener( l -> browse() );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > settings = new HashMap<>( 2 );

		final int targetChannel = sliderChannel.getValue();
		settings.put( KEY_TARGET_CHANNEL, targetChannel );

		settings.put( KEY_CLASSIFIER_FILEPATH, modelFileTextField.getText() );

		final int classID = sliderClassId.getValue() - 1;
		settings.put( KEY_CLASS_INDEX, classID );

		final double probaThreshold = ( ( Number ) ftfProbaThreshold.getValue() ).doubleValue();
		settings.put( KEY_PROBA_THRESHOLD, probaThreshold );
		return settings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );
		modelFileTextField.setText( ( String ) settings.get( KEY_CLASSIFIER_FILEPATH ) );
		sliderClassId.setValue( ( Integer ) settings.get( KEY_CLASS_INDEX ) + 1 );
		ftfProbaThreshold.setValue( settings.get( KEY_PROBA_THRESHOLD ) );
	}

	@Override
	public void clean()
	{}

	@Override
	@SuppressWarnings( "rawtypes" )
	protected IlastikDetectorFactory< ? > getDetectorFactory()
	{
		return new IlastikDetectorFactory();
	}

	protected void browse()
	{
		btnBrowse.setEnabled( false );
		try
		{
			final File file = FileChooser.chooseFile( this, modelFileTextField.getText(), fileFilter, "Select an ilastik project file", DialogType.LOAD );
			modelFileTextField.setText( file.getAbsolutePath() );
		}
		finally
		{
			btnBrowse.setEnabled( true );
		}
	}
}
