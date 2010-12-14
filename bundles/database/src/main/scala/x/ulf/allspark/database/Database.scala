package x.ulf.allspark.database

import org.apache.ibatis.session.SqlSession


/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 04.12.2010
 * Time: 11:32:29
 * To change this template use File | Settings | File Templates.
 */

trait Database {
  def execute[R](transaction: SqlSession => R): R;
}

class TransactionException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this() = this (null, null)

  def this(message: String) = this (message, null)

  def this(cause: Throwable) = this (null, cause)
}