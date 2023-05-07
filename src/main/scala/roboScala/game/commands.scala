package roboScala.game

import java.awt.Color
import scala.util.Random

import roboScala.*

/** Commands are sent by AI Players to the Game Actor */
sealed trait Command


enum TankCommand extends Command:

    /** Register for the game */
    case Register(color:Color)

    /** Rotate the turret */
    case TurretClockwise 
    case TurretAnticlockwise 

    /** Fire a shell */
    case Fire

    /** Rotate the tank body */
    case TurnClockwise 
    case TurnAnticlockwise

    /** Change the desired velocity for the tank */
    case FullSpeedAhead
    case FullReverse

    /** Rotate the radar on top of the turret */
    case RadarClockwise
    case RadarAnticlockwise

    /** Use the radar to scan for tanks in range */
    case RadarPing

    /** A special command, added by the GameActor, to say that a tank has been hit */
    case TakeHit


/** Insults are special commands to the InsultsActor for adjudication */
enum InsultsCommand extends Command:

    /** Launches an devastating insult at a named tank */
    case Insult(tank:String, insult:String) 

    /** Responds with a devastating retort */
    case Retort(retort:String) 


/** 
 * Smart Alec is standing on the side-lines. If you insult him, he'll always come back with the right witty retort.
 * Note that to send alec a message, you'll have to send him `(self, command)` so he can know who to reply to.
 */
enum SmartAlecCommand:
    case Insult(insult:String) 
    case Retort(retort:String) 


/**
  * Shortcut to get a random insult string
  */
def randomInsult = insults.toSeq(Random.nextInt(insults.size))

