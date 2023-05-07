package cosc250.roboScala.game

import scala.collection.mutable

// You should not need to edit this file.



/**
  * The state of the game is the state of all the tanks on the field and shells currently in motion.
  *
  * @param tanks - tanks on the field
  * @param shells - shells in motion
  */
case class GameState(tanks:Seq[Tank], shells:Seq[Shell]) {

  /**
    * Updates the game state, returning a new game state and a sequence of messages to be sent
    * 
    * @param dt - the time in ms since the last step
    * @param commands - the set of commands to apply for each tank
    */
  def step(dt:Double, commands:Map[String, Seq[TankCommand]]):(GameState, Seq[(String, Message)]) = {

    import Message.*
    
    val messages = mutable.Buffer.empty[(String, Message)]

    // Handle hits. Each hit tank has a TakeHit command added, and we remember the shells
    // in order to remove them from the field (later)
    val hits = for {
      s <- shells
      t <- tanks if t.hitBy(s)
    } yield {
      messages.append(s.firedBy -> YouHit(t))
      messages.append(t.name -> YouWereHit)

      (t, s)
    }
    def hitTanks = hits.map(_._1)
    def hitShells = hits.map(_._2)

    // Handle misses. For each shell that has gone out of bounds, we notify the player
    // who fired it. We remember these shells in order to remove them from the field (later)
    val missedShells = for {
      s <- shells if !GameState.inBounds(s.position)
    } yield {
      // Tell the player that fired the shot they missed
      messages.append(s.firedBy -> YouMissed(s))

      s
    }

    // Update tanks' states
    val newTanks = for {
      t <- tanks
    } yield {
      // Only alive tanks are updated. Dead tanks remain unchanged (but are not filtered out)
      if (t.isAlive) {
        // Augment the commands list for this tank with any hits it's taken
        val tankCommands = commands.getOrElse(t.name, Seq.empty) ++ (for hit <- hitTanks if hit == t yield TankCommand.TakeHit)
        t.updated(dt, tankCommands)
      } else t
    }

    // Perform radar detection for any tanks that have a RadarPing command and the
    // energy to make it succeed.
    for {
      t <- tanks if t.canPing && commands.getOrElse(t.name, Seq.empty).contains(TankCommand.RadarPing)
    } {
      val cone = t.transformedRadarCone
      val seen = tanks.filter(ot => ot != t &&
        cone.intersects(ot.transformedBody.getBounds)
      )

      messages.append(t.name -> RadarResult(t, seen.toSet))
    }

    // Create new shells for tanks that have a Fire command and the energy to make it
    // succeed.
    val newShells:Seq[Shell] = for {
      t <- tanks if {
        t.canFire && commands.getOrElse(t.name, Seq.empty).contains(TankCommand.Fire)
      }
    } yield {
      Shell(
        firedBy = t.name,
        position = t.position + Vec2.fromRTheta(Tank.barrelLength, t.turretFacing),
        velocity = Vec2.fromRTheta(Shell.velocity, t.turretFacing)
      )
    }

    // Work out what shells are in the next frame and where. This includes removing the
    // shells that have hit or gone out of bounds, and adding the new shells that have
    // been fired.
    val nextShells = (for {
      s <- shells diff (hitShells ++ missedShells)
    } yield s.updated(dt)) ++ newShells

    // We now have enough to create the new gameState
    val newGS = GameState(newTanks, nextShells)

    // Check whether the list of alive tanks has changed. If so, notify everyone
    val liveNames = tanks.filter(_.isAlive).map(_.name).sortBy(identity)
    val nextLiveNames = newTanks.filter(_.isAlive).map(_.name).sortBy(identity)
    
    if (liveNames != nextLiveNames) then
      for t <- tanks do
        messages.append(t.name -> TanksAlive(nextLiveNames))

    (newGS, messages.toSeq)
  }

}

object GameState {
  def empty = GameState(Seq.empty, Seq.empty)

  val width = 640

  val height = 480

  /** Returns whether a point is within the game area */
  def inBounds(p:Vec2, buffer:Double = 20):Boolean = {
    p.x > buffer && p.x < width - buffer &&
      p.y > buffer && p.y < height - buffer
  }

}