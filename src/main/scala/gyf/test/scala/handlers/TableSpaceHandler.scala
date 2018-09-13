package gyf.test.scala.handlers

import com.github.takezoe.scala.jdbc.DB
import javax.annotation.Resource
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.tomcat.jdbc.pool.DataSource
import org.beetl.core.GroupTemplate
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import com.github.takezoe.scala.jdbc.jdbc._

import scala.beans.BeanProperty

class TableSpaceHandler extends AbstractPathHandler{
  val log = LoggerFactory.getLogger("dbexcept")

  @Resource
  var ds: DataSource = null

  @Resource
  val templates: GroupTemplate = null

  override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, response: HttpServletResponse): Unit = {
    response.setContentType("text/html; charset=utf-8")
    val out = response.getWriter

    val t = templates.getTemplate("tablespace.html")

    case class TsInfo(@BeanProperty tsname: String, @BeanProperty pct: String, @BeanProperty free: String)

    val tsList = DB.autoClose(ds.getConnection) { db =>
      db.select(
        sql"""SELECT NVL(b.tablespace_name,nvl(a.tablespace_name,'UNKOWN')) name,
                     ((kbytes_alloc-NVL(kbytes_free,0))/kbytes_alloc)*100   pct_used, Kbytes_free
                     FROM   ( SELECT   SUM(bytes)/1024 Kbytes_free
                                     , MAX(bytes)/1024 largest
                                     , tablespace_name
                              FROM sys.dba_free_space
                              GROUP BY tablespace_name
                            ) a
                          , ( SELECT   SUM(bytes)/1024 Kbytes_alloc
                                     , tablespace_name
                              FROM sys.dba_data_files
                              GROUP BY tablespace_name
                            ) b
                     WHERE a.tablespace_name (+) = b.tablespace_name
        """) { rs =>
        TsInfo(rs.getString(1),
          f"${rs.getDouble(2)}%.4f",
          f"${rs.getLong(3)/1024/1024.0}%.3f")
      }
    }
    t.binding("tsList", tsList.iterator.asJava)
    t.renderTo(out)

    request.setHandled(true)
  }
}
