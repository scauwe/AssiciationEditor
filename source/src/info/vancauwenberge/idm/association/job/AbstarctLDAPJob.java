/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;

import com.novell.core.datatools.access.nds.DSAccess;
import com.novell.core.datatools.access.nds.LDAPTrustManager;
import com.novell.idm.IdmModel;
import com.novell.idm.model.IdentityVault;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.sun.net.ssl.internal.ssl.Provider;

import info.vancauwenberge.idm.association.Activator;
import info.vancauwenberge.idm.association.actions.api.Const;
import info.vancauwenberge.idm.association.dialog.AssociationStatusDialog;

public abstract class AbstarctLDAPJob extends Job {

	protected IProgressMonitor monitor;
	private long associationCount = 0;
	private long objectCount = 0;
	//private Throwable exception = null;
	private boolean errorWhilProcessing = false;
	private IStatus thisActionResultStates = null;
	private static final Status OK = new Status(Status.OK, Activator.PLUGIN_ID, "Finished with success");
	private static final Status ERROR = new Status(Status.ERROR, Activator.PLUGIN_ID, "Finished with errors");


	protected AbstarctLDAPJob(final String name, final IdentityVault vault) {
		super(name);
		this.vault = vault;
		addJobLogMessage(name);
	}


	public void setCompleteInError()
	{
		errorWhilProcessing = true;
	}
	/*
	public void setException(Throwable th){
		this.exception = th;
	}*/

	public void incrementObjectCount(){
		//TODO: remove this delay. Only for test purposes to see that it is actually doing something.
		/*try {
			Thread.currentThread().sleep(500);
		} catch (InterruptedException e) {
		}*/
		objectCount++;
	}

	public void incrementAssociationCount(){
		associationCount++;
	}

	protected void showResults() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				getJobFinishedAction().run();
			}
		});
	}

	private final Action getJobFinishedAction() {

		final Action action = new Action(getName()) {
			@Override
			public void run() {
				final StringBuilder message = new StringBuilder();
				if (getActionResultStatus().isOK()) {
					message.append("Task completed with success.\n");
				} else {
					message.append("Task completed with errors.\n");
				}
				//Append the total number of objects and associations processed
				message.append("Associations: ").append(associationCount).append("\n");
				message.append("Objects: ").append(objectCount ).append("\n");
				/*
					Iterator<String> messageIter = messages.iterator();
					while (messageIter.hasNext()) {
						String string = (String) messageIter.next();
						message.append(string).append("\n");
					}*/

				/*MessageDialog
							.openInformation(
									Activator.getActiveShell(),
									errorWhilProcessing ? "Task completed with errors."
											: "Task completed with success.",
											message.toString());*/
				final AssociationStatusDialog status = new AssociationStatusDialog(Activator.getActiveShell(), message.toString(), messages.toArray(new String[messages.size()]));
				status.open();
			}
		};
		return action;
	}



	protected IStatus getActionResultStatus(){
		if (thisActionResultStates == null){
			thisActionResultStates=monitor.isCanceled()?
					Status.CANCEL_STATUS:
						(errorWhilProcessing?ERROR:OK);
		}
		return thisActionResultStates ;
	}


	public void addJobLogMessage(final String message){
		if (monitor != null) {
			monitor.subTask(message);
		}
		messages.addLast(message);
	}


	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		this.monitor = monitor;
		final SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS");
		addJobLogMessage("Starting job at "+df.format(new Date()));
		doJob();
		setProperty(IProgressConstants.ICON_PROPERTY, Activator.getImageDescriptor("icons/sample2.png"));
		setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);

		if (isModal(this)) {
			// The progress dialog is still open so
			// just open the message
			showResults();
		}
		//But also make it 'clickable' for reference later on
		setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
		setProperty(IProgressConstants.ACTION_PROPERTY,getJobFinishedAction());

		addJobLogMessage("Stoping job at "+df.format(new Date()));
		final IStatus result = getActionResultStatus();
		this.monitor = null;
		return result;
	}


	protected abstract  void doJob(); 

	public static boolean isModal(final Job job) {
		final Boolean isModal = (Boolean)job.getProperty(
				IProgressConstants.PROPERTY_IN_DIALOG);
		if(isModal == null) {
			return false;
		}
		return isModal.booleanValue();
	}

	protected IdentityVault vault;
	private final LinkedList<String> messages = new LinkedList<String>();
	static final String[] readAttrs = new String[]{Const.ATTR_DIR_XML_ASSOCIATIONS};


	@SuppressWarnings("restriction")
	protected LDAPConnection getLDAPConnection() {
		LDAPConnection ldapConnection;
		try{
			if (vault.isUseLDAPSecureChannel())
			{
				Security.addProvider(new Provider());
				final TrustManager[] arrayOfTrustManager = { new LDAPTrustManager() };
				final SSLContext sslContext = SSLContext.getInstance("TLS", "SunJSSE");
				sslContext.init(null, arrayOfTrustManager, null);
				final LDAPJSSESecureSocketFactory socketFactory = new LDAPJSSESecureSocketFactory(sslContext.getSocketFactory());
				ldapConnection = new LDAPConnection(socketFactory);
				ldapConnection.connect(vault.getHost(), vault.getLdapSecurePort());
			}
			else
			{
				ldapConnection = new LDAPConnection();
				ldapConnection.connect(vault.getHost(), vault.getLdapClearTextPort());
			}
			final DSAccess access = IdmModel.getItemDSAccess(vault);
			ldapConnection.bind(3, access.convertToLDAPAcceptableFormat(vault.getUserName()), vault.getPassword().getBytes());

		}catch (final Exception e) {
			//Log the error
			Activator.log("Exception while getting LDAP connection:"+e.getClass()+"-"+e.getMessage(), e);
			return null;
		}
		return ldapConnection;
	}
	/*
		private PagedSearchResult pagedSearch(LdapContext ldapContext) throws Exception {
	    	PagedSearchResult result = new PagedSearchResult();

	        SearchControls searchControls = new SearchControls();
	        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
	        searchControls.setReturningAttributes(readAttrs);
	        searchControls.setCountLimit(0);
	        searchControls.setTimeLimit(0);


	        Control[] requestControls = new Control[]{new PagedResultsControl(pageSize, Control.CRITICAL)};
	        ldapContext.setRequestControls(requestControls);
	        boolean pagedSearchIssueReported = false;

	        AssociationValueParser valueParser = new AssociationValueParser(driverLDAPDN);
	        IValueFilter valueFilter = fromState.getValueFilter();
	        String[] ldapFilters = getSplitLDAPFilter();


			for (int j = 0; j < ldapFilters.length; j++) {
				String thisFilter = ldapFilters[j];
				Activator.log("Processing filter "+(j+1)+":"+thisFilter);
				boolean processData = false;
				do {
					List<SearchResult> pageResult = simpleSearch(ldapContext, searchControls, thisFilter);
					if (pageResult.size() > pageSize){
						if (pagedSearchIssueReported){
							processData=true;
						}else if (Util.syncOpenConfirm("Invalid eDirectory Paged Search Control",getLDAPIssueMessage(result)) ){
							//OK: issue is accepted
							processData=true;
							pagedSearchIssueReported = true;
						}else{
							//Stop processing
							result.setEncounteredError(true);
							processData=false;
						}
					}else{
						processData=true;            	
					}
					if (processData){
						//Do the actual processing of this page
						Iterator<SearchResult> iter = pageResult.iterator();
						while (iter.hasNext()) {
							SearchResult searchResult = (SearchResult) iter.next();
							result.incrementObjectCount();
							Attributes attributeSet = searchResult.getAttributes();
							final String objectDN = searchResult.getNameInNamespace();

							Attribute associations = attributeSet.get(ProcessDialog.ATTR_DIR_XML_ASSOCIATIONS);
							NamingEnumeration<String> values = (NamingEnumeration<String>) associations.getAll();
							AssociationValueParser.AssociationValue[] driverValues = valueFilter.getValue(values, valueParser, fromState);
							try {
								if (driverValues != null && driverValues.length > 0) {
									for (int i = 0; i < driverValues.length; i++) {
										result.incrementAssociationCount();
										AssociationValueParser.AssociationValue associationObj = driverValues[i];
										processEntry(objectDN,
												associationObj.getDriverDN(),
												associationObj.getState(),
												associationObj.getAssociation());
										processDialog.updateTableData(objectDN,
												associationObj.getDriverDN(),
												associationObj.getState(),
												associationObj.getAssociation());
									}
								} else {
									processEntry(objectDN, null, null, null);
									processDialog.updateTableData(objectDN, null, null,
											null);
								}
							} catch (Exception e) {
								result.setEncounteredError(true);
								Activator.log(
										"Exception during processing:" + e.getMessage()
										+ "\nContinue with next entry.", e);
								Core.errorDlg(
										"Exception during processing:" + e.getMessage()
										+ "\nContinue with next entry.", e);
							}

						}
					}
					Activator.log("Page returned " + pageResult.size() + " entries");
				} while (processData && prepareNextPage(ldapContext));
			}
	        return result;
	    }*/
	public long getObjectCount() {
		return objectCount;
	}


	protected IProgressMonitor getMonitor() {
		return monitor;
	}

}