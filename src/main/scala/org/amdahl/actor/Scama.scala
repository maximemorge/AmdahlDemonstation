// Copyright (C) Dialectics 2016
package org.amdahl.actor

/**
  *  All possible messages between the actors
 */
class Message
case object Start extends Message// start one computation
case class Stop(time: Double) extends  Message// computation is stopped and report the computation time
case class Run(nbOperation: Int) extends Message// run a local task with nbOperation atomic operations
case class Done(result: Double) extends Message// local task is performed and reports the result
