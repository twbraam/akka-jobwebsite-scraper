package org.twbraam.akkaGettingStarted

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, ScalaTestWithActorTestKit}
import org.scalatest.WordSpecLike

object IntroSpec {
  object HelloWorld {
    final case class Greet(whom: String, replyTo: ActorRef[Greeted])
    final case class Greeted(whom: String, from: ActorRef[Greet])

    val greeter: Behavior[Greet] = Behaviors.receive { (context, message) =>
      context.log.info("Hello {}!", message.whom)
      println(s"Hello ${message.whom}!")
      message.replyTo ! Greeted(message.whom, context.self)
      Behaviors.same
    }
  }
  object HelloWorldBot {

    def bot(greetingCounter: Int, max: Int): Behavior[HelloWorld.Greeted] =
      Behaviors.receive { (context, message) =>
        val n = greetingCounter + 1
        context.log.info("Greeting {} for {}", n, message.whom)
        println(s"Greeting ${n} for ${message.whom}")
        if (n == max) {
          Behaviors.stopped
        } else {
          message.from ! HelloWorld.Greet(message.whom, context.self)
          bot(n, max)
        }
      }
  }
  object HelloWorldMain {

    final case class Start(name: String)

    val main: Behavior[Start] =
      Behaviors.setup { context =>
        val greeter = context.spawn(HelloWorld.greeter, "greeter")

        Behaviors.receiveMessage { message =>
          val replyTo = context.spawn(HelloWorldBot.bot(greetingCounter = 0, max = 3), message.name)
          greeter ! HelloWorld.Greet(message.name, replyTo)
          Behaviors.same
        }
      }
  }
  object CustomDispatchersExample {
    import HelloWorldMain.Start

    val main: Behavior[Start] =
      Behaviors.setup { context =>
        val dispatcherPath = "akka.actor.default-blocking-io-dispatcher"

        val props = DispatcherSelector.fromConfig(dispatcherPath)
        val greeter = context.spawn(HelloWorld.greeter, "greeter", props)

        Behaviors.receiveMessage { message =>
          val replyTo = context.spawn(HelloWorldBot.bot(greetingCounter = 0, max = 3), message.name)

          greeter ! HelloWorld.Greet(message.name, replyTo)
          Behaviors.same
        }
      }
  }

  object ChatRoom {
    sealed trait RoomCommand
    final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent]) extends RoomCommand
    private final case class PublishSessionMessage(screenName: String, message: String) extends RoomCommand

    sealed trait SessionEvent
    final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent
    final case class SessionDenied(reason: String) extends SessionEvent
    final case class MessagePosted(screenName: String, message: String) extends SessionEvent

    trait SessionCommand
    final case class PostMessage(message: String) extends SessionCommand
    private final case class NotifyClient(message: MessagePosted) extends SessionCommand

    val behavior: Behavior[RoomCommand] =
      chatRoom(List.empty)

    private def chatRoom(sessions: List[ActorRef[SessionCommand]]): Behavior[RoomCommand] =
      Behaviors.receive { (context, message) =>
        message match {
          case GetSession(screenName, client) =>
            // create a child actor for further interaction with the client
            val ses = context.spawn(
              session(context.self, screenName, client),
              name = URLEncoder.encode(screenName, StandardCharsets.UTF_8.name))
            client ! SessionGranted(ses)
            chatRoom(ses :: sessions)
          case PublishSessionMessage(screenName, message) =>
            val notification = NotifyClient(MessagePosted(screenName, message))
            sessions.foreach(_ ! notification)
            Behaviors.same
        }
      }

    private def session(
                         room: ActorRef[PublishSessionMessage],
                         screenName: String,
                         client: ActorRef[SessionEvent]): Behavior[SessionCommand] =
      Behaviors.receiveMessage {
        case PostMessage(message) =>
          // from client, publish to others via the room
          room ! PublishSessionMessage(screenName, message)
          Behaviors.same
        case NotifyClient(message) =>
          // published from the room
          client ! message
          Behaviors.same
      }
  }

}

class IntroSpec extends ScalaTestWithActorTestKit with WordSpecLike {

  import IntroSpec._

  "Hello world" must {
    /*"say hello" in {



      val system: ActorSystem[HelloWorldMain.Start] =
        ActorSystem(HelloWorldMain.main, "hello")

      system ! HelloWorldMain.Start("World")
      system ! HelloWorldMain.Start("Akka")




      Thread.sleep(500) // it will not fail if too short
      ActorTestKit.shutdown(system)
    }*/

    "chat" in {
      import ChatRoom._

      val gabbler: Behavior[SessionEvent] =
        Behaviors.setup { context =>
          Behaviors.receiveMessage {
            // We document that the compiler warns about the missing handler for `SessionDenied`
            case SessionDenied(reason) =>
              context.log.info("cannot start chat room session: {}", reason)
              Behaviors.stopped
            case SessionGranted(handle) =>
              handle ! PostMessage("Hello World!")
              Behaviors.same
            case MessagePosted(screenName, message) =>
              context.log.info("message has been posted by '{}': {}", screenName, message)
              Behaviors.stopped
          }
        }

      val main: Behavior[NotUsed] =
        Behaviors.setup { context =>
          val chatRoom = context.spawn(ChatRoom.behavior, "chatroom")
          val gabblerRef = context.spawn(gabbler, "gabbler")
          context.watch(gabblerRef)
          chatRoom ! GetSession("ol’ Gabbler", gabblerRef)

          Behaviors.receiveSignal {
            case (_, Terminated(_)) =>
              Behaviors.stopped
          }
        }

      val system = ActorSystem(main, "ChatRoomDemo")
      system.whenTerminated // remove compiler warnings
    }
  }

}