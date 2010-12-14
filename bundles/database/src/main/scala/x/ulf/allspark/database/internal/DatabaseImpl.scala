package x.ulf.allspark.database.internal

import x.ulf.allspark.database.{Database, TransactionException}
import org.h2.jdbcx.JdbcDataSource
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory
import org.apache.ibatis.mapping.Environment
import org.apache.ibatis.session.{SqlSession, SqlSessionFactoryBuilder, Configuration}
import org.apache.ibatis.jdbc.ScriptRunner
import org.apache.ibatis.migration.MigrationReader
import java.io.InputStreamReader
import java.util.Properties

/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 04.12.2010
 * Time: 11:39:15
 * To change this template use File | Settings | File Templates.
 */

class DatabaseImpl extends Database {
  val dataSource = new JdbcDataSource
  this.dataSource.setURL("jdbc:h2:database/allspark")
  this.dataSource.setUser("sa")
  this.dataSource.setPassword("")

  val transactionFactory = new JdbcTransactionFactory();
  val environment = new Environment("development", transactionFactory, dataSource);
  val configuration = new Configuration(environment);
  val sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

  def initialize {
    val urls = getClass.getClassLoader.getResources("/sql/test.sql")
    println(urls)

    val runner = new ScriptRunner(this.dataSource.getConnection)

    while (urls.hasMoreElements) {
      val url = urls.nextElement
      println("running: " + url)
      runner.runScript(new MigrationReader(new InputStreamReader(url.openStream), false, new Properties))
    }
    runner.closeConnection

  }

  initialize

  def execute[R](transaction: SqlSession => R): R = {
    val session = this.sqlSessionFactory.openSession(false);
    try {
      val r = transaction.apply(session)
      session.commit
      return r;
    } catch {
      case e: Exception => {
        if (session != null) {
          session.rollback
        }
        throw new TransactionException(e)
      }
    } finally {
      if (session != null) {
        session.close
      }
    }

  }
}