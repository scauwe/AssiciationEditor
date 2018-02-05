/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.novell.admin.common.exceptions.SPIException;
import com.novell.application.console.snapin.ObjectEntry;
import com.novell.core.Core;
import com.novell.core.datatools.access.nds.DSAccess;
import com.novell.core.datatools.access.nds.DSAccessException;
import com.novell.core.datatools.access.nds.DSUtil;
import com.novell.core.util.DNConverter;
import com.novell.idm.IdmApp;
import com.novell.idm.IdmModel;
import com.novell.idm.model.Application;
import com.novell.idm.model.Driver;
import com.novell.idm.model.IdentityVault;
import com.novell.idm.model.Item;
//import com.novell.ldap.LDAPControl;
//import com.novell.ldap.controls.LDAPVirtualListResponse;

import info.vancauwenberge.idm.association.Activator;
import info.vancauwenberge.idm.association.dialog.AssociationDialog;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.FromAssociationState;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.Operations;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.ToAssociationState;
import info.vancauwenberge.idm.association.dialog.SelectDriverDialog;
import info.vancauwenberge.idm.association.dialog.SelectDriverDialog.SelectionType;
import info.vancauwenberge.idm.association.job.AbstarctLDAPJob;
import info.vancauwenberge.idm.association.job.ExportProcessingJob;
import info.vancauwenberge.idm.association.job.ImportAssociationsJob;
import info.vancauwenberge.idm.association.job.ModifyProcessingJob;


/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class AssociationAction implements IWorkbenchWindowActionDelegate {
	//private IWorkbenchWindow window;
	private ISelection selection;
	private boolean isLDAP=false;

	/**
	 * The constructor.
	 */
	public AssociationAction() {
		try {
			final Class<Core> clazz = Core.class;
			final Field field = clazz.getDeclaredField("isLDAP");
			if (field==null) {
				this.isLDAP = false;
			} else {
				this.isLDAP = ((Boolean)field.get(null)).booleanValue();
			}
		} catch (final NoSuchFieldException e) {
			//When the LDAP field is not available, we are not in LDAP mode.
			this.isLDAP = false;
		} catch (final Exception e) {
			Activator.log("Failed getting the isLDAP field. Assuming false.", e);
			this.isLDAP = false;
		}
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * NOTE: This is not called if action is activated from a context menu (eg. from JDT package view).
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(final IAction action) {
		final Map<Driver, ObjectEntry> selectedDrivers = getSelectedDriver(action);
		if (selectedDrivers != null){
			final Driver targetDriver = limitSelection(selectedDrivers);
			//if (isPagedSearchSupported(IdmModel.getIdentityVault(targetDriver))){
			if (targetDriver != null){
				/*try{
					final CAttributeImpl attribute = (CAttributeImpl)targetDriver.getAttribute("DirXML-UseNoRefAssoc");
					Activator.log(attribute.toString(),null);
					Activator.log(""+attribute.getStringValue(),null);
					Activator.log(""+attribute.eContents(),null);
					Activator.log(""+attribute.eGet(5,false,false),null);
				}catch(final Exception e){
					Activator.log("Exception while trying to read DirXML-UseNoRefAssoc:"+e.getMessage(), e);
					Core.errorDlg("Exception while trying to read DirXML-UseNoRefAssoc:"+e.getMessage(), e);			
				}*/

				//Activator.log("Selected object:"+targetDriver);
				final AssociationDialog dialog = new AssociationDialog(Activator.getActiveShell(), targetDriver);
				final Operations operation = dialog.getSelectedOperation();
				if (operation != null){

					//Do the search
					try {
						process(targetDriver, operation, 
								dialog.getSelectedSearchRoot(), 
								dialog.getSelectedLDAPFilter(), 
								dialog.getSelectedFromState(), 
								dialog.getSelectedToState(), 
								dialog.getSelectedFileName(), 
								dialog.isTestOnly(), dialog.getSelectedLogFile());
					} catch (final Exception e) {
						Activator.log("Exception:"+e.getClass(), e);
					}
				}

			}
			//}else{
			//	 Core.infoDlg("Paged search is not supported in you environment.");
			//}
		}
	}

	private void process(final Driver targetDriver, final Operations operation,
			final ObjectEntry searchRoot, final String filter,
			final FromAssociationState fromState, final ToAssociationState toState, final String fileName, final boolean isTestOnly, final String logFile) {
		final IdentityVault vault= IdmModel.getIdentityVault(targetDriver);

		try {
			final String searchRootLDAP = getLDAPDNFromOE(searchRoot);
			final String driverLDAPDN = getLDAPDNFromItem(targetDriver);

			//Start a new job that will do the actual action
			AbstarctLDAPJob processingJob = null;
			try{
				switch (operation) {
				case EXPORT:
					processingJob = new ExportProcessingJob(vault, driverLDAPDN,filter,fromState,searchRootLDAP,fileName);			
					break;
				case MODIFY:
					processingJob = new ModifyProcessingJob(vault, driverLDAPDN,filter,fromState,searchRootLDAP,toState, logFile);			
					break;
				case IMPORT:
					processingJob = new ImportAssociationsJob(vault, driverLDAPDN, fileName, isTestOnly, logFile);
					break;
				default:
					processingJob = new ExportProcessingJob(vault, driverLDAPDN,filter,fromState,searchRootLDAP, fileName);
					break;
				}
				processingJob.setUser(true);
				processingJob.schedule();

			}catch (final Exception e) {
				Activator.log("Unable to start the job:"+e.getMessage(), e);
				Core.errorDlg("Unable to start the job:"+e.getMessage(), e);
			}



			//doSearch(filter, ldapConnection, searchRootLDAP);
			//Pages search control
			//		    	int offset = 0;
			//				int beforeCount = 0;
			//				int pageSize = 10;
			//				int contentCount = 100;
			//	    		String context = null;

			//	    		//Sort is required with paged searches
			//		    	int okclicked = 0;
			//		    	
			//				LDAPSortKey[] keys = new LDAPSortKey[1];
			//				keys[0] = new LDAPSortKey("cn");
			//				LDAPSortControl sortControl = new LDAPSortControl(keys, true);
			//				Set<String> resultSet = new HashSet<String>();
			//				while (true){
			//	    			Activator.log("New VLC offset:"+offset+"\tcontentCount:"+contentCount);
			//		    		org.ietf.ldap.controls.LDAPVirtualListControl control = new org.ietf.ldap.controls.LDAPVirtualListControl(offset,beforeCount,pageSize,contentCount,context);
			//		    		
			//					
			//					// Set the controls to be sent as part of search request
			//		    		LDAPSearchConstraints sc = new LDAPSearchConstraints();//.getSearchConstraints();
			//		    		sc.setControls(new LDAPControl[]{sortControl,control});
			//		    		//ldapConnection.setConstraints(sc);
			//
			//		    		LDAPSearchResults res = ldapConnection.search(searchRootLDAP, LDAPConnection.SCOPE_SUB, filter, readAttrs, false, sc);
			//		    		int resultCount = 0;
			//		    		while (res.hasMore()){
			//		    			try{
			//		    				LDAPEntry entry = res.next();
			//		    				resultCount++;
			//		    				if (resultSet.contains(entry.getDN())){
			//		    					Activator.log("Duplicate entry:"+entry.getDN());
			//		    				}else{
			//		    					resultSet.add(entry.getDN());
			//		    				}
			//			    			//Activator.log(entry.getDN());
			//			    			/*
			//							LDAPAttributeSet findAttrs = entry.getAttributeSet();
			//							Iterator enumAttrs = findAttrs.iterator();
			//							while (enumAttrs.hasNext()) {
			//								LDAPAttribute anAttr = (LDAPAttribute) enumAttrs.next();
			//								Enumeration enumVals = anAttr.getStringValues();
			//								String result = anAttr.getName();
			//								while (enumVals.hasMoreElements()) {
			//									Object object = (Object) enumVals.nextElement();
			//									result = result + "\n" + object;
			//								}
			//								Activator.log("  Attribute:"+result);
			//							}*/
			//		    			}catch(LDAPReferralException e){
			//			    			Activator.log("LDAP Referal exception:"+e.getMessage());
			//		    				continue;
			//		    			}
			//		    		}
			//		    		LDAPControl[] responseControls = res.getResponseControls();
			//		    		org.ietf.ldap.controls.LDAPVirtualListResponse vlControl = getVLResponseControl(responseControls);
			//	    			Activator.log("Content count:"+vlControl.getContentCount()+"\tResults read:"+resultCount+"\tFirst:"+vlControl.getFirstPosition()+"\tSet size:"+resultSet.size());
			//	    		
			//		    		if (vlControl != null && vlControl.getContentCount()!=0){
			//		    			Core.infoDlg("Preparing for next loop: "
			//		    					+ "\nPrevious loop:"+resultCount
			//		    					+ "\nvlControl.count:"+vlControl.getContentCount()
			//		    					+ "\nvlControl.first:"+vlControl.getFirstPosition()
			//		    					+ "\nvlControl.resultCode:"+vlControl.getResultCode()
			//		    					+ "\nNew offest:"+(vlControl.getFirstPosition() + resultCount));
			//				    	offset = vlControl.getFirstPosition() + resultCount;//+=resultCount ;
			//				    	okclicked++;
			//				    	if (okclicked>=10){
			//				    		Core.infoDlg("Breaking due to error");
			//				    		break;
			//				    	}
			//						//beforeCount = 0;
			//						//pageSize = 10;
			//						contentCount = vlControl.getContentCount();
			//			    		context = vlControl.getContext();
			//			    		if (offset>vlControl.getContentCount()){
			//			    			Activator.log("Reached end of loop");
			//			    			break;
			//			    		}
			//		    		}else{
			//		    			Activator.log("Done reading loop: vlControl="+vlControl);
			//		    			break;
			//		    		}
			//	    		}
			//				Activator.log("Total entries processed:"+resultSet.size());
		} catch (final Exception e) {
			Activator.log("Uncaught exception:"+e.getClass()+"-"+e.getMessage(),e);
		}
	}


	/*
	private org.ietf.ldap.controls.LDAPVirtualListResponse getVLResponseControl(LDAPControl[] responseControls) {
		LDAPVirtualListResponse vlControl;
		for (int i = 0; i < responseControls.length; i++) {
			LDAPControl ldapControl = responseControls[i];
			if (ldapControl instanceof org.ietf.ldap.controls.LDAPVirtualListResponse){
				return (org.ietf.ldap.controls.LDAPVirtualListResponse)ldapControl;
			}
		}
		return null;
	}*/

	private static String getLDAPDNFromItem(final Item targetDriver)
			throws DSAccessException, SPIException {
		final DSAccess access = IdmModel.getItemDSAccess(targetDriver);
		final String driverLDAPDN = access.convertToLDAPAcceptableFormat(targetDriver.getDirectoryDN());
		return driverLDAPDN;
	}





	private String getLDAPDNFromOE(final ObjectEntry searchRoot) {
		if ((searchRoot == null) || searchRoot.equals("")) {
			Activator.log("No search root found.");
			return null;
		}
		String searchRootLDAP = DSUtil.getDNFromOE(searchRoot);
		//In IDM 4.6, the OE is already in LDAP format. Just return the result.LDAP 
		if (isLDAP) {
			return searchRootLDAP;
		}else{
			final DNConverter converter = new DNConverter(searchRootLDAP,"qualified-dot");
			searchRootLDAP = converter.getDN("ldap");
			return searchRootLDAP;
		}
	}


	/**
	 * If multiple drivers are selected, limit the selection to just one driver.
	 * @param selectedDrivers
	 * @return
	 */
	private Driver limitSelection(final Map<Driver, ObjectEntry> selectedDrivers) {
		Driver targetDriver = null;

		if (selectedDrivers.size() > 1){
			//We only can use 1: ask the user what driver.
			final Collection<ObjectEntry> values = selectedDrivers.values();
			ObjectEntry[] objectEntries = new ObjectEntry[values.size()];
			objectEntries = values.toArray(objectEntries);

			final SelectDriverDialog dialog = new SelectDriverDialog(Activator.getActiveShell() , objectEntries,SelectionType.SINGLE);
			if (dialog.getOked()){
				final ObjectEntry targetDriverOE = dialog.getSelectedOE();
				//Search for this OE in the map
				for (final Driver aDriver : selectedDrivers.keySet()) {
					final ObjectEntry anObjectEntry = selectedDrivers.get(aDriver);
					if (anObjectEntry==targetDriverOE) {
						targetDriver=aDriver;
					}
				}
			}
		}else if (selectedDrivers.size()==1){
			//OK: we have the single driver object. Use that as a base.
			targetDriver = selectedDrivers.keySet().iterator().next();
		}
		return targetDriver;
	}


	private Map<Driver, ObjectEntry> getSelectedDriver(final IAction action) {
		if (Core.isMac()) {
			Core.liveNotSupportedMsg();
			return null;
		}

		if ((this.selection == null) || (this.selection.isEmpty())
				|| (!(this.selection instanceof StructuredSelection))) {
			Core.infoDlg("The selected object is not a driver object.");
			return null;
		}

		final Item[] selectedItems = IdmApp.getActivePartSelectedItems(
				this.selection, true);
		final Set<Driver> drivers = new HashSet<Driver>();

		if (selectedItems == null) {
			return null;
		}

		// Get all driver objects selected.
		for (final Item aSelectedItem : selectedItems) {
			if ((aSelectedItem instanceof Driver)) {
				drivers.add((Driver) aSelectedItem);
			} else if ((aSelectedItem instanceof Application)) {
				final EList<Driver> localObject2 = ((Application) aSelectedItem)
						.getDrivers();
				final Iterator<Driver> driverIter = localObject2.iterator();
				while (driverIter.hasNext()) {
					drivers.add(driverIter.next());
				}
			} else {
				/*Core.infoDlg("The selected object is not a driver object:"+aSelectedItem.getKey());
				return null;*/
			}
		}

		// Loop over all the drivers to get the DSAccess objects
		final Map<Driver, ObjectEntry> resultMap = new HashMap<Driver, ObjectEntry>();
		final LinkedList<String> skippedObjects = new LinkedList<String>();
		boolean found = false;
		final Iterator<Driver> driverIterator = drivers.iterator();
		while (driverIterator.hasNext()) {
			final Driver aDriver = driverIterator.next();
			try {
				final DSAccess driverDSAccess = IdmModel.getItemDSAccess(aDriver);
				//com.novell.core.datatools.access.nds.LDAPDSUtil.getOEF
				final ObjectEntry result = DSUtil.getOEFromDN(driverDSAccess, aDriver.getDirectoryDN());
				resultMap.put(aDriver, result);
				found = true;
			} catch (final Exception localException) {
				Activator.log("Skipping object.", localException);
				skippedObjects.add(aDriver.getName());
			}
		}
		if (!found && (skippedObjects.size()==1)) {
			Core.infoDlg("The selected driver ("+skippedObjects.getFirst()+") is either not deployed or not reachable. Unable to continue the action.");
			return null;
		}
		if (!found && (skippedObjects.size()>1)) {
			Core.infoDlg("None of the selected drivers ("+skippedObjects+")are either deployed or reachable. Unable to continue the action.");
			return null;
		}
		if (!found && (drivers.size()>0)){
			Core.infoDlg("Unknown error: none of the following objects generated an excpetion, but none was found neither:\n"+drivers);
			return null;
		}
		return resultMap;
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	@Override
	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	@Override
	public void dispose() {
		this.selection = null;
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	@Override
	public void init(final IWorkbenchWindow window) {
		//Core.infoDlg("AssociationAction.init()"+window);
		//this.window = window;
	}
}