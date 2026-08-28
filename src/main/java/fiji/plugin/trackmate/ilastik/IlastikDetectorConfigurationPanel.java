/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2025 TrackMate developers.
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

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.ThresholdDetectorFactory.KEY_SMOOTHING_SCALE;
import static fiji.plugin.trackmate.gui.Fonts.BIG_FONT;
import static fiji.plugin.trackmate.gui.Fonts.FONT;
import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;
import static fiji.plugin.trackmate.gui.Icons.MAGNIFIER_ICON;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_CLASSIFIER_FILEPATH;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_CLASS_INDEX;
import static fiji.plugin.trackmate.ilastik.IlastikDetectorFactory.KEY_PROBA_THRESHOLD;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.scijava.prefs.PrefService;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.PanelProbaThreshold;
import fiji.plugin.trackmate.gui.components.PanelSmoothContour;
import fiji.plugin.trackmate.util.FileChooser;
import fiji.plugin.trackmate.util.FileChooser.DialogType;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;

public class IlastikDetectorConfigurationPanel extends IlastikDetectorBaseConfigurationPanel
{

	private static final long serialVersionUID = 1L;

	private static final String TITLE = IlastikDetectorFactory.NAME;

	private static final FileFilter fileFilter = new FileNameExtensionFilter( "ilastik project files.", "ilp" );

	private final JSlider sliderChannel;

	private final JTextField modelFileTextField;

	private final JButton btnBrowse;

	private final PanelProbaThreshold probaThresholdPanel;

	private final PanelSmoothContour smoothingPanel;

	protected final PrefService prefService;

	private final JSpinner spinner;

	private final IlastikDetectionPreviewer< ? > previewer;


	/**
	 * Creates the panel.
	 *
	 * @param settings
	 *            the TrackMate settings to use.
	 * @param model
	 *            the TrackMate model to use.
	 */
	public IlastikDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		super( settings, model );
		this.prefService = TMUtils.getContext().getService( PrefService.class );

		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 144, 0, 32 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 27, 0, 0, 0, 0, 0, 37, 23 };
		gridBagLayout.columnWeights = new double[] { 0., 1., 0. };
		setLayout( gridBagLayout );

		final JLabel lblDetector = new JLabel( TITLE, ICON, JLabel.RIGHT );
		lblDetector.setFont( BIG_FONT );
		lblDetector.setHorizontalAlignment( SwingConstants.CENTER );
		final GridBagConstraints gbcLblDetector = new GridBagConstraints();
		gbcLblDetector.gridwidth = 3;
		gbcLblDetector.insets = new Insets( 5, 5, 5, 5 );
		gbcLblDetector.fill = GridBagConstraints.HORIZONTAL;
		gbcLblDetector.gridx = 0;
		gbcLblDetector.gridy = 0;
		add( lblDetector, gbcLblDetector );

		/*
		 * Help text.
		 */
		final GridBagConstraints gbcLblHelptext = new GridBagConstraints();
		gbcLblHelptext.anchor = GridBagConstraints.NORTH;
		gbcLblHelptext.fill = GridBagConstraints.BOTH;
		gbcLblHelptext.gridwidth = 3;
		gbcLblHelptext.insets = new Insets( 5, 10, 5, 10 );
		gbcLblHelptext.gridx = 0;
		gbcLblHelptext.gridy = 1;
		gbcLblHelptext.weighty = 1.;
		add( GuiUtils.textInScrollPanel( GuiUtils.infoDisplay(
				IlastikDetectorFactory.INFO_TEXT.replace( "</html>", "" )
						+ "<p>Documentation online: <br/> <a href='"
						+ IlastikDetectorFactory.DOC_URL + "'>"
						+ IlastikDetectorFactory.DOC_URL
						+ "</a></html>" ) ),
				gbcLblHelptext );

		/*
		 * Channel selector.
		 */

		final JLabel lblSegmentInChannel = new JLabel( "Segment in channel:" );
		lblSegmentInChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblSegmentInChannel = new GridBagConstraints();
		gbcLblSegmentInChannel.anchor = GridBagConstraints.EAST;
		gbcLblSegmentInChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLblSegmentInChannel.gridx = 0;
		gbcLblSegmentInChannel.gridy = 2;
		add( lblSegmentInChannel, gbcLblSegmentInChannel );

		sliderChannel = new JSlider();
		final GridBagConstraints gbcSliderChannel = new GridBagConstraints();
		gbcSliderChannel.fill = GridBagConstraints.HORIZONTAL;
		gbcSliderChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcSliderChannel.gridx = 1;
		gbcSliderChannel.gridy = 2;
		add( sliderChannel, gbcSliderChannel );

		final JLabel labelChannel = new JLabel( "1" );
		labelChannel.setHorizontalAlignment( SwingConstants.CENTER );
		labelChannel.setFont( SMALL_FONT );
		final GridBagConstraints gbcLabelChannel = new GridBagConstraints();
		gbcLabelChannel.insets = new Insets( 5, 5, 5, 5 );
		gbcLabelChannel.gridx = 2;
		gbcLabelChannel.gridy = 2;
		add( labelChannel, gbcLabelChannel );

		sliderChannel.addChangeListener( l -> labelChannel.setText( "" + sliderChannel.getValue() ) );

		/*
		 * Model file.
		 */

		final JLabel lblCusstomModelFile = new JLabel( "ilastik file:" );
		lblCusstomModelFile.setFont( FONT );
		final GridBagConstraints gbcLblCusstomModelFile = new GridBagConstraints();
		gbcLblCusstomModelFile.anchor = GridBagConstraints.WEST;
		gbcLblCusstomModelFile.insets = new Insets( 5, 5, 5, 5 );
		gbcLblCusstomModelFile.gridx = 0;
		gbcLblCusstomModelFile.gridy = 3;
		add( lblCusstomModelFile, gbcLblCusstomModelFile );

		btnBrowse = new JButton( "Browse" );
		btnBrowse.setFont( FONT );
		final GridBagConstraints gbcBtnBrowse = new GridBagConstraints();
		gbcBtnBrowse.insets = new Insets( 5, 0, 5, 5 );
		gbcBtnBrowse.anchor = GridBagConstraints.SOUTHEAST;
		gbcBtnBrowse.gridwidth = 2;
		gbcBtnBrowse.gridx = 1;
		gbcBtnBrowse.gridy = 3;
		add( btnBrowse, gbcBtnBrowse );

		modelFileTextField = new JTextField( "" );
		modelFileTextField.setFont( SMALL_FONT );
		final GridBagConstraints gbcTextField = new GridBagConstraints();
		gbcTextField.gridwidth = 3;
		gbcTextField.insets = new Insets( 5, 5, 5, 5 );
		gbcTextField.fill = GridBagConstraints.BOTH;
		gbcTextField.gridx = 0;
		gbcTextField.gridy = 4;
		add( modelFileTextField, gbcTextField );
		modelFileTextField.setColumns( 10 );

		/*
		 * Class index.
		 */

		final JLabel lblClassId = new JLabel( "Segment class:" );
		lblClassId.setFont( SMALL_FONT );
		final GridBagConstraints gbcLblOverlapThreshold = new GridBagConstraints();
		gbcLblOverlapThreshold.anchor = GridBagConstraints.EAST;
		gbcLblOverlapThreshold.insets = new Insets( 5, 5, 5, 5 );
		gbcLblOverlapThreshold.gridx = 0;
		gbcLblOverlapThreshold.gridy = 5;
		add( lblClassId, gbcLblOverlapThreshold );

		spinner = new JSpinner( new SpinnerListModel() );
		spinner.setFont( SMALL_FONT );
		final DefaultEditor editor = new JSpinner.DefaultEditor( spinner );
		editor.getTextField().setHorizontalAlignment( JTextField.CENTER );
		spinner.setEditor( editor );
		final GridBagConstraints gbcSpinner = new GridBagConstraints();
		gbcSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbcSpinner.insets = new Insets( 5, 5, 5, 5 );
		gbcSpinner.gridx = 1;
		gbcSpinner.gridy = 5;
		add( spinner, gbcSpinner );

		/*
		 * Proba threshold.
		 */

		probaThresholdPanel = new PanelProbaThreshold( 0.5 );
		final GridBagConstraints gbcScore = new GridBagConstraints();
		gbcScore.fill = GridBagConstraints.HORIZONTAL;
		gbcScore.anchor = GridBagConstraints.NORTHWEST;
		gbcScore.insets = new Insets( 5, 5, 5, 5 );
		gbcScore.gridwidth = 3;
		gbcScore.gridx = 0;
		gbcScore.gridy = 6;
		add( probaThresholdPanel, gbcScore );

		/*
		 * Smooth output.
		 */

		smoothingPanel = new PanelSmoothContour( -1., model.getSpaceUnits() );
		final GridBagConstraints gbSmoothPanel = new GridBagConstraints();
		gbSmoothPanel.anchor = GridBagConstraints.NORTHWEST;
		gbSmoothPanel.insets = new Insets( 5, 5, 5, 5 );
		gbSmoothPanel.gridwidth = 3;
		gbSmoothPanel.gridx = 0;
		gbSmoothPanel.gridy = 7;
		gbSmoothPanel.fill = GridBagConstraints.HORIZONTAL;
		this.add( smoothingPanel, gbSmoothPanel );

		/*
		 * View last proba.
		 */

		final JButton btnLastProba = new JButton( "Last proba map", MAGNIFIER_ICON );
		btnLastProba.addActionListener( e -> showProbaImg() );
		btnLastProba.setFont( FONT );
		final GridBagConstraints gbcBtnLastProba = new GridBagConstraints();
		gbcBtnLastProba.gridwidth = 2;
		gbcBtnLastProba.anchor = GridBagConstraints.SOUTHEAST;
		gbcBtnLastProba.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnLastProba.gridx = 1;
		gbcBtnLastProba.gridy = 8;
		add( btnLastProba, gbcBtnLastProba );

		/*
		 * Preview.
		 */

		previewer = new IlastikDetectionPreviewer<>(
				model,
				settings,
				() -> getSettings(),
				() -> ( settings.imp.getFrame() - 1 ) );

		final GridBagConstraints gbcBtnPreview = new GridBagConstraints();
		gbcBtnPreview.gridwidth = 3;
		gbcBtnPreview.fill = GridBagConstraints.BOTH;
		gbcBtnPreview.insets = new Insets( 5, 5, 5, 5 );
		gbcBtnPreview.gridx = 0;
		gbcBtnPreview.gridy = 10;
		add( previewer.getPanel(), gbcBtnPreview );

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

		btnBrowse.addActionListener( l -> browse() );
		final PropertyChangeListener l = e -> prefService.put(
				IlastikDetectorConfigurationPanel.class, KEY_CLASSIFIER_FILEPATH, modelFileTextField.getText() );
		modelFileTextField.addPropertyChangeListener( "value", l );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > settings = new HashMap<>( 2 );

		final int targetChannel = sliderChannel.getValue();
		settings.put( KEY_TARGET_CHANNEL, targetChannel );

		settings.put( KEY_CLASSIFIER_FILEPATH, modelFileTextField.getText() );

		final SpinnerListModel model = ( SpinnerListModel ) spinner.getModel();
		final int classID = model.getList().indexOf( spinner.getValue() );
		settings.put( KEY_CLASS_INDEX, classID );

		final double probaThreshold = probaThresholdPanel.getThreshold();
		settings.put( KEY_PROBA_THRESHOLD, probaThreshold );

		final double scale = smoothingPanel.getScale();
		settings.put( KEY_SMOOTHING_SCALE, scale );

		return settings;
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		String filePath = ( String ) settings.get( KEY_CLASSIFIER_FILEPATH );
		if ( filePath == null || filePath.isEmpty() )
			filePath = prefService.get( IlastikDetectorConfigurationPanel.class, KEY_CLASSIFIER_FILEPATH );
		modelFileTextField.setText( filePath );

		sliderChannel.setValue( ( Integer ) settings.get( KEY_TARGET_CHANNEL ) );

		refreshLabelNames();
		final SpinnerListModel model = ( SpinnerListModel ) spinner.getModel();
		final int classID = ( Integer ) settings.get( KEY_CLASS_INDEX );
		spinner.setValue( model.getList().get( classID ) );

		final Object thresholdObj = settings.get( KEY_PROBA_THRESHOLD );
		final double threshold = thresholdObj == null ? 0.5 : ( ( Number ) thresholdObj ).doubleValue();
		probaThresholdPanel.setThreshold( threshold );

		final Object scaleObj = settings.get( KEY_SMOOTHING_SCALE );
		final double scale = scaleObj == null ? -1. : ( ( Number ) scaleObj ).doubleValue();
		smoothingPanel.setScale( scale );
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
			if ( file != null )
			{
				modelFileTextField.setText( file.getAbsolutePath() );
				prefService.put( IlastikDetectorConfigurationPanel.class, KEY_CLASSIFIER_FILEPATH, file.getAbsolutePath() );
				refreshLabelNames();
			}
		}
		finally
		{
			btnBrowse.setEnabled( true );
		}
	}

	private void showProbaImg()
	{
		final ImagePlus proba = previewer.getLastProbabilityImage();
		proba.show();
	}

	private void refreshLabelNames()
	{
		// Get new list of labels.
		final String path = modelFileTextField.getText();
		final List< String > classLabels = IlastikRunner.getClassLabels( path );
		if ( classLabels == null || classLabels.isEmpty() )
			return;

		// Store current selection index.
		final SpinnerListModel spinnerModel = ( SpinnerListModel ) spinner.getModel();
		final int currentIndex = spinnerModel.getList().indexOf( spinner.getValue() );

		// Regen model.
		spinnerModel.setList( classLabels );
		if ( currentIndex >= 0 && currentIndex < classLabels.size() )
			spinnerModel.setValue( spinnerModel.getList().get( currentIndex ) );
	}
}
