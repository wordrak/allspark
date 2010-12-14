package x.ulf.vinculum

import commands.ListCommand
import org.osgi.framework.{BundleActivator, BundleContext}

/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 23.11.2010
 * Time: 18:32:57
 * To change this template use File | Settings | File Templates.
 */

object Vinculum {
  val PORT = 8472
  val MULTICAST_GROUP = "224.9.1.1";
}

class Vinculum extends BundleActivator {
  private val thread = new VinculumThread();

  def start(context: BundleContext) {
    println("starting")
    this.thread.start();
    new ListCommand(this.thread).register(context)

    val refs = context.getAllServiceReferences(null, "(osgi.command.scope=*)")

    refs.foreach(r => {
      println(r)
      r.getPropertyKeys().foreach(k => println(k + " => " + r.getProperty(k)))
      println("------")
      println(r.getProperty("osgi.command.scope"))
      println(r.getProperty("osgi.command.function"))
    })
  }

  def stop(context: BundleContext) {
    this.thread.interrupt();
    this.thread.join(500)
  }
}

