package gyf.test.scala

import java.util
import java.util.{ArrayList, List}

import javax.annotation.PostConstruct
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import org.apache.curator.RetryPolicy
import org.apache.curator.framework.api.ACLProvider
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.{ExponentialBackoffRetry, RetryOneTime}
import org.apache.zookeeper.ZooDefs
import org.apache.zookeeper.data.{ACL, Id}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.text.SimpleDateFormat

@Component
class ZKUtil {

  @Value("${curator.zookeeper.addr:172.21.0.46:3181}")
  var zk_addr = ""

  @Value("${curator.zookeeper.acl.username:}")
  var aclUsername = "admin"
  @Value("${curator.zookeeper.acl.password:}")
  var aclPassword = "admin"
  val zk_connectTimeout = 6000
  val zk_sessionTimeout = 30000

  var zk: CuratorFramework = null

  val log = LoggerFactory.getLogger("gyf.test.ZKUtil")


  def createCurator(zkAddr: String):CuratorFramework = {
    import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
    import org.apache.curator.retry.ExponentialBackoffRetry
    import org.apache.curator.framework.api.ACLProvider
    import org.apache.zookeeper.ZooDefs
    import org.apache.zookeeper.data.ACL

    val zkUsername = "admin"
    val zkPassword = "admin"
    val builder = CuratorFrameworkFactory.builder()
      .retryPolicy(new ExponentialBackoffRetry(1000, 3))
      //.namespace("idmm")
      .connectString(zkAddr)

    if (zkUsername != null && zkPassword != null) {
      val authenticationString = zkUsername + ":" + zkPassword
      builder.authorization("digest", authenticationString.getBytes).aclProvider(new ACLProvider() {
        override def getDefaultAcl: java.util.List[ACL] = ZooDefs.Ids.CREATOR_ALL_ACL
        override  def getAclForPath(path: String): java.util.List[ACL] = ZooDefs.Ids.CREATOR_ALL_ACL
      })
    }
    val curator = builder.build
    curator.start()

    curator
  }

  def test(curator: CuratorFramework): Unit = {
    import java.text.SimpleDateFormat

    val zkAddr="10.113.172.58:8671"
    val curator = createCurator(zkAddr)

    val chld = curator.getChildren.forPath("/broker").toArray.map(_.toString)

    val data = new String(curator.getData.forPath("/broker/10.113.182.97:22301"))

    val state = curator.checkExists.forPath("/broker/10.113.182.97:22301")
    val fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val tmstr = fmt.format(state.getCtime)
  }

  @PostConstruct
  def init(): Unit = {
    val zkClient = buildClient
    zkClient.getConnectionStateListenable.addListener(new ConnectionStateListener() {
      override def stateChanged(client: CuratorFramework, newState: ConnectionState): Unit = {
        if (ConnectionState.CONNECTED eq newState) log.warn("state change to CONNECTED")
        else if (ConnectionState.LOST eq newState) { // 连接挂起或者丢失的情况下， 都直接关闭应用
          log.error("zookeeper connection lost, app exit...")
          System.exit(1)
        }
        else if (ConnectionState.SUSPENDED eq newState) log.warn("zookeeper connection suspended, waiting...")
      }
    })
    zkClient.start()
    try {
      zkClient.blockUntilConnected()
      log.info("zookeeper connected!")
    } catch {
      case e: InterruptedException =>
        log.error("wait zookeeper connect failed, exit", e)
        System.exit(2)
    }
    zk = zkClient
  }

  def buildClient: CuratorFramework = {
    val builder: CuratorFrameworkFactory.Builder = CuratorFrameworkFactory
      .builder
      .connectionTimeoutMs(zk_connectTimeout)
      .sessionTimeoutMs(zk_sessionTimeout)
      .connectString(zk_addr)
      .retryPolicy(new ExponentialBackoffRetry(2000, 5))
    if (aclUsername != null && !"".equals(aclUsername) && aclPassword != null && !"".equals(aclPassword)) {
      log.debug("--- use acl, user: {}", aclUsername)
      val authStr = aclUsername + ":" + aclPassword
      builder.authorization("digest", authStr.getBytes).aclProvider(new ACLProvider() {
        override def getDefaultAcl = ZooDefs.Ids.CREATOR_ALL_ACL
        override def getAclForPath(path: String) = ZooDefs.Ids.CREATOR_ALL_ACL
      })
    }
    builder.build
  }

}


