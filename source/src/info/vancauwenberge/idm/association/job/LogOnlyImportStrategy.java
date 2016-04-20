/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import info.vancauwenberge.idm.association.actions.AssociationValueParser;
import info.vancauwenberge.idm.association.actions.AssociationValueParser.AssociationValue;
import info.vancauwenberge.idm.association.log.LogStrategy;

import com.novell.ldap.LDAPConnection;

public class LogOnlyImportStrategy extends AbstractImportStrategy{

	public LogOnlyImportStrategy(LDAPConnection ldapCon,
			AssociationValueParser valueParser, LogStrategy logStrategy) {
		super(ldapCon, valueParser, logStrategy);
	}

	@Override
	protected void addAssociation(String dn, String upperDriverDN,
			String associationState, String associationValue) {
	}

	@Override
	protected void removeAndAddAssociations(String dn, String upperDriverDN,
			AssociationValue[] driverValues, String associationState,
			String associationValue) {
	}

}
