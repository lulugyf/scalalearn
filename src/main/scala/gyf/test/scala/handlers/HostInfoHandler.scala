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

class HostInfoHandler extends AbstractHandler with ContextPathTrait{
  val log = LoggerFactory.getLogger("HostInfoHandler")

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
    out.println("<pre>")
    val fis = Source.fromFile(s"${confPath}/hosts.txt")
    val keyFile = s"${confPath}/ssh_Identity"
    val lines = fis.getLines().toArray
    println(s"---- lines len ${lines.length}")
    fis.close()

    val futs = lines.map{line =>
      Future{
        one_host(line, keyFile, out)
      }
    }

    Await.result(Future.sequence(futs.toSeq), 1 minute).foreach(s => out.println(s"<h3>${s._1}</h3>\n${s._2}"))

    out.println("</pre>")
    out.println("</body></html>" )
    request.setHandled(true)
  }

  def one_host(host_str: String, keyFile: String, out: PrintWriter): Tuple2[String, String] = {
    val s = host_str.split(",")
    val hostname = s(0)
    val user=s(1)
    val host=s(2)
    var session: Session = null
    try{
      session = createSession(s"${user}@${host}", keyFile)

      val r = ssh_exec(session, "echo '====='; df -k /idmm; TERM=vt100 top -b -n 1|head -15")  // "hostname; df -k /idmm; top -n 1")
//      println(r._1)
      return (hostname, r._1 + r._2)
    }finally{
      if(session != null) session.disconnect()
    }
    (hostname, "failed")
  }

}

