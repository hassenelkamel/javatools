package javatools.database;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
Copyright 2016 Fabian M. Suchanek

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. 

The class OracleDatabase implements the Database-interface for an
Oracle SQL data base. Make sure that the file "classes12.jar" of the
Oracle distribution is in the classpath. When using Eclipse, add
the file via Project -&gt;Properties -&gt;JavaBuildPath -&gt;Libraries
-&gt;ExternalJARFile.<BR>
Example:
<PRE>
     Database d=new OracleDatabase("user","password");
     d.queryColumn("SELECT foodname FROM food WHERE origin=\"Italy\"")
     -&gt; [ "Pizza Romana", "Spaghetti alla Bolognese", "Saltimbocca"]
     Database.describe(d.query("SELECT * FROM food WHERE origin=\"Italy\"")
     -&gt; foodname |origin  |calories |
        ------------------------------
        Pizza Rom|Italy   |10000    |
        Spaghetti|Italy   |8000     |
        Saltimboc|Italy   |8000     |
</PRE>
This class also provides SQL datatypes (extensions of SQLType.java) that
behave according to the conventions of Oracle. For example, the ANSI SQL datatype
BOOLEAN is mapped to NUMBER(1). Furthermore, VARCHAR string literals print
inner quotes as doublequotes.
<P>
Oracle (and only Oracle!) often complains "ORA-01000: maximum open cursors exceeded".
This does not necessarily mean that the maximum number of open cursors is exceeded.
It can also mean that your SQL statement has an invalid character. Check your SQL
statements carefully. If the statements are OK, try the following:
<UL>
<LI> Avoid query() whenever possible and use executeQuery(), queryValue and
query/ResultIterator instead, because these close the open resources automatically.
<LI> If you use query(), be sure to call Database.close(ResultSet) afterwards.
<LI> Reset the connection from time to time by calling resetConnection(). (This
is an Oracle-specific trick).
<LI> Increase the number of cursors in Oracle by saying
dabatase.executeUpdate("ALTER SYSTEM SET open_cursors=1000000 scope=both")
</UL>
The simplest solution, though, is to use another database. Postgres and MySQL can
be downloaded for free, PostgresDatabase.java and MySQLDatabase.java provide the
respective Java-adapters.
*/
public class OracleDatabase extends Database {

  /** Prepares the query internally for a call (deletes trailing semicolon)*/
  @Override
  protected String prepareQuery(String sql) {
    if (sql.endsWith(";")) return (sql.substring(0, sql.length() - 1));
    else return (sql);
  }

  /** Constructs a non-functional OracleDatabase for use of getSQLType*/
  public OracleDatabase() {
    java2SQL.put(Boolean.class, bool);
    java2SQL.put(boolean.class, bool);
    java2SQL.put(String.class, varchar);
    java2SQL.put(Long.class, bigint);
    java2SQL.put(long.class, bigint);
    type2SQL.put(Types.VARCHAR, varchar);
    type2SQL.put(Types.BOOLEAN, bool);
    type2SQL.put(Types.BIGINT, bigint);
  }

  /** Constructs a new OracleDatabase from a user, a password and a host*/
  public OracleDatabase(String user, String password, String host)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    this(user, password, host, null);
  }

  /** Constructs a new OracleDatabase from a user, a password and a host*/
  public OracleDatabase(String user, String password, String host, String port)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    this(user, password, host, null, null);
  }

  /** Constructs a new OracleDatabase from a user, a password and a host
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws SQLException */
  public OracleDatabase(String user, String password, String host, String port, String inst)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
    this();
    if (password == null) password = "";
    if (host == null || host.length() == 0) host = "localhost";
    if (port == null || port.length() == 0) port = "1521";
    if (inst == null || inst.length() == 0) inst = "oracle";
    connectionString = "jdbc:oracle:thin:" + user + "/" + password + "@" + host + ":" + port + ":" + inst;
    Driver driver;
    driver = (Driver) Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
    DriverManager.registerDriver(driver);
    connect();
    description = "ORACLE database " + user + "/" + password + " at " + host + ":" + port + " instance " + inst;
  }

  /** Constructs a new OracleDatabase from a user and a password on the localhost*/
  public OracleDatabase(String user, String password) throws Exception {
    this(user, password, "localhost");
  }

  /** Holds the String by which the connection can be reset*/
  protected String connectionString;

  /** Resets the connection. 
   * @deprecated  replaced by {@link #reconnect()} */
  @Deprecated
  public void resetConnection() throws SQLException {
    close(connection);
    connect();
  }

  /** connects to the database specified */
  @Override
  public void connect() throws SQLException {
    connection = DriverManager.getConnection(connectionString);
    connection.setAutoCommit(true);
  }

  /** Makes an SQL query limited to n results */
  @Override
  public String limit(String sql, int n) {
    n++;
    Matcher m = Pattern.compile(" where", Pattern.CASE_INSENSITIVE).matcher(sql);
    if (!m.find()) {
      return (sql + " WHERE ROWNUM<" + n);
    }
    return (sql.substring(0, m.end()) + " ROWNUM<" + n + " AND " + sql.substring(m.end()));
  }

  // -------------------------------------------------------------------------------
  // ------------------ Datatypes --------------------------------------------------
  // -------------------------------------------------------------------------------

  public static class Varchar extends SQLType.ANSIvarchar {

    public Varchar(int size) {
      super(size);
    }

    public Varchar() {
      super();
    }

    @Override
    public String toString() {
      return ("VARCHAR2(" + scale + ")");
    }

    @Override
    public String format(Object o) {
      String s = o.toString().replace("'", "''");
      if (s.length() > scale) s = s.substring(0, scale);
      return ("'" + s + "'");
    }
  }

  public static Varchar varchar = new Varchar();

  public static class Bool extends SQLType.ANSIboolean {

    public Bool() {
      super();
      typeCode = java.sql.Types.INTEGER;
    }

    @Override
    public String format(Object o) {
      if (super.format(o).equals("true")) return ("1");
      else return ("0");
    }

    @Override
    public String toString() {
      return ("NUMBER(1)");
    }
  }

  public static Bool bool = new Bool();

  public static class Bigint extends SQLType.ANSIBigint {

    @Override
    public String toString() {
      return ("NUMBER(37)");
    }
  }

  public static Bigint bigint = new Bigint();

  @Override
  public boolean jarAvailable() {
    try {
      Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
      return true;
    } catch (Exception e) {
    }
    return false;
  }

  /** Checks whether the connection to the database is still alive */
  @Override
  public boolean connected() {
    try {
      return (!connection.isClosed()) && connection.isValid(validityCheckTimeout);
    } catch (SQLFeatureNotSupportedException nosupport) {
      try {
        ResultSet rs = query("SELECT 1 FROM DUAL", resultSetType, resultSetConcurrency, null);
        close(rs);
        return true;
      } catch (SQLException ex) {
        return false;
      }
    } catch (SQLException ex) {
      throw new RuntimeException("This is very unexpected and actually should never happen.", ex);
    }
  }

  public static void main(String[] args) {
    OracleDatabase database = new OracleDatabase();
    String sql = "SELECT arg2 FROM facts WHERE relation=something AND arg1= something";
    System.out.println(database.limit(sql, 7));
  }

};