package gyf.test.tx.db

object DBContext {
  //define count of database and it must match with resources/properties/jdbc.properties//define count of database and it must match with resources/properties/jdbc.properties

  private val DB_COUNT = 2

  private val tlDbKey = new ThreadLocal[String]

  def getDBKey: String = tlDbKey.get

  def setDBKey(dbKey: String): Unit = {
    tlDbKey.set(dbKey)
  }

  def getDBKeyByUserId(userId: Int): String =  "db_" + (userId % DB_COUNT + 1)

}

