package gyf.test.scala.db

import javax.annotation.Resource
import org.springframework.context.annotation.{Bean, Configuration}

class PassDecryptorBean {
  @Resource
  var decryptor: PassDecryptor = null

  @Bean
  def dbPass(): String = {
    val p = decryptor.getPassword()
    println(s"PassDecryptorBean === ${p}")
    p
  }

  @Bean
  def dbUser(): String = {
    decryptor.getUserName()
  }

}
