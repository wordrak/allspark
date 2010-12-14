package x.ulf.allspark

import collection.mutable.HashSet
import java.util.Properties
import org.osgi.framework.{ServiceRegistration, BundleContext, BundleActivator}

/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 04.12.2010
 * Time: 20:33:17
 * To change this template use File | Settings | File Templates.
 */

abstract class ModuleContext extends BundleActivator {
  val registrations = new HashSet[Registration[_]]
  var serviceRegistrations: HashSet[ServiceRegistration] = null;
  private var context: BundleContext = null;

  def configure

  def start(context: BundleContext) = {
    this.context = context
    this.configure
    this.serviceRegistrations = this.registrations.map({
      r => context.registerService(r.interface, r.service, r.properties)
    })
  }

  def stop(context: BundleContext) = {
    this.serviceRegistrations.foreach(sr => sr.unregister)
  }

  def register[T](service: T): Registration[T] = {
    val r = new Registration[T](service)
    this.registrations += r
    return r
  }
}

class Registration[T](val service: T) {
  var interface: String = null
  var properties = new Properties

  def under[I >: T](interface: Class[I]): Registration[T] = {
    this.interface = interface.getName
    return this
  }
}