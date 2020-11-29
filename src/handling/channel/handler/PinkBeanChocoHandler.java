/* 
    This file is part of the OdinMS Maple Story Server 
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
               Matthias Butz <matze@odinms.de> 
               Jan Christian Meyer <vimes@odinms.de> 

    This program is free software: you can redistribute it and/or modify 
    it under the terms of the GNU Affero General Public License as 
    published by the Free Software Foundation version 3 as published by 
    the Free Software Foundation. You may not use, modify or distribute 
    this program under any other version of the GNU Affero General Public 
    License. 

    This program is distributed in the hope that it will be useful, 
    but WITHOUT ANY WARRANTY; without even the implied warranty of 
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
    GNU Affero General Public License for more details. 

    You should have received a copy of the GNU Affero General Public License 
    along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/ 
package handling.channel.handler; 

import client.MapleCharacter; 
import client.MapleClient; 
import handling.MapleServerHandler;
//import net.AbstractMaplePacketHandler; this is for v148 correct one is ^ for v117 - Rummy
import tools.data.input.SeekableLittleEndianAccessor; 
import tools.packet.CWvsContext; 

/** 
 * 
 * @author Sai 
 */ 
public class PinkBeanChocoHandler extends MapleServerHandler { 
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) { 
        MapleCharacter chr = c.getPlayer(); 
        if (chr == null || chr.hasBlockedInventory()) { 
            return; 
        } 
        // 0x1: First chocolate, 3994200 
        int flag = 0; 
        for (int i = 0, j = 0x1; i < 9; i ++, j *= 2) { 
            if (chr.haveItem(3994200 + i)) { 
                flag |= j; 
            } 
        } 
        c.getSession().write(CWvsContext.sendPinkBeanChoco(true, false, flag)); 
    } 
}  