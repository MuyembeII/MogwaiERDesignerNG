/**
 * Mogwai ERDesigner. Copyright (C) 2002 The Mogwai Project.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package de.mogwai.erdesignerng.visual;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;

import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphLayoutCacheEvent.GraphLayoutCacheChange;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;

import de.mogwai.erdesignerng.ERDesignerBundle;
import de.mogwai.erdesignerng.io.GenericFileFilter;
import de.mogwai.erdesignerng.io.ModelFileFilter;
import de.mogwai.erdesignerng.io.ModelIOUtilities;
import de.mogwai.erdesignerng.model.Model;
import de.mogwai.erdesignerng.model.Relation;
import de.mogwai.erdesignerng.model.Table;
import de.mogwai.erdesignerng.util.ApplicationPreferences;
import de.mogwai.erdesignerng.visual.cells.ModelCell;
import de.mogwai.erdesignerng.visual.cells.RelationEdge;
import de.mogwai.erdesignerng.visual.cells.TableCell;
import de.mogwai.erdesignerng.visual.cells.views.CellViewFactory;
import de.mogwai.erdesignerng.visual.cells.views.TableCellView;
import de.mogwai.erdesignerng.visual.editor.classpath.ClasspathEditor;
import de.mogwai.erdesignerng.visual.editor.connection.DatabaseConnectionEditor;
import de.mogwai.erdesignerng.visual.editor.defaultvalue.DefaultValueEditor;
import de.mogwai.erdesignerng.visual.editor.domain.DomainEditor;
import de.mogwai.erdesignerng.visual.editor.table.TableEditor;
import de.mogwai.erdesignerng.visual.export.Exporter;
import de.mogwai.erdesignerng.visual.export.ImageExporter;
import de.mogwai.erdesignerng.visual.export.SVGExporter;
import de.mogwai.erdesignerng.visual.plaf.basic.ERDesignerGraphUI;
import de.mogwai.erdesignerng.visual.tools.EntityTool;
import de.mogwai.erdesignerng.visual.tools.HandTool;
import de.mogwai.erdesignerng.visual.tools.RelationTool;
import de.mogwai.erdesignerng.visual.tools.ToolEnum;
import de.mogwai.i18n.ResourceHelper;
import de.mogwai.looks.UIInitializer;
import de.mogwai.looks.components.DefaultComboBox;
import de.mogwai.looks.components.DefaultFrame;
import de.mogwai.looks.components.DefaultScrollPane;
import de.mogwai.looks.components.DefaultToggleButton;
import de.mogwai.looks.components.DefaultToolbar;
import de.mogwai.looks.components.action.ActionEventProcessor;
import de.mogwai.looks.components.action.DefaultAction;
import de.mogwai.looks.components.menu.DefaultMenu;
import de.mogwai.looks.components.menu.DefaultMenuItem;

/**
 * 
 * @author $Author: mirkosertic $
 * @version $Date: 2007-08-05 18:15:04 $
 */
public class ERDesignerMainFrame extends DefaultFrame {

	private class ZoomInfo {

		private String description;

		private double value;

		public ZoomInfo(String aDescription, double aValue) {
			description = aDescription;
			value = aValue;
		}

		public double getValue() {
			return value;
		}

		@Override
		public String toString() {
			return description;
		}
	}

	private final ZoomInfo ZOOMSCALE_HUNDREDPERCENT = new ZoomInfo("100%", 1);

	private GraphModel graphModel;

	private GraphLayoutCache layoutCache;

	private ERDesignerGraph graph;

	private Model model;

	private DefaultScrollPane scrollPane = new DefaultScrollPane();

	private DefaultComboBox zoomBox = new DefaultComboBox();

	private ApplicationPreferences preferences;

	private DefaultAction fileAction = new DefaultAction(this,
			ERDesignerBundle.FILE);

	private DefaultAction lruAction = new DefaultAction(this,
			ERDesignerBundle.RECENTLYUSEDFILES);

	private DefaultMenu lruMenu = new DefaultMenu(lruAction);

	private DefaultAction newAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandNew();
				}
			}, this, ERDesignerBundle.NEWMODEL);

	private DefaultAction saveAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent aEvent) {
					commandSaveFile();
				}

			}, this, ERDesignerBundle.SAVEMODEL);

	private DefaultAction loadAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent aEvent) {
					commandOpenFile();
				}

			}, this, ERDesignerBundle.LOADMODEL);

	private DefaultAction exitAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandExit();
				}

			}, this, ERDesignerBundle.EXITPROGRAM);

	private DefaultAction exportAction = new DefaultAction(this,
			ERDesignerBundle.EXPORT);

	private DefaultAction exportSVGAction = new DefaultAction(this,
			ERDesignerBundle.ASSVG);

	private DefaultAction databaseAction = new DefaultAction(this,
			ERDesignerBundle.DATABASE);

	private DefaultAction classpathAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandClasspath();
				}

			}, this, ERDesignerBundle.CLASSPATH);

	private DefaultAction dbConnectionAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandDBConnection();
				}

			}, this, ERDesignerBundle.DBCONNECTION);

	private DefaultAction reverseEngineerAction = new DefaultAction(this,
			ERDesignerBundle.REVERSEENGINEER);

	private DefaultAction domainsAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent aEvent) {
					commandShowDomainEditor();
				}

			}, this, ERDesignerBundle.DOMAINS);

	private DefaultAction defaultValuesAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent aEvent) {
					commandShowDefaultValuesEditor();
				}

			}, this, ERDesignerBundle.DEFAULTVALUES);

	private DefaultAction viewAction = new DefaultAction(this,
			ERDesignerBundle.VIEW);

	private DefaultAction zoomAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent aEvent) {
					commandSetZoom((ZoomInfo) ((JComboBox) aEvent.getSource())
							.getSelectedItem());
				}
			}, this, ERDesignerBundle.ZOOM);

	private DefaultAction zoomInAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandZoomIn();
				}

			}, this, ERDesignerBundle.ZOOMIN);

	private DefaultAction zoomOutAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandZoomOut();
				}

			}, this, ERDesignerBundle.ZOOMOUT);

	private DefaultAction handAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandSetTool(ToolEnum.HAND);
				}

			}, this, ERDesignerBundle.HAND);

	private DefaultAction entityAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandSetTool(ToolEnum.ENTITY);
				}

			}, this, ERDesignerBundle.ENTITY);

	private DefaultAction relationAction = new DefaultAction(
			new ActionEventProcessor() {

				public void processActionEvent(ActionEvent e) {
					commandSetTool(ToolEnum.RELATION);
				}

			}, this, ERDesignerBundle.RELATION);

	private JToggleButton handButton;

	private JToggleButton relationButton;

	private JToggleButton entityButton;

	private File currentEditingFile;

	public ERDesignerMainFrame() {
		super(ERDesignerBundle.TITLE);
		initialize();
	}

	@Override
	public ResourceHelper getResourceHelper() {
		return ResourceHelper.getResourceHelper(ERDesignerBundle.BUNDLE_NAME);
	}

	protected void addExportEntries(DefaultMenu aMenu, final Exporter aExporter) {
		JMenuItem theAllInOneItem = aMenu.add("All in one");
		theAllInOneItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				commandExport(aExporter, ExportType.ALL_IN_ONE);
			}
		});

		JMenuItem theOnePerTable = aMenu.add("One file per table");
		theOnePerTable.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				commandExport(aExporter, ExportType.ONE_PER_FILE);
			}
		});
	}

	private void initialize() {

		DefaultMenu theFileMenu = new DefaultMenu(fileAction);
		JMenuItem theItem = theFileMenu.add(newAction);
		theFileMenu.addSeparator();
		theFileMenu.add(saveAction);
		theFileMenu.add(loadAction);
		theFileMenu.addSeparator();

		DefaultMenu theExportMenu = new DefaultMenu(exportAction);

		List<String> theSupportedFormats = ImageExporter.getSupportedFormats();
		if (theSupportedFormats.contains("IMAGE/PNG")) {
			DefaultMenu theSingleExportMenu = new DefaultMenu(this,
					ERDesignerBundle.ASPNG);
			theExportMenu.add(theSingleExportMenu);

			addExportEntries(theSingleExportMenu, new ImageExporter("png"));
		}
		if (theSupportedFormats.contains("IMAGE/JPEG")) {
			DefaultMenu theSingleExportMenu = new DefaultMenu(this,
					ERDesignerBundle.ASJPEG);
			theExportMenu.add(theSingleExportMenu);

			addExportEntries(theSingleExportMenu, new ImageExporter("jpg"));
		}
		if (theSupportedFormats.contains("IMAGE/BMP")) {
			DefaultMenu theSingleExportMenu = new DefaultMenu(this,
					ERDesignerBundle.ASBMP);
			theExportMenu.add(theSingleExportMenu);

			addExportEntries(theSingleExportMenu, new ImageExporter("bmp"));
		}

		DefaultMenu theSVGExportMenu = new DefaultMenu(exportSVGAction);

		theExportMenu.add(theSVGExportMenu);
		addExportEntries(theSVGExportMenu, new SVGExporter());

		theFileMenu.add(theExportMenu).getClass();

		theFileMenu.addSeparator();
		theFileMenu.add(lruMenu);
		theFileMenu.addSeparator();
		theFileMenu.add(new DefaultMenuItem(exitAction));

		DefaultMenu theDBMenu = new DefaultMenu(databaseAction);
		theDBMenu.add(new DefaultMenuItem(classpathAction));
		theDBMenu.add(new DefaultMenuItem(dbConnectionAction));
		theDBMenu.addSeparator();
		theDBMenu.add(new DefaultMenuItem(reverseEngineerAction));
		theDBMenu.addSeparator();
		theDBMenu.add(new DefaultMenuItem(domainsAction));
		theDBMenu.add(new DefaultMenuItem(defaultValuesAction));

		menuBar.add(theFileMenu);
		menuBar.add(theDBMenu);

		DefaultComboBoxModel theZoomModel = new DefaultComboBoxModel();
		theZoomModel.addElement(ZOOMSCALE_HUNDREDPERCENT);
		for (int i = 9; i > 0; i--) {
			theZoomModel.addElement(new ZoomInfo(i * 10 + " %", ((double) i)
					/ (double) 10));
		}
		zoomBox.setPreferredSize(new Dimension(100, 21));
		zoomBox.setMaximumSize(new Dimension(100, 21));
		zoomBox.setAction(zoomAction);
		zoomBox.setModel(theZoomModel);

		DefaultToolbar theToolBar = getDefaultFrameContent().getToolbar();

		theToolBar.add(newAction);
		theToolBar.addSeparator();
		theToolBar.add(loadAction);
		theToolBar.add(saveAction);
		theToolBar.addSeparator();
		theToolBar.add(zoomBox);
		theToolBar.addSeparator();
		theToolBar.add(zoomInAction);
		theToolBar.add(zoomOutAction);
		theToolBar.addSeparator();

		handButton = new DefaultToggleButton(handAction);
		relationButton = new DefaultToggleButton(relationAction);
		entityButton = new DefaultToggleButton(entityAction);

		ButtonGroup theGroup = new ButtonGroup();
		theGroup.add(handButton);
		theGroup.add(relationButton);
		theGroup.add(entityButton);

		theToolBar.add(handButton);
		theToolBar.add(entityButton);
		theToolBar.add(relationButton);

		frameContent.add(scrollPane, BorderLayout.CENTER);

		setSize(800, 600);

		initTitle();

		try {
			preferences = new ApplicationPreferences(this, 10);
		} catch (BackingStoreException e) {
			logException(e);
		}

		initLRUMenu();

		UIInitializer.getInstance().initialize(this);
	}

	private void initLRUMenu() {

		lruMenu.removeAll();
		if (preferences != null) {

			List<File> theFiles = preferences.getLRUfiles();
			for (final File theFile : theFiles) {
				JMenuItem theItem = new JMenuItem(theFile.toString());
				theItem.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						commandOpenFile(theFile);
					}

				});

				UIInitializer.getInstance().initializeFontAndColors(theItem);

				lruMenu.add(theItem);
			}
		}
	}

	private void initTitle() {

		StringBuffer theTitle = new StringBuffer();
		if (currentEditingFile != null) {
			theTitle.append(" - ").append(currentEditingFile.toString());
		}

		setTitle(getResourceHelper().getText(getResourceBundleID()) + theTitle);
	}

	public void setModel(Model aModel) {
		model = aModel;

		graphModel = new DefaultGraphModel();
		layoutCache = new GraphLayoutCache(graphModel, new CellViewFactory());

		graphModel.addGraphModelListener(graphModelListener);

		graph = new ERDesignerGraph(graphModel, layoutCache) {

			@Override
			public void commandNewTable(Point2D aLocation) {
				ERDesignerMainFrame.this.commandAddTable(aLocation);
			}

		};
		graph.setUI(new ERDesignerGraphUI());

		scrollPane.getViewport().removeAll();
		scrollPane.getViewport().add(graph);

		Map<Table, TableCell> theCells = new HashMap<Table, TableCell>();

		for (Table theTable : model.getTables()) {
			TableCell theCell = new TableCell(theTable);
			theCell.transferPropertiesToAttributes(theTable);

			layoutCache.insert(theCell);

			theCells.put(theTable, theCell);
		}

		for (Relation theRelation : model.getRelations()) {

			TableCell theImportingCell = theCells.get(theRelation
					.getImportingTable());
			TableCell theExportingCell = theCells.get(theRelation
					.getExportingTable());

			RelationEdge theCell = new RelationEdge(theRelation,
					theImportingCell, theExportingCell);
			theCell.transferPropertiesToAttributes(theRelation);

			layoutCache.insert(theCell);
		}

		commandSetZoom(ZOOMSCALE_HUNDREDPERCENT);
		commandSetTool(ToolEnum.HAND);
	}

	protected void commandShowDomainEditor() {
		DomainEditor theEditor = new DomainEditor(model, this);
		if (theEditor.showModal() == DomainEditor.MODAL_RESULT_OK) {
			try {
				theEditor.applyValues();
			} catch (Exception e) {
				logException(e);
			}
		}
	}

	protected void commandShowDefaultValuesEditor() {
		DefaultValueEditor theEditor = new DefaultValueEditor(model, this);
		if (theEditor.showModal() == DomainEditor.MODAL_RESULT_OK) {
			try {
				theEditor.applyValues();
			} catch (Exception e) {
				logException(e);
			}
		}
	}

	protected void commandOpenFile(File aFile) {

		try {
			Model theModel = ModelIOUtilities.getInstance()
					.deserializeModelFromXML(new FileInputStream(aFile));
			setModel(theModel);

			currentEditingFile = aFile;
			initTitle();

			preferences.addLRUFile(aFile);

			initLRUMenu();

		} catch (Exception e) {
			logException(e);
		}
	}

	protected void commandOpenFile() {

		ModelFileFilter theFiler = new ModelFileFilter();

		JFileChooser theChooser = new JFileChooser();
		theChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		theChooser.setFileFilter(theFiler);
		if (theChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {

			File theFile = theFiler.getCompletedFile(theChooser
					.getSelectedFile());

			commandOpenFile(theFile);
		}
	}

	protected void commandSaveFile() {

		ModelFileFilter theFiler = new ModelFileFilter();

		JFileChooser theChooser = new JFileChooser();
		theChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		theChooser.setFileFilter(theFiler);
		theChooser.setSelectedFile(currentEditingFile);
		if (theChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

			File theFile = theFiler.getCompletedFile(theChooser
					.getSelectedFile());
			try {

				ModelIOUtilities.getInstance().serializeModelToXML(model,
						new FileOutputStream(theFile));

				currentEditingFile = theFile;
				initTitle();

				preferences.addLRUFile(theFile);

				initLRUMenu();

			} catch (Exception e) {
				logException(e);
			}

		}
	}

	protected void commandNew() {
		currentEditingFile = null;

		Model theModel = new Model();
		setModel(theModel);

		initTitle();
	}

	protected void commandSetZoom(ZoomInfo aZoomInfo) {
		graph.setScale(aZoomInfo.getValue());
		zoomBox.setSelectedItem(aZoomInfo);
	}

	/**
	 * Log an exception.
	 * 
	 * @param aException
	 */
	protected void logException(Exception aException) {
		aException.printStackTrace();
	}

	protected void commandAddTable(Point2D aPoint) {
		Table theTable = new Table();
		TableEditor theEditor = new TableEditor(model, this);
		theEditor.initializeFor(theTable);
		if (theEditor.showModal() == TableEditor.MODAL_RESULT_OK) {
			try {
				theEditor.applyValues();

				TableCell theCell = new TableCell(theTable);
				theCell.transferPropertiesToAttributes(theTable);

				GraphConstants.setBounds(theCell.getAttributes(),
						new Rectangle2D.Double(aPoint.getX(), aPoint.getY(),
								-1, -1));

				layoutCache.insert(theCell);

				theCell.transferAttributesToProperties(theCell.getAttributes());

			} catch (Exception e) {
				logException(e);
			}
		}
	}

	protected void commandDelete(Object aCell) {

	}

	protected void commandSetTool(ToolEnum aTool) {
		if (aTool.equals(ToolEnum.HAND)) {

			if (!handButton.isSelected()) {
				handButton.setSelected(true);
			}

			graph.setTool(new HandTool(graph));
		}
		if (aTool.equals(ToolEnum.ENTITY)) {

			if (!entityButton.isSelected()) {
				entityButton.setSelected(true);
			}

			graph.setTool(new EntityTool(graph));
		}
		if (aTool.equals(ToolEnum.RELATION)) {

			if (!relationButton.isSelected()) {
				relationButton.setSelected(true);
			}

			graph.setTool(new RelationTool(graph));
		}
	}

	protected void commandZoomOut() {
		int theIndex = zoomBox.getSelectedIndex();
		if (theIndex < zoomBox.getItemCount() - 1) {
			theIndex++;
			zoomBox.setSelectedIndex(theIndex);
			commandSetZoom((ZoomInfo) zoomBox.getSelectedItem());
		}
	}

	protected void commandZoomIn() {
		int theIndex = zoomBox.getSelectedIndex();
		if (theIndex > 0) {
			theIndex--;
			zoomBox.setSelectedIndex(theIndex);
			commandSetZoom((ZoomInfo) zoomBox.getSelectedItem());
		}
	}

	protected void commandExit() {
		try {
			preferences.store();
		} catch (BackingStoreException e) {
			logException(e);
		}

		System.exit(0);
	}

	protected void commandDBConnection() {
		DatabaseConnectionEditor theEditor = new DatabaseConnectionEditor(this,
				model, preferences);
		if (theEditor.showModal() == DatabaseConnectionEditor.MODAL_RESULT_OK) {
			try {
				theEditor.applyValues();
			} catch (Exception e) {
				logException(e);
			}
		}
	}

	protected void commandClasspath() {
		ClasspathEditor theEditor = new ClasspathEditor(this, preferences);
		if (theEditor.showModal() == ClasspathEditor.MODAL_RESULT_OK) {
			try {
				theEditor.applyValues();
			} catch (Exception e) {
				logException(e);
			}
		}

	}

	protected void commandExport(Exporter aExporter, ExportType aExportType) {

		if (aExportType.equals(ExportType.ONE_PER_FILE)) {

			JFileChooser theChooser = new JFileChooser();
			theChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (theChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				File theBaseDirectory = theChooser.getSelectedFile();

				CellView[] theViews = layoutCache.getAllViews();
				for (CellView theView : theViews) {
					if (theView instanceof TableCellView) {
						TableCellView theTableCellView = (TableCellView) theView;
						TableCell theTableCell = (TableCell) theTableCellView
								.getCell();
						Table theTable = (Table) theTableCell.getUserObject();

						File theOutputFile = new File(theBaseDirectory,
								theTable.getName()
										+ aExporter.getFileExtension());
						try {
							aExporter.exportToStream(theTableCellView
									.getRendererComponent(graph, false, false,
											false), new FileOutputStream(
									theOutputFile));
						} catch (Exception e) {
							logException(e);
						}
					}
				}
			}

		} else {

			JFileChooser theChooser = new JFileChooser();
			GenericFileFilter theFilter = new GenericFileFilter(aExporter
					.getFileExtension(), aExporter.getFileExtension() + " File");
			theChooser.setFileFilter(theFilter);
			if (theChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {

				File theFile = theFilter.getCompletedFile(theChooser
						.getSelectedFile());
				try {
					aExporter.fullExportToStream(graph, new FileOutputStream(
							theFile));
				} catch (Exception e) {
					logException(e);
				}
			}

		}
	}

	private static GraphModelListener graphModelListener = new GraphModelListener() {

		public void graphChanged(GraphModelEvent aEvent) {
			GraphLayoutCacheChange theChange = aEvent.getChange();

			Object[] theChangedObjects = theChange.getChanged();
			Map theChangedAttributes = theChange.getPreviousAttributes();
			for (Object theChangedObject : theChangedObjects) {
				Map theAttributes = (Map) theChangedAttributes
						.get(theChangedObject);

				if (theChangedObject instanceof ModelCell) {

					ModelCell theCell = (ModelCell) theChangedObject;
					theCell.transferAttributesToProperties(theAttributes);
				}
			}

		}
	};
}
