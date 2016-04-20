/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import info.vancauwenberge.idm.association.Activator;
import info.vancauwenberge.idm.association.actions.AssociationValueParser;
import info.vancauwenberge.idm.association.log.LogStrategy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import au.com.bytecode.opencsv.CSVReader;

import com.novell.idm.model.IdentityVault;
import com.novell.ldap.LDAPConnection;

public class ImportAssociationsJob extends AbstarctLDAPJob  {

	private CSVReader csvReader;
	private LogStrategy logStrategy;
	private boolean running = true;
	private String driverLDAPDN;
	private boolean isTestOnly;

	public ImportAssociationsJob(
			IdentityVault vault, String driverLDAPDN, String fileName, boolean isTestOnly, String logFile) throws IOException {
		super("Import Associations", vault);
		File f = new File(fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(f),Charset.forName("UTF-8"));
		//in opencsv, the escape character cannot be the same as the quote character. Null characters should never be in a CSV file, so we use that.
		//opencsv will accept double quote characters as an escaped quote character, so we actually do not need an escape character...
		//but the parameter must be defined.
		csvReader = new CSVReader(new BufferedReader(isr),'\t', '"', '\u0000' );
		logStrategy = LogStrategy.getLogStrategy(logFile);
		this.driverLDAPDN = driverLDAPDN;
		this.isTestOnly = isTestOnly;
		
        addJobLogMessage("  Driver:"+driverLDAPDN);
        addJobLogMessage("  File:"+fileName);
        addJobLogMessage("  Test:"+isTestOnly);
        addJobLogMessage("Job details:");
	}

	
	@Override
	public void canceling() {
		running = false;
	}

	
	protected void doJob() {
		monitor.beginTask("Importing associations", 2);
		addJobLogMessage("Importing associations");		
		addJobLogMessage("Getting ldap connection");		
		final LDAPConnection ldapCon = getLDAPConnection();
		monitor.worked(1);

		final AbstractImportStrategy importAction;
		if (isTestOnly)
			importAction = new LogOnlyImportStrategy(ldapCon, new AssociationValueParser(driverLDAPDN),logStrategy);
		else
			importAction = new LogAndImportStrategy(ldapCon, new AssociationValueParser(driverLDAPDN),logStrategy);
		
		//Start processing
		monitor.subTask("Importing associations");
		UpdateStatusThread th = new UpdateStatusThread(this, "Imported {0,number} lines.");
		th.start();
		String [] nextLine;
	    try {
			while ((nextLine = csvReader.readNext()) != null && running ) {
				if (nextLine.length == 3){
					String objectDN = nextLine[0];
					String associationState = nextLine[1];
					String associationValue = nextLine[2];
					importAction.processEntry(objectDN, associationState, associationValue);
					incrementObjectCount();
					incrementAssociationCount();
				}else{
					//TODO: set job status to WARNING
					addJobLogMessage("Invalid number of elements in CSV record");
					Activator.log("Invalid number of elements in CSV record");
				}
			}
		} catch (Exception e) {
			Activator.log("Error importing associations from file.",e);
		} finally{
			th.stopThread();
			try {
				logStrategy.close();
			} catch (Throwable e) {
			}
			
			if (csvReader != null)
				try {
					csvReader.close();
				} catch (Throwable e) {
				}
			if (ldapCon!=null)
				try {
					ldapCon.disconnect();
				} catch (Throwable e) {
				}
			
			csvReader = null;
			monitor.done();
		}
	}

}
