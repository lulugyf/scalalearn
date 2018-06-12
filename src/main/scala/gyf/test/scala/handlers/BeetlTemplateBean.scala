package gyf.test.scala.handlers

import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.beans.factory.annotation.Value
import org.beetl.core.{Configuration => btConfiguration}
import org.beetl.core.GroupTemplate
import org.beetl.core.resource.{FileResourceLoader, StringTemplateResourceLoader}

@Configuration
class BeetlTemplateBean {

  @Value("${template.path:resource/templates}")
  val template_path: String = null

  @Bean
  def pageTemplate(): GroupTemplate = {
    val cfg = btConfiguration.defaultConfiguration
    val resourceLoader = new FileResourceLoader(template_path, "utf-8")
    new GroupTemplate(resourceLoader, cfg)
  }

  def strTemplate(): GroupTemplate = {
    val cfg = btConfiguration.defaultConfiguration
    new GroupTemplate(new StringTemplateResourceLoader(), cfg)
  }
}
