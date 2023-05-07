package cosc250.roboScala

import java.awt.BorderLayout
import javax.swing.{JFrame, JPanel}

import java.awt.Color

import com.wbillingsley.amdram.* 
import iteratees.*

import scala.concurrent.* 
import ExecutionContext.Implicits.global

import game.*
import cosc250.roboScala.RegisterSink

/**
  * The actor system is a top-level "given" instance so that it is automatically found where it
  * is needed
  */
given troupe:Troupe = SingleEcTroupe()


@main def main() = {

    /*
     * Let's register the players.
     * The Register message takes a name stem. (The game will affix a random 4-character suffix to it to get the bot's name.)
     * It also takes a color to show the tank in.
     * And a function from `name:String` => MessageHandler[Message]. i.e. the behaviour the bot should start with.
     * In the examples below, bots.spinningDuck is a function of type String => MessageHandler[Message], so we can just pass the function.
     * 
     * For a bot with slightly more state, see insultingDuck further down.
     */
    gameActor ! GameControl.Register("Spinning Duck", Color.orange, bots.spinningDuck)
    gameActor ! GameControl.Register("Spinning Duck", Color.yellow, bots.spinningDuck)

    // Note that because insultingDuck's behaviour method takes more than just its name as a state argument, we've
    // made a function (String) => MessageHandler, by defaulting the other argument that insultingDuck needs
    gameActor ! GameControl.Register("Insulting Duck", Color.pink, (name:String) => bots.insultingDuck(name, Seq.empty))     

}

