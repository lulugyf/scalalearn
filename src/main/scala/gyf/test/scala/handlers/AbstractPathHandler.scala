package gyf.test.scala.handlers

import org.eclipse.jetty.server.handler.AbstractHandler

import scala.beans.BeanProperty

abstract class AbstractPathHandler extends AbstractHandler{
  @BeanProperty var contextPath: String = "/"
  @BeanProperty var contextDesc: String = "handler description"
  def path(): String = contextPath
  def desc(): String = contextDesc
}
