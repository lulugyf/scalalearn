package gyf.test.scala.handlers

import scala.beans.BeanProperty

trait ContextPathTrait {
  @BeanProperty var contextPath: String = "/"
  @BeanProperty var contextDesc: String = "handler description"
  def path(): String = contextPath
  def desc(): String = contextDesc
}
