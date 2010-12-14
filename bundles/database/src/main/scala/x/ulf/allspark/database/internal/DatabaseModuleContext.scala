package x.ulf.allspark.database.internal

import x.ulf.allspark.ModuleContext
import x.ulf.allspark.database.Database

/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 04.12.2010
 * Time: 21:43:34
 * To change this template use File | Settings | File Templates.
 */

class DatabaseModuleContext extends ModuleContext {
  def configure = {
    val db = new DatabaseImpl
    register(db) under classOf[Database]
  }
}