// Copyright (C) Dialectics 2016
package org.amdahl.actor

import akka.actor.Actor
import org.amdahl.task.Task

/**
  * Worker which performs the local computation as a slave
  * @param id of the worker for debugging
  * */
class Worker(id: Int) extends Actor {
  val debug = false

  /**
    * Method invoked when a message is received
    */
  def receive = {
    // When the worker receive the start message
    case Run(intensity: Int) =>
      if (debug) println("Worker "+id+" runs "+intensity+" operations")
      val task= new Task()
      val result= task.cpuIntensive(intensity)// computes
      sender ! Done(result)// reports
  }


}
