/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import info.vancauwenberge.idm.association.Activator;
import info.vancauwenberge.idm.association.actions.AssociationValueParser;
import info.vancauwenberge.idm.association.actions.AssociationValueParser.AssociationValue;
import info.vancauwenberge.idm.association.actions.api.Const;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.FromAssociationState;
import info.vancauwenberge.idm.association.log.LogStrategy;

import java.io.IOException;
import java.util.Enumeration;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;

public abstract class AbstractImportStrategy {
	protected LDAPConnection ldapCon;
	protected AssociationValueParser valueParser;
	protected LogStrategy logStrategy;

	public AbstractImportStrategy(LDAPConnection ldapCon, AssociationValueParser valueParser, LogStrategy logStrategy){
		this.ldapCon = ldapCon;
		this.valueParser = valueParser;
		this.logStrategy = logStrategy;
	}

	public void processEntry(String objectDN, 
			String associationState, String associationValue) throws IOException{
		try {
			LDAPEntry entry = ldapCon.read(objectDN, ImportAssociationsJob.readAttrs);
			String[] validationResult = doPProcessEntry(entry, associationState, associationValue);
			logStrategy.log(objectDN, associationState, associationValue, validationResult);
		} catch (LDAPException e) {
			Activator.log("Error while processing entry.",e);
			logStrategy.log(objectDN, associationState, associationValue, new String[]{"Exception:"+e.getClass().getName()+"-"+e.getMessage()});
		}

	}

	@SuppressWarnings("unchecked")
	protected String[] doPProcessEntry(LDAPEntry entry, String associationState, String associationValue) throws LDAPException {
		if (entry != null){
			LDAPAttribute associations = entry.getAttribute(Const.ATTR_DIR_XML_ASSOCIATIONS);
			AssociationValueParser.AssociationValue[] driverValues = null;
			if (associations!=null)
				driverValues = FromAssociationState.ANY_ASSOCIATION.getValueFilter().getValue(
					(Enumeration<String>)associations.getStringValues(), valueParser);
			if (driverValues!= null && driverValues.length>0){
				String[] results = new String[driverValues.length];
				for (int i = 0; i < driverValues.length; i++) {
					AssociationValueParser.AssociationValue idvAssociationValue = driverValues[i];
					results[i] = getValidationMessage(associationState, associationValue, idvAssociationValue);
				}
				removeAndAddAssociations(entry.getDN(), valueParser.getUpperDriverDN(), driverValues, associationState, associationValue);
				return results;
			}else{
				addAssociation(entry.getDN(), valueParser.getUpperDriverDN(), associationState, associationValue);

				return new String[]{"OK (no current association)"};
			}
		}else{
			return new String[]{"Error: entry not found."};
		}
	}
	
	protected abstract void addAssociation(String dn, String upperDriverDN, String associationState, String associationValue) throws LDAPException;

	protected abstract void removeAndAddAssociations(String dn, String upperDriverDN, AssociationValue[] driverValues, String associationState, String associationValue) throws LDAPException;

	/**
	 * Returns a bitmask of the association state and value:
	 * 0x01: bit indicating if the association values are equal (set: equal; unset: not equal)
	 * 0x02: bit indicating if the association states are equal (set: equal; unset: not equal)
	 * @param associationState
	 * @param associationValue
	 * @param idvAssociationValue
	 * @return
	 */
	protected int getValidationMask(String associationState, String associationValue, AssociationValueParser.AssociationValue idvAssociationValue) {
		return (associationState.equals(idvAssociationValue.getState())?2:0) | (associationValue.equals(idvAssociationValue.getAssociation())?1:0);
	}

	/**
	 * Get a string representation of the validation of the idv association(value and state) and the imported file association.
	 * @param associationState
	 * @param associationValue
	 * @param idvAssociationValue
	 * @return
	 */
	protected String getValidationMessage(String associationState, String associationValue,
			AssociationValueParser.AssociationValue idvAssociationValue) {
		int validation = getValidationMask(associationState, associationValue, idvAssociationValue);
		switch (validation) {
		case 0:
			return "Warning: overwriting association value and state";
		case 1:
			return "OK: overwriting state";
		case 2:
			return "Warning: overwriting association value";
		case 3:
			return "OK: association state and value equal";
		default:
			return "Error: Internal error";
		}
	}
}
