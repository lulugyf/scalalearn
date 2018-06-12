package gyf.test.tx.db

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource

class RoutingDataSource extends AbstractRoutingDataSource{
  protected def determineCurrentLookupKey: Object = DBContext.getDBKey
}


