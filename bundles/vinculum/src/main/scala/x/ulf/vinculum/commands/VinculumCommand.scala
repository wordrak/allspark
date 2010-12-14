package x.ulf.vinculum.commands

import java.util.logging.Logger
import x.ulf.vinculum.VinculumThread

/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 28.11.2010
 * Time: 11:41:01
 * To change this template use File | Settings | File Templates.
 */

abstract class VinculumCommand extends AbstractCommand {
  override def scope = "vinculum"
}

class ListCommand(thread: VinculumThread) extends VinculumCommand {
  private val log = Logger.getLogger(classOf[ListCommand].getName)

  override def name = "execute"

  override def execute() {
    this.thread.peers.foreach(peer => log.info(peer.toString))
  }
}