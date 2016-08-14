// Copyright (C) Dialectics 2016
package org.amdahl

import akka.actor.{Actor, ActorRef, Props}

/**
  * Supervisor which starts and stopq the computation of PI
  * with nbWorkers slaves to performs the computation
  * with nbTasks "atomic" operations
  * */
class Supervisor(nbWorkers: Int=1000, nbTasks: Int =1e6.toInt) extends Actor {
  val debug=false
  var boss : ActorRef = _// reference to the main Actor system
  var workers = Seq[ActorRef]()// the references to the workers

  val nbLocalTasks = nbTasks/nbWorkers// nb of operators per workers

  var startingTime=0.0// the clock
  var nbStoppedWorkers=0// how many workers have finished
  var totalCountInCircle=0.0// how many shoots are in the circle

  /**
    * Method invoked after starting the actor
   */
  override def preStart(): Unit = {
    // start of the workers
    workers = for(i <- 1 to nbWorkers) yield context.actorOf(Props(classOf[Worker], i), "worker"+i)
  }

  /**
    * Method invoked when a message is received
    */
  def receive = {
    // When the works should be done
    case Start => {
      boss= sender// note the reference to the application
      startingTime= System.nanoTime// start the clock
      // signal all workers that they should start and run nbLocalTasks tasks
      workers.foreach(_ !  Run(nbLocalTasks))
    }
    // When a work is done
    case Done(countInCircle) => {
      nbStoppedWorkers += 1// One more work done
      totalCountInCircle+=countInCircle// Add the nob of shoots in the circle

      if (nbStoppedWorkers == nbWorkers){// And all the work is done
        val pi= totalCountInCircle/nbTasks*4// compute Pi
        if (debug) println("Pi= "+pi)
        var elapsedTime = System.nanoTime - startingTime// stop the clock
        if (debug) println("Workers "+nbWorkers+" runs "+elapsedTime/ 1e9+" s")
        boss ! Stop(elapsedTime)// report the computation time to the application
        context.stop(self)//stop the supervisor
      }
    }
  }
}
