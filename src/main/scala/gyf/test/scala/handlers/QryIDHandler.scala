package gyf.test.scala.handlers


import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import javax.annotation.Resource
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.tomcat.jdbc.pool.DataSource
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.slf4j.LoggerFactory
import com.github.takezoe.scala.jdbc._
import com.github.takezoe.scala.jdbc.jdbc._
import org.json4s._
import org.json4s.native.JsonMethods._

// mysql -A -h172.21.0.67 -P3307 -uidmm3 -pidmm3 idmmys
class QryIDHandler extends AbstractHandler with ContextPathTrait{
  val log = LoggerFactory.getLogger("dbexcept")

  @Resource
  var ds: DataSource = null

  override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType("text/html; charset=utf-8");
    val out = response.getWriter

    out.println("""<html><header>
        <link href="../resource/style.css" rel="stylesheet" type="text/css" />
        <title>idmm check</title></header>
        <body>""")

    out.println( "<h1>&sect; 消息查询：</h1> </br>")
    out.println(
      """
        <form method="GET" name="query">
        <label>消息ID: </label> <input type="text" name="id" size="70" /> <br />
        <input type="submit" value="查询" />
        </form>
      """)

    val id = httpServletRequest.getParameter("id")
    if(id == null){
      out.println("")
    }else {
      val ids = id.split("::").takeRight(2)
      val tbl_suffix_idx = ids(0)
      val tbl_suffix_body = ids(1) //""" + tbl_suffix_idx + """


      case class MsgIdx1(id: String, cli: String, topic: String, create_time: Long, consume_time: Long, retry: Int)

      DB.autoClose(ds.getConnection) { db =>
        out.println("<p/> ----index:<br />")
        out.println("<b>Cols</b>: idmm_msg_id, dst_cli_id,dst_topic_id, create_time, commit_time-create_time, consumer_resend <br />")
        db.select(SqlTemplate(
          s"""select idmm_msg_id, dst_cli_id,dst_topic_id, create_time, commit_time-create_time,
        consumer_resend from msgidx_part_${tbl_suffix_idx} where idmm_msg_id=?""", id), MsgIdx1.apply _).map { mi =>
          out.println(mi.id, mi.cli, mi.topic, mi.create_time, mi.consume_time, mi.retry)
          out.println(s"<br />创建时间: ${new java.util.Date(mi.create_time).toString}")
          out.println(s"<br />消费提交时间: ${new java.util.Date(mi.create_time+mi.consume_time).toString}")
        }

        out.println("<p/> ----error:<br />")
        out.println("<b>Cols</b>: idmm_msg_id, dst_cli_id,dst_topic_id, create_time, consumer_resend <br />")
        db.select(sql"select idmm_msg_id, dst_cli_id,dst_topic_id, create_time, 0, consumer_resend from msgidx_part_err where idmm_msg_id=$id",
          MsgIdx1.apply _)
          .map { mi =>
            out.println(mi.id, mi.cli, mi.topic, mi.create_time, mi.retry)
            out.println(s"<br />创建时间: ${new java.util.Date(mi.create_time).toString}")
          }

        out.println("<p/> ----body:<br />")

        val mb = db.selectFirst(SqlTemplate(s"select id, properties, length(content), content from messagestore_${tbl_suffix_body} where id=?", id) ) { rs =>
          val stream = rs.getBinaryStream(4)
          val length = rs.getInt(3)
          println(s"===content length=${length}")
          val bytes = new Array[Byte](length)
          stream.read(bytes)
          (rs.getString(1), rs.getString(2), bytes)
        }

        if(mb != None) {
          val m = mb.get
          out.print("<br /> <b>ID:</b>")
          out.println(m._1)
          out.print("<br /> <b>Properties:</b>")
          out.println(m._2)
          out.print("<br /> <b>Content:</b>")
          implicit val formats = DefaultFormats
          val jp = parse(m._2).extract[Map[String, AnyVal]]
          if (jp.get("compress") != None) {
            if(jp.get("compress").get.asInstanceOf[Boolean]) {
              println("------compressed")
              out.println(new String(GZCompress.gunzipFromBytes(m._3) ))
            }else {
              out.println(new String(m._3))
            }
          } else {
            out.println(new String(m._3))
          }
        }
      }
    }

    out.println("</body></html>" )
    request.setHandled(true)
  }

}

object GZCompress {
  def gzipToBytes(plaintext: Array[Byte]): Array[Byte] = {
    if (plaintext == null || plaintext.length == 0) return new Array[Byte](0)
    val out = new ByteArrayOutputStream
    val gzip = new GZIPOutputStream(out)
    gzip.write(plaintext)
    gzip.close()
    out.toByteArray
  }

  def gunzipFromBytes(ciphertext: Array[Byte]): Array[Byte] = {
    if (ciphertext == null || ciphertext.length == 0) return new Array[Byte](0)
    val out = new ByteArrayOutputStream
    val in = new ByteArrayInputStream(ciphertext)
    val gunzip = new GZIPInputStream(in)
    val buffer = new Array[Byte](256)
    var n = 1
    while (n > 0) {
      n = gunzip.read(buffer)
      if(n > 0)
        out.write(buffer, 0, n)
    }
    out.toByteArray
  }
}
