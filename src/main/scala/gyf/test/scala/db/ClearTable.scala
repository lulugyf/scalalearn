package gyf.test.scala.db

import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Properties
import java.util.concurrent.{Executors, TimeUnit}

import com.github.takezoe.scala.jdbc.{DB, SqlTemplate}
import gyf.test.scala.ZKUtil
import javax.sql.DataSource
import org.apache.curator.framework.CuratorFramework
import org.apache.tomcat.jdbc.pool.{PoolProperties, DataSource => TDataSource}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JObject
import org.json4s.native.JsonMethods.parse

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/*
jdbc.driverClassName = oracle.jdbc.driver.OracleDriver
jdbc.url = jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.120)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.122)(PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.181.124)(PORT=1521)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))
jdbc.username = IDMMOPR
jdbc.password = ykRwj_b6
jdbc.filters = state
jdbc.maxActive = 20
jdbc.initialSize = 20

jdbc.driverClassName = com.mysql.jdbc.Driver
jdbc.url = jdbc:mysql://172.21.0.67:3307/idmmys?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true&cachePrepStmts=true&autoReconnect=true&socketTimeout=10000
jdbc.username = idmm3
jdbc.password = idmm3
jdbc.filters = state
jdbc.maxActive = 5
jdbc.initialSize = 5

mysql -A -h 172.21.0.67 -P 3307 -uidmm3 -pidmm3 idmmys

ClearTable fq.properties <idx_count> <body_count>

fsc -d target/classes src\main\scala\gyf\test\scala\db\ClearTable.scala

cp=""
for f in lib/  *.jar
do
        cp="$cp:$f"
done
export CLASSPATH="$cp"

java gyf.test.scala.db.ClearTable --cmd clear_all --db config_fq/app.properties --idx_end 200 --bdy_end 1000 --thread-count 20
java gyf.test.scala.db.ClearTable --cmd clear_all --db config_xq/app.properties --idx_end 200 --bdy_end 1000 --thread-count 20

 */
object ClearTable {

  def main(args: Array[String]): Unit = {
    import scopt._

    val parser = new scopt.OptionParser[Config]("clear") {
      head("Clear tools", "0.1.0")

      opt[String]('c', "cmd").action( (x, c) =>
        c.copy(cmd = x) ).text("Command, [clear_one] or clear_all")

      opt[String]('d', "db").action( (x, c) =>
        c.copy(db_props = x) ).text("db properties file")

      opt[Int]('i', "idx_end").action( (x, c) =>
        c.copy(idx_end = x) ).text("index table suffix end")

      opt[Int]('b', "bdy_end").action( (x, c) =>
        c.copy(bdy_end = x) ).text("body table suffix end")

      opt[Int]('t', "thread-count").action( (x, c) =>
        c.copy(thread_count = x) ).text("threads count to execute")
    }
    val a = Array[String]("--cmd", "clear_one", "--db", "config_fq/app.properties")

    parser.parse(args, Config()) match {
      case Some(config) => {
        println(s"command: ${config.cmd} ${config.db_props}")
        if(config.cmd == "clear_all") {
          clearAllTable(config)
        }
      }
      case None => }
  }

  case class BleInfo(id: String, addr: String, jmx: String, ctime: String)
  case class Info(target_client_id: String, total: Int, size: Int, err: Int, target_topic_id: String,
                   sending: Int, status: String, var bleid: String="")
  case class Config(cmd: String="clear_one", db_props: String="", idx_end: Int=200, bdy_end: Int=1000, idx_begin: Int=0, bdy_begin: Int=0,
                    topic: String="", client: String="", thread_count: Int=20)

  implicit val ec = new ExecutionContext { // use own defined ExcutionContext
    val threadPool = Executors.newFixedThreadPool(5);
    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }
    def reportFailure(t: Throwable) {}
  }

  def test1(args: Array[String]): Unit = {
    val zkAddr = "10.113.172.56:8671,10.113.172.57:8671,10.113.172.58:8671,10.112.185.2:8671,10.112.185.3:8671"

    val zk = new ZKUtil().createCurator(zkAddr)
    val lst = bleList("/idmm/ble", zk).map{ ble =>
      Future{
        qinfo(ble.jmx, ble.id)
      }
    }
    val rets = Await.result(Future.sequence(lst.toSeq),  5.minutes)
    zk.close
    ec.threadPool.shutdownNow()

    for(r <- rets.flatten.toArray.sortWith{(a,b) => {
      if(a.size>b.size)
        true
      else if(a.total > b.total)
        true
      else
        false
    }}) {
      println(s" == ${r.bleid} ${r.target_client_id} ${r.target_topic_id} ${r.total} ${r.size}")
    }
  }


  def clearAllTable(conf: Config): Unit = {
    println("begin clear all table...")
    val propsFile = conf.db_props

    val props = new Properties()
    props.load(new FileReader(propsFile))

    val ds = createDS(props)

    //import scala.concurrent.ExecutionContext.Implicits.global  // use implicit global
    implicit val ec = new ExecutionContext { // use own defined ExcutionContext
      val threadPool = Executors.newFixedThreadPool(conf.thread_count);
      def execute(runnable: Runnable) {
        threadPool.submit(runnable)
      }
      def reportFailure(t: Throwable) {}
    }

    val futs1 = (conf.bdy_begin to conf.bdy_end-1).map { i =>
      val fut = Future {
        DB.autoClose(ds.getConnection) { db =>
          println(s"truncate table messagestore_${i} ${Thread.currentThread().getName()}")
          db.update(SqlTemplate(s"truncate table messagestore_${i}"))
          s"body ${i} done"
        }
      }
      fut.onComplete {
        case Success(value) => println(value)
        case Failure(e) => println(s"body ${i} failed ${e.getMessage}")
      }
      fut
    }

    val futs2 = (conf.idx_begin to conf.idx_end-1).map { i =>
      val fut = Future {
        DB.autoClose(ds.getConnection) { db =>
          println(s"truncate table msgidx_part_${i}")
          db.update(SqlTemplate(s"truncate table msgidx_part_${i}"))
          s"index ${i} done"
        }
      }
      fut.onComplete {
        case Success(value) => println(value)
        case Failure(e) => println(s"index ${i} failed ${e.getMessage}")
      }
      fut
    }
    val futs3 = Array(
      Future{DB.autoClose(ds.getConnection) { db =>
        db.update(SqlTemplate(s"truncate table msgidx_part_err"))
        s"index err done"
      }},
      Future{DB.autoClose(ds.getConnection) { db =>
        db.update(SqlTemplate(s"truncate table ble_not_found"))
        s"ble_not_found done"
      }}
    )

    Await.result(Future.sequence(futs1 ++ futs2 ++ futs3), 10.minutes)


    ds.close(true)

    println("clear Tables done")

    // clearAllQueue(props.getProperty("curator.zookeeper.addr"))

    ec.threadPool.shutdown()
  }


  val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

  def bleList(path: String, zk: CuratorFramework): Array[BleInfo] = {
    zk.getChildren.forPath(path).toArray.map{ p =>
      val state = zk.checkExists.forPath(s"$path/$p")
      // 10.113.182.101:21052 jolokia-port:21152
      val data = new String(zk.getData.forPath(s"$path/$p")).split("[ :]")
      BleInfo(p.toString.substring(3), s"${data(0)}:${data(1)}", s"${data(0)}:${data(3)}", fmt.format(state.getCtime))
    }
  }

  def get(url: String) = scala.io.Source.fromURL(url).mkString

  def qinfo(jmxaddr: String, bleid: String): Array[Info] = {
    //   val jmxaddr = "172.21.0.46:7002"
    val result = get(s"http://${jmxaddr}/jolokia/exec/com.sitech.crmpd.idmm.ble.RunTime:name=runTime/info")

    implicit val formats = DefaultFormats
    val j = parse(result)
    val lst = parse((j \ "value").values.toString).extract[Array[Info]]
    if(bleid != "")
      lst.map{l => l.bleid = bleid; l}
    else
      lst
//    val m = j.extract[Map[String, Any]]
//    m.asInstanceOf[JObject].extract[Array[QInfo]]
//    parse(m("value").asInstanceOf[String]).extract[Array[QInfo]]
  }

  def clearAllQueue(zkAddr: String): Unit = {
    val zk = new ZKUtil().createCurator(zkAddr)
    bleList("/idmm/ble", zk).foreach{ ble =>
      qinfo(ble.jmx, ble.id).foreach{ q =>
        println(s"== ${q.target_client_id}  ${q.target_topic_id}")
        if(q.total > 0){
          val url = s"http://${ble.jmx}/jolokia/exec/clear/${q.target_client_id}/${q.target_topic_id}"
          println(url)
          get(url)
        }
      }
    }
    zk.close
  }


  def createDS(props: Properties) : TDataSource = {
    val config = new PoolProperties
    val minsize = props.getProperty("jdbc.initialSize").toInt
    config.setDriverClassName(props.getProperty("jdbc.driverClassName"))
    config.setUrl(props.getProperty("jdbc.url"))
    config.setUsername(props.getProperty("jdbc.username"))
    config.setPassword(props.getProperty("jdbc.password"))
    config.setMaxActive(props.getProperty("jdbc.maxActive").toInt)
    config.setInitialSize(minsize)
    config.setTestWhileIdle(true)
    config.setValidationQuery("")
    config.setJmxEnabled(true)
    config.setMinIdle(minsize)
    config.setMaxIdle(minsize)
    config.setLogAbandoned(true)
    config.setRemoveAbandoned(true)

    new TDataSource(config)
  }

}
