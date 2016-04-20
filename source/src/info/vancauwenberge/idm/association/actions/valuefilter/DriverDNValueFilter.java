/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions.valuefilter;

import info.vancauwenberge.idm.association.actions.AssociationValueParser;
import info.vancauwenberge.idm.association.actions.AssociationValueParser.AssociationValue;
import info.vancauwenberge.idm.association.actions.api.IValueFilter;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.FromAssociationState;

import java.util.ArrayList;
import java.util.Enumeration;

public class DriverDNValueFilter implements IValueFilter{
	
	private FromAssociationState fromState;
	
	
	@Override
	public AssociationValueParser.AssociationValue[] getValue(Enumeration<String> values, AssociationValueParser valueParser) {
		ArrayList<AssociationValueParser.AssociationValue> l = new ArrayList<AssociationValueParser.AssociationValue>();
    	if( values != null) {
    		while(values.hasMoreElements()) {
    			final String Value = (String) values.nextElement();
    			if (Value.toUpperCase().startsWith(valueParser.getUpperDriverDN())){
    				AssociationValue aValue = valueParser.parse(Value);
    				if (aValue != null){
    					switch (fromState) {
						case ANY_ASSOCIATION://Always add
							l.add(aValue);
							break;
						default://Only add when it has the correct state
							String state = Long.toString(fromState.getState());
							if (state.equals(aValue.getState()))
								l.add(aValue);
							break;
						}
    				}
    			}
    		}
        }
    	return l.toArray(new AssociationValueParser.AssociationValue[l.size()]);
	}


	public void setAssociationState(FromAssociationState fromAssociationState) {
		this.fromState = fromAssociationState;
		
	}

}
