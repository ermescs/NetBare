/*  NetBare - An android network capture and injection library.
 *  Copyright (C) 2018-2019 Megatron King
 *  Copyright (C) 2018-2019 GuoShi
 *
 *  NetBare is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Found-
 *  ation, either version 3 of the License, or (at your option) any later version.
 *
 *  NetBare is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with NetBare.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.megatronking.netbare.http2;

import android.support.annotation.NonNull;

import com.github.megatronking.netbare.NetBareUtils;
import com.github.megatronking.netbare.NetBareXLog;
import com.github.megatronking.netbare.gateway.InterceptorChain;
import com.github.megatronking.netbare.http.HttpIndexInterceptor;
import com.github.megatronking.netbare.http.HttpProtocol;
import com.github.megatronking.netbare.http.HttpRequest;
import com.github.megatronking.netbare.http.HttpRequestChain;
import com.github.megatronking.netbare.http.HttpResponse;
import com.github.megatronking.netbare.http.HttpResponseChain;
import com.google.common.primitives.Bytes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Encodes HTTP2 request and response packets.
 *
 * @author Megatron King
 * @since 2019/1/5 14:24
 */
public final class Http2EncodeInterceptor extends HttpIndexInterceptor {

    private NetBareXLog mLog;

    @Override
    protected void intercept(@NonNull HttpRequestChain chain, @NonNull ByteBuffer buffer, int index)
            throws IOException {
        if (chain.request().httpProtocol() == HttpProtocol.HTTP_2) {
            if (mLog == null) {
                HttpRequest request = chain.request();
                mLog = new NetBareXLog(request.protocol(), request.ip(), request.port());
            }
            mLog.w("request: " + new String(buffer.array(), buffer.position(),
                    buffer.remaining()));
            if (index == 0) {
                encodeRequestHeader(chain);
            } else {
                encodeRequestData(chain, buffer);
            }
        } else {
            chain.process(buffer);
        }

    }

    @Override
    protected void intercept(@NonNull HttpResponseChain chain, @NonNull ByteBuffer buffer, int index)
            throws IOException {
        if (chain.response().httpProtocol() == HttpProtocol.HTTP_2) {
            if (mLog == null) {
                HttpResponse response = chain.response();
                mLog = new NetBareXLog(response.protocol(), response.ip(), response.port());
            }
            mLog.w("response: " + new String(buffer.array(), buffer.position(), buffer.remaining()));
            if (index == 0) {
                encodeResponseHeader(chain);
            } else {
                encodeResponseData(chain, buffer);
            }
        } else {
            chain.process(buffer);
        }
    }

    private void encodeRequestHeader(HttpRequestChain chain) throws IOException {
        HttpRequest request = chain.request();
        Hpack.Writer writer = new Hpack.Writer();
        Http2Settings peerHttp2Settings = request.peerHttp2Settings();
        if (peerHttp2Settings != null) {
            int headerTableSize = peerHttp2Settings.getHeaderTableSize();
            if (headerTableSize != -1) {
                writer.setHeaderTableSizeSetting(headerTableSize);
            }
        }
        byte[] headerBlock = writer.writeRequestHeaders(request.method(), request.path(), request.host(),
                request.requestHeaders());
        sendHeaderBlockFrame(chain, headerBlock, peerHttp2Settings, request.streamId());
    }

    private void encodeResponseHeader(HttpResponseChain chain) throws IOException {
        HttpResponse response = chain.response();
        Hpack.Writer writer = new Hpack.Writer();
        Http2Settings clientHttp2Settings = response.clientHttp2Settings();
        if (clientHttp2Settings != null) {
            int headerTableSize = clientHttp2Settings.getHeaderTableSize();
            if (headerTableSize != -1) {
                writer.setHeaderTableSizeSetting(headerTableSize);
            }
        }
        byte[] headerBlock = writer.writeResponseHeaders(response.code(), response.message(),
                response.responseHeaders());
        sendHeaderBlockFrame(chain, headerBlock, clientHttp2Settings, response.streamId());
    }

    private void encodeRequestData(HttpRequestChain chain, ByteBuffer buffer) throws IOException {
        byte[] data = Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.limit());
        HttpRequest request = chain.request();
        sendDataFrame(chain, data, request.peerHttp2Settings(), request.streamId());
    }

    private void encodeResponseData(HttpResponseChain chain, ByteBuffer buffer) throws IOException {
        byte[] data = Arrays.copyOfRange(buffer.array(), buffer.position(), buffer.limit());
        HttpResponse response = chain.response();
        sendDataFrame(chain, data, response.clientHttp2Settings(), response.streamId());
    }

    private void sendHeaderBlockFrame(InterceptorChain chain, byte[] headerBlock, Http2Settings http2Settings,
                                 int streamId) throws IOException  {
        int maxFrameSize = http2Settings == null ? Http2.INITIAL_MAX_FRAME_SIZE :
                http2Settings.getMaxFrameSize(Http2.INITIAL_MAX_FRAME_SIZE);
        int byteCount = headerBlock.length;
        int length = Math.min(maxFrameSize, byteCount);
        byte type = FrameType.HEADERS.get();
        byte flags = byteCount == length ? Http2.FLAG_END_HEADERS : 0;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(frameHeader(streamId, length, type, flags));
        os.write(headerBlock, 0, length);
        chain.process(ByteBuffer.wrap(os.toByteArray()));
        if (byteCount > length) {
            byte[] left = Arrays.copyOfRange(headerBlock, length, byteCount);
            sendContinuationFrame(chain, left, streamId, maxFrameSize, byteCount - length);
        }
    }

    private void sendContinuationFrame(InterceptorChain chain, byte[] headerBlock, int streamId,
                                       int maxFrameSize, long byteCount) throws IOException {
        int offset = 0;
        while (byteCount > 0) {
            int length = (int) Math.min(maxFrameSize, byteCount);
            byteCount -= length;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(frameHeader(streamId, length, FrameType.CONTINUATION.get(), byteCount == 0 ?
                    Http2.FLAG_END_HEADERS : 0));
            os.write(headerBlock, offset, length);
            offset += length;
            chain.process(ByteBuffer.wrap(os.toByteArray()));
        }
    }

    private void sendDataFrame(InterceptorChain chain, byte[] data, Http2Settings http2Settings,
                               int streamId) throws IOException {
        boolean isFinished = false;
        int endLineIndex = Bytes.indexOf(data, NetBareUtils.PART_END_BYTES);
        if (endLineIndex != -1) {
            byte[] newData = new byte[data.length - (endLineIndex + NetBareUtils.PART_END_BYTES.length)];
            System.arraycopy(data, 0, newData, 0, newData.length);
            data = newData;
            isFinished = true;
        }
        int maxFrameSize = http2Settings == null ? Http2.INITIAL_MAX_FRAME_SIZE :
                http2Settings.getMaxFrameSize(Http2.INITIAL_MAX_FRAME_SIZE);
        int byteCount = data.length;
        byte type = FrameType.DATA.get();
        byte flags = isFinished ? Http2.FLAG_END_STREAM : 0;
        int offset = 0;
        if (byteCount == 0) {
            chain.process(ByteBuffer.wrap(frameHeader(streamId, 0, type, flags)));
        } else {
            while (byteCount > 0) {
                int length = Math.min(maxFrameSize, byteCount);
                byteCount -= length;
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                os.write(frameHeader(streamId, length, type, byteCount <= 0 ? flags : 0));
                os.write(data, offset, length);
                offset += length;
                chain.process(ByteBuffer.wrap(os.toByteArray()));
            }
        }
    }

    private byte[] frameHeader(int streamId, int length, byte type, byte flags) {
        mLog.i("Encode a http2 frame: " + FrameType.parse(type) + " stream(" + streamId +
                ") length(" + length + ")");
        ByteBuffer header = ByteBuffer.allocate(Http2.FRAME_HEADER_LENGTH);
        header.put((byte) ((length >>> 16) & 0xff));
        header.put((byte) ((length >>> 8) & 0xff));
        header.put((byte) (length & 0xff));
        header.put((byte) (type & 0xff));
        header.put((byte) (flags & 0xff));
        header.putInt(streamId & 0x7fffffff);
        return header.array();
    }

}
