package gyf.test.scala

import org.springframework.context.support.ClassPathXmlApplicationContext

// mvn dependency:copy-den

object Main extends App {
  val app = new ClassPathXmlApplicationContext("jettyweb_spring.xml")

  val entry = app.getBean("mainServer").asInstanceOf[WebEntry]

  entry.start
}
