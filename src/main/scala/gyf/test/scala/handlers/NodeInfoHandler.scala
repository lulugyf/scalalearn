package gyf.test.scala.handlers

import java.text.SimpleDateFormat

import gyf.test.scala.ZKUtil
import javax.annotation.Resource
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.beetl.core.GroupTemplate
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.json4s.{DefaultFormats, JObject}
import org.json4s.native.JsonMethods.parse
import org.slf4j.LoggerFactory

import scala.beans.BeanProperty

class NodeInfoHandler extends AbstractPathHandler{
  val log = LoggerFactory.getLogger("dbexcept")
  val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  @Resource
  var zkUtil: ZKUtil = null
  @Resource
  val templates: GroupTemplate = null

  // return addr, jmxaddr, ctime
  case class BrokerInfo(addr: String, jmx: String, ctime: String)
  def brokerList(path: String): Array[BrokerInfo] = {
    val zk = zkUtil.zk
    zk.getChildren.forPath(path).toArray.map{ p =>
      val state = zk.checkExists.forPath(s"$path/$p")
      val data = new String(zk.getData.forPath(s"$path/$p"))
      BrokerInfo(p.toString, data.split("/")(2), fmt.format(state.getCtime))
    }
  }

  def get(url: String) = scala.io.Source.fromURL(url).mkString

  // 通过jmx端口获取jvm的内存使用情况
  case class MemUsage(init: Long, committed: Long, max: Long, used: Long)
  def memory(jmxaddr: String): MemUsage = {
    //   val jmxaddr = "172.21.0.46:7002"
    val result = get(s"http://${jmxaddr}/jolokia/read/java.lang:type=Memory")

    val j = parse(result).asInstanceOf[JObject]
    implicit val formats = DefaultFormats

    val m = ( j \ "value" \ "HeapMemoryUsage").asInstanceOf[JObject]
    m.extract[MemUsage]
  }

  // return id, addr, jmxaddr, ctime
  case class bleInfo(id: String, addr: String, jmx: String, ctime: String)
  def getBleList(path: String): Array[bleInfo] = {
    val zk = zkUtil.zk
    zk.getChildren.forPath(path).toArray.map{ p =>
      val state = zk.checkExists.forPath(s"$path/$p")
      // 10.113.182.101:21052 jolokia-port:21152
      val data = new String(zk.getData.forPath(s"$path/$p")).split("[ :]")
      bleInfo(p.toString.substring(3), s"${data(0)}:${data(1)}", s"${data(0)}:${data(3)}", fmt.format(state.getCtime))
    }
  }

  override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType("text/html; charset=utf-8");
    val out = response.getWriter
    val t = templates.getTemplate("nodeinfo.html")

    import collection.JavaConverters._

    case class BrokerInfo(@BeanProperty addr: String, @BeanProperty jmx: String, @BeanProperty ctime: String, @BeanProperty ms: String)
    val brkList = brokerList("/idmm/broker")
    .map { broker =>
      val m = memory(broker.jmx)
      val percent = m.used * 100.0/m.max
      val ms = f"${m.used/1024/1024} MB / ${m.max/1024/1024} MB == $percent%2.2f %%"
      //out.println(s"<tr><td>${broker.addr}</td><td>${broker.jmx}</td><td>${broker.ctime}</td><td>$ms</td></tr>")
      BrokerInfo(broker.addr, broker.jmx, broker.ctime, ms)
    }
    t.binding("brkList", brkList.iterator.asJava)

    case class BLEInfo(@BeanProperty id: String, @BeanProperty addr: String, @BeanProperty jmx: String, @BeanProperty ctime: String, @BeanProperty ms: String)
    val bleList = getBleList("/idmm/ble").map { ble =>
        val m = memory(ble.jmx)
        val percent = m.used * 100.0/m.max
        val ms = f"${m.used/1024/1024} MB / ${m.max/1024/1024} MB == $percent%2.2f %%"
//      out.println(s"<tr><td>${ble.id}</td><td>${ble.addr}</td><td>${ble.jmx}</td><td>${ble.ctime}</td><td>${ms}</td></tr>")
        BLEInfo(ble.id, ble.addr, ble.jmx, ble.ctime, ms)
    }
    t.binding("bleList", bleList.iterator.asJava)

    t.renderTo(out)

    request.setHandled(true)
  }
}
