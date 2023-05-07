# Assignment 3 

For assignment 3, we're going to implement a game played by AI.
The puzzle is not, however, in building brilliant AI, but in using Actors 

The game we're going to implement is loosely based on *RoboCode*, which was an IBM DeveloperWorks game for learning programming.

I've redesigned it as *RoboScala*, a game for learning to code with actors.

### RoboScala
![RoboScala](screenshot720.png)

Each AI pilots a robotic tank on a virtual battlefield. 
Each tank has a body, a turret, and a radar. The radar is
mounted on top of the turret.

The tanks do not know where each other is, unless they detect
them with their radar. The radar has a cone of vision that you
can see in the UI. 

Each tank also has a health level and an energy level.
Energy replenishes from the power plant at 1 unit per tick.
The radar (when it is pinged) and the gun (when it is fired) use power. 

### What's written for you

As the assignment is about Actors, much of the game is already
written for you:

- the UI, in the `roboScala.ui` package
- the classes and messages for the game, in the `roboScala.game` package
- the actors that run the game, in the `roboScala` package, and
- two sample bots in the `roboScala.bots` package

But there is still some work for you to do -- see the tasks below.

There are two places you'll need to write or edit code:

- in `roboScala.bots`, to write your bots
- in `main.scala`, to put your bots on the field

The rest shouldn't need editing.

First, though, a little about the tanks/players.

### Players are actors. You write the behaviour function.

Each Player is an Actor. 

To define a new kind of tank AI, you'd need to define a behaviour for it.
These are defined as functions that return a message handler. The state is in the arguments, the message handler
(how to handle the next message) is in the function body.

Take a look at `SpinningDuck` in the `roboScala.bots` package for a simple behaviour.

```scala
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
```

If the behaviour wants to return a new state, it returns a call to itself.

Take a look at `InsultingDuck` in the `roboScala.bots` package for an example of a bot changing its behaviour (when it receives a `TanksAlive` message)

```scala
def insultingDuck(name:String, otherTanks:Seq[String]):MessageHandler[Message] = MessageHandler { (msg, context) =>

  msg match 

    // Whenever the set of alive tanks changes (eg, start of game or a tank dies) we get a message with the names
    // of all the remaining tanks. Remove ourselves, so we don't insult ourself.
    case Message.TanksAlive(tanks) =>
      // We need to update our state, because there's a different set of tanks on the field
      insultingDuck(name, tanks.filter(_ != name))

    // On any other message, we're just randomly sending an insult  
    case m =>
      // Randomly insult a random tank
      if (otherTanks.nonEmpty && Random.nextFloat() < (1.0/60)) {
        insultsReferee ! (name, InsultsCommand.Insult(otherTanks(Random.nextInt(otherTanks.size)), randomInsult))
      }
}
```

### Registering the bots

To create a robot using our behaviour, we edit `main` (in `main.scala`) to send the `gameActor` a `GameControl.Register` message.

e.g.:

```scala
gameActor ! GameControl.Register("Insulting Duck", Color.pink, (name:String) => bots.insultingDuck(name, Seq.empty))
```

The `GameControl.Register` message has three parameters:

- The start of the tank's name. (It'll get appended with a random string to make it less likely to collide with another.)
- The color of the tank for the UI
- A function to create its initial behaviour. This is a `(String) => MessageHandler[Message]`. See the examples in `main`
  
### The game tick 

Each game tick, it will receive a `TankState` message with the state of the tank.

To command the tank, a tank's Actor should send instructions to `gameActor`. The instructions are of the type `(String, TankCommand)`
For example

```scala
gameActor ! (name, TankCommand.FullSpeedAhead)
```

Commands are reset after ever game tick, so to keep accelerating forward you'll
have to keep sending the command. (Though the tank can coast if you don't 
drive the engines.)



### Task 1: Write WallPatroler (10 marks)

Your first task is to write an behaviour that will control a tank.
So for this task, you will find `SpinningDuck` useful as an example.

In terms of the exercise, this ensures you can

* Write behaviours that receive and respond to messages
* Handle state

The particular tank you are asked to write should try to drive around the boundary of the field, near the edge. 

It can take rounded corners if you want: a simple technique is to drive forwards, but check
whether a point one second's travel ahead of you is in the boundary, and if it's not, turn steer right.

It should also always try to aim its turret and radar towards the center of the field. 

It should ping the radar whenever energy is full (at `Tank.startingEnergy`), and fire whenever a RadarResult message 
shows it has seen a tank that is alive.

It should be called `WallPatroler`, so the marker knows which tank to look for, and the code should be in the `roboScala.bots` package.

**Note:**

The radar is mounted on the top of the turret. So, if you turn the turret, the radar will go with it.

#### Marking:

You'll notice there are some marks available even if WallPatroller doesn't do everything we want.

* WallPatroler is on the field (implies you've defined an actor):
  2 marks 

* WallPatroler moves (implies your actor is sending messages in response to TankState messages)
  2 marks 

* WallPatroler patrols the border: 
  2 marks

* WallPatroler points its turret towards the centre: 
  2 marks

* WallPatroler fires when it sees an enemy: 
  2 marks


### Task 2: Write your own tank (6 marks)

To make the game more interesting, we'd like *you* to write a tank of your own design.

You should include a short description of your tank's strategy in the
comments. You should demo it and talk through its design in your video.

Your tank should be called `MyVeryOwnTank` so the marker knows which tank
to look for.

Marking:
* Quality of strategy in the comments: 3 marks
* Quality of strategy implementation: 3 marks


### Task 3: Tankfighting with Insults (4 marks)

> *So, imagine we're fighting up a storm, when suddenly there's a lull
> in the action. And then I says "Soon, you'll be sitting in a pile of smoking rubble." 
> And so you says...*

This part of the assignment is inspired by a famous part of *The Secret of Monkey Island*, but will get you to use `Future`s 
and the ask pattern, and integrate them with your tank's actor.

First, add `InsultingDuck` to your game (in Main):

Take a look at its code.

InsultingDuck won't try to fight normally. Instead, it will periodically send
insults via the InsultsActor. These are then sent to tanks as an `Insulted(insult)` message.

Because `InsultsActor` uses the `ask` pattern, tanks receive this from a
*temporary actor that the InsultsActor creates specifically for handling replies*. 
The address to send a reply to is included in the message. 

The task is to alter your tank so that when it receives an insult, it fires back
a devastatingly witty retort. This needs to be an `InsultsCommand.Retort` , with the correct
response, sent to the (temporary) actor you received the `Insulted` message from.

But how are you going to get the correct retort, you may wonder.

Standing on the sidelines is `smartAlec`. Smart Alec isn't a player, but another actor you can talk to whose only
job is giving witty replies to retorts. Ask smartAlec a `SmartAlecCommand.Insult` and he'll reply with a `SmartAlecCommand.Retort`.

So, when you receive `Messages.Insulted`, you will need to:

1. ask SmartAlec for the correct reply
2. when you get his response, reply to the insult with the witty retort he just gave you.

(i.e. use the ask pattern)
  
You are also asked to *log* the insult and the retort, and to remember (and log) all
the insults you've learned so far. 

Marking: 

* Logs insults when it receives them (1 mark)
* Gets the retort from SmartAlec and logs it (1 marks)
* Successfully sends the retort to the right actor (2 marks)


### Streams and iteratees

As the game's just been updated to use Amdram, there's no task asking you to implement streams or iteratees.

There are, however, some examples of streams and iteratees present in the code.

e.g. the panel on the right hand side of the UI comes from a commentator, which receives every message and command in the game and 
transforms it to a stream of strings that are then posted into the UI.


