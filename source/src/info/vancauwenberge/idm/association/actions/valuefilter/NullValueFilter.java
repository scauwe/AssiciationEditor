/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions.valuefilter;

import info.vancauwenberge.idm.association.actions.AssociationValueParser;
import info.vancauwenberge.idm.association.actions.api.IValueFilter;

import java.util.Enumeration;

public class NullValueFilter implements IValueFilter{

	@Override
	public AssociationValueParser.AssociationValue[] getValue(Enumeration<String> values, AssociationValueParser driverDN) {
		return null;
	}

}
