/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions.api;

import info.vancauwenberge.idm.association.actions.AssociationValueParser;

import java.util.Enumeration;

public interface IValueFilter {
	public AssociationValueParser.AssociationValue[] getValue(Enumeration<String> values, AssociationValueParser driverDN);
}
