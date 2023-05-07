package cosc250.roboScala.game

import com.wbillingsley.amdram.*

/** The messages that your tank can receive */
enum Message:

    /** A list of names of alive tanks still in the game. Sent whenever this changes. */
    case TanksAlive(names:Seq[String])

    /** The current state of your tank */
    case TankState(tank:Tank) 

    /** If your shell hit a tank */
    case YouHit(target:Tank) 

    /** If your shell expired, having missed everything */
    case YouMissed(shell:Shell)

    /** If you were hit by a shell */
    case YouWereHit

    /** The latest results from your radar */
    case RadarResult(you:Tank, tanks:Set[Tank])

    /** A message you'll receive if you've just been insulted. The address to send the retort to is included. */
    case Insulted(replyTo:Recipient[InsultsCommand.Retort], insult:String) 


/** The messages the game actor can receive. Note that your tank will send (String, TankCommand) tuples */
type GameMessage = GameControl | (String, TankCommand)


/** Special messages received only by the game actor, that manage the running of the game */
enum GameControl: 

    /** A Tick is sent by the timer, to cause the Game Actor to move the game forward */
    case Tick(time:Long)

    /** Sent by Main to say the game is ready to show */
    case Ready(time:Long)

    /** To launch a tank, we send it a register message. This includes a function that will produce the handler */
    case Register(nameStem:String, color:java.awt.Color, start:String => MessageHandler[Message])

    /** Gets the address of a player */
    case LookUp(replyTo:Recipient[Recipient[Message]], name:String)


