/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.dialog;

import info.vancauwenberge.idm.association.Activator;
import info.vancauwenberge.idm.association.actions.api.IValueFilter;
import info.vancauwenberge.idm.association.actions.valuefilter.DriverDNValueFilter;
import info.vancauwenberge.idm.association.actions.valuefilter.NullValueFilter;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

import com.novell.application.console.snapin.ObjectEntry;
import com.novell.core.datatools.access.nds.DSUtil;
import com.novell.core.datatools.edirbrowser.EDirBrowser;
import com.novell.core.datatools.edirbrowser.internal.model.EdirInfo;
import com.novell.idm.IdmModel;
import com.novell.idm.model.Driver;
import com.novell.idm.model.IdentityVault;

public class AssociationDialog extends TitleAreaDialog {
	private static final String TOKEN_DRIVER_NAME = "{$driverName}";
	private static final String PROP_ASSICIATIONDIALOG_SEARCH_BASE = "assiciationdialog.searchBase";
	private static final String PROP_ASSICIATIONDIALOG_TO_STATE = "assiciationdialog.toState";
	private static final String PROP_ASSICIATIONDIALOG_FROM_STATE = "assiciationdialog.fromState";
	private static final String PROP_ASSICIATIONDIALOG_LOG_FILE = "assiciationdialog.logFile";
	private static final String PROP_ASSICIATIONDIALOG_IS_TEST = "assiciationdialog.isTest";
	private static final String PROP_ASSICIATIONDIALOG_FILE = "assiciationdialog.file";
	private static final String PROP_ASSICIATIONDIALOG_FILTER = "assiciationdialog.filter";
	private static final String PROP_ASSICIATIONDIALOG_OPERATION = "assiciationdialog.operation";

	private interface DisplayLabel{
		public String getDisplayLabel();
	}
	
	public enum Operations implements DisplayLabel{
		EXPORT("Export Associations"),
		IMPORT("Import Associations"),
		MODIFY ("Modify Associations");
		private String label;

		Operations(String label){
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

		FromAssociationState(String label, long state, IValueFilter valueFilter){
			this.state = state;
			if (state != -1 && (state != 4278190086L))
				this.label = state + " - " + label;
			else
				this.label = label;
			this.valueFilter =  valueFilter;
			if (valueFilter instanceof DriverDNValueFilter){
				((DriverDNValueFilter)valueFilter).setAssociationState(this);
			}
		}
		
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

		ToAssociationState(String label, long state){
			this.state = state;
			if (state != -1 && (state != 4278190086L))
				this.label = state + " - " + label;
			else
				this.label = label;
		}
		
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
			List<String> errors = AssociationDialog.this.getValidationErrors();
			if (errors.size()==0)
				AssociationDialog.this.setErrorMessage(null);
			else{
				for (Iterator<String> iterator = errors.iterator(); iterator.hasNext();) {
					String string = (String) iterator.next();
					AssociationDialog.this.setErrorMessage(string);
				}
			}
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			togleAndValidate();
			if (e.getSource()==ldapSearchBaseButton){
				IdentityVault identityVault = IdmModel.getIdentityVaultFromItem(targetDriver);
				String searchContext = (selectedSearchRoot==null)?"": DSUtil.getDNFromOE(selectedSearchRoot.getParent());
				EDirBrowser browser = new EDirBrowser(identityVault, null, searchContext, true);
				browser.allowMultipleSelect(false);
				browser.setDescriptionLabel("Select the search root for the association modifier.");
				browser.removeIVConnectMenuOption();
				browser.open();
				
				String result = browser.getSelectedObject();
				if (result != null && !"".equals(result)){
					ldapSearchBaseText.setText(result);
					EdirInfo edirinfo = new EdirInfo(browser.internalGetNamespace());
					selectedSearchRoot = edirinfo.getObjEntry(result);
					togleAndValidate();
				}				
			}else
			if (e.getSource()==fileButton){
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
		        String[] filterExt = { "*.csv", "*.txt", ".tsv", "*.*" };
		        fd.setFilterExtensions(filterExt);
		        String selected = fd.open();

		        if (selected != null){
		        	fileText.setText(selected);
					if (logfileText.getText().equals("")){
						logfileText.setText(selected+".log");
					}
					togleAndValidate();
		        }
		      }else
		    	  if (e.getSource()==logfileButton){
		    		  FileDialog fd = null;
	    			  fd = new FileDialog(getShell(), SWT.SAVE);
	    			  fd.setText("Log file");
	    			  fd.setOverwrite(true);
		    		  //fd.setFilterPath("C:/");
		    		  String[] filterExt = { "*.log","*.csv", "*.txt", ".tsv", "*.*" };
		    		  fd.setFilterExtensions(filterExt);
		    		  String selected = fd.open();
		    		  if (selected != null){
	    				  logfileText.setText(selected+".log");
		    			  togleAndValidate();
		    		  }
			      }else
			    	  if (e.getSource()==testImportButton){
		    			  togleAndValidate();
			    	  }
			}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			togleAndValidate();
		}

		@Override
		public void keyPressed(KeyEvent e) {
			togleAndValidate();
		}

		@Override
		public void keyReleased(KeyEvent e) {
			togleAndValidate();
		}
		
	}
	private SelectionValidator singleValidator = new SelectionValidator();
	private Text ldapFilterText;
	private ObjectEntry selectedSearchRoot=null;

	//private Text lastNameText;
	private String selectedLDAPFilter;
	private Driver targetDriver;
	private Combo fromStateCombo;
	private Combo toStateCombo;
	private Combo operationTypeCombo;
	
	private Map<String,Object> fromStateMap = new HashMap<String, Object>();
	private Map<String,Object> toStateMap = new HashMap<String, Object>();
	private Map<String,Object> operationStateMap = new HashMap<String, Object>();
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

	public String getSelectedLogFile() {
		return selectedLogFile;
	}

	public AssociationDialog(Shell parentShell, Driver targetDriver) {
		super(parentShell);
		this.targetDriver = targetDriver;
		setHelpAvailable(false);
		open();
	}
	
	private void savePluginSettings() {
		Properties props = Activator.getProperties();
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
		Activator.storeProperties(props);
	  }


	private void loadPluginSettings() {
		String value=null;
		Properties props = Activator.getProperties();
		value = (String) props.get(PROP_ASSICIATIONDIALOG_OPERATION);
		//
		if (value != null)
			operationTypeCombo.setText(value);

		value = (String) props.get(PROP_ASSICIATIONDIALOG_FILTER);
		if (value != null)
			ldapFilterText.setText(value);
		value = (String) props.get(PROP_ASSICIATIONDIALOG_FILE);
		if (value != null){
			value = value.replace(TOKEN_DRIVER_NAME, targetDriver.getName());
			fileText.setText(value);
		}
		value = (String) props.get(PROP_ASSICIATIONDIALOG_IS_TEST);
		if (value != null)
			testImportButton.setSelection(Boolean.parseBoolean(value));
		value = (String) props.get(PROP_ASSICIATIONDIALOG_LOG_FILE);
		if (value != null){
			value = value.replace(TOKEN_DRIVER_NAME, targetDriver.getName());
			logfileText.setText(value);
		}
		value = (String) props.get(PROP_ASSICIATIONDIALOG_FROM_STATE);
		if (value != null)
			fromStateCombo.setText(value);
		value = (String) props.get(PROP_ASSICIATIONDIALOG_TO_STATE);
		if (value != null)
			toStateCombo.setText(value);
		value = (String) props.get(PROP_ASSICIATIONDIALOG_SEARCH_BASE);
		if (value != null)
			ldapSearchBaseText.setText(value);		
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
	protected Control createDialogArea(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		// layout.horizontalAlignment = GridData.FILL;
		parent.setLayout(layout);
		
		//Operation type
		Label operationLabel = new Label(parent, SWT.NONE);
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
	
	public int open(){
		return super.open();
	}

	private DisplayLabel[] getOperationList() {
		Operations[] assocationStates = Operations.values();
		DisplayLabel[] languages = new DisplayLabel[assocationStates.length];
		for (int j = 0; j < assocationStates.length; j++) {
			Operations associationState = assocationStates[j];
			languages[j]= associationState;			
		}
	    return languages;
	}

	private void addSelections(Combo fromState, DisplayLabel[] fromSelection, Map<String, Object> map) {
		String[] values = new String[fromSelection.length];
		for (int i = 0; i < fromSelection.length; i++) {
			DisplayLabel displayLabel = fromSelection[i];
			String label = displayLabel.getDisplayLabel();
			values[i] = label;
			map.put(label, displayLabel);
		}
		Arrays.sort(values);
		fromState.setItems(values);
		fromState.select(0);
	}

	private DisplayLabel[] getFromStateList() {
		FromAssociationState[] assocationStates = FromAssociationState.values();
		DisplayLabel[] languages = new DisplayLabel[assocationStates.length];
		for (int j = 0; j < assocationStates.length; j++) {
			FromAssociationState associationState = assocationStates[j];
			languages[j]= associationState;			
		}
	    return languages;
	}

	private DisplayLabel[] getToStateList() {
		ToAssociationState[] assocationStates = ToAssociationState.values();
		DisplayLabel[] languages = new DisplayLabel[assocationStates.length];
		for (int j = 0; j < assocationStates.length; j++) {
			ToAssociationState associationState = assocationStates[j];
			languages[j]= associationState;			
		}
	    return languages;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 3;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = SWT.CENTER;

		parent.setLayoutData(gridData);
		// Create Start button
		Button startButton = createButton(parent, OK, "Start", true);
		// Create Cancel button
		Button cancelButton = createButton(parent, CANCEL, "Cancel", false);
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
		List<String> errors = getValidationErrors();
		getButton(OK).setEnabled(errors.size()==0);
		
		Operations selectedItem = fetchSelectedOperation();
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
	
	private void hide(Control aControl) {
		aControl.setVisible(false);
		((GridData)aControl.getLayoutData()).exclude=true;
	}

	private void show(Control aControl) {
		aControl.setVisible(true);
		((GridData)aControl.getLayoutData()).exclude=false;
	}

	private List<String> getValidationErrors(){
		List<String> result = new java.util.LinkedList<String>();
		switch (fetchSelectedOperation()) {
		case EXPORT:
			if (ldapFilterText.getText().length() == 0) {
				result.add("Please enter an ldap filter.");
			}else{
				//Validate the ldap filter
				try{
					com.novell.ldap.rfc2251.RfcFilter ldapFilter = new com.novell.ldap.rfc2251.RfcFilter(ldapFilterText.getText());
				}catch (Exception e) {
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
			} catch (Exception e) {
				result.add("Invalid search base:."+ldapSearchBaseText.getText());			
			}

			if (fileText.getText().length()==0)
				result.add("Please specify a file.");			
			break;
		case IMPORT:
			if (fileText.getText().length()==0)
				result.add("Please specify a file.");			
			if (testImportButton.getSelection() && logfileText.getText().length()==0)
				result.add("Please specify a log file.");			
			break;
		case MODIFY:
			if (ldapFilterText.getText().length() == 0) {
				result.add("Please enter an ldap filter.");
			}else{
				//Validate the ldap filter
				try{
					com.novell.ldap.rfc2251.RfcFilter ldapFilter = new com.novell.ldap.rfc2251.RfcFilter(ldapFilterText.getText());
				}catch (Exception e) {
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
			} catch (Exception e) {
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
		try {
			selectedSearchRoot = DSUtil.getOEFromDN(IdmModel.getIdentityVaultFromItem(targetDriver).getDSAccess(), 
					ldapSearchBaseText.getText());
		} catch (Exception e) {
			e.printStackTrace();
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
				if (MessageDialog.openConfirm(getShell(), "Confirm Association Modification", "Are you sure you want to modify the associations for this driver?"))
					super.okPressed();
				break;
			case IMPORT:
				//Ask additional confirmation when modifying associations (unless it's only a test)
				if (isTestOnly)
					super.okPressed();
				else if (MessageDialog.openConfirm(getShell(), "Confirm Association Modification", "Current driver associations for the objects imported will be overwritten. Are you sure?"))
					super.okPressed();
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

}

			