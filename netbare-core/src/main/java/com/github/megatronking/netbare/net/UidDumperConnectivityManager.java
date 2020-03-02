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
package com.github.megatronking.netbare.net;

import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Process;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.annotation.RequiresApi;

import com.github.megatronking.netbare.NetBareConfig;
import com.github.megatronking.netbare.NetBareLog;
import com.github.megatronking.netbare.NetBareUtils;
import com.github.megatronking.netbare.ip.Protocol;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiresApi(api = Build.VERSION_CODES.Q)
public final class UidDumperConnectivityManager implements UidDumper {
    private final UidProvider mUidProvider;
    private String mLocalIp;
    private ConnectivityManager connectivityManager;
    private PackageManager packageManager;

    public UidDumperConnectivityManager(ConnectivityManager connectivityManager, PackageManager packageManager, String localIp, UidProvider provider) {
        this.mLocalIp = localIp;
        this.mUidProvider = provider;
        this.connectivityManager = connectivityManager;
        this.packageManager = packageManager;
    }

    public void request(final Session session) {
        if (mUidProvider != null) {
            int uid = mUidProvider.uid(session);
            if (uid != UidProvider.UID_UNKNOWN) {
                session.uid = uid;
                return;
            }
        }

        int protocol;

        switch (session.protocol) {
            case TCP:
                protocol = OsConstants.IPPROTO_TCP;
                break;
            case UDP:
                protocol = OsConstants.IPPROTO_UDP;
                break;
            default:
                return;
        }

        InetSocketAddress source = new InetSocketAddress(this.mLocalIp, NetBareUtils.convertPort(session.localPort));
        InetSocketAddress destination = new InetSocketAddress(NetBareUtils.convertIp(session.remoteIp), NetBareUtils.convertPort(session.remotePort));
        int connectionOwnerUid = connectivityManager.getConnectionOwnerUid(protocol, source, destination);

        if (connectionOwnerUid == Process.INVALID_UID) {
            session.uid = UidProvider.UID_UNKNOWN;
        } else {
            session.uid = connectionOwnerUid;
        }
    }

}
