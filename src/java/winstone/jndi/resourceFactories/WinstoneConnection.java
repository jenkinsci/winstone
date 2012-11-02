/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.jndi.resourceFactories;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import winstone.Logger;

/**
 * JDBC Connection wrapper for use in the pooling datasource. This just suppresses 
 * the close() call, and releases the connection.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneConnection.java,v 1.3 2006/02/28 07:32:48 rickknowles Exp $
 */
public class WinstoneConnection implements Connection {
    private Connection realConnection;
    private WinstoneDataSource datasource;

    /**
     * Constructor - this sets the real connection and the link back to the pool
     */
    public WinstoneConnection(Connection connection,
            WinstoneDataSource datasource) {
        this.realConnection = connection;
        this.datasource = datasource;
    }

    public void close() throws SQLException {
        if ((this.datasource != null) && (this.datasource.getLogWriter() != null)) {
            this.datasource.getLogWriter().println(
                    WinstoneDataSource.DS_RESOURCES.getString(
                            "WinstoneConnection.ReleaseRollback"));
        } else {
            Logger.log(Logger.FULL_DEBUG, WinstoneDataSource.DS_RESOURCES,
                    "WinstoneConnection.ReleaseRollback");
        }
        
        Connection realConnectionHolder = null;
        try {
            if (this.realConnection != null) {
                realConnectionHolder = this.realConnection;
                this.realConnection = null;
                
                if (!realConnectionHolder.getAutoCommit())
                    realConnectionHolder.rollback();
            }
        } finally {
            if ((this.datasource != null) && (realConnectionHolder != null)) {
                this.datasource.releaseConnection(this, realConnectionHolder);
                this.datasource = null;
            }
        }
    }

    public boolean isClosed() throws SQLException {
        return (this.realConnection == null);
    }

    public void commit() throws SQLException {
        this.realConnection.commit();
    }

    public void rollback() throws SQLException {
        this.realConnection.rollback();
    }

    public void rollback(Savepoint sp) throws SQLException {
        this.realConnection.rollback(sp);
    }

    public boolean getAutoCommit() throws SQLException {
        return this.realConnection.getAutoCommit();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.realConnection.setAutoCommit(autoCommit);
    }

    public int getHoldability() throws SQLException {
        return this.realConnection.getHoldability();
    }

    public void setHoldability(int hold) throws SQLException {
        this.realConnection.setHoldability(hold);
    }

    public int getTransactionIsolation() throws SQLException {
        return this.realConnection.getTransactionIsolation();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        this.realConnection.setTransactionIsolation(level);
    }

    public void clearWarnings() throws SQLException {
        this.realConnection.clearWarnings();
    }

    public SQLWarning getWarnings() throws SQLException {
        return this.realConnection.getWarnings();
    }

    public boolean isReadOnly() throws SQLException {
        return this.realConnection.isReadOnly();
    }

    public void setReadOnly(boolean ro) throws SQLException {
        this.realConnection.setReadOnly(ro);
    }

    public String getCatalog() throws SQLException {
        return this.realConnection.getCatalog();
    }

    public void setCatalog(String catalog) throws SQLException {
        this.realConnection.setCatalog(catalog);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return this.realConnection.getMetaData();
    }

    public Savepoint setSavepoint() throws SQLException {
        return this.realConnection.setSavepoint();
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return this.realConnection.setSavepoint(name);
    }

    public void releaseSavepoint(Savepoint sp) throws SQLException {
        this.realConnection.releaseSavepoint(sp);
    }

    public Map getTypeMap() throws SQLException {
        return this.realConnection.getTypeMap();
    }

    public String nativeSQL(String sql) throws SQLException {
        return this.realConnection.nativeSQL(sql);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return this.realConnection.prepareCall(sql);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return this.realConnection.prepareCall(sql, resultSetType,
                resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return this.realConnection.prepareCall(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability);
    }

    public Statement createStatement() throws SQLException {
        return this.realConnection.createStatement();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return this.realConnection.createStatement(resultSetType,
                resultSetConcurrency);
    }

    public Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return this.realConnection.createStatement(resultSetType,
                resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return this.realConnection.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql, int autogeneratedKeys)
            throws SQLException {
        return this.realConnection.prepareStatement(sql, autogeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return this.realConnection.prepareStatement(sql, resultSetType,
                resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return this.realConnection.prepareStatement(sql, resultSetType,
                resultSetConcurrency, resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        return this.realConnection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return this.realConnection.prepareStatement(sql, columnNames);
    }
    
    public Array createArrayOf(String typeName, Object[] elements) 
    		throws SQLException
    {
    	return this.realConnection.createArrayOf (typeName, elements);
    }    
    
    public Clob createClob() throws SQLException
    {
    	return this.realConnection.createClob ();
    }


    public Blob createBlob() throws SQLException
    {
    	return this.realConnection.createBlob ();
    }
    
    public NClob createNClob() throws SQLException
    {
    	return this.realConnection.createNClob ();
    }
    
    public SQLXML createSQLXML() throws SQLException
    {
    	return this.realConnection.createSQLXML ();
    }
    
    public Struct createStruct(String typeName, Object[] attributes)
    		throws SQLException
    {
    	return this.realConnection.createStruct (typeName, attributes);
    }    		
    
    public String getClientInfo(String name)
		throws SQLException
    {
    	return this.realConnection.getClientInfo (name);
    }		
	 
    public Properties getClientInfo()
		throws SQLException
    {
    	return this.realConnection.getClientInfo ();
    }
	 
    public boolean isValid(int timeout) throws SQLException
    {
    	return this.realConnection.isValid (timeout);
    }
    
    public void setClientInfo(String name, String value)
    		throws SQLClientInfoException
    {
    	this.realConnection.setClientInfo (name, value);
    }
    
    public void setClientInfo(Properties properties)
		throws SQLClientInfoException
	{
		this.realConnection.setClientInfo (properties);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.realConnection.unwrap(iface);
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return this.realConnection.isWrapperFor(iface);
	}

	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        this.realConnection.setTypeMap(map);
	}

    public void setSchema(String schema) throws SQLException {
        this.realConnection.setSchema(schema);
    }

    public String getSchema() throws SQLException {
        return this.realConnection.getSchema();
    }

    public void abort(Executor executor) throws SQLException {
        this.realConnection.abort(executor);
    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        this.realConnection.setNetworkTimeout(executor,milliseconds);
    }

    public int getNetworkTimeout() throws SQLException {
        return this.realConnection.getNetworkTimeout();
    }
}
