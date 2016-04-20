/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;

public class UpdateStatusThread extends Thread {
	private MessageFormat formatter;

	public UpdateStatusThread(AbstarctLDAPJob job, String pattern) {
		super("AssociationEditorUpdateStatusThread");
		this.job = job;
		this.formatter = new MessageFormat(pattern);
	}

	private AbstarctLDAPJob job = null;
	private boolean running = true;
	
	public void run(){
		do{
			//Update the monitor every second
			long count = job.getObjectCount();
			if (count > 0){
				IProgressMonitor monitor = job.getMonitor();
				if (monitor != null){
					Object[] messageArguments = {new Long(count)};
					String output = formatter.format(messageArguments);
					monitor.subTask(output);
				}
			}
			try {
				//Sleep a second
				sleep(1000);
			} catch (InterruptedException e) {
			}
		}while (running);
	}
	
	public void stopThread(){
		running = false;
		this.interrupt();
	}

}
