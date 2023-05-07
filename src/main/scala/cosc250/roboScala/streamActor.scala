package cosc250.roboScala

import com.wbillingsley.amdram.* 
import iteratees.*
import scala.collection.immutable.Queue

import cosc250.roboScala.*
import game.*

import scala.concurrent.*
import scala.concurrent.duration._

import ExecutionContext.Implicits.global


case class RegisterSink(iter:Iteratee[(String, Command | Message),_])

/**
  * The streamActor is responsible for sending out a listenable stream of every tank command and message happening 
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
def streamBehaviour(
  subscribers:Queue[UnboundedBuffer[(String, Command | Message)]]
)(
  using ec:ExecutionContext
):MessageHandler[(String, Command | Message) | RegisterSink] = MessageHandler { (msg, context) =>

  msg match 
    case RegisterSink(iter) =>
      info("Registered the command stream")

      // Create a new buffer for this recipient (because it might not process messages quickly)
      // This buffer will do its work sending the messages on to recipients on an implicit execution context
      val buffer = UnboundedBuffer[(String, Command | Message)]()

      // Tell the buffer to send out every message it receives to the subscriber
      buffer.foldOver(iter)

      // Add this buffer to our state, so we push to this buffer whenever a new `(name, command)` pair comes in
      streamBehaviour(subscribers.enqueue(buffer))

    case (n, c:(Command | Message)) => 
      // Send the message out to every subscribed queue
      for s <- subscribers do s.push(n -> c)
      
      // We continue with the same behaviour, so do not return a new message handler

}

val streamActor = troupe.spawn(streamBehaviour(Queue.empty))


