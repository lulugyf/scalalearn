package gyf.test.scala

import gyf.test.scala.handlers.ContextPathTrait
import javax.annotation.{PostConstruct, Resource}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.beetl.core.GroupTemplate
import org.eclipse.jetty.server.{Handler, Request, Server}
import org.eclipse.jetty.server.handler.{AbstractHandler, ContextHandler, ContextHandlerCollection, ResourceHandler}
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import scala.beans.BeanProperty

@Component("mainServer")
class WebEntry {

  @Value("${server.port:8080}")
  var port: Int = 0
  var serv: Server = null

  @Value("${resource.dir:resource}")
  var resource_dir: String = null

  @Resource(name="handlers")
  var handlers: java.util.ArrayList[AbstractHandler] = null
  @Resource
  val templates: GroupTemplate = null

  case class Entry(pathTrait: ContextPathTrait, handler: AbstractHandler)
  var entries: Array[Entry] = null


  @PostConstruct
  def server(): Unit = {
    val server = new Server(port)

    val resource_handler = new ResourceHandler
    resource_handler.setDirectoriesListed(true)
    resource_handler.setWelcomeFiles(Array[String]("index.html"))
    resource_handler.setResourceBase(resource_dir)

    entries = handlers.toArray.map{ h =>
      Entry(h.asInstanceOf[ContextPathTrait], h.asInstanceOf[AbstractHandler])
    }

    val contexts = new ContextHandlerCollection
    val allHandlers = entries.map{ entry =>
      addContext(entry.pathTrait.path, entry.handler)
    } ++ List[Handler](
      addContext("/resource", resource_handler),
      addContext("/", new RootHandler(entries, templates))
    )

    contexts.setHandlers(allHandlers.toArray)

    server.setHandler(contexts)

    this.serv = server
  }

  def start(): Unit = {
    serv.start
    serv.join
  }


  private def addContext(contextPath: String, handler: Handler) = {
    val context = new ContextHandler(contextPath)
    context.setHandler(handler)
    context
  }

  class RootHandler(entries: Array[Entry], templates: GroupTemplate) extends AbstractHandler {
    override def handle(s: String, request: Request, httpServletRequest: HttpServletRequest, response: HttpServletResponse): Unit = {
      response.setContentType("text/html; charset=utf-8");
      val out = response.getWriter

      val t = templates.getTemplate("index.html")
      t.binding("from_here", "beetl")
//      t.binding("entList", entries.map{e =>
//          s"""<li> <a href="${e.pathTrait.path}"> link </a> ${e.pathTrait.desc} <br /> </li>"""}.iterator.asJava)
      case class Ent(@BeanProperty path: String, @BeanProperty desc: String)
      import collection.JavaConverters._
      t.binding("entList", entries.map{e =>Ent(e.pathTrait.path, e.pathTrait.desc)} .iterator.asJava)
      t.renderTo(out)

      request.setHandled(true)
    }
  }
}
