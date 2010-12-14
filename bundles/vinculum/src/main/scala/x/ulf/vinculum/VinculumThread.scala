package x.ulf.vinculum

import java.net.{DatagramPacket, MulticastSocket, InetAddress}
import java.util.logging.Logger
import collection.mutable.HashSet

/**
 * Created by IntelliJ IDEA.
 * User: ulf
 * Date: 27.11.2010
 * Time: 14:36:01
 * To change this template use File | Settings | File Templates.
 */

class VinculumThread extends Thread {
  val SYN = "ViNcUlUm SYN"
  val ACK = "ViNcUlUm ACK"
  val FIN = "ViNcUlUm FIN"

  val peers = new HashSet[Peer]()
  val socket = new MulticastSocket(Vinculum.PORT);
  val group = InetAddress.getByName(Vinculum.MULTICAST_GROUP)
  val name = this.hashCode().toString()

  val log = Logger.getLogger(classOf[VinculumThread].getName)

  override def run() {
    log.info("starting")
    this.sendHelloDatagram()
    socket.joinGroup(this.group)

    try {
      while
      (!this.isInterrupted) {
        val packet = this.receive
        if (packet != null) {
          this.handle(packet)
        }
      }
    } finally {
      log.info("finishing");
      this.sendGoodByDatagram()
      log.info("stopping")
    }
  }

  private def send(code: String, msg: String, target: InetAddress) {
    val content = code + " " + System.currentTimeMillis + " " + msg
    log.info("Sending: " + content)
    val bytes = content.getBytes();
    val packet = new DatagramPacket(bytes, bytes.length, target, Vinculum.PORT);
    this.socket.send(packet);
  }

  private val buffer = new Array[Byte](256)

  private def receive(): DatagramPacket = {
    val packet = new DatagramPacket(buffer, buffer.length)
    this.socket.receive(packet)
    val content = new String(packet.getData(), packet.getOffset(), packet.getLength());
    log.info("Receiving: " + content + " from " + packet.getAddress)
    return packet
  }

  def sendHelloDatagram() {
    this.send(SYN, name, this.group)
  }

  def sendGoodByDatagram() {
    this.send(FIN, name, this.group)
  }

  def handle(packet: DatagramPacket) {
    val content = new String(packet.getData(), packet.getOffset(), packet.getLength());
    val address = packet.getAddress();

    if (content.startsWith(SYN)) {
      this.peers.add(new Peer(packet.hashCode, content.split(' ').last, address));
      this.send(ACK, name, address)
    }
    if (content.startsWith(ACK)) {
      this.peers.add(new Peer(packet.hashCode, content.split(' ').last, packet.getAddress));
    }
    if (content.startsWith(FIN)) {
      this.peers.remove(new Peer(packet.hashCode, content.split(' ').last, packet.getAddress));
    }
    log.info("New peer count: " + this.peers.size)
  }

}
