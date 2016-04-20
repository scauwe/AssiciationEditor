/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class LogStrategy {
	private static class CSVLogStrategy extends LogStrategy {

		private BufferedWriter bos;

		public CSVLogStrategy(String file) throws IOException {
			File f = new File(file);
			if (f.exists())
				f.delete();
			f.createNewFile();
			FileWriter fos = new FileWriter(f);
			bos = new BufferedWriter(fos);
		}

		@Override
		public void log(String objectDN, String associationState,
				String associationValue, String[] validationResult) throws IOException {
			writeField(objectDN);
			bos.write('\t');
			//writeField(driverDN);
			//bos.write('\t');
			writeField(associationState);
			bos.write('\t');
			writeField(associationValue);
			bos.write('\t');
			writeField(validationResult);
			bos.write('\n');
		}

		@Override
		public void log(String objectDN, String associationState,
				String associationValue, String validationResult) throws IOException {
			writeField(objectDN);
			bos.write('\t');
			//writeField(driverDN);
			//bos.write('\t');
			writeField(associationState);
			bos.write('\t');
			writeField(associationValue);
			bos.write('\t');
			writeField(validationResult);
			bos.write('\n');
		}
		
		private void writeField(String[] validationResult) throws IOException {
			if (validationResult != null && validationResult.length>0){
				String nl = "";
				bos.write('"');
				for (int i = 0; i < validationResult.length; i++) {
					bos.write(nl);
					bos.write(validationResult[i].replaceAll("\"", "\"\""));
					nl = "\n";
				}
				bos.write('"');
			}
		}

		private void writeField(String objectDN) throws IOException {
			if (objectDN != null){
				bos.write('"');
				bos.write(objectDN.replaceAll("\"", "\"\""));
				bos.write('"');
			}
		}
		@Override
		public void close() throws IOException {
			bos.close();
		}

	}

	private static  class EmptyLogStrategy extends LogStrategy {

		@Override
		public void log(String objectDN, String associationState,
				String associationValue, String[] validationResult) {
		}

		@Override
		public void close() {
		}

		@Override
		public void log(String objectDN, String associationState,
				String associationValue, String validationResult)
				throws IOException {
		}

	}

	public static LogStrategy getLogStrategy(String file) throws IOException{
		if (file==null || "".equals(file)){
			return new EmptyLogStrategy();
		}
		return new CSVLogStrategy(file);
	}
	
	public abstract void log(String objectDN, String associationState, String associationValue, String[] validationResult) throws IOException ;
	public abstract void log(String objectDN, String associationState, String associationValue, String validationResult) throws IOException ;
	public abstract void close() throws IOException ;
}
