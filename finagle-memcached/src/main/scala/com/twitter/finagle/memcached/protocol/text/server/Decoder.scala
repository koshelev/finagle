package com.twitter.finagle.memcached.protocol.text.server

import org.jboss.netty.channel._
import com.twitter.util.StateMachine
import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.memcached.protocol.{NonStorageCommand, StorageCommand, ClientError, Command}
import com.twitter.finagle.memcached.util.ParserUtils
import com.twitter.finagle.memcached.protocol.text.{MemcachedCommandVocabulary, CommandVocabulary, Show, AbstractDecoder}

class Decoder(parser: MemcachedCommandVocabulary) extends AbstractDecoder[Command] with StateMachine {
  import ParserUtils._

  case class AwaitingCommand() extends State
  case class AwaitingData(tokens: Seq[ChannelBuffer], bytesNeeded: Int) extends State

  final protected[memcached] def start() {
    state = AwaitingCommand()
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    Channels.write(ctx.getChannel, Show(e.getCause))
    super.exceptionCaught(ctx, e)
  }

  def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): Command = {
    state match {
      case AwaitingCommand() =>
        decodeLine(buffer, parser.needsData(_)) { tokens =>
          parser.parseNonStorageCommand(tokens)
        }
      case AwaitingData(tokens, bytesNeeded) =>
        decodeData(bytesNeeded, buffer) { data =>
          parser.parseStorageCommand(tokens, data)
        }
    }
  }

  final protected[memcached] def awaitData(tokens: Seq[ChannelBuffer], bytesNeeded: Int) = {
    state = AwaitingData(tokens, bytesNeeded)
    needMoreData
  }

  protected val needMoreData: Command = null
}