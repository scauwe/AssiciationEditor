/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import info.vancauwenberge.idm.association.actions.api.Const;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.FromAssociationState;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.ToAssociationState;
import info.vancauwenberge.idm.association.log.LogStrategy;

import java.io.IOException;

import com.novell.idm.model.IdentityVault;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;

public class ModifyProcessingJob extends AbstractLDAPSearchProcessingJob {

	private ToAssociationState toState;
	private LDAPConnection ldap;
	private LogStrategy logStrategy;

	public ModifyProcessingJob(
			IdentityVault vault, String driverLDAPDN, String filter,
			FromAssociationState fromState, String searchRootLDAP, ToAssociationState toState, String logFile) throws IOException {
		super("Modify Associations", vault, driverLDAPDN, filter, fromState, searchRootLDAP);
		this.toState = toState;
		ldap = super.getLDAPConnection();
		this.logStrategy = LogStrategy.getLogStrategy(logFile);
        addJobLogMessage("  Filter:"+filter);
        addJobLogMessage("  Driver:"+driverLDAPDN);
        addJobLogMessage("  Search base:"+searchRootLDAP);
        addJobLogMessage("  To state:"+toState);
        addJobLogMessage("  From state:"+fromState);
        addJobLogMessage("Job details:");
	}

	@Override
	public void processEntry(String objectDN, String driverDN, String state,
			String association) throws Exception{
		try{
			switch (toState) {
			case REMOVE:
				if (state != null && association != null){
					LDAPAttribute attribute= new LDAPAttribute( Const.ATTR_DIR_XML_ASSOCIATIONS, driverDN+"#"+state+"#"+association);			
					LDAPModification mod = new LDAPModification(LDAPModification.DELETE, attribute);
					ldap.modify(objectDN, mod);
					logStrategy.log(objectDN, state, association, "Association removed");
				}
				else{
					logStrategy.log(objectDN, state, association, "Nothing to remove");				
				}
				break;
			
			default:
				//Issue 1:
				//The old attribute can be null (eg: change association from not associated to something) 
				LDAPAttribute oldAttribute = null;
				if (state != null && association != null){
					oldAttribute= new LDAPAttribute( Const.ATTR_DIR_XML_ASSOCIATIONS, driverDN+"#"+state+"#"+association);
				}
				//What if the current association is the same as the old association??? => remove the value and add it again. Does this work???
				//Note: Only relevant if you want to perform a sync (state 4).
				LDAPAttribute newAttribute= new LDAPAttribute( Const.ATTR_DIR_XML_ASSOCIATIONS, driverDN+"#"+toState.getState()+"#"+((association==null)?"":association));			
				LDAPModification[] mods = (oldAttribute==null)?new LDAPModification[]{new LDAPModification(LDAPModification.ADD, newAttribute)}:new LDAPModification[]{new LDAPModification(LDAPModification.DELETE, oldAttribute),new LDAPModification(LDAPModification.ADD, newAttribute)};
				try{
					ldap.modify(objectDN, mods);
				}catch (LDAPException e) {
					if (e.getResultCode()==-614){
						addJobLogMessage("Caught exception:"+e);
						//LDAPException: Attribute Or Value Exists (20) Attribute Or Value Exists
						//LDAPException: Server Message: NDS error: duplicate value (-614)
					}
					else
						throw e;
				}
				
				logStrategy.log(objectDN, state, association, "State changed from "+((state==null)?"<empty>":state)+ " to "+toState.getState());
			break;
			}
		}catch (Exception e) {
			logStrategy.log(objectDN, state, association, "Exception while processing entry:"+e.getClass().getName()+":"+e.getMessage());
			throw e;
		}
	}



	@Override
	public void doneUpdate() throws Exception {
		if (ldap != null){
			try{
				ldap.disconnect();
				ldap = null;
			}catch (Exception e) {
			}
		}
		logStrategy.close();
	}

}
