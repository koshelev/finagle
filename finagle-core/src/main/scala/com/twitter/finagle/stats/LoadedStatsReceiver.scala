package com.twitter.finagle.stats

import com.twitter.finagle.util.LoadService

/**
 * A [[com.twitter.finagle.stats.StatsReceiver]] that loads
 * all service-loadable receivers and broadcasts stats to them.
 */
object LoadedStatsReceiver extends {
  @volatile var self: StatsReceiver = {
    val receivers = LoadService[StatsReceiver]()
    BroadcastStatsReceiver(receivers)
  }
} with StatsReceiverProxy

/**
 * A "default" StatsReceiver loaded by Finagle's
 * [[com.twitter.finagle.util.LoadService]] mechanism.
 */
object DefaultStatsReceiver extends {
  val self: StatsReceiver = LoadedStatsReceiver
} with StatsReceiverProxy {
  val get = this
}

/**
 * A client-specific StatsReceiver. All stats recorded using this receiver
 * are prefixed with the string "clnt" by default.
 */
object ClientStatsReceiver extends StatsReceiverProxy {
  @volatile private[this] var _self: StatsReceiver = LoadedStatsReceiver.scope("clnt")
  def self: StatsReceiver = _self
  def setRootScope(rootScope: String) {
    _self = LoadedStatsReceiver.scope(rootScope)
  }
}

/**
 * A server-specific StatsReceiver. All stats recorded using this receiver
 * are prefixed with the string "srv" by default.
 */
object ServerStatsReceiver extends StatsReceiverProxy {
  @volatile private[this] var _self: StatsReceiver = LoadedStatsReceiver.scope("srv")
  def self: StatsReceiver = _self
  def setRootScope(rootScope: String) {
    _self = LoadedStatsReceiver.scope(rootScope)
  }
}