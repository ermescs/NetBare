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

import com.github.megatronking.netbare.NetBareConfig;

/**
 * A dumper analyzes /proc/net/ files to dump uid of the network session. This class may be a
 * battery-killer, but can set {@link NetBareConfig.Builder#dumpUid} to false to close the dumper.
 *
 * @author Megatron King
 * @since 2018-12-03 16:54
 */
public interface UidDumper {

    /**
     * Update the given session with the UID.
     *
     * @param session
     */
    public void request(final Session session);

}
