/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions;

import info.vancauwenberge.idm.association.job.AbstractLDAPSearchProcessingJob;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class Util {

	public static boolean syncOpenConfirm(final String title, final String message){
		final AbstractLDAPSearchProcessingJob.BooleanResultWrapper result = new AbstractLDAPSearchProcessingJob.BooleanResultWrapper();
		
		Display.getDefault().syncExec(new Runnable() {
		    public void run() {
				result.setResult(MessageDialog.openConfirm(
						PlatformUI.getWorkbench().getDisplay().getActiveShell()
		                , title, message));
		    }
		});
		return result.getResult();
	}

}
