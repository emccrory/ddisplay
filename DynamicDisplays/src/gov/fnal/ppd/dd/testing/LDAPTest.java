package gov.fnal.ppd.dd.testing;

//import java.util.Hashtable;
//
//import javax.naming.Context;
//import javax.naming.NamingEnumeration;
//import javax.naming.NamingException;
//import javax.naming.directory.DirContext;
//import javax.naming.directory.InitialDirContext;
//import javax.naming.directory.SearchControls;
//import javax.naming.directory.SearchResult;

/**
 * Example code for retrieving a Users Primary Group from Microsoft Active Directory via its LDAP API
 * 
 * Code removed
 * 
 * @author Adam Retter <adam.retter@googlemail.com>
 * @deprecated
 */
public class LDAPTest {
//
//	/**
//	 * @param args
//	 *            the command line arguments
//	 * @throws NamingException 
//	 */
//	public static void main(String[] args) throws NamingException {
//
//		// From my working PHP code:
//		
//		// Active Directory server
//		// $ldap_host = "ldaps://services.fnal.gov";
//
//		// Active Directory DN
//		// $ldap_dn = "ou=FermiUsers,dc=services,dc=fnal,dc=gov ";
//
//		// Active Directory user group
//		// $ldap_user_group = "services-ad-www-inst";
//
//		// Active Directory manager group
//		// $ldap_manager_group = "services-ad-net-mgmt";
//
//		// Domain, for purposes of constructing $user
//		// $ldap_usr_dom = "@services.fnal.gov";
//
//		final String ldapAdServer = "ldaps://services.fnal.gov";
//		final String ldapSearchBase = "ou=FermiUsers,dc=services,dc=fnal,dc=gov";
//
//		final String ldapUsername = "mccrory";
//		final String ldapPassword = "myLdapPassword";
//
//		final String ldapAccountToLookup = "myOtherLdapUsername";
//
//		Hashtable<String, Object> env = new Hashtable<String, Object>();
//		env.put(Context.SECURITY_AUTHENTICATION, "simple");
//		env.put(Context.SECURITY_PRINCIPAL, ldapUsername);
//		env.put(Context.SECURITY_CREDENTIALS, ldapPassword);
//		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
//		env.put(Context.PROVIDER_URL, ldapAdServer);
//
//		// ensures that objectSID attribute values will be returned as a byte[] instead of a String
//		env.put("java.naming.ldap.attributes.binary", "objectSID");
//
//		// the following is helpful in debugging errors
//		env.put("com.sun.jndi.ldap.trace.ber", System.err);
//
//		System.out.println("Getting context ...");
//        DirContext ctx = new InitialDirContext(env);
//		// LdapContext ctx = new InitialLdapContext();
//		System.out.println("GOT context: " + ctx);
//
//		LDAPTest ldap = new LDAPTest();
//		System.out.println("GOT ldap: " + ldap);
//
//		// 1) lookup the ldap account
//		SearchResult srLdapUser = ldap.findAccountByAccountName(ctx, ldapSearchBase, ldapAccountToLookup);
//		System.out.println("GOT search results: " + srLdapUser);
//
//		// 2) get the SID of the users primary group
//		String primaryGroupSID = ldap.getPrimaryGroupSID(srLdapUser);
//		System.out.println("primaryGroupSID = " + primaryGroupSID);
//
//		// 3) get the users Primary Group
//		String primaryGroupName = ldap.findGroupBySID(ctx, ldapSearchBase, primaryGroupSID);
//		System.out.println("primaryGroupName = " + primaryGroupName);
//
//	}
//
//	/**
//	 * @param ctx
//	 * @param ldapSearchBase
//	 * @param accountName
//	 * @return the Search Result
//	 * @throws NamingException
//	 */
//	public SearchResult findAccountByAccountName(final DirContext ctx, final String ldapSearchBase, final String accountName) throws NamingException {
//
//		String searchFilter = "(&(objectClass=user)(sAMAccountName=" + accountName + "))";
//
//		SearchControls searchControls = new SearchControls();
//		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
//
//		NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase, searchFilter, searchControls);
//
//		SearchResult searchResult = null;
//		if (results.hasMoreElements()) {
//			searchResult = results.nextElement();
//
//			// make sure there is not another item available, there should be only 1 match
//			if (results.hasMoreElements()) {
//				System.err.println("Matched multiple users for the accountName: " + accountName);
//				return null;
//			}
//		}
//
//		return searchResult;
//	}
//
//	/**
//	 * @param ctx
//	 * @param ldapSearchBase
//	 * @param sid
//	 * @return the string
//	 * @throws NamingException
//	 */
//	public String findGroupBySID(DirContext ctx, String ldapSearchBase, String sid) throws NamingException {
//
//		String searchFilter = "(&(objectClass=group)(objectSid=" + sid + "))";
//
//		SearchControls searchControls = new SearchControls();
//		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
//
//		NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase, searchFilter, searchControls);
//
//		if (results.hasMoreElements()) {
//			SearchResult searchResult = results.nextElement();
//
//			// make sure there is not another item available, there should be only 1 match
//			if (results.hasMoreElements()) {
//				System.err.println("Matched multiple groups for the group with SID: " + sid);
//				return null;
//			}
//			return (String) searchResult.getAttributes().get("sAMAccountName").get();
//		}
//		return null;
//	}
//
//	/**
//	 * @param srLdapUser
//	 * @return the string
//	 * @throws NamingException
//	 */
//	public String getPrimaryGroupSID(final SearchResult srLdapUser) throws NamingException {
//		byte[] objectSID = (byte[]) srLdapUser.getAttributes().get("objectSid").get();
//		String strPrimaryGroupID = (String) srLdapUser.getAttributes().get("primaryGroupID").get();
//
//		String strObjectSid = decodeSID(objectSID);
//
//		return strObjectSid.substring(0, strObjectSid.lastIndexOf('-') + 1) + strPrimaryGroupID;
//	}
//
//	/**
//	 * The binary data is in the form: byte[0] - revision level byte[1] - count of sub-authorities byte[2-7] - 48 bit authority
//	 * (big-endian) and then count x 32 bit sub authorities (little-endian)
//	 * 
//	 * The String value is: S-Revision-Authority-SubAuthority[n]...
//	 * 
//	 * Based on code from here - http://forums.oracle.com/forums/thread.jspa?threadID=1155740&tstart=0
//	 * @param sid 
//	 * @return the string
//	 */
//	public static String decodeSID(byte[] sid) {
//
//		final StringBuilder strSid = new StringBuilder("S-");
//
//		// get version
//		final int revision = sid[0];
//		strSid.append(Integer.toString(revision));
//
//		// next byte is the count of sub-authorities
//		final int countSubAuths = sid[1] & 0xFF;
//
//		// get the authority
//		long authority = 0;
//		// String rid = "";
//		for (int i = 2; i <= 7; i++) {
//			authority |= ((long) sid[i]) << (8 * (5 - (i - 2)));
//		}
//		strSid.append("-");
//		strSid.append(Long.toHexString(authority));
//
//		// iterate all the sub-auths
//		int offset = 8;
//		int size = 4; // 4 bytes for each sub auth
//		for (int j = 0; j < countSubAuths; j++) {
//			long subAuthority = 0;
//			for (int k = 0; k < size; k++) {
//				subAuthority |= (long) (sid[offset + k] & 0xFF) << (8 * k);
//			}
//
//			strSid.append("-");
//			strSid.append(subAuthority);
//
//			offset += size;
//		}
//
//		return strSid.toString();
//	}
}