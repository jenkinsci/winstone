/*
 * Copyright 2006 Rui Damas <rui.damas at gmail com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package contrib.winstone;

import java.sql.*;
import java.util.*;

import winstone.*;

/**
 * A JDBC authentication realm to be used with Winstone Servelet container.
 * <p>
 *  --JDBCRealm.url and --JDBCRealm.user are required.
 * </p>
 *
 * @author Rui Damas
 */
public class JDBCRealm implements AuthenticationRealm {

	// Command line arguments prefix
	public static String ARGS = "JDBCRealm.";

	// Command line arguments for connecting
	public static String
		ARGS_DRIVER = ARGS + "driver",
		ARGS_URL = ARGS + "url",
		ARGS_USER = ARGS + "user",
		ARGS_PASSWORD = ARGS + "password";

	// Command line arguments to SQL identifiers
	public static String
		ARGS_USER_REL = ARGS + "userRel",
		ARGS_USER_NAME_COL = ARGS + "userNameCol",
		ARGS_USER_CRED_COL = ARGS + "userCredCol",
		ARGS_USER_ROLE_REL = ARGS + "userRoleRel",
		ARGS_ROLE_NAME_COL = ARGS + "roleNameCol";

	// Defaults for SQL identifiers
	public static String
		DEFAULT_USER_REL = "web_users",
		DEFAULT_USER_NAME_COL = "username",
		DEFAULT_USER_CRED_COL ="credential",
		DEFAULT_USER_ROLE_REL = "web_user_roles",
		DEFAULT_ROLE_NAME_COL = "rolename";

	private Connection connection;

	private final String url, user, password,
		retriveUserQuery, authenticationQueryPostfix, userRolesQuery;

	/**
	 Creates a new instance of JDBCAuthenticationRealm.
	 <p>
		If a <code>"JDBCRealm.driver"</code> exists in the <code>args</code>
	  map an atempt to load the class will be made and
	  a success message will be printed to <code>System.out</code>,
	  or, if the class fails to load,
	  an error message will be printed to <code>System.err</code>.
	 </p>
	 */
	public JDBCRealm(Set rolesAllowed, Map<String, String> args) {
		// Get connection arguments
		String driver = args.get(ARGS_DRIVER),
			url = args.get(ARGS_URL),
			user = args.get(ARGS_USER),
			password = args.get(ARGS_PASSWORD);

		this.url = url;
		this.user = user;
		this.password = password;
		
		// Get SQL identifier arguments
		String userRel = args.get(ARGS_USER_REL),
			userNameCol = args.get(ARGS_USER_NAME_COL),
			userCredCol = args.get(ARGS_USER_CRED_COL),
			userRoleRel = args.get(ARGS_USER_ROLE_REL),
			roleNameCol = args.get(ARGS_ROLE_NAME_COL);
		
		// Get defaults if necessary
		if (userRel == null) userRel = DEFAULT_USER_REL;
		if (userNameCol == null) userNameCol = DEFAULT_USER_NAME_COL;
		if (userCredCol == null) userCredCol = DEFAULT_USER_CRED_COL;
		if (userRoleRel == null) userRoleRel = DEFAULT_USER_ROLE_REL;
		if (roleNameCol == null) roleNameCol = DEFAULT_ROLE_NAME_COL;
		
		retriveUserQuery =
			"SELECT 1\n" +
			"  FROM \"" + userRel + "\"\n" +
			"  WHERE \"" + userNameCol + "\" = ?";

		// Prepare query prefixes
		authenticationQueryPostfix =
			"\n    AND \"" + userCredCol + "\" = ?";

		userRolesQuery =
			"SELECT \"" + roleNameCol + "\"\n" +
			"  FROM \"" + userRoleRel + "\"\n" +
			"  WHERE \"" + userNameCol + "\" = ?";

		// If the driver was specified
		if (driver != null)
			try {
				// Try to load the driver
				Class.forName(driver);
				// and notify if loaded
				System.out.println("JDBCRealm loaded jdbc driver: " + driver);}
			catch (ClassNotFoundException cnfe) {
				// Notify if fails
				System.err.println(
					"JDBCRealm failed to load jdbc driver: "+ driver);}
	}

	public AuthenticationPrincipal getPrincipal
		(String userName, String password, boolean usePassword) {
			try {
				// Get a connection
				if ((connection == null) || connection.isClosed())
					connection = DriverManager.getConnection(url, user, password);
				// Query for user
				String query = retriveUserQuery;
				if (usePassword) query = query + authenticationQueryPostfix;
				PreparedStatement ps = connection.prepareStatement(query);
				ps.setString(1, userName);
				if (usePassword) ps.setString(2, password);
				ResultSet resultSet = ps.executeQuery();
				// If there is a user (row)
				if (resultSet.next()) {
					// Query for the user roles
					query = userRolesQuery;
					ps = connection.prepareStatement(query);
					ps.setString(1, userName);
					resultSet = ps.executeQuery();
					// Load list
					List<String> roles = new Vector<String>();
					while (resultSet.next())
						roles.add(resultSet.getString(1));
					return new AuthenticationPrincipal(userName, password, roles);
				}
			}
			catch (SQLException sqle) {sqle.printStackTrace();}
			return null;
	}

	/**
	 * Authenticate the user - do we know them ? Return a distinct id once we
	 * know them.
	 * @return <code>getPrincipal(userName, password, true);</code>
	 */
	public AuthenticationPrincipal authenticateByUsernamePassword
		(String userName, String password) {
			return getPrincipal(userName, password, true);
	}

	/**
	 * Retrieve an authenticated user
	 * @return <code>getPrincipal(userName, password, false);</code>
	 */
	public AuthenticationPrincipal retrieveUser(String userName) {
			return getPrincipal(userName, null, false);
	}
}
