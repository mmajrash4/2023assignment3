package cosc250.roboScala

import javax.swing._
import java.util.{Timer, TimerTask}

import com.wbillingsley.amdram.*
import game.*

import scala.collection.mutable
import scala.util.Random


/** The time of the last step we processed */
@volatile var lastTime = System.currentTimeMillis()

/**
  * The game actor is a globally visible actor.
  * 
  * It handles the game state. Every 16ms, it receives a tick message
  * and executes the actions on the tanks (and sends out their new states). In the
  * meantime, it waits for commands from the tanks to apply on the next tick.
  * 
  * Its behaviour is defined by the gameHandler function, starting out empty.
  */
val gameActor = troupe.spawn(gameHandler(Map.empty, GameState.empty, Nil, 0))

/**
  * The behaviour of the "game actor" is defined functionally.
  * 
  * That is, it's a function whose parameters represent the current state of the actor.
  * The result of the function is a message handler describing how to respond to the next incoming message.
  * 
  * Consequently, almost all responses are "for the next message, use gameHandler with this updated state", 
  * i.e. another call to gameHandler with new parameters.
  * 
  * There are three pieces of state:
  *
  * @param players - the currently registered players
  * @param state - the state of the game 
  * @param queued - commands that have been queued by players to run on the next game "tick"
  * @param lastTime - the last time a tick was run
  */
def gameHandler(
  players:Map[String, Recipient[Message]], 
  state:GameState, 
  queued:List[(String, TankCommand)],
  lastTime:Double
):MessageHandler[GameMessage] = MessageHandler { (msg, context) =>

  msg match

    // Indicates the game is ready to show on the UI. Also sent before the first tick
    case GameControl.Ready(time) => 
      trace(s"Ready received")      

      ui.GameUI.gameState = state
      ui.GameUI.commands = Map.empty
      ui.GameUI.repaint()

      val alive = for t <- state.tanks if t.isAlive yield t.name
      for t <- alive do players(t) ! Message.TanksAlive(alive)

      // Return a handler with the updated time, so we measure the next tick correctly
      gameHandler(players, state, queued, time)


    // A tick moves the game forward
    case GameControl.Tick(time) =>
      
      // Step the game forward
      val dt = (time - lastTime) / 1000.0
      trace(s"Tick received for $dt ms")      

      val tankCommands = queued.reverse.groupMap(_._1)(_._2)
      val (newState, messages) = state.step(dt, tankCommands)

      // Update the GameUI
      ui.GameUI.gameState = newState
      ui.GameUI.commands = tankCommands
      ui.GameUI.repaint()

      // Send any resulting messages
      for (name, message) <- messages do
        players(name) ! message
        streamActor ! (name, message)

      // Tell the players what their state is. This should prompt tanks to respond with
      // their next commands.
      for t <- newState.tanks do players(t.name) ! Message.TankState(t)

      // return a handler for the new state
      gameHandler(players, newState, List.empty, time)

    case GameControl.Register(nameStem, color, launchMethod) => 
      /** Create a unique name for this player */
      val name = s"$nameStem ${Random.alphanumeric.take(4).mkString}"

      val tank = Tank.random(name, color)

      trace(s"Tanks werwe ${state.tanks}")
      val newState = state.copy(tanks = state.tanks :+ tank)
      val actor = context.spawn(launchMethod(name))

      info(s"Registered $name")
      trace(s"Tanks are ${newState.tanks}")

      ui.GameUI.addTank(name)
      ui.GameUI.gameState = newState
      ui.GameUI.repaint()

      gameHandler(players + (name -> actor), newState, queued, lastTime)
      
    case GameControl.LookUp(replyTo, name) =>
      if players.contains(name) then 
        replyTo ! players(name)
      else error(s"Asked for $name which doesn't exist")

    case (name, c:TankCommand) => 
      // pass it on to the command stream 
      streamActor ! (name, c)

      // Queue the command to handle it on the next tick
      gameHandler(players, state, (name -> c) :: queued, lastTime)

}

/* A timer that will send tick messages */
val timer = new Timer

/** Called by the button on the GameUI */
def startGame():Unit = {
  val task = new TimerTask {
    def run():Unit = { gameActor ! GameControl.Tick(System.currentTimeMillis()) }
  }

  // Tell the game actor the time now
  gameActor ! GameControl.Ready(System.currentTimeMillis())

  // Start the timer
  timer.schedule(task, 16L, 16L)
}




