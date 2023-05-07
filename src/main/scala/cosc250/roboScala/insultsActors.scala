package cosc250.roboScala

import com.wbillingsley.amdram.*
import game.* 

import scala.concurrent.*
import ExecutionContext.Implicits.global


/**
  * smartAlec is an expert at insults. 
  * 
  * Throw an insult at smartAlec by sending him a 
  * (Recipient[SmartAlecCommand.Retort], SmartAlecCommand.Insult)
  * 
  * If the insult is in the game, he'll throw back the witty retort.
  */
export Insults.smartAlec

/**
  * insultsReferee is where you send your insults and retorts 
  * 
  * Throw an insult at smartAlec by sending him a 
  * (Recipient[SmartAlecCommand.Retort], SmartAlecCommand.Insult)
  * 
  * If the insult is in the game, he'll throw back the witty retort.
  */
export Insults.insultsReferee

/**
  * The set of insults available in the game
  */
export Insults.insults

object Insults {

  def insults = insultsAndRetorts.keys

  /**
    * If you send a `(self, SmartAlecCommand.Insult)` to Smart Alec, he'll always send you back the right retort
    */
  val smartAlec = troupe.spawnLoop[(Recipient[SmartAlecCommand.Retort], SmartAlecCommand.Insult)] { (sender, command) => 
    sender ! SmartAlecCommand.Retort(insultsAndRetorts.getOrElse(command.insult, "I am rubber, you are glue!"))
  }


  /**
    * The insults actor handles the "tankfighting with insults" part of the game.
    */
  val insultsReferee = troupe.spawnLoop[(String, InsultsCommand.Insult)] { (sender, command) =>
    
    // Forward the command to the commandStream for the UI
    streamActor ! (sender -> command)

    info(s"$sender throws shade on ${command.tank} : ${command.insult}")
    
    for {
      insulted <- gameActor.ask[GameControl.LookUp, Recipient[Message]](r => GameControl.LookUp(r, command.tank))
      response <- insulted.ask[Message.Insulted, InsultsCommand.Retort](r => Message.Insulted(r, command.insult))
    } do {
      streamActor ! (command.tank, response)
      println(s"${command.tank} responds: ${response.retort}")

      if insultsAndRetorts.get(command.insult).contains(response.retort) then 
        println(s"The retort was right! Insulter $sender takes the hit!")
        gameActor ! (sender -> TankCommand.TakeHit)
      else
        println(s"The retort was wrong! Target ${command.tank} takes the hit!")
        gameActor ! (command.tank -> TankCommand.TakeHit)

    }

    
  }

  /** The canonical list of insults and retorts */
  private val insultsAndRetorts = Map(
    "I've seen sharper looking radar dishes in the spoons drawer" ->
      "I wondered where you get your equipment!",

    "I hear your AI whines like a baby" ->
      "No, that's the shells: a foot long, nine pounds, and screaming right at you",

    "Your aim's wobbling like you've just come out of the jelly factory" ->
      "And your chassis's moving like you've just eaten one",

    "I have suffered your insolence long enough!" ->
      "I'll try to put you out of your misery quickly, then",

    "You have the manners of a troll!" ->
      "How cute -- you think I'm family",

    "You couldn't hit the side of a bus!" ->
      "Thank goodness you don't move as fast!",

    "I can hear the gears grinding in your body" ->
      "And I can hear the gears grinding in your head",

    "Soon, you'll be sitting in a pile of smoking rubble" ->
      "It looks like you already are",

    "I've never seen a tank as incompetent as you" ->
      "You mean they always catch you unawares?",

    "I've seen dogs that were better pilots than you" ->
      "Grab the fire extinguisher -- that's not a dog you're about to hear going 'woof'",

    "You'll never see me fight as badly as you" ->
      "The smoke pouring from your engine does get in the way",

    "You'll be hearing the bang when I press this button" ->
      "A pity that's the accelerator",

    "Soon I'll be dancing on your grave!" ->
      "First you'd better dance around these shells",

    "I've half a mind to crush you like a bug!" ->
      "You mean your programmer didn't finish writing your routines?",

    "Watch me fly like the wind!" ->
      "I think it's your wind that's attracting those flies",

    "You are lower than a dung beetle!" ->
      "And you are higher than his food!",

    "My socks have more brains than you!" ->
      "Now you know where your coder went wrong!",

    "Now, vile snake, hear me roar!" ->
      "You're that angry with your coder?"

  )


}
