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

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.os.Process;

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

/**
 * A dumper analyzes /proc/net/ files to dump uid of the network session. This class may be a
 * battery-killer, but can set {@link NetBareConfig.Builder#dumpUid} to false to close the dumper.
 *
 * @author Megatron King
 * @since 2018-12-03 16:54
 */
public interface UidDumper {

    /**
     *  Update the given session with the UID. Currently does not work with Android 10
     *
     * @param session
     */
    public void request(final Session session);

}
