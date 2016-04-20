/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions.api;

public interface ITableCallback {
	public void updateTableData(String userDN, String associationDriver, String associationState, String associationValue, String[] comment);
	public void doneUpdate(boolean exception, final long records, final long associations);
}
