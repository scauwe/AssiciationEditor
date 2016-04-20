/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import java.util.LinkedList;

import info.vancauwenberge.idm.association.Activator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

class JobResultData{
	private long objectCount = 0;
	private long associationCount = 0;
	private boolean encounteredError = false;
	private Status OK = new Status(Status.OK, Activator.PLUGIN_ID, "Finished with success");
	private Status ERROR = new Status(Status.ERROR, Activator.PLUGIN_ID, "Finished with errors");
	private boolean cancled = false;
	private LinkedList<String> messages = new LinkedList<String>();
	//private Throwable exception;

	public void incrementObjectCount(){
		objectCount++;
	}
	
	public void incrementAssociationCount(){
		associationCount++;
	}
	
	public void setEncounteredError(boolean value){
		this.encounteredError = value;
	}
	
	public long getObjectCount() {
		return objectCount;
	}
	public long getAssociationCount() {
		return associationCount;
	}
	public boolean isEncounteredError() {
		return encounteredError;
	}
	public IStatus getStatus(){
		return cancled?Status.CANCEL_STATUS:(encounteredError?ERROR:OK);
	}

	public void setCancled() {
		this.cancled  = true;
	}
	
	public void pushMessage(String message){
		messages.add(message);
	}
	/*
	public void setThrowable(Throwable th){
		this.exception = th;
	}*/
	
}