/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.job;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.novell.idm.model.IdentityVault;

import info.vancauwenberge.idm.association.dialog.AssociationDialog.FromAssociationState;
import info.vancauwenberge.idm.association.dialog.AssociationDialog.SearchScope;

public class ExportProcessingJob extends AbstractLDAPSearchProcessingJob {

	private BufferedWriter bos;

	public ExportProcessingJob(
			final IdentityVault vault, final String driverLDAPDN, final String filter,
			final FromAssociationState fromState, final String searchRootLDAP, final SearchScope searchScope, final String fileName) throws IOException {
		super("Export associations", vault, driverLDAPDN, filter, fromState, searchRootLDAP, searchScope);
		final File f = new File(fileName);
		if (f.exists()) {
			f.delete();
		}
		f.createNewFile();
		final OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(f),Charset.forName("UTF-8"));
		bos = new BufferedWriter(osw);
		addJobLogMessage("  Filter:"+filter);
		addJobLogMessage("  Driver:"+driverLDAPDN);
		addJobLogMessage("  Search base:"+searchRootLDAP);
		addJobLogMessage("  Search scope:"+searchScope);
		addJobLogMessage("  From state:"+fromState);
		addJobLogMessage("Job details:");
	}

	@Override
	public void processEntry(final String objectDN, final String driverDN, final String state,
			final String association) throws Exception{
		writeField(objectDN);
		bos.write('\t');
		//writeField(driverDN);
		//bos.write('\t');
		writeField(state);
		bos.write('\t');
		writeField(association);
		bos.write('\n');
	}


	private void writeField(final String objectDN) throws IOException {
		if (objectDN != null){
			bos.write('"');
			//Escape the quote by doubling it.
			bos.write(objectDN.replaceAll("\"", "\"\""));
			bos.write('"');
		}
	}

	@Override
	public void doneUpdate() throws Exception {
		bos.close();
		bos = null;		
	}

}
