package cosc250.weekSix

import java.util.concurrent.TimeoutException

import akka.actor._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContext.Implicits.global

object MyApp extends App {

  import Exercise._

  // Create the actor system
  val system = ActorSystem("PingPongSystem")



  // Let's create five Terrible players.
  // Each of these returns an ActorRef
  val algernon = system.actorOf(Props[FizzBuzzActor], name = "Algernon")
  val bertie = system.actorOf(Props[Terrible], name = "Bertie")
  val cecily = system.actorOf(Props[FizzBuzzActor], name = "Cecily")
  val daliah = system.actorOf(Props[FizzBuzzActor], name = "Dahlia")
  val earnest = system.actorOf(Props[FizzBuzzActor], name = "Earnest")

  // Ok, this code I'll need to put in the README.md
  // To use the "ask" pattern, we need an implicit timeout.
  // This sets a default timeout of 5 seconds
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._
  implicit val timeout = Timeout(5.seconds)

  /*
     Here, we're putting the players in a circle and checking they've done it.
     It relies on the ask pattern (actor ? message)
     This will wait for a reply to come back, and gives us a Future[Any]

     And then we're using for notation to chain these together. We don't actually care what comes
     back in the future (it'll be the string "Ok"), we just care that the reply has come in before we
     move on to the next line. So we're getting the value 'a' from the first future ... but then not doing
     anything with it.
   */
  for {
    a <- algernon ? NextPlayerIs(bertie)
    b <- bertie ? NextPlayerIs(cecily)
    c <-cecily ? NextPlayerIs(daliah)
    d <- daliah ? NextPlayerIs(earnest)
    e <- earnest ? NextPlayerIs(algernon)
  } {
    /*
     Now this line -- effectively in the middle of a foreach on all those nested futures -- can
     only happen when all the Futures have completed. ie, when all the actors have responded to their
     NextPlayerIs messages.
     */
    algernon ! 0
  }



}

