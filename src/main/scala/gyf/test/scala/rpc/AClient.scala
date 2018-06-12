package gyf.test.scala.rpc

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class AClient extends Actor {

  //远程Actor
  var remoteActor : ActorSelection = null
  //当前Actor
  var localActor : akka.actor.ActorRef = null

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    remoteActor = context.actorSelection("akka.tcp://lxw1234@127.0.0.1:2555/user/server")
    println("远程服务端地址 : " + remoteActor)
  }

  override def receive: Receive = {
    //接收到消息类型为AkkaMessage后，将消息转发至远程Actor
    case msg: AkkaMessage => {
      println("客户端发送消息 : " + msg)
      this.localActor = sender()
      remoteActor ! msg
    }
    //接收到远程Actor发送的消息类型为Response，响应
    case res: Response => {
      localActor ! res
    }
    case _ => println("客户端不支持的消息类型 .. ")

  }

}

object AClient extends App{

    val clientSystem = ActorSystem("ClientSystem", ConfigFactory.parseString("""
      akka {
       actor {
          provider = "akka.remote.RemoteActorRefProvider"
        }
      }
     """))

    var client = clientSystem.actorOf(Props[AClient])
    var msgs = Array[AkkaMessage](AkkaMessage("message1"),AkkaMessage("message2"),AkkaMessage("message3"),AkkaMessage("message4"))

    implicit val timeout = Timeout(3 seconds)

    msgs.foreach { x =>
      val future = client ask x
      val result = Await.result(future,timeout.duration).asInstanceOf[Response]
      println("收到的反馈： " + result)
    }


    val remoteActor = clientSystem.actorSelection("akka.tcp://lxw1234@127.0.0.1:2555/user/server")
    msgs.foreach { x =>
      val fut = remoteActor.ask(x)
      val ret = Await.result(fut, timeout.duration).asInstanceOf[Response]
      println("got: " + ret)
    }

    clientSystem.terminate()

}