package gyf.test.scala.utils

import java.io._
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.atomic.AtomicInteger

import com.jcraft.jsch._
import org.json4s.{DefaultFormats, JObject}


//import com.jcraft.jsch.ChannelExec

object RemoteSSH {
  def main(args: Array[String]): Unit = {
//    val session = createSession("idmm@10.113.182.96:21101", "resource/ssh_identity")
//    val session = createSession("guanyf@127.0.0.1:8022", "resource/ssh_identity")
    val session = createSession("idmm@10.113.182.96:21104", "conf/ssh_identity")

//    test_shell(session)

//    scp_from(session, "/tmp/HP*.pdf", "d:/tmp/2")
//    scp_from(session, "/tmp/HPx360-15m.pdf", "d:/tmp/2/HPx360-15m.pdf")

//    scp_to(session, "/tmp", "D:/tmp/jstack2.log")
//    scp_to(session, "/tmp", "D:/tmp/2/HP1.pdf")

//    portforwarding_L(session)

    //scp_send_one_file(session, "/tmp/abc/123/jstack2.log", "d:/tmp/jstack2.log")

    get_process_mon(session).foreach(p =>
    println(p.dataSource)
    )
//    test_exec1(session)

    session.disconnect()
  }

  def test_exec1(session: Session): Unit = {
    val jhome="/idmm/jdk1.8.0_131"
    val cmd =
      s"""${jhome}/bin/jps -l|grep -e App|while read pid class; do echo "##PS:"; ls -ld /proc/$$pid;
         |cwd=`ls -l /proc/$$pid/cwd|awk '{print $$NF}'`;
         |jmxport=`grep jolokia.port $$cwd/config/broker/*.properties|awk -F "=" '{print $$NF}'`;
         |listenport=`grep netty.listen.port $$cwd/config/broker/*.properties|awk -F "=" '{print $$NF}'`;
         |echo "#%CWD: $$pid $$jmxport $$cwd $$class $$listenport";
         |echo "#%LSOF:"; /usr/sbin/lsof -p $$pid -P|grep TCP|awk '{print $$(NF-1), $$NF}';
         |echo "#%MEM:"; curl "http://localhost:$$jmxport/jolokia/read/java.lang:type=Memory" 2>/dev/null; echo "";
         |echo "#%DATASOURCE:"; curl "http://localhost:$$jmxport/jolokia/exec/com.sitech.crmpd.idmm:name=dsMon/dsinfo" 2>/dev/null; echo "";
         |done""".stripMargin
    val ps = ssh_exec(session, cmd) match {
      case (stdout: String, stderr: String) => {
        println(stdout)
      }
    }
  }

 def get_process_mon(session: Session): Array[ProcessInfo] = {
    val jhome="/idmm/jdk1.8.0_131"
    val cmd =
      s"""${jhome}/bin/jps -l|grep -e App|while read pid class; do echo "##PS:"; ls -ld /proc/$$pid;
         |cwd=`ls -l /proc/$$pid/cwd|awk '{print $$NF}'`;
         |jmxport=`grep jolokia.port $$cwd/config/broker/*.properties|awk -F "=" '{print $$NF}'`;
         |listenport=`grep netty.listen.port $$cwd/config/broker/*.properties|awk -F "=" '{print $$NF}'`;
         |echo "#%CWD: $$pid $$jmxport $$cwd $$class $$listenport";
         |echo "#%LSOF:"; /usr/sbin/lsof -p $$pid -P|grep TCP|awk '{print $$(NF-1), $$NF}';
         |echo "#%MEM:"; curl "http://localhost:$$jmxport/jolokia/read/java.lang:type=Memory" 2>/dev/null; echo "";
         |echo "#%DATASOURCE:"; curl "http://localhost:$$jmxport/jolokia/exec/com.sitech.crmpd.idmm:name=dsMon/dsinfo" 2>/dev/null; echo "";
         |done""".stripMargin
    val ps = ssh_exec(session, cmd) match {
      case (stdout: String, stderr: String) =>{
//         println(stdout)
        var idx = 0
        stdout.split("##").map{ block=>
          idx = idx + 1
          val pi = new ProcessInfo(idx.toString)
          block.split("#%").filter(_.indexOf(':')>0).map{ss =>
          val p=ss.indexOf(':')
          val ctype=ss.substring(0, p)
          val cvalue=ss.substring(p+1).trim
          ctype match {
            case "PS" => pi.start_time = cvalue.split(" +").slice(5,8).mkString(" ")
            case "CWD" =>
              val v = cvalue.split(" +")
              pi.pid = v(0)
              pi.jmxport=v(1)
              pi.cwd = v(2)
              pi.args = v(3)
              pi.listenport=v(4)
            case "MEM" =>
              implicit val formats = DefaultFormats
              import org.json4s.native.JsonMethods.parse
              val j = parse(cvalue)
              val m = ( j \ "value" \ "HeapMemoryUsage").asInstanceOf[JObject].extract[MemUsage]
              pi.mem = m.toString()
            case "DATASOURCE" =>
              implicit val formats = DefaultFormats
              import org.json4s.native.JsonMethods.parse
              val j = parse(cvalue)
              if(cmd.indexOf("/config/ble/") > 0) {
                val m1 = (j \ "value" \ "dsConfig").asInstanceOf[JObject].extract[DSInfo]
                val m2 = (j \ "value" \ "dataSource").asInstanceOf[JObject].extract[DSInfo]
                pi.dataSource = m1.toString() + ";" + m2.toString()
              }else if(cmd.indexOf("/config/broker/") > 0){
                pi.dataSource = (j \ "value" \ "dataSource").asInstanceOf[JObject].extract[DSInfo].toString
              }
            case "LSOF" =>
              pi.conns = __conns(cvalue)
          }
        }
          pi
        }
      }

    }
    ps
  }
  def __conns(cvalue: String): String = {
    val lines = cvalue.split("\n")
    val listent_ports = lines.filter(_.indexOf("LISTEN") > 0).map{ s =>
      s.split("[: ]")(1)
    }
    import scala.collection.mutable
    val income = new mutable.HashMap[String, Int]()
    val outgoing = new mutable.HashMap[String, Int]()
    lines.filter(_.indexOf("ESTABLISHED") > 0).foreach{s =>
      // xqidmm1:9621->xqidmm2:45623 (ESTABLISHED)
      val fv = s.split("[-: >]")
      if(listent_ports.contains(fv(1))){ //外来连接
        val k = s"${fv(1)}<-${fv(3)}"
        income(k) = income.getOrElse(k, 0) + 1
      }else{
        val k = s"${fv(3)}:${fv(4)}"
        outgoing(k) = outgoing.getOrElse(k, 0) + 1
      }
    }
    "IN:\n" + income.mkString("\n") +"\nOUT:\n" + outgoing.mkString("\n")
  }
  case class ProcessInfo(id: String){
    var pid: String=null
    var start_time: String=null
    var cwd = ""
    var jmxport=""
    var args: String=null
    var listenport=""

    var mem = ""
    var dataSource = ""
    var conns = ""
  }
  case class MemUsage(init: Long, committed: Long, max: Long, used: Long){
    override def toString(): String = f"${used/1024/1024} MB / ${max/1024/1024} MB == ${used * 100.0/max}%2.2f %%"
  }
  case class DSInfo(idle: Int, active: Int, maxActive: Int, className: String, props: String){
    override def toString(): String = s"active: ${active} / idle: ${idle} / maxActive: ${maxActive}"
  }

  /**
    *
    * @param session  ssh连接session
    * @param rfile 必须指定远端文件名， 而非目录名
    * @param lfile 本地文件名
    * @return
    */
  def scp_send_one_file(session: Session, rfile: String, lfile: String): Boolean = {
    val r = scp_to(session, rfile, lfile)
    if (r != 0){
      // 尝试建立目录后重试
      val path = rfile.substring(0, rfile.lastIndexOf('/'))
      ssh_exec(session, s"mkdir -p ${path}")
      val r1 = scp_to(session, rfile, lfile)
      return r1 == 0
    }else{
      return true
    }
    false
  }

  // 要求实现当对端目录不存在而报错时, 自动调用 exec 建立目录, 重试一次
  def scp_to(session: Session, rfile: String, lfile: String) : Int = {
    val command = "scp -t " + rfile
    val channel = session.openChannel("exec")
    (channel.asInstanceOf[ChannelExec]).setCommand(command)

    val out = channel.getOutputStream
    val in = channel.getInputStream
    channel.connect

    if(scp_checkArk(in) != 0){
      channel.disconnect()
      return -1
    }

    val _file = new File(lfile)
    var filesize = _file.length()
    val _fullFilePath = _file.getAbsolutePath
    val lfilename = if(_fullFilePath.lastIndexOf(File.separator) > 0) _fullFilePath.substring(_fullFilePath.lastIndexOf(File.separator)+1)
    else _fullFilePath
    val command1 = "C0644 "+filesize + " " + lfilename + "\n"
//    print("send: "+  command1)
    out.write(command1.getBytes()); out.flush()
    if(scp_checkArk(in) != 0){
      channel.disconnect()
      println("0644 failed, disconnected")
      return -2
    }

    val buf = new Array[Byte](1024)
    val fis = new FileInputStream(_file)
    var loop = true
    while(loop){
      val len = fis.read(buf)
      if(len <= 0){
        loop = false
      }else{
        out.write(buf, 0, len)
      }
    }
    fis.close()
    buf(0) = 0; out.write(buf, 0, 1); out.flush(); // send \0
    if(scp_checkArk(in) != 0){
      channel.disconnect()
      return -3
    }
    out.close()

    channel.disconnect()
//    println("scp_to done!")
    0
  }

  /* scp transfer remote file to local
  * val lines = Iterator.continually(reader.readLine()).takeWhile(_ != null).mkString
  * */
  def scp_from(session: Session, rfile: String, lfile: String): Unit = {

    val command = "scp -f " + rfile
    val channel = session.openChannel("exec")
    (channel.asInstanceOf[ChannelExec]).setCommand(command)

    // get I/O streams for remote scp
    val out = channel.getOutputStream
    val in = channel.getInputStream
    channel.connect

    val buf = new Array[Byte](1024)
    // send '\0'
    buf(0) = 0; out.write(buf, 0, 1); out.flush();
    var loop  = true
    while(loop){ // maybe more than one file
      val c = scp_checkArk(in)
      if(c == 'C'){
        // read '0644 '
        in.read(buf, 0, 5)
        println("read 0644: "+new String(buf, 0, 5))
        var filesize = scp_readUntil(in, ' ').toLong
        val file = scp_readUntil(in, 0x0a.toChar)

        println("filesize: "+filesize + " file: "+file)

        // send '\0'
        buf(0)=0; out.write(buf, 0, 1); out.flush();

        val fos = new FileOutputStream(if (new File(lfile).isDirectory) lfile + File.separator + file else lfile)
        while(filesize > 0) {
          val foo: Int = if(filesize < buf.length) filesize.toInt else buf.length
          val n = in.read(buf, 0, foo)
          if(n > 0) {
            fos.write(buf, 0, n)
            filesize = filesize - n
          }else{
            println("read file failed")
            filesize = -1
          }
        }
        fos.close()
        if(scp_checkArk(in) != 0 ) {
          println("Error: checkark failed")
          System.exit(0)
        }
        // send '\0'
        buf(0)=0; out.write(buf, 0, 1); out.flush();

     }else{
        loop = false
      }
    }
    channel.disconnect()
    println("finished")
  }

  def scp_readUntil(in: InputStream, c: Char): String = {
    val sb = new StringBuffer
    var v: Int = -1
    var loop = true
    while({ v=in.read; v >= 0 && v != c}) sb.append(v.toChar)
    return sb.toString
  }

  def scp_checkArk(in: InputStream): Int = {
    val b = in.read
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
//    println("scp_checkArk read = " + b + " " + b.toChar)
    if (b == 0) return b
    if (b == -1) return b

    if (b == 1 || b == 2) {
      val sb = new StringBuffer
      var c = 0
      do {
        c = in.read
        sb.append(c.toChar)
      } while ( c != '\n')
      if (b == 1) { // error
        System.out.print("ERR 1:" + sb.toString)
      }
      if (b == 2) { // fatal error
        System.out.print("ERR 2:" +sb.toString)
      }
    }
    b
  }


  // 端口转发， lport: 本地监听端口， rhost:rport - 映射到远端的端口地址
  def portforwarding_L(session: Session): Unit ={
    val lport = 13306
    val rhost = "127.0.0.1"
    val rport = 22
    val assinged_port = session.setPortForwardingL(lport, rhost, rport);
    System.out.println("localhost:"+assinged_port+" -> "+rhost+":"+rport);
    println("begin sleep...")

    Thread.sleep(500000)
  }

  // 端口转发， rport: 远端监听端口， lhost:lport - 映射到本地的端口地址
  // 测试连接mysql失败了, 报错: ERROR 2013 (HY000): Lost connection to MySQL server at 'reading initial communication packet', system error: 0 "Internal error/check (Not system error)"
  // ssh 也不行, 报错: ssh_exchange_identification: Connection closed by remote host
  // 这个实现上可能没有完成或者有bug
  def portforwarding_R(session: Session): Unit ={
    val rport = 13306
    val lhost = "172.21.0.46"
    val lport = 22
    val assinged_port = session.setPortForwardingR(rport, lhost, rport);
    System.out.println(":"+rport+" -> "+lhost+":"+lport);
    println("begin sleep...")

    Thread.sleep(500000)
  }

  /* 远程执行一个命令并打印输出结果 */
  def ssh_exec(session: Session, command: String): Tuple2[String, String] = {
//    val command = "ls -l /"
    val channel = session.openChannel("exec")
    (channel.asInstanceOf[ChannelExec]).setCommand(command)
    channel.setInputStream(null)
    //channel.setOutputStream(System.out);
    val bout = new ByteArrayOutputStream()
    val bout_e = new ByteArrayOutputStream()

    (channel.asInstanceOf[ChannelExec]).setErrStream(bout_e)
    val in = channel.getInputStream
    channel.connect

    val tmp = new Array[Byte](1024)
    while ( ! channel.isClosed ) {
      while ( in.available > 0 ) {
        val i = in.read(tmp, 0, tmp.length)
        if( i > 0){
//          print(new String(tmp, 0, i))
          bout.write(tmp, 0, i)
        }else{
          in.close()
          channel.disconnect()
          println("=== disconnected")
        }
      }
    }
    (bout.toString, bout_e.toString)
  }

  def test_nothing(): Unit = {
    val a = new AtomicInteger()
    val b = new LinkedTransferQueue[Long]()
  }

  /* 交互式的 shell */
  def test_shell(session: Session): Unit = {
    val channel=session.openChannel("shell")
    channel.setInputStream(System.in)
    channel.setOutputStream(System.out)
    channel.connect()
  }

  def createSession(hostStr: String, id: String): Session = {
    val jsch = new JSch
    jsch.addIdentity(id)
    val v = hostStr.split("[@:]")
    val port = if(v.length > 2) v(2).toInt else 22
    val session = jsch.getSession(v(0), v(1), port)
    session.setUserInfo(new MyUserInfo)
    session.connect()
    session
  }

  class MyUserInfo extends UserInfo with UIKeyboardInteractive {
    override def getPassphrase: String = ""

    override def getPassword: String = ""

    override def promptPassword(s: String): Boolean = false

    override def promptPassphrase(s: String): Boolean = false

    override def promptYesNo(s: String): Boolean = true

    override def showMessage(s: String): Unit = println(s"msg>>${s}")

    override def promptKeyboardInteractive(s: String, s1: String, s2: String, strings: Array[String], booleans: Array[Boolean]): Array[String] = null
  }

}
