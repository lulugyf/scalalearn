package gyf.test.scala.db

import java.sql.Connection


import javax.annotation.{PostConstruct, PreDestroy}

import org.apache.tomcat.jdbc.pool.{DataSource, PoolProperties}

// mysql -A -h 172.21.0.67 -P 3307 -uidmm3 -pidmm3 idmmys

class MyDataSource {

  var connPool: DataSource = null

  var jdbc_driverClassName = "com.mysql.jdbc.Driver"
  var jdbc_url = "jdbc:mysql://172.21.0.67:3307/idmmys?useUnicode=true&characterEncoding=UTF-8&useServerPrepStmts=true&cachePrepStmts=true&autoReconnect=true&socketTimeout=10000"
  var jdbc_username = "idmm3"
  var jdbc_password= "idmm3"
  var jdbc_filters = "state"
  var jdbc_maxActive = 5
  var jdbc_initialSize = 5

  /*
  var jdbc_driverClassName = "oracle.jdbc.driver.OracleDriver"
  var jdbc_url = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(FAILOVER=ON)(ADDRESS=(PROTOCOL=TCP)(HOST=10.113.182.96)(PORT=21108)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=idmmdb)(FAILOVER_MODE=(TYPE=SELECT)(METHOD=BASIC)(RETRIES=180)(DELAY=5))))"
  var jdbc_username = "idmmopr"
  var jdbc_password= "ykRwj_b6"
  var jdbc_filters = "state"
  var jdbc_maxActive = 5
  var jdbc_initialSize = 5
   */


  @PostConstruct
  private def init(): Unit = {
    //Class.forName("com.mysql.jdbc.Driver")

    val config = new PoolProperties
    config.setDriverClassName(jdbc_driverClassName)
    config.setUrl(jdbc_url)
    config.setUsername(jdbc_username)
    config.setPassword(jdbc_password)
    config.setMaxActive(jdbc_maxActive)
    config.setInitialSize(jdbc_initialSize)
    config.setTestWhileIdle(true)
    config.setValidationQuery("")
    config.setJmxEnabled(true)
    config.setMinIdle(jdbc_initialSize)
    config.setLogAbandoned(true)
    config.setRemoveAbandoned(true)


    connPool = new DataSource(config)
  }

  @PreDestroy
  def close(): Unit = {
    connPool.close(true)
  }

  def getConnection(): Connection = {
    connPool.getConnection
  }

  def test(): Unit = {
    import com.github.takezoe.scala.jdbc._
    import com.github.takezoe.scala.jdbc.jdbc._
    //val conn = connectionPool.getConnection

    val bleid = 10000001
    val lst  = DB.autoClose(connPool.getConnection) { db =>
      db.select(sql"select BLE_ID, addr_ip, addr_port from ble_base_info_1 where BLE_ID= ${bleid} ") { rs =>
        (rs.getInt(1), rs.getString(2), rs.getInt(3))
      }
    }

    case class Ble(id: Int, addr: String, port: Int)
    val lst1  = DB.autoClose(connPool.getConnection) { db =>
      db.select(sql"select BLE_ID, addr_ip, addr_port from ble_base_info_1  ", Ble.apply _)
    }

    val userId = ""
    val user: Option[(Int, String)] = DB.autoClose(connPool.getConnection) { db =>
      db.selectFirst(sql"SELECT * FROM USERS WHERE USER_ID = $userId"){ rs =>
        (rs.getInt("USER_ID"), rs.getString("USER_NAME"))
      }
    }

    val id="1496852428928::601::172.21.0.46:60793::1::1"
    val tbl_suffix_body= "1"

    val mb = DB.autoClose(connPool.getConnection) { db =>
      db.selectFirst(SqlTemplate(s"select id, properties, content from messagestore_${tbl_suffix_body} where id=?", id) ) { rs =>
        val stream = rs.getBinaryStream(3)
        val bytes = new Array[Byte](stream.available)
        stream.read(bytes)
        (rs.getString(1), rs.getString(2), new String(bytes))
      }
    }

    val use_status = "1   "
    val lst3  = DB.autoClose(connPool.getConnection) { db =>
      db.select(sql"select BLE_id, addr_ip, addr_port from tmp_ble_1 where use_status=${use_status}", Ble.apply _)
    }

  }

  def test33(): Unit = {
    import java.sql.Connection
    import java.sql.DriverManager
    val props = new java.util.Properties
    props.put("user", jdbc_username)
    props.put("password", jdbc_password)
    props.put("fixedstring", "true")

    Class.forName("oracle.jdbc.driver.OracleDriver")

    val conn = DriverManager.getConnection(jdbc_url, props)
    val sql1 = "select BLE_id, addr_ip, addr_port from tmp_ble_1 where use_status=cast(:v1 as char(4))"
    val stmt = conn.prepareStatement(sql1)
    stmt.setString(1, "1")
    val rs = stmt.executeQuery
    while (rs.next ) {
      println(rs.getString(1))
    }
    rs.close()
    stmt.close()
    conn.close()
  }

}
