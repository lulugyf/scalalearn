package gyf.test.tx.db

import java.util

import gyf.test.tx.beans.BleBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BaseService {
  @Autowired private val dao: BaseDao = null

  def insert(money: Int, flag: Boolean): Unit = {
    dao.insertSql(money)
    if(flag)
      throw new Exception("exception of mine")
  }

  def update(sql: String): Unit = {

  }

  def delete(sql: String): Unit = {
  }

  def sum: Integer = {
    dao.sum
  }

  def selidx(): Unit = {
    val lst = dao.select_cond1("1", "1")
//    val lst = dao.select_idx("1", "1")
    println("size: "+lst.size)
    lst.toArray.foreach{ o1 =>
      val o = o1.asInstanceOf[BleInfo]
      println(s"${o.getBle_id} == ${o.getAddr_ip}:${o.getAddr_port}")
    }

  }
}
