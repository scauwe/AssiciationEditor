/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions;

public class AssociationValueParser {
	public class AssociationValue{
		private AssociationValue(String driverDN, String state,
				String association) {
			super();
			this.driverDN = driverDN;
			this.state = state;
			this.association = association;
		}
		private String driverDN;
		private String state;
		private String association;
		public String getDriverDN() {
			return driverDN;
		}
		public String getState() {
			return state;
		}
		public String getAssociation() {
			return association;
		}
		public String getLDAPValue(){
			return driverDN+"#"+state+"#"+association;
		}
		
	}
	private String uppderDriverDN;
	private int driverLength;

	public AssociationValueParser(String driverDN){
		this.uppderDriverDN = driverDN.toUpperCase();
		this.driverLength = uppderDriverDN.length() + 1;
	}

	public AssociationValue parse(String driverValue){
		if (driverValue == null)
			return null;
		String driverDN = driverValue.substring(0,driverLength-1);
		String state = driverValue.substring(driverLength);
		state = state.substring(0,state.indexOf("#"));
		String association = driverValue.substring(driverLength);
		association = association.substring(association.indexOf('#')+1);
		return new AssociationValue(driverDN, state, association);
	}

	public String getUpperDriverDN() {
		return uppderDriverDN;
	}
}
