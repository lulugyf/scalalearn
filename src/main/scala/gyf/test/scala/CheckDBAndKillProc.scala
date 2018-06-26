package gyf.test.scala

import java.io.{FileReader, PrintWriter}
import java.sql.DriverManager
import java.util.Properties

import com.jcraft.jsch.Session
import com.sitech.crmpd.idmm.encrypt.SC65DecryptDB
import gyf.test.scala.utils.RemoteSSH._

import scala.io.Source


/**
  *  四川idmmdb 数据库3实例 检查, 避免因为数据库主机 ib网卡同时down的情况下
  *    可能hang死数据库连接导致业务不能自动恢复的情况
  *
  *   测试规则:
  *   1. 3个实例分别连接, 有任何一个实例连接失败, 满足条件
  *
  *   动作:
  *   1. 距离上次重启超过5分钟
  *   2. 向本集群6个主机执行 /idmm/jdk1.8.0_131/bin/jps -ml|grep -e App -e BLEServ|awk '{print $1}'|xargs kill
  *   3. 等待 各自的crontab自动重启进程
  *
  *
  * 配置文件格式:
  *
  * db.file=/path/to/.ngdbpassword/file
  * db.sec=IDMMOPR
  * db.user=IDMMOPR
  * db.url.0=???
  * db.url.1=???
  * db.driver=???
  *
  * host.file=
  * host.keyfile=
  * host.cmd=/idmm/jdk1.8.0_131/bin/jps -ml|grep -e App -e BLEServ|awk '{print $1}'|xargs kill
  *
  *
  * host.file 的格式如下:
  * fqidmm1,idmm,10.113.181.86[:22]
  * ...
  */

object CheckDBAndKillProc {
  def main(args: Array[String]): Unit = {
    val propfile = args(0)
    val props = new Properties()
    props.load(new FileReader(propfile))

    val checkret = checkDB(props)

    if(checkret == 2){
      println(s"checkDB return ${checkret}, begin to kill all processes")
      killAll(props)
    }else{
      println(s"checkDB return ${checkret}, nothing to do.")
    }
  }

  def checkDB(props: Properties): Int = {
    val dbpasswd = SC65DecryptDB.decPass(props.getProperty("db.file"), props.getProperty("db.sec"))
    val dbuser = props.getProperty("db.user")
    val urls = 0.to(10).map { i =>
      props.getProperty(s"db.url.${i}")
    }.filter(_ != null)

    val driver = props.getProperty("db.driver")
    try {
      Class.forName(driver)
    }catch{
      case e:Throwable =>
        println(s"load driver failed: ${e.getMessage} ${driver}")
        return 1
    }
    urls.map{url =>
      try{
        val c = DriverManager.getConnection(url, dbuser, dbpasswd)
        c.close
      }catch{
        case e: Throwable =>
          println(s"connect to ${url} failed ${e.getMessage}")
          return 2
      }
    }
    return 0
  }

  /**
    * 向集群主机发送指令
    * @param props
    */
  def killAll(props: Properties): Unit = {
    val cmd = props.getProperty("host.cmd")
    val hostfile = props.getProperty("host.file")
    val keyfile = props.getProperty("host.keyfile")

    val fis = Source.fromFile(hostfile)
    val lines = fis.getLines().toArray
    println(s"---- lines len ${lines.length}")
    fis.close()

    lines.foreach{line =>
      val cmdret = one_host(line, keyfile, cmd )
      println(s"=====${cmdret._1}")
      println(cmdret._2)
    }
  }

  def one_host(host_str: String, keyFile: String, cmd: String): Tuple2[String, String] = {
    val s = host_str.split(",")
    val hostname = s(0)
    val user=s(1)
    val host=s(2)
    var session: Session = null
    try{
      session = createSession(s"${user}@${host}", keyFile)

      val r = ssh_exec(session, cmd)
      return (hostname, r._1 + r._2)
    }finally{
      if(session != null) session.disconnect()
    }
    (hostname, "failed")
  }

}
