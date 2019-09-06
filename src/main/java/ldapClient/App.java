package ldapClient;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App {

	private static boolean debug = false;

	private static Options OPTIONS = new Options();

	static {
		OPTIONS.addOption("U", "ldapUrl", true, "URL of the LDAP server, defaults to 'ldap://localhost:1389'");
		OPTIONS.addOption("B", "bindingDN", true, "DN of binding user, defaults to 'cn=Directory Manager'");
		OPTIONS.addOption("P", "bindingPW", true, "Password of binding user");
		OPTIONS.addOption("u", "lookupUser", true, "Name of user to lookup, defaults to '*'");
		OPTIONS.addOption("g", "lookupGroup", true, "Name of group to lookup, defaults to '*'");
		OPTIONS.addOption("U", "userQuery", true,
				"LDAP user query, defaults to: '(&(uid={0})(objectClass=inetOrgPerson))'");
		OPTIONS.addOption("G", "groupQuery", true,
				"LDAP group query, defaults to: '(&(cn={0})(objectClass=groupOfUniqueNames))'");
		OPTIONS.addOption("a", "printAttributes", false, "Print attributes of all users and groups found");
		OPTIONS.addOption("h", "help", false, "Usage doc");
		OPTIONS.addOption("v", "verbose", false, "Debug traces");
	}

	private static void debug(String msg) {
		if (debug)
			System.out.println(msg);
	}

	public static DirContext getRootContext(CommandLine cmd) throws NamingException {
		final Hashtable<String, String> env = new Hashtable<String, String>();

		final String factory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		final String ldapServerUrl = cmd.getOptionValue("U", "ldap://localhost:1389"); // ldapportal.intranet.gc:389
		final String ldapUser = cmd.getOptionValue("B", "cn=Directory Manager"); // cn=SONAR_LDAP,ou=SERVICIOS,o=GC
		final String ldapPasswd = cmd.getOptionValue("P", "password"); // Toledo2019

		debug(Context.PROVIDER_URL + "=" + ldapServerUrl);
		debug(Context.SECURITY_PRINCIPAL + "=" + ldapUser);
		if (debug) {
			System.out.print(Context.SECURITY_CREDENTIALS + "=");
			final int len = (ldapPasswd == null) ? 0 : ldapPasswd.length();
			for (int i = 0; i < len; i++)
				System.out.print("*");
			System.out.println();
		}

		env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
		env.put(Context.SECURITY_PRINCIPAL, ldapUser);
		env.put(Context.PROVIDER_URL, ldapServerUrl);
		env.put(Context.SECURITY_CREDENTIALS, ldapPasswd);

		InitialContext root = new InitialLdapContext(env, null);

		return (DirContext) (root.lookup(""));
	}

	public static List<SearchResult> lookupGroups(DirContext root, CommandLine cmd)
			throws IOException, NamingException {

		List<SearchResult> list = new ArrayList<SearchResult>();

		MessageFormat groupQueryFormat = new MessageFormat(
				cmd.getOptionValue("G", "(&(cn={0})(objectClass=groupOfUniqueNames))"));
		String groupName = cmd.getOptionValue("g", "*");
		Object[] args = { groupName };
		String query = groupQueryFormat.format(args);

		System.out.println("Looking up groups with filter: " + query);

		SearchControls ctrls = new SearchControls();
		ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		final NamingEnumeration<SearchResult> results = root.search("", query, ctrls);
		while (results.hasMore()) {
			final SearchResult r = results.nextElement();
			debug("Found node: " + r.getName());
			list.add(r);
		}
		return list;
	}

	public static List<SearchResult> lookupUsers(DirContext root, CommandLine cmd) throws IOException, NamingException {

		List<SearchResult> list = new ArrayList<SearchResult>();

		MessageFormat userQueryFormat = new MessageFormat(
				cmd.getOptionValue("U", "(&(uid={0})(objectClass=inetOrgPerson))"));
		String userName = cmd.getOptionValue("u", "*");
		Object[] args = { userName };
		String query = userQueryFormat.format(args);

		System.out.println("Looking up users with filter: " + query);

		SearchControls ctrls = new SearchControls();
		ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);

		final NamingEnumeration<SearchResult> results = root.search("", query, ctrls);
		while (results.hasMore()) {
			final SearchResult r = results.nextElement();
			debug("Found node: " + r.getName());
			list.add(r);
		}
		return list;
	}

	public static void main(String[] args) {
		try {
			CommandLine cmd = setupCmdline(args, OPTIONS);
			debug = cmd.hasOption("v");
			if (cmd.hasOption("h")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("LdapClient", "Simple LDAP user and group lookup tool.", OPTIONS, "", true);
				System.exit(0);
			} else {
				final DirContext root = getRootContext(cmd);
				debug("root is: " + root.getNameInNamespace());

				List<SearchResult> l = lookupUsers(root, cmd);
				System.out.println("Number of users found : " + l.size());
				if (cmd.hasOption("a")) {
					printSearchResult(l);
				}

				List<SearchResult> l2 = lookupGroups(root, cmd);
				System.out.println("Number of groups found : " + l2.size());
				if (cmd.hasOption("a")) {
					printSearchResult(l2);
				}
			}
		} catch (Exception x) {
			System.err.println("Unexpected exception caught in main: " + x);
			x.printStackTrace(System.err);
		}
	}

	public static void printSearchResult(List<SearchResult> searchResults) throws NamingException {
		for (SearchResult searchResult : searchResults) {
			System.out.println("  DN = " + searchResult.getName());
			Attributes attrs = searchResult.getAttributes();
			NamingEnumeration<String> ne = attrs.getIDs();
			while (ne.hasMoreElements()) {
				String attrId = ne.next();
				Attribute attr = attrs.get(attrId);
				System.out.println("    " + attrId + " = " + attr.get());
			}
		}
	}

	public static CommandLine setupCmdline(String[] args, Options options) throws ParseException {

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		return cmd;
	}
}