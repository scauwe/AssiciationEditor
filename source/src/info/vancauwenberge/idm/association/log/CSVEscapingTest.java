package info.vancauwenberge.idm.association.log;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import au.com.bytecode.opencsv.CSVReader;

public class CSVEscapingTest {
	
	private static StringWriter bos;

	public static void main(String args[]){
		bos = new StringWriter();
		try {
			writeField("with \"quote");
			bos.write('\t');
			writeField("with \\slash");
			bos.write('\n');
			writeField("with \" quote");
			bos.write('\t');
			writeField("with \nnewline");
			bos.write('\n');
			writeField("with \" quote and \\ slash");
			bos.write('\t');
			writeField("with \nnewline and \\ slash");
			bos.write('\n');
			bos.close();
			String s = bos.toString();
			System.out.println(s);
			StringReader sr = new StringReader(s);
			CSVReader csvReader = new CSVReader(sr,'\t', '"', '\u0000' );
			String [] nextLine;
			while ((nextLine = csvReader.readNext()) != null ) {
				for (int i = 0; i < nextLine.length; i++) {
					System.out.println(i+":"+nextLine[i]);
				}
				System.out.println("");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void writeField(String objectDN) throws IOException {
		if (objectDN != null){
			bos.write('"');
			bos.write(objectDN.replaceAll("\"", "\"\""));
			bos.write('"');
		}
	}
}
