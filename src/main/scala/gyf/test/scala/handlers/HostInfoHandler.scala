package gyf.test.scala.handlers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException, PrintWriter}
import java.util.concurrent.Executors
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.jcraft.jsch.Session
import javax.annotation.Resource
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.tomcat.jdbc.pool.DataSource
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.io.Source
import gyf.test.scala.utils.RemoteSSH._
import org.springframework.beans.factory.annotation.Value

import scala.beans.BeanProperty

class HostInfoHandler extends AbstractHandler with ContextPathTrait{
  val log = LoggerFactory.getLogger("HostInfoHandler")

  @Value("${hostpass:}")
  val hostpass: String = null

  implicit val ec = new ExecutionContext { // use own defined ExcutionContext
    val threadPool = Executors.newFixedThreadPool(10);
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

    out.println(s"<h1>采集时间: ${new java.util.Date().toString}</h1><br/>")
    out.println("<table>")
    out.println("<tr><th>hostname</th><th>disk-path</th><th>disk-used</th><th>disk-free</th><th>cpu-used</th><th>mem-used</th>" +
      "<th>mem-free</th><th>swap-used</th><th>swap-free</th></tr>")
    //out.println("<pre>")
    val fis = Source.fromFile(s"${confPath}/hosts.txt")
    val keyFile = s"${confPath}/ssh_Identity"
    val lines = fis.getLines().toArray
    //println(s"---- lines len ${lines.length}")
    fis.close()

    val futs = lines.map{line =>
      Future{
        one_host(line, keyFile, out)
      }
    }

    Await.result(Future.sequence(futs.toSeq), 1 minute).filter(_._2 != "failed").foreach { s =>
      //out.println(s"<h3>${s._1}</h3>\n${s._2}")
      out.println(parse_result(s._2, s._1))
    }

    out.println("</table>")
    out.println("</body></html>" )
    request.setHandled(true)
  }

  case class HostInfo(@BeanProperty hostname: String, @BeanProperty hostip:String)

  def one_host(host_str: String, keyFile: String, out: PrintWriter): Tuple2[String, String] = {
    val s = host_str.split(",")
    val hostname = s(0)
    val user=s(1)
    val host=s(2)
    var session: Session = null
    try{
      val host_str = s"${user}@${host}"
      session = if(hostpass == "") createSession(host_str, keyFile)
      else createSessionWithPass(host_str, hostpass)

      val r = ssh_exec(session, "echo '====='; df -k /idmm; echo; echo; TERM=vt100 top -b -n 1|head -15")  // "hostname; df -k /idmm; top -n 1")
//      println(r._1)
      return (hostname, r._1 + r._2)
    }finally{
      if(session != null) session.disconnect()
    }
    (hostname, "failed")
  }

  def parse_result(s: String, hostname: String): String = {
    val df = """(\d+) +(\d+) +(\d+) +(\d+)% +(/idmm)""".r
    val dfr = df.findFirstMatchIn(s).get   //206293688 10549660 185258268   6% /idmm === total used avail percent
    val disk_path = dfr.group(5)
    val disk_used = dfr.group(4)
    val disk_free = dfr.group(3).toLong / 1024  //mb


    val cpu="""Cpu\(s\): .+ ([\d\.]+)%id,""".r
    val cpur = cpu.findFirstMatchIn(s).get   // Cpu(s):  0.2%us,  0.1%sy,  0.0%ni, 99.6%id,
    val cpu_used = 100.0 - cpur.group(1).toFloat

    val mem = """Mem: +(\d+)k +total, +(\d+)k +used""".r
    val memr = mem.findFirstMatchIn(s).get //  Mem:  264409644k total, 36303808k used
    val mem_used = memr.group(2).toInt * 100.0 / memr.group(1).toInt
    val mem_free = (memr.group(1).toInt - memr.group(2).toInt) / 1024  // mb

    val swap = """Swap: +(\d+)k +total, +(\d+)k +used""".r
    val swapr = swap.findFirstMatchIn(s).get //Swap: 33554428k total,        0k used
    val swap_used = swapr.group(2).toInt * 100.0 / swapr.group(1).toInt
    val swap_free = (swapr.group(1).toInt - swapr.group(2).toInt) / 1024  // mb

    f"""<tr><td><b>${hostname}</b></td><td>${disk_path}</td><td>${disk_used} %%</td><td>${disk_free} MB</td>
       |<td>${cpu_used}%.2f %%</td><td>${mem_used}%.2f %%</td><td>${mem_free} MB</td>
       |<td>${swap_used}%.2f %%</td><td>${swap_free} MB</td></tr>""".stripMargin

    //"disk-path disk-used(%) disk-free(mb) cpu-used(%) mem-used(%) mem-free(mb) swap-used(%) swap-free(mb)"

  }

}

