package cosc250.roboScala.bots

import java.awt.Color

import com.wbillingsley.amdram.*

import cosc250.roboScala.*
import game.*

import scala.util.Random

/**
  * Spinning duck turns in circles, while also spinning its turret and radar (making its
  * radar sweep fast). It pings constantly, and if it sees a tank always fires.
  */
def spinningDuck(name:String):MessageHandler[Message] = MessageHandler { (msg, context) =>

  msg match
    
    // Every game tick, we receive our tank state, and should send commands to
    // Main.gameActor to say what we want to do this tick
    case Message.TankState(me) =>
      gameActor ! (name, TankCommand.FullSpeedAhead)

      // If we have enough energy to ping and shoot, ping
      if (me.energy > Tank.radarPower + Shell.energy) gameActor ! (name, TankCommand.RadarPing)

      gameActor ! (name, TankCommand.TurnClockwise)
      gameActor ! (name, TankCommand.TurretClockwise)
      gameActor ! (name, TankCommand.RadarClockwise)
  
    // If we successfully Ping the radar, we'll get a message containing the
    // states of any tanks we see
    case Message.RadarResult(me, seenTanks) =>
      if (seenTanks.nonEmpty) then gameActor ! (name, TankCommand.Fire)

    case _  =>
      // ignore all other messages

}