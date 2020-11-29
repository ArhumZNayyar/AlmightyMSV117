package client.messages.commands;


import client.inventory.Item;
import server.RankingWorker;
import client.MapleCharacter;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleStat;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.messages.commands.CommandExecute.PokemonExecute;
import client.messages.commands.CommandExecute.TradeExecute;
import constants.BattleConstants.PokemonItem;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import scripting.NPCConversationManager;
import scripting.NPCScriptManager;
import server.ItemInformation;
import server.MapleAchievements;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleShopFactory;
import server.PokemonBattle;
import server.RankingWorker.RankingInformation;
import server.life.MapleMonster;


import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SavedLocationType;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.packet.CWvsContext;

/**
 *
 * @author Emilyx3
 */
public class PlayerCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }

    public static class STR extends DistributeStatCommands {

        public STR() {
            stat = MapleStat.STR;
        }
    }

    public static class DEX extends DistributeStatCommands {

        public DEX() {
            stat = MapleStat.DEX;
        }
    }

    public static class INT extends DistributeStatCommands {

        public INT() {
            stat = MapleStat.INT;
        }
    }

    public static class LUK extends DistributeStatCommands {

        public LUK() {
            stat = MapleStat.LUK;
        }
    }

    public abstract static class DistributeStatCommands extends CommandExecute {

        protected MapleStat stat = null;
        private static int statLim = 32000;

        private void setStat(MapleCharacter player, int amount) {
            switch (stat) {
                case STR:
                    player.getStat().setStr((short) amount, player);
                    player.updateSingleStat(MapleStat.STR, player.getStat().getStr());
                    break;
                case DEX:
                    player.getStat().setDex((short) amount, player);
                    player.updateSingleStat(MapleStat.DEX, player.getStat().getDex());
                    break;
                case INT:
                    player.getStat().setInt((short) amount, player);
                    player.updateSingleStat(MapleStat.INT, player.getStat().getInt());
                    break;
                case LUK:
                    player.getStat().setLuk((short) amount, player);
                    player.updateSingleStat(MapleStat.LUK, player.getStat().getLuk());
                    break;
            }
        }

        private int getStat(MapleCharacter player) {
            switch (stat) {
                case STR:
                    return player.getStat().getStr();
                case DEX:
                    return player.getStat().getDex();
                case INT:
                    return player.getStat().getInt();
                case LUK:
                    return player.getStat().getLuk();
                default:
                    throw new RuntimeException(); //Will never happen.
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Invalid number entered.");
                return 0;
            }
            int change = 0;
            try {
                change = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(5, "Invalid number entered.");
                return 0;
            }
            if (change <= 0) {
                c.getPlayer().dropMessage(5, "You must enter a number greater than 0.");
                return 0;
            }
            if (c.getPlayer().getRemainingAp() < change) {
                c.getPlayer().dropMessage(5, "You don't have enough AP for that.");
                return 0;
            }
            if (getStat(c.getPlayer()) + change > statLim) {
                c.getPlayer().dropMessage(5, "The stat limit is " + statLim + ".");
                return 0;
            }
            setStat(c.getPlayer(), getStat(c.getPlayer()) + change);
            c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - change));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            c.getPlayer().dropMessage(5, StringUtil.makeEnumHumanReadable(stat.name()) + " has been raised by " + change + ".");
            return 1;
        }
    }

    public static class Mob extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleMonster mob = null;
            for (final MapleMapObject monstermo : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 100000, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.isAlive()) {
                    c.getPlayer().dropMessage(6, "Monster " + mob.toString());
                    break; //only one
                }
            }
            if (mob == null) {
                c.getPlayer().dropMessage(6, "No monster was found.");
            }
            return 1;
        }
    }

    public static class Duel extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length <= 1) {
                c.getPlayer().dropMessage(6, "@duel [playername OR accept/decline OR block/unblock]");
                return 0;
            }
            if (c.getPlayer().getLevel() < 50) {
                c.getPlayer().dropMessage(6, "You do not meet the level requirment of level 50!");
                return 0;
            }
            if (splitted[1].equalsIgnoreCase("accept")) {
                if (c.getPlayer().getChallenge() > 0) {
                    final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(c.getPlayer().getChallenge());
                    if (chr != null) {
                        if ((c.getPlayer().isInTownMap() || chr.isInTownMap() && chr.getChallenge() == c.getPlayer().getId() && c.getPlayer().getBattle() == null)) {
                            c.getPlayer().changeMap(925020014);
                            chr.dropMessage(6, c.getPlayer().getName() + " has accepted!");
                            chr.changeMap(925020014);
                        } else {
                            c.getPlayer().dropMessage(6, "You may only use it in towns, or the other character level is too low (50+), or something failed.");
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "They do not exist in the map.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "You don't have a challenge.");
                }
            } else if (splitted[1].equalsIgnoreCase("decline")) {
                if (c.getPlayer().getChallenge() > 0) {
                    c.getPlayer().cancelChallenge();
                } else {
                    c.getPlayer().dropMessage(6, "You don't have a challenge.");
                }
            } else if (splitted[1].equalsIgnoreCase("block")) {
                if (c.getPlayer().getChallenge() == 0) {
                    c.getPlayer().setChallenge(-1);
                    c.getPlayer().dropMessage(6, "You have blocked challenges.");
                } else {
                    c.getPlayer().dropMessage(6, "You have a challenge or they are already blocked.");
                }
            } else if (splitted[1].equalsIgnoreCase("unblock")) {
                if (c.getPlayer().getChallenge() < 0) {
                    c.getPlayer().setChallenge(0);
                    c.getPlayer().dropMessage(6, "You have unblocked challenges.");
                } else {
                    c.getPlayer().dropMessage(6, "You didn't block challenges.");
                }
            } else {
                if (c.getPlayer().getChallenge() == 0) {
                    final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chr != null && chr.getMap() == c.getPlayer().getMap() && chr.getId() != c.getPlayer().getId()) {
                        if ((c.getPlayer().isInTownMap() || chr.isInTownMap() || chr.getChallenge() == 0 && c.getPlayer().getBattle() == null)) {
                            chr.setChallenge(c.getPlayer().getId());
                            chr.dropMessage(6, c.getPlayer().getName() + " has challenged you! Type @duel [accept/decline] to answer!");
                            c.getPlayer().setChallenge(chr.getId());
                            c.getPlayer().dropMessage(6, "Successfully sent the request.");
                        } else {
                            c.getPlayer().dropMessage(6, "You may only use it in towns, or the other character is not above level 50, or they have a challenge.");
                        }
                    } else {
                        c.getPlayer().dropMessage(6, splitted[1] + " does not exist in the map.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "You have a challenge or you have blocked them.");
                }
            }
            return 1;
        }
    }
    
        public static class Challenge extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length <= 1) {
                c.getPlayer().dropMessage(6, "@challenge [playername OR accept/decline OR block/unblock]");
                return 0;
            }
            if (c.getPlayer().getBattler(0) == null) {
                c.getPlayer().dropMessage(6, "You have no monsters!");
                return 0;
            }
            if (splitted[1].equalsIgnoreCase("accept")) {
                if (c.getPlayer().getChallenge() > 0) {
                    final MapleCharacter chr = c.getPlayer().getMap().getCharacterById(c.getPlayer().getChallenge());
                    if (chr != null) {
                        if ((c.getPlayer().isInTownMap() || c.getPlayer().isGM() || chr.isInTownMap() || chr.isGM()) && chr.getBattler(0) != null && chr.getChallenge() == c.getPlayer().getId() && chr.getBattle() == null && c.getPlayer().getBattle() == null) {
                            if (c.getPlayer().getPosition().y != chr.getPosition().y) {
                                c.getPlayer().dropMessage(6, "Please be near them.");
                                return 0;
                            } else if (c.getPlayer().getPosition().distance(chr.getPosition()) > 600.0 || c.getPlayer().getPosition().distance(chr.getPosition()) < 400.0) {
                                c.getPlayer().dropMessage(6, "Please be at a moderate distance from them.");
                                return 0;
                            }
                            chr.setChallenge(0);
                            chr.dropMessage(6, c.getPlayer().getName() + " has accepted!");
                            c.getPlayer().setChallenge(0);
                            final PokemonBattle battle = new PokemonBattle(chr, c.getPlayer());
                            chr.setBattle(battle);
                            c.getPlayer().setBattle(battle);
                            battle.initiate();
                        } else {
                            c.getPlayer().dropMessage(6, "You may only use it in towns, or the other character has no monsters, or something failed.");
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "They do not exist in the map.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "You don't have a challenge.");
                }
            } else if (splitted[1].equalsIgnoreCase("decline")) {
                if (c.getPlayer().getChallenge() > 0) {
                    c.getPlayer().cancelChallenge();
                } else {
                    c.getPlayer().dropMessage(6, "You don't have a challenge.");
                }
            } else if (splitted[1].equalsIgnoreCase("block")) {
                if (c.getPlayer().getChallenge() == 0) {
                    c.getPlayer().setChallenge(-1);
                    c.getPlayer().dropMessage(6, "You have blocked challenges.");
                } else {
                    c.getPlayer().dropMessage(6, "You have a challenge or they are already blocked.");
                }
            } else if (splitted[1].equalsIgnoreCase("unblock")) {
                if (c.getPlayer().getChallenge() < 0) {
                    c.getPlayer().setChallenge(0);
                    c.getPlayer().dropMessage(6, "You have unblocked challenges.");
                } else {
                    c.getPlayer().dropMessage(6, "You didn't block challenges.");
                }
            } else {
                if (c.getPlayer().getChallenge() == 0) {
                    final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chr != null && chr.getMap() == c.getPlayer().getMap() && chr.getId() != c.getPlayer().getId()) {
                        if ((c.getPlayer().isInTownMap() || c.getPlayer().isGM() || chr.isInTownMap() || chr.isGM()) && chr.getBattler(0) != null && chr.getChallenge() == 0 && chr.getBattle() == null && c.getPlayer().getBattle() == null) {
                            chr.setChallenge(c.getPlayer().getId());
                            chr.dropMessage(6, c.getPlayer().getName() + " has challenged you! Type @challenge [accept/decline] to answer!");
                            c.getPlayer().setChallenge(chr.getId());
                            c.getPlayer().dropMessage(6, "Successfully sent the request.");
                        } else {
                            c.getPlayer().dropMessage(6, "You may only use it in towns, or the other character has no monsters, or they have a challenge.");
                        }
                    } else {
                        c.getPlayer().dropMessage(6, splitted[1] + " does not exist in the map.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "You have a challenge or you have blocked them.");
                }
            }
            return 1;
        }
    } 
  
    
    public static class search extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length == 1) {
                c.getPlayer().dropMessage(6, splitted[0] + ": <ITEM>");
            } else if (splitted.length == 2) {
                c.getPlayer().dropMessage(6, "Provide something to search.");
            } else {
                String type = splitted[1];
                String search = StringUtil.joinStringFrom(splitted, 2);
                MapleData data = null;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                c.getPlayer().dropMessage(6, "<<Type: " + type + " | Search: " + search + ">>");
                 if (type.equalsIgnoreCase("ITEM")) {
                    List<String> retItems = new ArrayList<String>();
                    for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                        if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.itemId + " - " + itemPair.name);
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "No Items Found.");
                    }
                
                } else {
                    c.getPlayer().dropMessage(6, "Sorry, that search call is unavailable.");
                }
            }
            return 0;
        }
        
    }
    
    public abstract static class OpenNPCCommand extends CommandExecute {

        protected int npc = -1;
        private static int[] npcs = { //Ish yur job to make sure these are in order and correct ;(
            9270035, 
            9900000, 
            9000018, 
            9000000,
            9000030,
            9010000,
            9000085,
            9000018,
            9201094,
            1012121,
            9133080,
            9090000,
            2300000
            };

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (npc != 7 && npc != 8 && npc != 9 && npc != 10 && npc != 11 && npc != 6 && npc != 5 && npc != 4 && npc != 3 && npc != 1 && c.getPlayer().getMapId() != 910000000) { //drpcash can use anywhere
                if (c.getPlayer().getLevel() < 10 && c.getPlayer().getJob() != 200) {
                    c.getPlayer().dropMessage(5, "You must be over level 10 to use this command.");
                    return 0;
                }
                if (c.getPlayer().isInBlockedMap()) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                    return 0;
                }
            } else if (npc == 1) {
                if (c.getPlayer().getLevel() < 70) {
                    c.getPlayer().dropMessage(5, "You must be over level 70 to use this command.");
                    return 0;
                }
            }
            if (c.getPlayer().hasBlockedInventory()) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            NPCScriptManager.getInstance().start(c, npcs[npc]);
            return 1;
        }
    }

    public static class npc extends OpenNPCCommand {

        public npc() {
            npc = 0;
        }
    }
    public static class kin extends OpenNPCCommand {

        public kin() {
            npc = 1;
            
        }
    }

    public static class DCash extends OpenNPCCommand {

        public DCash() {
            npc = 9;
        }
    }

    public static class Event extends OpenNPCCommand {

        public Event() {
            npc = 3;
        }
    }

    public static class CheckDrop extends OpenNPCCommand {

        public CheckDrop() {
            npc = 5;
        }
    }

    public static class Pokedex extends OpenNPCCommand {

        public Pokedex() {
            npc = 6;
        }
    }

    public static class Pokemon extends OpenNPCCommand {

        public Pokemon() {
            npc = 2;
        }
    }
    
    public static class JQ extends OpenNPCCommand {

        public JQ() {
            npc = 7;
        }
    }
    
    public static class hunts extends OpenNPCCommand {
        public hunts() {
        npc = 10; 
        }
    }
    
    public static class shop extends OpenNPCCommand {
        public shop() {
        npc = 11;
        }
    }
    
    public static class find extends OpenNPCCommand {
        public find() {
            npc = 12;
        }
    }

    public static class ClearSlot extends CommandExecute {
    
    private static MapleInventoryType[] invs = {
    MapleInventoryType.EQUIP,
    MapleInventoryType.USE,
    MapleInventoryType.SETUP,
    MapleInventoryType.ETC,
    MapleInventoryType.CASH,};
    
    @Override
    public int execute(MapleClient c, String[] splitted) {
    MapleCharacter player = c.getPlayer();
    if (splitted.length < 2 || player.hasBlockedInventory()) {
    c.getPlayer().dropMessage(5, "@clearslot <eq/use/setup/etc/cash/all>");
    return 0;
    } else {
    MapleInventoryType type;
    if (splitted[1].equalsIgnoreCase("eq")) {
    type = MapleInventoryType.EQUIP;
    } else if (splitted[1].equalsIgnoreCase("use")) {
    type = MapleInventoryType.USE;
    } else if (splitted[1].equalsIgnoreCase("setup")) {
    type = MapleInventoryType.SETUP;
    } else if (splitted[1].equalsIgnoreCase("etc")) {
    type = MapleInventoryType.ETC;
    } else if (splitted[1].equalsIgnoreCase("cash")) {
    type = MapleInventoryType.CASH;
    } else if (splitted[1].equalsIgnoreCase("all")) {
    type = null;
    } else {
    c.getPlayer().dropMessage(5, "Invalid. @clearslot <eq/use/setup/etc/cash/all>");
    return 0;
    }
    if (type == null) { //All, a bit hacky, but it's okay
    for (MapleInventoryType t : invs) {
    type = t;
    MapleInventory inv = c.getPlayer().getInventory(type);
    byte start = -1;
    for (byte i = 0; i < inv.getSlotLimit(); i++) {
    if (inv.getItem(i) != null) {
    start = i;
    break;
    }
    }
    if (start == -1) {
    c.getPlayer().dropMessage(5, "There are no items in that inventory.");
    return 0;
    }
    int end = 0;
    for (byte i = start; i < inv.getSlotLimit(); i++) {
    if (inv.getItem(i) != null) {
    MapleInventoryManipulator.removeFromSlot(c, type, i, inv.getItem(i).getQuantity(), true);
    } else {
    end = i;
    break;//Break at first empty space.
    }
    }
    c.getPlayer().dropMessage(5, "Cleared slots " + start + " to " + end + ".");
    }
    } else {
    MapleInventory inv = c.getPlayer().getInventory(type);
    byte start = -1;
    for (byte i = 0; i < inv.getSlotLimit(); i++) {
    if (inv.getItem(i) != null) {
    start = i;
    break;
    }
    }
    if (start == -1) {
    c.getPlayer().dropMessage(5, "There are no items in that inventory.");
    return 0;
    }
    byte end = 0;
    for (byte i = start; i < inv.getSlotLimit(); i++) {
    if (inv.getItem(i) != null) {
    MapleInventoryManipulator.removeFromSlot(c, type, i, inv.getItem(i).getQuantity(), true);
    } else {
    end = i;
    break;//Break at first empty space.
    }
    }
    c.getPlayer().dropMessage(5, "Cleared slots " + start + " to " + end + ".");
    }
    return 1;
    }
    }
    }
    public static class FM extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            for (int i : GameConstants.blockedMaps) {
                if (c.getPlayer().getMapId() == i) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                    return 0;
                }
            }
            if (c.getPlayer().getLevel() < 10 && c.getPlayer().getJob() != 200) {
                c.getPlayer().dropMessage(5, "You must be over level 10 to use this command.");
                return 0;
            }
            if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000/* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET, c.getPlayer().getMap().getReturnMap().getId());
            MapleMap map = c.getChannelServer().getMapFactory().getMap(910000000);
            c.getPlayer().changeMap(map, map.getPortal(0));
            return 1;
        }
    }

    public static class EA extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(CWvsContext.enableActions());
            return 1;
        }
    }
    
    public static class dispose extends CommandExecute
  {
    public int execute(MapleClient c, String[] splitted)
    {
      c.removeClickedNPC();
      NPCScriptManager.getInstance().dispose(c);
      c.getSession().write(CWvsContext.enableActions());
      c.getPlayer().dropMessage(6, "You have been disposed.");
      return 1;
    }
    }

    public static class TSmega extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSmega();
            return 1;
        }
    }

    public static class Ranking extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) { //job start end
                c.getPlayer().dropMessage(5, "Use @ranking [job] [start number] [end number] where start and end are ranks of the players");
                final StringBuilder builder = new StringBuilder("JOBS: ");
                for (String b : RankingWorker.getJobCommands().keySet()) {
                    builder.append(b);
                    builder.append(" ");
                }
                c.getPlayer().dropMessage(5, builder.toString());
            } else {
                int start = 1, end = 20;
                try {
                    start = Integer.parseInt(splitted[2]);
                    end = Integer.parseInt(splitted[3]);
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(5, "You didn't specify start and end number correctly, the default values of 1 and 20 will be used.");
                }
                if (end < start || end - start > 20) {
                    c.getPlayer().dropMessage(5, "End number must be greater, and end number must be within a range of 20 from the start number.");
                } else {
                    final Integer job = RankingWorker.getJobCommand(splitted[1]);
                    if (job == null) {
                        c.getPlayer().dropMessage(5, "Please use @ranking to check the job names.");
                    } else {
                        final List<RankingInformation> ranks = RankingWorker.getRankingInfo(job.intValue());
                        if (ranks == null || ranks.size() <= 0) {
                            c.getPlayer().dropMessage(5, "Please try again later.");
                        } else {
                            int num = 0;
                            for (RankingInformation rank : ranks) {
                                if (rank.rank >= start && rank.rank <= end) {
                                    if (num == 0) {
                                        c.getPlayer().dropMessage(6, "Rankings for " + splitted[1] + " - from " + start + " to " + end);
                                        c.getPlayer().dropMessage(6, "--------------------------------------");
                                    }
                                    c.getPlayer().dropMessage(6, rank.toString());
                                    num++;
                                }
                            }
                            if (num == 0) {
                                c.getPlayer().dropMessage(5, "No ranking was returned.");
                            }
                        }
                    }
                }
            }
            return 1;
        }
    }

    public static class Check extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getParagons() + " Paragon levels.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getReborns() + " Reborns.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getCSPoints(1) + " Cash.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getPoints() + " donation points.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getVPoints() + " voting points.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getIntNoRecord(GameConstants.BOSS_PQ) + " Boss Party Quest points.");
            c.getPlayer().dropMessage(6, "The time is currently " + FileoutputUtil.CurrentReadable_TimeGMT() + " GMT.");
            return 1;
        }
    }
      public static class Peter extends CommandExecute {
          @Override
          public int execute (MapleClient c, String [] splitted) {
              MapleCharacter player = c.getPlayer();
            if (splitted.length == 1) {
                c.getPlayer().dropMessage(6, "Syntax: @Peter <player name>");
                return 0;
            }
              MapleCharacter victim = null;
            for (int i = 1; i < splitted.length; i++) {
                try {
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "Player " + splitted[i] + " not found.");
                }        
                                        victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                                        c.getPlayer().dropMessage(6, "Level: " +victim.getLevel());
                                        c.getPlayer().dropMessage(6, "Reborns: " +victim.getReborns()); 
                                        c.getPlayer().dropMessage(6, "Paragons: " +victim.getParagons()); 
                                        c.getPlayer().dropMessage(6, "Str: " +victim.getStat().getStr());
                                        c.getPlayer().dropMessage(6, "Int: " +victim.getStat().getInt());
                                        c.getPlayer().dropMessage(6, "Luk: " +victim.getStat().getLuk());
                                        c.getPlayer().dropMessage(6, "Dex: " +victim.getStat().getDex());
                                        c.getPlayer().dropMessage(6, "Fame: " + victim.getFame());
              return 0;
              
          }
              return 0;
      }
}

    public static class Paragons extends CommandExecute {
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getParagons() + " Paragon levels.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getExp() + " Experience points.");
            c.getPlayer().dropMessage(6, "You currently need " + c.getPlayer().getNeededParagon() + " Experience points to advance to the next Paragon level.");
            return 1;
        }
    }

    public static class Help extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "@Job <To Choose a Job at Level 10 for 1st Job ONLY>");
            c.getPlayer().dropMessage(5, "@Advance <To Advance to 2nd Job, 3rd Job, or 4th Job>");
            c.getPlayer().dropMessage(5, "@Paragons <Checks your paragon level + EXP needed to advance to the next Paragon.>");
            c.getPlayer().dropMessage(5, "@str, @dex, @int, @luk <amount to add>");
            c.getPlayer().dropMessage(5, "@EXP < Shows your current exp and remaining exp needed to level >");
            c.getPlayer().dropMessage(5, "@Peter -IGN- <Check Information on that character>");
            c.getPlayer().dropMessage(5, "@Engage -IGN- <to become engaged to that person>");
            c.getPlayer().dropMessage(5, "@Divorce -IGN- < to divorce your marriage>");
            c.getPlayer().dropMessage(5, "@mob < Information on the closest monster >");
            c.getPlayer().dropMessage(5, "@Funcommands <Shows a list of fun commands>");
            c.getPlayer().dropMessage(5, "@lotto  <Particpiate in the lotto>");
            c.getPlayer().dropMessage(5, "@check < Displays various information >");
            c.getPlayer().dropMessage(5, "@fm < Warp to FM >");
            c.getPlayer().dropMessage(5, "@search item < Search up an item. >");
            /*c.getPlayer().dropMessage(5, "@changesecondpass - Change second password, @changesecondpass <current Password> <new password> <Confirm new password> ");*/
            c.getPlayer().dropMessage(5, "@npc < Universal Town Warp / Event NPC >");
            c.getPlayer().dropMessage(5, "@Reborn < Kinda obvious aintitjay?>");
            c.getPlayer().dropMessage(5, "@dcash < Universal Cash Item Dropper >");
            if (!GameConstants.GMS) {
            c.getPlayer().dropMessage(5, "@pokedex < Universal Pocket Monsters Information >");
            c.getPlayer().dropMessage(5, "@pokemon < Universal Pocket Monsters Information >");        
            }
            c.getPlayer().dropMessage(5, "@achievements < Shows all your achievements >");
            c.getPlayer().dropMessage(5, "@duel < playername, or accept/decline or block/unblock >");
            c.getPlayer().dropMessage(5, "@Respawn < Respawns all mob in map >");
            c.getPlayer().dropMessage(5, "@ea < If you are unable to attack or talk to NPC >");
            c.getPlayer().dropMessage(5, "@clearslot OR @clearinv < Cleanup that trash in your inventory >");
            c.getPlayer().dropMessage(5, "@ranking < Use @ranking for more details >");
            c.getPlayer().dropMessage(5, "@checkdrop < Use @checkdrop for drops on mob >");
            c.getPlayer().dropMessage(5, "@ResetEXP < Reset your EXP >");
            c.getPlayer().dropMessage(5, "@Killcount < Shows how many mobs you've killed. >");
            c.getPlayer().dropMessage(5, "@CancelBuffs < Cancel all your buffs use if stuck in siege mode or any transformation >");
            return 1;
        }
    }

    public static class TradeHelp extends TradeExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(-2, "[System] : <@offerequip, @offeruse, @offersetup, @offeretc, @offercash> <quantity> <name of the item>");
            return 1;
        }
    }
public static class Job extends CommandExecute { 

        @Override 
        public int execute(MapleClient c, String[] splitted) { 
            if (c.getPlayer().isInBlockedMap()) { 
                c.getPlayer().dropMessage(5, "You may not use this command here."); 
                return 0; 
            } 
            if (c.getPlayer().getLevel() < 10) { 
                c.getPlayer().dropMessage(5, "You must be over level 10 to use this command."); 
                return 0; 
            } 
            NPCScriptManager.getInstance().start(c,2003); 
            return 1; 
        } 
    }  
public static class Advance extends CommandExecute { 

       @Override 
        public int execute(MapleClient c, String[] splitted) { 
            if (c.getPlayer().isInBlockedMap()) { 
                c.getPlayer().dropMessage(5, "You may not use this command here."); 
                return 0; 
            } 
            if (c.getPlayer().getLevel() < 29) { 
                c.getPlayer().dropMessage(5, "You must be over level 30 to use this command."); 
                return 0; 
            } 
            NPCScriptManager.getInstance().start(c,9200000); 
            return 1; 
        } 
}
    public abstract static class OfferCommand extends TradeExecute {

        protected int invType = -1;

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-2, "[Error] : <quantity> <name of item>");
            } else if (c.getPlayer().getLevel() < 70) {
                c.getPlayer().dropMessage(-2, "[Error] : Only level 70+ may use this command");
            } else {
                int quantity = 1;
                try {
                    quantity = Integer.parseInt(splitted[1]);
                } catch (Exception e) { //swallow and just use 1
                }
                String search = StringUtil.joinStringFrom(splitted, 2).toLowerCase();
                Item found = null;
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                for (Item inv : c.getPlayer().getInventory(MapleInventoryType.getByType((byte) invType))) {
                    if (ii.getName(inv.getItemId()) != null && ii.getName(inv.getItemId()).toLowerCase().contains(search)) {
                        found = inv;
                        break;
                    }
                }
                if (found == null) {
                    c.getPlayer().dropMessage(-2, "[Error] : No such item was found (" + search + ")");
                    return 0;
                }
                if (GameConstants.isPet(found.getItemId()) || GameConstants.isRechargable(found.getItemId())) {
                    c.getPlayer().dropMessage(-2, "[Error] : You may not trade this item using this command");
                    return 0;
                }
                if (quantity > found.getQuantity() || quantity <= 0 || quantity > ii.getSlotMax(found.getItemId())) {
                    c.getPlayer().dropMessage(-2, "[Error] : Invalid quantity");
                    return 0;
                }
                if (!c.getPlayer().getTrade().setItems(c, found, (byte) -1, quantity)) {
                    c.getPlayer().dropMessage(-2, "[Error] : This item could not be placed");
                    return 0;
                } else {
                    c.getPlayer().getTrade().chatAuto("[System] : " + c.getPlayer().getName() + " offered " + ii.getName(found.getItemId()) + " x " + quantity);
                }
            }
            return 1;
        }
    }

    public static class OfferEquip extends OfferCommand {

        public OfferEquip() {
            invType = 1;
        }
    }

    public static class OfferUse extends OfferCommand {

        public OfferUse() {
            invType = 2;
        }
    }

    public static class OfferSetup extends OfferCommand {

        public OfferSetup() {
            invType = 3;
        }
    }

    public static class OfferEtc extends OfferCommand {

        public OfferEtc() {
            invType = 4;
        }
    }

    public static class OfferCash extends OfferCommand {

        public OfferCash() {
            invType = 5;
        }
    }

    public static class BattleHelp extends PokemonExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(-3, "(...I can use @use <attack name> to take down the enemy...)");
            c.getPlayer().dropMessage(-3, "(...I can use @info to check out the stats of my battle...)");
            c.getPlayer().dropMessage(-3, "(...I can use @ball <basic, great, ultra> to use an ball, but only if I have it...)");
            c.getPlayer().dropMessage(-3, "(...I can use @run if I don't want to fight anymore...)");
            c.getPlayer().dropMessage(-4, "(...This is a tough choice! What do I do?...)"); //last msg they see
            return 1;
        }
    }

    public static class Ball extends PokemonExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getBattle().getInstanceId() < 0 || c.getPlayer().getBattle().isTrainerBattle()) {
                c.getPlayer().dropMessage(-3, "(...I can't use it in a trainer battle...)");
                return 0;
            }
            if (splitted.length <= 1) {
                c.getPlayer().dropMessage(-3, "(...I can use @ball <basic, great, or ultra> if I have the ball...)");
                return 0;
            }
            PokemonItem item = null;
            if (splitted[1].equalsIgnoreCase("basic")) {
                item = PokemonItem.Basic_Ball;
            } else if (splitted[1].equalsIgnoreCase("great")) {
                item = PokemonItem.Great_Ball;
            } else if (splitted[1].equalsIgnoreCase("ultra")) {
                item = PokemonItem.Ultra_Ball;
            }
            if (item != null) {
                if (c.getPlayer().haveItem(item.id, 1)) {
                    if (c.getPlayer().getBattle().useBall(c.getPlayer(), item)) {
                        MapleInventoryManipulator.removeById(c, GameConstants.getInventoryType(item.id), item.id, 1, false, false);
                    } else {
                        c.getPlayer().dropMessage(-3, "(...The monster is too strong, maybe I don't need it...)");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(-3, "(...I don't have a " + splitted[1] + " ball...)");
                    return 0;
                }
            } else {
                c.getPlayer().dropMessage(-3, "(...I can use @ball <basic, great, or ultra> if I have the ball...)");
                return 0;
            }
            return 1;
        }
    }

    public static class Info extends PokemonExecute {

        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000021); //no checks are needed
            return 1;
        }
    }

    public static class Run extends PokemonExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getBattle().forfeit(c.getPlayer(), false);
            return 1;
        }
    }

    public static class Use extends PokemonExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length <= 1) {
                c.getPlayer().dropMessage(-3, "(...I need an attack name...)");
                return 0;
            }
            if (!c.getPlayer().getBattle().attack(c.getPlayer(), StringUtil.joinStringFrom(splitted, 1))) {
                c.getPlayer().dropMessage(-3, "(...I've already selected an action...)");
            }
            return 1;
        }
    }
      public static class GM extends CommandExecute {
        
        @Override
        public int execute(MapleClient c,String[] splitted) {
             int minimumTimeToWait = 6;
            try{
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("Select * from characters where name = ?");
                ps.setString(1, c.getPlayer().getName());
                ResultSet rs = ps.executeQuery();
                if(!rs.next()){
                    rs.close();
                    ps.close();
                } else {
                    long ts2 = rs.getLong("gmCallTime");
                    long ts1 = System.currentTimeMillis();
                    int timeDif = (int)(ts1-ts2)/60000;
                    if(timeDif >= minimumTimeToWait || timeDif < 0){
                        c.getChannelServer().broadcastGMPacket(CWvsContext.serverNotice(1, c.getPlayer().getName()+": " + StringUtil.joinStringFrom(splitted, 1)));
                        ps = DatabaseConnection.getConnection().prepareStatement("update characters set gmCallTime = ? where name = ?");
                        ps.setLong(1, ts1);
                        ps.setString(2, c.getPlayer().getName());           
                        ps.executeUpdate();
                    } else {
                        c.getPlayer().dropMessage(1, "You cannot call a GM for " + (minimumTimeToWait-timeDif) + " more minutes.");
                    }
                    rs.close();
                    ps.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }
      }
     
      

      public static class reborn extends CommandExecute { 
    public int execute(MapleClient c, String[] splitted) { 
        if (c.getPlayer().getLevel() < 200 && c.getPlayer().getParagons() >= 8 ) { 
            c.getPlayer().dropMessage(5, "You are not level 200 and/or you have more than 8 paragons."); 
            return 0; 
        } else if (c.getPlayer().getLevel() == 200 && c.getPlayer().getParagons() < 8) { 
            c.getPlayer().doReborn(); 
            c.getPlayer().dropMessage(6, "You have been reborn!"); 
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Congratulations]" + c.getPlayer().getName() + " has reached " + c.getPlayer().reborns + " reborn(s)! Congratulate," + c.getPlayer().getName() + " on such an amazing achievement!"));    
                c.getPlayer().dropMessage(6, " You may now do @Evolve to evolve one item!");
        } 
        return 1; 
    } 
}  
        public static class rebirth extends CommandExecute { 
    public int execute(MapleClient c, String[] splitted) { 
        if (c.getPlayer().getLevel() < 200 && c.getPlayer().getParagons() >= 8 ) { 
            c.getPlayer().dropMessage(5, "You are not level 200 and/or you have more than 8 paragons."); 
            return 0; 
        } else if (c.getPlayer().getLevel() == 200 && c.getPlayer().getParagons() < 8) { 
            c.getPlayer().doReborn(); 
            c.getPlayer().dropMessage(6, "You have been reborn!"); 
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Congratulations]" + c.getPlayer().getName() + " has reached " + c.getPlayer().reborns + " reborn(s)! Congratulate," + c.getPlayer().getName() + " on such an amazing achievement!"));    
                c.getPlayer().dropMessage(6, " You may now do @Evolve to evolve one item!");
        } 
        return 1; 
    } 
} 
      public static class ClearInv extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Pair<Short, Short>, MapleInventoryType> eqs = new HashMap<Pair<Short, Short>, MapleInventoryType>();
            if (splitted[1].equals("all")) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (Item item : c.getPlayer().getInventory(type)) {
                        eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), type);
                    }
                }
            } else if (splitted[1].equals("eqp")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIPPED);
                }
            } else if (splitted[1].equals("eq")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIP);
                }
            } else if (splitted[1].equals("u")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.USE);
                }
            } else if (splitted[1].equals("s")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.SETUP);
                }
            } else if (splitted[1].equals("e")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.ETC);
                }
            } else if (splitted[1].equals("c")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    eqs.put(new Pair<Short, Short>(item.getPosition(), item.getQuantity()), MapleInventoryType.CASH);
                }
            } else {
                c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
            }
            for (Map.Entry<Pair<Short, Short>, MapleInventoryType> eq : eqs.entrySet()) {
                MapleInventoryManipulator.removeFromSlot(c, eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
            }
            return 1;
        }
    

      }
       public static class Gayboy extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Characters connected to channel " + c.getChannel() + ":");
            c.getPlayer().dropMessage(6, c.getChannelServer().getPlayerStorage().getOnlinePlayers(true));
            return 1;
        }
    }
      public static class Gaygirl extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Integer, Integer> connected = World.getConnected();
            StringBuilder conStr = new StringBuilder("Connected Clients: ");
            boolean first = true;
            for (int i : connected.keySet()) {
                if (!first) {
                    conStr.append(", ");
                } else {
                    first = false;
                }
                if (i == 0) {
                    conStr.append("Total: ");
                    conStr.append(connected.get(i));
                } else {
                    conStr.append("Channel");
                    conStr.append(i);
                    conStr.append(": ");
                    conStr.append(connected.get(i));
                }
            }
            c.getPlayer().dropMessage(6, conStr.toString());
            return 1;
        }
    }
      public static class CC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().changeChannel(Integer.parseInt(splitted[1]));
            return 1;
        }
    }
      
       public static class Suicide extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getPlayer();
    victim.setHp(0);
    victim.setMp(0);
    victim.updateSingleStat(MapleStat.HP, 0);
    victim.updateSingleStat(MapleStat.MP, 0);
   
            return 0;
            
        }
      
}
      public static class RebirthC extends CommandExecute {
    public int execute(MapleClient c, String[] splitted) { 
        if (c.getPlayer().getJob() >= 1000 && c.getPlayer().getJob() <= 1512 && c.getPlayer().getLevel() < 120)
         { 
            c.getPlayer().dropMessage(5, "You are not level 120."); 
            return 0; 
        } else { 
            if (c.getPlayer().getJob() >= 1000 && c.getPlayer().getJob() <= 1512 && c.getPlayer().getLevel() < 120)
            c.getPlayer().doReborn(); 
            
            c.getPlayer().dropMessage(5, "You have been reborn!"); 
        } 
        return 1; 
    } 
}
      public static class Slap extends CommandExecute {
          @Override
          public int execute (MapleClient c, String [] splitted) {
              MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: @Slap <player name>");
                return 0;
            }
              MapleCharacter victim = null;
            for (int i = 1; i < splitted.length; i++) {
                try {
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "Player " + splitted[i] + " not found.");
                }        
                                        victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                                        victim.getAllDiseases(); 
                                        c.getPlayer().dropMessage(6, "You slapped " +victim.getName()+"."); 
                                         
                         
              return 0;
              
          }
              return 0;
      }
}
      
       public static class AC extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 1) {
                c.getPlayer().dropMessage(6, "[HG] You're using this command wrong.");
                return 0;
            }
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> players = map.getMapObjectsInRange(c.getPlayer().getPosition(), (double) 1500, Arrays.asList(MapleMapObjectType.PLAYER));
            for (MapleMapObject closeplayers : players) {
                MapleCharacter playernear = (MapleCharacter) closeplayers;
                if (c.getPlayer().getMap().toggleHungerGames == false) {
                    c.getPlayer().dropMessage(6, "Toggle Hunger Games is not Activated.");
                    c.getPlayer().dropMessage(6, "A GM must activate it to use.");
                } else {
                if (playernear.isAlive());
                {
                    if (c.getPlayer().isAlive()) {
                        if (playernear.isAlive()) {
                            if (!playernear.isGM()) {
                                if (playernear != c.getPlayer()) {
                                    playernear.getStat().setHp((short) 0, playernear);
                                    playernear.updateSingleStat(MapleStat.HP, 0);
                                    playernear.dropMessage(5, "Executed by " + c.getPlayer().getName() + ", you have lost the Event.");
                                    playernear.changeMap(c.getPlayer().getMapId(), 0);
                                }
                            }
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "You must be alive to kill people. You have lost.");
                        }
                    }
                }
            }
            return 1;
        }
    }
       public static class HG extends AC {
        
    }
       public static class Rape extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
       
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }

            
            chrs.dropMessage(5, c.getPlayer().getName() + " raped you.");
            c.getPlayer().dropMessage(1, "You have raped " + chrs.getName() + " You horny little shit.");
            return 1;
        }
    }
       
       public static class Engage extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(5, "[Syntax] @engage <name>");
                return 0;
            }
            
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (c.getPlayer().getId() == chrs.getId()) {
                c.getPlayer().dropMessage(5, "[Error] You can't engage yourself.");
            } else if (c.getPlayer().getPendingMarriageId() > 0) {
                c.getPlayer().dropMessage(5, "[Engagement] We're currently waiting on his/her response.");
            }  else if (c.getPlayer().getMarriageId() > 0) {
                c.getPlayer().dropMessage(5, "[Engagement] Congratulations on your engagement.");
            } else if (chrs == null) {
                c.getPlayer().dropMessage(5, "[Error] " + chrs.getName() + " cannot be found Online or in your map or channel.");
            } else if (!c.getPlayer().canHold(4031358)) {
                c.getPlayer().dropMessage(5, "[Error] Please check if you have room in your Equip Slot. (For the Ring)");
            } else if (c.getPlayer().haveItem(2240000) || c.getPlayer().haveItem(2240001) || c.getPlayer().haveItem(2240002) || c.getPlayer().haveItem(2240003)) {
                c.getPlayer().dropMessage(5, "[Engagement] " + c.getPlayer().getName() + " : " + chrs.getName() + ", will you marry me? @accept or @decline");
                chrs.dropMessage(5, "[AlmightyMS] " + c.getPlayer().getName() + " : " + chrs.getName() + ", will you marry me? @accept or @decline");
                c.getPlayer().setPendingMarriageId(chrs.getId());
                c.getPlayer().removeAll(2240000);
                c.getPlayer().removeAll(2240001);
                c.getPlayer().removeAll(2240002);
                c.getPlayer().removeAll(2240003);
            } else {
                c.getPlayer().dropMessage(5, "[Error] You don't have an Engagement Box.");
            }
            return 1;
        }
    }
    
    public static class Accept extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "[Syntax] @accept <name>");
                return 0;
            }
            
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getPendingMarriageId() != c.getPlayer().getId()) {
                c.getPlayer().dropMessage(5, "[Error] This person did not ask to marry you.");
            } else if (c.getPlayer().getMarriageId() > 0) {
                c.getPlayer().dropMessage(5, "[Engaged] Congratulations on your engagement!");
            } else if (chrs == null) {
                c.getPlayer().dropMessage(5, chrs.getName() + " cannot be found Online or in your map or channel.");
            } else if (!c.getPlayer().canHold(4031358)) {
                c.getPlayer().dropMessage(5, "[Error] Please check if you have room in your Equip Slot. (For the Ring)");
            } else {
                c.getPlayer().dropMessage(5, "[Engagement] " + c.getPlayer().getName() + " : Yes, I accept.");
                chrs.dropMessage(5, "[Engagement] " + c.getPlayer().getName() + " : Yes, I accept.");
                c.getPlayer().setMarriageId(chrs.getId());
                chrs.setMarriageId((int) chrs.getPendingMarriageId());
                chrs.setPendingMarriageId(0);
                c.getPlayer().serverNotice("[AlmightyMS] " + chrs.getName() + " and " + c.getPlayer().getName() + " are now Engaged! Congratulations!");
            }
            return 1;
        }
    }
    
    public static class Divorce extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "[Syntax] @divorce <name>");
                return 0;
            }
            
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getMarriageId() != c.getPlayer().getId()) {
                c.getPlayer().dropMessage(5, "[Error] You're not married to this person.");
            } else if (chrs == null) {
                c.getPlayer().dropMessage(5, chrs.getName() + " cannot be found Online or in your map or channel.");
            } else if (!c.getPlayer().haveItem(4031358)) {
                c.getPlayer().dropMessage(5, "[Divorce] Please un-equip your ring.");
            } else if (!chrs.haveItem(4031358)) {
                c.getPlayer().dropMessage(5, "[Divorce] Please tell your spouse to un-equip the ring.");
            } else if (c.getPlayer().getMarriageId() > 0) {
                c.getPlayer().dropMessage(5, "[Divorce] " + c.getPlayer().getName() + " : It's over, we're done.");
                chrs.dropMessage(5, "[Divorce] " + c.getPlayer().getName() + " : It's over, we're done.");
                c.getPlayer().setMarriageId(0);
                chrs.setMarriageId(0);
                c.getPlayer().removeAll(4031358, false);
                chrs.removeAll(4031358, false);
                c.getPlayer().serverNotice("[Divorce] " + chrs.getName() + " and " + c.getPlayer().getName() + " has divorced.");
            }
            return 1;
        }
    }
    
    public static class Decline extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "[Syntax] @decline <name>");
                return 0;
            }
            
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
             if (chrs.getPendingMarriageId() == c.getPlayer().getId()) {
                c.getPlayer().dropMessage(5, "[Error] This person did not ask to marry you.");
            } else if (c.getPlayer().getMarriageId() > 0) {
                c.getPlayer().dropMessage(5, "[Married] Congratulations on your Marriage.");
            } else if (chrs == null) {
                c.getPlayer().dropMessage(5, chrs.getName() + " cannot be found Online or in your map or channel.");
            } else {
                c.getPlayer().dropMessage(5, "[Engagement] " + c.getPlayer().getName() + " : No, I decline.");
                chrs.dropMessage(5, "[Engagement] " + c.getPlayer().getName() + " : No, I decline.");
                chrs.setPendingMarriageId(0);
                c.getPlayer().serverNotice("[Engaged] " + chrs.getName() + " and " + c.getPlayer().getName() + " is now Engaged! Congratulations!");
            }
            return 1;
        }
    }
    
      public static class Attack extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 1) {
                c.getPlayer().dropMessage(6, "[FFA] You're using this command wrong.");
                return 0;
            }
            if (c.getPlayer().getMapId() != 262022133) {
                c.getPlayer().dropMessage(6, "[FFA] You must join the Free for All Arena to use this command.");
                return 0;
            }
            if (c.getPlayer().getCharsOnMap() < 5) {
                c.getPlayer().dropMessage(6, "[FFA] There must be atleast 5 people in the Free for All Arena to kill people.");
                return 0;
            }
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> players = map.getMapObjectsInRange(c.getPlayer().getPosition(), (double) 1500, Arrays.asList(MapleMapObjectType.PLAYER));
            for (MapleMapObject closeplayers : players) {
                MapleCharacter playernear = (MapleCharacter) closeplayers;
                if (c.getPlayer().isAlive()) {
                    if (playernear.isAlive()) {
                        if (playernear != c.getPlayer()) {
                            if (System.currentTimeMillis() - playernear.getInvulerableTime() > 3000) {
                                playernear.getStat().setHp((short) 0, playernear);
                                playernear.updateSingleStat(MapleStat.HP, 0);
                                c.getPlayer().gainFfaKills(1);
                                c.getPlayer().dropMessage(6, "[Kill] You have killed " + playernear.getName() + ", your score is now " + c.getPlayer().getFfaKills() + "/" + c.getPlayer().getFfaDeaths() + ".");
                                playernear.gainFfaDeaths(1);
                                playernear.dropMessage(6, "[Death] Executed by " + c.getPlayer().getName() + ", your score is now " + playernear.getFfaKills() + "/" + playernear.getFfaDeaths() + ".");
                                playernear.dropMessage(6, "[Death] Type @revive to revive yourself, don't press OK unless you want to leave.");
                                c.getPlayer().saveToDB(false, false);
                                playernear.saveToDB(false, false);
                            } else {
                                c.getPlayer().dropMessage(5, "[Spawned] " + playernear.getName() + " has respawned under 3 seconds ago, they are invulnerable.");
                            }
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(6, "[FFA] Type @revive to revive yourself.");
                }
            }
            return 1;
        }
    }
      
      public static class poop extends CommandExecute {

		@Override
		public int execute(MapleClient c, String[] splitted) {
			MapleCharacter target;
			try {
				target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
				c.getPlayer().getMap().spawnItemDrop(target, target, new Item(4000378, (byte) 0, (short) 1, (byte) 0), target.getPosition(), true, true);
                                c.getPlayer().dropMessage(0, "fukin connor");
				target.dropMessage(0, c.getPlayer().getName() + " has made you poop");
		    } catch (NullPointerException npe) {
		      	c.getPlayer().dropMessage(0, "The target player cannot be found");
		    }
			return 0;
		}
    	
    }
      
      public static class Revive extends CommandExecute {
        
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMapId() != 262022133) {
                c.getPlayer().dropMessage(6, "[FFA] You must join the Free for All Arena to use this command.");
                return 0;
            }
            c.getPlayer().changeMap(c.getPlayer().getMapId(), 0);
            c.getPlayer().getStat().heal(c.getPlayer());
            c.getPlayer().dispelDebuffs();
            c.getPlayer().setInvulnerableTime();
            c.getPlayer().dropMessage(5, "[Anti-Spawn Killing] You're invulnerable for 3 seconds.");
            return 1;
        }
    }
      
       public static class Lotto extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            int number = Integer.parseInt(splitted[1]); 
            int mesos = c.getPlayer().getClient().getChannelServer().mesos;
            if (c.getPlayer().getClient().getChannelServer().lotteryOn == false) { 
                c.getPlayer().dropMessage(6, "[Lottery] The lottery event has not started yet.");
            } else if (System.currentTimeMillis() - c.getPlayer().getLastLotto() < 5000) {
                c.getPlayer().dropMessage(5, "[Lottery] Please wait 5 seconds before guessing again.");
            } else if (number < 0 || number > 100) {
                c.getPlayer().dropMessage(6, "[Lottery] Please pick a number between 0 - 100.");
            }   else if (number != c.getPlayer().getNumber()) {
                c.getPlayer().gainMeso(-45000, true);
                c.getPlayer().setLastLotto(System.currentTimeMillis());
                c.getPlayer().dropMessage(5, "[Lottery] Try again, you did not guess the correct number. You lost " + mesos + " mesos");
            }
            else if (number == c.getPlayer().getNumber()) {
                c.getPlayer().gainMeso(mesos, true);
                c.getPlayer().setLastLotto(System.currentTimeMillis());
            c.getPlayer().dropMessage(5, "[Lottery] Jackpot! You got the correct number!");
                }
            else {
                NPCScriptManager.getInstance().start(c, 9000055);
            }
        return 1;
        }
    }
       
        public static class FunCommands extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "----------------------------");
            c.getPlayer().dropMessage(6, "Fun Commands List");
            c.getPlayer().dropMessage(6, "----------------------------");
            c.getPlayer().dropMessage(5, "Costs 30,000,000 Mesos");
            c.getPlayer().dropMessage(6, "@hug <name> - Hugs the target for FREE");
            c.getPlayer().dropMessage(6, "@stroke <name> - Strokes the targets penis");
            c.getPlayer().dropMessage(6, "@lick <name> - Licks the target");
            c.getPlayer().dropMessage(6, "@bite <name> - Bites the target");
            c.getPlayer().dropMessage(6, "@tickle <name> - Tickles the target");
            c.getPlayer().dropMessage(6, "@kiss <name> - Kisses the target");
            c.getPlayer().dropMessage(6, "@kick <name> - Kicks the target");
            c.getPlayer().dropMessage(6, "@smack <name> - Smacks the target");
            c.getPlayer().dropMessage(6, "@blow <name> - Blows the target");
            c.getPlayer().dropMessage(6, "@poop <name> - Poops/Spawns poop on the target. Dedicated to connor and his bathroom snaps.");
            c.getPlayer().dropMessage(6, "@rape <name> - Rapes the target");
            c.getPlayer().dropMessage(6, "----------------------------");
            c.getPlayer().dropMessage(5, "Costs 5 Rubians");
            c.getPlayer().dropMessage(6, "@mug <name> - Mugs the target of mesos/rubians and kills");
            c.getPlayer().dropMessage(6, "----------------------------");
            
            
        return 1;
        }
    }
    
    
    public static class Hug extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }

            chrs.dropMessage(5, c.getPlayer().getName() + " hugged you. Aw <3");
            c.getPlayer().dropMessage(1, "You have hugged " + chrs.getName() + " for 30,000,000 Mesos.");
            return 1;
        }
    }
    
    public static class Stroke extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }
            
            if (c.getPlayer().getMeso() <= 10000) {
                c.getPlayer().dropMessage(5, "You don't have 30,000,000 Mesos");
                return 1;
            }
            c.getPlayer().gainMeso(-10000, true);
            chrs.dropMessage(5, c.getPlayer().getName() + " stroked your small penis.");
            c.getPlayer().dropMessage(1, "You have stroked " + chrs.getName() + " small penis for 30,000,000 Mesos.");
            return 1;
        }
    }
    
    public static class Lick extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }
            
            if (c.getPlayer().getMeso() <= 10000) {
                c.getPlayer().dropMessage(5, "You don't have 30,000,000 Mesos");
                return 1;
            }
            c.getPlayer().gainMeso(-10000, true);
            chrs.dropMessage(5, c.getPlayer().getName() + " licked you.");
            c.getPlayer().dropMessage(1, "You have licked " + chrs.getName() + " for 30,000,000 Mesos.");
            return 1;
        }
    }
    
    public static class Bite extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }
            
            if (c.getPlayer().getMeso() <= 10000) {
                c.getPlayer().dropMessage(5, "You don't have 30,000,000 Mesos");
                return 1;
            }
            c.getPlayer().gainMeso(-10000, true);
            chrs.dropMessage(5, c.getPlayer().getName() + " bited you.");
            c.getPlayer().dropMessage(1, "You have bited " + chrs.getName() + " for 30,000,000 Mesos.");
            return 1;
        }
    }
    
    public static class Tickle extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }
            
            if (c.getPlayer().getMeso() <= 10000) {
                c.getPlayer().dropMessage(5, "You don't have 30,000,000 Mesos");
                return 1;
            }
            c.getPlayer().gainMeso(-10000, true);
            chrs.dropMessage(5, c.getPlayer().getName() + " tickled you.");
            c.getPlayer().dropMessage(1, "You have tickled " + chrs.getName() + " for 30,000,000 Mesos.");
            return 1;
        }
    }
    
    public static class Kiss extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }
            
            if (c.getPlayer().getMeso() <= 10000) {
                c.getPlayer().dropMessage(5, "You don't have 30,000,000 Mesos");
                return 1;
            }
            c.getPlayer().gainMeso(-10000, true);
            chrs.dropMessage(5, c.getPlayer().getName() + " kissed you.");
            c.getPlayer().dropMessage(1, "You have kissed " + chrs.getName() + " for 30,000,000 Mesos.");
            return 1;
        }
    }
    
    public static class Smack extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }
            
            if (c.getPlayer().getMeso() <= 10000) {
                c.getPlayer().dropMessage(5, "You don't have 30,000,000 Mesos");
                return 1;
            }
            c.getPlayer().gainMeso(-10000, true);
            chrs.dropMessage(5, c.getPlayer().getName() + " smacked you.");
            c.getPlayer().dropMessage(1, "You have smacked " + chrs.getName() + " for 30,000,000 Mesos.");
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            return 1;
        }
    }

    public static class Blow extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }
            
            if (c.getPlayer().getMeso() <= 10000) {
                c.getPlayer().dropMessage(5, "You don't have 30,000,000 Mesos");
                return 1;
            }
            c.getPlayer().gainMeso(-10000, true);
            chrs.dropMessage(5, c.getPlayer().getName() + " blew you.");
            c.getPlayer().dropMessage(1, "You blew " + chrs.getName() + " for 30,000,000 Mesos.");
            return 1;
        }
    }
    
    public static class Kick extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs.getGMLevel() > 2) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not found.");
                return 1;
            }
            if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
                return 1;
            }
            if (chrs.getGMLevel() >= 6) {
                c.getPlayer().getStat().setHp((short) 0, c.getPlayer());
                c.getPlayer().getStat().setMp((short) 0, c.getPlayer());
                c.getPlayer().updateSingleStat(MapleStat.HP, 0);
                c.getPlayer().updateSingleStat(MapleStat.MP, 0);
                chrs.dropMessage(5, c.getPlayer().getName() + " attempted to kick you.");
                c.getPlayer().dropMessage(1, "You have attempted to kick " + chrs.getName() + " for 30,000,000 Mesos.");
            } else if (c.getPlayer().getMeso() <= 10000) {
                c.getPlayer().dropMessage(5, "You don't have 30,000,000 Mesos");
            } else {
            c.getPlayer().gainMeso(-10000, true);
            chrs.dropMessage(5, c.getPlayer().getName() + " kicked you.");
                c.getPlayer().dropMessage(1, "You have kicked " + chrs.getName() + " for 30,000,000 Mesos.");
            }
            return 1;
        }
    }
    
    public static class Mug extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            
                int minimumTimeToWait = 260;
            try{
                PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("Select * from characters where name = ?");
                ps.setString(1, c.getPlayer().getName());
                ResultSet rs = ps.executeQuery();
                if(!rs.next()){
                    rs.close();
                    ps.close();
                } else {
                    long ts2 = rs.getLong("mugTime");
                    long ts1 = System.currentTimeMillis();
                    int timeDif = (int)(ts1-ts2)/60000;
                    if(timeDif >= minimumTimeToWait || timeDif < 0){
                        MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            int random = (int) ((Math.random() * 100) + 0);
            if (c.getPlayer().getMapId() == 103000007) {
                c.getPlayer().dropMessage(1, "You cannot mug in this map.");
                c.getPlayer().dropMessage(1, "You cannot mug in this map.");
                c.getPlayer().dropMessage(1, "You cannot mug in this map.");
                c.getPlayer().dropMessage(1, "You cannot mug in this map.");
                c.getPlayer().dropMessage(1, "You cannot mug in this map.");
            }  else if (chrs.getMapId() != c.getPlayer().getMapId()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is not in the same map as you.");
            } else if (!chrs.isAlive()) {
                c.getPlayer().dropMessage(6, chrs.getName() + " is already dead.");
            } else if (chrs == null) {
                c.getPlayer().dropMessage(6, chrs.getName() + " could not be found.");
            } else if (chrs.getGMLevel() >= 6) {
                c.getPlayer().getStat().setHp((short) 0, c.getPlayer());
                c.getPlayer().getStat().setMp((short) 0, c.getPlayer());
                c.getPlayer().updateSingleStat(MapleStat.HP, 0);
                c.getPlayer().updateSingleStat(MapleStat.MP, 0);
                c.getPlayer().dropMessage(1, "Nice try.");
                c.getPlayer().dropMessage(6, "You did not lose any rubians trying to mug a ADMIN. However you're a banana now and lost 60m. Fucking punk.");
                c.getPlayer().gainMeso(-60000000, true);
            }  else {
                
                if (random > 30) {
                    c.getPlayer().getStat().setHp((short) 0, c.getPlayer());
                    c.getPlayer().getStat().setMp((short) 0, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.HP, 0);
                    c.getPlayer().updateSingleStat(MapleStat.MP, 0);
                    c.getPlayer().dropMessage(1, "You have attempted to mug " + chrs.getName() + "but, " + chrs.getName() + " killed you, took YOUR mesos and got away.");
                    c.getPlayer().gainMeso(-2000000, true);
                } else if (random > 20) {
                    chrs.getStat().setHp((short) 0, chrs);
                    chrs.getStat().setMp((short) 0, chrs);
                    chrs.updateSingleStat(MapleStat.HP, 0);
                    chrs.updateSingleStat(MapleStat.MP, 0);
                    if (chrs.getMeso() >= 2000000) {
                        chrs.gainMeso(-2000000, true);
                        c.getPlayer().gainMeso(2000000, true);
                    chrs.dropMessage(1, "You've been mugged for 2,000,000 Mesos by " + c.getPlayer().getName() + "do not call a GM, we do not give a shit. #NYPD");
                    chrs.dropMessage(5, c.getPlayer().getName() + " mugged you.");
                    c.getPlayer().dropMessage(1, "You have mugged " + chrs.getName() + " Smoooooooth Criminal.");
                    }
                    
                    else {
                        c.getPlayer().dropMessage(1, "Unable to mug" + chrs.getName() + " for 2,000,000 mesos. They must not have that amount. Oh well!");
                    }
                           
                    
                } else if (random > 10) {
                    chrs.getStat().setHp((short) 0, chrs);
                    chrs.getStat().setMp((short) 0, chrs);
                    chrs.updateSingleStat(MapleStat.HP, 0);
                    chrs.updateSingleStat(MapleStat.MP, 0);
                    if (chrs.getMeso() >= 5000000) {
                        chrs.gainMeso(-5000000, true);
                        c.getPlayer().gainMeso(5000000, true);
                    chrs.dropMessage(1, "You've been mugged for 5,000,000 Mesos by " + c.getPlayer().getName() + "do not call a GM, we do not give a shit #NYPD");
                    chrs.dropMessage(5, c.getPlayer().getName() + " mugged you.");
                    c.getPlayer().dropMessage(1, "You got lucky! You have mugged " + chrs.getName() + " for 5,000,000 mesos Smoooooooth Criminal.");
                    }
                    
                    else {
                        c.getPlayer().dropMessage(1, "Unable to mug" + chrs.getName() + " for 5,000,000 mesos. They must not have that amount. Oh well!");
                    }
                    
                }  else if (random >= 5) {
                    c.getPlayer().getStat().setHp((short) 0, chrs);
                    c.getPlayer().getStat().setMp((short) 0, chrs);
                    c.getPlayer().updateSingleStat(MapleStat.HP, 0);
                    c.getPlayer().updateSingleStat(MapleStat.MP, 0);
                    if (c.getPlayer().getMeso() >= 15000000) {
                        c.getPlayer().gainMeso(-15000000, true);
                    chrs.dropMessage(1, c.getPlayer().getName() + "tried to mug you but got caught by the police. #NYPD");
                    c.getPlayer().dropMessage(1, "You got caught! You tried to mug" + chrs.getName() + " for 5,000,000 mesos and the police caught you in the act. You lose 15,000,000 mesos for bail.");
                    }
                    else {
                      c.getPlayer().dropMessage(1, "since you have no mesos you were let free after 367 years. Aku has taken over the world, Goddamnit.");
                    }
                    
                }
                else if (random < 2) {
                    chrs.getStat().setHp((short) 0, chrs);
                    chrs.getStat().setMp((short) 0, chrs);
                    chrs.updateSingleStat(MapleStat.HP, 0);
                    chrs.updateSingleStat(MapleStat.MP, 0);
                    if (chrs.haveItem(4001024, 1)) {
                        chrs.gainItem(4001024, (short) -1);
                        c.getPlayer().gainItem(4001024, (short) 1);
                    chrs.dropMessage(1, "You've been mugged for 1 Rubian by " + c.getPlayer().getName());
                    chrs.dropMessage(5, c.getPlayer().getName() + " mugged you.");
                    c.getPlayer().dropMessage(1, "You have mugged " + chrs.getName() + " Smoooooooth Criminal.");
                    }
                    else {
                        c.getPlayer().dropMessage(1, "Unable to mug. They do not have 1 rubian");
                        
                    }
                    
                } 
            }
                        ps = DatabaseConnection.getConnection().prepareStatement("update characters set mugTime = ? where name = ?");
                        ps.setLong(1, ts1);
                        ps.setString(2, c.getPlayer().getName());           
                        ps.executeUpdate();
                    } else {
                        c.getPlayer().dropMessage(1, "You cannot mug for " + (minimumTimeToWait-timeDif) + " more minutes.");
                    }
                    rs.close();
                    ps.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 1;
            
            
        }
    }
    
    
    public static class Vote extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "[AlmightyMS]Usage: @vote 0 for gtop100, and @vote 1 for ultimate private servers>");
                return 0;
            }
            final int myVote = c.getPlayer().vote(Integer.parseInt(splitted[1]));
            if (myVote == 0) {
                c.getPlayer().dropMessage(5, "[AlmightyMS]Your vote will have been finalized when you enter the CAPTCHA on the site.");
                return 1;
            } else if (myVote < 0) {
                c.getPlayer().dropMessage(5, "[AlmightyMS]Your vote was erroneous. Did you put a number that was not 0 or 1?");
            } else {
                c.getPlayer().dropMessage(5, "[AlmightyMS]Please wait " + myVote + " minutes to vote.");
            }
            return 0;
        }
    }  
    
    public static class achievements extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
       
     if (splitted[0].equalsIgnoreCase("@achievements")) { 
            c.getPlayer().dropMessage(5, "Your finished achievements:"); 
            for (Integer i : c.getPlayer().getFinishedAchievements()) { 
                c.getPlayer().dropMessage(5, MapleAchievements.getInstance().getById(i).getName() + " - " + MapleAchievements.getInstance().getById(i).getReward() + " NX."); 
            } 
        }  
            return 0;
    
   
            }
         
        }
    
    public static class CancelBuffs extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().cancelAllBuffs();
            return 1;
        }
    }
    
        public static class Tag extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMapId() != 800020400) {
                c.getPlayer().dropMessage(5, "You may not use command outside of the Tag Battle Map");
                return 0;
            }
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> players = map.getMapObjectsInRange(c.getPlayer().getPosition(), (double) 7000, Arrays.asList(MapleMapObjectType.PLAYER));
            for (MapleMapObject closeplayers : players) {
                MapleCharacter playernear = (MapleCharacter) closeplayers;
                if (playernear.isAlive() && playernear != c.getPlayer() && !playernear.isGM()) {
                    if (playernear.isAlive() && playernear != c.getPlayer() && !playernear.isGM()) {
                        playernear.updateSingleStat(MapleStat.HP, 0);
                        playernear.dropMessage(5, "You have been tagged by " + c.getPlayer().getName());
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(910000000);
                        playernear.changeMap(target);
                    }
                }
            }
            return 0;
        }
    }
        
         public static class resetexp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setExp(0);
            return 1;
        }
    }
         
         
          public static class EXP extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getExp() + " Experience points.");
            c.getPlayer().dropMessage(6, "You currently need " + c.getPlayer().getNeededExp() + " Experience points to advance to the next level.");
            return 1;
        }
    }
          
        public static class Evolve extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3 || splitted[1] == null || splitted[1].equals("") || splitted[2] == null || splitted[2].equals("")) {
                c.getPlayer().dropMessage(6, "!Evolve <your ign> <itemid>");
                return 0;
            } else {
                int id = Integer.parseInt(splitted[2]);
                MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                int itemamount = chr.getItemQuantity(id, true);
                if (itemamount > 0) {
                    c.getPlayer().dropMessage(6, " Your chosen item has evolved.");        
                    chr.evolveItem(id);
                } else {
                    c.getPlayer().dropMessage(6, chr.getName() + " doesn't have (" + id + ")");
                }
            }
            return 1;
        }
    }      
        
        public static class Killcount extends CommandExecute {
            
            public int execute(MapleClient c, String [] splitted) {
                c.getPlayer().dropMessage(5, "You currently have: " + c.getPlayer().getMonsterkills() + " monster kills." );
            return 1;
            }
        }
          
        
        
  /*          public static class TLVL extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int Tlvl = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("save")) {
                    c.getPlayer().setTormentLevel(Tlvl);
                }   
                c.getPlayer().dropMessage(6, "TormentMode has been set to " + Tlvl + "x");
            } else {
                c.getPlayer().dropMessage(6, "Syntax: @TormentMode number (1-6) save");
                c.getPlayer().dropMessage(6, "So for example : @TormentMode 1 save");
            }
            return 1;
        }
    }
    */
                 public static class T1On extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
                c.getPlayer().setTormentMode();
                c.getPlayer().dropMessage(6, "Torment Mode is now activated! Set the level using @TLVL <number> <save>");
                c.getPlayer().dropMessage(6, "Torment Mode is in BETA so do not use the above command yet. T1 mode is on.");
                

            
            return 1;
        }
    }
                 
        public static class T1Off extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
                c.getPlayer().setTormentMode();
                c.getPlayer().dropMessage(6, "Torment Mode is now deactivated, pussy.");
                

            
            return 1;
        }
    }
         public static class joinrace extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

//dirty af pls no flame :( 
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                   if (c.getPlayer().getEntryNumber() < 1) {
                   if (c.getPlayer().getMapId() == 240000000){
                   if (cserv.getWaiting()){
                       c.getPlayer().setEntryNumber(cserv.getCompetitors() + 1);
                       cserv.setCompetitors(cserv.getCompetitors() + 1);
		c.getPlayer().dropMessage(6, "[Notice]: You have successfully joined the race! Your entry number is " + c.getPlayer().getEntryNumber() + ".");
                   } else {
                       c.getPlayer().dropMessage(6, "There is no event currently taking place.");
                   }
                   } else {
                        c.getPlayer().dropMessage(6, "You are not at Leafre.");
                   }
                   }else{
                         c.getPlayer().dropMessage(6, "You have already joined this race.");
                   }
                 } 
               
            return 1;
}
          }
      public static class rules extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

//dirty af pls no flame :( 
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                   if (cserv.getWaiting() || cserv.getRace()) {
		c.getPlayer().dropMessage(6, "The Official Rules and Regulations of the Usain Bolt Race:");
                c.getPlayer().dropMessage(6, "-------------------------------------------------------------------------------------------");
               c.getPlayer().dropMessage(6, "To win you must race from Henesys all the way to Henesys going Eastward.");
                c.getPlayer().dropMessage(6, "Rule #1: No cheating. You can't use any warping commands, or you'll be disqualified.");
                c.getPlayer().dropMessage(6, "Rule #2: You may use any form of transportation. This includes Teleport, Flash Jump and Mounts. And no buffs. All buffs must be disposed prior to the race.");
               c.getPlayer().dropMessage(6, "Rule #3: You are NOT allowed to kill any monsters in your way. They are obstacles.");
                c.getPlayer().dropMessage(6, "Rule #4: You may start from anywhere in Henesys, but moving on to the next map before the start won't work.");
                   } else {
                       c.getPlayer().dropMessage(6, "There is no event currently taking place.");
                   }
                         }
    
            return 1;
  
}
      }
  
}

    



    
