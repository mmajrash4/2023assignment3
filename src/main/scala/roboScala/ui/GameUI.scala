package roboScala.ui

import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing._

import roboScala.*
import game.*

import com.wbillingsley.amdram.* 
import iteratees.*

import scala.concurrent.*
import ExecutionContext.Implicits.global

import scala.collection.mutable


/** The UI for a game. Note that it takes a by-name argument. */
object GameUI {

  @volatile var gameState:GameState = GameState.empty
  @volatile var commands:Map[String, Seq[Command]] = Map.empty

  // Draws the tanks, etc
  val gamePanel = new GamePanel(gameState, commands)

  // Starts the game
  val startButton = new JButton("Start game!")
  startButton.addActionListener((e:ActionEvent) =>     
    registerCommandStream()
    startGame()
  )

  /** The eastern panel, showing the log of message */
  val messagesPanel = new Box(BoxLayout.Y_AXIS)
  val messageTextArea = new JTextArea("Commands and messages from the tanks will appear here once a stream is registered")
  messageTextArea.setLineWrap(true)
  messageTextArea.setWrapStyleWord(true)
  messageTextArea.setColumns(20)

  val messages:mutable.Queue[String] = mutable.Queue.empty


  val filters = new Box(BoxLayout.LINE_AXIS)
  messagesPanel.add(filters)
  messagesPanel.add(messageTextArea)
  messagesPanel.add(Box.createGlue())

  /** The western panel, holding the state of all the tanks */
  private val commandsPanel = new Box(BoxLayout.Y_AXIS)
  commandsPanel.add(Box.createGlue())

  /** Each command panel shows the state of a single tank */
  private val commandPanels = mutable.Buffer.empty[CommandPanel]

  /** The main window */
  val frame = new JFrame("RoboScala")
  frame.setLayout(new BorderLayout())
  frame.add(gamePanel, BorderLayout.CENTER)
  frame.add(messagesPanel, BorderLayout.EAST)
  frame.add(commandsPanel, BorderLayout.WEST)
  frame.add(startButton, BorderLayout.SOUTH)
  frame.setSize(1024, 768)
  frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
  frame.setVisible(true)

  /** Called by the game actor to request the game panel be repainted */
  def repaint():Unit = {
    val commands = this.commands
    for { p <- commandPanels.toSeq } p.update(commands.getOrElse(p.name, Seq.empty).toSet)

    gamePanel.repaint()
  }

  /** Adds a tank to the ui */
  def addTank(name:String):Unit = {
    val p = new CommandPanel(name)
    commandPanels.append(p)
    commandsPanel.add(p, 0)

    // trigger a re-layout
    frame.revalidate()
  }

  /** Pushes a command from a tank into the commands that will be rendered. */
  def pushCommentary(s:String):Unit = {
    messages.enqueue(s)
    if (messages.lengthCompare(12) > 0) messages.dequeue()
    updateLog()
  }

  /** Updates the messageTextArea to display the commands in the log */
  def updateLog():Unit = {
    val text = messages.mkString("\n")

    SwingUtilities.invokeLater { () =>
      messageTextArea.setText(text)
      messageTextArea.repaint()
    }
  }


  val commentator:Transformer[(String, Command | Message), String] = {
    case Input.Datum((name, c)) => c match {
      case TankCommand.Fire => Some(Input.Datum(s"$name fires a shot"))
      case Message.YouMissed(_) =>  Some(Input.Datum(s"$name's shell goes into the ground"))
      case Message.YouHit(t) =>  Some(Input.Datum(s"$name scores a mighty hit on ${t.name}"))
      case Message.RadarResult(_, tanks) if tanks.nonEmpty => Some(Input.Datum(s"$name spots ${tanks.map(_.name).mkString(", ")}"))
      case InsultsCommand.Insult(t, insult) => Some(Input.Datum(s"$name sneers at $t and says '$insult'"))
      case InsultsCommand.Retort(retort) => Some(Input.Datum(s"$name replies '$retort'"))
      case TankCommand.TakeHit => Some(Input.Datum(s"$name took a hit"))
      case _ => None
    }
    case x => None
  }


  def registerCommandStream() = {
    object CommandSink extends Iteratee[String, Unit] {
      override def accept(el:Input[String]) = { 
        
        el match
          case Input.Datum(text) =>
            ui.GameUI.pushCommentary(text)
            Future.successful(RequestState.Continue(this))
          case Input.Error(x) =>
            error(x.getMessage())
            Future.successful(RequestState.Done(()))
          case Input.EndOfStream => 
            Future.successful(RequestState.Done(()))
      }
      
    }

    streamActor ! RegisterSink(AdaptedIteratee(CommandSink)(commentator))
  }



}
