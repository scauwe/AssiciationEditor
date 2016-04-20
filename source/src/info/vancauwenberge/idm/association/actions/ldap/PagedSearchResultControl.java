/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association.actions.ldap;

import java.io.IOException;

import com.novell.ldap.LDAPControl;
import com.sun.jndi.ldap.Ber;
import com.sun.jndi.ldap.BerDecoder;

public class PagedSearchResultControl {

    /**
     * An estimate of the number of entries in the search result.
     *
     * @serial
     */
    private int resultSize;

    /**
     * A server-generated cookie.
     *
     * @serial
     */
    private byte[] cookie;

    /**
     * Constructs a paged-results response control.
     *
     * @param   id              The control's object identifier string.
     * @param   criticality     The control's criticality.
     * @param   value           The control's ASN.1 BER encoded value.
     *                          It is not cloned - any changes to value
     *                          will affect the contents of the control.
     * @exception IOException   If an error was encountered while decoding
     *                          the control's value.
     */
    @SuppressWarnings("restriction")
	public PagedSearchResultControl(LDAPControl control) throws IOException {

    	byte[] value = control.getValue();
        // decode value
        BerDecoder ber = new BerDecoder(value, 0, value.length);

        ber.parseSeq(null);
        resultSize = ber.parseInt();
        cookie = ber.parseOctetString(Ber.ASN_OCTET_STR, null);
    }

    /**
     * Retrieves (an estimate of) the number of entries in the search result.
     *
     * @return The number of entries in the search result, or zero if unknown.
     */
    public int getResultSize() {
        return resultSize;
    }

    /**
     * Retrieves the server-generated cookie. Null is returned when there are
     * no more entries for the server to return.
     *
     * @return A possibly null server-generated cookie. It is not cloned - any
     *         changes to the cookie will update the control's state and thus
     *         are not recommended.
     */
    public byte[] getCookie() {
        if (cookie.length == 0) {
            return null;
        } else {
            return cookie;
        }
    }
}
