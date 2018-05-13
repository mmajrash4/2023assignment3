package cosc250.weekSix

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import play.api.libs.json
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This week, let's play FizzBuzz with Actors.
 *
 * If you're not sure what FizzBuzz is, see https://en.wikipedia.org/wiki/Fizz_buzz
 *
 */
object Exercise {

  /*
   * First, because we're working with Actors, we need some messages to pass
   * Note: we can also just pass a bare Int! Normally we wouldn't -- it's nice to have a single trait
   * or abstract class all our messages belong to -- but for this exercise let's show it's possible
   */
  case class Fizz(i:Int)
  case class Buzz(i:Int)
  case class FizzBuzz(i:Int)

  /*
   * And we need a message so the next player can say "Wrong! You're out!"
   *
   * Note, I've made this message generic, so it can contain any of our messages
   */
  case class Wrong[T](item:T)

  /*
   * We're also going to need some "set-up" messages, to tell the Actors who is standing
   * next to whom (if Alice goes "Fizz(3)", who goes next?
   */
  case class NextPlayerIs(a:ActorRef)

  /**
    * This is a little utility method I've made. It creates a PartialFunction that
    * writes out whatever it received.
    *
    * PartialFunctions can be composed together using "andThen". So if you want a method
    * that logs what it receives and then takes an action depending on what method it receives...
    *
    * log("MyActor") andThen { case ... }
    */
  def log(name:String):PartialFunction[Any, Any] = {
    case m =>
      println(name + " received " + m)
      m
  }

  /*
   * Now you need to define your FizzBuzz Actor...
   */
  class FizzBuzzActor extends Actor {

    var nextPlayer:Option[ActorRef] = None

    def nextResponse(i:Int) = {
      val nextNum = i + 1

      if (nextNum % 3 == 0 && nextNum % 5 == 0) {
        FizzBuzz(nextNum)
      } else if (nextNum % 5 == 0) {
        Buzz(nextNum)
      } else if (nextNum % 3 == 0) {
        Fizz(nextNum)
      } else nextNum
    }

    /**
      * This should check the veracity of a FizzBuzz message.
      * (Including checking whether a Fizz should really have been a FizzBuzz!)
      */
    def checkMessage(m:Any):Boolean = m match {
      case 0 => true
      case FizzBuzz(i) => i % 3 == 0 && i % 5 == 0
      case Fizz(i) => i % 3 == 0 && i % 5 != 0
      case Buzz(i) => i % 3 != 0 && i % 5 == 0
      case i:Int => i % 3 != 0 && i % 5 != 0
      case _ => false
    }

    def respond(i:Int) = {
      val n = nextResponse(i)
      println("FBA says " + n)
      for { p <- nextPlayer } p ! n
    }

    /**
      * We want to check the message, and that means we need a reference to it.
      * So I've broken this out as its own function, taking the message m as a parameter
      */
    def checkAndRespond(m:Any) = {
      if (checkMessage(m)) {
        // If the message is good, then we keep going as before
        m match {
          case i:Int => respond(i)
          case Fizz(i) => respond(i)
          case Buzz(i) => respond(i)
          case FizzBuzz(i) => respond(i)
        }
      } else {
        // But if the message was wrong, we send "Wrong" to the sender
        // sender is a message defined on ActorRef that gets the sender of the message we're dealing with.
        sender ! Wrong(m)
      }
    }

    def receive = Exercise.log("FBA") andThen {
      case NextPlayerIs(p) => {
        nextPlayer = Some(p)

        // Let's send a message back so there is some kind of response, so the set-up code can know we've done it.
        sender ! "Ok"
      }

      // We need to handle the Wrong message
      case Wrong(x) => println("Someone just said I got it wrong.")

      // I've broken this code out into the checkAndRespond method
      case m => checkAndRespond(m)
    }
  }


}
