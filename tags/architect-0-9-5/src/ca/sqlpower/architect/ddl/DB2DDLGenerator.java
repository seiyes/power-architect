package ca.sqlpower.architect.ddl;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;

// TODO: override to_identifier routine to ensure identifier names are legal
// and unique for DB2.  See the Oracle, SQL Server, and Postgres DDL generators
// for hints on how it should be written.  Development of this stuff has been
// deferred until we have an architect customer who wants to use it with DB2.

public class DB2DDLGenerator extends GenericDDLGenerator {

	public DB2DDLGenerator() throws SQLException {
		super();
	}

	public static final String GENERATOR_VERSION = "$Revision$";

    @Override
	public void writeHeader() {
		println("-- Created by SQLPower DB2 DDL Generator "+GENERATOR_VERSION+" --");
	}

    @Override
	protected void createTypeMap() throws SQLException {
		typeMap = new HashMap();

		typeMap.put(Integer.valueOf(Types.BIGINT), new GenericTypeDescriptor("BIGINT", Types.BIGINT, 38, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.BINARY), new GenericTypeDescriptor("BLOB", Types.BINARY, 2147483647, "0x", null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.BIT), new GenericTypeDescriptor("DECIMAL", Types.BIT, 1, null, null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.BLOB), new GenericTypeDescriptor("BLOB", Types.BLOB, 2147483647, "0x", null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.CHAR), new GenericTypeDescriptor("CHAR", Types.CHAR, 254, "'", "'", DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.CLOB), new GenericTypeDescriptor("CLOB", Types.CLOB, 2147483647, "'", "'", DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.DATE), new GenericTypeDescriptor("DATE", Types.DATE, 10, "'", "'", DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.DECIMAL), new GenericTypeDescriptor("DECIMAL", Types.DECIMAL, 31, null, null, DatabaseMetaData.columnNullable, true, true));
		typeMap.put(Integer.valueOf(Types.DOUBLE), new GenericTypeDescriptor("DOUBLE", Types.DOUBLE, 53, null, null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.FLOAT), new GenericTypeDescriptor("FLOAT", Types.FLOAT, 53, null, null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.INTEGER), new GenericTypeDescriptor("INTEGER", Types.INTEGER, 10, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.LONGVARBINARY), new GenericTypeDescriptor("BLOB", Types.LONGVARBINARY, 2147483647, "0x", null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.LONGVARCHAR), new GenericTypeDescriptor("CLOB", Types.LONGVARCHAR, 2147483647, "'", "'", DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.NUMERIC), new GenericTypeDescriptor("DECIMAL", Types.NUMERIC, 31, null, null, DatabaseMetaData.columnNullable, true, true));
		typeMap.put(Integer.valueOf(Types.REAL), new GenericTypeDescriptor("FLOAT", Types.REAL, 31, null, null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.SMALLINT), new GenericTypeDescriptor("SMALLINT", Types.SMALLINT, 5, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.TIME), new GenericTypeDescriptor("TIME", Types.TIME, 8, "'", "'", DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.TIMESTAMP), new GenericTypeDescriptor("TIMESTAMP", Types.TIMESTAMP, 26, "'", "'", DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.TINYINT), new GenericTypeDescriptor("SMALLINT", Types.TINYINT, 5, null, null, DatabaseMetaData.columnNullable, false, false));
		typeMap.put(Integer.valueOf(Types.VARBINARY), new GenericTypeDescriptor("BLOB", Types.VARBINARY, 2147483647, null, null, DatabaseMetaData.columnNullable, true, false));
		typeMap.put(Integer.valueOf(Types.VARCHAR), new GenericTypeDescriptor("VARCHAR", Types.VARCHAR, 32672, "'", "'", DatabaseMetaData.columnNullable, true, false));
	}

	/**
	 * Returns the string "Database".
	 */
    @Override
	public String getCatalogTerm() {
		return "Database";
	}

	/**
	 * Returns the string "Schema".
	 */
    @Override
	public String getSchemaTerm() {
		return "Schema";
	}
}