package cosc250.roboScala

import com.wbillingsley.amdram.* 
import iteratees.*
import scala.collection.immutable.Queue

import cosc250.roboScala.*
import game.*

import scala.concurrent.*
import scala.concurrent.duration._

import ExecutionContext.Implicits.global


case class RegisterSink(iter:Iteratee[(String, Command),_])

/**
  * The commandStreamActor is responsible for sending out a listenable stream of every tank command happening 
  * in the game. It is defined by this behaviour:
  *
  * For every subscriber request, it maintains an unbounded queue. This means that whenever a 
  * `(name, command)` touple comes in, it can immediately push it out to all queues, without having to block on 
  * whether any recipients were ready to receive it.
  * 
  * @param subscribers
  * @param ec
  * @return
  */
def commandStreamBehaviour(
  subscribers:Queue[UnboundedBuffer[(String, Command)]]
)(
  using ec:ExecutionContext
):MessageHandler[(String, Command) | RegisterSink] = MessageHandler { (msg, context) =>

  msg match 
    case RegisterSink(iter) =>
      // Create a new buffer for this recipient (because it might not process messages quickly)
      // This buffer will do its work sending the messages on to recipients on an implicit execution context
      val buffer = UnboundedBuffer[(String, Command)]()

      // Tell the buffer to send out every message it receives to the subscriber
      buffer.foldOver(iter)

      // Add this buffer to our state, so we push to this buffer whenever a new `(name, command)` pair comes in
      commandStreamBehaviour(subscribers.enqueue(buffer))

    case (n, c:Command) => 
      // Send the message out to every subscribed queue
      for s <- subscribers do s.push(n -> c)
      
      // We continue with the same behaviour, so do not return a new message handler

}

val commandStreamActor = troupe.spawn(commandStreamBehaviour(Queue.empty))
