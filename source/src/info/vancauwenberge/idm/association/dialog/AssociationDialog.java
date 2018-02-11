/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.dialog;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.directory.SearchControls;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.novell.admin.ns.AdminNamespace;
import com.novell.admin.ns.nds.NDSNamespace;
import com.novell.application.console.snapin.ObjectEntry;
import com.novell.core.Core;
import com.novell.core.datatools.access.nds.DSUtil;
import com.novell.core.datatools.edirbrowser.EDirBrowser;
import com.novell.core.datatools.edirbrowser.internal.model.EdirInfo;
import com.novell.idm.IdmModel;
import com.novell.idm.model.Driver;
import com.novell.idm.model.IdentityVault;

import info.vancauwenberge.idm.association.Activator;
import info.vancauwenberge.idm.association.actions.api.IValueFilter;
import info.vancauwenberge.idm.association.actions.valuefilter.DriverDNValueFilter;
import info.vancauwenberge.idm.association.actions.valuefilter.NullValueFilter;

public class AssociationDialog extends TitleAreaDialog {
	private static final String TOKEN_DRIVER_NAME = "{$driverName}";
	private static final String PROP_ASSICIATIONDIALOG_SEARCH_SCOPE = "assiciationdialog.searchScope";
	private static final String PROP_ASSICIATIONDIALOG_SEARCH_BASE = "assiciationdialog.searchBase";
	private static final String PROP_ASSICIATIONDIALOG_TO_STATE = "assiciationdialog.toState";
	private static final String PROP_ASSICIATIONDIALOG_FROM_STATE = "assiciationdialog.fromState";
	private static final String PROP_ASSICIATIONDIALOG_LOG_FILE = "assiciationdialog.logFile";
	private static final String PROP_ASSICIATIONDIALOG_IS_TEST = "assiciationdialog.isTest";
	private static final String PROP_ASSICIATIONDIALOG_FILE = "assiciationdialog.file";
	private static final String PROP_ASSICIATIONDIALOG_FILTER = "assiciationdialog.filter";
	private static final String PROP_ASSICIATIONDIALOG_OPERATION = "assiciationdialog.operation";

	public enum SearchScope{
		OBJECT(SearchControls.OBJECT_SCOPE),
		ONELEVEL(SearchControls.ONELEVEL_SCOPE),
		SUBTREE(SearchControls.SUBTREE_SCOPE);
		private int value;

		SearchScope(final int value){
			this.value = value;
		}

		public int getSearchControl(){
			return value;
		}
	}

	private interface DisplayLabel{
		public String getDisplayLabel();
	}

	public enum Operations implements DisplayLabel{
		EXPORT("Export Associations"),
		IMPORT("Import Associations"),
		MODIFY ("Modify Associations");
		private String label;

		Operations(final String label){
			this.label = label;
		}

		@Override
		public String getDisplayLabel() {
			return label;
		}
	}

	public static final IValueFilter nullValueFilter = new NullValueFilter();

	public enum FromAssociationState implements DisplayLabel{
		MIGRATE("Migrate",4, new DriverDNValueFilter()),
		PROCESSED("Processed",1,new DriverDNValueFilter()),
		DISABLED("Disabled",0,new DriverDNValueFilter()),
		PENDING("Pending",2,new DriverDNValueFilter()),
		MANUAL("Manual",3,new DriverDNValueFilter()),
		NOT_ASSOCIATED("Not associated",-1, nullValueFilter),
		ANY_ASSOCIATION("Any association state",4278190086L,new DriverDNValueFilter()),
		;

		private long state;
		private String label;
		private IValueFilter valueFilter;

		FromAssociationState(final String label, final long state, final IValueFilter valueFilter){
			this.state = state;
			if ((state != -1) && (state != 4278190086L)) {
				this.label = state + " - " + label;
			} else {
				this.label = label;
			}
			this.valueFilter =  valueFilter;
			if (valueFilter instanceof DriverDNValueFilter){
				((DriverDNValueFilter)valueFilter).setAssociationState(this);
			}
		}

		@Override
		public String getDisplayLabel(){
			return label;
		}

		public long getState() {
			return state;
		}

		public IValueFilter getValueFilter() {
			return valueFilter;
		}

	}

	public enum ToAssociationState implements DisplayLabel{
		MIGRATE("Migrate",4),
		PROCESSED("Processed",1),
		DISABLED("Disabled",0),
		PENDING("Pending",2),
		MANUAL("Manual",3),
		REMOVE("Remove association",-1)
		;
		private long state;
		private String label;

		ToAssociationState(final String label, final long state){
			this.state = state;
			if ((state != -1) && (state != 4278190086L)) {
				this.label = state + " - " + label;
			} else {
				this.label = label;
			}
		}

		@Override
		public String getDisplayLabel(){
			return label;
		}

		public long getState() {
			return state;
		}

	}

	private class SelectionValidator implements SelectionListener, KeyListener{

		private void togleAndValidate(){
			AssociationDialog.this.setWidgetVisibility();
			final List<String> errors = AssociationDialog.this.getValidationErrors();
			if (errors.size()==0) {
				AssociationDialog.this.setErrorMessage(null);
			} else{
				for (final String string2 : errors) {
					final String string = string2;
					AssociationDialog.this.setErrorMessage(string);
				}
			}
		}
		@Override
		public void widgetSelected(final SelectionEvent e) {
			togleAndValidate();
			if (e.getSource()==ldapSearchBaseButton){
				final IdentityVault identityVault = IdmModel.getIdentityVaultFromItem(targetDriver);
				final String searchContext = (selectedSearchRoot==null)?"": DSUtil.getDNFromOE(selectedSearchRoot.getParent());
				final EDirBrowser browser = new EDirBrowser(identityVault, null, searchContext, true);
				browser.allowMultipleSelect(false);
				browser.setDescriptionLabel("Select the search root for the association modifier.");
				browser.removeIVConnectMenuOption();
				browser.open();

				final String result = browser.getSelectedObject();
				if ((result != null) && !"".equals(result)){
					ldapSearchBaseText.setText(result);
					//IDM 4.6
					//We cannot compile against LDAP and NDS. Do via reflection for now
					final AdminNamespace ns;
					try{
						final Method method = browser.getClass().getDeclaredMethod("internalGetNamespace");
						ns = (AdminNamespace) method.invoke(browser);
					}catch(final Exception e3){
						Activator.log("Unable to find Admin or NDS namespace.", e3);
						Core.errorDlg("Unable to find Admin or NDS namespace.", e3);
						return;
					}
					//final AdminNamespace ns = browser.internalGetNamespace();
					EdirInfo edirinfo =null;
					//IDM 4.6
					//We cannot compile against LDAP and NDS. Do via reflection for now
					try {
						final Constructor<EdirInfo> cons = EdirInfo.class.getDeclaredConstructor(AdminNamespace.class);
						edirinfo = cons.newInstance(ns);
					} catch (final Exception e1) {
						try{
							final Constructor<EdirInfo> cons = EdirInfo.class.getDeclaredConstructor(NDSNamespace.class);
							edirinfo = cons.newInstance((NDSNamespace)ns);
						}catch(final Exception e2){
							Core.errorDlg("Unable to find LDAP or NDS constrcutor.", e2);
							Activator.log("Unable to find LDAP or NDS constrcutor.", e2);
							Activator.log("Unable to find LDAP or NDS constrcutor.", e1);
							return;
						}
					}
					selectedSearchRoot = edirinfo.getObjEntry(result);
					togleAndValidate();
				}				
			}else if (e.getSource()==fileButton){
				FileDialog fd = null;
				if (fetchSelectedOperation()==Operations.EXPORT){
					fd = new FileDialog(getShell(), SWT.SAVE);
					fd.setText("Export file");
					fd.setOverwrite(true);
				}else{
					fd = new FileDialog(getShell(), SWT.OPEN);
					fd.setText("Import file");
				}
				//fd.setFilterPath("C:/");
				final String[] filterExt = { "*.csv", "*.txt", ".tsv", "*.*" };
				fd.setFilterExtensions(filterExt);
				final String selected = fd.open();

				if (selected != null){
					fileText.setText(selected);
					if (logfileText.getText().equals("")){
						logfileText.setText(selected+".log");
					}
					togleAndValidate();
				}
			}else if (e.getSource()==logfileButton){
				FileDialog fd = null;
				fd = new FileDialog(getShell(), SWT.SAVE);
				fd.setText("Log file");
				fd.setOverwrite(true);
				//fd.setFilterPath("C:/");
				final String[] filterExt = { "*.log","*.csv", "*.txt", ".tsv", "*.*" };
				fd.setFilterExtensions(filterExt);
				final String selected = fd.open();
				if (selected != null){
					logfileText.setText(selected+".log");
					togleAndValidate();
				}
			}else if (e.getSource()==testImportButton){
				togleAndValidate();
				/*			}else if (e.getSource()==searchScopeObject){
				selectedSearchScope=SearchScope.OBJECT;
				Activator.log("Search scope:"+selectedSearchScope);
			}
			else if (e.getSource()==searchScopeOne){
				selectedSearchScope=SearchScope.ONELEVEL;
				Activator.log("Search scope:"+selectedSearchScope);
			}else if (e.getSource()==searchScopeSubtree){
				selectedSearchScope=SearchScope.SUBTREE;
				Activator.log("Search scope:"+selectedSearchScope);*/
			}
		}

		@Override
		public void widgetDefaultSelected(final SelectionEvent e) {
			togleAndValidate();
		}

		@Override
		public void keyPressed(final KeyEvent e) {
			togleAndValidate();
		}

		@Override
		public void keyReleased(final KeyEvent e) {
			togleAndValidate();
		}

	}
	private final SelectionValidator singleValidator = new SelectionValidator();
	private Text ldapFilterText;
	private ObjectEntry selectedSearchRoot=null;

	//private Text lastNameText;
	private String selectedLDAPFilter;
	private final Driver targetDriver;
	private Combo fromStateCombo;
	private Combo toStateCombo;
	private Combo operationTypeCombo;

	private final Map<String,Object> fromStateMap = new HashMap<String, Object>();
	private final Map<String,Object> toStateMap = new HashMap<String, Object>();
	private final Map<String,Object> operationStateMap = new HashMap<String, Object>();
	private Label fromStateLabel;
	private Label toStateLabel;
	private Label testImportLabel;
	private Button testImportButton;
	private Operations selectedOperation;
	private FromAssociationState selectedFromState;
	private ToAssociationState selectedToState;
	private Button ldapSearchBaseButton;
	private Text ldapSearchBaseText;
	private Label fileLabel;
	private Text fileText;
	private Button fileButton;
	private String selectedFile;
	private Label ldapSearchBaseLabel;
	private Label ldapFilterLabel;
	private boolean isTestOnly;
	private Label logfileLabel;
	private Text logfileText;
	private Button logfileButton;
	private String selectedLogFile;
	private Button searchScopeSubtree;
	private Button searchScopeOne;
	private Button searchScopeObject;
	private SearchScope selectedSearchScope;
	private Label ldapScopeLabel;

	public String getSelectedLogFile() {
		return selectedLogFile;
	}

	public AssociationDialog(final Shell parentShell, final Driver targetDriver) {
		super(parentShell);
		this.targetDriver = targetDriver;
		setHelpAvailable(false);
		open();
	}

	private void savePluginSettings() {
		final Properties props = Activator.getProperties();
		props.put(PROP_ASSICIATIONDIALOG_OPERATION, operationTypeCombo.getText());

		props.put(PROP_ASSICIATIONDIALOG_FILTER, ldapFilterText.getText());
		String value = fileText.getText();
		value = value.replace(targetDriver.getName(), TOKEN_DRIVER_NAME);


		props.put(PROP_ASSICIATIONDIALOG_FILE, value);
		props.put(PROP_ASSICIATIONDIALOG_IS_TEST, Boolean.toString(testImportButton.getSelection()));
		value = logfileText.getText();
		value = value.replace(targetDriver.getName(), TOKEN_DRIVER_NAME);

		props.put(PROP_ASSICIATIONDIALOG_LOG_FILE, value);
		props.put(PROP_ASSICIATIONDIALOG_FROM_STATE, fromStateCombo.getText());
		props.put(PROP_ASSICIATIONDIALOG_TO_STATE, toStateCombo.getText());
		props.put(PROP_ASSICIATIONDIALOG_SEARCH_BASE, ldapSearchBaseText.getText());
		//Activator.log("Search scope savePluginSettings:"+_getSearchScope());
		props.put(PROP_ASSICIATIONDIALOG_SEARCH_SCOPE, _getSearchScope().toString());		
		Activator.storeProperties(props);
	}


	private void loadPluginSettings() {
		String value=null;
		final Properties props = Activator.getProperties();
		value = (String) props.get(PROP_ASSICIATIONDIALOG_OPERATION);
		//
		if (value != null) {
			operationTypeCombo.setText(value);
		}

		value = (String) props.get(PROP_ASSICIATIONDIALOG_FILTER);
		if (value != null) {
			ldapFilterText.setText(value);
		}
		value = (String) props.get(PROP_ASSICIATIONDIALOG_FILE);
		if (value != null){
			value = value.replace(TOKEN_DRIVER_NAME, targetDriver.getName());
			fileText.setText(value);
		}
		value = (String) props.get(PROP_ASSICIATIONDIALOG_IS_TEST);
		if (value != null) {
			testImportButton.setSelection(Boolean.parseBoolean(value));
		}
		value = (String) props.get(PROP_ASSICIATIONDIALOG_LOG_FILE);
		if (value != null){
			value = value.replace(TOKEN_DRIVER_NAME, targetDriver.getName());
			logfileText.setText(value);
		}
		value = (String) props.get(PROP_ASSICIATIONDIALOG_FROM_STATE);
		if (value != null) {
			fromStateCombo.setText(value);
		}
		value = (String) props.get(PROP_ASSICIATIONDIALOG_TO_STATE);
		if (value != null) {
			toStateCombo.setText(value);
		}
		value = (String) props.get(PROP_ASSICIATIONDIALOG_SEARCH_BASE);
		if (value != null) {
			ldapSearchBaseText.setText(value);
		}

		value = (String) props.get(PROP_ASSICIATIONDIALOG_SEARCH_SCOPE);
		//Activator.log("Search scope loadPluginSettings:"+value);

		if (value != null) {
			try{
				switch (SearchScope.valueOf(value)) {
				case OBJECT:
					searchScopeObject.setSelection(true);
					searchScopeOne.setSelection(false);
					searchScopeSubtree.setSelection(false);
					break;
				case ONELEVEL:
					searchScopeObject.setSelection(false);
					searchScopeOne.setSelection(true);
					searchScopeSubtree.setSelection(false);
					break;
				case SUBTREE:
					searchScopeObject.setSelection(false);
					searchScopeOne.setSelection(false);
					searchScopeSubtree.setSelection(true);
					break;
				default:
					break;
				}
			}catch (final Exception e) {
				Activator.log("Failed to set search scope.", e);
			}
		}
	}


	@Override
	public void create() {
		super.create();
		// Set the title
		setTitle("Association editor:"+targetDriver.getName());
		// Set the message
		setMessage("Editing associations will be done on the live environement. Know what you are doing!", IMessageProvider.WARNING);

	}

	@Override
	protected Control createDialogArea(final Composite parent) {
		final GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		// layout.horizontalAlignment = GridData.FILL;
		parent.setLayout(layout);

		//Operation type
		final Label operationLabel = new Label(parent, SWT.NONE);
		operationLabel.setText("Operation type");
		// You should not re-use GridData
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		operationTypeCombo = new Combo(parent, SWT.READ_ONLY);

		addSelections(operationTypeCombo, getOperationList(),operationStateMap);
		operationTypeCombo.setLayoutData(gridData);
		operationTypeCombo.addSelectionListener(singleValidator);
		operationTypeCombo.setToolTipText("Select the operation:\nModify: do a live modify of the association states\nExport: Export the associations to a CSV file\nImport: Import associations from a CSV file");


		//Search base selector
		gridData = new GridData();
		ldapSearchBaseLabel = new Label(parent, SWT.NONE);
		ldapSearchBaseLabel.setText("Search base");
		ldapSearchBaseLabel.setToolTipText("Search base for the objects");
		ldapSearchBaseLabel.setLayoutData(gridData);

		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		ldapSearchBaseText = new Text(parent, SWT.BORDER);
		ldapSearchBaseText.setLayoutData(gridData);
		ldapSearchBaseText.setToolTipText("Search base for the objects");
		ldapSearchBaseText.addKeyListener(singleValidator);

		gridData = new GridData();
		ldapSearchBaseButton = new Button(parent, 8);
		ldapSearchBaseButton.setText("Browse...");
		ldapSearchBaseButton.setToolTipText("Browse for the search root");
		ldapSearchBaseButton.addSelectionListener(singleValidator);
		ldapSearchBaseButton.setFocus();
		ldapSearchBaseButton.setLayoutData(gridData);

		//Search scope
		gridData = new GridData();
		gridData.verticalSpan = 3;
		ldapScopeLabel = new Label(parent, SWT.NONE);
		ldapScopeLabel.setText("LDAP search scope");
		ldapScopeLabel.setToolTipText("LDAP search scope.");
		ldapScopeLabel.setLayoutData(gridData);

		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		searchScopeObject = new Button(parent, SWT.RADIO);
		searchScopeObject.setLayoutData(gridData);
		searchScopeObject.setText("Object");
		searchScopeObject.setToolTipText("Only get rhe selected obhect.");
		//searchScopeObject.addSelectionListener(singleValidator);
		//searchScopeObject.addKeyListener(singleValidator);

		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		searchScopeOne = new Button(parent, SWT.RADIO);
		searchScopeOne.setLayoutData(gridData);
		searchScopeOne.setText("One Level");
		searchScopeOne.setToolTipText("One level search.");
		//searchScopeOne.addSelectionListener(singleValidator);
		//searchScopeOne.addKeyListener(singleValidator);

		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		searchScopeSubtree = new Button(parent, SWT.RADIO);
		searchScopeSubtree.setLayoutData(gridData);
		searchScopeSubtree.setText("Subtree");
		searchScopeSubtree.setToolTipText("Subtree search.");
		searchScopeSubtree.setSelection(true);
		//searchScopeSubtree.addSelectionListener(singleValidator);
		//searchScopeSubtree.addKeyListener(singleValidator);

		//LDAP Filter
		gridData = new GridData();
		ldapFilterLabel = new Label(parent, SWT.NONE);
		ldapFilterLabel.setText("LDAP object filter");
		ldapFilterLabel.setToolTipText("LDAP filter that will restrict the operation to a set of objects.");
		ldapFilterLabel.setLayoutData(gridData);

		// The text fields will grow with the size of the dialog
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		ldapFilterText = new Text(parent, SWT.BORDER);
		ldapFilterText.setLayoutData(gridData);
		ldapFilterText.setText("objectclass=*");
		ldapFilterText.setToolTipText("LDAP filter that will restrict the operation to a set of objects.");
		ldapFilterText.addKeyListener(singleValidator);

		//From association status
		// You should not re-use GridData
		gridData = new GridData();
		fromStateLabel = new Label(parent, SWT.NONE);
		fromStateLabel.setText("Modify association status from:");
		fromStateLabel.setLayoutData(gridData);
		// You should not re-use GridData
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		fromStateCombo = new Combo(parent, SWT.READ_ONLY);

		addSelections(fromStateCombo, getFromStateList(), fromStateMap);
		fromStateCombo.setLayoutData(gridData);
		fromStateCombo.addSelectionListener(singleValidator);
		fromStateCombo.setToolTipText("Select the state of te associations to export or modify");


		//To association status
		// You should not re-use GridData
		gridData = new GridData();
		toStateLabel = new Label(parent, SWT.NONE);
		toStateLabel.setText("Modify association status to:");
		toStateLabel.setLayoutData(gridData);

		// You should not re-use GridData
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		toStateCombo = new Combo(parent, SWT.READ_ONLY);
		addSelections(toStateCombo, getToStateList(), toStateMap);
		toStateCombo.setLayoutData(gridData);
		toStateCombo.addSelectionListener(singleValidator);
		toStateCombo.setToolTipText("Select the new state of the associations found");


		//File browser
		// You should not re-use GridData
		gridData = new GridData();
		fileLabel = new Label(parent, SWT.NONE);
		fileLabel.setText("File:");
		fileLabel.setLayoutData(gridData);
		// You should not re-use GridData
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		fileText = new Text(parent, SWT.BORDER);
		fileText.setLayoutData(gridData);
		fileText.addKeyListener(singleValidator);
		fileText.setText(System.getProperty("user.home")+File.separator+"Associations_"+targetDriver.getName()+".csv");

		//Select file button
		gridData = new GridData();
		fileButton = new Button(parent, SWT.PUSH);
		fileButton.setText("Browse...");
		fileButton.setToolTipText("Browse for a file");
		fileButton.addSelectionListener(singleValidator);
		fileButton.setLayoutData(gridData);

		//Test import checkbox
		gridData = new GridData();
		testImportLabel = new Label(parent, SWT.NONE);
		testImportLabel.setText("");
		testImportLabel.setLayoutData(gridData);
		// You should not re-use GridData
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		testImportButton = new Button(parent, SWT.CHECK);
		testImportButton.setLayoutData(gridData);
		testImportButton.setText("Validate only");
		testImportButton.addSelectionListener(singleValidator);

		//Test import file browser
		gridData = new GridData();
		logfileLabel = new Label(parent, SWT.NONE);
		logfileLabel.setText("Logfile:");
		logfileLabel.setLayoutData(gridData);
		// You should not re-use GridData
		gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		logfileText = new Text(parent, SWT.BORDER);
		logfileText.setLayoutData(gridData);
		logfileText.addKeyListener(singleValidator);
		logfileText.setText(System.getProperty("user.home")+File.separator+"DAModify_"+targetDriver.getName()+".log");
		// Test file Button
		gridData = new GridData();
		logfileButton = new Button(parent, SWT.PUSH);
		logfileButton.setText("Browse...");
		logfileButton.setToolTipText("Browse for a file");
		logfileButton.addSelectionListener(singleValidator);
		logfileButton.setLayoutData(gridData);

		//Load previous data
		loadPluginSettings();

		return parent;
	}

	@Override
	public int open(){
		return super.open();
	}

	private DisplayLabel[] getOperationList() {
		final Operations[] assocationStates = Operations.values();
		final DisplayLabel[] languages = new DisplayLabel[assocationStates.length];
		for (int j = 0; j < assocationStates.length; j++) {
			final Operations associationState = assocationStates[j];
			languages[j]= associationState;			
		}
		return languages;
	}

	private void addSelections(final Combo fromState, final DisplayLabel[] fromSelection, final Map<String, Object> map) {
		final String[] values = new String[fromSelection.length];
		for (int i = 0; i < fromSelection.length; i++) {
			final DisplayLabel displayLabel = fromSelection[i];
			final String label = displayLabel.getDisplayLabel();
			values[i] = label;
			map.put(label, displayLabel);
		}
		Arrays.sort(values);
		fromState.setItems(values);
		fromState.select(0);
	}

	private DisplayLabel[] getFromStateList() {
		final FromAssociationState[] assocationStates = FromAssociationState.values();
		final DisplayLabel[] languages = new DisplayLabel[assocationStates.length];
		for (int j = 0; j < assocationStates.length; j++) {
			final FromAssociationState associationState = assocationStates[j];
			languages[j]= associationState;			
		}
		return languages;
	}

	private DisplayLabel[] getToStateList() {
		final ToAssociationState[] assocationStates = ToAssociationState.values();
		final DisplayLabel[] languages = new DisplayLabel[assocationStates.length];
		for (int j = 0; j < assocationStates.length; j++) {
			final ToAssociationState associationState = assocationStates[j];
			languages[j]= associationState;			
		}
		return languages;
	}

	@Override
	protected void createButtonsForButtonBar(final Composite parent) {
		final GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 3;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = SWT.CENTER;

		parent.setLayoutData(gridData);
		// Create Start button
		final Button startButton = createButton(parent, OK, "Start", true);
		// Create Cancel button
		final Button cancelButton = createButton(parent, CANCEL, "Cancel", false);
		setWidgetVisibility();
	}

	private Operations fetchSelectedOperation(){
		return (Operations) operationStateMap.get(operationTypeCombo.getText());
	}

	private FromAssociationState fetchSelectedFromState(){
		return (FromAssociationState) fromStateMap.get(fromStateCombo.getText());
	}

	private ToAssociationState fetchSelectedToState(){
		return (ToAssociationState) toStateMap.get(toStateCombo.getText());
	}

	private void setWidgetVisibility(){
		final List<String> errors = getValidationErrors();
		getButton(OK).setEnabled(errors.size()==0);

		final Operations selectedItem = fetchSelectedOperation();
		switch (selectedItem) {
		case MODIFY:
			//Perform a modify: show the to state
			show(ldapFilterLabel);
			show(ldapFilterText);
			show(ldapSearchBaseLabel);
			show(ldapSearchBaseText);
			show(ldapSearchBaseButton);
			show(toStateLabel);
			show(toStateCombo);
			show(fromStateLabel);
			show(fromStateCombo);
			show(ldapScopeLabel);
			show(searchScopeObject);
			show(searchScopeOne);
			show(searchScopeSubtree);
			fromStateLabel.setText("Modify association status from:");
			//Perform a modify: hide the file selectors
			hide(fileButton);
			hide(fileText);
			hide(fileLabel);
			hide(testImportLabel);
			hide(testImportButton);
			show(logfileLabel);
			show(logfileText);
			show(logfileButton);
			break;
		case EXPORT:
			//Only perform a search
			show(ldapFilterLabel);
			show(ldapFilterText);
			show(ldapSearchBaseLabel);
			show(ldapSearchBaseText);
			show(ldapSearchBaseButton);
			show(ldapScopeLabel);
			show(searchScopeObject);
			show(searchScopeOne);
			show(searchScopeSubtree);

			hide(toStateLabel);
			hide(toStateCombo);
			show(fromStateLabel);
			show(fromStateCombo);
			fromStateLabel.setText("Search association status:");
			//And ask for a file
			show(fileButton);
			show(fileText);
			show(fileLabel);			
			hide(testImportLabel);
			hide(testImportButton);
			hide(logfileLabel);
			hide(logfileText);
			hide(logfileButton);
			break;
		case IMPORT:
			//No LDAP filter required
			hide(ldapFilterLabel);
			hide(ldapFilterText);
			hide(ldapSearchBaseLabel);
			hide(ldapSearchBaseText);
			hide(ldapSearchBaseButton);
			hide(ldapScopeLabel);
			hide(searchScopeObject);
			hide(searchScopeOne);
			hide(searchScopeSubtree);
			//No search required
			hide(toStateLabel);
			hide(toStateCombo);
			hide(fromStateLabel);
			hide(fromStateCombo);
			//And ask for a file
			show(fileButton);
			show(fileText);
			show(fileLabel);
			show(testImportLabel);
			show(testImportButton);
			show(logfileLabel);
			show(logfileText);
			show(logfileButton);
			break;
		default:
			Activator.log("OOPS...."+operationTypeCombo.getText());
			break;
		}
		toStateLabel.getParent().layout(false);
	}

	private void hide(final Control aControl) {
		aControl.setVisible(false);
		((GridData)aControl.getLayoutData()).exclude=true;
	}

	private void show(final Control aControl) {
		aControl.setVisible(true);
		((GridData)aControl.getLayoutData()).exclude=false;
	}

	private List<String> getValidationErrors(){
		final List<String> result = new java.util.LinkedList<String>();
		switch (fetchSelectedOperation()) {
		case EXPORT:
			if (ldapFilterText.getText().length() == 0) {
				result.add("Please enter an ldap filter.");
			}else{
				//Validate the ldap filter
				try{
					final com.novell.ldap.rfc2251.RfcFilter ldapFilter = new com.novell.ldap.rfc2251.RfcFilter(ldapFilterText.getText());
				}catch (final Exception e) {
					result.add("Invalid LDAP filter.");
				}
			}
			if (fromStateCombo.getText().length() == 0){
				result.add("Please specify the current state of the association.");			
			}
			if (ldapSearchBaseText.getText().length() == 0){
				result.add("Please specify the search base.");			
			}
			//Validate the search base
			try {
				selectedSearchRoot = DSUtil.getOEFromDN(IdmModel.getIdentityVaultFromItem(targetDriver).getDSAccess(), 
						ldapSearchBaseText.getText());
			} catch (final Exception e) {
				result.add("Invalid search base:."+ldapSearchBaseText.getText());			
			}

			if (fileText.getText().length()==0) {
				result.add("Please specify a file.");
			}			
			break;
		case IMPORT:
			if (fileText.getText().length()==0) {
				result.add("Please specify a file.");
			}			
			if (testImportButton.getSelection() && (logfileText.getText().length()==0)) {
				result.add("Please specify a log file.");
			}			
			break;
		case MODIFY:
			if (ldapFilterText.getText().length() == 0) {
				result.add("Please enter an ldap filter.");
			}else{
				//Validate the ldap filter
				try{
					final com.novell.ldap.rfc2251.RfcFilter ldapFilter = new com.novell.ldap.rfc2251.RfcFilter(ldapFilterText.getText());
				}catch (final Exception e) {
					result.add("Invalid LDAP filter.");
				}
			}
			if (fromStateCombo.getText().length() == 0){
				result.add("Please specify the current state of the association.");			
			}
			if (ldapSearchBaseText.getText().length() == 0){
				result.add("Please specify the search base.");			
			}
			try {
				selectedSearchRoot = DSUtil.getOEFromDN(IdmModel.getIdentityVaultFromItem(targetDriver).getDSAccess(), 
						ldapSearchBaseText.getText());
			} catch (final Exception e) {
				result.add("Invalid search base:."+ldapSearchBaseText.getText());			
			}
			if (toStateCombo.getText().length() == 0){
				result.add("Please specify the new state of the association.");			
			}
			break;
		}

		return result;		
	}

	@Override
	protected boolean isResizable() {
		return true;
	}


	private SearchScope _getSearchScope(){
		if (searchScopeObject.getSelection()){
			return SearchScope.OBJECT;
		}else if (searchScopeOne.getSelection()){
			return SearchScope.ONELEVEL;
		}else {
			return SearchScope.SUBTREE;
		}
	}


	// We need to have the textFields into Strings because the UI gets disposed
	// and the Text Fields are not accessible any more.
	private void saveInput() {
		selectedLDAPFilter = ldapFilterText.getText();
		selectedOperation = fetchSelectedOperation();
		selectedFromState = fetchSelectedFromState();
		selectedToState = fetchSelectedToState();
		selectedFile = fileText.getText();
		isTestOnly = testImportButton.getSelection();
		selectedLogFile = logfileText.getText();
		selectedSearchScope = _getSearchScope();
		//Activator.log("Search scope saveInput:"+selectedSearchScope);

		try {
			selectedSearchRoot = DSUtil.getOEFromDN(IdmModel.getIdentityVaultFromItem(targetDriver).getDSAccess(), 
					ldapSearchBaseText.getText());
		} catch (final Exception e) {
			Activator.log("Failed to save the input.",e);
		}
	}

	@Override
	protected void okPressed() {
		if (getValidationErrors().size()==0) {
			saveInput();
			savePluginSettings();
			switch (selectedOperation) {
			case MODIFY:
				//Ask additional confirmation when modifying associations
				if (MessageDialog.openConfirm(getShell(), "Confirm Association Modification", "Are you sure you want to modify the associations for this driver?")) {
					super.okPressed();
				}
				break;
			case IMPORT:
				//Ask additional confirmation when modifying associations (unless it's only a test)
				if (isTestOnly) {
					super.okPressed();
				} else if (MessageDialog.openConfirm(getShell(), "Confirm Association Modification", "Current driver associations for the objects imported will be overwritten. Are you sure?")) {
					super.okPressed();
				}
				break;
			default:
				super.okPressed();
				break;
			}
		}
	}

	public String getLDAPFilter() {
		return selectedLDAPFilter;
	}

	public String getSelectedLDAPFilter() {
		return selectedLDAPFilter;
	}

	public boolean isTestOnly() {
		return isTestOnly;
	}

	public Operations getSelectedOperation() {
		return selectedOperation;
	}

	public FromAssociationState getSelectedFromState() {
		return selectedFromState;
	}

	public ToAssociationState getSelectedToState() {
		return selectedToState;
	}

	public ObjectEntry getSelectedSearchRoot() {
		return selectedSearchRoot;
	}

	public String getSelectedFileName() {
		return selectedFile;
	}

	public SearchScope getSelectedSearchScope() {
		return selectedSearchScope;
	}

}

