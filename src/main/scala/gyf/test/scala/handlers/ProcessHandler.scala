package gyf.test.scala.handlers

import java.io.{FileWriter, PrintWriter}
import java.text.SimpleDateFormat
import java.util.concurrent.Executors

import com.jcraft.jsch.Session
import gyf.test.scala.ZKUtil
import gyf.test.scala.utils.RemoteSSH._
import javax.annotation.Resource
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.json4s.{DefaultFormats, JObject}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source

/**
  * 进程信息采集:
  *
  * BLE 进程:
  * > 进程id
  * > 进程命令
  * > 对应的运行目录
  * > 启动时间
  * > 对应zookeeper上的ble-id,  或者没有
  * > 监听端口 通讯端口  jmx端口
  * > TCP连接数量, 怎么分组?
  * > 数据库连接池信息
  * > 内存情况
  *
  * Broker 进程:
  * > 进程id
  * > 进程命令
  * > 对应的运行目录
  * > 启动时间
  * > 对应zookeeper上的broker-id
  * > 监听端口 通讯端口  jmx端口 http端口
  * > TCP连接数量, 怎么分组?
  * > 数据库连接池信息
  * > 内存情况
  */

class ProcessHandler extends AbstractHandler with ContextPathTrait{
  val log = LoggerFactory.getLogger("ProcessHandler")

  @Value("${JHOME}")
  val jhome:String = null
  @Value("${LSOF_PATH:/usr/sbin/lsof}")
  val lsof:String = null

  @Resource
  var zkUtil: ZKUtil = null

  implicit val ec = new ExecutionContext { // use own defined ExcutionContext
    val threadPool = Executors.newFixedThreadPool(12);
    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }
    def reportFailure(t: Throwable) {}
  }


  override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType("text/html; charset=utf-8");
    val out = response.getWriter

    var confPath = System.getProperty("CONF-PATH")
    if(confPath == null) confPath = "conf"

    out.println("""<html><header>
        <link href="../resource/style.css" rel="stylesheet" type="text/css" />
        <title>idmm check</title></header>
        <body>""")

    out.println(s"<h1>节点进程信息, 采集时间: ${new java.util.Date().toString}</h1><br/>")
//    out.println("<pre>")
    val fis = Source.fromFile(s"${confPath}/hosts.txt")
    val keyFile = s"${confPath}/ssh_Identity"
    val lines = fis.getLines().toArray
    fis.close()

    val blelist = bleList("/idmm/ble")

    val futs = lines.map{line =>
      Future{
        one_host_ble(line, keyFile, blelist)
      }
    }

    Await.result(Future.sequence(futs.toSeq), 2.minutes).foreach(s => out.println(s"<h3>${s._1}</h3>\n${s._2}"))

//    out.println("</pre>")
    out.println("</body></html>" )
    request.setHandled(true)
  }

  def one_host_ble(host_str: String, keyFile: String, blelist: Array[BleInfo]): Tuple2[String,String] = {
    val s = host_str.split(",")
    val hostname = s(0)
    val user=s(1)
    val host=s(2)
    val ip = s(3).trim
    var session: Session = null
    try{
      val host_str = s"${user}@${host}"
//      println(s"host_str: [${host_str}]")
      session = createSession(host_str, keyFile)

      val tbls = (setBLEId(get_process_one(session, "ble"), blelist, ip) ++ get_process_one(session, "broker"))
        .filter(_.pid != "").map{ p =>
        p.toTable()
      }.mkString("\n")
      return (hostname, "<table>"+ ProcessInfo("1").tblHeader()+tbls + "</table>")
    }finally{
      if(session != null) session.disconnect()
    }
    (hostname, "failed")
  }

  def setBLEId(ps: Array[ProcessInfo], blelist: Array[BleInfo], ip: String): Array[ProcessInfo] = {
    for(p <- ps; b <- blelist) {
      if(s"${ip}:${p.jmxport}" == b.jmx)
        p.bleid = b.id
    }
    ps
  }
  def get_process_one(session: Session, ptype: String): Array[ProcessInfo] = {
    val cmd = ptype match {
      case "ble" => s"""${jhome}/bin/jps -l|grep -e BLEServ|while read pid class; do echo "##PS:"; ls -ld /proc/$$pid;
                       |cwd=`ls -l /proc/$$pid/cwd|awk '{print $$NF}'`;
                       |jmxport=`grep jmx.jolokiaPort $$cwd/config/ble/*.properties|awk -F "=" '{print $$NF}'`;
                       |listenport=`grep netty.listen.port $$cwd/config/ble/*.properties|awk -F "=" '{print $$NF}'`;
                       |echo "#%CWD: $$pid $$cwd $$class $$jmxport $$listenport";
                       |echo "#%LSOF:"; ${lsof} -p $$pid -P|grep TCP|awk '{print $$(NF-1), $$NF}';
                       |echo "#%MEM:"; curl "http://localhost:$$jmxport/jolokia/read/java.lang:type=Memory" 2>/dev/null; echo "";
                       |echo "#%DATASOURCE:"; curl "http://localhost:$$jmxport/jolokia/exec/com.sitech.crmpd.idmm:name=dsMon/dsinfo" 2>/dev/null; echo "";
                       |done""".stripMargin
      case "broker" => s"""${jhome}/bin/jps -l|grep -e App|while read pid class; do echo "##PS:"; ls -ld /proc/$$pid;
                          |cwd=`ls -l /proc/$$pid/cwd|awk '{print $$NF}'`;
                          |jmxport=`grep jolokia.port $$cwd/config/broker/*.properties|awk -F "=" '{print $$NF}'`;
                          |listenport=`grep netty.listen.port $$cwd/config/broker/*.properties|awk -F "=" '{print $$NF}'`;
                          |echo "#%CWD: $$pid $$cwd $$class $$jmxport $$listenport";
                          |echo "#%LSOF:"; ${lsof} -p $$pid -P|grep TCP|awk '{print $$(NF-1), $$NF}';
                          |echo "#%MEM:"; curl "http://localhost:$$jmxport/jolokia/read/java.lang:type=Memory" 2>/dev/null; echo "";
                          |echo "#%DATASOURCE:"; curl "http://localhost:$$jmxport/jolokia/exec/com.sitech.crmpd.idmm:name=dsMon/dsinfo" 2>/dev/null; echo "";
                          |done""".stripMargin
      case _ => ""
    }
    val ps = ssh_exec(session, cmd) match {
      case (stdout: String, stderr: String) =>{
        //        println(stdout)
        var idx = 0
        stdout.split("##").map{ block=>
          idx = idx + 1
          val pi = new ProcessInfo(idx.toString)
          block.split("#%").filter(_.indexOf(':')>0).map{ss =>
            val p=ss.indexOf(':')
            val ctype=ss.substring(0, p)
            val cvalue=ss.substring(p+1).trim
            ctype match {
              case "PS" => pi.start_time = cvalue.split(" +").slice(5,8).mkString(" ")
              case "CWD" =>
                val v = cvalue.split(" +")
                pi.pid = v(0)
                pi.jmxport=v(1)
                pi.cwd = v(1)
                pi.args = v(2)
                if(v.length > 3)
                  pi.jmxport = v(3)
                if(v.length > 4)
                  pi.listenport=v(4)
              case "MEM" =>
                try {
                  implicit val formats = DefaultFormats
                  import org.json4s.native.JsonMethods.parse
                  val j = parse(cvalue)
                  val m = (j \ "value" \ "HeapMemoryUsage").asInstanceOf[JObject].extract[MemUsage]
                  pi.mem = m.toString()
                }catch{
                  case _ : Throwable => pi.mem="[failed]: " + cvalue
                }
              case "DATASOURCE" =>
                try {
                  implicit val formats = DefaultFormats
                  import org.json4s.native.JsonMethods.parse
                  val j = parse(cvalue)
                  if (cmd.indexOf("/config/ble/") > 0) {
                    val m1 = (j \ "value" \ "dsConfig").asInstanceOf[JObject].extract[DSInfo]
                    m1.dsName = "dsConfig"
                    val m2 = (j \ "value" \ "dataSource").asInstanceOf[JObject].extract[DSInfo]
                    m2.dsName = "dataSource"
                    pi.dataSource = m1.toString() + "\n" + m2.toString()
                  } else if (cmd.indexOf("/config/broker/") > 0) {
                    val d = (j \ "value" \ "dataSource").asInstanceOf[JObject].extract[DSInfo]
                    d.dsName = "dataSource"
                    pi.dataSource = d.toString
                  }
                }catch{
                  case _ : Throwable => pi.mem = "[failed]: "+cvalue
                }
              case "LSOF" =>
                __conns(cvalue, pi)
            }
          }
          pi
        }
      }
    }
    ps
  }
  def __conns(cvalue: String, pi: ProcessInfo): Unit = {
    val lines = cvalue.split("\n")
    val listent_ports = lines.filter(_.indexOf("LISTEN") > 0).map{ s =>
      s.split("[: ]")(1)
    }
    //println(s"listen ports ${listent_ports.mkString(",")}")
    import scala.collection.mutable
    val income = new mutable.HashMap[String, Int]()
    val outgoing = new mutable.HashMap[String, Int]()
    var incounts = 0
    var outcounts = 0
    lines.filter(_.indexOf("ESTABLISHED") > 0).foreach{s =>
      // xqidmm1:9621->xqidmm2:45623 (ESTABLISHED)
      val fv = s.split("[-: >]")
      if(listent_ports.contains(fv(1))){ //外来连接
        val k = s"${fv(1)}<-${fv(3)}"
        income(k) = income.getOrElse(k, 0) + 1
        incounts = incounts + 1
      }else{
        val k = s"${fv(3)}:${fv(4)}"
        outgoing(k) = outgoing.getOrElse(k, 0) + 1
        outcounts = outcounts + 1
      }
    }
    pi.conns_detail = s"IN(${incounts}):\n${income.mkString("\n")}\nOUT(${outcounts}):\n${outgoing.mkString("\n")}"
    pi.conns = s"IN(${incounts}) OUT(${outcounts})"
  }

  case class ProcessInfo(id: String){
    var pid=""
    var start_time: String=null
    var cwd = ""
    var jmxport=""
    var args: String=null
    var listenport=""

    var mem = ""
    var dataSource = ""
    var conns = ""
    var conns_detail = ""
    var bleid = ""
    def tblHeader(): String = {
      <tr><th>PID</th><th>args</th><th>bleid</th><th>cwd</th><th>start_time</th><th>jmxport</th><th>listenport</th><th>mem(used/max)</th>
        <th>dataSource<br/>(active/idle/max)</th><th>conns</th></tr>.toString
    }
    def toTable(): String = {
      <tr><td>{pid}</td><td>{args}</td><td>{bleid}</td><td>{cwd}</td><td>{start_time}</td><td>{jmxport}</td><td>{listenport}</td>
        <td><pre>{mem}</pre></td><td><pre>{dataSource}</pre></td><td><div class="tooltip">{conns}<span class="tooltiptext"><pre>{conns_detail}</pre></span></div></td></tr>.toString
    }
  }
  case class MemUsage(init: Long, committed: Long, max: Long, used: Long){
    override def toString(): String = f"${used/1024/1024} MB/${max/1024/1024} MB ${used * 100.0/max}%2.2f %%"
  }
  case class DSInfo(idle: Int, active: Int, maxActive: Int, className: String, props: String){
    var dsName: String = ""
    override def toString(): String = s"${dsName}: ${active}/${idle}/${maxActive}"
  }

  case class BleInfo(id: String, addr: String, jmx: String, ctime: String)
  val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def bleList(path: String): Array[BleInfo] = {
    val zk = zkUtil.zk
    zk.getChildren.forPath(path).toArray.map{ p =>
      val state = zk.checkExists.forPath(s"$path/$p")
      // 10.113.182.101:21052 jolokia-port:21152
      val data = new String(zk.getData.forPath(s"$path/$p")).split("[ :]")
      BleInfo(p.toString.substring(3), s"${data(0)}:${data(1)}", s"${data(0)}:${data(3)}", fmt.format(state.getCtime))
    }
  }

}

