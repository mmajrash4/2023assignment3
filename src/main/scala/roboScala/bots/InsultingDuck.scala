package roboScala.bots

import java.awt.Color

import com.wbillingsley.amdram.*

import roboScala.*
import game.*

import scala.util.Random


/**
  * Insulting Duck won't move, but will randomly fire an insult at another tank every second.
  * 
  * Note that it's a function returning a MessageHandler, but it has more than just the name in the state parameter.
  * We can happily do this, so long as the name is there. We'll just have to remember to default the other arguments when
  * we send the Register message. (See main)
  *
  * @param name
  * @param otherTanks
  * @return
  */
def insultingDuck(name:String, otherTanks:Seq[String]):MessageHandler[Message] = MessageHandler { (msg, context) =>

  msg match 
    case Message.TanksAlive(tanks) =>
      // Whenever the set of alive tanks changes (eg, start of game or a tank dies) we get a message with the names
      // of all the remaining tanks. Remove ourselves, so we don't insult ourself.
      info(s"Living tanks are ${tanks}")

      // We need to update our state, because there's a different set of tanks on the field
      insultingDuck(name, tanks.filter(_ != name))
  
    case m =>
      trace(s"$name received $m")

      // Randomly insult a random tank
      if (otherTanks.nonEmpty && Random.nextFloat() < (1.0/60)) {
        insultsReferee ! (name, InsultsCommand.Insult(otherTanks(Random.nextInt(otherTanks.size)), randomInsult))
      }

}
