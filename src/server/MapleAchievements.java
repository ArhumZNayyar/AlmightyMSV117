/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server;

import handling.world.World;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.Map.Entry;
import tools.packet.CWvsContext;


public class MapleAchievements {

    private Map<Integer, MapleAchievement> achievements = new LinkedHashMap<Integer, MapleAchievement>();
    private static MapleAchievements instance = new MapleAchievements();

    protected MapleAchievements() {
        achievements.put(1, new MapleAchievement("getting their first point", 1200, true));
        achievements.put(2, new MapleAchievement("getting their first rebirth", 1500, true));
        achievements.put(3, new MapleAchievement("reaching their 2nd rebirth", 17500, true));
        achievements.put(4, new MapleAchievement("reaching 120 rebirth", 120800, true));
        achievements.put(5, new MapleAchievement("reaching 200 rebirth", 200000, true));
        achievements.put(7, new MapleAchievement("reached 10 fame", 500, true));
        achievements.put(9, new MapleAchievement("equipping a reverse item", 400, true));
        achievements.put(10, new MapleAchievement("equipping a timeless item", 500, true));
        achievements.put(11, new MapleAchievement("saying our server makes them cum!", 5500, true));
        achievements.put(12, new MapleAchievement("killing Anego", 3700, true));
        achievements.put(13, new MapleAchievement("killing Papulatus", 3600, true));
        achievements.put(14, new MapleAchievement("killing Pianus", 3500, true));
        achievements.put(15, new MapleAchievement("killing the almighty Zakum", 3300, true));
        achievements.put(16, new MapleAchievement("defeating Horntail", 3000, true));
        achievements.put(17, new MapleAchievement("defeating Pink Bean", 12500, true));
        achievements.put(18, new MapleAchievement("killing a boss", 100, true));
        achievements.put(19, new MapleAchievement("winning the event 'OX Quiz'", 8000, true));
        achievements.put(20, new MapleAchievement("winning the event 'MapleFitness'", 8000, true));
        achievements.put(21, new MapleAchievement("winning the event 'Ola Ola'", 8000, true));
        achievements.put(22, new MapleAchievement("defeating BossQuest HELL mode", 10500));
        achievements.put(23, new MapleAchievement("killing the Almighty Chaos Zakum", 6000, true));
        achievements.put(24, new MapleAchievement("defeating Chaos Horntail", 8000, true));
        achievements.put(25, new MapleAchievement("winning the event 'Survival Challenge'", 7800, true));
        achievements.put(26, new MapleAchievement("hitting over 10000 damage", 500, true));
        achievements.put(27, new MapleAchievement("hitting over 50000 damage", 1100, true));
        achievements.put(28, new MapleAchievement("hitting over 100000 damage", 2600, true));
        achievements.put(29, new MapleAchievement("hitting over 500000 damage", 3100, true));
        achievements.put(30, new MapleAchievement("hitting 999999 damage", 8900, true));
        achievements.put(31, new MapleAchievement("getting over 1 000 000 mesos", 200, true));
        achievements.put(32, new MapleAchievement("getting over 10 000 000 mesos", 200, true));
        achievements.put(33, new MapleAchievement("getting over 100 000 000 mesos", 200, true));
        achievements.put(34, new MapleAchievement("getting over 1 000 000 000 mesos", 200, true));
        achievements.put(35, new MapleAchievement("creating a guild", 30, true));
        achievements.put(36, new MapleAchievement("creating a family", 20, true));
        achievements.put(37, new MapleAchievement("successfully beating the Crimsonwood Party Quest", 2500, true));
        achievements.put(38, new MapleAchievement("defeating Von Leon", 3050, true));
        achievements.put(39, new MapleAchievement("defeating Empress Cygnus", 15000, true));
        achievements.put(40, new MapleAchievement("equipping am item above level 130", 400, true));
        achievements.put(41, new MapleAchievement("equipping am item above level 140", 550, true));
        achievements.put(42, new MapleAchievement("getting their first Paragon", 1100, true));
        achievements.put(43, new MapleAchievement("reaching 70 Paragons", 300, true));
        achievements.put(44, new MapleAchievement("reaching 120 Paragons", 700, true));
        achievements.put(45, new MapleAchievement("reaching 200 Paragons", 2050, true));
        achievements.put(46, new MapleAchievement("reaching level 200 Congratulate them on such an amazing achievement!", 5000, true));
        achievements.put(47, new MapleAchievement("reaching level 120 Congratulate them on such an amazing achievement!", 3500, true));
        
    }

    public static MapleAchievements getInstance() {
        return instance;
    }

    public MapleAchievement getById(int id) {
        return achievements.get(id);
    
    }

    public Integer getByMapleAchievement(MapleAchievement ma) {
        for (Entry<Integer, MapleAchievement> achievement : this.achievements.entrySet()) {
            if (achievement.getValue() == ma) {
                return achievement.getKey();
            }
        }
        return null;
    }
}
