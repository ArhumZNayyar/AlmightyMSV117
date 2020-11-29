/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.messages.commands;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConstants.PlayerGMRank;
import handling.channel.ChannelServer;
import handling.world.World;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import scripting.EventInstanceManager;
import scripting.EventManager;
import scripting.NPCScriptManager;
import server.MapleCarnivalChallenge;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleShopFactory;
import server.Timer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.OverrideMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.quest.MapleQuest;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InventoryPacket;

/**
 *
 * @author Rummy
 */
public class GMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.GM;
    }
    
public static class cleardrops extends CommandExecute
  {
    public int execute(MapleClient c, String[] splitted)
    {
      c.getPlayer().dropMessage(5, "Cleared " + c.getPlayer().getMap().getNumItems() + " drops");
      c.getPlayer().getMap().removeDrops();
      return 1;
    }
  }
   /*
      public static class giftnx extends CommandExecute
  {
    public int execute(MapleClient c, String[] splitted)
    {
      ChannelServer cserv = c.getChannelServer();
      cserv.getPlayerStorage().getCharacterByName(splitted[1]).modifyCSPoints(1, Integer.parseInt(splitted[2]));
      c.getPlayer().dropMessage(6, "The NX has been sent.");
      return 1;
    }
  }*/
      
    
    
    
    
        public static class warpoxmiddle extends CommandExecute
  {
    public int execute(MapleClient c, String[] splitted)
    {
      c.getPlayer().dropMessage(6, "The middle has been warped out.");
      for (MapleMapObject wrappedPerson : c.getPlayer().getMap().getCharactersAsMapObjects()) {
        MapleCharacter person = (MapleCharacter)wrappedPerson;
        if ((person.getPosition().y > -206) && (person.getPosition().y <= 274) && (person.getPosition().x >= -308) && (person.getPosition().x <= -142) && (!(person.isGM()))) {
          person.changeMap(person.getMap().getReturnMap(), person.getMap().getReturnMap().getPortal(0));
        }
      }
      return 1;
    }
  }

  public static class warpoxright extends CommandExecute
  {
    public int execute(MapleClient c, String[] splitted)
    {
      c.getPlayer().dropMessage(6, "The right has been warped out.");
      for (MapleMapObject wrappedPerson : c.getPlayer().getMap().getCharactersAsMapObjects()) {
        MapleCharacter person = (MapleCharacter)wrappedPerson;
        if ((person.getPosition().y > -206) && (person.getPosition().y <= 334) && (person.getPosition().x >= -142) && (person.getPosition().x <= 502) && (!(person.isGM()))) {
          person.changeMap(person.getMap().getReturnMap(), person.getMap().getReturnMap().getPortal(0));
        }
      }
      return 1;
    }
  }

  public static class warpox extends CommandExecute
  {
    public int execute(MapleClient c, String[] splitted)
    {
      if (c.getPlayer().getMap().getId() == 109020001) {
        MapleMap map1 = c.getPlayer().getMap();
        List<MapleMapObject> players1 = map1.getMapObjectsInRange(c.getPlayer().getPosition(), 1000.0D, Arrays.asList(new MapleMapObjectType[] { MapleMapObjectType.PLAYER }));

        for (MapleMapObject closeplayers : players1) {
          MapleCharacter playernear = (MapleCharacter)closeplayers;
          if ((!(playernear.isGM())) || (!(playernear.isAlive()))) {
            if (splitted[1].equalsIgnoreCase("hene"))
              playernear.changeMap(c.getChannelServer().getMapFactory().getMap(Integer.valueOf(100000000).intValue()));
            else {
              playernear.changeMap(c.getChannelServer().getMapFactory().getMap(Integer.valueOf(splitted[1]).intValue()));
            }
            playernear.dropMessage(5, "You have lost the event; you will be warped out.");
          }
        }
      } else {
        c.getPlayer().dropMessage(5, "You are not in OX event map !");
      }
      return 1;
    }
  }

  public static class warpoxleft extends CommandExecute
  {
    public int execute(MapleClient c, String[] splitted)
    {
      c.getPlayer().dropMessage(6, "The left has been warped out.");
      for (MapleMapObject wrappedPerson : c.getPlayer().getMap().getCharactersAsMapObjects()) {
        MapleCharacter person = (MapleCharacter)wrappedPerson;
        if ((person.getPosition().y > -206) && (person.getPosition().y <= 334) && (person.getPosition().x >= -952) && (person.getPosition().x <= -308) && (!(person.isGM()))) {
          person.changeMap(person.getMap().getReturnMap(), person.getMap().getReturnMap().getPortal(0));
        }
      }
      return 1;
    }
  }

  public static class warpoxtop extends CommandExecute
  {
    public int execute(MapleClient c, String[] splitted)
    {
      c.getPlayer().dropMessage(6, "The top has been warped out.");
      for (MapleMapObject wrappedPerson : c.getPlayer().getMap().getCharactersAsMapObjects()) {
        MapleCharacter person = (MapleCharacter)wrappedPerson;
        if ((person.getPosition().y <= -206) && (!(person.isGM()))) {
          person.changeMap(person.getMap().getReturnMap(), person.getMap().getReturnMap().getPortal(0));
        }
      }
      return 1;
    }
  }

    public static class GetSkill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);

            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            c.getPlayer().changeSingleSkillLevel(skill, level, masterlevel);
            return 1;
        }
    }

    

    

    public static class SP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setRemainingSp(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
            return 1;
        }
    }

    public static class Job extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (MapleCarnivalChallenge.getJobNameById(Integer.parseInt(splitted[1])).length() == 0) {
                c.getPlayer().dropMessage(5, "Invalid Job");
                return 0;
            }
            c.getPlayer().changeJob((short)Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class Shop extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory shop = MapleShopFactory.getInstance();
            int shopId = Integer.parseInt(splitted[1]);
            if (shop.getShop(shopId) != null) {
                shop.getShop(shopId).sendShop(c);
            }
            return 1;
        }
    }

    public static class LevelUp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getLevel() < 200) {
                c.getPlayer().gainExp(500000000, true, false, true);
                c.getPlayer().dropMessage(6, "You have now leveled up to level " + c.getPlayer().getLevel() + ".");
            }
            return 1;
        }
    }

  

    public static class Level extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setLevel(Short.parseShort(splitted[1]));
            c.getPlayer().levelUp();
            if (c.getPlayer().getExp() < 0) {
                c.getPlayer().gainExp(-c.getPlayer().getExp(), false, false, true);
                c.getPlayer().dropMessage(6, "You are now level " + c.getPlayer().getLevel() + ".");
            }
            return 1;
        }
    }

    public static class StartAutoEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final EventManager em = c.getChannelServer().getEventSM().getEventManager("AutomatedEvent");
            if (em != null) {
                em.scheduleRandomEvent();
            }
            return 1;
        }
    }

    public static class SetEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleEvent.onStartEvent(c.getPlayer());
            return 1;
        }
    }
        public int execute(MapleClient c, String[] splitted) {
            if (c.getChannelServer().getEvent() == c.getPlayer().getMapId()) {
                MapleEvent.setEvent(c.getChannelServer(), false);
                c.getPlayer().dropMessage(5, "Started the event and closed off");
                return 1;
            } else {
                c.getPlayer().dropMessage(5, "!event must've been done first, and you must be in the event map.");
                return 0;
            }
        }
    

    public static class Event extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleEventType type = MapleEventType.getByString(splitted[1]);
            if (type == null) {
                final StringBuilder sb = new StringBuilder("Wrong syntax: ");
                for (MapleEventType t : MapleEventType.values()) {
                    sb.append(t.name()).append(",");
                }
                c.getPlayer().dropMessage(5, sb.toString().substring(0, sb.toString().length() - 1));
                return 0;
            }
            final String msg = MapleEvent.scheduleEvent(type, c.getChannelServer());
            if (msg.length() > 0) {
                c.getPlayer().dropMessage(5, msg);
                return 0;
            }
            return 1;
        }
    }

    public static class RemoveItem extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "This player does not exist.");
                return 0;
            }
            chr.removeAll(Integer.parseInt(splitted[2]), false);
            c.getPlayer().dropMessage(6, "All items with the ID " + splitted[2] + " has been removed from the inventory of " + splitted[1] + ".");
            return 1;

        }
    }

    public static class LockItem extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "This player does not exist.");
                return 0;
            }
            int itemid = Integer.parseInt(splitted[2]);
            MapleInventoryType type = GameConstants.getInventoryType(itemid);
            for (Item item : chr.getInventory(type).listById(itemid)) {
                item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                chr.getClient().getSession().write(InventoryPacket.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, chr));
            }
            if (type == MapleInventoryType.EQUIP) {
                type = MapleInventoryType.EQUIPPED;
                for (Item item : chr.getInventory(type).listById(itemid)) {
                    item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                    //chr.getClient().getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                }
            }
            c.getPlayer().dropMessage(6, "All items with the ID " + splitted[2] + " has been locked from the inventory of " + splitted[1] + ".");
            return 1;
        }
    }

    public static class KillMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter map : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (map != null && !map.isGM()) {
                    map.getStat().setHp((short) 0, map);
                    map.getStat().setMp((short) 0, map);
                    map.updateSingleStat(MapleStat.HP, 0);
                    map.updateSingleStat(MapleStat.MP, 0);
                    c.getPlayer().dropMessage(6, "You have killed the map.");
                }
            }
            return 1;
        }
    }

    

    

    public static class Disease extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "!disease <type> [charname] <level> where type = SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            int type = 0;
            if (splitted[1].equalsIgnoreCase("SEAL")) {
                type = 120;
            } else if (splitted[1].equalsIgnoreCase("DARKNESS")) {
                type = 121;
            } else if (splitted[1].equalsIgnoreCase("WEAKEN")) {
                type = 122;
            } else if (splitted[1].equalsIgnoreCase("STUN")) {
                type = 123;
            } else if (splitted[1].equalsIgnoreCase("CURSE")) {
                type = 124;
            } else if (splitted[1].equalsIgnoreCase("POISON")) {
                type = 125;
            } else if (splitted[1].equalsIgnoreCase("SLOW")) {
                type = 126;
            } else if (splitted[1].equalsIgnoreCase("SEDUCE")) {
                type = 128;
            } else if (splitted[1].equalsIgnoreCase("REVERSE")) {
                type = 132;
            } else if (splitted[1].equalsIgnoreCase("ZOMBIFY")) {
                type = 133;
            } else if (splitted[1].equalsIgnoreCase("POTION")) {
                type = 134;
            } else if (splitted[1].equalsIgnoreCase("SHADOW")) {
                type = 135;
            } else if (splitted[1].equalsIgnoreCase("BLIND")) {
                type = 136;
            } else if (splitted[1].equalsIgnoreCase("FREEZE")) {
                type = 137;
            } else if (splitted[1].equalsIgnoreCase("POTENTIAL")) {
                type = 138;
            } else {
                c.getPlayer().dropMessage(6, "!disease <type> [charname] <level> where type = SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            if (splitted.length == 4) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
                if (victim == null) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return 0;
                }
                victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
            } else {
                for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                    victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
                }
            }
            return 1;
        }
    }

    public static class CloneMe extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().cloneLook();
            c.getPlayer().dropMessage(6, "You have now been cloned.");
            return 1;
        }
    }

    public static class DisposeClones extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, c.getPlayer().getCloneSize() + " clones disposed.");
            c.getPlayer().disposeClones();
            return 1;
        }
    }

    public static class SetInstanceProperty extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
            if (em == null || em.getInstances().size() <= 0) {
                c.getPlayer().dropMessage(5, "none");
            } else {
                em.setProperty(splitted[2], splitted[3]);
                for (EventInstanceManager eim : em.getInstances()) {
                    eim.setProperty(splitted[2], splitted[3]);
                }
            }
            return 1;
        }
    }

    public static class ListInstanceProperty extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
            if (em == null || em.getInstances().size() <= 0) {
                c.getPlayer().dropMessage(5, "none");
            } else {
                for (EventInstanceManager eim : em.getInstances()) {
                    c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", eventManager: " + em.getName() + " iprops: " + eim.getProperty(splitted[2]) + ", eprops: " + em.getProperty(splitted[2]));
                }
            }
            return 0;
        }
    }

    public static class LeaveInstance extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getEventInstance() == null) {
                c.getPlayer().dropMessage(5, "You are not in one");
            } else {
                c.getPlayer().getEventInstance().unregisterPlayer(c.getPlayer());
            }
            return 1;
        }
    }

    public static class WhosThere extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder builder = new StringBuilder("Players on Map: ").append(c.getPlayer().getMap().getCharactersThreadsafe().size()).append(", ");
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (builder.length() > 150) { // wild guess :o
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(6, builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2);
            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }

    public static class StartInstance extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getEventInstance() != null) {
                c.getPlayer().dropMessage(5, "You are in one");
            } else if (splitted.length > 2) {
                EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
                if (em == null || em.getInstance(splitted[2]) == null) {
                    c.getPlayer().dropMessage(5, "Not exist");
                } else {
                    em.getInstance(splitted[2]).registerPlayer(c.getPlayer());
                }
            } else {
                c.getPlayer().dropMessage(5, "!startinstance [eventmanager] [eventinstance]");
            }
            return 1;

        }
    }

    public static class ResetMobs extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().killAllMonsters(false);
            c.getPlayer().dropMessage(6, "Monsters have been reset.");
            return 1;
        }
    }

    public static class KillMonsterByOID extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.killMonster(monster, c.getPlayer(), false, false, (byte) 1);
                c.getPlayer().dropMessage(6, "You have killed the monster by its ID.");
            }
            return 1;
        }
    }

    

    public static class Notice extends CommandExecute {

        protected static int getNoticeType(String typestring) {
            if (typestring.equals("n")) {
                return 0;
            } else if (typestring.equals("p")) {
                return 1;
            } else if (typestring.equals("l")) {
                return 2;
            } else if (typestring.equals("nv")) {
                return 5;
            } else if (typestring.equals("v")) {
                return 5;
            } else if (typestring.equals("b")) {
                return 6;
            }
            return -1;
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int joinmod = 1;
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }

            int tfrom = 2;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            int type = getNoticeType(splitted[tfrom]);
            if (type == -1) {
                type = 0;
                joinmod = 0;
            }
            StringBuilder sb = new StringBuilder();
            if (splitted[tfrom].equals("nv")) {
                sb.append("[AlmightyMS]");
            } else {
                sb.append("");
            }
            joinmod += tfrom;
            sb.append(StringUtil.joinStringFrom(splitted, joinmod));

            byte[] packet = CWvsContext.serverNotice(type, sb.toString());
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class Yellow extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 2;
            }
            byte[] packet = CWvsContext.yellowChat((splitted[0].equals("!y") ? ("[" + c.getPlayer().getName() + "] ") : "") + StringUtil.joinStringFrom(splitted, 2));
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class Y extends Yellow {
    }

    public static class WhatsMyIP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "IP: " + c.getSession().getRemoteAddress().toString().split(":")[0]);
            return 1;
        }
    }

    

    public static class TDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().toggleDrops();
            return 1;
        }
    }
    
    public static class rb extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
           c.getPlayer().doReborn();
            return 1;
        }
    } 
     
    public static class Spawn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int mid = Integer.parseInt(splitted[1]);
            final int num = Math.min(CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1), 500);
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");

            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                return 0;
            }
            if (onemob == null) {
                c.getPlayer().dropMessage(5, "Mob does not exist");
                return 0;
            }
            long newhp = 0;
            int newexp = 0;
            if (hp != null) {
                newhp = hp.longValue();
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php.doubleValue() / 100));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (exp != null) {
                newexp = exp.intValue();
            } else if (pexp != null) {
                newexp = (int) (onemob.getMobExp() * (pexp.doubleValue() / 100));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }

            final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setHp(newhp);
                if (level != null) {
                    mob.changeLevel(level.intValue(), false);
                } else {
                    mob.setOverrideStats(overrideStats);
                }
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            }
           return 1;
        }
    }
    public static class Zakum extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnZakum(c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    
    public static class Czak extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnChaosZakum(c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    
    public static class PB extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8820001, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    
    public static class HT extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8810026, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            
            return 0;
        }
        
    }
     
    public static class CHT extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8810130, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            
            return 0;
        }
        
    }
    public static class VL extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8840000, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    public static class Cygnus extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8850011, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    public static class Ark extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8860000, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    public static class Hilla extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8870000, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    
    public static class cMagnus extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8880100, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    
    public static class Magnus extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8880000, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    
     public static class Gollux extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(9390601, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
           
            return 0;
        }
        
    }
     
               
            public static class OnlineAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Characters connected to channel 1: " + ChannelServer.getInstance(1).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 2: " + ChannelServer.getInstance(2).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 3: " + ChannelServer.getInstance(3).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 4: " + ChannelServer.getInstance(4).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 5: " + ChannelServer.getInstance(5).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 6: " + ChannelServer.getInstance(6).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 7: " + ChannelServer.getInstance(7).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 8: " + ChannelServer.getInstance(8).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 9: " + ChannelServer.getInstance(9).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 10: " + ChannelServer.getInstance(10).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 11: " + ChannelServer.getInstance(11).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 12: " + ChannelServer.getInstance(12).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 13: " + ChannelServer.getInstance(13).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 14: " + ChannelServer.getInstance(14).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 15: " + ChannelServer.getInstance(15).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 16: " + ChannelServer.getInstance(16).getPlayerStorage().getOnlinePlayers(true));
            c.getPlayer().dropMessage(6, "Characters connected to channel 17: " + ChannelServer.getInstance(17).getPlayerStorage().getOnlinePlayers(true));
            return 1;
        }
    }
    
    public static class Kaiser extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8880004, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
    
     public static class Vellum extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getPosition().getX();
            c.getPlayer().getPosition().getY();
           c.getPlayer().getMap().spawnMonsterOnGroundBelow(8930000, c.getPlayer().getPosition().x,c.getPlayer().getPosition().y);
            return 0;
        }
        
    }
     public static class Revive extends CommandExecute {
@Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                victim.changeMap(victim.getMapId(), 0);
                victim.getStat().heal(victim);
                victim.dispelDebuffs();
                victim.dropMessage(5, "[AlmightyMS] You're Invincible for 3 seconds.");
                victim.setInvulnerableTime();
    return 1;

            }

            
           
        }
    
   
    public static class FakeRelog extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().fakeRelog();
            return 1;
        }
    }
    public static class Hide extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SkillFactory.getSkill(GameConstants.GMS ? 9101004 : 9001004).getEffect(1).applyTo(c.getPlayer());
            c.getPlayer().dropMessage(6, "You are now in hide.");
            return 0;
        }
    }
    public static class unHide extends CommandExecute { 

        @Override 
        public int execute(MapleClient c, String[] splitted) { 
            c.getPlayer().dispelSkill(9101004); 
             
            return 0; 
        } 
    } 
    

    public static class TagKill extends CommandExecute {
	
        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> players = map.getMapObjectsInRange(c.getPlayer().getPosition(), (double) 12000, Arrays.asList(MapleMapObjectType.PLAYER));
            for (MapleMapObject closeplayers : players) {
                MapleCharacter playernear = (MapleCharacter) closeplayers;
                if (playernear.isAlive());
                {
                    if (!playernear.isGM()) {
                        playernear.getStat().setHp((short) 0, playernear);
                        playernear.updateSingleStat(MapleStat.HP, 0);
                        playernear.dropMessage(5, "Tagged");
                    }
                }
            }
            return 1;
        }
    }
    
 
    
    public static class Trollvac extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            
        MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
        List<MapleMapObject> items = chr.getMap().getMapObjectsInRange(chr.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
        for (MapleMapObject item : items) {
            MapleMapItem mapItem = (MapleMapItem) item;
            if (mapItem.getMeso() > 0) { // Mesos
                chr.gainMeso(mapItem.getMeso(), true);
            } else {
                MapleInventoryManipulator.addFromDrop(c, mapItem.getItem(), true);
            }
            mapItem.setPickedUp(true);
            chr.getMap().removeMapObject(item);
            chr.getMap().broadcastMessage(CField.removeItemFromMap(mapItem.getObjectId(), 2, chr.getId()), mapItem.getPosition());
            c.getPlayer().dropMessage(5, chr.getName() + " has been trolled.");
    }
    return 1;
        }
    }
    
       public static class HGEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap target = c.getChannelServer().getMapFactory().getMap(931050412);
            c.getPlayer().changeMap(target, target.getPortal(0));
           return 1;
        }
    }
    
    
    public static class MapleStory extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap target = c.getChannelServer().getMapFactory().getMap(109060001);
            c.getPlayer().changeMap(target, target.getPortal(0));
           return 1;
        }
    }
    
    public static class Maze extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap target = c.getChannelServer().getMapFactory().getMap(109030101);
            c.getPlayer().changeMap(target, target.getPortal(0));
           return 1;
        }
    }
    
    public static class PrizeMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap target = c.getChannelServer().getMapFactory().getMap(109050000);
            c.getPlayer().changeMap(target, target.getPortal(0));
           return 1;
        }
    }
    public static class Lottery extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int number = Integer.parseInt(splitted[1]); 
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(5, "[Syntax] !lotto <Number> <Mesos each guess>");
                return 0;
            } else if (number < 0 || number > 100) {
                c.getPlayer().dropMessage(6, "[Lottery] Please pick a number between 0 - 100.");
                return 0;
            }
            if (c.getPlayer().getClient().getChannelServer().lotteryOn == false && c.getPlayer().getClient().getChannelServer().freeLotteryOn == false) {
                int mesos = Integer.parseInt(splitted[2]); 
                c.getPlayer().getClient().getChannelServer().lotteryOn = true; 
                c.getPlayer().getClient().getChannelServer().number = number; 
               // c.getPlayer().getClient().getChannelServer().rubians = rubians;
                c.getPlayer().getClient().getChannelServer().mesos = mesos;
                c.getChannelServer().broadcastMessage(CWvsContext.serverNotice(6, "[Lottery] The lottery has started. To guess the number between 0 - 100, type @lotto [#]."));
                c.getChannelServer().broadcastMessage(CWvsContext.serverNotice(6, "[Lottery] This will cost " + splitted[2] + " Mesos each guess."));
            } else { 
                c.getPlayer().getClient().getChannelServer().lotteryOn = false; 
                c.getPlayer().getClient().getChannelServer().freeLotteryOn = false; 
                c.getPlayer().getClient().getChannelServer().jackpot = 0; 
                c.getChannelServer().broadcastMessage(CWvsContext.serverNotice(6, "[Lottery] The lottery event has ended.")); 
                c.getPlayer().dropMessage(5, "[AlmightyMS] You can't have a Free Lottery Event and a Lottery Event going on at the same time.");
            }
    return 1;
        }
    }
    
    public static class DiseaseMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "!disease <type> <level> where type = SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            int type = 0;
            if (splitted[1].equalsIgnoreCase("SEAL")) {
                type = 120;
            } else if (splitted[1].equalsIgnoreCase("DARKNESS")) {
                type = 121;
            } else if (splitted[1].equalsIgnoreCase("WEAKEN")) {
                type = 122;
            } else if (splitted[1].equalsIgnoreCase("STUN")) {
                type = 123;
            } else if (splitted[1].equalsIgnoreCase("CURSE")) {
                type = 124;
            } else if (splitted[1].equalsIgnoreCase("POISON")) {
                type = 125;
            } else if (splitted[1].equalsIgnoreCase("SLOW")) {
                type = 126;
            } else if (splitted[1].equalsIgnoreCase("SEDUCE")) {
                type = 128;
            } else if (splitted[1].equalsIgnoreCase("REVERSE")) {
                type = 132;
            } else if (splitted[1].equalsIgnoreCase("ZOMBIFY")) {
                type = 133;
            } else if (splitted[1].equalsIgnoreCase("POTION")) {
                type = 134;
            } else if (splitted[1].equalsIgnoreCase("SHADOW")) {
                type = 135;
            } else if (splitted[1].equalsIgnoreCase("BLIND")) {
                type = 136;
            } else if (splitted[1].equalsIgnoreCase("FREEZE")) {
                type = 137;
            } else if (splitted[1].equalsIgnoreCase("POTENTIAL")) {
                type = 138;
            } else {
                c.getPlayer().dropMessage(6, "!disease <type> [charname] <level> where type = SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            if (splitted.length == 3) {
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                 if (chr == null) {
                         c.getPlayer().dropMessage(5, "Not found.");
                         return 0;
                    }
                    if (chr.getGMLevel() < 3) {
                        chr.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1));
                    }
                }
            }
            return 1;
        }
    }
    
    public static class StunMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                 if (chr == null) {
                         c.getPlayer().dropMessage(5, "Not found.");
                         return 0;
                    }
                    if (chr.getGMLevel() < 3) {
                        chr.disease(123, 1);
                    }
                } 
            return 1;
        }
    }
    

     
     public static class CCPlayer extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().changeChannel(World.Find.findChannel(splitted[1]));
            return 1;
        }
    }
     
      public static class CheckPoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Need playername.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                c.getPlayer().dropMessage(6, chrs.getName() + " has " + chrs.getPoints() + " points.");
            }
            return 1;
        }
    }

    public static class CheckVPoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Need playername.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
            } else {
                c.getPlayer().dropMessage(6, chrs.getName() + " has " + chrs.getVPoints() + " vpoints.");
            }
            return 1;
        }
    }

    public static class PermWeather extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMap().getPermanentWeather() > 0) {
                c.getPlayer().getMap().setPermanentWeather(0);
                c.getPlayer().getMap().broadcastMessage(CField.removeMapEffect());
                c.getPlayer().dropMessage(5, "Map weather has been disabled.");
            } else {
                final int weather = CommandProcessorUtil.getOptionalIntArg(splitted, 1, 5120000);
                if (!MapleItemInformationProvider.getInstance().itemExists(weather) || weather / 10000 != 512) {
                    c.getPlayer().dropMessage(5, "Invalid ID.");
                } else {
                    c.getPlayer().getMap().setPermanentWeather(weather);
                    c.getPlayer().getMap().broadcastMessage(CField.startMapEffect("", weather, false));
                    c.getPlayer().dropMessage(5, "Map weather has been enabled.");
                }
            }
            return 1;
        }
    }
    
    public static class WarpAllFm extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                final MapleMap target = c.getChannelServer().getMapFactory().getMap(910000000);
                final MapleMap from = c.getPlayer().getMap();
                for (MapleCharacter chr : from.getCharactersThreadsafe()) {
                    if (chr.getGMLevel() < 4) { 
                        chr.changeMap(target, target.getPortal(0));
                        chr.dropMessage(6, "Everyone has been warped to the FM.");
                    } else {
                        chr.dropMessage(6, "Everyone has been warped to the FM except for you.");
                    }
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                return 0; //assume drunk GM
            }
            return 1;
        }
    }
  /*  
     public static class PVPON extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
                c.getPlayer().getMap().TurnOnPvP();
                for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                    World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "Hunger Games is now activated! Kill a player beside you using any skills, And may the odds be ever in your favor."));
                    World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "Tip: Try not to die."));
                    
                }
            return 0;
            
        }
     }
     //not done not gunna fix either
     public static class PVPOFF extends CommandExecute {
         @Override
         public int execute(MapleClient c, String[] splitted) {
             
             c.getPlayer().getMap().TurnOffPvP();
            for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "Hunger Games is now deactivated!"));
                }
             return 0;
         }
     }
    */
     
     
     public static class AskOX extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMapId() != 109020001) {
                c.getPlayer().changeMap(109020001, 0);
                c.getPlayer().dropMessage(6, "Warp other players to this map to begin the event");
                return 0;
            }
            final MapleCharacter asker = c.getPlayer();
            final MapleMap thisMap =  asker.getMap();        
            String[] trueCmd = {"true", "t", "left", "l", "o"};
            String[] falseCmd = {"false", "f", "right", "r", "x"};
            boolean tempAns;
            if (Arrays.asList(trueCmd).contains(splitted[1].toLowerCase())) {
                tempAns = true;
            } else if (Arrays.asList(falseCmd).contains(splitted[1].toLowerCase())) {   
                tempAns = false;
            } else {
                c.getPlayer().dropMessage(6, "Syntax: !AskOX <True/False> <Time> <Question>");
                return 1;
            }
            final boolean ans = tempAns;
            final int timeLimit = Math.min(Math.max(Integer.parseInt(splitted[2]), 1), 60);
            final String question = StringUtil.joinStringFrom(splitted, 3);

            thisMap.broadcastMessage(CField.MapEff("SportsDay/EndMessage/Start"));
            thisMap.broadcastMessage(CField.playSound("Dojang/start"));
            Timer.MapTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    thisMap.broadcastMessage(CWvsContext.serverNotice(0, question));
                    thisMap.broadcastMessage(CField.getClock(timeLimit));
                    Timer.MapTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            List<MapleCharacter> winners = new ArrayList<MapleCharacter>();
                            for (final MapleCharacter victim : thisMap.getCharacters()) {
                                if (victim == asker) {
                                    continue;
                                }
                                if ((victim.getPosition().x > -308 && victim.getPosition().x < -142) || victim.getPosition().y <= -206) {
                                    // NO ANSWER
                                    victim.getClient().getSession().write(CField.MapEff("SportsDay/EndMessage/TimeOver"));
                                    victim.getClient().getSession().write(CField.playSound("phantom/skaia"));
                                } else if (victim.getPosition().x > -308 ^ ans) {
                                    // CORRECT ANSWER
                                    winners.add(victim);
                                    victim.getClient().getSession().write(CField.MapEff("SportsDay/EndMessage/Win"));
                                    victim.getClient().getSession().write(CField.playSound("Party1/Clear"));  
                                } else {
                                    // WRONG ANSWER
                                    victim.getClient().getSession().write(CField.MapEff("SportsDay/EndMessage/Lose"));
                                    victim.getClient().getSession().write(CField.playSound("Party1/Failed"));
                                }
                            }
                            if (winners.size() > 0) {
                                for (final MapleCharacter victim : thisMap.getCharacters()) {
                                    if (victim != asker && !winners.contains(victim)) {
                                        // WARP OUT IN 5 SEC
                                        Timer.MapTimer.getInstance().schedule(new Runnable() {
                                        @Override
                                            public void run() {
                                                victim.changeMap(100000000, 0);
                                            }
                                         }, 5000);
                                    }
                                }
                            } else {
                                thisMap.broadcastMessage(CWvsContext.serverNotice(6, "Since there are no winners for this round, all participants will proceed on to the next round"));
                           
                            }
                        }
                    }, timeLimit * 1000);
                }
             }, 2000);
            return 1;
        }
    }
     
     public static class Commands extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            String p = Character.toString(splitted[0].charAt(0));//optional, you could type it manually if you want

            String[] commandsArray = {"WarpHere",
"Warp", "GoTo",
"Jail", "UnJail", "Fame", "GiftNX", "ban", "DC", "CCPlayer", "GMMap", "UnHide", "Hide", "FakeRelog", "warpcashshop",
"GivePoint",
"GiveVPoint",
"Heal",
"Healhere", "ClearDrops", "ReloadMap",
"Tempban",
"CC",
"CCPlayer",
"Kill",
"WhereAmI",
"Online",
"OnlineChannel",
"Connected",
"UpTime",
"Find",
"WhosFirst",
"WhoCompartor",
"WhosLast",
"WhosNext",
"WarpMap",
"KillAll",
"Winner",
"ToggleHG",
"Elf",
"Mute",
"UnMute",
"MuteMap",
"UnMuteMap",
"ItemCheck",
"LevelPerson",
 "ServerMessage", "DestroyPNPC", "MakeOfflineP", "MakePNPC", "NPC", "KillAllEXP", "KillAllDrops", "HitMonster", "HitAll", "ClearSquads", "StripEveryone", "TMegaPhone", "TogglePhone", "ToggleOffense", "Mointor", "SpeakWorld",
"Y", "Yellow", "Notice", "Revive", "ARK", "Hilla", "Cygnus", "VL", "CHT", "HT", "PB", "Czak", "Zakum", "Spawn", "RB", "TDrops", "WhatsMyIP",
"BuffItem", "BuffItemEX", "ItemSize", "Respawn", "BanIP", "TempBanIP", "RemoveNPCs", "SpeakMega",
 "ResetMap", "ReloadEvents", "ReloadShops", "ReloadPortals", "ReloadOPS", "GainVP", "GainP", "GainCash", "GainMeso", "GetPokemon", "ClearPokemon", "SeePokeDex,", "FillPokeDex", "ListBook", "FillBook", "PMOB", "Rev", "Crash", 
 "TestTimer", "PTS", "Packet",
 "SpeakCHN", "SpeakMap", "Speak", "Say", "ItemVac", "Vac", "Drop", "UnlockInv", "UnbanIP", "ReloadCustomLife", "Save", "unban", "UnHellBan", "HellBan", "GMNPC", "MaxSkills", "Song", "Clock", "Warn", "HotTime", "WarpAllHere", "PermWeather", "CheckVPoint",
  "Jobperson", "StunMap", "DiseaseMap", "Lottery", "PrizeMap", "TrollVac", "Maze", "MapleStory",  "TagKill",  
 "KillMonsterByOID", "ResetMobs", "WhosThere", "DisposeClones", "CloneME", "Disease", "Killmap", "LockItem", "RemoveItem", "Event", "SetEvent", "StartAutoEvent", "Level", "GetItem", "LevelUp", "Shop", "Job", "SP", "GetSkill", "WarpOXTop", "WarpOX", "WarpOXRight", "WarpOXMiddle", "ClearDrops", 
"immune",
"bomb",
"listslot",
"CharInfo",
"Reports",
"ClearReports",
"Cheaters",
"NearestPortal",
"SpawnDebug",
"LookNPC",
"LookReactor",
"LookPortals",
"MyPos",
"Clock",
"Letter",
"SendAllNote",
"ResetReactor",
"CompleteQuest",
"StartQuest",
"ResetQuest"};
            ArrayList<String> commandsList = new ArrayList<>(Arrays.asList(commandsArray));
            commandsList.add("you can add commands here");//for commands not accessible to everyone

            int currPage = splitted.length >=2 ? Integer.valueOf(splitted[1]) : 1;
            int pageLen = 5;//length of each page in lines
            int totalPages = (commandsList.size() / pageLen )> 0 ? commandsList.size() / pageLen : 1;
            int min = splitted.length >= 1 ? pageLen * (currPage - 1) : 0;
            int max = splitted.length < 2 ? pageLen : currPage == totalPages ? commandsList.size() : pageLen * currPage;

                   

            if (totalPages >= currPage) {
                c.getPlayer().dropMessage(5, "==AlmightyMS Commands(!Help For Full Array): Page " + currPage + "==");
                for (int i = min; i < max; i++) {
                    c.getPlayer().dropMessage(5, p + commandsList.get(i));
                }
                if (totalPages > currPage) {
                    c.getPlayer().dropMessage(5, "More commands! [Please type " + p + "commands " + (currPage + 1) + " for another page of commands (:]");
                }
            } else {//page number is too high
                c.getPlayer().dropMessage(5, "Please type " + p + "commands <#>, where # = 1 to " + totalPages + ".");
            }
            return 1;
        }
    }  
     
public static class g extends CommandExecute {
        public int execute(MapleClient c,String[] splitted) {
            if (c.getPlayer().isIntern()) {
            for(final ChannelServer cserv : ChannelServer.getAllInstances()) {
                 cserv.broadcastGMMessage(tools.packet.CField.multiChat(">>[AlmightyStaffChat] " + c.getPlayer().getName()," " + StringUtil.joinStringFrom(splitted, 1), 6));
            }
            }
            return 1;
        }
        }

 public static class Jail extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "jail [name] [minutes, 0 = forever]");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            final int minutes = Math.max(0, Integer.parseInt(splitted[2]));
            if (victim.getGMLevel() >= 6) {
                c.getPlayer().getStat().setHp((short) 0, c.getPlayer());
                c.getPlayer().getStat().setMp((short) 0, c.getPlayer());
                c.getPlayer().updateSingleStat(MapleStat.HP, 0);
                c.getPlayer().updateSingleStat(MapleStat.MP, 0);
                c.getPlayer().setMap(922900000);
                c.getPlayer().dropMessage(1, "Nice try.");
                c.getPlayer().dropMessage(6, "That character is an ADMIN. You are now a banana now. Fucking punk.");
            }
             
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(GameConstants.JAIL);
                victim.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData(String.valueOf(minutes * 60));
                victim.changeMap(target, target.getPortal(0));
            } else {
                c.getPlayer().dropMessage(6, "Please be on their channel.");
                return 0;
            }
            return 1;
        }
    }
    public static class UnJail extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]); 
            int mapid = 100000000; //Henesys 
            if (splitted.length > 2 && splitted[1].equals("2")) { 
                mapid = 922900000; 
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]); 
            } 
            if (victim != null) { 
                MapleMap target = c.getPlayer().getMap();
                MaplePortal targetPortal = target.getPortal(0);
                victim.changeMap(100000000);
 World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, victim.getName() + " was un-jailed. We have our eyes on you so you better be good!")); 
                 
            } else { 
                c.getPlayer().dropMessage(6, "Player " + splitted[1] + " not found.");
            } 
            
            return 0;
             
            
        }
    }

 public static class Emotion extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(0, splitted[0] + " [victim] [emote] [duration]");
                return 0;
            }
            victim.getMap().broadcastMessage(victim, CField.facialExpression2(Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3])), false);
            return 1;
        }
    }
 

  
  public static class WarpAllHere extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (mch.getMapId() != c.getPlayer().getMapId()) {
                    mch.changeMap(c.getPlayer().getMap(), c.getPlayer().getPosition());
                }
            }
            return 1;
        }
    }
  
   
    
       public static class warn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(0, "[Syntax] !warn [name] [reason]");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            String reason = StringUtil.joinStringFrom(splitted, 2);
            //victim.giveWarning(victim, reason);
           // victim.getMap().startMapEffect(victim.getName() + " has been warned for " + reason + ". Warnings: " + victim.getWarnings(victim) + "/3", 5120041);
            victim.getMap().startMapEffect(victim.getName() + " has been warned for " + reason + ".", 5120041);
            return 1;
        }
    }
       
        public static class Clock extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().broadcastMessage(CField.getClock(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 60)));
            return 1;
        }
    }
        
          public static class Song extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().broadcastMessage(CField.musicChange(splitted[1]));
            return 1;
        }
    }
          

     
         public static class WarpHere extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim.getGMLevel() == 7) {
                c.getPlayer().getStat().setHp((short) 0, c.getPlayer());
                c.getPlayer().getStat().setMp((short) 0, c.getPlayer());
                c.getPlayer().updateSingleStat(MapleStat.HP, 0);
                c.getPlayer().updateSingleStat(MapleStat.MP, 0);
                c.getPlayer().dropMessage(6, "Can't warp me out of my trap house, ho!");
                return 0;
            }
            if (victim != null) {
                if (c.getPlayer().inPVP() || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "Try again later.");
                    return 0;
                }
                victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
            } else {
                int ch = World.Find.findChannel(splitted[1]);
                if (ch < 0) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return 0;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null || victim.inPVP() || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "Try again later.");
                    return 0;
                }
                c.getPlayer().dropMessage(5, "Victim is cross changing channel.");
                victim.dropMessage(5, "Cross changing channel.");
                if (victim.getMapId() != c.getPlayer().getMapId()) {
                    final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                    victim.changeMap(mapp, mapp.findClosestPortal(c.getPlayer().getTruePosition()));
                }
                victim.changeChannel(c.getChannel());
            }
            return 1;
        }
    }

    public static class Warp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel() && !victim.inPVP() && !c.getPlayer().inPVP()) {
                if (splitted.length == 2) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getTruePosition()));
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "Map does not exist");
                        return 0;
                    }
                    MaplePortal targetPortal = null;
                    if (splitted.length > 3) {
                        try {
                            targetPortal = target.getPortal(Integer.parseInt(splitted[3]));
                        } catch (IndexOutOfBoundsException e) {
                            // noop, assume the gm didn't know how many portals there are
                            c.getPlayer().dropMessage(5, "Invalid portal selected.");
                        } catch (NumberFormatException a) {
                            // noop, assume that the gm is drunk
                        }
                    }
                    if (targetPortal == null) {
                        targetPortal = target.getPortal(0);
                    }
                    victim.changeMap(target, targetPortal);
                }
            } else {
                try {
                    victim = c.getPlayer();
                    int ch = World.Find.findChannel(splitted[1]);
                    if (ch < 0) {
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        if (target == null) {
                            c.getPlayer().dropMessage(6, "Map does not exist");
                            return 0;
                        }
                        MaplePortal targetPortal = null;
                        if (splitted.length > 2) {
                            try {
                                targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                            } catch (IndexOutOfBoundsException e) {
                                // noop, assume the gm didn't know how many portals there are
                                c.getPlayer().dropMessage(5, "Invalid portal selected.");
                            } catch (NumberFormatException a) {
                                // noop, assume that the gm is drunk
                            }
                        }
                        if (targetPortal == null) {
                            targetPortal = target.getPortal(0);
                        }
                        c.getPlayer().changeMap(target, targetPortal);
                    } else {
                        victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().dropMessage(6, "Cross changing channel. Please wait.");
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                            c.getPlayer().changeMap(mapp, mapp.findClosestPortal(victim.getTruePosition()));
                        }
                        c.getPlayer().changeChannel(ch);
                    }
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "Something went wrong " + e.getMessage());
                    return 0;
                }
            }
            return 1;
        }
    }
    
     public static class StartEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleEvent.onStartEvent(c.getPlayer());
            return 1;
        }
    }

    public static class FinishEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleEvent.finishEvent(c.getPlayer());
            return 1;
        }
    }
    
      public static class CloseEntry extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (c.getPlayer().getClient().getChannelServer().eventOn == true && c.getPlayer().getClient().getChannelServer().eventClosed == false) {

               c.getPlayer().getClient().getChannelServer().eventClosed = true;
                    World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, c.getChannel(), "[Event] The event has been closed. No more participants allowed to join."));
            }
            return 1;
        }
        }
        
        public static class CloseEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (c.getPlayer().getClient().getChannelServer().eventOn == true && c.getPlayer().getClient().getChannelServer().eventClosed == true) {

                c.getPlayer().getClient().getChannelServer().eventOn = false;
                    World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, c.getChannel(), "[Event] The event has ended. Thanks to all of those who participated."));
            }
            return 1;
        }
        }
    
        public static class OpenEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

            if (c.getPlayer().getClient().getChannelServer().eventOn == false) {
                int mapid = CommandProcessorUtil.getOptionalIntArg(splitted, 1, c.getPlayer().getMapId());
                c.getPlayer().getClient().getChannelServer().eventOn = true;
                c.getPlayer().getClient().getChannelServer().eventClosed = false;
                c.getPlayer().getClient().getChannelServer().eventMap = mapid;
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, c.getChannel(), "[Event] The event has started in Channel " + c.getChannel() + " in " + c.getPlayer().getMapId() + "! Use @event to join it. Note: You must be in Channel " + c.getChannel() + " for it to work."));
              //  c.getPlayer().setEventDone(1);   
            } else {
               c.getPlayer().dropMessage(5, "An event is going on already...");
            }
            return 1;
        }
        }
    
     public static class Usain extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {

//dirty af pls no flame :(
     for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    if (!cserv.getRace() && !cserv.getWaiting()){
            cserv.setWaiting(true);
            cserv.setWaitingTime(2); //Replace with time in minutes you want to wait.
            cserv.raceCountdown();
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Event]: Usain Bolt Race will begin soon! Please head to Leafre!"));
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "Use @joinrace to join and @rules to see the rules and regulations of this event."));
                 }else{
                   c.getPlayer().dropMessage(6, "[Notice]: A race is still in progress.");
                 }
                    
     }
    
            return 1;
   
                
                }
     }
            
     
} //wtf das all man?!!
    

 

    

