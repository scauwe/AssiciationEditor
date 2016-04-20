/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import info.vancauwenberge.idm.association.Activator;
import info.vancauwenberge.idm.association.actions.AssociationValueParser;
import info.vancauwenberge.idm.association.actions.Util;
import info.vancauwenberge.idm.association.actions.api.Const;
import info.vancauwenberge.idm.association.actions.api.IValueFilter;
import info.vancauwenberge.idm.association.actions.ldap.PagedSearchControl;
import info.vancauwenberge.idm.association.actions.ldap.PagedSearchResultControl;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.FromAssociationState;

import java.util.Enumeration;
import java.util.Iterator;

import javax.naming.directory.SearchControls;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.StatusHandler;
import org.eclipse.ui.statushandlers.StatusManager;

import com.novell.core.Core;
import com.novell.idm.model.IdentityVault;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPControl;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

@SuppressWarnings("restriction")
public abstract class AbstractLDAPSearchProcessingJob extends AbstarctLDAPJob{
	public static class BooleanResultWrapper{
		private boolean result;
		public void setResult(boolean value){
			this.result = value;
		}
		public boolean getResult(){
			return this.result;
		}
	}

	/**
	 * 
	 */
	private boolean stopped = false;
	private String driverLDAPDN;
	private String filter;
	private FromAssociationState fromState;
	private String searchRootLDAP;
	//private JobResultData taskResult;
	private static final int pageSize = 10;

	public AbstractLDAPSearchProcessingJob(String jobName, IdentityVault vault, String driverLDAPDN,
			String filter, FromAssociationState fromState,
			String searchRootLDAP) {
		super(jobName, vault);
		this.vault = vault;
		this.driverLDAPDN = driverLDAPDN;
		filter = filter.trim();
		if (!filter.startsWith("(")){
			filter = '('+filter+')';
		}
		this.filter = filter;
		this.fromState = fromState;
		this.searchRootLDAP = searchRootLDAP;
	}
	

	
	@SuppressWarnings("unchecked")
	private boolean isPagedSearchSupported(){
		LDAPConnection lc = getLDAPConnection();
		
		if (lc==null)
			return false;
		
		try{
			String         returnedAttributes[] = {"supportedControl"};
			LDAPSearchResults searchResults = lc.search("", LDAPConnection.SCOPE_BASE, "(objectclass=*)", returnedAttributes, false);

			/*
			 * The search returns one entry in the search results, and
			 * 
			 * it is the root DSE.
			 */

			LDAPEntry entry = null;
			try {
				entry = searchResults.next();
			}catch (LDAPException e) {
				Activator.log("Failed getting root of vault.",e);
			}

			LDAPAttributeSet attributeSet = entry.getAttributeSet();

			Iterator<LDAPAttribute> allAttributes = attributeSet.iterator();

			while (allAttributes.hasNext()) {

				LDAPAttribute attribute = (LDAPAttribute) allAttributes.next();

				String attrName = attribute.getName();

				Enumeration<String> allValues = attribute.getStringValues();

				if (allValues != null) {
					while (allValues.hasMoreElements()) {
						if (attrName.equalsIgnoreCase("supportedControl")) {
							String oid = (String) allValues.nextElement();

							// Check whether the requested server supports the
							// VLV Control

							// Return true if it is supported.

							if (oid.equalsIgnoreCase("2.16.840.1.113730.3.4.9")
									|| oid.equalsIgnoreCase("2.16.840.1.113730.3.4.10")
									|| oid.equals("1.2.840.113556.1.4.319"))

								return true;
							Activator.log("oid:"+oid);

						}
					else
						Activator.log("attrName:"+attrName);
						allValues.nextElement();
					}

				}

			}
			return false;
		} catch (LDAPException e) {
			Activator.log("Exception while getting supported operations",e);
			return false;
		}finally{
			try {
				lc.disconnect();
			} catch (LDAPException e) {
			}
		}
	}

	
//	protected LdapContext getLDAPContext(){
//		try{
//			Hashtable<String, Object> env = new Hashtable<String, Object>(11);
//			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
//			//Username and passwd
//			DSAccess access = IdmModel.getItemDSAccess(vault);
//			env.put(Context.SECURITY_PRINCIPAL,access.convertToLDAPAcceptableFormat(vault.getUserName()));
//			env.put(Context.SECURITY_CREDENTIALS, vault.getPassword());
//			//Simple bind
//			env.put(Context.SECURITY_AUTHENTICATION,"simple");
//
//			//host, port and TLS
//			if (vault.isUseLDAPSecureChannel()){
//				env.put(Context.SECURITY_PROTOCOL,"ssl");
//				env.put(Context.PROVIDER_URL, "ldaps://"+vault.getHost()+":"+vault.getLdapSecurePort());
//				env.put("java.naming.ldap.factory.socket","info.vancauwenberge.idm.association.actions.SSLSocketFactory");
//			}else{
//				env.put(Context.PROVIDER_URL, "ldap://"+vault.getHost()+":"+vault.getLdapClearTextPort());		    	
//			}
//
//			LdapContext ctx = new InitialLdapContext(env, null);
//
//			/*if (vault.isUseLDAPSecureChannel()){
//				StartTlsResponse tls = ( StartTlsResponse ) ctx.extendedOperation( new StartTlsRequest() );
//				tls.setHostnameVerifier( new HostnameVerifier() {
//					public boolean verify( String hostname, SSLSession session )
//					{
//						return true;
//					}
//				} );
//			}*/
//
//			return ctx;
//		}catch (Exception e) {
//			Activator.log("Exception in LDAP connection:"+e.getClass()+"-"+e.getMessage(), e);
//			return null;
//		}
//	}
	
/*	@SuppressWarnings("restriction")
	private byte[] getLDAPControlValue(int pageSize) throws IOException{
		return getLDAPControlValue(pageSize, new byte[0]);
	}
*/
	
	
	@SuppressWarnings("unchecked")
	private void pagedSearch(LDAPConnection ldapContext) throws Exception {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(AbstarctLDAPJob.readAttrs);
        searchControls.setCountLimit(0);
        searchControls.setTimeLimit(0);
        
        
        
        boolean pagedSearchIssueReported = false;
        
        AssociationValueParser valueParser = new AssociationValueParser(driverLDAPDN);
        IValueFilter valueFilter = fromState.getValueFilter();
        //We split the filter in order to avoid ldap searches where Novell eDir does return double or tripple results.
        String[] ldapFilters = getSplitLDAPFilter();
        
        	
		for (int j = 0; j < ldapFilters.length && !stopped; j++) {			
	        //LDAPControl pagedSearch = new LDAPControl("1.2.840.113556.1.4.319",true,getLDAPControlValue(pageSize));
	        LDAPControl pagedSearch = new PagedSearchControl(pageSize, true);

	        LDAPSearchConstraints cons = ldapContext.getSearchConstraints();
	        cons.setMaxResults(0);
	        cons.setServerTimeLimit(0);
	        cons.setTimeLimit(0);
	        cons.setControls( pagedSearch );
	        ldapContext.setConstraints(cons);

	        String thisFilter = ldapFilters[j];
	        addJobLogMessage("Processing filter "+(j+1)+":"+thisFilter);
			boolean processData = false;
			LDAPSearchResults namingEnumeration = null;
			do {
				//List<LDAPEntry> pageResult = simpleSearch(ldapContext, thisFilter);

				namingEnumeration = ldapContext.search(
		        		searchRootLDAP,
		        		LDAPConnection.SCOPE_SUB,
		        		thisFilter, 
		        		AbstarctLDAPJob.readAttrs,
		        		false,
		        		(LDAPSearchConstraints) null);
				
				
				
				if (namingEnumeration.getCount() > pageSize){
					if (pagedSearchIssueReported){
						processData=true;
					}else if (Util.syncOpenConfirm("Invalid eDirectory Paged Search Control",getLDAPIssueMessage()) ){
						//OK: issue is accepted
				        addJobLogMessage("Paged search issue accepted.");
						processData=true;
						pagedSearchIssueReported = true;
					}else{
						//Stop processing
				        addJobLogMessage("Paged search issue rejected. Stopping.");
						setCompleteInError();
						processData=false;
					}
				}else{
					processData=true;            	
				}
				if (processData){
					//Do the actual processing of this page
					while (namingEnumeration.hasMore() && !stopped) {
						LDAPEntry searchResult = namingEnumeration.next();
						incrementObjectCount();
						LDAPAttribute associations = searchResult.getAttribute(Const.ATTR_DIR_XML_ASSOCIATIONS);
						final String objectDN = searchResult.getDN();

						try {
							if (associations != null){
								Enumeration<String> values = (Enumeration<String>) associations.getStringValues();
								AssociationValueParser.AssociationValue[] driverValues = valueFilter.getValue(values, valueParser);
								if (driverValues != null && driverValues.length > 0) {
									for (int i = 0; i < driverValues.length; i++) {
										incrementAssociationCount();
										AssociationValueParser.AssociationValue associationObj = driverValues[i];
										processEntry(objectDN,
												driverLDAPDN,
											associationObj.getState(),
											associationObj.getAssociation());
									}
								} else {
									processEntry(objectDN, driverLDAPDN, null, null);
								}
							}else{
								//The object does not have associations => we did a query for unassociated objects...
								processEntry(objectDN, driverLDAPDN, null, null);
							}
						} catch (Exception e) {
							setCompleteInError();
							/*Activator.log(
									"Exception during processing:" + e.getMessage()
									+ "\nContinue with next entry.", e);*/
				            IStatus status = new Status(4, Activator.PLUGIN_ID, 0, "Exception during processing:" + e.getMessage()+ "\nContinue with next entry.", e);
							//StatusManager.getManager().handle(status, StatusManager.LOG);
							StatusManager.getManager().handle(status, StatusManager.SHOW);
						}

					}
				}
			} while (!stopped && processData && prepareNextPage(ldapContext, namingEnumeration));
			monitor.worked(1);
	        addJobLogMessage("Search "+(j+1)+": "+getObjectCount()+" objects in total.");
		}
        return;
    }


	private String getLDAPIssueMessage() {
		return "The given ldap filter resulted in the invalid eDirectory Paged Search Control. Some LDAP queries using Simple Paged Results method will cause eDirectory to sometimes return double or triple results for one object, e.g. (&(directReports=*)(manager=*)) might cause incorrect search results to be returned by eDirectory.\n" +
		"Do you want to continue knowing this limitation ("+
				(getObjectCount()==0?
						"nothing has been processed yet)?":
						(getObjectCount()+" objects have been processed)?")
				);
	}


	private String[] getSplitLDAPFilter() {
		switch (fromState) {
		case NOT_ASSOCIATED:
			//We do once with and without a '*': ldap does not return the entries that do not have an association value...
			return new String[] {"(&(!("+Const.ATTR_DIR_XML_ASSOCIATIONS+"="+driverLDAPDN+"#"+FromAssociationState.ANY_ASSOCIATION.getState()+"#))(!("+Const.ATTR_DIR_XML_ASSOCIATIONS+"="+driverLDAPDN+"#"+FromAssociationState.ANY_ASSOCIATION.getState()+"#*))"+filter+")"};
		default:
			String[] filters = new String[2];
			//We do once with and without a '*': ldap does not return the entries that do not have an association value...
			filters[0] = "(&("+Const.ATTR_DIR_XML_ASSOCIATIONS+"="+driverLDAPDN+"#"+fromState.getState()+"#)"+filter+")";
			filters[1] = "(&("+Const.ATTR_DIR_XML_ASSOCIATIONS+"="+driverLDAPDN+"#"+fromState.getState()+"#*)"+filter+")";
			return filters;
		}
	}
/*
    private List<SearchResult> simpleSearch(DirContext ldapContext, SearchControls searchControls, String filter) throws NamingException {
        List<SearchResult> data = new ArrayList<SearchResult>();
        NamingEnumeration<SearchResult> namingEnumeration = ldapContext.search(searchRootLDAP, filter, searchControls);
        while (namingEnumeration != null && namingEnumeration.hasMore()) {
            data.add(namingEnumeration.next());
        }
        return data;
    }
  */
	/*
    private List<LDAPEntry> simpleSearch(LDAPConnection ldapContext, String filter) throws LDAPException {
        List<LDAPEntry> data = new ArrayList<LDAPEntry>();
        LDAPSearchResults namingEnumeration = ldapContext.search(
        		searchRootLDAP,
        		LDAPConnection.SCOPE_SUB,
        		filter, 
        		readAttrs,
        		false,
        		(LDAPSearchConstraints) null);
        while (namingEnumeration != null && namingEnumeration.hasMore()) {
            data.add(namingEnumeration.next());
        }
        Activator.log("simpleSearch.responseControls:"+namingEnumeration.getResponseControls());
        return data;
    }*/
    
    
/*    private boolean prepareNextPage(LdapContext ldapContext) throws Exception {
        Control[] responseControls = ldapContext.getResponseControls();

        byte[] cookie = null;
        if (responseControls != null) {
            for (int i = 0; i < responseControls.length; i++) {
                if (responseControls[i] instanceof PagedResultsResponseControl) {
                    PagedResultsResponseControl prrc = (PagedResultsResponseControl) responseControls[i];
                    cookie = prrc.getCookie();
                    if (cookie != null) {
                        ldapContext.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
                        return true;
                    }
                }
            }
        }
        return false;

    }
*/
	
    private boolean prepareNextPage(LDAPConnection ldapContext, LDAPSearchResults res) throws Exception {
        LDAPControl[] responseControls = res.getResponseControls();
        if (responseControls != null) {
            for (int i = 0; i < responseControls.length; i++) {
                if ("1.2.840.113556.1.4.319".equals(responseControls[i].getID())) {
                    byte[] responseValue = responseControls[i].getValue();

                    if (responseValue != null && responseValue.length>0) {
                    	PagedSearchResultControl result = new PagedSearchResultControl(responseControls[i]);
                    	byte[] cookie = result.getCookie();
                    	if (cookie != null && cookie.length>0){
                    		LDAPControl pagedSearch = new PagedSearchControl(pageSize, result.getCookie(), true);
                    		LDAPSearchConstraints cons = ldapContext.getSearchConstraints();
                    		cons.setControls( pagedSearch );
                    		ldapContext.setConstraints(cons);
                    		return true;
                    	}
                    }
                }
            }
        }
        return false;

    }
	/*
	private byte[] getLDAPControlValue(int pagesize2, byte[] cookie) throws IOException {
        // build the ASN.1 encoding
        BerEncoder ber = new BerEncoder(10 + cookie.length);

        ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
            ber.encodeInt(pageSize);
            ber.encodeOctetString(cookie, Ber.ASN_OCTET_STR);
        ber.endSeq();

        return ber.getTrimmedBuf();
	}*/


	/**
	 * 
	 */
	protected void doJob() {
		monitor.beginTask("Searching for objects", 3);
		addJobLogMessage("Searching for objects");
		UpdateStatusThread th = new UpdateStatusThread(this, "Found {0,number} objects.");
		LDAPConnection ctx = null;
		try {
			addJobLogMessage("Getting ldap connection");
			ctx = getLDAPConnection();
			monitor.worked(1);
			//We have 2 filters: one with an association value, one without an association value
			th.start();
			pagedSearch(ctx);
			monitor.done();
		} catch (Throwable e) {
			addJobLogMessage("Exception :"+e.getClass().getName()+"-"+e.getMessage());
			Activator.log("Exception :"+e.getClass().getName()+"-"+e.getMessage(),e);
			monitor.done();
		}finally{
			if (th != null){
				th.stopThread();
			}
			try{
				doneUpdate();
			}catch(Exception e){
				Activator.log("Exception during clean up after processing:"+e.getMessage(), e);
				Core.errorDlg("Exception during clean up after processing:"+e.getMessage(), e);				
			}

			if (ctx != null)
				try{
					ctx.disconnect();
				}catch (Exception e) {
				}
		}
		
		
//		LdapContext ctx = null;
//		int objectCount = 0;
//		int associationCount = 0;
//		boolean exception = false;
//		try{
//			ctx = getLDAPContext();
//			
//			byte[] cookie = null;
//			ctx.setRequestControls(new Control[] { new PagedResultsControl(pageSize, Control.CRITICAL) });
//			
//
//			SearchControls ctls = new SearchControls();
//			ctls.setReturningAttributes(readAttrs);
//			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
//			
//			IValueFilter valueFilter = fromState.getValueFilter();
//
//			//final Display display = ProcessDialog.this.getShell().getDisplay();
//			AssociationValueParser valueParser = new AssociationValueParser(driverLDAPDN);
//
//			do{
//				NamingEnumeration<SearchResult> results = ctx.search(searchRootLDAP, filter,ctls);
//				cookie = null;
//				
//				while ( results.hasMore() && !stopped) {
//					SearchResult nextEntry = null;
//					try {
//						nextEntry = results.next();
//						objectCount++;
//					} catch(NamingException e) {
//						Activator.log("Process halted due to exception :"+e.getClass().getName()+"-"+e.getMessage(),e);
//						break;
//						/*
//						if(e.getResultCode() == LDAPException.LDAP_TIMEOUT || e.getResultCode() == LDAPException.CONNECT_ERROR){
//							Activator.log("Process halted due to exception :"+e.getClass().getName()+"-"+e.getResultCode()+"-"+e.getMessage(),e);
//							break;
//						}
//						else{
//							Activator.log("Process encouneterd an exception but will try to continue. Exception :"+e.getClass().getName()+"-"+e.getResultCode()+"-"+e.getMessage(),e);
//							continue;
//						}
//						*/
//					}
//					Attributes attributeSet = nextEntry.getAttributes();
//					final String objectDN = nextEntry.getNameInNamespace();
//					
//					Attribute associations = attributeSet.get(ProcessDialog.ATTR_DIR_XML_ASSOCIATIONS);
//					NamingEnumeration<String> values = (NamingEnumeration<String>) associations.getAll();
//					AssociationValueParser.AssociationValue[] driverValues = valueFilter.getValue(values, valueParser, fromState);
//					try{
//						if (driverValues != null && driverValues.length>0){
//							for (int i = 0; i < driverValues.length; i++) {
//								associationCount++;
//								AssociationValueParser.AssociationValue associationObj = driverValues[i];
//								processEntry(objectDN, associationObj.getDriverDN(), associationObj.getState(), associationObj.getAssociation());
//								processDialog.updateTableData(objectDN, associationObj.getDriverDN(), associationObj.getState(), associationObj.getAssociation());
//							}
//						}else{
//							processEntry(objectDN, null,null,null);
//							processDialog.updateTableData(objectDN, null,null,null);
//						}
//					}catch (Exception e) {
//						exception = true;
//						Activator.log("Exception during processing:"+e.getMessage()+"\nContinue with next entry.", e);
//						Core.errorDlg("Exception during processing:"+e.getMessage()+"\nContinue with next entry.", e);
//					}
//				}//while processing entries
//				
//				// Examine the paged results control response
//				Control[] controls = ctx.getResponseControls();
//				if (controls != null) {
//					for (int i = 0; i < controls.length; i++) {
//						if (controls[i] instanceof PagedResultsResponseControl) {
//							PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
//							int total = prrc.getResultSize();
//							if (total != 0) {
//								Activator.log("***************** END-OF-PAGE (total : " + total + ") *****************\n");
//							} else {
//								Activator.log("***************** END-OF-PAGE  (total: unknown) ***************\n");
//							}
//							cookie = prrc.getCookie();
//							// Re-activate paged results
//							if (cookie != null){
//								Activator.log("Starting next page");
//								ctx.setRequestControls(new Control[] { new PagedResultsControl(pageSize, cookie, Control.CRITICAL) });
//							}else{
//								Activator.log("No cookie!");								
//							}
//						}
//					}
//				} else {
//					System.out.println("No controls were sent from the server");
//				}
//
//			} while (((cookie != null) && (cookie.length != 0)));
//		}catch(Exception ldapE){
//			exception = true;
//			Activator.log("Exception during processing:"+ldapE.getMessage()+"\nProcess will stop.", ldapE);
//			Core.errorDlg("Exception during processing:"+ldapE.getMessage()+"\nProcess will stop.", ldapE);				
//		}finally{
//			try{
//				doneUpdate();
//			}catch(Exception e){
//				Activator.log("Exception during clean up after processing:"+e.getMessage(), e);
//				Core.errorDlg("Exception during clean up after processing:"+e.getMessage(), e);				
//			}
//			
//			if (ctx != null)
//				try{
//					ctx.close();
//				}catch (Exception e) {
//				}
//			processDialog.doneUpdate(exception, objectCount, associationCount);
//		}
	}

	public abstract void doneUpdate() throws Exception;


	public abstract void processEntry(String objectDN, String driverDN, String state,
			String association) throws Exception;


	public void canceling() {
		stopped = true;
	}
	
	
	
}