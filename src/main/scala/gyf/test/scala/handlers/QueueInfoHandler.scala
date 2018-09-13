package gyf.test.scala.handlers

import java.text.SimpleDateFormat

import gyf.test.scala.ZKUtil
import javax.annotation.Resource
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.curator.framework.CuratorFramework
import org.beetl.core.GroupTemplate
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.beans.BeanProperty
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import collection.JavaConverters._

class QueueInfoHandler extends AbstractPathHandler{
  val log = LoggerFactory.getLogger("dbexcept")
  val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  case class Info(@BeanProperty target_client_id: String, @BeanProperty total: Int, @BeanProperty size: Int,
                  @BeanProperty err: Int, @BeanProperty target_topic_id: String, @BeanProperty sending: Int,
                  @BeanProperty status: String, @BeanProperty var bleid: String="")
  case class QInfo(target_client_id: String, total: Int, size: Int, err: Int, target_topic_id: String, sending: Int, status: String)
  case class BleInfo(id: String, addr: String, jmx: String, ctime: String)

  implicit val formats = DefaultFormats

  @Resource
  var zkUtil: ZKUtil = null

  @Resource
  val templates: GroupTemplate = null

  // "target_client_id\":\"SubPayment\",\"total\":0,\"size\":0,\"err\":0,\"target_topic_id\":\"TpaymentResultNotify-A\",\"sending\":0,\"status\":\"ready\"
  // return id, addr, jmxaddr, ctime
  def bleList(path: String): Array[BleInfo] = {
    val zk = zkUtil.zk
    zk.getChildren.forPath(path).toArray.map{ p =>
      val state = zk.checkExists.forPath(s"$path/$p")
      // 10.113.182.101:21052 jolokia-port:21152
      val data = new String(zk.getData.forPath(s"$path/$p")).split("[ :]")
      BleInfo(p.toString.substring(3), s"${data(0)}:${data(1)}", s"${data(0)}:${data(3)}", fmt.format(state.getCtime))
    }
  }

  def get(url: String) = scala.io.Source.fromURL(url).mkString

  def qinfo(jmxaddr: String, bleid: String): Array[Info] = {
    //   val jmxaddr = "172.21.0.46:7002"
    val result = get(s"http://${jmxaddr}/jolokia/exec/com.sitech.crmpd.idmm.ble.RunTime:name=runTime/info")

    val j = parse(result)
    parse((j \ "value").values.toString).extract[Array[QInfo]]
      .map{q => Info(q.target_client_id, q.total, q.size, q.err, q.target_topic_id, q.sending, q.status, bleid)}
  }

  override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType("text/html; charset=utf-8");
    val out = response.getWriter
    var uri = httpServletRequest.getRequestURI
    if(uri.indexOf('?') > 0)
      uri = uri.substring(0, uri.indexOf('?'))

    val t = templates.getTemplate("qinfo.html")

    t.binding("uri", uri)

    val ble_id = httpServletRequest.getParameter("bleid")
    val jmx_addr = httpServletRequest.getParameter("jmxaddr")
    val onlysized = "true".equals(httpServletRequest.getParameter("sized"))
    log.debug("got ble_id: [{}] onlysized: {}", ble_id + jmx_addr, onlysized)
    if(ble_id == null || "".equals(ble_id )) {
      // 并行执行 qinfo
      val lst = bleList("/idmm/ble").map { ble =>
        Future {
          qinfo(ble.jmx, ble.id)
        }
      }
      val rets = Await.result(Future.sequence(lst.toSeq), 1 minute).flatten.toArray
      var all_total=0
      var all_size=0
      var all_sending=0
      var all_err = 0
      for(q <- rets){
        all_total = all_total + q.total
        all_size = all_size + q.size
        all_sending=all_sending + q.sending
        all_err = all_err + q.err
      }
      t.binding("all_total", all_total)
      t.binding("all_size", all_size)
      t.binding("all_err", all_err)
      t.binding("all_sending", all_sending)

      // 排序
      val qlist = rets.sortWith{(a,b) => {
        if(a.size>b.size)
          true
        else if(a.size < b.size)
          false
        else if(a.total > b.total)
          true
        else if(a.total < b.total)
          false
        else
          false
      }}.filter(q => (onlysized && q.size > 0) || !onlysized)

      t.binding("qList", qlist.iterator.asJava)
      t.renderTo(out)
    }/*else{
      val qlist = qinfo(jmx_addr, ble_id)
      out.println(s"<tr><th colspan=2> ${ble_id}</th><td colspan=5>${jmx_addr} </td></tr>")
//      out.println("<tr><th>消息主题</th><th>消费者ID</th><th>消息总量</th> <th >积压</th><th>失败</th><th >在途</th><th>状态</th></tr>")
      qlist.map { q =>
        if((onlysized && q.size > 0) || !onlysized)
          out.println(s"<tr><td>${q.bleid}</td><td>${q.target_topic_id}</td><td>${q.target_client_id}</td><td>${q.total}</td><td>${q.size}</td><td>${q.err}</td><td>${q.sending}</td><td>${q.status}</td></tr>")
      }
    } */

    request.setHandled(true)
  }

  def test(): Unit = {
    val fut = Future {
      Thread.sleep(10000); 21 + 21
    }
    val x = Await.result(fut, 15.seconds)
  }
}
