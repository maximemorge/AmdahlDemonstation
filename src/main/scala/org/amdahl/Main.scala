// Copyright (C) Dialectics 2016
package org.amdahl

import akka.actor.{ActorSystem, Props}

import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import org.amdahl.actor.{Start, Stop, Supervisor}

import scala.concurrent.duration._

/**
  * Main app to compute Pi
  * */
object Main {
  val debug=false
  val system = ActorSystem("AmdhalDemonstration")
  var idSupervisor=1// give a different id to each spuervisor
  val TIMEOUTVALUE=50 seconds// default timeout of a run
  implicit val timeout = Timeout(TIMEOUTVALUE)

  val NBTASKS=1e7.toInt// number of shoots
  val MAXNUMWORKER=10// maximum number of workers
  val NBRUNS=10// for each number of workers , we consider NBRUNS runs

/**
* Run the Actor system with the following default dispatcher and print a CSV file nbwWorkers,speedup
* @see https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html
* fork-join-executor {
*        # Min number of threads to cap factor-based parallelism number to
*        parallelism-min = 8
*
*        # The parallelism factor is used to determine thread pool size using the
*       # following formula: ceil(available processors * factor). Resulting size
*        # is then bounded by the parallelism-min and parallelism-max values.
*        parallelism-factor = 3.0
*
*        # Max number of threads to cap factor-based parallelism number to
*        parallelism-max = 64
*
*        # Setting to "FIFO" to use queue like peeking mode which "poll" or "LIFO" to use stack
*        # like peeking mode which "pop".
*        task-peeking-mode = "FIFO"
*      }
*/
  def main(args: Array[String]): Unit = {
    //This UNIX command returns the number physical core:
    // sysctl -n machdep.cpu.core_count
    //This command returns the number of logical threads in the JVM
    println("Number of logical CPUs "+Runtime.getRuntime().availableProcessors())
    val referenceTime = runExperiment(1)// the reference time with a single worker
    println("1,1.0")
    for (nbWorkers <- 2 to MAXNUMWORKER.toInt by 1){// for each number of workers
      val runningTime = runExperiment(nbWorkers)// run an experiment
      val speedup = referenceTime / runningTime// compute the speedup
      println(nbWorkers + "," + speedup)
    }
    System.exit(0)
  }


  /**
    * Execute an experiment, i.e. we consider NBRUNS runs for each number of workers
    * @param nbWorkers
    * @return the average running time in nanoseconds
    *
    */
  def runExperiment(nbWorkers: Int): Double = {
    val runningTimes: Seq[Double] = for (run <- 1 to NBRUNS) yield {
      this.runActor(nbWorkers)
    }
    val runningTime = runningTimes.sum / runningTimes.length
    return runningTime
  }

/**
  * Execute a single run with Actors
  * @param nbWorkers
  * @return the running time in nanoseconds
  *
 */
  def runActor(nbWorkers: Int): Double ={
    // Launch a new supervisor
    val supervisor = system.actorOf(Props(classOf[Supervisor], nbWorkers, NBTASKS), name = "supervisor" + idSupervisor)
    idSupervisor+=1// increment idSupervisor to have supervisors with different names
    // The current thread is blocked and it waits for the supervisor to "complete" the Future with it's reply.
    val future = supervisor ? Start
    val result = Await.result(future, timeout.duration).asInstanceOf[Stop]
    if (debug) print(result.time + " ")
    return result.time
  }


  /**
    * Execute a single run with Thread
    * @param nbWorkers
    * @return the running time in nanoseconds
    *
    */
  def runThread(nbWorkers: Int): Double ={
    // Launch
    val time=0
    if (debug) print(time + " ")
    return time
  }


}
