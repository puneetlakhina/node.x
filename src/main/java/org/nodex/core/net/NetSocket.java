/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.nodex.core.net;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.util.CharsetUtil;
import org.nodex.core.Actor;
import org.nodex.core.ConnectionBase;
import org.nodex.core.Nodex;
import org.nodex.core.buffer.Buffer;
import org.nodex.core.buffer.DataHandler;
import org.nodex.core.streams.ReadStream;
import org.nodex.core.streams.WriteStream;

import java.io.File;
import java.nio.charset.Charset;

public class NetSocket extends ConnectionBase implements ReadStream, WriteStream {

  private DataHandler dataHandler;
  private Runnable endHandler;
  private Runnable drainHandler;

  public final long writeActorID;

  NetSocket(Channel channel, long contextID, Thread th) {
    super(channel, contextID, th);

    writeActorID = Nodex.instance.registerActor(new Actor<Buffer>() {
      public void onMessage(Buffer buff) {
        writeBuffer(buff);
      }
    });
  }

  public void writeBuffer(Buffer data) {
    doWrite(data._getChannelBuffer());
  }

  public NetSocket write(Buffer data) {
    doWrite(data._getChannelBuffer());
    return this;
  }

  public NetSocket write(String str) {
    doWrite(ChannelBuffers.copiedBuffer(str, CharsetUtil.UTF_8));
    return this;
  }

  public NetSocket write(String str, String enc) {
    doWrite(ChannelBuffers.copiedBuffer(str, Charset.forName(enc)));
    return this;
  }

  public NetSocket write(Buffer data, final Runnable done) {
    addFuture(done, doWrite(data._getChannelBuffer()));
    return this;
  }

  public NetSocket write(String str, Runnable done) {
    addFuture(done, doWrite(ChannelBuffers.copiedBuffer(str, CharsetUtil.UTF_8)));
    return this;
  }

  public NetSocket write(String str, String enc, Runnable done) {
    addFuture(done, doWrite(ChannelBuffers.copiedBuffer(str, Charset.forName(enc))));
    return this;
  }

  public void dataHandler(DataHandler dataHandler) {
    checkThread();
    this.dataHandler = dataHandler;
  }

  public void endHandler(Runnable endHandler) {
    checkThread();
    this.endHandler = endHandler;
  }

  public void drainHandler(Runnable drained) {
    checkThread();
    this.drainHandler = drained;
    callDrainHandler(); //If the channel is already drained, we want to call it immediately
  }

  public void sendFile(String filename) {
    checkThread();
    File f = new File(filename);
    super.sendFile(f);
  }

  protected void handleClosed() {
    super.handleClosed();
  }

  protected long getContextID() {
    return super.getContextID();
  }

  protected void handleException(Exception e) {
    super.handleException(e);
  }

  void handleInterestedOpsChanged() {
    setContextID();
    callDrainHandler();
  }

  void handleDataReceived(Buffer data) {
    if (dataHandler != null) {
      setContextID();
      try {
        dataHandler.onData(data);
      } catch (Throwable t) {
        handleHandlerException(t);
      }
    }
  }

  private ChannelFuture doWrite(ChannelBuffer buff) {
    checkThread();
    return channel.write(buff);
  }

  private void callDrainHandler() {
    if (drainHandler != null) {
      if ((channel.getInterestOps() & Channel.OP_WRITE) == Channel.OP_WRITE) {
        try {
          drainHandler.run();
        } catch (Throwable t) {
          handleHandlerException(t);
        }
      }
    }
  }
}

