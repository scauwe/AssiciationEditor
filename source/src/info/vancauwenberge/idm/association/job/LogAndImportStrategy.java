/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import info.vancauwenberge.idm.association.actions.AssociationValueParser;
import info.vancauwenberge.idm.association.actions.AssociationValueParser.AssociationValue;
import info.vancauwenberge.idm.association.actions.api.Const;
import info.vancauwenberge.idm.association.log.LogStrategy;

import java.util.ArrayList;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPModification;

public class LogAndImportStrategy extends AbstractImportStrategy{

	public LogAndImportStrategy(LDAPConnection ldapCon,
			AssociationValueParser valueParser, LogStrategy logStrategy) {
		super(ldapCon, valueParser, logStrategy);
	}


	protected void removeAndAddAssociations(String dn, String upperDriverDN, AssociationValue[] driverValues,
			String associationState, String associationValue) throws LDAPException {
		ArrayList<LDAPModification> modList = new ArrayList<LDAPModification>();
		//Remove the old values
		for (int i = 0; i < driverValues.length; i++) {
			AssociationValue anAssociationValue = driverValues[i];
			LDAPAttribute attribute= new LDAPAttribute( Const.ATTR_DIR_XML_ASSOCIATIONS, anAssociationValue.getLDAPValue());
			modList.add(new LDAPModification(LDAPModification.DELETE, attribute));
		}
		//Add an association to a user
		LDAPAttribute attribute= new LDAPAttribute( Const.ATTR_DIR_XML_ASSOCIATIONS, upperDriverDN+"#"+associationState+"#"+associationValue);
		modList.add(new LDAPModification(LDAPModification.ADD, attribute));
		ldapCon.modify(dn, modList.toArray(new LDAPModification[modList.size()]));
		
	}

	protected void addAssociation(String objectDN, String upperDriverDN,
			String associationState, String associationValue) throws LDAPException {
		//Add an association to a user
		LDAPAttribute attribute= new LDAPAttribute(Const.ATTR_DIR_XML_ASSOCIATIONS, upperDriverDN+"#"+associationState+"#"+associationValue);
		ldapCon.modify(objectDN, new LDAPModification(LDAPModification.ADD, attribute));
	}

}
