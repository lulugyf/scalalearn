package gyf.test.scala.db

import java.io.FileReader
import java.util.Properties
import java.util.concurrent.Executors

import javax.sql.DataSource
import org.apache.tomcat.jdbc.pool.{PoolProperties, DataSource => TDataSource}
import com.mchange.v2.c3p0.{ComboPooledDataSource => CDataSource}

import scala.concurrent.ExecutionContext
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

object DataSourceTest {

  def main(args: Array[String]): Unit = {
    val thread_count = 40
    val times = 1000000
    val times_per_thread = times / thread_count

    implicit val ec = new ExecutionContext { // use own defined ExcutionContext
      val threadPool = Executors.newFixedThreadPool(thread_count)
      def execute(runnable: Runnable) =  threadPool.submit(runnable)
      def reportFailure(t: Throwable) = {}
    }


    val props = new Properties
    props.load(new FileReader("st.properties"))
//    val ds = createTomcatDS(props)
    val ds = createC3p0DS(props)
    val t1 = System.currentTimeMillis()
    val futs = (0 to thread_count - 1).map{i =>
      Future{
        var i = 0
        while ( {
          i < times_per_thread
        }) {
          ds.getConnection().close()
            i += 1
        }
      }
    }

    Await.result(Future.sequence(futs), 10.minutes)
    val t2 = System.currentTimeMillis()
    closeDS(ds)
    println(s"time: ${t2-t1}")
  }

  def createTomcatDS(props: Properties) : DataSource = {
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

  def createC3p0DS(props: Properties) : DataSource = {
    val minsize = props.getProperty("jdbc.initialSize").toInt

    val ds = new CDataSource
    ds.setDriverClass(props.getProperty("jdbc.driverClassName"))
    ds.setJdbcUrl(props.getProperty("jdbc.url"))
    ds.setUser(props.getProperty("jdbc.username"))
    ds.setPassword(props.getProperty("jdbc.password"))
    ds.setMaxPoolSize(props.getProperty("jdbc.maxActive").toInt)
    ds.setMinPoolSize(minsize)
    ds.setInitialPoolSize(minsize)
    ds.setMaxIdleTime(1800)
    ds.setAcquireIncrement(2)
    ds.setMaxStatements(0)
    ds.setIdleConnectionTestPeriod(1800)
    ds.setAcquireRetryAttempts(30)
    ds.setBreakAfterAcquireFailure(true)
    ds.setTestConnectionOnCheckout(false)
    ds
  }

  def closeDS(ds: DataSource) : Unit = {
    if(ds.isInstanceOf[TDataSource]){
      ds.asInstanceOf[TDataSource].close()
    }else if(ds.isInstanceOf[CDataSource]){
      ds.asInstanceOf[CDataSource].close()
    }
  }

}
