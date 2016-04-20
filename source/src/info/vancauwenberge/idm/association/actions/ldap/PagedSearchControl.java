/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions.ldap;

import java.io.IOException;

import com.novell.ldap.LDAPControl;
import com.sun.jndi.ldap.Ber;
import com.sun.jndi.ldap.BerEncoder;

public class PagedSearchControl extends LDAPControl {
    /**
     * The paged-results control's assigned object identifier
     * is 1.2.840.113556.1.4.319.
     */
    public static final String OID = "1.2.840.113556.1.4.319";
    //The initial request is with an empty cookie.
    private static final byte[] EMPTY_COOKIE = new byte[0];
    
    
    
    public PagedSearchControl(int pageSize, boolean criticality)
            throws IOException {

        super(OID, criticality, getEncodedValue(pageSize));
    }
    
    
    public PagedSearchControl(int pageSize, byte[] cookie,
            boolean criticality) throws IOException {
            super(OID, criticality, getEncodedValue(pageSize, cookie==null?EMPTY_COOKIE:cookie));
        }

    
    
    @SuppressWarnings("restriction")
	private static byte[] getEncodedValue(int pageSize, byte[] cookie)
            throws IOException {

            // build the ASN.1 encoding
            BerEncoder ber = new BerEncoder(10 + cookie.length);

            ber.beginSeq(Ber.ASN_SEQUENCE | Ber.ASN_CONSTRUCTOR);
                ber.encodeInt(pageSize);
                ber.encodeOctetString(cookie, Ber.ASN_OCTET_STR);
            ber.endSeq();

            return ber.getTrimmedBuf();
        }

    private static byte[] getEncodedValue(int pageSize)
            throws IOException {
    	return getEncodedValue(pageSize, EMPTY_COOKIE);
        }


}
