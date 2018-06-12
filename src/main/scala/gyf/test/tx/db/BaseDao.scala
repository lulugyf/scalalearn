package gyf.test.tx.db

import gyf.test.tx.beans.BleBase
import org.apache.ibatis.annotations.Param

import scala.beans.BeanProperty

class BleInfo {
  @BeanProperty var ble_id: Int = 0
  @BeanProperty var addr_ip: String = null
  @BeanProperty var addr_port: Int = 0
}

trait BaseDao {

  def insertSql(money: Int): Unit

  def sum: Integer

  def select_idx(@Param("table_suffix") tbl_sfx: String, @Param("status") status: String): java.util.List[BleBase]

  def select_cond1(@Param("table_suffix") tbl_sfx: String, @Param("status") status: String): java.util.List[BleInfo]
}
