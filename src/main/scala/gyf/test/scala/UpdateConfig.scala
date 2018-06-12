package gyf.test.scala

/**
  * 批量更新配置文件, 便于集群部署的配置更新
  *
  * 支持统一并相同的配置文件
  *  也支持按不同 位置(主机和目录) 有不同的部分属性的文件的更新, 带 .proplist 的文件中保存对应文件中按位置的属性值
  */
import java.io.{File, FileOutputStream, FileWriter, PrintWriter}
import java.util.concurrent.Executors

import com.jcraft.jsch.Session

import collection.JavaConverters._
import gyf.test.scala.utils.RemoteSSH._
import org.beetl.core.GroupTemplate
import org.beetl.core.exception.BeetlException
import org.beetl.core.resource.StringTemplateResourceLoader
import org.beetl.core.{Configuration => btConfiguration}
import org.json4s._
import org.json4s.native.JsonMethods._

//import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.io.Source

object UpdateConfig {
  implicit val ec = new ExecutionContext { // use own defined ExcutionContext
    val threadPool = Executors.newFixedThreadPool(10);
    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }
    def reportFailure(t: Throwable) {}
  }

  implicit val formats = DefaultFormats

  case class FileTobeUpdate(fname: String, file: File, file_pl: File, modified: Boolean, lastModified: Long)
  case class DeployPos(id: String, host: String, user: String, path: String, authFile: String)

  var tmpdir: String = "tmp"
  var isCheck: Boolean = false
  def main(args: Array[String]): Unit = {
    var basedir = "resource/idmm"
    if(args.length > 0)
      basedir = args(0)
    if(args.length > 1)
      tmpdir = args(1)
    if(args.length > 2 && args(2) == "check")
      isCheck = true;

    val deploy_files = basedir + "/deploy_files.txt"
    val fis = Source.fromFile(deploy_files)
    val modified_files = fis.getLines().map{ line =>
      val fs = line.split(" ")
      val fname = fs(0)
      val lastmodify = fs(1).toLong
      val f = new File(basedir + "/" + fname)
      val fp = new File(basedir + "/" + fname + ".proplist")
      val fp_mod = if(fp.exists()) fp.lastModified() else 0L // 要检查如果存在属性文件， 谁修改最后才算
      val lastmodify_actual = if(f.lastModified() > fp_mod) f.lastModified() else fp_mod
      FileTobeUpdate(fname, f, if(fp.exists()) fp else null, lastmodify_actual > lastmodify, lastmodify_actual)
    }.filter( b => (b.modified ) ).toArray  //|| b.fname.indexOf(".properties")>0
    fis.close()

    val deploy_pos = Source.fromFile(basedir + "/deploy_pos.txt").getLines().map{ line =>
      val fs = line.split(",")
      (fs(0), DeployPos(fs(0), fs(1), fs(2), fs(3), fs(4)) )
    }.toMap



    val futs = modified_files.map { f =>
      println("====" + f.fname + " " + (f.file_pl!=null))
      sendOneFile(f, deploy_pos)
    }.toArray.flatten

    Await.result(Future.sequence(futs.toSeq), 10 minutes).foreach{ m => println("Result: "+ m) }

    if(!isCheck)
      updateLastModified(deploy_files, modified_files)

    ec.threadPool.shutdown()
  }

  // 向所有的目标位置, 上传一个配置文件
  def sendOneFile(f: FileTobeUpdate, pos: Map[String, DeployPos]): Array[Future[String]] = {
    if(f.file_pl == null){
      // 全部位置完全相同的文件, 直接上传
      pos.map{ p =>
        Future{
          sendOnePos(p._2, f.fname, f.file)
        }
      }.toArray
    }else{
      // 按位置有配置的, 读取配置的json文件, 然后生成对应的最终文件, 需要一个临时目录
      //val tmpdir = "d:/tmp/xx"
      val src1 = Source.fromFile(f.file)
      val template_str = src1.getLines().mkString("\n")
      src1.close()

      val template = templateGroup.getTemplate(template_str)

      val src2 = Source.fromFile(f.file_pl)
      val params_str = src2.getLines().mkString("\n")
      src2.close()

      val params = parse(params_str).extract[Map[String, Map[String, String]]]
      pos.map{ p =>
        val posid = p._1
        val tmpfile = new File(tmpdir + "/" + f.fname+"."+posid)
        if(! tmpfile.getParentFile.exists()) {
          tmpfile.getParentFile.mkdirs()
        }
        val fos = new FileOutputStream(tmpfile)
        for(pm <- params.get(posid).get) {
          template.binding(pm._1, pm._2)
//          println(s"${pm._1} === ${pm._2}")
        }
        try {
          template.renderTo(fos)
          Future{ sendOnePos(p._2, f.fname, tmpfile) }
        }catch{
          case be: BeetlException => Future{ s"format failed ${be.getMessage}"}
        }finally {fos.close()}
      }.toArray
    }
  }

  // 向一个目标上传一个文件
  def sendOnePos(pos: DeployPos, fname: String, fsrc: File): String = {
    val tag = s"${pos.user}@${pos.host} ${fsrc.getAbsolutePath} TO ${pos.path}/${fname}"
    if(isCheck)
      return s"${tag} check done"
//    println(tag)
    var session: Session = null
    try{
      session = createSession(s"${pos.user}@${pos.host}", pos.authFile)
      var r = scp_send_one_file(session, s"${pos.path}/${fname}", fsrc.getAbsolutePath)
      return s"${tag} return ${r}"
    }catch{
      case e: Throwable => return s"${tag} failed ${e.getMessage}"
    }finally{
      if(session != null) session.disconnect()
    }
  }

  val templateGroup = strTemplate()

  def strTemplate(): GroupTemplate = {
    val cfg = btConfiguration.defaultConfiguration
    cfg.setPlaceholderStart("#:")
    cfg.setPlaceholderEnd(":#")
    new GroupTemplate(new StringTemplateResourceLoader(), cfg)
  }

  // 更新文件 deploy_files.txt 中的文件修改时间
  def updateLastModified(fname: String, modified: Array[FileTobeUpdate]): Unit = {
    if(modified.length <= 0)
      return
    val _tmpFile = fname+".tmp"
    val fos = new PrintWriter(new FileWriter(_tmpFile))
    val mod = modified.map(f => (f.fname, f.lastModified)).toMap
    val fis = Source.fromFile(fname)
    fis.getLines().foreach { line =>
      val f = line.split(" ")(0)
      //println(s"---- [${f}]")
      if(mod.get(f).isDefined) {
        //println("----" + f + " " + mod.get(f).get)
        fos.println(f + " " + mod.get(f).get)
      }else{
        fos.println(line)
      }
    }
    fos.close()
    fis.close()
    println("delete file return :"+new File(fname).delete())
    println("rename file return: "+ new File(_tmpFile).renameTo(new File(fname)) )
  }
}
