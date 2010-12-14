package x.ulf.vinculum.commands

import org.osgi.framework.BundleContext
import java.util.Properties

/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 28.11.2010
 * Time: 11:33:49
 * To change this template use File | Settings | File Templates.
 */

abstract class AbstractCommand {
  def scope: String

  def name: String

  def execute

  def register(ctx: BundleContext) {
    val properties = new Properties
    properties.setProperty("osgi.command.scope", this.scope)
    val name = new Array[String](1)
    name(0) = this.name
    println(name.getClass)
    name.foreach(n => println(n))
    properties.put("osgi.command.function", name)
    ctx.registerService(this.getClass.getName, this, properties)
  }
}