/*      */ package tools.packet;
/*      */ 
/*      */ import client.BuddyList;
/*      */ import client.MapleCharacter;
           import client.InnerSkillValueHolder;
import client.MapleBuffStat;
/*      */ import client.MapleClient;
/*      */ import client.MapleCoolDownValueHolder;
/*      */ import client.MapleQuestStatus;
/*      */ import client.MapleTrait;
/*      */ import client.MapleTrait.MapleTraitType;
/*      */ import client.MonsterBook;
/*      */ import client.PlayerStats;
/*      */ import client.Skill;
/*      */ import client.SkillEntry;
/*      */ import client.inventory.Equip;
/*      */ import client.inventory.Item;
/*      */ import client.inventory.MapleInventory;
/*      */ import client.inventory.MapleInventoryType;
/*      */ import client.inventory.MaplePet;
/*      */ import client.inventory.MapleRing;
/*      */ import constants.GameConstants;
/*      */ import handling.Buffstat;
/*      */ import handling.world.MapleCharacterLook;
/*      */ import java.util.ArrayList;
import java.util.Arrays;
/*      */ import java.util.Collection;
/*      */ import java.util.Collections;
/*      */ import java.util.Date;
/*      */ import java.util.Iterator;
/*      */ import java.util.LinkedHashMap;
/*      */ import java.util.List;
/*      */ import java.util.Map;
/*      */ import java.util.Map.Entry;
/*      */ import java.util.SimpleTimeZone;
/*      */ import java.util.TimeZone;
/*      */ import server.MapleItemInformationProvider;
/*      */ import server.MapleShop;
/*      */ import server.MapleShopItem;
/*      */ import server.movement.LifeMovementFragment;
/*      */ import server.quest.MapleQuest;
/*      */ import server.shops.AbstractPlayerStore;
/*      */ import server.shops.IMaplePlayerShop;
/*      */ import tools.BitTools;
/*      */ import tools.Pair;
/*      */ import tools.StringUtil;
/*      */ import tools.Triple;
/*      */ import tools.data.MaplePacketLittleEndianWriter;
/*      */ 
/*      */ public class PacketHelper
/*      */ {
/*      */   public static final long FT_UT_OFFSET = 116444592000000000L;
/*      */   public static final long MAX_TIME = 150842304000000000L;
/*      */   public static final long ZERO_TIME = 94354848000000000L;
/*      */   public static final long PERMANENT = 150841440000000000L;
/*      */ 
/*      */   public static final long getKoreanTimestamp(long realTimestamp)
/*      */   {
/*   70 */     return getTime(realTimestamp);
/*      */   }
/*      */ 
/*      */   public static final long getTime(long realTimestamp) {
/*   74 */     if (realTimestamp == -1L)
/*   75 */       return 150842304000000000L;
/*   76 */     if (realTimestamp == -2L)
/*   77 */       return 94354848000000000L;
/*   78 */     if (realTimestamp == -3L) {
/*   79 */       return 150841440000000000L;
/*      */     }
/*   81 */     return realTimestamp * 10000L + 116444592000000000L;
/*      */   }
/*      */ 
/*      */   public static long getFileTimestamp(long timeStampinMillis, boolean roundToMinutes) {
/*   85 */     if (SimpleTimeZone.getDefault().inDaylightTime(new Date()))
/*   86 */       timeStampinMillis -= 3600000L;
/*      */     long time;

/*   89 */     if (roundToMinutes)
/*   90 */       time = timeStampinMillis / 1000L / 60L * 600000000L;
/*      */     else {
/*   92 */       time = timeStampinMillis * 10000L;
/*      */     }
/*   94 */     return time + 116444592000000000L;
/*      */   }
/*      */ 
/*      */   public static void addQuestInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
/*   98 */     boolean idk = true;
/*      */ 
/*  137 */     final List<MapleQuestStatus> started = chr.getStartedQuests();
/*  138 */     mplew.write(1);
/*      */ 
/*  140 */     mplew.writeShort(started.size());
/*  141 */      for (final MapleQuestStatus q : started) {
/*  142 */       mplew.writeShort(q.getQuest().getId());
/*  143 */       if (q.hasMobKills()) {
/*  144 */         StringBuilder sb = new StringBuilder();
/*  145 */         for (Iterator i$ = q.getMobKills().values().iterator(); i$.hasNext(); ) { int kills = ((Integer)i$.next()).intValue();
/*  146 */           sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
/*      */         }
/*  148 */         mplew.writeMapleAsciiString(sb.toString());
/*      */       } else {
/*  150 */         mplew.writeMapleAsciiString(q.getCustomData() == null ? "" : q.getCustomData());
/*      */       }
/*      */ 
/*      */     }
/*      */ 
/*  157 */     mplew.writeShort(0);
/*      */ 
/*  160 */     mplew.write(1);
/*      */ 
/*  182 */      final List<MapleQuestStatus> completed = chr.getCompletedQuests();
/*  183 */     mplew.writeShort(completed.size());
/*  184 */     for (MapleQuestStatus q : completed) {
/*  185 */       mplew.writeShort(q.getQuest().getId());
/*  186 */       mplew.writeLong(getTime(q.getCompletionTime()));
/*      */     }
/*      */   }
/*      */ 
/*      */   public static final void addSkillInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr)
/*      */   {
        final Map<Skill, SkillEntry> skills = chr.getSkills();
/*  195 */     boolean useOld = skills.size() < 500;
/*      */ 
/*  210 */     mplew.write(useOld ? 1 : 0);
/*  211 */     if (useOld) {
/*  212 */       mplew.writeShort(skills.size());
/*  213 */       for (Map.Entry skill : skills.entrySet()) {
/*  214 */         mplew.writeInt(((Skill)skill.getKey()).getId());
/*  215 */         mplew.writeInt(((SkillEntry)skill.getValue()).skillevel);
/*  216 */         addExpirationTime(mplew, ((SkillEntry)skill.getValue()).expiration);
/*      */ 
/*  218 */         if (((Skill)skill.getKey()).isFourthJob())
/*  219 */           mplew.writeInt(((SkillEntry)skill.getValue()).masterlevel);
/*      */       }
/*      */     }
/*      */     else {
       final Map<Integer, Integer> skillsWithoutMax = new LinkedHashMap<>();
            final Map<Integer, Long> skillsWithExpiration = new LinkedHashMap<>();
            final Map<Integer, Byte> skillsWithMax = new LinkedHashMap<>();
/*      */ 
/*  228 */        for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
/*  229 */         skillsWithoutMax.put(Integer.valueOf(((Skill)skill.getKey()).getId()), Integer.valueOf(((SkillEntry)skill.getValue()).skillevel));
/*  230 */         if (((SkillEntry)skill.getValue()).expiration > 0L) {
/*  231 */           skillsWithExpiration.put(Integer.valueOf(((Skill)skill.getKey()).getId()), Long.valueOf(((SkillEntry)skill.getValue()).expiration));
/*      */         }
/*  233 */         if (((Skill)skill.getKey()).isFourthJob()) {
/*  234 */           skillsWithMax.put(Integer.valueOf(((Skill)skill.getKey()).getId()), Byte.valueOf(((SkillEntry)skill.getValue()).masterlevel));
/*      */         }
/*      */       }
/*      */ 
/*  238 */       int amount = skillsWithoutMax.size();
/*  239 */       mplew.writeShort(amount);
/*  240 */       for (final Entry<Integer, Integer> x : skillsWithoutMax.entrySet()) {
/*  241 */         mplew.writeInt(((Integer)x.getKey()).intValue());
/*  242 */         mplew.writeInt(((Integer)x.getValue()).intValue());
/*      */       }
/*  244 */       mplew.writeShort(0);
/*      */ 
/*  246 */       amount = skillsWithExpiration.size();
/*  247 */       mplew.writeShort(amount);
/*  248 */           for (final Entry<Integer, Long> x : skillsWithExpiration.entrySet()) {
/*  249 */         mplew.writeInt(((Integer)x.getKey()).intValue());
/*  250 */         mplew.writeLong(((Long)x.getValue()).longValue());
/*      */       }
/*  252 */       mplew.writeShort(0);
/*      */ 
/*  254 */       amount = skillsWithMax.size();
/*  255 */       mplew.writeShort(amount);
/*  256 */          for (final Entry<Integer, Byte> x : skillsWithMax.entrySet()) {
/*  257 */         mplew.writeInt(((Integer)x.getKey()).intValue());
/*  258 */         mplew.writeInt(((Byte)x.getValue()).byteValue());
/*      */       }
/*  260 */       mplew.writeShort(0);
/*      */     }
/*      */   }
/*      */ 
/*      */   public static final void addCoolDownInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
/*  265 */      final List<MapleCoolDownValueHolder> cd = chr.getCooldowns();
/*      */ 
/*  267 */     mplew.writeShort(cd.size());
/*  268 */     for (MapleCoolDownValueHolder cooling : cd) {
/*  269 */       mplew.writeInt(cooling.skillId);
/*  270 */       mplew.writeInt((int)(cooling.length + cooling.startTime - System.currentTimeMillis()) / 1000);
/*      */     }
/*      */   }
/*      */ 
/*      */   public static final void addRocksInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
/*  275 */     int[] mapz = chr.getRegRocks();
/*  276 */     for (int i = 0; i < 5; i++) {
/*  277 */       mplew.writeInt(mapz[i]);
/*      */     }
/*      */ 
/*  280 */     int[] map = chr.getRocks();
/*  281 */     for (int i = 0; i < 10; i++) {
/*  282 */       mplew.writeInt(map[i]);
/*      */     }
/*      */ 
/*  285 */     int[] maps = chr.getHyperRocks();
/*  286 */     for (int i = 0; i < 13; i++) {
/*  287 */       mplew.writeInt(maps[i]);
/*      */     }
/*  289 */     for (int i = 0; i < 13; i++)
/*  290 */       mplew.writeInt(maps[i]);
/*      */   }
/*      */ 
/*      */   public static final void addRingInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr)
/*      */   {
/*  295 */     mplew.writeShort(0);
/*      */ 
/*  304 */     Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> aRing = chr.getRings(true);
/*  305 */     List<MapleRing> cRing = aRing.getLeft();
/*  306 */     mplew.writeShort(cRing.size());
/*  307 */     for (MapleRing ring : cRing) {
/*  308 */       mplew.writeInt(ring.getPartnerChrId());
/*  309 */       mplew.writeAsciiString(ring.getPartnerName(), 13);
/*  310 */       mplew.writeLong(ring.getRingId());
/*  311 */       mplew.writeLong(ring.getPartnerRingId());
/*      */     }
/*  313 */     List<MapleRing> fRing = aRing.getMid();
/*  314 */     mplew.writeShort(fRing.size());
/*  315 */     for (MapleRing ring : fRing) {
/*  316 */       mplew.writeInt(ring.getPartnerChrId());
/*  317 */       mplew.writeAsciiString(ring.getPartnerName(), 13);
/*  318 */       mplew.writeLong(ring.getRingId());
/*  319 */       mplew.writeLong(ring.getPartnerRingId());
/*  320 */       mplew.writeInt(ring.getItemId());
/*      */     }
/*  322 */      List<MapleRing> mRing = aRing.getRight();
/*  323 */     mplew.writeShort(mRing.size());
/*  324 */     int marriageId = 30000;
/*  325 */     for (MapleRing ring : mRing) {
/*  326 */       mplew.writeInt(marriageId);
/*  327 */       mplew.writeInt(chr.getId());
/*  328 */       mplew.writeInt(ring.getPartnerChrId());
/*  329 */       mplew.writeShort(3);
/*  330 */       mplew.writeInt(ring.getItemId());
/*  331 */       mplew.writeInt(ring.getItemId());
/*  332 */       mplew.writeAsciiString(chr.getName(), 13);
/*  333 */       mplew.writeAsciiString(ring.getPartnerName(), 13);
/*      */     }
/*      */   }
/*      */ 
 public static void addInventoryInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.writeInt(chr.getMeso()); // mesos
        mplew.writeInt(0); // 4 ints per size
        mplew.write(chr.getInventory(MapleInventoryType.EQUIP).getSlotLimit()); // equip slots
        mplew.write(chr.getInventory(MapleInventoryType.USE).getSlotLimit()); // use slots
        mplew.write(chr.getInventory(MapleInventoryType.SETUP).getSlotLimit()); // set-up slots
        mplew.write(chr.getInventory(MapleInventoryType.ETC).getSlotLimit()); // etc slots
        mplew.write(chr.getInventory(MapleInventoryType.CASH).getSlotLimit()); // cash slots

        final MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)); // 0x200000 : int + int actually
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()) {
            mplew.writeLong(getTime(Long.parseLong(stat.getCustomData())));
        } else {
            mplew.writeLong(getTime(-2));
        }
        MapleInventory iv = chr.getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        for (Item item : equipped) {
            if (item.getPosition() < 0 && item.getPosition() > -100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of equipped nx
        for (Item item : equipped) {
            if (item.getPosition() <= -100 && item.getPosition() > -1000) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of equip inventory
        iv = chr.getInventory(MapleInventoryType.EQUIP);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.writeShort(0); //start of evan equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1000 && item.getPosition() > -1100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); //start of mechanic equips, ty KDMS
        for (Item item : equipped) {
            if (item.getPosition() <= -1100 && item.getPosition() > -1200) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of android equips
        for (Item item : equipped) {
            if (item.getPosition() <= -1200) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        
        mplew.writeShort(0); // start of totem inventory
        for (Item item : equipped) {
            if ((item.getPosition() <= -5000 && item.getPosition() >= -5003)) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.writeShort(0); // start of use inventory
        iv = chr.getInventory(MapleInventoryType.USE);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of set-up inventory
        iv = chr.getInventory(MapleInventoryType.SETUP);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of etc inventory
        iv = chr.getInventory(MapleInventoryType.ETC);
        for (Item item : iv.list()) {
            if (item.getPosition() < 100) {
                addItemPosition(mplew, item, false, false);
                addItemInfo(mplew, item, chr);
            }
        }
        mplew.write(0); // start of cash inventory
        iv = chr.getInventory(MapleInventoryType.CASH);
        for (Item item : iv.list()) {
            addItemPosition(mplew, item, false, false);
            addItemInfo(mplew, item, chr);
        }
        mplew.write(0); // start of extended slots
        for (int i = 0; i < chr.getExtendedSlots().size(); i++) {
            mplew.writeInt(i);
            mplew.writeInt(chr.getExtendedSlot(i));
            for (Item item : chr.getInventory(MapleInventoryType.ETC).list()) {
                if (item.getPosition() > (i * 100 + 100) && item.getPosition() < (i * 100 + 200)) {
                    addItemPosition(mplew, item, false, true);
                    addItemInfo(mplew, item, chr);
                }
            }
            mplew.writeInt(-1);
        }
        mplew.writeInt(-1);
        mplew.writeInt(0); // 0x40000000 Foreach : Int + Long
        mplew.writeInt(0); // 0x400 Foreach : Long + Long
        mplew.write(0); // 0x20000000 if got, then below
		/*mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);		
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeLong(0);
        
        mplew.write(0); // a boolean
         */
    }
/*      */ 
/*      */   public static final void addCharStats(MaplePacketLittleEndianWriter mplew, MapleCharacter chr)
/*      */   {
/*  497 */     mplew.writeInt(chr.getId());
/*  498 */     mplew.writeAsciiString(chr.getName(), 13);
/*  499 */     mplew.write(chr.getGender());
/*  500 */     mplew.write(chr.getSkinColor());
/*  501 */     mplew.writeInt(chr.getFace());
/*  502 */     mplew.writeInt(chr.getHair());
/*  503 */     mplew.writeZeroBytes(24);
/*  504 */     mplew.write(chr.getLevel());
/*  505 */     mplew.writeShort(chr.getJob());
/*  506 */     chr.getStat().connectData(mplew);
/*  507 */     mplew.writeShort(chr.getRemainingAp());
/*  508 */     if (GameConstants.isSeparatedSp(chr.getJob())) {
/*  509 */       int size = chr.getRemainingSpSize();
/*  510 */       mplew.write(size);
/*  511 */       for (int i = 0; i < chr.getRemainingSps().length; i++)
/*  512 */         if (chr.getRemainingSp(i) > 0) {
/*  513 */           mplew.write(i + 1);
/*  514 */           mplew.write(chr.getRemainingSp(i));
/*      */         }
/*      */     }
/*      */     else {
/*  518 */       mplew.writeShort(chr.getRemainingSp());
/*      */     }
/*  520 */     mplew.writeInt(chr.getExp());
/*  521 */     mplew.writeInt(chr.getFame());
/*  522 */     mplew.writeInt(chr.getGachExp());
/*  523 */     mplew.writeInt(chr.getMapId());
/*  524 */     mplew.write(chr.getInitialSpawnpoint());
/*  525 */     mplew.writeInt(0);
/*  526 */     mplew.writeShort(chr.getSubcategory());
/*  527 */     if (GameConstants.isDemon(chr.getJob())) {
/*  528 */       mplew.writeInt(chr.getDemonMarking());
/*      */     }
/*  530 */     mplew.write(chr.getFatigue());
/*  531 */     mplew.writeInt(GameConstants.getCurrentDate());
/*  532 */     for (MapleTrait.MapleTraitType t : MapleTrait.MapleTraitType.values()) {
/*  533 */       mplew.writeInt(chr.getTrait(t).getTotalExp());
/*      */     }
/*  535 */     for (MapleTrait.MapleTraitType t : MapleTrait.MapleTraitType.values()) {
/*  536 */       mplew.writeShort(0);
/*      */     }
/*      */ 
/*  580 */     mplew.write(0);
/*  581 */     mplew.writeInt(-35635200);
/*  582 */     mplew.writeInt(21968699);
/*  583 */     mplew.writeInt(chr.getStat().pvpExp);
/*  584 */     mplew.write(chr.getStat().pvpRank);
/*  585 */     mplew.writeInt(chr.getBattlePoints());
/*  586 */     mplew.write(5);
/*  587 */     mplew.writeInt(0);
/*      */ 
/*  590 */     mplew.write(0);
/*  591 */     mplew.write(new byte[] { 59, 55, 79, 1, 0, 64, -32, -3 });
/*  592 */     mplew.writeShort(0);
/*  593 */     mplew.writeZeroBytes(3);
/*      */ 
/*  595 */     for (int i = 0; i < 6; i++)
/*      */     {
/*  597 */       mplew.writeZeroBytes(9);
/*      */     }
/*      */ 
/*  605 */     mplew.writeInt(-1778686763);
/*  606 */     mplew.writeInt(311);
/*      */   }
/*      */ 
/*      */   public static final void addCharLook(MaplePacketLittleEndianWriter mplew, MapleCharacterLook chr, boolean mega) {
/*  610 */     mplew.write(chr.getGender());
/*  611 */     mplew.write(chr.getSkinColor());
/*  612 */     mplew.writeInt(chr.getFace());
/*  613 */     mplew.writeInt(chr.getJob());
/*  614 */     mplew.write(mega ? 0 : 1);
/*  615 */     mplew.writeInt(chr.getHair());
/*      */ 
/*  617 */       final Map<Byte, Integer> myEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> maskedEquip = new LinkedHashMap<>();
        final Map<Byte, Integer> equip = chr.getEquips();
        final Map<Byte, Integer> totem = new LinkedHashMap<>();
        for (final Entry<Byte, Integer> item : equip.entrySet()) {
            if (item.getKey() < -127) { //not visible
                continue;
            }
            byte pos = (byte) (item.getKey() * -1);
            
            if (pos <= -118 && pos >= -120) {
                pos = (byte) (pos + 118);
                totem.put(pos, item.getValue());
            } else if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, item.getValue());
            } else if (pos > 100 && pos != 111) {
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, item.getValue());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, item.getValue());
            }
        }
        for (final Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // end of visible itens
        for (final Entry<Byte, Integer> entry : totem.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // end of totem
        // masked itens
        for (final Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF); // ending markers
/*  666 */     final Integer cWeapon = equip.get((byte) -111);
               mplew.writeInt(cWeapon != null ? cWeapon : 0);
/*  668 */     mplew.write(GameConstants.isMercedes(chr.getJob()) ? 1 : 0);
/*  669 */     mplew.writeZeroBytes(12); //petsz
/*  670 */     if (GameConstants.isDemon(chr.getJob()))
/*  671 */       mplew.writeInt(chr.getDemonMarking());
/*      */   }
/*      */ 
/*      */   public static final void addExpirationTime(MaplePacketLittleEndianWriter mplew, long time)
/*      */   {
/*  676 */     mplew.writeLong(getTime(time));
/*      */   }
/*      */ 
/*      */   public static void addItemPosition(MaplePacketLittleEndianWriter mplew, Item item, boolean trade, boolean bagSlot) {
/*  680 */     if (item == null) {
/*  681 */       mplew.write(0);
/*  682 */       return;
/*      */     }
/*  684 */     short pos = item.getPosition();
/*  685 */     if (pos <= -1) {
/*  686 */       pos = (short)(pos * -1);
/*  687 */       if ((pos > 100) && (pos < 1000)) {
/*  688 */         pos = (short)(pos - 100);
/*      */       }
/*      */     }
/*  691 */     if (bagSlot)
/*  692 */       mplew.writeInt(pos % 100 - 1);
/*  693 */     else if ((!trade) && (item.getType() == 1))
/*  694 */       mplew.writeShort(pos);
/*      */     else
/*  696 */       mplew.write(pos);
/*      */   }
/*      */ 
/*      */   public static final void addItemInfo(MaplePacketLittleEndianWriter mplew, Item item)
/*      */   {
/*  701 */     addItemInfo(mplew, item, null);
/*      */   }
/*      */ 
/*      */   public static final void addItemInfo(MaplePacketLittleEndianWriter mplew, Item item, MapleCharacter chr) {
/*  705 */     mplew.write(item.getPet() != null ? 3 : item.getType());
/*  706 */     mplew.writeInt(item.getItemId());
/*  707 */     boolean hasUniqueId = (item.getUniqueId() > 0) && (!GameConstants.isMarriageRing(item.getItemId())) && (item.getItemId() / 10000 != 166);
/*      */ 
/*  709 */     mplew.write(hasUniqueId ? 1 : 0);
/*  710 */     if (hasUniqueId) {
/*  711 */       mplew.writeLong(item.getUniqueId());
/*      */     }
/*  713 */     if (item.getPet() != null) {
/*  714 */       addPetItemInfo(mplew, item, item.getPet(), true);
/*      */     } else {
/*  716 */       addExpirationTime(mplew, item.getExpiration());
/*  717 */       mplew.writeInt(chr == null ? -1 : chr.getExtendedSlots().indexOf(Integer.valueOf(item.getItemId())));
/*  718 */       if (item.getType() == 1) {
/*  719 */         Equip equip = (Equip)item;
/*  720 */         mplew.write(equip.getUpgradeSlots());
/*  721 */         mplew.write(equip.getLevel());
/*  722 */         mplew.writeShort(equip.getStr());
/*  723 */         mplew.writeShort(equip.getDex());
/*  724 */         mplew.writeShort(equip.getInt());
/*  725 */         mplew.writeShort(equip.getLuk());
/*  726 */         mplew.writeShort(equip.getHp());
/*  727 */         mplew.writeShort(equip.getMp());
/*  728 */         mplew.writeShort(equip.getWatk());
/*  729 */         mplew.writeShort(equip.getMatk());
/*  730 */         mplew.writeShort(equip.getWdef());
/*  731 */         mplew.writeShort(equip.getMdef());
/*  732 */         mplew.writeShort(equip.getAcc());
/*  733 */         mplew.writeShort(equip.getAvoid());
/*  734 */         mplew.writeShort(equip.getHands());
/*  735 */         mplew.writeShort(equip.getSpeed());
/*  736 */         mplew.writeShort(equip.getJump());
/*  737 */         mplew.writeMapleAsciiString(equip.getOwner());
/*  738 */         mplew.writeShort(equip.getFlag());
/*  739 */         mplew.write(equip.getIncSkill() > 0 ? 1 : 0);
/*  740 */         mplew.write(Math.max(equip.getBaseLevel(), equip.getEquipLevel()));
/*  741 */         mplew.writeInt(equip.getExpPercentage() * 100000);
/*  742 */         mplew.writeInt(equip.getDurability());
/*  743 */         mplew.writeInt(equip.getViciousHammer());
/*  744 */         mplew.writeShort(equip.getPVPDamage());
/*  745 */         mplew.write(equip.getState());
/*  746 */         mplew.write(equip.getEnhance());
/*  747 */         mplew.writeShort(equip.getPotential1());
/*  748 */         if (!hasUniqueId) {
/*  749 */           mplew.writeShort(equip.getPotential2());
/*  750 */           mplew.writeShort(equip.getPotential3());
/*  751 */           mplew.writeShort(equip.getPotential4());
/*  752 */           mplew.writeShort(equip.getPotential5());
/*      */         }
/*  754 */         mplew.writeShort(equip.getSocketState());
/*  755 */         mplew.writeShort(equip.getSocket1() % 10000);
/*  756 */         mplew.writeShort(equip.getSocket2() % 10000);
/*  757 */         mplew.writeShort(equip.getSocket3() % 10000);
/*  758 */         mplew.writeLong(equip.getInventoryId() <= 0L ? -1L : equip.getInventoryId());
/*  759 */         mplew.writeLong(getTime(-2L));
/*  760 */         mplew.writeInt(-1);
/*      */       } else {
/*  762 */         mplew.writeShort(item.getQuantity());
/*  763 */         mplew.writeMapleAsciiString(item.getOwner());
/*  764 */         mplew.writeShort(item.getFlag());
/*  765 */         if ((GameConstants.isThrowingStar(item.getItemId())) || (GameConstants.isBullet(item.getItemId())) || (item.getItemId() / 10000 == 287))
/*  766 */           mplew.writeLong(item.getInventoryId() <= 0L ? -1L : item.getInventoryId());
/*      */       }
/*      */     }
/*      */   }
/*      */ 
/*      */   public static final void serializeMovementList(MaplePacketLittleEndianWriter lew, List<LifeMovementFragment> moves)
/*      */   {
/*  773 */     lew.write(moves.size());
/*  774 */     for (LifeMovementFragment move : moves)
/*  775 */       move.serialize(lew);
/*      */   }
/*      */ 
/*      */   public static final void addAnnounceBox(MaplePacketLittleEndianWriter mplew, MapleCharacter chr)
/*      */   {
/*  780 */     if ((chr.getPlayerShop() != null) && (chr.getPlayerShop().isOwner(chr)) && (chr.getPlayerShop().getShopType() != 1) && (chr.getPlayerShop().isAvailable()))
/*  781 */       addInteraction(mplew, chr.getPlayerShop());
/*      */     else
/*  783 */       mplew.write(0);
/*      */   }
/*      */ 
/*      */   public static final void addInteraction(MaplePacketLittleEndianWriter mplew, IMaplePlayerShop shop)
/*      */   {
/*  788 */     mplew.write(shop.getGameType());
/*  789 */     mplew.writeInt(((AbstractPlayerStore)shop).getObjectId());
/*  790 */     mplew.writeMapleAsciiString(shop.getDescription());
/*  791 */     if (shop.getShopType() != 1) {
/*  792 */       mplew.write(shop.getPassword().length() > 0 ? 1 : 0);
/*      */     }
/*  794 */     mplew.write(shop.getItemId() % 10);
/*  795 */     mplew.write(shop.getSize());
/*  796 */     mplew.write(shop.getMaxSize());
/*  797 */     if (shop.getShopType() != 1)
/*  798 */       mplew.write(shop.isOpen() ? 0 : 1);
/*      */   }
/*      */ 
/*      */      public static final void addCharacterInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr)
/*      */   {
/*  803 */     mplew.writeInt(-1);
/*  804 */     mplew.writeInt(-3);
/*      */ 
/*  812 */     mplew.writeZeroBytes(8);
/*  813 */     addCharStats(mplew, chr);
/*  814 */     mplew.write(chr.getBuddylist().getCapacity());
/*  815 */     if (chr.getBlessOfFairyOrigin() != null) {
/*  816 */       mplew.write(1);
/*  817 */       mplew.writeMapleAsciiString(chr.getBlessOfFairyOrigin());
/*      */     } else {
/*  819 */       mplew.write(0);
/*      */     }
/*  821 */     if (chr.getBlessOfEmpressOrigin() != null) {
/*  822 */       mplew.write(1);
/*  823 */       mplew.writeMapleAsciiString(chr.getBlessOfEmpressOrigin());
/*      */     } else {
/*  825 */       mplew.write(0);
/*      */     }
/*  827 */     MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(111111));
/*  828 */     if ((ultExplorer != null) && (ultExplorer.getCustomData() != null)) {
/*  829 */       mplew.write(1);
/*  830 */       mplew.writeMapleAsciiString(ultExplorer.getCustomData());
/*      */     } else {
/*  832 */       mplew.write(0);
/*      */     }
/*  834 */     addInventoryInfo(mplew, chr);
/*  835 */     addSkillInfo(mplew, chr);
/*  836 */     addCoolDownInfo(mplew, chr);
/*  837 */     addQuestInfo(mplew, chr);
/*  838 */     addRingInfo(mplew, chr);
/*  839 */     addRocksInfo(mplew, chr);
/*      */ 
/*  853 */     addMonsterBookInfo(mplew, chr);
/*      */ 
/*  855 */     mplew.writeShort(0);
/*  856 */     mplew.writeShort(0);
/*      */ 
/*  870 */     chr.QuestInfoPacket(mplew);
/*      */ 
/*  925 */     if ((chr.getJob() >= 3300) && (chr.getJob() <= 3312)) {
/*  926 */       addJaguarInfo(mplew, chr);
/*      */     }
/*  928 */     mplew.writeShort(0);
/*  929 */     mplew.writeShort(0);
/*  930 */     addStealSkills(mplew, chr);
/*  931 */     mplew.writeShort(0);
/*  932 */     mplew.writeInt(1);
/*  933 */     mplew.writeInt(0);
/*  934 */     mplew.writeInt(150676844);
/*  935 */     for (int i = 0; i < 17; i++) {
/*  936 */       mplew.writeInt(0);
/*      */     }
/*  938 */     long someExpireTime = getTime(System.currentTimeMillis() + 701913504L);
/*  939 */     mplew.writeLong(someExpireTime);
/*  940 */     mplew.writeInt(0);
/*  941 */     mplew.writeShort(1);
/*  942 */     mplew.write(0);
/*  943 */     mplew.writeInt(28634010);
/*  944 */     mplew.writeInt(chr.getId());
/*  945 */     int size = 4;
/*  946 */     mplew.writeLong(size);
/*  947 */     for (int i = 0; i < size; i++)
/*  948 */       mplew.writeLong(9410165 + i);
/*      */   }
/*      */     public static final int getSkillBook(final int i) {
        switch (i) {
            case 1:
            case 2:
                return 4;
            case 3:
                return 3;
            case 4:
                return 2;
        }
        return 0;
    }

 public static void addStolenSkills(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, int jobNum, boolean writeJob) {
        if (writeJob) {
            mplew.writeInt(jobNum);
        }
        int count = 0;
        if (chr.getStolenSkills() != null) {
            for (Pair<Integer, Boolean> sk : chr.getStolenSkills()) {
                if (GameConstants.getJobNumber(sk.left / 10000) == jobNum) {
                    mplew.writeInt(sk.left);
                    count++;
                    if (count >= GameConstants.getNumSteal(jobNum)) {
                        break;
                    }
                }
            }
        }
        while (count < GameConstants.getNumSteal(jobNum)) { //for now?
            mplew.writeInt(0);
            count++;
        }
    }

    public static void addChosenSkills(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        for (int i = 1; i <= 4; i++) {
            boolean found = false;
            if (chr.getStolenSkills() != null) {
                for (Pair<Integer, Boolean> sk : chr.getStolenSkills()) {
                    if (GameConstants.getJobNumber(sk.left / 10000) == i && sk.right) {
                        mplew.writeInt(sk.left);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                mplew.writeInt(0);
            }
        }
    }
    
     public static final void addInnerStats(final MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        final List<InnerSkillValueHolder> skills = chr.getInnerSkills();
        if (skills != null) {
            mplew.writeShort(skills.size());
            for (int i = 0; i < skills.size(); ++i) {
                mplew.write(i + 1); // key
                mplew.writeInt(skills.get(i).getSkillId()); //d 7000000 id ++, 71 = char cards
                mplew.write(skills.get(i).getSkillLevel()); // level
                mplew.write(skills.get(i).getRank()); //rank, C, B, A, and S
            }
        }

        mplew.writeInt(chr.getHonourLevel()); //honor lvl
        mplew.writeInt(chr.getHonourExp()); //honor exp
    }


    public static final void addStealSkills(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr) {
            for (int i = 1; i <= 4; i++) {
                addStolenSkills(mplew, chr, i, false);
            }
            addChosenSkills(mplew, chr);
    }



/*      */   public static final void addMonsterBookInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr)
/*      */   {
/*  964 */     mplew.writeInt(0);
/*      */ 
/*  966 */     if (chr.getMonsterBook().getSetScore() > 0)
/*  967 */       chr.getMonsterBook().writeFinished(mplew);
/*      */     else {
/*  969 */       chr.getMonsterBook().writeUnfinished(mplew);
/*      */     }
/*      */ 
/*  972 */     mplew.writeInt(chr.getMonsterBook().getSet());
/*      */   }
/*      */ 
/*      */  public static final void addPetItemInfo(MaplePacketLittleEndianWriter mplew, Item item, MaplePet pet, boolean active) {
/*  976 */     if (item == null)
/*  977 */             mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
/*      */     else {
/*  979 */       addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1L : item.getExpiration());
/*      */     }
/*  981 */     mplew.writeInt(-1);
/*  982 */     mplew.writeAsciiString(pet.getName(), 13);
/*  983 */     mplew.write(pet.getLevel());
/*  984 */     mplew.writeShort(pet.getCloseness());
/*  985 */     mplew.write(pet.getFullness());
/*  986 */     if (item == null)
                 mplew.writeLong(PacketHelper.getKoreanTimestamp((long) (System.currentTimeMillis() * 1.5)));
/*      */     else {
/*  989 */       addExpirationTime(mplew, item.getExpiration() <= System.currentTimeMillis() ? -1L : item.getExpiration());
/*      */     }
/*  991 */     mplew.writeShort(0);
/*  992 */     mplew.writeShort(pet.getFlags());
/*  993 */     mplew.writeInt((pet.getPetItemId() == 5000054) && (pet.getSecondsLeft() > 0) ? pet.getSecondsLeft() : 0);
/*  994 */     mplew.writeShort(0);
/*  995 */     mplew.write(active ? 0 : pet.getSummoned() ? pet.getSummonedValue() : 0);
/*  996 */     for (int i = 0; i < 4; i++)
/*  997 */       mplew.write(0);
/*      */   }
/*      */ 
/*      */   public static final void addShopInfo(MaplePacketLittleEndianWriter mplew, MapleShop shop, MapleClient c)
/*      */   {
/* 1002 */     MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
/* 1003 */     mplew.write(shop.getRanks().size() > 0 ? 1 : 0);
/*      */ 
/* 1006 */     if (shop.getRanks().size() > 0) {
/* 1007 */       mplew.write(shop.getRanks().size());
/* 1008 */       for (Pair s : shop.getRanks()) {
/* 1009 */         mplew.writeInt(((Integer)s.left).intValue());
/* 1010 */         mplew.writeMapleAsciiString((String)s.right);
/*      */       }
/*      */     }
/* 1013 */     mplew.writeShort(shop.getItems().size() + c.getPlayer().getRebuy().size());
/* 1014 */     for (MapleShopItem item : shop.getItems()) {
/* 1015 */       addShopItemInfo(mplew, item, shop, ii, null);
/*      */     }
/* 1017 */     for (Item i : c.getPlayer().getRebuy())
/* 1018 */       addShopItemInfo(mplew, new MapleShopItem(i.getItemId(), (int)ii.getPrice(i.getItemId()), i.getQuantity()), shop, ii, i);
/*      */   }
    public static final void addShopItemInfo(MaplePacketLittleEndianWriter mplew, MapleShopItem item, MapleShop shop, MapleItemInformationProvider ii, Item i) {
        mplew.writeInt(item.getItemId()); //Item ID.
        mplew.writeInt(item.getPrice() <= 0 ? 0 : item.getPrice()); //Get price, if 0, hide price.
        mplew.write(0); //unk
        mplew.writeInt(item.getReqItem()); //Secondary currency required to purchase the item.
        mplew.writeInt(item.getReqItemQ()); //Secondary currency quantity required to purchase the item.
        mplew.writeInt(0); //Time in minutes for it to expire, currently not handled.
        mplew.writeInt(0); // 0 to show confirmation box, 1 to hide it.
        mplew.writeInt(0); //No visible effects besides if value is > 0 then it hides the buyable items.
        mplew.write(0); //unk
        mplew.writeInt(0); //No visible changes.
        mplew.writeInt(0); //This item can only be purchased X amount of times, currently not handled.

        //If needed to put a quantity on a specific item in a specific shop do:
        //if(shop.getId() == xx && item.getItemId() == xx){ }
        
        if (item.getItemId() == 2061000 || item.getItemId() == 2060000) {
            mplew.writeShort(2000); //Arrows for Bow and Arrows for Crossbow are 2000 Arrows for  1 meso.   
            mplew.writeShort(1000); //Not sure what this is.
        } else if (item.getItemId() == 4310036) {
            mplew.writeShort(11); //Conqeuror's Coin, display 11 as it is hardcoded to recieve 11 per quantity.
            mplew.writeShort(1000); //Not sure what this is.
        } else if (i != null && i.getQuantity() > 1) { //I believe i is only NOT null on re-buy items.
            mplew.writeShort(i.getQuantity());  //For Re-Buys;
            mplew.writeShort(1000); //Not sure what this is.
        } else if ((!GameConstants.isThrowingStar(item.getItemId())) && (!GameConstants.isBullet(item.getItemId()))) {
            mplew.writeShort(1); //Hardcoded quantity for items..If > 1, input box asking how many will be supressed. 
            mplew.writeShort(1000); //Not sure what this is.
        } else {
            mplew.writeZeroBytes(6);
            mplew.writeShort(BitTools.doubleToShortBits(ii.getPrice(item.getItemId())));
            mplew.writeShort(ii.getSlotMax(item.getItemId()));
        }

        mplew.write(i == null ? 0 : 1);
        if (i != null) {
            addItemInfo(mplew, i);
        }
        if (shop.getRanks().size() > 0) {
            mplew.write(item.getRank() >= 0 ? 1 : 0);
            if (item.getRank() >= 0) {
                mplew.write(item.getRank());
            }
        }
        mplew.writeZeroBytes(16);
        int size = 4;
        for (int j = 0; j < size; j++) {
            mplew.writeLong(9410165 + j);
        }
    }
/*      */ 
/*      */   public static final void addJaguarInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr)
/*      */   {
/* 1091 */     mplew.write(chr.getIntNoRecord(111112));
/* 1092 */     mplew.writeZeroBytes(20);
/*      */   }
/*      */ 
/*      */  
/*      */   public static <E extends Buffstat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup)
/*      */   {
/* 1180 */     for (int i = 8; i >= 1; i--)
/* 1181 */       mplew.writeInt(i == statup.getPosition() ? statup.getValue() : 0);
/*      */   }
/*      */ 
/*      */   public static <E extends Buffstat> void writeMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups)
/*      */   {
/* 1186 */     int[] mask = new int[8];
/* 1187 */     for (Buffstat statup : statups) {
/* 1188 */       mask[(statup.getPosition() - 1)] |= statup.getValue();
/*      */     }
/* 1190 */     for (int i = mask.length; i >= 1; i--)
/* 1191 */       mplew.writeInt(mask[(i - 1)]);
/*      */   }
/*      */ 
/*      */   public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Collection<Pair<E, Integer>> statups)
/*      */   {
/* 1196 */     int[] mask = new int[8];
/* 1197 */     for (Pair statup : statups) {
/* 1198 */       mask[(((Buffstat)statup.left).getPosition() - 1)] |= ((Buffstat)statup.left).getValue();
/*      */     }
/* 1200 */     for (int i = mask.length; i >= 1; i--)
/* 1201 */       mplew.writeInt(mask[(i - 1)]);
/*      */   }
/*      */ 
/*      */   public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Map<E, Integer> statups)
/*      */   {
/* 1206 */     int[] mask = new int[8];
/* 1207 */     for (Buffstat statup : statups.keySet()) {
/* 1208 */       mask[(statup.getPosition() - 1)] |= statup.getValue();
/*      */     }
/* 1210 */     for (int i = mask.length; i >= 1; i--)
/* 1211 */       mplew.writeInt(mask[(i - 1)]);
/*      */   }
 public static List<Pair<MapleBuffStat, Integer>> sortBuffStats(Map<MapleBuffStat, Integer> statups){
        List<Pair<MapleBuffStat,Integer>> statvals = new ArrayList<>();
        //Buffstats that should always be first in the sort order.
        List<MapleBuffStat> alwaysFirst = Collections.unmodifiableList(Arrays.asList(
            MapleBuffStat.STR, MapleBuffStat.DEX, MapleBuffStat.INT, MapleBuffStat.LUK,
            MapleBuffStat.MP_CON_INCREASE
            ));
        for(Entry<MapleBuffStat,Integer> stat : statups.entrySet()){
            statvals.add(new Pair<>(stat.getKey(), stat.getValue()));
        }
        //TODO: This has O(n^2) time-complexity.  Efficiency, where art thou?
        //TODO: There are still a few buffstat combinations that can get out-of-order.  Needs more investigation.
        boolean changed;
        int i;
        int k;
        //The implementation here is just a bubble-sort, and a crude one at that.
        do{
            changed = false;
            i = 0;
            k = 1;
            for(int iter = 0; iter < statvals.size()-1; iter++){
                Pair<MapleBuffStat, Integer> a = statvals.get(i);
                Pair<MapleBuffStat, Integer> b = statvals.get(k);
                if(a != null && b != null){
                    //Check for position first, but ignore the check if the buffstat is supposed to always be first
                    if((a.left.getPosition() > b.left.getPosition() && !alwaysFirst.contains(a.left) && 
                        (a.left.canStack() && !b.left.canStack())) || 
                        //If the two buffstats have an equal position, sort based on value (lowest to highest).
                       (a.left.getPosition() == b.left.getPosition() && a.left.getValue() > b.left.getValue()) ||
                        //If the buffstat is supposed to be first (and is not behind another always-first buffstat, sort.
                        (alwaysFirst.contains(b.left) && !alwaysFirst.contains(a.left)) ||
                        //Stackable buffstats always come *after* non-stackable ones.
                        (a.left.canStack() && !b.left.canStack())){
                        Pair<MapleBuffStat, Integer> swap = new Pair<>(a.left, a.right);
                        statvals.remove(i);
                        statvals.add(i, b);
                        statvals.remove(k);
                        statvals.add(k, swap);
                        changed = true;
                    }
                }
                i++;
                k++;
            }
        } while (changed);
        
        return statvals;
    }




/*      */ }

/* Location:           C:\Users\Sjogren\Desktop\lithium.jar
 * Qualified Name:     tools.packet.PacketHelper
 * JD-Core Version:    0.6.0
 */
      
      
           