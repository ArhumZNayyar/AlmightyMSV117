package client.messages.commands;

import client.MapleClient;
import constants.GameConstants;
import constants.ServerConstants.PlayerGMRank;
import java.util.HashMap;
import server.MaplePortal;
import server.maps.MapleMap;

/**
 *
 * @author Emilyx3
 */
public class DonatorCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
         return PlayerGMRank.DONATOR;
    }
              public static class GoTo extends CommandExecute { 
                  

        private static final HashMap<String, Integer> gotomaps = new HashMap<String, Integer>();

        static {
            
            gotomaps.put("horntail", 240040700);
            gotomaps.put("lumiere", 150000000);
            gotomaps.put("zakum", 211042300);
            gotomaps.put("southperry", 2000000);
            gotomaps.put("amherst", 1010000);
            gotomaps.put("henesys", 100000000);
            gotomaps.put("ellinia", 101000000);
            gotomaps.put("perion", 102000000);
            gotomaps.put("kerning", 103000000);
            gotomaps.put("harbor", 104000000);
            gotomaps.put("sleepywood", 105000000);
            gotomaps.put("florina", 120000300);
            gotomaps.put("orbis", 200000000);
            gotomaps.put("happyville", 209000000);
            gotomaps.put("elnath", 211000000);
            gotomaps.put("ludi", 220000000);
            gotomaps.put("aqua", 230000000);
            gotomaps.put("leafre", 240000000);
            gotomaps.put("mulung", 250000000);
            gotomaps.put("herb", 251000000);
            gotomaps.put("omega", 221000000);
            gotomaps.put("korea", 222000000);
            gotomaps.put("nlc", 600000000);
            gotomaps.put("sharenian", 990000000);
            gotomaps.put("pianus", 230040420);
            gotomaps.put("showatown", 801000000);
            gotomaps.put("zipangu", 800000000);
            gotomaps.put("ariant", 260000100);
            gotomaps.put("nautilus", 120000000);
            gotomaps.put("boatquay", 541000000);
            gotomaps.put("malaysia", 550000000);
            gotomaps.put("erev", 130000000);
            gotomaps.put("ellin", 300000000);
            gotomaps.put("kampung", 551000000);
            gotomaps.put("singapore", 540000000);
            gotomaps.put("amoria", 680000000);
            gotomaps.put("timetemple", 270000000);
            gotomaps.put("fm", 910000000);
            gotomaps.put("freemarket", 910000000);
            gotomaps.put("oxquiz", 109020001);
            gotomaps.put("ola", 109030101);
            gotomaps.put("fitness", 109040000);
            gotomaps.put("snowball", 109060000);
            gotomaps.put("golden", 950100000);
            gotomaps.put("pforest", 610010000);
            gotomaps.put("cwk", 610030000);
            gotomaps.put("rien", 140000000);
            gotomaps.put("edelstein", 310000000);
            gotomaps.put("ardent", 910001000);
            gotomaps.put("craft", 910001000);
            gotomaps.put("pvp", 960000000);
            gotomaps.put("future", 271000000);
            gotomaps.put("lhc", 211060100);
            gotomaps.put("lhc5", 211060900);
            gotomaps.put("redleaf", 744000000);
            gotomaps.put("golden", 809060000);
            gotomaps.put("elluel", 101050000);
            gotomaps.put("naut", 120000000);
            gotomaps.put("dojo", 925020000);
            gotomaps.put("gani", 252020700);
            gotomaps.put("twilight", 273000000);
            gotomaps.put("lothric", 299000001);
            gotomaps.put("lothric3", 299000200);
            gotomaps.put("farron", 299001000);
            gotomaps.put("bpq", 689010000);
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (int i : GameConstants.blockedMaps) {
                if (c.getPlayer().getMapId() == i) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                    return 0;
                }
            }
            if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000/* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: @goto <mapname> must MATCH LETTER CASE");
            } else {
                if (gotomaps.containsKey(splitted[1])) {
                    MapleMap target = c.getChannelServer().getMapFactory().getMap(gotomaps.get(splitted[1]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "Map does not exist");
                        return 0;
                    }
                    MaplePortal targetPortal = target.getPortal(0);
                    c.getPlayer().changeMap(target, targetPortal);
                } else {
                    if (splitted[1].equals("locations")) {
                        c.getPlayer().dropMessage(6, "Use @goto <location>. Must MATCH LETTER CASE. Locations are as follows:");
                        StringBuilder sb = new StringBuilder();
                        for (String s : gotomaps.keySet()) {
                            sb.append(s).append(", ");
                        }
                        c.getPlayer().dropMessage(6, sb.substring(0, sb.length() - 2));
                    } else {
                        c.getPlayer().dropMessage(6, "Invalid command syntax - Use @goto <location>. For a list of locations, use @goto locations. MUST MATCH LETTER CASE.");
                    }
                }
                 
            }
            return 1;
        }
    }
              
                 public static class AutolootOff extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
                c.getPlayer().stopAutoLooter();
                c.getPlayer().dropMessage(6, "Autolooting is now Deactivated.");
            return 1;
        }
    }
                       public static class AutolootOn extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
                c.getPlayer().startAutoLooter();
                c.getPlayer().dropMessage(6, "Autolooting is now Activated.");
            return 1;
        }
    }
        
       
    }


