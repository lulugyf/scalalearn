package gyf.test.tx

import gyf.test.tx.db.{BaseService, DBContext}
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallbackWithoutResult
/*

create table tbl_account(money int);

insert into tbl_account values(107);

create uniq index __vidx on tbl_account(money);

 */

object TxMain extends App {
  val app = new ClassPathXmlApplicationContext("tx_test.xml")


  val test = app.getBean("test").asInstanceOf[TestClass]

//  DBContext.setDBKey("db_2")
//  println(test.sum)
//  try {
//    test.test_tran1()
//  }catch{
//    case e: Throwable => println(e.getMessage)
//  }
//  println(test.sum)

  DBContext.setDBKey("db_idmmxq")
  println(test.sum)
  try {
    test.test_tran1()
  }catch{
    case e: Throwable => println(e.getMessage)
  }
  println(test.sum)

  app.close()
}

@Component("test")
class TestClass {

  import org.springframework.beans.factory.annotation.Autowired

  import org.springframework.transaction.support.TransactionTemplate
  import javax.annotation.Resource

  @Resource private val transactionTemplate: TransactionTemplate = null

  @Autowired private val baseService:BaseService = null

  @Transactional
  def test_tran(): Unit = {

    baseService.insert(100, false)
    baseService.insert(100, true)
  }

  implicit def func2doInTransaction(f: TransactionStatus => Unit) =
    new TransactionCallbackWithoutResult {
      def doInTransactionWithoutResult(status: TransactionStatus) = f(status)
    }

  def test_tran1() : Unit = {
    transactionTemplate.execute{ (status: TransactionStatus) =>
      try {
        baseService.selidx
        baseService.insert(100, false)
        baseService.insert(103, true)
      } catch {
        case e: Exception =>
          //对于抛出Exception类型的异常且需要回滚时，需要捕获异常并通过调用status对象的setRollbackOnly()方法告知事务管理器当前事务需要回滚
          status.setRollbackOnly()
          println("exception: " + e.getMessage)
      }
    }

//    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
//      override protected def doInTransactionWithoutResult(status: TransactionStatus): Unit = {
//        try {
//          baseService.selidx
//          baseService.insert(100, false)
//          baseService.insert(103, true)
//        } catch {
//          case e: Exception =>
//            //对于抛出Exception类型的异常且需要回滚时，需要捕获异常并通过调用status对象的setRollbackOnly()方法告知事务管理器当前事务需要回滚
//            status.setRollbackOnly()
//            println("exception: " + e.getMessage)
//            //e.printStackTrace()
//        }
//      }
//    })
  }

  def sum(): Int = {
    baseService.sum
  }

  def testtx1(): Unit = {
    System.out.println("before transaction")
    val sum1 = baseService.sum
    System.out.println("before transaction sum: " + sum1)
    System.out.println("transaction....")

    try {
      test_tran()
    }catch{
      case e: Throwable => e.printStackTrace()
    }

    System.out.println("after transaction")
    val sum2 = baseService.sum
    System.out.println("after transaction sum: " + sum2)
  }
}