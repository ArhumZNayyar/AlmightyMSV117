/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
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
package client;

import client.MapleTrait.MapleTraitType;
import client.anticheat.CheatTracker;
import client.anticheat.ReportType;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.ItemLoader;
import client.inventory.MapleAndroid;
import client.inventory.MapleImp;
import client.inventory.MapleImp.ImpFlag;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MapleMount;
import client.inventory.MaplePet;
import client.inventory.MapleRing;
import client.messages.commands.CommandExecute;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.*;
import constants.BattleConstants.PokemonNature;
import constants.BattleConstants.PokemonStat;
import constants.GameConstants;
import database.DatabaseConnection;
import database.DatabaseException;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.MapleCharacterLook;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.PlayerBuffStorage;
import handling.world.PlayerBuffValueHolder;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import provider.MapleData;
import provider.MapleDataTool;
import scripting.EventInstanceManager;
import scripting.NPCConversationManager;
import scripting.NPCScriptManager;
import server.CashShop;
import server.MapleAchievements;
import server.MapleCarnivalChallenge;
import server.MapleCarnivalParty;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleShop;
import server.MapleShopFactory;
import server.MapleShopItem;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelEffectAction;
import server.MapleStorage;
import server.MapleTrade;
import server.PokemonBattle;
import server.RandomRewards;
import server.Randomizer;
import server.Timer.BuffTimer;
import server.Timer.MapTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.maps.*;
import server.maps.AnimatedMapleMapObject;
import server.maps.FieldLimitType;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleSummon;
import server.maps.SavedLocationType;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.IMaplePlayerShop;
import tools.ConcurrentEnumMap;
import tools.FileoutputUtil;
import tools.MockIOSession;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.packet.CField;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.NPCPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuddylistPacket;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;
import tools.packet.LoginPacket;
import tools.packet.MTSCSPacket;
import tools.packet.MobPacket;
import tools.packet.MonsterCarnivalPacket;
import tools.packet.PetPacket;
import tools.packet.PlayerShopPacket;

public class MapleCharacter extends AnimatedMapleMapObject implements Serializable, MapleCharacterLook {

    private static final long serialVersionUID = 845748950829L;

    private String name, chalktext, BlessOfFairy_Origin, BlessOfEmpress_Origin, teleportname;
    public long lastCombo, lastfametime, keydown_skill, nextConsume, pqStartTime, lastDragonBloodTime,
            lastBerserkTime, lastRecoveryTime, lastSummonTime, mapChangeTime, lastFishingTime, lastFairyTime,
            lastHPTime, lastMPTime, lastFamiliarEffectTime, lastDOTTime, mesosbank, expL;
    private byte gmLevel, gender, initialSpawnPoint, skinColor, guildrank = 5, allianceRank = 5,
            world, fairyExp, numClones, subcategory, cardStack, runningStack;
    private short level, job, mulung_energy, combo, force, availableCP, fatigue, totalCP, hpApUsed, scrolledPosition, hcMode;
    public int accountid, id, meso, exp, hair, face, demonMarking, elf, mapid, fame, pvpExp, pvpPoints, totalWins, totalLosses,
            guildid = 0, fallcounter, maplepoints, acash, nxcredit, chair, itemEffect, points, vpoints,
            rank = 1, rankMove = 0, jobRank = 1, jobRankMove = 0, marriageId, marriageItemId, dotHP,
            currentrep, totalrep, coconutteam, followid, battleshipHP, gachexp, challenge, guildContribution = 0, remainingAp, evolve, honourExp, honourLevel, chatType = -3, azwanCoinsAvail, azwanCoinsRedeemed, azwanECoinsAvail, azwanCCoinsAvail, blackjackchips;
    private Point old;
    private MonsterFamiliar summonedFamiliar;
    private int[] wishlist, rocks, savedLocations, regrocks, hyperrocks, remainingSp = new int[10];
    private transient AtomicInteger inst, insd;
    private transient List<LifeMovementFragment> lastres;
    private List<Integer> lastmonthfameids, lastmonthbattleids, extendedSlots;
    private List<MapleDoor> doors;
    private List<MechDoor> mechDoors;
    private List<MaplePet> pets;
    private List<Item> rebuy;
    private MapleImp[] imps;
    private List<Pair<Integer, Boolean>> stolenSkills = new ArrayList<Pair<Integer, Boolean>>();
    private transient WeakReference<MapleCharacter>[] clones;
    private transient Set<MapleMonster> controlled;
    private transient Set<MapleMapObject> visibleMapObjects;
    private transient ReentrantReadWriteLock visibleMapObjectsLock;
    private transient ReentrantReadWriteLock summonsLock;
    private transient ReentrantReadWriteLock controlledLock;
    private transient MapleAndroid android;
    private Map<MapleQuest, MapleQuestStatus> quests;
    private Map<Integer, String> questinfo;
    private Map<Skill, SkillEntry> skills;
    private transient Map<MapleBuffStat, MapleBuffStatValueHolder> effects;
    private transient List<MapleSummon> summons;
    private transient Map<Integer, MapleCoolDownValueHolder> coolDowns;
    private transient Map<MapleDisease, MapleDiseaseValueHolder> diseases;
    private Map<ReportType, Integer> reports;
    private CashShop cs;
    private transient Deque<MapleCarnivalChallenge> pendingCarnivalRequests;
    private transient MapleCarnivalParty carnivalParty;
    private BuddyList buddylist;
    private MonsterBook monsterbook;
    private transient CheatTracker anticheat;
    private MapleClient client;
    private transient MapleParty party;
    private PlayerStats stats;
    private MapleCharacterCards characterCard;
    private transient MapleMap map;
    private transient MapleShop shop;
    private transient MapleDragon dragon;
    private transient MapleExtractor extractor;
    private transient RockPaperScissors rps;
    private Map<Integer, MonsterFamiliar> familiars;
    private MapleStorage storage;
    private transient MapleTrade trade;
    private MapleMount mount;
    private int sp;
    private List<Integer> finishedAchievements;
    private MapleMessenger messenger;
    private byte[] petStore;
    private transient IMaplePlayerShop playerShop;
    private boolean invincible, canTalk, clone, followinitiator, followon, smega, hasSummon;
    private MapleGuildCharacter mgc;
    private MapleFamilyCharacter mfc;
    private transient EventInstanceManager eventInstance;
    private MapleInventory[] inventory;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    private EnumMap<MapleTraitType, MapleTrait> traits;
    private Battler[] battlers = new Battler[6];
    private List<Battler> boxed;
    private MapleKeyLayout keylayout;
    public MapleCharacter chr;
    public static MapleCharacter Tchr;
    private transient ScheduledFuture<?> mapTimeLimitTask;
    private transient Event_PyramidSubway pyramidSubway = null;
    private transient List<Integer> pendingExpiration = null;
    private transient Map<Skill, SkillEntry> pendingSkills = null;
    private transient Map<Integer, Integer> linkMobs;
    private transient PokemonBattle battle;
    private boolean changed_wishlist, changed_trocklocations, changed_regrocklocations, changed_hyperrocklocations, changed_skillmacros, changed_achievements,
            changed_savedlocations, changed_pokemon, changed_questinfo, changed_skills, changed_reports, changed_extendedSlots, update_skillswipe, whiteEnabled = true, innerskill_changed = false, keepBuffs = false;
    private long gmtime = 0;
    private boolean hidden = false; // TODO: gm hide
    public byte vaccinated;
    public int monsterkills;
    public short Tlvl;
    private static Map<Integer, MapleMonsterStats> monsterStats = new HashMap<Integer, MapleMonsterStats>();
    private List<InnerSkillValueHolder> innerSkills;
    private MapleShop azwanShopList;
    private AutoLooter autolooter;
    private String namechange;
    private Map<Short, String> area_info = new LinkedHashMap<>();
    /*
     * Start of Custom Feature
     */
    /*
     * All custom shit declare here
     */
    private boolean megaHidden;
    public int reborns, apstorage, Paragons;
    private short occupationId, occupationEXP, mobKilledNo;
    private long loginTime;
    private int victorypoints, dailyachtime, ffakills, ffadeaths, ac3points, eventpoints, bwtstorage, eventach, fameach, dojoach, cpqach, rebornach, dailyach, acdualhp, guildpoints, muted, dsmega;
    private long lastdsay, lastlotto, lastgmcall, lastbuff, lastac3, invulnerableTime;
    private int pendingMarriageId;
    private java.util.Timer healTimer;
    private java.util.Timer DFRecoveryTimer;
    private boolean petAutoFeed = false; 
    private int[] friendshippoints = new int[4];
    /*
     * End of Custom Feature
     */
    private int str;
    private int luk;
    private int int_;
    private int dex;
    private int DonatorPoints;
    private long lastVoteTime;
    private int totalvotes;
    private long lastVoteTime2;
    private int entryNumber = 0;
    private int progress = 0;
    private MapleCharacter(final boolean ChannelServer) {
        setStance(0);
        setPosition(new Point(0, 0));

        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        quests = new LinkedHashMap<MapleQuest, MapleQuestStatus>(); // Stupid erev quest.
        skills = new LinkedHashMap<Skill, SkillEntry>(); //Stupid UAs.
        stats = new PlayerStats();
        innerSkills = new LinkedList<InnerSkillValueHolder>();
        azwanShopList = null;
        characterCard = new MapleCharacterCards();
        for (int i = 0; i < remainingSp.length; i++) {
            remainingSp[i] = 0;
        }
        traits = new EnumMap<MapleTraitType, MapleTrait>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            traits.put(t, new MapleTrait(t));
        }
        if (ChannelServer) {
            changed_reports = false;
            changed_skills = false;
            changed_achievements = false;
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_pokemon = false;
            changed_extendedSlots = false;
            changed_questinfo = false;
            update_skillswipe = false;
            scrolledPosition = 0;
            lastCombo = 0;
            mulung_energy = 0;
            /*
             * START OF CUSTOM FEATURES
             * 
             */
            
            loginTime = 0;
            /*
             * END OF CUSTOM FEATURES
             */
            combo = 0;
            force = 0;
            keydown_skill = 0;
            nextConsume = 0;
            pqStartTime = 0;
            fairyExp = 0;
            cardStack = 0;
            runningStack = 1;
            mapChangeTime = 0;
            lastRecoveryTime = 0;
            lastDragonBloodTime = 0;
            lastBerserkTime = 0;
            lastFishingTime = 0;
            lastFairyTime = 0;
            lastHPTime = 0;
            lastMPTime = 0;
            lastFamiliarEffectTime = 0;
            old = new Point(0, 0);
            coconutteam = 0;
            followid = 0;
            battleshipHP = 0;
            marriageItemId = 0;
            fallcounter = 0;
            challenge = 0;
            dotHP = 0;
            lastSummonTime = 0;
            hasSummon = false;
            invincible = false;
            canTalk = true;
            clone = false;
            followinitiator = false;
            followon = false;
            rebuy = new ArrayList<Item>();
            linkMobs = new HashMap<Integer, Integer>();
            finishedAchievements = new ArrayList<Integer>();
            reports = new EnumMap<ReportType, Integer>(ReportType.class);
            teleportname = "";
            smega = true;
            petStore = new byte[3];
            for (int i = 0; i < petStore.length; i++) {
                petStore[i] = (byte) -1;
            }
            wishlist = new int[10];
            rocks = new int[10];
            regrocks = new int[5];
            hyperrocks = new int[13];
            imps = new MapleImp[3];
            clones = new WeakReference[5]; //for now
            for (int i = 0; i < clones.length; i++) {
                clones[i] = new WeakReference<MapleCharacter>(null);
            }
            boxed = new ArrayList<Battler>();
            familiars = new LinkedHashMap<Integer, MonsterFamiliar>();
            extendedSlots = new ArrayList<Integer>();
            effects = new ConcurrentEnumMap<MapleBuffStat, MapleBuffStatValueHolder>(MapleBuffStat.class);
            coolDowns = new LinkedHashMap<Integer, MapleCoolDownValueHolder>();
            diseases = new ConcurrentEnumMap<MapleDisease, MapleDiseaseValueHolder>(MapleDisease.class);
            inst = new AtomicInteger(0);// 1 = NPC/ Quest, 2 = Duey, 3 = Hired Merch store, 4 = Storage
            insd = new AtomicInteger(-1);
            keylayout = new MapleKeyLayout();
            doors = new ArrayList<MapleDoor>();
            mechDoors = new ArrayList<MechDoor>();
            controlled = new LinkedHashSet<MapleMonster>();
            controlledLock = new ReentrantReadWriteLock();
            summons = new LinkedList<MapleSummon>();
            summonsLock = new ReentrantReadWriteLock();
            visibleMapObjects = new LinkedHashSet<MapleMapObject>();
            visibleMapObjectsLock = new ReentrantReadWriteLock();
            pendingCarnivalRequests = new LinkedList<MapleCarnivalChallenge>();

            savedLocations = new int[SavedLocationType.values().length];
            for (int i = 0; i < SavedLocationType.values().length; i++) {
                savedLocations[i] = -1;
            }
            questinfo = new LinkedHashMap<Integer, String>();
            pets = new ArrayList<MaplePet>();
            namechange = null;
        }
    }
    
    public long getGMTime(){
     return gmtime;
}

public void setGMTime(){
     this.gmtime = System.currentTimeMillis();
}

    public static MapleCharacter getDefault(final MapleClient client, final JobType type) {
        MapleCharacter ret = new MapleCharacter(false);
        ret.client = client;
        ret.map = null;
        ret.exp = 0;
        ret.gmLevel = 0;
        ret.job = (short) type.id;
        ret.meso = 0;
        ret.level = 1;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList((byte) 20);
        ret.stats.str = 12;
        ret.stats.dex = 5;
        ret.stats.int_ = 4;
        ret.stats.luk = 4;
        ret.stats.maxhp = 50;
        ret.stats.hp = 50;
        ret.stats.maxmp = 50;
        ret.stats.mp = 50;
        ret.gachexp = 0;

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.nxcredit = rs.getInt("nxCredit");
                ret.acash = rs.getInt("ACash");
                ret.maplepoints = rs.getInt("mPoints");
                ret.points = rs.getInt("points");
                ret.vpoints = rs.getInt("vpoints");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
        }
        return ret;
    }

    public final static MapleCharacter ReconstructChr(final CharacterTransfer ct, final MapleClient client, final boolean isChannel) {
        final MapleCharacter ret = new MapleCharacter(true); // Always true, it's change channel
        ret.client = client;
        if (!isChannel) {
            ret.client.setChannel(ct.channel);
        }
        ret.id = ct.characterid;
        ret.name = ct.name;
        ret.level = ct.level;
        ret.fame = ct.fame;

        ret.CRand = new PlayerRandomStream();

        ret.stats.str = ct.str;
        ret.stats.dex = ct.dex;
        ret.stats.int_ = ct.int_;
        ret.stats.luk = ct.luk;
        ret.stats.maxhp = ct.maxhp;
        ret.stats.maxmp = ct.maxmp;
        ret.stats.hp = ct.hp;
        ret.stats.mp = ct.mp;

        ret.characterCard.setCards(ct.cardsInfo);

        ret.chalktext = ct.chalkboard;
        ret.gmLevel = ct.gmLevel;
        ret.exp = (ret.level >= 200 || (GameConstants.isKOC(ret.job) && ret.level >= 120)) && !ret.isIntern() ? 0 : ct.exp;
        ret.hpApUsed = ct.hpApUsed;
        ret.remainingSp = ct.remainingSp;
        ret.remainingAp = ct.remainingAp;
        ret.meso = ct.meso;
        ret.stolenSkills = ct.stolenSkills;
        ret.skinColor = ct.skinColor;
        ret.gender = ct.gender;
        ret.job = ct.job;
        ret.hair = ct.hair;
        ret.face = ct.face;
        ret.demonMarking = ct.demonMarking;
        ret.accountid = ct.accountid;
        ret.totalWins = ct.totalWins;
        ret.totalLosses = ct.totalLosses;
        client.setAccID(ct.accountid);
        ret.mapid = ct.mapid;
        ret.initialSpawnPoint = ct.initialSpawnPoint;
        ret.world = ct.world;
        ret.guildid = ct.guildid;
        ret.guildrank = ct.guildrank;
        ret.guildContribution = ct.guildContribution;
        ret.allianceRank = ct.alliancerank;
        ret.points = ct.points;
        ret.vpoints = ct.vpoints;
        ret.fairyExp = ct.fairyExp;
        ret.cardStack = ct.cardStack;
        ret.marriageId = ct.marriageId;
        ret.currentrep = ct.currentrep;
        ret.totalrep = ct.totalrep;
        ret.gachexp = ct.gachexp;
        ret.pvpExp = ct.pvpExp;
        ret.pvpPoints = ct.pvpPoints;
        ret.innerSkills = (LinkedList<InnerSkillValueHolder>) ct.innerSkills;
        ret.azwanShopList = (MapleShop) ct.azwanShopList;
        /*
         * Start of Custom Feature
         */
        ret.reborns = ct.reborns;
        ret.apstorage = ct.apstorage;
        ret.Paragons = ct.Paragons;
        ret.monsterkills = ct.monsterkills;
        ret.mesosbank = ct.mesosbank;
        ret.healTimer = ct.healTimer;
        ret.DFRecoveryTimer = ct.DFRecoveryTimer;
        ret.petAutoFeed = ct.petAutoFeed;
        
        /*
         * End of Custom Feature
         */
        ret.makeMFC(ct.familyid, ct.seniorid, ct.junior1, ct.junior2);
        if (ret.guildid > 0) {
            ret.mgc = new MapleGuildCharacter(ret);
        }
        ret.fatigue = ct.fatigue;
        ret.buddylist = new BuddyList(ct.buddysize);
        ret.subcategory = ct.subcategory;

        if (isChannel) {
            final MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
            ret.map = mapFactory.getMap(ret.mapid);
            if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                ret.map = mapFactory.getMap(100000000);
            } else {
                if (ret.map.getForcedReturnId() != 999999999 && ret.map.getForcedReturnMap() != null) {
                    ret.map = ret.map.getForcedReturnMap();
                }
            }
            MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
            if (portal == null) {
                portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                ret.initialSpawnPoint = 0;
            }
            ret.setPosition(portal.getPosition());

            final int messengerid = ct.messengerid;
            if (messengerid > 0) {
                ret.messenger = World.Messenger.getMessenger(messengerid);
            }
        } else {

            ret.messenger = null;
        }
        int partyid = ct.partyid;
        if (partyid >= 0) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null && party.getMemberById(ret.id) != null) {
                ret.party = party;
            }
        }

        MapleQuestStatus queststatus_from;
        for (final Map.Entry<Integer, Object> qs : ct.Quest.entrySet()) {
            queststatus_from = (MapleQuestStatus) qs.getValue();
            queststatus_from.setQuest(qs.getKey());
            ret.quests.put(queststatus_from.getQuest(), queststatus_from);
        }
        for (final Map.Entry<Integer, SkillEntry> qs : ct.Skills.entrySet()) {
            ret.skills.put(SkillFactory.getSkill(qs.getKey()), qs.getValue());
        }
        for (final Integer zz : ct.finishedAchievements) {
            ret.finishedAchievements.add(zz);
        }
        for (final Object zz : ct.boxed) {
            Battler zzz = (Battler) zz;
            zzz.setStats();
            ret.boxed.add(zzz);
        }
        for (Entry<MapleTraitType, Integer> t : ct.traits.entrySet()) {
            ret.traits.get(t.getKey()).setExp(t.getValue());
        }
        for (final Map.Entry<Byte, Integer> qs : ct.reports.entrySet()) {
            ret.reports.put(ReportType.getById(qs.getKey()), qs.getValue());
        }
        ret.monsterbook = new MonsterBook(ct.mbook, ret);
        ret.inventory = (MapleInventory[]) ct.inventorys;
        ret.BlessOfFairy_Origin = ct.BlessOfFairy;
        ret.BlessOfEmpress_Origin = ct.BlessOfEmpress;
        ret.skillMacros = (SkillMacro[]) ct.skillmacro;
        ret.battlers = (Battler[]) ct.battlers;
        for (Battler b : ret.battlers) {
            if (b != null) {
                b.setStats();
            }
        }
        ret.petStore = ct.petStore;
        ret.keylayout = new MapleKeyLayout(ct.keymap);
        ret.questinfo = ct.InfoQuest;
        ret.familiars = ct.familiars;
        ret.savedLocations = ct.savedlocation;
        ret.wishlist = ct.wishlist;
        ret.rocks = ct.rocks;
        ret.regrocks = ct.regrocks;
        ret.hyperrocks = ct.hyperrocks;
        ret.buddylist.loadFromTransfer(ct.buddies);
        ret.keydown_skill = 0; // Keydown skill can't be brought over
        ret.lastfametime = ct.lastfametime;
        ret.lastmonthfameids = ct.famedcharacters;
        ret.lastmonthbattleids = ct.battledaccs;
        ret.extendedSlots = ct.extendedSlots;
        ret.storage = (MapleStorage) ct.storage;
        ret.cs = (CashShop) ct.cs;
        client.setAccountName(ct.accountname);
        ret.nxcredit = ct.nxCredit;
        ret.acash = ct.ACash;
        ret.maplepoints = ct.MaplePoints;
        ret.numClones = ct.clonez;
        ret.imps = ct.imps;
        ret.anticheat = (CheatTracker) ct.anticheat;
        ret.anticheat.start(ret);
        ret.rebuy = ct.rebuy;
        ret.mount = new MapleMount(ret, ct.mount_itemid, ret.stats.getSkillByJob(1004, ret.job), ct.mount_Fatigue, ct.mount_level, ct.mount_exp);
        ret.expirationTask(false, false);
        ret.stats.recalcLocalStats(true, ret);
        client.setTempIP(ct.tempIP);
        ret.vaccinated = ct.vaccinated;
        
        return ret;
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) {
        return loadCharFromDB(charid, client, channelserver, null);
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver, final Map<Integer, CardData> cads) {
        final MapleCharacter ret = new MapleCharacter(channelserver);
        ret.client = client;
        ret.id = charid;

        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Loading the Char Failed (char not found)");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getShort("level");
            ret.fame = rs.getInt("fame");
            ret.mesosbank = rs.getLong("mesosbank");
            ret.stats.str = rs.getShort("str");
            ret.stats.dex = rs.getShort("dex");
            ret.stats.int_ = rs.getShort("int");
            ret.stats.luk = rs.getShort("luk");
            ret.stats.maxhp = rs.getInt("maxhp");
            ret.stats.maxmp = rs.getInt("maxmp");
            ret.stats.hp = rs.getInt("hp");
            ret.stats.mp = rs.getInt("mp");
            ret.job = rs.getShort("job");
            ret.gmLevel = rs.getByte("gm");
            ret.exp = (ret.level >= 200 || (GameConstants.isKOC(ret.job) && ret.level >= 120)) && !ret.isIntern() ? 0 : rs.getInt("exp");
            ret.hpApUsed = rs.getShort("hpApUsed");
            final String[] sp = rs.getString("sp").split(",");
            for (int i = 0; i < ret.remainingSp.length; i++) {
                ret.remainingSp[i] = Integer.parseInt(sp[i]);
            }
            ret.remainingAp = rs.getShort("ap");
            ret.meso = rs.getInt("meso");
            ret.skinColor = rs.getByte("skincolor");
            ret.gender = rs.getByte("gender");

            ret.hair = rs.getInt("hair");
            ret.face = rs.getInt("face");
            ret.demonMarking = rs.getInt("demonMarking");
            ret.accountid = rs.getInt("accountid");
            client.setAccID(ret.accountid);
            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getByte("spawnpoint");
            ret.world = rs.getByte("world");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getByte("guildrank");
            ret.allianceRank = rs.getByte("allianceRank");
            ret.guildContribution = rs.getInt("guildContribution");
            ret.totalWins = rs.getInt("totalWins");
            ret.totalLosses = rs.getInt("totalLosses");
            ret.currentrep = rs.getInt("currentrep");
            ret.totalrep = rs.getInt("totalrep");
            ret.makeMFC(rs.getInt("familyid"), rs.getInt("seniorid"), rs.getInt("junior1"), rs.getInt("junior2"));
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            ret.gachexp = rs.getInt("gachexp");
            ret.buddylist = new BuddyList(rs.getByte("buddyCapacity"));
            ret.subcategory = rs.getByte("subcategory");
            ret.mount = new MapleMount(ret, 0, ret.stats.getSkillByJob(1004, ret.job), (byte) 0, (byte) 1, 0);
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            ret.marriageId = rs.getInt("marriageId");
            ret.fatigue = rs.getShort("fatigue");
            ret.pvpExp = rs.getInt("pvpExp");
            ret.pvpPoints = rs.getInt("pvpPoints");
            ret.honourExp = rs.getInt("honourExp");
            ret.honourLevel = rs.getInt("honourLevel");
            ret.azwanCoinsAvail = rs.getInt("azwanCoinsAvail");
            ret.azwanCoinsRedeemed = rs.getInt("azwanCoinsRedeemed");
            ret.azwanECoinsAvail = rs.getInt("azwanECoinsAvail");
            ret.azwanCCoinsAvail = rs.getInt("azwanCCoinsAvail");
            /*
             * Start of Custom Features
             */
            ret.reborns = rs.getInt("reborns");
            ret.apstorage = rs.getInt("apstorage");
            ret.Paragons = rs.getInt("Paragons");
            ret.vaccinated = rs.getByte("vaccinated");
            ret.monsterkills = rs.getInt("monsterkills");
            
            /*
             * End of Custom Features
             */
            for (MapleTrait t : ret.traits.values()) {
                t.setExp(rs.getInt(t.getType().name()));
            }
            if (channelserver) {
                ret.CRand = new PlayerRandomStream();
                ret.anticheat = new CheatTracker(ret);
                MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                    ret.map = mapFactory.getMap(100000000);
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());

                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    MapleParty party = World.Party.getParty(partyid);
                    if (party != null && party.getMemberById(ret.id) != null) {
                        ret.party = party;
                    }
                }
                final String[] pets = rs.getString("pets").split(",");
                for (int i = 0; i < ret.petStore.length; i++) {
                    ret.petStore[i] = Byte.parseByte(pets[i]);
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM achievements WHERE accountid = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.finishedAchievements.add(rs.getInt("achievementid"));
                }
                ps.close();
                rs.close();

                ps = con.prepareStatement("SELECT * FROM reports WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (ReportType.getById(rs.getByte("type")) != null) {
                        ret.reports.put(ReportType.getById(rs.getByte("type")), rs.getInt("count"));
                    }
                }

            }
            rs.close();
            ps.close();

            if (cads != null) { // so that we load only once.
                ret.characterCard.setCards(cads);
            } else { // load
                ret.characterCard.loadCards(client, channelserver);
            }

            ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");

            while (rs.next()) {
                final int id = rs.getInt("quest");
                final MapleQuest q = MapleQuest.getInstance(id);
                final byte stat = rs.getByte("status");
                if ((stat == 1 || stat == 2) && channelserver && (q == null || q.isBlocked())) { //bigbang
                    continue;
                }
                if (stat == 1 && channelserver && !q.canStart(ret, null)) { //bigbang
                    continue;
                }
                final MapleQuestStatus status = new MapleQuestStatus(q, stat);
                final long cTime = rs.getLong("time");
                if (cTime > -1) {
                    status.setCompletionTime(cTime * 1000);
                }
                status.setForfeited(rs.getInt("forfeited"));
                status.setCustomData(rs.getString("customData"));
                ret.quests.put(q, status);
                pse.setInt(1, rs.getInt("queststatusid"));
                final ResultSet rsMobs = pse.executeQuery();

                while (rsMobs.next()) {
                    status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                }
                rsMobs.close();
            }
            rs.close();
            ps.close();
            pse.close();

            if (channelserver) {
                ret.monsterbook = MonsterBook.loadCards(ret.accountid, ret);

                ps = con.prepareStatement("SELECT * FROM inventoryslot where characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                if (!rs.next()) {
                    rs.close();
                    ps.close();
                    throw new RuntimeException("No Inventory slot column found in SQL. [inventoryslot]");
                } else {
                    ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(rs.getByte("equip"));
                    ret.getInventory(MapleInventoryType.USE).setSlotLimit(rs.getByte("use"));
                    ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(rs.getByte("setup"));
                    ret.getInventory(MapleInventoryType.ETC).setSlotLimit(rs.getByte("etc"));
                    ret.getInventory(MapleInventoryType.CASH).setSlotLimit(rs.getByte("cash"));
                }
                ps.close();
                rs.close();

                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(false, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                    if (mit.getLeft().getPet() != null) {
                        ret.pets.add(mit.getLeft().getPet());
                    }
                }

                ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    ret.getClient().setAccountName(rs.getString("name"));
                    ret.nxcredit = rs.getInt("nxCredit");
                    ret.acash = rs.getInt("ACash");
                    ret.maplepoints = rs.getInt("mPoints");
                    ret.points = rs.getInt("points");
                    ret.vpoints = rs.getInt("vpoints");

                    if (rs.getTimestamp("lastlogon") != null) {
                        final Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(rs.getTimestamp("lastlogon").getTime());
                    }
                    if (rs.getInt("banned") > 0) {
                        rs.close();
                        ps.close();
                        ret.getClient().getSession().close();
                        throw new RuntimeException("Loading a banned character");
                    }
                    rs.close();
                    ps.close();

                    ps = con.prepareStatement("UPDATE accounts SET lastlogon = CURRENT_TIMESTAMP() WHERE id = ?");
                    ps.setInt(1, ret.accountid);
                    ps.executeUpdate();
                } else {
                    rs.close();
                }
                ps.close();

                ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                while (rs.next()) {
                    ret.questinfo.put(rs.getInt("quest"), rs.getString("customData"));
                }
                rs.close();
                ps.close();


                ps = con.prepareStatement("SELECT skillid, skilllevel, masterlevel, expiration FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                Skill skil;
                while (rs.next()) {
                    final int skid = rs.getInt("skillid");
                    skil = SkillFactory.getSkill(skid);
                    int skl = rs.getInt("skilllevel");
                    byte msl = rs.getByte("masterlevel");
                    if (skil != null && GameConstants.isApplicableSkill(skid)) {
                        if (skl > skil.getMaxLevel() && skid < 92000000) {
                            if (!skil.isBeginnerSkill() && skil.canBeLearnedBy(ret.job) && !skil.isSpecialSkill()) {
                                ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += (skl - skil.getMaxLevel());
                            }
                            skl = (byte) skil.getMaxLevel();
                        }
                        if (msl > skil.getMaxLevel()) {
                            msl = (byte) skil.getMaxLevel();
                        }
                        ret.skills.put(skil, new SkillEntry(skl, msl, rs.getLong("expiration")));
                    } else if (skil == null) { //doesnt. exist. e.g. bb
                        if (!GameConstants.isBeginnerJob(skid / 10000) && skid / 10000 != 900 && skid / 10000 != 800 && skid / 10000 != 9000) {
                            ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += skl;
                        }
                    }
                }
                rs.close();
                ps.close();

                ret.expirationTask(false, true); //do it now

                // Bless of Fairy handling
                ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                int maxlevel_ = 0, maxlevel_2 = 0;
                while (rs.next()) {
                    if (rs.getInt("id") != charid) { // Not this character
                        if (GameConstants.isKOC(rs.getShort("job"))) {
                            int maxlevel = (rs.getShort("level") / 5);

                            if (maxlevel > 24) {
                                maxlevel = 24;
                            }
                            if (maxlevel > maxlevel_2 || maxlevel_2 == 0) {
                                maxlevel_2 = maxlevel;
                                ret.BlessOfEmpress_Origin = rs.getString("name");
                            }
                        }
                        int maxlevel = (rs.getShort("level") / 10);

                        if (maxlevel > 20) {
                            maxlevel = 20;
                        }
                        if (maxlevel > maxlevel_ || maxlevel_ == 0) {
                            maxlevel_ = maxlevel;
                            ret.BlessOfFairy_Origin = rs.getString("name");
                        }

                    }
                }
                /*
                 * if (!compensate_previousSP) { for (Entry<Skill, SkillEntry>
                 * skill : ret.skills.entrySet()) { if
                 * (!skill.getKey().isBeginnerSkill() &&
                 * !skill.getKey().isSpecialSkill()) {
                 * ret.remainingSp[GameConstants.getSkillBookForSkill(skill.getKey().getId())]
                 * += skill.getValue().skillevel; skill.getValue().skillevel =
                 * 0; } } ret.setQuestAdd(MapleQuest.getInstance(170000), (byte)
                 * 0, null); //set it so never again }
                 */
                if (ret.BlessOfFairy_Origin == null) {
                    ret.BlessOfFairy_Origin = ret.name;
                }
                ret.skills.put(SkillFactory.getSkill(GameConstants.getBOF_ForJob(ret.job)), new SkillEntry(maxlevel_, (byte) 0, -1));
                if (SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)) != null) {
                    if (ret.BlessOfEmpress_Origin == null) {
                        ret.BlessOfEmpress_Origin = ret.BlessOfFairy_Origin;
                    }
                    ret.skills.put(SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)), new SkillEntry(maxlevel_2, (byte) 0, -1));
                }
                ps.close();
                rs.close();
                // END
                ps = con.prepareStatement("SELECT skill_id, skill_level, max_level, rank FROM inner_ability_skills WHERE player_id = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.innerSkills.add(new InnerSkillValueHolder(rs.getInt("skill_id"), rs.getByte("skill_level"), rs.getByte("max_level"), rs.getByte("rank")));
                }
                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int position;
                while (rs.next()) {
                    position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();
                /*
                 * ps = con.prepareStatement("SELECT victimid, skillid,
                 * skilllevel, slot, category FROM stolen_skills WHERE chrid =
                 * ?"); ps.setInt(1, charid); rs = ps.executeQuery(); int slot;
                 * while (rs.next()) { slot = rs.getInt("slot"); SkillSwipe ss =
                 * new SkillSwipe(rs.getInt("victimid"), rs.getInt("skillid"),
                 * rs.getInt("skilllevel"), rs.getInt("category"), slot);
                 * ret.skillSwipe[slot] = ss; } rs.close(); ps.close();
                 */
                ps = con.prepareStatement("SELECT * FROM familiars WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("expiry") <= System.currentTimeMillis()) {
                        continue;
                    }
                    ret.familiars.put(rs.getInt("familiar"), new MonsterFamiliar(charid, rs.getInt("id"), rs.getInt("familiar"), rs.getLong("expiry"), rs.getString("name"), rs.getInt("fatigue"), rs.getByte("vitality")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM pokemon WHERE characterid = ? OR (accountid = ? AND active = 0)");
                ps.setInt(1, charid);
                ps.setInt(2, ret.accountid);
                rs = ps.executeQuery();
                position = 0;
                while (rs.next()) {
                    Battler b = new Battler(rs.getInt("level"), rs.getInt("exp"), charid, rs.getInt("monsterid"), rs.getString("name"), PokemonNature.values()[rs.getInt("nature")], rs.getInt("itemid"), rs.getByte("gender"), rs.getByte("hpiv"), rs.getByte("atkiv"), rs.getByte("defiv"), rs.getByte("spatkiv"), rs.getByte("spdefiv"), rs.getByte("speediv"), rs.getByte("evaiv"), rs.getByte("acciv"), rs.getByte("ability"));
                    if (b.getFamily() == null) {
                        continue;
                    }
                    if (rs.getInt("active") > 0 && position < 6 && rs.getInt("characterid") == charid) {
                        ret.battlers[position] = b;
                        position++;
                    } else {
                        ret.boxed.add(b);
                    }
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                final Map<Integer, Pair<Byte, Integer>> keyb = ret.keylayout.Layout();
                while (rs.next()) {
                    keyb.put(Integer.valueOf(rs.getInt("key")), new Pair<Byte, Integer>(rs.getByte("type"), rs.getInt("action")));
                }
                rs.close();
                ps.close();
                ret.keylayout.unchanged();

                ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[rs.getInt("locationtype")] = rs.getInt("map");
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<Integer>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(Integer.valueOf(rs.getInt("characterid_to")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `accid_to`,`when` FROM battlelog WHERE accid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                ret.lastmonthbattleids = new ArrayList<Integer>();
                while (rs.next()) {
                    ret.lastmonthbattleids.add(Integer.valueOf(rs.getInt("accid_to")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `itemId` FROM extendedSlots WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.extendedSlots.add(Integer.valueOf(rs.getInt("itemId")));
                }
                rs.close();
                ps.close();

                ret.buddylist.loadFromDb(charid);
                ret.storage = MapleStorage.loadStorage(ret.accountid);
                ret.cs = new CashShop(ret.accountid, charid, ret.getJob());

                ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int i = 0;
                while (rs.next()) {
                    ret.wishlist[i] = rs.getInt("sn");
                    i++;
                }
                while (i < 10) {
                    ret.wishlist[i] = 0;
                    i++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int r = 0;
                while (rs.next()) {
                    ret.rocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 10) {
                    ret.rocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM regrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.regrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 5) {
                    ret.regrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM hyperrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.hyperrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 13) {
                    ret.hyperrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * from stolen WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.stolenSkills.add(new Pair<Integer, Boolean>(rs.getInt("skillid"), rs.getInt("chosen") > 0));
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT * FROM imps WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.imps[r] = new MapleImp(rs.getInt("itemid"));
                    ret.imps[r].setLevel(rs.getByte("level"));
                    ret.imps[r].setState(rs.getByte("state"));
                    ret.imps[r].setCloseness(rs.getShort("closeness"));
                    ret.imps[r].setFullness(rs.getShort("fullness"));
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException("No mount data found on SQL column");
                }
                final Item mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (GameConstants.GMS ? -18 : -23));
                ret.mount = new MapleMount(ret, mount != null ? mount.getItemId() : 0, GameConstants.GMS ? 80001000 : ret.stats.getSkillByJob(1004, ret.job), rs.getByte("Fatigue"), rs.getByte("Level"), rs.getInt("Exp"));
                ps.close();
                rs.close();

                ret.stats.recalcLocalStats(true, ret);
            
            } else { // Not channel server
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(true, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                }
                ret.stats.recalcPVPRank(ret);
            }
        } catch (SQLException ess) {
            ess.printStackTrace();
            System.out.println("Failed to load character..");
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ess);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignore) {
            }
        }
        
        return ret;
    }

    public static void saveNewCharToDB(final MapleCharacter chr, final JobType type, short db) {
        Connection con = DatabaseConnection.getConnection();

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("INSERT INTO characters (level, str, dex, luk, `int`, hp, mp, maxhp, maxmp, sp, ap, skincolor, gender, job, hair, face, demonMarking, map, meso, party, buddyCapacity, pets, subcategory, mesosbank, accountid, name, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, chr.level); // Level
            final PlayerStats stat = chr.stats;
            ps.setInt(2, stat.getStr()); // Str
            ps.setInt(3, stat.getDex()); // Dex
            ps.setInt(4, stat.getInt()); // Int
            ps.setInt(5, stat.getLuk()); // Luk
            ps.setInt(6, stat.getHp()); // HP
            ps.setInt(7, stat.getMp());
            ps.setInt(8, stat.getMaxHp()); // MP
            ps.setInt(9, stat.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < chr.remainingSp.length; i++) {
                sps.append(chr.remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(10, sp.substring(0, sp.length() - 1));
            ps.setShort(11, (short) chr.remainingAp); // Remaining AP
            ps.setByte(12, chr.skinColor);
            ps.setByte(13, chr.gender);
            ps.setInt(14, chr.job);
            ps.setInt(15, chr.hair);
            ps.setInt(16, chr.face);
            ps.setInt(17, chr.demonMarking);
            if (db < 0 || db > 10) { //todo legend
                db = 0;
            }
            ps.setInt(18, db == 2 ? 3000600 : type.map);
            ps.setInt(19, chr.meso); // Meso
            ps.setInt(20, -1); // Party
            ps.setByte(21, chr.buddylist.getCapacity()); // Buddylist
            ps.setString(22, "-1,-1,-1");
            ps.setInt(23, db); //for now
            ps.setLong(24, chr.mesosbank);
            ps.setInt(25, chr.getAccountID());
            ps.setString(26, chr.name);
            ps.setByte(27, chr.world);
            
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chr.id = rs.getInt(1);
            } else {
                ps.close();
                rs.close();
                throw new DatabaseException("Inserting char failed.");
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (final MapleQuestStatus q : chr.quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);

            for (final Entry<Skill, SkillEntry> skill : chr.skills.entrySet()) {
                if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                    ps.setInt(2, skill.getKey().getId());
                    ps.setInt(3, skill.getValue().skillevel);
                    ps.setByte(4, skill.getValue().masterlevel);
                    ps.setLong(5, skill.getValue().expiration);
                    ps.execute();
                }
            }
            ps.close();

            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setByte(2, (byte) 96); // Eq
            ps.setByte(3, (byte) 96); // Use
            ps.setByte(4, (byte) 96); // Setup
            ps.setByte(5, (byte) 96); // ETC
            ps.setByte(6, (byte) 96); // Cash
            ps.execute();
            ps.close();

            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setByte(2, (byte) 1);
            ps.setInt(3, 0);
            ps.setByte(4, (byte) 0);
            ps.execute();
            ps.close();

              final int[] array1 = {2,3,4,5,6,7,8,13,16,17,18,19,20,21,23,24,25,26,27,29,31,33,34,35,37,38,39,40,41,43,44,45,46,47,48,49,50,56,57,59,60,61,62,63,64,65};
            final int[] array2 = {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,5,4,4,4,4,4,4,4,4,4,4,5,5,4,4,4,4,4,5,5,6,6,6,6,6,6,6};
            final int[] array3 = {10,12,13,18,24,21,29,33,8,5,0,4,28,31,1,25,19,14,15,52,2,26,17,11,3,20,27,16,23,9,50,51,6,32,30,22,7,53,54,100,101,102,103,104,105,106};

            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (int i = 0; i < array1.length; i++) {
                ps.setInt(2, array1[i]);
                ps.setInt(3, array2[i]);
                ps.setInt(4, array3[i]);
                ps.execute();
            }
            ps.close();

            List<Pair<Item, MapleInventoryType>> listing = new ArrayList<Pair<Item, MapleInventoryType>>();
            for (final MapleInventory iv : chr.inventory) {
                for (final Item item : iv.list()) {
                    listing.add(new Pair<Item, MapleInventoryType>(item, iv.getType()));
                }
            }
            ItemLoader.INVENTORY.saveItems(listing, con, chr.id);

            con.commit();
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
            System.err.println("[charsave] Error saving character data");
            try {
                con.rollback();
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                ex.printStackTrace();
                System.err.println("[charsave] Error Rolling Back");
            }
        } finally {
            try {
                if (pse != null) {
                    pse.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                e.printStackTrace();
                System.err.println("[charsave] Error going back to autocommit mode");
            }
        }
    }
    
    public static String getNameById(int id_) {
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		try {
			ps = con.prepareStatement("SELECT name FROM characters WHERE id = ? AND world = ?");
			ps.setInt(1, id_);
			ps.setInt(2, 0);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return "nobody"; //null
			}
			String name = rs.getString("name");
			rs.close();
			ps.close();
			return name;
		} catch (SQLException e) {
			System.out.print("ERROR" + e);
		}
		return "nobody";
	}
    
      public String getPartner() {
        return getNameById(marriageId);
    }

    public void saveToDB(boolean dc, boolean fromcs) {
        if (isClone()) {
            return;
        }
        Connection con = DatabaseConnection.getConnection();

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, demonMarking = ?, map = ?, meso = ?, hpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, pets = ?, subcategory = ?, marriageId = ?, currentrep = ?, totalrep = ?, gachexp = ?, fatigue = ?, charm = ?, charisma = ?, craft = ?, insight = ?, sense = ?, will = ?, totalwins = ?, totallosses = ?, pvpExp = ?, pvpPoints = ?, reborns = ?, apstorage = ?, Paragons = ?, vaccinated = ?, monsterkills = ?, mesosbank = ?, name = ? WHERE id = ?", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, level);
            ps.setInt(2, fame);
            ps.setInt(3, stats.getStr());
            ps.setInt(4, stats.getDex());
            ps.setInt(5, stats.getLuk());
            ps.setInt(6, stats.getInt());
            ps.setInt(7, (level >= 200 || (GameConstants.isKOC(job) && level >= 200)) && !isIntern() ? 0 : exp);
            ps.setInt(8, stats.getHp() < 1 ? 50 : stats.getHp());
            ps.setInt(9, stats.getMp());
            ps.setInt(10, stats.getMaxHp());
            ps.setInt(11, stats.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < remainingSp.length; i++) {
                sps.append(remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(12, sp.substring(0, sp.length() - 1));
            ps.setInt(13, remainingAp);
            ps.setByte(14, gmLevel);
            ps.setByte(15, skinColor);
            ps.setByte(16, gender);
            ps.setInt(17, job);
            ps.setInt(18, hair);
            ps.setInt(19, face);
            ps.setInt(20, demonMarking);
            if (!fromcs && map != null) {
                if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                    ps.setInt(21, map.getForcedReturnId());
                } else {
                    ps.setInt(21, stats.getHp() < 1 ? map.getReturnMapId() : map.getId());
                }
            } else {
                ps.setInt(21, mapid);
            }
            ps.setInt(22, meso);
            ps.setShort(23, hpApUsed);
            if (map == null) {
                ps.setByte(24, (byte) 0);
            } else {
                final MaplePortal closest = map.findClosestSpawnpoint(getTruePosition());
                ps.setByte(24, (byte) (closest != null ? closest.getId() : 0));
            }
            ps.setInt(25, party == null ? -1 : party.getId());
            ps.setShort(26, buddylist.getCapacity());
            final StringBuilder petz = new StringBuilder();
            int petLength = 0;
            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    pet.saveToDb();
                    petz.append(pet.getInventoryPosition());
                    petz.append(",");
                    petLength++;
                }
            }
            while (petLength < 3) {
                petz.append("-1,");
                petLength++;
            }
            final String petstring = petz.toString();
            ps.setString(27, petstring.substring(0, petstring.length() - 1));
            ps.setByte(28, subcategory);
            ps.setInt(29, marriageId);
            ps.setInt(30, currentrep);
            ps.setInt(31, totalrep);
            ps.setInt(32, gachexp);
            ps.setShort(33, fatigue);
            ps.setInt(34, traits.get(MapleTraitType.charm).getTotalExp());
            ps.setInt(35, traits.get(MapleTraitType.charisma).getTotalExp());
            ps.setInt(36, traits.get(MapleTraitType.craft).getTotalExp());
            ps.setInt(37, traits.get(MapleTraitType.insight).getTotalExp());
            ps.setInt(38, traits.get(MapleTraitType.sense).getTotalExp());
            ps.setInt(39, traits.get(MapleTraitType.will).getTotalExp());
            ps.setInt(40, totalWins);
            ps.setInt(41, totalLosses);
            ps.setInt(42, pvpExp);
            ps.setInt(43, pvpPoints);
            /*
             * Start of Custom Features
             */
            ps.setInt(44, reborns);
            ps.setInt(45, apstorage);
            ps.setInt(46, Paragons);
            ps.setByte(47, vaccinated);
            ps.setInt(48, monsterkills);
            ps.setLong(49, mesosbank);
            /*
             * End of Custom Features
             */
            ps.setString(50, name);
            ps.setInt(51, id);
             
            if (ps.executeUpdate() < 1) {
                ps.close();
                throw new DatabaseException("Character not in database (" + id + ")");
            }
            ps.close();
                deleteWhereCharacterId(con, "DELETE FROM stolen WHERE characterid = ?");
                for (Pair<Integer, Boolean> st : stolenSkills) {
                    ps = con.prepareStatement("INSERT INTO stolen (characterid, skillid, chosen) VALUES (?, ?, ?)");
                    ps.setInt(1, id);
                    ps.setInt(2, st.left);
                    ps.setInt(3, st.right ? 1 : 0);
                    ps.execute();
                    ps.close();
                }

            if (changed_skillmacros) {
                deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
                for (int i = 0; i < 5; i++) {
                    final SkillMacro macro = skillMacros[i];
                    if (macro != null) {
                        ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                        ps.setInt(1, id);
                        ps.setInt(2, macro.getSkill1());
                        ps.setInt(3, macro.getSkill2());
                        ps.setInt(4, macro.getSkill3());
                        ps.setString(5, macro.getName());
                        ps.setInt(6, macro.getShout());
                        ps.setInt(7, i);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (changed_pokemon) {
                ps = con.prepareStatement("DELETE FROM pokemon WHERE characterid = ? OR (accountid = ? AND active = 0)");
                ps.setInt(1, id);
                ps.setInt(2, accountid);
                ps.execute();
                ps.close();
                ps = con.prepareStatement("INSERT INTO pokemon (characterid, level, exp, monsterid, name, nature, active, accountid, itemid, gender, hpiv, atkiv, defiv, spatkiv, spdefiv, speediv, evaiv, acciv, ability) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                for (int i = 0; i < battlers.length; i++) {
                    final Battler macro = battlers[i];
                    if (macro != null) {
                        ps.setInt(1, id);
                        ps.setInt(2, macro.getLevel());
                        ps.setInt(3, macro.getExp());
                        ps.setInt(4, macro.getMonsterId());
                        ps.setString(5, macro.getName());
                        ps.setInt(6, macro.getNature().ordinal());
                        ps.setInt(7, 1);
                        ps.setInt(8, accountid);
                        ps.setInt(9, macro.getItem() == null ? 0 : macro.getItem().id);
                        ps.setByte(10, macro.getGender());
                        ps.setByte(11, macro.getIV(PokemonStat.HP));
                        ps.setByte(12, macro.getIV(PokemonStat.ATK));
                        ps.setByte(13, macro.getIV(PokemonStat.DEF));
                        ps.setByte(14, macro.getIV(PokemonStat.SPATK));
                        ps.setByte(15, macro.getIV(PokemonStat.SPDEF));
                        ps.setByte(16, macro.getIV(PokemonStat.SPEED));
                        ps.setByte(17, macro.getIV(PokemonStat.EVA));
                        ps.setByte(18, macro.getIV(PokemonStat.ACC));
                        ps.setByte(19, macro.getAbilityIndex());
                        ps.execute();
                    }
                }
                for (Battler macro : boxed) {
                    ps.setInt(1, id);
                    ps.setInt(2, macro.getLevel());
                    ps.setInt(3, macro.getExp());
                    ps.setInt(4, macro.getMonsterId());
                    ps.setString(5, macro.getName());
                    ps.setInt(6, macro.getNature().ordinal());
                    ps.setInt(7, 0);
                    ps.setInt(8, accountid);
                    ps.setInt(9, macro.getItem() == null ? 0 : macro.getItem().id);
                    ps.setByte(10, macro.getGender());
                    ps.setByte(11, macro.getIV(PokemonStat.HP));
                    ps.setByte(12, macro.getIV(PokemonStat.ATK));
                    ps.setByte(13, macro.getIV(PokemonStat.DEF));
                    ps.setByte(14, macro.getIV(PokemonStat.SPATK));
                    ps.setByte(15, macro.getIV(PokemonStat.SPDEF));
                    ps.setByte(16, macro.getIV(PokemonStat.SPEED));
                    ps.setByte(17, macro.getIV(PokemonStat.EVA));
                    ps.setByte(18, macro.getIV(PokemonStat.ACC));
                    ps.setByte(19, macro.getAbilityIndex());
                    ps.execute();
                }
                ps.close();
            }

            deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setByte(2, getInventory(MapleInventoryType.EQUIP).getSlotLimit());
            ps.setByte(3, getInventory(MapleInventoryType.USE).getSlotLimit());
            ps.setByte(4, getInventory(MapleInventoryType.SETUP).getSlotLimit());
            ps.setByte(5, getInventory(MapleInventoryType.ETC).getSlotLimit());
            ps.setByte(6, getInventory(MapleInventoryType.CASH).getSlotLimit());
            ps.execute();
            ps.close();

            saveInventory(con);

            if (changed_questinfo) {
                deleteWhereCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `customData`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (final Entry<Integer, String> q : questinfo.entrySet()) {
                    ps.setInt(2, q.getKey());
                    ps.setString(3, q.getValue());
                    ps.execute();
                }
                ps.close();
            }

            deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, id);
            for (final MapleQuestStatus q : quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

            if (changed_skills) {
                deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration) VALUES (?, ?, ?, ?, ?)");
                ps.setInt(1, id);

                for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                    if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                        ps.setInt(2, skill.getKey().getId());
                        ps.setInt(3, skill.getValue().skillevel);
                        ps.setByte(4, skill.getValue().masterlevel);
                        ps.setLong(5, skill.getValue().expiration);
                        ps.execute();
                    }
                }
                ps.close();
            }
             if (innerskill_changed) {
                if (innerSkills != null) {
                    deleteWhereCharacterId(con, "DELETE FROM inner_ability_skills WHERE player_id = ?");
                    ps = con.prepareStatement("INSERT INTO inner_ability_skills (player_id, skill_id, skill_level, max_level, rank) VALUES (?, ?, ?, ?, ?)");
                    ps.setInt(1, id);

                    for (int i = 0; i < innerSkills.size(); ++i) {
                        ps.setInt(2, innerSkills.get(i).getSkillId());
                        ps.setInt(3, innerSkills.get(i).getSkillLevel());
                        ps.setInt(4, innerSkills.get(i).getMaxLevel());
                        ps.setInt(5, innerSkills.get(i).getRank());
                        ps.executeUpdate();
                        innerskill_changed = false;
                    }
                    ps.close();
                }
            }

            List<MapleCoolDownValueHolder> cd = getCooldowns();
            if (dc && cd.size() > 0) {
                ps = con.prepareStatement("INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                ps.setInt(1, getId());
                for (final MapleCoolDownValueHolder cooling : cd) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.execute();
                }
                ps.close();
            }


            if (changed_savedlocations) {
                deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (final SavedLocationType savedLocationType : SavedLocationType.values()) {
                    if (savedLocations[savedLocationType.getValue()] != -1) {
                        ps.setInt(2, savedLocationType.getValue());
                        ps.setInt(3, savedLocations[savedLocationType.getValue()]);
                        ps.execute();
                    }
                }
                ps.close();
            }

            if (changed_achievements) {
                ps = con.prepareStatement("DELETE FROM achievements WHERE accountid = ?");
                ps.setInt(1, accountid);
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("INSERT INTO achievements(charid, achievementid, accountid) VALUES(?, ?, ?)");
                for (Integer achid : finishedAchievements) {
                    ps.setInt(1, id);
                    ps.setInt(2, achid);
                    ps.setInt(3, accountid);
                    ps.execute();
                }
                ps.close();
            }

            if (changed_reports) {
                deleteWhereCharacterId(con, "DELETE FROM reports WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO reports VALUES(DEFAULT, ?, ?, ?)");
                for (Entry<ReportType, Integer> achid : reports.entrySet()) {
                    ps.setInt(1, id);
                    ps.setByte(2, achid.getKey().i);
                    ps.setInt(3, achid.getValue());
                    ps.execute();
                }
                ps.close();
            }

            if (buddylist.changed()) {
                deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (BuddylistEntry entry : buddylist.getBuddies()) {
                    ps.setInt(2, entry.getCharacterId());
                    ps.setInt(3, entry.isVisible() ? 0 : 1);
                    ps.execute();
                }
                ps.close();
                buddylist.setChanged(false);
            }

            ps = con.prepareStatement("UPDATE accounts SET `nxCredit` = ?, `ACash` = ?, `mPoints` = ?, `points` = ?, `vpoints` = ? WHERE id = ?");
            ps.setInt(1, nxcredit);
            ps.setInt(2, acash);
            ps.setInt(3, maplepoints);
            ps.setInt(4, points);
            ps.setInt(5, vpoints);
            ps.setInt(6, client.getAccID());
            ps.executeUpdate();
            ps.close();

            if (storage != null) {
                storage.saveToDB();
            }
            if (cs != null) {
                cs.save();
            }
            PlayerNPC.updateByCharId(this);
            keylayout.saveKeys(id);
            mount.saveMount(id);
            monsterbook.saveCards(accountid);

            deleteWhereCharacterId(con, "DELETE FROM familiars WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO familiars (characterid, expiry, name, fatigue, vitality, familiar) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            for (MonsterFamiliar f : familiars.values()) {
                ps.setLong(2, f.getExpiry());
                ps.setString(3, f.getName());
                ps.setInt(4, f.getFatigue());
                ps.setByte(5, f.getVitality());
                ps.setInt(6, f.getFamiliar());
                ps.executeUpdate();
            }
            ps.close();

            deleteWhereCharacterId(con, "DELETE FROM imps WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO imps (characterid, itemid, closeness, fullness, state, level) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            for (int i = 0; i < imps.length; i++) {
                if (imps[i] != null) {
                    ps.setInt(2, imps[i].getItemId());
                    ps.setShort(3, imps[i].getCloseness());
                    ps.setShort(4, imps[i].getFullness());
                    ps.setByte(5, imps[i].getState());
                    ps.setByte(6, imps[i].getLevel());
                    ps.executeUpdate();
                }
            }
            ps.close();
            if (changed_wishlist) {
                deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
                for (int i = 0; i < getWishlistSize(); i++) {
                    ps = con.prepareStatement("INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ");
                    ps.setInt(1, getId());
                    ps.setInt(2, wishlist[i]);
                    ps.execute();
                    ps.close();
                }
            }
            if (changed_trocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
                for (int i = 0; i < rocks.length; i++) {
                    if (rocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, rocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }

            if (changed_regrocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM regrocklocations WHERE characterid = ?");
                for (int i = 0; i < regrocks.length; i++) {
                    if (regrocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO regrocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, regrocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (changed_hyperrocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM hyperrocklocations WHERE characterid = ?");
                for (int i = 0; i < hyperrocks.length; i++) {
                    if (hyperrocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO hyperrocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, hyperrocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (changed_extendedSlots) {
                deleteWhereCharacterId(con, "DELETE FROM extendedSlots WHERE characterid = ?");
                for (int i : extendedSlots) {
                    if (getInventory(MapleInventoryType.ETC).findById(i) != null) { //just in case
                        ps = con.prepareStatement("INSERT INTO extendedSlots(characterid, itemId) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, i);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (namechange != null) {
                ps = con.prepareStatement("UPDATE characters SET `name` = ? WHERE id = ?");
                ps.setString(1, namechange);
                ps.setInt(2, id);
                ps.execute();
                ps.close();
                namechange = null;
            }
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_pokemon = false;
            changed_questinfo = false;
            changed_achievements = false;
            changed_extendedSlots = false;
            changed_skills = false;
            changed_reports = false;
            con.commit();
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
            System.err.println(MapleClient.getLogMessage(this, "[charsave] Error saving character data") + e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                System.err.println(MapleClient.getLogMessage(this, "[charsave] Error Rolling Back") + e);
            }
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (pse != null) {
                    pse.close();
                }
                if (rs != null) {
                    rs.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                System.err.println(MapleClient.getLogMessage(this, "[charsave] Error going back to autocommit mode") + e);
            }
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        deleteWhereCharacterId(con, sql, id);
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    public static void deleteWhereCharacterId_NoLock(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.execute();
        ps.close();
    }

    public void saveInventory(final Connection con) throws SQLException {
        List<Pair<Item, MapleInventoryType>> listing = new ArrayList<Pair<Item, MapleInventoryType>>();
        for (final MapleInventory iv : inventory) {
            for (final Item item : iv.list()) {
                listing.add(new Pair<Item, MapleInventoryType>(item, iv.getType()));
            }
        }
        if (con != null) {
            ItemLoader.INVENTORY.saveItems(listing, con, id);
        } else {
            ItemLoader.INVENTORY.saveItems(listing, id);
        }
    }

    public final PlayerStats getStat() {
        return stats;
    }

    public final void QuestInfoPacket(final tools.data.MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(questinfo.size()); // // Party Quest data (quest needs to be added in the quests list)

        for (final Entry<Integer, String> q : questinfo.entrySet()) {
            mplew.writeShort(q.getKey());
            mplew.writeMapleAsciiString(q.getValue() == null ? "" : q.getValue());
        }
    }

    public final void updateInfoQuest(final int questid, final String data) {
        questinfo.put(questid, data);
        changed_questinfo = true;
        client.getSession().write(InfoPacket.updateInfoQuest(questid, data));
    }

    public final String getInfoQuest(final int questid) {
        if (questinfo.containsKey(questid)) {
            return questinfo.get(questid);
        }
        return "";
    }

    public final int getNumQuest() {
        int i = 0;
        for (final MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !(q.isCustom())) {
                i++;
            }
        }
        return i;
    }

    public final byte getQuestStatus(final int quest) {
        final MapleQuest qq = MapleQuest.getInstance(quest);
        if (getQuestNoAdd(qq) == null) {
            return 0;
        }
        return getQuestNoAdd(qq).getStatus();
    }

    public final MapleQuestStatus getQuest(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, (byte) 0);
        }
        return quests.get(quest);
    }

    public final void setQuestAdd(final MapleQuest quest, final byte status, final String customData) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus stat = new MapleQuestStatus(quest, status);
            stat.setCustomData(customData);
            quests.put(quest, stat);
        }
    }

    public final MapleQuestStatus getQuestNAdd(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus status = new MapleQuestStatus(quest, (byte) 0);
            quests.put(quest, status);
            return status;
        }
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestNoAdd(final MapleQuest quest) {
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestRemove(final MapleQuest quest) {
        return quests.remove(quest);
    }

    public final void updateQuest(final MapleQuestStatus quest) {
        updateQuest(quest, false);
    }

    public final void updateQuest(final MapleQuestStatus quest, final boolean update) {
        quests.put(quest.getQuest(), quest);
        if (!(quest.isCustom())) {
            client.getSession().write(InfoPacket.updateQuest(quest));
            if (quest.getStatus() == 1 && !update) {
                client.getSession().write(CField.updateQuestInfo(this, quest.getQuest().getId(), quest.getNpc(), (byte) 10));
            }
        }
    }

    public final Map<Integer, String> getInfoQuest_Map() {
        return questinfo;
    }

    public final Map<MapleQuest, MapleQuestStatus> getQuest_Map() {
        return quests;
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : Integer.valueOf(mbsvh.value);
    }

    public final Integer getBuffedSkill_X(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getX();
    }

    public final Integer getBuffedSkill_Y(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getY();
    }

    public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null || mbsvh.effect == null) {
            return false;
        }
        return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public int getBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh == null ? -1 : mbsvh.effect.getSourceId();
    }

    public int getTrueBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh == null ? -1 : (mbsvh.effect.isSkill() ? mbsvh.effect.getSourceId() : -mbsvh.effect.getSourceId());
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[GameConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setSchedule(MapleBuffStat effect, ScheduledFuture<?> sched) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.schedule.cancel(false);
        mbsvh.schedule = sched;
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : Long.valueOf(mbsvh.startTime);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : mbsvh.effect;
    }

    public void doDragonBlood() {
        final MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.DRAGONBLOOD);
        if (bloodEffect == null) {
            lastDragonBloodTime = 0;
            return;
        }
        prepareDragonBlood();
        if (stats.getHp() - bloodEffect.getX() <= 1) {
            cancelBuffStats(MapleBuffStat.DRAGONBLOOD);
           
        } else {
            addHP(-bloodEffect.getX());
            client.getSession().write(EffectPacket.showOwnBuffEffect(bloodEffect.getSourceId(), 7, getLevel(), bloodEffect.getLevel()));
            map.broadcastMessage(MapleCharacter.this, EffectPacket.showBuffeffect(getId(), bloodEffect.getSourceId(), 7, getLevel(), bloodEffect.getLevel()), false);
        }
    }

    public final boolean canBlood(long now) {
        return lastDragonBloodTime > 0 && lastDragonBloodTime + 4000 < now;
    }

    private void prepareDragonBlood() {
        lastDragonBloodTime = System.currentTimeMillis();
    }

    public void doRecovery() {
        MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.RECOVERY);
        if (bloodEffect == null) {
            bloodEffect = getStatForBuff(MapleBuffStat.MECH_CHANGE);
            if (bloodEffect == null) {
                lastRecoveryTime = 0;
                return;
            } else if (bloodEffect.getSourceId() == 35121005) {
                prepareRecovery();
                if (stats.getMp() < bloodEffect.getU()) {
                    cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                    cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
                } else {
                    addMP(-bloodEffect.getU());
                }
            }
        } else {
            prepareRecovery();
            if (stats.getHp() >= stats.getCurrentMaxHp()) {
                cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
            } else {
                healHP(bloodEffect.getX());
            }
        }
    }

    public final boolean canRecover(long now) {
        return lastRecoveryTime > 0 && lastRecoveryTime + 5000 < now;
    }

    private void prepareRecovery() {
        lastRecoveryTime = System.currentTimeMillis();
    }

    public void startMapTimeLimitTask(int time, final MapleMap to) {
        if (time <= 0) { //jail
            time = 1;
        }
        client.getSession().write(CField.getClock(time));
        final MapleMap ourMap = getMap();
        time *= 1000;
        mapTimeLimitTask = MapTimer.getInstance().register(new Runnable() {

            @Override
            public void run() {
                if (ourMap.getId() == GameConstants.JAIL) {
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME)).setCustomData(String.valueOf(System.currentTimeMillis()));
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData("0"); //release them!
                }
                changeMap(to, to.getPortal(0));
            }
        }, time, time);
    }

    public boolean canDOT(long now) {
        return lastDOTTime > 0 && lastDOTTime + 8000 < now;
    }

    public boolean hasDOT() {
        return dotHP > 0;
    }

    public void doDOT() {
        addHP(-(dotHP * 4));
        dotHP = 0;
        lastDOTTime = 0;
    }

    public void setDOT(int d, int source, int sourceLevel) {
        this.dotHP = d;
        addHP(-(dotHP * 4));
        map.broadcastMessage(CField.getPVPMist(id, source, sourceLevel, d));
        lastDOTTime = System.currentTimeMillis();
    }

    public void startFishingTask() {
        cancelFishingTask();
        lastFishingTime = System.currentTimeMillis();
    }

    public boolean canFish(long now) {
        return lastFishingTime > 0 && lastFishingTime + GameConstants.getFishingTime(false, isGM()) < now; // custom occupations?
    }

    public void doFish(long now) {
        lastFishingTime = now;
        if (client == null || client.getPlayer() == null || !client.isReceiving() || (!haveItem(2270008, 1, false, true)) || !GameConstants.isFishingMap(getMapId()) || chair <= 0) {
            cancelFishingTask();
            return;
        }
        MapleInventoryManipulator.removeById(client, MapleInventoryType.USE, 2270008, 1, false, false);
        boolean passed = false;
        while (!passed) {
            int randval = RandomRewards.getFishingReward();
            switch (randval) {
                case 0: // Meso
                    final int money = Randomizer.rand(10, 50000);
                    gainMeso(money, true);
                    passed = true;
                    break;
                case 1: // EXP
                    final int experi = Math.min(Randomizer.nextInt(Math.abs(getNeededExp() / 200) + 1), 500000);
                    gainExp(experi, true, false, true);
                    passed = true;
                    break;
                default:
                    if (MapleItemInformationProvider.getInstance().itemExists(randval)) {
                        MapleInventoryManipulator.addById(client, randval, (short) 1, "Fishing" + " on " + FileoutputUtil.CurrentReadable_Date());
                        passed = true;
                    }
                    break;
            }
        }
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
            mapTimeLimitTask = null;
        }
    }
    
    public int getNeededParagon() {
        return GameConstants.getExpNeededForParagon(Paragons);
    }
    
     public long getNeedReborn() {
        return GameConstants.getRebornExpNeededForLevel(level);
    }

    public int getNeededExp() {
        return GameConstants.getExpNeededForLevel(level);
    }

    public void cancelFishingTask() {
        lastFishingTime = 0;
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, int from) {
        registerEffect(effect, starttime, schedule, effect.getStatups(), false, effect.getDuration(), from);
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, Map<MapleBuffStat, Integer> statups, boolean silent, final int localDuration, final int cid) {
        if (effect.isHide()) {
            map.broadcastMessage(this, CField.removePlayerFromMap(getId()), false);
        } else if (effect.isDragonBlood()) {
            prepareDragonBlood();
        } else if (effect.isRecovery()) {
            prepareRecovery();
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isMonsterRiding_()) {
            getMount().startSchedule();
        }
        int clonez = 0;
        for (Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            if (statup.getKey() == MapleBuffStat.ILLUSION) {
                clonez = statup.getValue();
            }
            int value = statup.getValue().intValue();
            if (statup.getKey() == MapleBuffStat.MONSTER_RIDING) {
                if (effect.getSourceId() == 5221006 && battleshipHP <= 0) {
                    battleshipHP = maxBattleshipHP(effect.getSourceId()); //copy this as well
                }
                removeFamiliar();
            }
            effects.put(statup.getKey(), new MapleBuffStatValueHolder(effect, starttime, schedule, value, localDuration, cid));
        }
        if (clonez > 0) {
            int cloneSize = Math.max(getNumClones(), getCloneSize());
            if (clonez > cloneSize) { //how many clones to summon
                for (int i = 0; i < clonez - cloneSize; i++) { //1-1=0
                    cloneLook();
                }
            }
        }
        if (!silent) {
            stats.recalcLocalStats(this);
        }
        //System.out.println("Effect registered. Effect: " + effect.getSourceId());
    }

    public List<MapleBuffStat> getBuffStats(final MapleStatEffect effect, final long startTime) {
        final List<MapleBuffStat> bstats = new ArrayList<MapleBuffStat>();
        final Map<MapleBuffStat, MapleBuffStatValueHolder> allBuffs = new EnumMap<MapleBuffStat, MapleBuffStatValueHolder>(effects);
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : allBuffs.entrySet()) {
            final MapleBuffStatValueHolder mbsvh = stateffect.getValue();
            if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime || stateffect.getKey().canStack())) {
                bstats.add(stateffect.getKey());
            }
        }
        return bstats;
    }

    private boolean deregisterBuffStats(List<MapleBuffStat> stats) {
        boolean clonez = false;
        List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<MapleBuffStatValueHolder>(stats.size());
        for (MapleBuffStat stat : stats) {
            final MapleBuffStatValueHolder mbsvh = effects.remove(stat);
            if (mbsvh != null) {
                boolean addMbsvh = true;
                for (MapleBuffStatValueHolder contained : effectsToCancel) {
                    if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                        addMbsvh = false;
                    }
                }
                if (addMbsvh) {
                    effectsToCancel.add(mbsvh);
                }
                if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.PUPPET || stat == MapleBuffStat.REAPER || stat == MapleBuffStat.BEHOLDER || stat == MapleBuffStat.DAMAGE_BUFF || stat == MapleBuffStat.RAINING_MINES || stat == MapleBuffStat.ANGEL_ATK) {
                    final int summonId = mbsvh.effect.getSourceId();
                    final List<MapleSummon> toRemove = new ArrayList<MapleSummon>();
                    visibleMapObjectsLock.writeLock().lock(); //We need to lock this later on anyway so do it now to prevent deadlocks.
                    summonsLock.writeLock().lock();
                    try {
                        for (MapleSummon summon : summons) {
                            if (summon.getSkill() == summonId || (stat == MapleBuffStat.RAINING_MINES && summonId == 33101008) || (summonId == 35121009 && summon.getSkill() == 35121011) || ((summonId == 86 || summonId == 88 || summonId == 91) && summon.getSkill() == summonId + 999) || ((summonId == 1085 || summonId == 1087 || summonId == 1090 || summonId == 1179) && summon.getSkill() == summonId - 999)) { //removes bots n tots
                                map.broadcastMessage(SummonPacket.removeSummon(summon, true));
                                map.removeMapObject(summon);
                                visibleMapObjects.remove(summon);
                                toRemove.add(summon);
                            }
                        }
                        for (MapleSummon s : toRemove) {
                            summons.remove(s);
                        }
                    } finally {
                        summonsLock.writeLock().unlock();
                        visibleMapObjectsLock.writeLock().unlock(); //lolwut
                    }
                    if (summonId == 3111005 || summonId == 3211005) {
                        cancelEffectFromBuffStat(MapleBuffStat.SPIRIT_LINK);
                    }
                } else if (stat == MapleBuffStat.DRAGONBLOOD) {
                    lastDragonBloodTime = 0;
                } else if (stat == MapleBuffStat.RECOVERY || mbsvh.effect.getSourceId() == 35121005) {
                    lastRecoveryTime = 0;
                } else if (stat == MapleBuffStat.HOMING_BEACON || stat == MapleBuffStat.ARCANE_AIM) {
                    linkMobs.clear();
                } else if (stat == MapleBuffStat.ILLUSION) {
                    disposeClones();
                    clonez = true;
                }
            }
        }
        for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
            if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).size() == 0) {
                if (cancelEffectCancelTasks.schedule != null) {
                    cancelEffectCancelTasks.schedule.cancel(false);
                }
            }
        }
        return clonez;
    }

    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the
     * Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime) {
        if (effect == null) {
            return;
        }
        cancelEffect(effect, overwrite, startTime, effect.getStatups());
    }

    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime, Map<MapleBuffStat, Integer> statups) {
        if (effect == null) {
            return;
        }
        List<MapleBuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            buffstats = new ArrayList<MapleBuffStat>(statups.keySet());
        }
        if (buffstats.size() <= 0) {
            return;
        }
        if (effect.isInfinity() && getBuffedValue(MapleBuffStat.INFINITY) != null) { //before
            int duration = Math.max(effect.getDuration(), effect.alchemistModifyVal(this, effect.getDuration(), false));
            final long start = getBuffedStarttime(MapleBuffStat.INFINITY);
            duration += (int) ((start - System.currentTimeMillis()));
            if (duration > 0) {
                final int neworbcount = getBuffedValue(MapleBuffStat.INFINITY) + effect.getDamage();
                final Map<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                stat.put(MapleBuffStat.INFINITY, neworbcount);
                setBuffedValue(MapleBuffStat.INFINITY, neworbcount);
                client.getSession().write(BuffPacket.giveBuff(effect.getSourceId(), duration, stat, effect));
                addHP((int) (effect.getHpR() * this.stats.getCurrentMaxHp()));
                addMP((int) (effect.getMpR() * this.stats.getCurrentMaxMp(this.getJob())));
                setSchedule(MapleBuffStat.INFINITY, BuffTimer.getInstance().schedule(new CancelEffectAction(this, effect, start, stat), effect.alchemistModifyVal(this, 4000, false)));
                return;
            }
        }
        final boolean clonez = deregisterBuffStats(buffstats);
        if (effect.isMagicDoor()) {
            // remove for all on maps
            if (!getDoors().isEmpty()) {
                removeDoor();
                silentPartyUpdate();
            }
        } else if (effect.isMechDoor()) {
            if (!getMechDoors().isEmpty()) {
                removeMechDoor();
            }
        } else if (effect.isMonsterRiding_()) {
            getMount().cancelSchedule();
        } else if (effect.isMonsterRiding()) {
            cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (effect.isAranCombo()) {
            combo = 1;
        }
        // check if we are still logged in o.o
        cancelPlayerBuffs(buffstats, overwrite);
        if (!overwrite) {
            if (effect.isHide() && client.getChannelServer().getPlayerStorage().getCharacterById(this.getId()) != null) { //Wow this is so fking hacky...
                map.broadcastMessage(this, CField.spawnPlayerMapobject(this), false);

                for (final MaplePet pet : pets) {
                    if (pet.getSummoned()) {
                        map.broadcastMessage(this, PetPacket.showPet(this, pet, false, false), false);
                    }
                }
                for (final WeakReference<MapleCharacter> chr : clones) {
                    if (chr.get() != null) {
                        map.broadcastMessage(chr.get(), CField.spawnPlayerMapobject(chr.get()), false);
                    }
                }
            }
        }
        if (effect.getSourceId() == 35121005 && !overwrite) { //when siege 2 deactivates, missile re-activates
            SkillFactory.getSkill(35121013).getEffect(getTotalSkillLevel(35121013)).applyTo(this);
        }
        if (!clonez) {
            for (WeakReference<MapleCharacter> chr : clones) {
                if (chr.get() != null) {
                    chr.get().cancelEffect(effect, overwrite, startTime);
                }
            }
        }
        //System.out.println("Effect deregistered. Effect: " + effect.getSourceId());
    }

    public void cancelBuffStats(MapleBuffStat... stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList, false);
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        if (effects.get(stat) != null) {
            cancelEffect(effects.get(stat).effect, false, -1);
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat, int from) {
        if (effects.get(stat) != null && effects.get(stat).cid == from) {
            cancelEffect(effects.get(stat).effect, false, -1);
        }
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats, boolean overwrite) {
        boolean write = client != null && client.getChannelServer() != null && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
        if (buffstats.contains(MapleBuffStat.HOMING_BEACON)) {
            client.getSession().write(BuffPacket.cancelHoming());
        } else {
            if (overwrite) {
                List<MapleBuffStat> z = new ArrayList<MapleBuffStat>();
                for (MapleBuffStat s : buffstats) {
                    if (s.canStack()) {
                        z.add(s);
                    }
                }
                if (z.size() > 0) {
                    buffstats = z;
                } else {
                    return; //don't write anything
                }
            } else if (write) {
                stats.recalcLocalStats(this);
            }
            client.getSession().write(BuffPacket.cancelBuff(buffstats));
            map.broadcastMessage(this, BuffPacket.cancelForeignBuff(getId(), buffstats), false);
        }
    }

    public void dispel() {
        if (!isHidden()) {
            final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
            for (MapleBuffStatValueHolder mbsvh : allBuffs) {
                if (mbsvh.effect.isSkill() && mbsvh.schedule != null && !mbsvh.effect.isMorph() && !mbsvh.effect.isGmBuff() && !mbsvh.effect.isMonsterRiding() && !mbsvh.effect.isMechChange() && !mbsvh.effect.isEnergyCharge() && !mbsvh.effect.isAranCombo()) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            }
        }
    }

    public void dispelSkill(int skillid) {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public void dispelSummons() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSummonMovementType() != null) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void dispelBuff(int skillid) {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public void cancelAllBuffs_() {
        effects.clear();
    }

    public void cancelAllBuffs() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    public void cancelMorphs() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            switch (mbsvh.effect.getSourceId()) {
                case 5111005:
                case 5121003:
                case 15111002:
                case 13111005:
                    return; // Since we can't have more than 1, save up on loops
                default:
                    if (mbsvh.effect.isMorph()) {
                        if (MapConstants.isStorylineMap(getMapId())) {
                            return;
                        }
                        if (mbsvh.effect.isMorph()) {
                            disposeClones();
                            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                            continue;
                        }
                    }
            }
        }
    }

    public int getMorphState() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMorph()) {
                return mbsvh.effect.getSourceId();
            }
        }
        return -1;
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        if (buffs == null) {
            return;
        }
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime, mbsvh.localDuration, mbsvh.statup, mbsvh.cid);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        final List<PlayerBuffValueHolder> ret = new ArrayList<PlayerBuffValueHolder>();
        final Map<Pair<Integer, Byte>, Integer> alreadyDone = new HashMap<Pair<Integer, Byte>, Integer>();
        final LinkedList<Entry<MapleBuffStat, MapleBuffStatValueHolder>> allBuffs = new LinkedList<Entry<MapleBuffStat, MapleBuffStatValueHolder>>(effects.entrySet());
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : allBuffs) {
            final Pair<Integer, Byte> key = new Pair<Integer, Byte>(mbsvh.getValue().effect.getSourceId(), mbsvh.getValue().effect.getLevel());
            if (alreadyDone.containsKey(key)) {
                ret.get(alreadyDone.get(key)).statup.put(mbsvh.getKey(), mbsvh.getValue().value);
            } else {
                alreadyDone.put(key, ret.size());
                final EnumMap<MapleBuffStat, Integer> list = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
                list.put(mbsvh.getKey(), mbsvh.getValue().value);
                ret.add(new PlayerBuffValueHolder(mbsvh.getValue().startTime, mbsvh.getValue().effect, list, mbsvh.getValue().localDuration, mbsvh.getValue().cid));
            }
        }
        return ret;
    }

    public void cancelMagicDoor() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<MapleBuffStatValueHolder>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public int getSkillLevel(int skillid) {
        return getSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getTotalSkillLevel(int skillid) {
        return getTotalSkillLevel(SkillFactory.getSkill(skillid));
    }

    
    public final void handleEnergyCharge(final int skillid, final int targets) {
        final Skill echskill = SkillFactory.getSkill(skillid);
        final int skilllevel = getTotalSkillLevel(echskill);
        if (skilllevel > 0) {
            final MapleStatEffect echeff = echskill.getEffect(skilllevel);
            if (targets > 0) {
                if (getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null) {
                //    echeff.applyEnergyBuff(this, true); // Infinity time
                } else {
                    Integer energyLevel = getBuffedValue(MapleBuffStat.ENERGY_CHARGE);
                    //TODO: bar going down
                    if (energyLevel < 10000) {
                        energyLevel += (echeff.getX() * targets);

                        client.getSession().write(EffectPacket.showOwnBuffEffect(skillid, 2, getLevel(), skilllevel));
                        map.broadcastMessage(this, EffectPacket.showBuffeffect(id, skillid, 2, getLevel(), skilllevel), false);

                        if (energyLevel >= 10000) {
                            energyLevel = 10000;
                        }
                        client.getSession().write(BuffPacket.giveEnergyChargeTest(energyLevel, echeff.getDuration() / 1000));
                        setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(energyLevel));
                    } else if (energyLevel == 10000) {
                      //  echeff.applyEnergyBuff(this, false); // One with time
                        setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(10001));
                    }
                }
            }
        }
    }
 /*public final void handleEnergyCharge(int skillid, int targets) {
    Skill echskill = SkillFactory.getSkill(skillid);
    int skilllevel = getTotalSkillLevel(echskill);
    if (skilllevel > 0) {
      MapleStatEffect echeff = echskill.getEffect(skilllevel);
      if (targets > 0)
        if (getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null) {
          echeff.applyEnergyBuff(this, true, targets);
        } else {
          Integer energyLevel = getBuffedValue(MapleBuffStat.ENERGY_CHARGE);

          if (energyLevel.intValue() < 10000) {
            energyLevel = Integer.valueOf(energyLevel.intValue() + echeff.getX() * targets);

            this.client.getSession().write(CField.EffectPacket.showOwnBuffEffect(skillid, 2, getLevel(), skilllevel));
            this.map.broadcastMessage(this, CField.EffectPacket.showBuffeffect(this.id, skillid, 2, getLevel(), skilllevel), false);

            if (energyLevel.intValue() >= 10000) {
              energyLevel = Integer.valueOf(10000);
            }
            this.client.getSession().write(CWvsContext.BuffPacket.giveEnergyChargeTest(energyLevel.intValue(), echeff.getDuration() / 1000));
            setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(energyLevel.intValue()).intValue());
          } else if (energyLevel.intValue() == 10000) {
            echeff.applyEnergyBuff(this, false, targets);
            setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(10001).intValue());
          }
        }
    }
  } */

    public final void handleBattleshipHP(int damage) {
        if (damage < 0) {
            final MapleStatEffect effect = getStatForBuff(MapleBuffStat.MONSTER_RIDING);
            if (effect != null && effect.getSourceId() == 5221006) {
                battleshipHP += damage;
                client.getSession().write(CField.skillCooldown(5221999, battleshipHP / 10));
                if (battleshipHP <= 0) {
                    battleshipHP = 0;
                    client.getSession().write(CField.skillCooldown(5221006, effect.getCooldown(this)));
                    addCooldown(5221006, System.currentTimeMillis(), effect.getCooldown(this) * 1000);
                    cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                }
            }
        }
    }

    public final void handleOrbgain() {
        int orbcount = getBuffedValue(MapleBuffStat.COMBO);
        Skill combo;
        Skill advcombo;

        switch (getJob()) {
            case 1110:
            case 1111:
            case 1112:
                combo = SkillFactory.getSkill(11111001);
                advcombo = SkillFactory.getSkill(11110005);
                break;
            default:
                combo = SkillFactory.getSkill(1111002);
                advcombo = SkillFactory.getSkill(1120003);
                break;
        }

        MapleStatEffect ceffect = null;
        int advComboSkillLevel = getTotalSkillLevel(advcombo);
        if (advComboSkillLevel > 0) {
            ceffect = advcombo.getEffect(advComboSkillLevel);
        } else if (getSkillLevel(combo) > 0) {
            ceffect = combo.getEffect(getTotalSkillLevel(combo));
        } else {
            return;
        }

        if (orbcount < ceffect.getX() + 1) {
            int neworbcount = orbcount + 1;
            if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                if (neworbcount < ceffect.getX() + 1) {
                    neworbcount++;
                }
            }
            EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
            stat.put(MapleBuffStat.COMBO, neworbcount);
            setBuffedValue(MapleBuffStat.COMBO, neworbcount);
            int duration = ceffect.getDuration();
            duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));

            client.getSession().write(BuffPacket.giveBuff(combo.getId(), duration, stat, ceffect));
            map.broadcastMessage(this, BuffPacket.giveForeignBuff(getId(), stat, ceffect), false);
        }
    }

    public void handleOrbconsume(int howmany) {
        Skill combo;

        switch (getJob()) {
            case 1110:
            case 1111:
            case 1112:
                combo = SkillFactory.getSkill(11111001);
                break;
            default:
                combo = SkillFactory.getSkill(1111002);
                break;
        }
        if (getSkillLevel(combo) <= 0) {
            return;
        }
        MapleStatEffect ceffect = getStatForBuff(MapleBuffStat.COMBO);
        if (ceffect == null) {
            return;
        }
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<MapleBuffStat, Integer>(MapleBuffStat.class);
        stat.put(MapleBuffStat.COMBO, Math.max(1, getBuffedValue(MapleBuffStat.COMBO) - howmany));
        setBuffedValue(MapleBuffStat.COMBO, Math.max(1, getBuffedValue(MapleBuffStat.COMBO) - howmany));
        int duration = ceffect.getDuration();
        duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));

        client.getSession().write(BuffPacket.giveBuff(combo.getId(), duration, stat, ceffect));
        map.broadcastMessage(this, BuffPacket.giveForeignBuff(getId(), stat, ceffect), false);
    }

    public void silentEnforceMaxHpMp() {
        stats.setMp(stats.getMp(), this);
        stats.setHp(stats.getHp(), true, this);
    }

    public void enforceMaxHpMp() {
        Map<MapleStat, Integer> statups = new EnumMap<MapleStat, Integer>(MapleStat.class);
        if (stats.getMp() > stats.getCurrentMaxMp(this.getJob())) {
            stats.setMp(stats.getMp(), this);
            statups.put(MapleStat.MP, Integer.valueOf(stats.getMp()));
        }
        if (stats.getHp() > stats.getCurrentMaxHp()) {
            stats.setHp(stats.getHp(), this);
            statups.put(MapleStat.HP, Integer.valueOf(stats.getHp()));
        }
        if (statups.size() > 0) {
            client.getSession().write(CWvsContext.updatePlayerStats(statups, this));
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public byte getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public final String getBlessOfFairyOrigin() {
        return this.BlessOfFairy_Origin;
    }

    public final String getBlessOfEmpressOrigin() {
        return this.BlessOfEmpress_Origin;
    }
//the sp isnt fixed oh hold on

    public final short getLevel() {
        return level;
    }

    public final int getFame() {
        return fame;
    }

    public final int getFallCounter() {
        return fallcounter;
    }

    public final MapleClient getClient() {
        return client;
    }

    public final void setClient(final MapleClient client) {
        this.client = client;
    }

    public int getExp() {
        return exp;
    }
    

    public int getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp[GameConstants.getSkillBook(job)]; //default
    }

    public int getRemainingSp(final int skillbook) {
        return remainingSp[skillbook];
    }

    public int[] getRemainingSps() {
        return remainingSp;
    }

    public int getRemainingSpSize() {
        int ret = 0;
        for (int i = 0; i < remainingSp.length; i++) {
            if (remainingSp[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public short getHpApUsed() {
        return hpApUsed;
    }

    public boolean isHidden() {
        return getBuffSource(MapleBuffStat.DARKSIGHT) / 1000000 == 9;
    }

    public void setHpApUsed(short hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    public byte getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(byte skinColor) {
        this.skinColor = skinColor;
    }

    public short getJob() {
        return job;
    }

    public byte getGender() {
        return gender;
    }

    public int getHair() {
        return hair;
    }

    public int getFace() {
        return face;
    }

    public int getDemonMarking() {
        return demonMarking;
    }

    public void setDemonMarking(int mark) {
        this.demonMarking = mark;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setParagon(int Paragons) {
            this.Paragons = Paragons;
    }
    public void setExp(int exp) {
        this.exp = exp;
    }
    
    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public Point getOldPosition() {
        return old;
    }

    public void setOldPosition(Point x) {
        this.old = x;
    }

    public void setRemainingAp(int remainingAp) {
        this.remainingAp = remainingAp;
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp[GameConstants.getSkillBook(job)] = remainingSp; //default
    }

    public void setRemainingSp(int remainingSp, final int skillbook) {
        this.remainingSp[skillbook] = remainingSp;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public void setInvincible(boolean invinc) {
        invincible = invinc;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public CheatTracker getCheatTracker() {
        return anticheat;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
        getTrait(MapleTraitType.charm).addLocalExp(famechange);
        if (this.fame >= 10) {
            finishAchievement(7);
        }
    }

    public void updateFame() {
        updateSingleStat(MapleStat.FAME, this.fame);
    }

    public void changeMapBanish(final int mapid, final String portal, final String msg) {
        dropMessage(5, msg);
        final MapleMap map = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(map, map.getPortal(portal));
    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, CField.getWarpToMap(to, 0x80, this), null);
    }

    public void changeMap(final MapleMap to) {
        changeMapInternal(to, to.getPortal(0).getPosition(), CField.getWarpToMap(to, 0, this), to.getPortal(0));
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), null);
    }
       
    

    public void changeMapPortal(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), pto);
    }

  private void changeMapInternal(MapleMap to, final Point pos, byte[] warpPacket, final MaplePortal pto) {
        if (to == null) {
            return;
        }

        int nowmapid = map.getId();
        MapleMap NewTo = to;
        if (eventInstance != null) {
            eventInstance.changedMap(this, to.getId());
        }

        if (to.getId() == 105040300) {
            NewTo = client.getChannelServer().getMapFactory().getMap(105000000);
        }

        final boolean pyramid = pyramidSubway != null;
        if (map.getId() == nowmapid) {
            client.getSession().write(warpPacket);
            final boolean shouldChange = !isClone() && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
            final boolean shouldState = map.getId() == NewTo.getId();
            if (shouldChange && shouldState) {
                NewTo.setCheckStates(false);
            }
            map.removePlayer(this);
            if (shouldChange) {

                map = NewTo;
                setPosition(pos);
                NewTo.addPlayer(this);
                stats.relocHeal(this);
                if (shouldState) {
                    NewTo.setCheckStates(true);
                }
            }
        }
        if (pyramid && pyramidSubway != null) { //checks if they had pyramid before AND after changing
            pyramidSubway.onChangeMap(this, NewTo.getId());
        }
    }

    public void cancelChallenge() {
        if (challenge != 0 && client.getChannelServer() != null) {
            final MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(challenge);
            if (chr != null) {
                chr.dropMessage(6, getName() + " has denied your request.");
                chr.setChallenge(0);
            }
            dropMessage(6, "Denied the challenge.");
            challenge = 0;
        }
    }

    public void leaveMap(MapleMap map) {
        controlledLock.writeLock().lock();
        visibleMapObjectsLock.writeLock().lock();
        try {
            for (MapleMonster mons : controlled) {
                if (mons != null) {
                    mons.setController(null);
                    mons.setControllerHasAggro(false);
                    map.updateMonsterController(mons);
                }
            }
            controlled.clear();
            visibleMapObjects.clear();
        } finally {
            controlledLock.writeLock().unlock();
            visibleMapObjectsLock.writeLock().unlock();
        }
        if (chair != 0) {
            chair = 0;
        }
        clearLinkMid();
        cancelFishingTask();
        cancelChallenge();
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        cancelMapTimeLimitTask();
        if (getTrade() != null) {
            MapleTrade.cancelTrade(getTrade(), client, this);
        }
    }

    public void changeJob(short newJob) {
        try {
            cancelEffectFromBuffStat(MapleBuffStat.SHADOWPARTNER);
            this.job = newJob;
            updateSingleStat(MapleStat.JOB, newJob);
            if (!GameConstants.isBeginnerJob(newJob)) {
                if (GameConstants.isEvan(newJob) || GameConstants.isResist(newJob) || GameConstants.isMercedes(newJob) || GameConstants.isJett(newJob) || GameConstants.isPhantom(newJob) || GameConstants.isMihile(newJob)) {
                    if (GameConstants.isPhantom(job)) {
                        client.getSession().write(CField.updateCardStack(0));
                        resetRunningStack();
                    }
                    int changeSp = (newJob == 2200 || newJob == 2210 || newJob == 2211 || newJob == 2213 ? 3 : 5);
                    if (GameConstants.isResist(job) && newJob != 3100 && newJob != 3200 && newJob != 3300 && newJob != 3500) {
                        changeSp = 3;
                    }
                    remainingSp[GameConstants.getSkillBook(newJob)] += changeSp;
                    client.getSession().write(InfoPacket.getSPMsg((byte) changeSp, (short) newJob));
                } else {
                    remainingSp[GameConstants.getSkillBook(newJob)]++;
                    if (newJob % 10 >= 2) {
                        remainingSp[GameConstants.getSkillBook(newJob)] += 2;
                    }
                }
                if (newJob % 10 >= 1 && level >= 70) { //3rd job or higher. lucky for evans who get 80, 100, 120, 160 ap...
                    remainingAp += 5;
                    updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
                }
                if (!isGM()) {
                    resetStatsByJob(true);
                    if (!GameConstants.isEvan(newJob)) {
                        if (getLevel() > (newJob == 200 ? 8 : 10) && newJob % 100 == 0 && (newJob % 1000) / 100 > 0) { //first job
                            remainingSp[GameConstants.getSkillBook(newJob)] += 3 * (getLevel() - (newJob == 200 ? 8 : 10));
                        }
                    } else if (newJob == 2200) {
                        MapleQuest.getInstance(22100).forceStart(this, 0, null);
                        MapleQuest.getInstance(22100).forceComplete(this, 0);
                        expandInventory((byte) 1, 4);
                        expandInventory((byte) 2, 4);
                        expandInventory((byte) 3, 4);
                        expandInventory((byte) 4, 4);
                        client.getSession().write(NPCPacket.getEvanTutorial("UI/tutorial/evan/14/0"));
                        dropMessage(5, "The baby Dragon hatched and appears to have something to tell you. Click the baby Dragon to start a conversation.");
                    }
                }
                updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
            }

            int maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

            switch (job) {
                case 100: // Warrior
                case 1100: // Soul Master
                case 2100: // Aran
                case 3200:
                case 5100: // Mihile
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3100:
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3110:
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 200: // Magician
                case 2200: //evan
                case 2210: //evan
                    maxmp += Randomizer.rand(100, 150);
                    break;
                case 300: // Bowman
                case 400: // Thief
                case 500: // Pirate
                case 2300:
                case 3300:
                case 3500:
                    maxhp += Randomizer.rand(100, 150);
                    maxmp += Randomizer.rand(25, 50);
                    break;
                case 110: // Fighter
                case 120: // Page
                case 130: // Spearman
                case 1110: // Soul Master
                case 2110: // Aran
                case 3210:
                case 5110: // Mihile
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 210: // FP
                case 220: // IL
                case 230: // Cleric
                    maxmp += Randomizer.rand(400, 450);
                    break;
                case 310: // Bowman
                case 320: // Crossbowman
                case 410: // Assasin
                case 420: // Bandit
                case 430: // Semi Dualer
                case 510:
                case 520:
                case 530:
                case 2310:
                case 1310: // Wind Breaker
                case 1410: // Night Walker
                case 3310:
                case 3510:
                    maxhp += Randomizer.rand(200, 250);
                    maxhp += Randomizer.rand(150, 200);
                    break;
                case 900: // GM
                case 800: // Manager
                    maxhp += 99999;
                    maxmp += 99999;
                    break;
            }
            if (maxhp >= 99999) {
                maxhp = 99999;
            }
            if (maxmp >= 99999) {
                maxmp = 99999;
            }
            if (GameConstants.isDemon(job)) {
                maxmp = GameConstants.getMPByJob(job);
            }
            stats.setInfo(maxhp, maxmp, maxhp, maxmp);
            Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);
            statup.put(MapleStat.MAXHP, Integer.valueOf(maxhp));
            statup.put(MapleStat.MAXMP, Integer.valueOf(maxmp));
            statup.put(MapleStat.HP, Integer.valueOf(maxhp));
            statup.put(MapleStat.MP, Integer.valueOf(maxmp));
            characterCard.recalcLocalStats(this);
            stats.recalcLocalStats(this);
            client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
            map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 11), false);
            silentPartyUpdate();
            guildUpdate();
            familyUpdate();
            if (dragon != null) {
                map.broadcastMessage(CField.removeDragon(this.id));
                dragon = null;
            }
            baseSkills();
            if (newJob >= 2200 && newJob <= 2218) { //make new
                if (getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                    cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
                }
                makeDragon();
            }
            if (level == 10 && job == 3100){
                setShield(1099000);
                setWeapon(1322122);
                setRemainingAp((short) 10);
                setRemainingSp((short) 3);
                fakeRelog();
            }
            
            if (level == 10 && job == 5100) {
                setWeapon(1302000);
                setShield(1098000);
                setRemainingAp((short) 10);
                fakeRelog();
                
            }
            
            if (level == 10 && job == 2400) {
                setShield(1352100);
                setWeapon(1362001);
                setRemainingAp((short) 30);
                fakeRelog();
                
            }
            
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
    }

    public void baseSkills() {
        Map<Skill, SkillEntry> list = new HashMap<>();
        if (GameConstants.getJobNumber(job) >= 3) { //third job.
            List<Integer> skills = SkillFactory.getSkillsByJob(job);
            if (skills != null) {
                for (int i : skills) {
                    final Skill skil = SkillFactory.getSkill(i);
                    if (skil != null && !skil.isInvisible() && skil.isFourthJob() && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0 && skil.getMasterLevel() > 0) {
                        list.put(skil, new SkillEntry((byte) 0, (byte) skil.getMasterLevel(), SkillFactory.getDefaultSExpiry(skil))); //usually 10 master
                    } else if (skil != null && skil.getName() != null && skil.getName().contains("Maple Warrior") && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0) {
                        list.put(skil, new SkillEntry((byte) 0, (byte) 10, SkillFactory.getDefaultSExpiry(skil))); //hackish
                    }
                }

            }
        }
        Skill skil;
        if (job >= 2211 && job <= 2218) { // evan fix magic guard
            skil = SkillFactory.getSkill(22111001);
            if (skil != null) {
                if (getSkillLevel(skil) <= 0) { // no total
                    list.put(skil, new SkillEntry((byte) 0, (byte) 20, -1));
                    if (job >= 430 && job <= 434) { 
            final int[] ss0 = {4331002, 4330009, 4341004, 4341006, 4341007, 4341011, 4340013}; 
            for (int i : ss0) { 
                skil = SkillFactory.getSkill(i); 
                if (skil != null) { 
                    if (getSkillLevel(skil) <= 0) { // no total 
                        list.put(skil, new SkillEntry((byte) 0, (byte) 10, -1)); 
                    } 
                } 
            } 
            skil = SkillFactory.getSkill(4311003); 
            if (skil != null) { 
                if (getSkillLevel(skil) <= 0) { // no total 
                    list.put(skil, new SkillEntry((byte) -1, (byte) 5, -1)); 
                } 
            } 
            skil = SkillFactory.getSkill(4321006); 
            if (skil != null) { 
                if (getSkillLevel(skil) <= 0) { // no total 
                    list.put(skil, new SkillEntry((byte) -1, (byte) 5, -1)); 
                } 
            } 
        }  
                }
            }
        }
         if (job >= 430 && job <= 434) { 
            final int[] ss0 = {4331002, 4330009, 4341004, 4341006, 4341007, 4341011, 4340013}; 
            for (int i : ss0) { 
                skil = SkillFactory.getSkill(i); 
                if (skil != null) { 
                    if (getSkillLevel(skil) <= 0) { // no total 
                        list.put(skil, new SkillEntry((byte) 0, (byte) 10, -1)); 
                    } 
                } 
            } 
            skil = SkillFactory.getSkill(4311003); 
            if (skil != null) { 
                if (getSkillLevel(skil) <= 0) { // no total 
                    list.put(skil, new SkillEntry((byte) -1, (byte) 5, -1)); 
                } 
            } 
            skil = SkillFactory.getSkill(4321006); 
            if (skil != null) { 
                if (getSkillLevel(skil) <= 0) { // no total 
                    list.put(skil, new SkillEntry((byte) -1, (byte) 5, -1)); 
                } 
            } 
        }  
        
        if (GameConstants.isWildHunter(job)) {
            final int[] ss = {30001061, 30001062};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null) {
                    if (getSkillLevel(skil) <= 0) { // no total
                        list.put(skil, new SkillEntry((byte) 1, (byte) 1, -1));
                    }
                }
            }
        }
        if (GameConstants.isEvan(job)) {
            final int[] ss3 = {20010022};
            for (int i : ss3) {
                skil = SkillFactory.getSkill(i);
                if (skil != null) {
                    if (getSkillLevel(skil) <= 0) { // no total
                        list.put(skil, new SkillEntry((byte) 1, (byte) 1, -1));
                    }
                }
            }
        }
        
        
        
        
        
        if (GameConstants.isMercedes(job)) {
            final int[] ss = {20021000, 20021001, 20020002, 20020022, 20020109, 20021110, 20020111, 20020112};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null) {
                    if (getSkillLevel(skil) <= 0) { // no total
                        list.put(skil, new SkillEntry((byte) 1, (byte) 1, -1));
                    }
                }
            }
            skil = SkillFactory.getSkill(20021181);
            if (skil != null) {
                if (getSkillLevel(skil) <= 0) { // no total
                    list.put(skil, new SkillEntry((byte) -1, (byte) 0, -1));
                }
            }
            skil = SkillFactory.getSkill(20021166);
            if (skil != null) {
                if (getSkillLevel(skil) <= 0) { // no total
                    list.put(skil, new SkillEntry((byte) -1, (byte) 0, -1));
                }
            }
        }
        if (GameConstants.isDemon(job)) {
            final int[] ss1 = {30011000, 30011001, 30010002, 30010185, 30010112, 30010111, 30010110, 30010022, 30011109};
            for (int i : ss1) {
                skil = SkillFactory.getSkill(i);
                if (skil != null) {
                    if (getSkillLevel(skil) <= 0) { // no total
                        list.put(skil, new SkillEntry((byte) 1, (byte) 1, -1));
                    }
                }
            }
            final int[] ss2 = {30011170, 30011169, 30011168, 30011167, 30010166, 30010184, 30010183, 30010186};
            for (int i : ss2) {
                skil = SkillFactory.getSkill(i);
                if (skil != null) {
                    if (getSkillLevel(skil) <= 0) { // no total
                        list.put(skil, new SkillEntry((byte) -1, (byte) -1, -1));
                    }
                }
            }
        }
        if (!list.isEmpty()) {
            changeSkillsLevel(list);
        }
        //redemption for completed quests. holy fk. ex
	    /*
         * List<MapleQuestStatus> cq = getCompletedQuests(); for
         * (MapleQuestStatus q : cq) { for (MapleQuestAction qs :
         * q.getQuest().getCompleteActs()) { if (qs.getType() ==
         * MapleQuestActionType.skill) { for (Pair<Integer, Pair<Integer,
         * Integer>> skill : qs.getSkills()) { final Skill skil =
         * SkillFactory.getSkill(skill.left); if (skil != null &&
         * getSkillLevel(skil) <= skill.right.left && getMasterLevel(skil) <=
         * skill.right.right) { changeSkillLevel(skil, (byte)
         * (int)skill.right.left, (byte) (int)skill.right.right); } } } else if
         * (qs.getType() == MapleQuestActionType.item) { //skillbooks for
         * (MapleQuestAction.QuestItem item : qs.getItems()) { if (item.itemid /
         * 10000 == 228 && !haveItem(item.itemid,1)) { //skillbook //check if we
         * have the skill final Map<String, Integer> skilldata =
         * MapleItemInformationProvider.getInstance().getSkillStats(item.itemid);
         * if (skilldata != null) { byte i = 0; Skill finalSkill = null; Integer
         * skillID = 0; while (finalSkill == null) { skillID =
         * skilldata.get("skillid" + i); i++; if (skillID == null) { break; }
         * final Skill CurrSkill = SkillFactory.getSkill(skillID); if (CurrSkill
         * != null && CurrSkill.canBeLearnedBy(job) && getSkillLevel(CurrSkill)
         * <= 0 && getMasterLevel(CurrSkill) <= 0) { finalSkill = CurrSkill; } }
         * if (finalSkill != null) { //may as well give the skill
         * changeSkillLevel(finalSkill, (byte) 0, (byte)10);
         * //MapleInventoryManipulator.addById(client, item.itemid, item.count);
         * } } } } } } }
         */
    }

    public void makeDragon() {
        dragon = new MapleDragon(this);
        map.broadcastMessage(CField.spawnDragon(dragon));
    }

    public MapleDragon getDragon() {
        return dragon;
    }

    public void gainAp(short ap) {
        this.remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void gainSP(int sp) {
        this.remainingSp[GameConstants.getSkillBook(job)] += sp; //default
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
        client.getSession().write(InfoPacket.getSPMsg((byte) sp, (short) job));
    }

    public void gainSP(int sp, final int skillbook) {
        this.remainingSp[skillbook] += sp; //default
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
        client.getSession().write(InfoPacket.getSPMsg((byte) sp, (short) 0));
    }

    public void resetSP(int sp) {
        for (int i = 0; i < remainingSp.length; i++) {
            this.remainingSp[i] = sp;
        }
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
    }
    
    public void resetAP() {
        gainAp((short) -this.remainingAp);
    }
    

    public void resetAPSP() {
        resetSP(0);
        gainAp((short) -this.remainingAp);
    }

    public List<Integer> getProfessions() {
        List<Integer> prof = new ArrayList<Integer>();
        for (int i = 9200; i <= 9204; i++) {
            if (getProfessionLevel(id * 10000) > 0) {
                prof.add(i);
            }
        }
        return prof;
    }

    public byte getProfessionLevel(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (byte) ((ret >>> 24) & 0xFF); //the last byte
    }

    public short getProfessionExp(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (short) (ret & 0xFFFF); //the first two byte
    }

    public boolean addProfessionExp(int id, int expGain) {
        int ret = getProfessionLevel(id);
        if (ret <= 0 || ret >= 10) {
            return false;
        }
        int newExp = getProfessionExp(id) + expGain;
        if (newExp >= GameConstants.getProfessionEXP(ret)) {
            //gain level
            changeProfessionLevelExp(id, ret + 1, newExp - GameConstants.getProfessionEXP(ret));
            int traitGain = (int) Math.pow(2, ret + 1);
            switch (id) {
                case 92000000:
                    traits.get(MapleTraitType.sense).addExp(traitGain, this);
                    break;
                case 92010000:
                    traits.get(MapleTraitType.will).addExp(traitGain, this);
                    break;
                case 92020000:
                case 92030000:
                case 92040000:
                    traits.get(MapleTraitType.craft).addExp(traitGain, this);
                    break;
            }
            return true;
        } else {
            changeProfessionLevelExp(id, ret, newExp);
            return false;
        }
    }

    public void changeProfessionLevelExp(int id, int level, int exp) {
        changeSingleSkillLevel(SkillFactory.getSkill(id), ((level & 0xFF) << 24) + (exp & 0xFFFF), (byte) 10);
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel) { //1 month
        if (skill == null) {
            return;
        }
        changeSingleSkillLevel(skill, newLevel, newMasterlevel, SkillFactory.getDefaultSExpiry(skill));
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        final Map<Skill, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        if (changeSkillData(skill, newLevel, newMasterlevel, expiration)) { // no loop, only 1
            list.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            if (GameConstants.isRecoveryIncSkill(skill.getId())) {
                hasRecovery = true;
            }
            if (skill.getId() < 80000000) {
                recalculate = true;
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.getSession().write(CWvsContext.updateSkills(list));
        reUpdateStat(hasRecovery, recalculate);
    }

    public void changeSkillsLevel(final Map<Skill, SkillEntry> ss) {
        if (ss.isEmpty()) {
            return;
        }
        final Map<Skill, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        for (final Entry<Skill, SkillEntry> data : ss.entrySet()) {
            if (changeSkillData(data.getKey(), data.getValue().skillevel, data.getValue().masterlevel, data.getValue().expiration)) {
                list.put(data.getKey(), data.getValue());
                if (GameConstants.isRecoveryIncSkill(data.getKey().getId())) {
                    hasRecovery = true;
                }
                if (data.getKey().getId() < 80000000) {
                    recalculate = true;
                }
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.getSession().write(CWvsContext.updateSkills(list));
        reUpdateStat(hasRecovery, recalculate);
    }

    public void reUpdateStat(boolean hasRecovery, boolean recalculate) {
        changed_skills = true;
        if (hasRecovery) {
            stats.relocHeal(this);
        }
        if (recalculate) {
            stats.recalcLocalStats(this);
        }
    }

    public boolean changeSkillData(final Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        if (skill == null || (!GameConstants.isApplicableSkill(skill.getId()) && !GameConstants.isApplicableSkill_(skill.getId()))) {
            return false;
        }
        if (newLevel == 0 && newMasterlevel == 0) {
            if (skills.containsKey(skill)) {
                skills.remove(skill);
            } else {
                return false; //nothing happen
            }
        } else {
            skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
        }
        return true;
    }

    public void changeSkillLevel_Skip(final Map<Skill, SkillEntry> skill, final boolean write) { // only used for temporary skills (not saved into db)
        if (skill.isEmpty()) {
            return;
        }
        final Map<Skill, SkillEntry> newL = new HashMap<>();
        for (final Entry<Skill, SkillEntry> z : skill.entrySet()) {
            if (z.getKey() == null) {
                continue;
            }
            newL.put(z.getKey(), z.getValue());
            if (z.getValue().skillevel == 0 && z.getValue().masterlevel == 0) {
                if (skills.containsKey(z.getKey())) {
                    skills.remove(z.getKey());
                } else {
                    continue;
                }
            } else {
                skills.put(z.getKey(), z.getValue());
            }
        }
        if (write && !newL.isEmpty()) {
            client.getSession().write(CWvsContext.updateSkills(newL));
        }
    }

    public void playerDead() {
        final MapleStatEffect statss = getStatForBuff(MapleBuffStat.SOUL_STONE);
        if (statss != null) {
            dropMessage(5, "You have been revived by the goddess of swag.");
            getStat().setHp(((getStat().getMaxHp() / 100) * statss.getX()), this);
            setStance(0);
            changeMap(getMap(), getMap().getPortal(0));
            return;
        }
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        cancelEffectFromBuffStat(MapleBuffStat.SHADOWPARTNER);
        cancelEffectFromBuffStat(MapleBuffStat.MORPH);
        cancelEffectFromBuffStat(MapleBuffStat.SOARING);
        cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
        cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
        cancelEffectFromBuffStat(MapleBuffStat.HP_BOOST_PERCENT);
        cancelEffectFromBuffStat(MapleBuffStat.MP_BOOST_PERCENT);
        cancelEffectFromBuffStat(MapleBuffStat.HP_BOOST);
        cancelEffectFromBuffStat(MapleBuffStat.MP_BOOST);
        cancelEffectFromBuffStat(MapleBuffStat.ENHANCED_MAXHP);
        cancelEffectFromBuffStat(MapleBuffStat.ENHANCED_MAXMP);
        cancelEffectFromBuffStat(MapleBuffStat.MAXHP);
        cancelEffectFromBuffStat(MapleBuffStat.MAXMP);
        dispelSummons();
        checkFollow();
        dotHP = 0;
        lastDOTTime = 0;
        if (!GameConstants.isBeginnerJob(job) && !inPVP()) {
            int charms = getItemQuantity(5130000, false);
            if (charms > 0) {
                MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, 5130000, 1, true, false);

                charms--;
                if (charms > 0xFF) {
                    charms = 0xFF;
                }
                client.getSession().write(EffectPacket.useCharm((byte) charms, (byte) 0, true));
            } else {
                float diepercentage = 0.0f;
                int expforlevel = getNeededExp();
                if (map.isTown() || FieldLimitType.RegularExpLoss.check(map.getFieldLimit())) {
                    diepercentage = 0.01f;
                } else {
                    diepercentage = (float) (0.1f - ((traits.get(MapleTraitType.charisma).getLevel() / 20) / 100f) - (stats.expLossReduceR / 100f));
                }
                int v10 = (int) (exp - (long) ((double) expforlevel * diepercentage));
                if (v10 < 0) {
                    v10 = 0;
                }
                this.exp = v10;
            }
            this.updateSingleStat(MapleStat.EXP, this.exp);
        }
        if (!stats.checkEquipDurabilitys(this, -100)) { //i guess this is how it works ?
            dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
        } //lol
        if (pyramidSubway != null) {
            stats.setHp((short) 50, this);
            pyramidSubway.fail(this);
        }
        
    //   if (this.getMap().getMobFromChr(this) != null) {
      //      if (getStat().getHp() != 0) {
      //          setHp(0);
       //         updateSingleStat(MapleStat.HP, getStat().getHp());
      //    }
      //  }
        
        
    }

    public void updatePartyMemberHP() {
        if (party != null && client.getChannelServer() != null) {
            final int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    final MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient().getSession().write(CField.updatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party == null) {
            return;
        }
        int channel = client.getChannel();
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                if (other != null) {
                    client.getSession().write(CField.updatePartyMemberHP(other.getId(), other.getStat().getHp(), other.getStat().getCurrentMaxHp()));
                }
            }
        }
    }

    public void healHP(int delta) {
        addHP(delta);
        client.getSession().write(EffectPacket.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, EffectPacket.showHpHealed(getId(), delta), false);
    }

    public void healMP(int delta) {
        addMP(delta);
        client.getSession().write(EffectPacket.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, EffectPacket.showHpHealed(getId(), delta), false);
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setHp(int)
     * @param delta
     */
    public void addHP(int delta) {
        if (stats.setHp(stats.getHp() + delta, this)) {
            updateSingleStat(MapleStat.HP, stats.getHp());
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setMp(int)
     * @param delta
     */
    public void addMP(int delta) {
        addMP(delta, false);
    }

    public void addMP(int delta, boolean ignore) {
        if ((delta < 0 && GameConstants.isDemon(getJob())) || !GameConstants.isDemon(getJob()) || ignore) {
            if (stats.setMp(stats.getMp() + delta, this)) {
                updateSingleStat(MapleStat.MP, stats.getMp());
            }
        }
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        Map<MapleStat, Integer> statups = new EnumMap<MapleStat, Integer>(MapleStat.class);

        if (stats.setHp(stats.getHp() + hpDiff, this)) {
            statups.put(MapleStat.HP, Integer.valueOf(stats.getHp()));
        }
        if ((mpDiff < 0 && GameConstants.isDemon(getJob())) || !GameConstants.isDemon(getJob())) {
            if (stats.setMp(stats.getMp() + mpDiff, this)) {
                statups.put(MapleStat.MP, Integer.valueOf(stats.getMp()));
            }
        }
        if (statups.size() > 0) {
            client.getSession().write(CWvsContext.updatePlayerStats(statups, this));
        }
    }

    public void updateSingleStat(MapleStat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }
     public void updateSingleStatL(MapleStat stat, long newval) {
        updateSingleStatL(stat, newval, false);
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
        Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);
        statup.put(stat, newval);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, itemReaction, this));
    }
    
     public void updateSingleStatL(MapleStat stat, long newval, boolean itemReaction) {
        Map<MapleStat, Long> statup = new EnumMap<MapleStat, Long>(MapleStat.class);
        statup.put(stat, newval);
        client.getSession().write(CWvsContext.updatePlayerStatsL(statup, itemReaction, this));
    }

    public void gainExp(final int total, final boolean show, final boolean inChat, final boolean white) {
        try {
            int prevexp = getExp();
            int needed = getNeededExp();
            int neededp = getNeededParagon();
            long neededr = getNeedReborn();
            if (total > 0) {
                stats.checkEquipLevels(this, total); //gms like
            }
            
            if (reborns == 0 && level == 200) {
                                setExp(0);
                
                if (exp + total > neededp) {
                    setExp(neededp);
                } else {
                    exp += total;
                }
            } else { 
                if (haveEXPCard()){ 
                    exp=exp*2; 
                } 
               
            }
            
             
                boolean leveledp = false; 
                long totp = exp + total;
                if (totp >= neededp) {
                    exp += total;
                    levelUpParagon();
                    leveledp = true;
                
            
        }
            
           if ((level >= 200 || (GameConstants.isKOC(job) && level >= 120)) && !isIntern() && reborns < 10) {
                setExp(0);
                
                if (exp + total > needed) {
                    setExp(needed);
                } else {
                    exp += total;
                }
            } else { 
                if (haveEXPCard()){ 
                    exp=exp*2; 
                } 
                boolean leveled = false; 
                long tot = exp + total;
                if (tot >= needed && reborns < 10) {
                    exp += total;
                    levelUp();
                    leveled = true;
                   
        if (level >= 120) { 
            finishAchievement(47); 
        } 
        
        if (level == 50) {
            chr.dropMessage(5, "Congratulations on reaching level 50! Buffed Channels and Torment Mode will now apply to every level 50 + mobs you face. Good luck and becareful on what channel you train on as the challenge increases the higher number the channel.");
        }
        
      
         if ((level == 200) && (reborns == 0)) {
                       setExp(0);
                       neededp = getNeededParagon();
                        if (exp >= neededp) {
                            setExp(neededp - 1);
                        } 
                   
                    }
                    if ((level == 200)) {
                       setExp(0);
                        needed = getNeededExp();
                        if (exp >= needed) {
                            setExp(needed - 1);
                        }                      
                    }
                } else {
                    exp += total;
                }
                if (total > 0) {
                    familyRep(prevexp, needed, leveled);
                }
                
            }
            if (total != 0) {
                if (exp < 0) { // After adding, and negative
                    if (total > 0) {
                        setExp(needed);
                    } else if (total < 0) {
                        setExp(0);
                    }
                }
                updateSingleStat(MapleStat.EXP, getExp());
                if (show) { // still show the expgain even if it's not there
                    client.getSession().write(InfoPacket.GainEXP_Others(total, inChat, white));
                }
            }
            if (total != 0 && reborns == 0) {
                if (exp < 0) { // After adding, and negative
                    if (total > 0) {
                        setExp(neededp);
                    } else if (total < 0) {
                        setExp(0);
                    }
                }
                updateSingleStat(MapleStat.EXP, getExp());
                if (show) { // still show the expgain even if it's not there
                    client.getSession().write(InfoPacket.GainEXP_Others(total, inChat, white));
                }
            }
            
            
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
        }
        
    }

    public void setGmLevel(byte level) {
        this.gmLevel = level;
    }

    public void familyRep(int prevexp, int needed, boolean leveled) {
        if (mfc != null) {
            int onepercent = needed / 100;
            if (onepercent <= 0) {
                return;
            }
            int percentrep = (getExp() / onepercent - prevexp / onepercent);
            if (leveled) {
                percentrep = 100 - percentrep + (level / 2);
            }
            if (percentrep > 0) {
                int sensen = World.Family.setRep(mfc.getFamilyId(), mfc.getSeniorId(), percentrep * 14, level, name);
                if (sensen > 0) {
                    World.Family.setRep(mfc.getFamilyId(), sensen, percentrep * 7, level, name); //and we stop here
                }
            }
        }
    }

    public void gainExpMonster(final int gain, final boolean show, final boolean white, final byte pty, int Class_Bonus_EXP, int Equipment_Bonus_EXP, int Premium_Bonus_EXP, boolean partyBonusMob, final int partyBonusRate) {
        if (!isAlive()) {
            return;
        }
        mobKilledNo++;
        int total = gain + Class_Bonus_EXP + Equipment_Bonus_EXP + Premium_Bonus_EXP;
        long Trio_Bonus_EXP = 1;
        short percentage = 1;
        double hoursFromLogin = 1.0;
        if (mobKilledNo == 3 && ServerConstants.TRIPLE_TRIO) { // Count begins at 0
            //After 1 hour of login until 2 hours:	Bonus 30% EXP at every 3rd mob hunted
            //2 hours to 3 hours: Bonus 100% EXP at every 3rd mob hunted
            //3 hours to 4 hours: Bonus 150% EXP at every 3rd mob hunted
            //4 hours to 5 hours: Bonus 180% EXP at every 3rd mob hunted
            //5 hours and above: Bonus 200% EXP at every 3rd mob hunted
            hoursFromLogin = ((System.currentTimeMillis() - loginTime) / (1000 * 60 * 60));
            if (hoursFromLogin >= 1 && hoursFromLogin < 2) {
                percentage = 4;
            } else if (hoursFromLogin >= 2 && hoursFromLogin < 3) {
                percentage = 6;
            } else if (hoursFromLogin >= 3 && hoursFromLogin < 4) {
                percentage = 8;
            } else if (hoursFromLogin >= 4 && hoursFromLogin < 5) {
                percentage = 9;
            } else if (hoursFromLogin >= 5) {
                percentage = 15;
            }
            Trio_Bonus_EXP = (long) ((float) ((gain / 100) * percentage));
            total += Trio_Bonus_EXP;
            mobKilledNo = 4;
        }
        int partyinc = 0;
        int prevexp = getExp();
        if (pty > 1) {
            final double rate = (pty / 1.3);
            partyinc = (int) (((float) (gain * rate / 1.66)) + (pty + 20));
            total += partyinc;
        }
        
        if (gain > 0 && total < gain) { //just in case
            total = Integer.MAX_VALUE;
        }
        if (total > 0) {
            stats.checkEquipLevels(this, total); //gms like
        }
        int neededp = getNeededParagon();
        int needed = getNeededExp();
        long neededr = getNeedReborn();
        if ((level >= 200 || (GameConstants.isKOC(job) && level >= 120)) && !isIntern()) {
            setExp(0);
            //if (exp + total > needed) {
            //    setExp(needed);
            //} else {
            //    exp += total;
            //}
        } else {
            boolean leveled = false;
            if (exp + total >= needed || exp >= needed && reborns < 10) {
                exp += total;
                levelUp();
                leveled = true;
                if ((level >= 200 || (GameConstants.isKOC(job) && level >= 120)) && !isIntern()) {
                    setExp(0);
                } else {
                    needed = getNeededExp();
                    
                    if (exp >= needed) {
                        setExp(needed);
                    }
                }
            } else {
                exp += total;
            }
            if (total > 0) {
                familyRep(prevexp, needed, leveled);
                if (getOccupation() / 100 == 1 && Randomizer.nextInt(100) < 40) { // clones (ninja) 40%
                    gainOccupationEXP(1);
                } else if (getOccupation() / 100 == 5) { // celebrity
                    if (pty > 0 && Randomizer.nextInt(100) < ((getOccupation() % 10) * 2 * pty)) { // random (2%, 4%, 6 8 and 10)
                        gainOccupationEXP(1);
                    }
                }
            } else {
                return;
            }
        }
        if (gain != 0) {
            if (exp < 0) { // After adding, and negative
                if (gain > 0) {
                    setExp(getNeededExp());
                } else if (gain < 0) {
                    setExp(0);
                }
                
            }
            updateSingleStat(MapleStat.EXP, getExp());
            if (show) { // still show the expgain even if it's not there
                client.getSession().write(InfoPacket.GainEXP_Monster(gain, white, partyinc, Class_Bonus_EXP, Equipment_Bonus_EXP, Premium_Bonus_EXP));
            }
        }
        
        if (reborns == 0 && level == 200) {
            boolean leveled = false;
            if (exp + total >= neededp || exp >= neededp) {
                exp += total;
                levelUpParagon();
                leveled = true;
                if ((level >= 200 || (GameConstants.isKOC(job) && level >= 120)) && !isIntern()) {
                    setExp(0);
                } else {
                    neededp = getNeededParagon();
                    
                   
                }
            } else {
                exp += total;
            }
         if (gain != 0) {
            if (exp < 0) { // After adding, and negative
                if (gain > 0) {
                    setExp(getNeededParagon());
                } else if (gain < 0) {
                    setExp(0);
                }
            }
            updateSingleStat(MapleStat.EXP, getExp());
            if (show) { // still show the expgain even if it's not there
                client.getSession().write(InfoPacket.GainEXP_Monster(gain, white, partyinc, Class_Bonus_EXP, Equipment_Bonus_EXP, Premium_Bonus_EXP));
            }
        }
        }
        

        
       
        
    }
    
      public Pair<Double, Boolean> modifyDamageTaken(double damage, MapleMapObject attacke, boolean magic) {
        Pair<Double, Boolean> ret = new Pair<Double, Boolean>(damage, false);
        if (damage <= 0) {
            return ret;
        }
        if (stats.ignoreDAMr > 0 && Randomizer.nextInt(100) < stats.ignoreDAMr_rate) {
            damage -= Math.floor((stats.ignoreDAMr * damage) / 100.0f);
        }
        if (stats.ignoreDAM > 0 && Randomizer.nextInt(100) < stats.ignoreDAM_rate) {
            damage -= stats.ignoreDAM;
        }
        final Integer div = getBuffedValue(MapleBuffStat.DIVINE_SHIELD);
        final Integer div2 = getBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL);
        if (div2 != null) {
            if (div2 <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.HOLY_MAGIC_SHELL);
            } else {
                setBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL, div2 - 1);
                damage = 0;
            }
        } else if (div != null) {
            if (div <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.DIVINE_SHIELD);
            } else {
                setBuffedValue(MapleBuffStat.DIVINE_SHIELD, div - 1);
                damage = 0;
            }
        }
        MapleStatEffect barrier = getStatForBuff(MapleBuffStat.COMBO_BARRIER);
        if (barrier != null) {
            damage *= (1.0d - (barrier.getT() / 100.0d));
        }
        barrier = getStatForBuff(MapleBuffStat.MAGIC_SHIELD);
        if (barrier != null) {
            damage *= (1.0d - (barrier.getX() / 100.0d));
        }
        barrier = getStatForBuff(MapleBuffStat.WATER_SHIELD);
        if (barrier != null) {
            //damage = ((barrier.getX() / 1000.0) * damage); //uhm what?
            damage *= (1.0d - (barrier.getX() / 100.0d));
        }
        List<Integer> attack = attacke instanceof MapleMonster || attacke == null ? null : (new ArrayList<Integer>());
        if (damage > 0) {
            if (getJob() == 122 && !skillisCooling(1220013)) {
                final Skill divine = SkillFactory.getSkill(1220013);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(CField.skillCooldown(1220013, divineShield.getCooldown(this)));
                        addCooldown(1220013, System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
            } else if (getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC) != null && getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB) != null && getBuffedValue(MapleBuffStat.PUPPET) != null) {
                double buff = getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC).doubleValue();
                double buffz = getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB).doubleValue();
                if ((int) ((buff / 100.0) * getStat().getMaxHp()) <= damage) {
                    damage -= ((buffz / 100.0) * damage);
                    cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                }
            } else if (getJob() == 433 || getJob() == 434) {
                final Skill advDarkSight = SkillFactory.getSkill(4330001);
                if (getTotalSkillLevel(advDarkSight) > 0 && getBuffedValue(MapleBuffStat.DARKSIGHT) == null && !skillisCooling(advDarkSight.getId())) {
                    final MapleStatEffect advMSE = advDarkSight.getEffect(getTotalSkillLevel(advDarkSight));
                    if (Randomizer.nextInt(100) < advMSE.getX()) {
                        advMSE.applyTo(this);
                    }
                }
            } else if ((getJob() == 512 || getJob() == 522) && getBuffedValue(MapleBuffStat.PIRATES_REVENGE) == null) {
                final Skill divine = SkillFactory.getSkill(getJob() == 512 ? 5120011 : 5220012);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(CField.skillCooldown(divine.getId(), divineShield.getCooldown(this)));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
                /*} else if (getJob() == 312 && attacke != null) {
                 final Skill divine = SkillFactory.getSkill(3120010);
                 if (getTotalSkillLevel(divine) > 0) {
                 final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                 if (divineShield.makeChanceResult()) {
                 if (attacke instanceof MapleMonster) {
                 final Rectangle bounds = divineShield.calculateBoundingBox(getTruePosition(), isFacingLeft());
                 final List<MapleMapObject> affected = getMap().getMapObjectsInRect(bounds, Arrays.asList(attacke.getType()));
                 int i = 0;

                 for (final MapleMapObject mo : affected) {
                 MapleMonster mons = (MapleMonster) mo;
                 if (mons.getStats().isFriendly() || mons.isFake()) {
                 continue;
                 }
                 mons.applyStatus(this, new MonsterStatusEffect(MonsterStatus.STUN, 1, divineShield.getSourceId(), null, false), false, divineShield.getDuration(), true, divineShield);
                 final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                 mons.damage(this, theDmg, true);
                 getMap().broadcastMessage(MobPacket.damageMonster(mons.getObjectId(), theDmg));
                 i++;
                 if (i >= divineShield.getMobCount()) {
                 break;
                 }
                 }
                 } else {
                 MapleCharacter chr = (MapleCharacter) attacke;
                 chr.addHP(-divineShield.getDamage());
                 attack.add((int) divineShield.getDamage());
                 }
                 }
                 }*/
            } else if ((getJob() == 531 || getJob() == 532) && attacke != null) {
                final Skill divine = SkillFactory.getSkill(5310009); //slea.readInt() = 5310009, then slea.readInt() = damage. (175000)
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            } else if (getJob() == 132 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(1320011);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId()) && getBuffSource(MapleBuffStat.BEHOLDER) == 1321007) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        client.getSession().write(CField.skillCooldown(divine.getId(), divineShield.getCooldown(this)));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            }
            if (attacke != null) {
                final int damr = (Randomizer.nextInt(100) < getStat().DAMreflect_rate ? getStat().DAMreflect : 0) + (getBuffedValue(MapleBuffStat.POWERGUARD) != null ? getBuffedValue(MapleBuffStat.POWERGUARD) : 0);
                final int bouncedam_ = damr + (getBuffedValue(MapleBuffStat.POWERGUARD) != null ? getBuffedValue(MapleBuffStat.POWERGUARD) : 0);
                if (bouncedam_ > 0) {
                    long bouncedamage = (long) (damage * bouncedam_ / 100);
                    if (attacke instanceof MapleMonster) {
                        final MapleMonster attacker = (MapleMonster) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);
                        attacker.damage(this, bouncedamage, true);
                        getMap().broadcastMessage(this, MobPacket.damageMonster(attacker.getObjectId(), bouncedamage), getTruePosition());
                        if (getBuffSource(MapleBuffStat.POWERGUARD) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.POWERGUARD);
                            if (eff.makeChanceResult()) {
                                attacker.applyStatus(this, new MonsterStatusEffect(MonsterStatus.STUN, 1, eff.getSourceId(), null, false), false, eff.getSubTime(), true, eff);
                            }
                        }
                    } else {
                        final MapleCharacter attacker = (MapleCharacter) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getStat().getCurrentMaxHp() / 10);
                        attacker.addHP(-((int) bouncedamage));
                        attack.add((int) bouncedamage);
                        if (getBuffSource(MapleBuffStat.POWERGUARD) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.POWERGUARD);
                            if (eff.makeChanceResult()) {
                                attacker.disease(MapleDisease.STUN.getDisease(), 1);
                            }
                        }
                    }
                    ret.right = true;
                }
                if (getJob() >= 1110 && getJob() <= 1112) {
                    final int damReduce = getBuffedValue(MapleBuffStat.POWERGUARD) == null ? 0 : getBuffedValue(MapleBuffStat.POWERGUARD);
                    if (damReduce > 0) {
                        damage *= (1.0d - (damReduce / 100.0d));
                    }
                }
                if ((getJob() == 411 || getJob() == 412 || getJob() == 421 || getJob() == 422) && getBuffedValue(MapleBuffStat.SUMMON) != null && attacke != null) {
                    final List<MapleSummon> ss = getSummonsReadLock();
                    try {
                        for (MapleSummon sum : ss) {
                            if (sum.getTruePosition().distanceSq(getTruePosition()) < 400000.0 && (sum.getSkill() == 4111007 || sum.getSkill() == 4211007)) {
                                final List<Pair<Integer, Integer>> allDamage = new ArrayList<Pair<Integer, Integer>>();
                                if (attacke instanceof MapleMonster) {
                                    final MapleMonster attacker = (MapleMonster) attacke;
                                    final int theDmg = (int) (SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX() * damage / 100.0);
                                    allDamage.add(new Pair<Integer, Integer>(attacker.getObjectId(), theDmg));
                                    getMap().broadcastMessage(SummonPacket.summonAttack(sum.getOwnerId(), sum.getObjectId(), (byte) 0x84, allDamage, getLevel(), true));
                                    attacker.damage(this, theDmg, true);
                                    checkMonsterAggro(attacker);
                                    if (!attacker.isAlive()) {
                                        getClient().getSession().write(MobPacket.killMonster(attacker.getObjectId(), 1));
                                    }
                                } else {
                                    final MapleCharacter chr = (MapleCharacter) attacke;
                                    final int dmg = SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX();
                                    chr.addHP(-dmg);
                                    attack.add(dmg);
                                }
                            }
                        }
                    } finally {
                        unlockSummonsReadLock();
                    }
                }
            }
        }
       
        if (attack != null && attack.size() > 0 && attacke != null) {
            getMap().broadcastMessage(CField.pvpCool(attacke.getObjectId(), attack));
        }
        ret.left = damage;
        return ret;
    }
    
        public void announce(byte[] packet) {
        client.announce(packet);
    }
       public void updateAreaInfo(int area, String info) {
        area_info.put(Short.valueOf((short) area), info);
        announce(CWvsContext.updateAreaInfo(area, info));
    }
       
          public boolean containsAreaInfo(int area, String info) {
        Short area_ = Short.valueOf((short) area);
        if (area_info.containsKey(area_)) {
            return area_info.get(area_).contains(info);
        }
        return false;
    }
    
          public void changeElf() {//questEx 7784 
        updateAreaInfo(7784, containsAreaInfo(7784, "sw=") ? containsAreaInfo(7784, "sw=1") ? "sw=0" : "sw=1" : "sw=1"); 
        if (containsAreaInfo(7784, GameConstants.isMercedes(getJob()) ? "sw=0" : "sw=1")) { 
            announce(CWvsContext.showWeirdEffect("Effect/BasicEff.img/JobChangedElf", 5155000));
            getMap().broadcastMessage(this, CWvsContext.showWeirdEffect(getId(), "Effect/BasicEff.img/JobChangedElf", 5155000), false);
        } else { 
            announce(CWvsContext.showWeirdEffect("Effect/BasicEff.img/JobChanged", 5155000));
            getMap().broadcastMessage(this, CWvsContext.showWeirdEffect(getId(), "Effect/BasicEff.img/JobChanged", 5155000), false);
        } 
        equipChanged(); 
    } 

    public boolean isElf() { 
        if (containsAreaInfo(7784, "sw=")) { 
            return containsAreaInfo(7784, GameConstants.isMercedes(getJob()) ? "sw=0" : "sw=1"); 
        } 
        return GameConstants.isMercedes(getJob()); 
    }  
      /*  public void changeElf() {//questEx 7784
        updateAreaInfo(7784, containsAreaInfo(7784, "sw=") ? containsAreaInfo(7784, "sw=1") ? "sw=0" : "sw=1" : "sw=1");
        if (containsAreaInfo(7784, GameConstants.isMercedes(getJob()) ? "sw=0" : "sw=1")) {
            announce(CWvsContext.showWeirdEffect("Effect/BasicEff.img/JobChangedElf", 5155000));
            getMap().broadcastMessage(this, CWvsContext.showWeirdEffect(getId(), "Effect/BasicEff.img/JobChangedElf", 5155000), false);
            
        } else {
            announce(CWvsContext.showWeirdEffect("Effect/BasicEff.img/JobChanged", 5155000));
            getMap().broadcastMessage(this, CWvsContext.showWeirdEffect(getId(), "Effect/BasicEff.img/JobChanged", 5155000), false);
        }
        equipChanged();
    } */

    public void forceReAddItem_NoUpdate(Item item, MapleInventoryType type) {
        getInventory(type).removeSlot(item.getPosition());
        getInventory(type).addFromDB(item);
    }

    public void forceReAddItem(Item item, MapleInventoryType type) { //used for stuff like durability, item exp/level, probably owner?
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(InventoryPacket.updateSpecialItemUse(item, type == MapleInventoryType.EQUIPPED ? (byte) 1 : type.getType(), this));
        }
    }

    public void forceReAddItem_Flag(Item item, MapleInventoryType type) { //used for flags
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(InventoryPacket.updateSpecialItemUse_(item, type == MapleInventoryType.EQUIPPED ? (byte) 1 : type.getType(), this));
        }
    }

    public void forceReAddItem_Book(Item item, MapleInventoryType type) { //used for mbook
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(CWvsContext.upgradeBook(item, this));
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            World.Party.updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(this));
        }
    }

    public boolean isSuperGM() {
        return gmLevel >= 5;
    }
    public boolean isHeadGM() {
        return gmLevel >= 6;
    }

    public boolean isIntern() {
        return gmLevel >= 3;
    }

    public boolean isGM() {
        return gmLevel >= 4;
    }

    public boolean isAdmin() {
        return gmLevel == 7;
    }

    public int getGMLevel() {
        return gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }

    public final MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public final MapleInventory[] getInventorys() {
        return inventory;
    }

    public final void expirationTask(boolean pending, boolean firstLoad) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (pending) {
            if (pendingExpiration != null) {
                for (Integer z : pendingExpiration) {
                    client.getSession().write(InfoPacket.itemExpired(z.intValue()));
                    if (!firstLoad) {
                        final Pair<Integer, String> replace = ii.replaceItemInfo(z.intValue());
                        if (replace != null && replace.left > 0 && replace.right.length() > 0) {
                            dropMessage(5, replace.right);
                        }
                    }
                }
            }
            pendingExpiration = null;
            if (pendingSkills != null) {
                client.getSession().write(CWvsContext.updateSkills(pendingSkills));
                for (Skill z : pendingSkills.keySet()) {
                    client.getSession().write(CWvsContext.serverNotice(5, "[" + SkillFactory.getSkillName(z.getId()) + "] skill has expired and will not be available for use."));
                }
            } //not real msg
            pendingSkills = null;
            return;
        }
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        long expiration;
        final List<Integer> ret = new ArrayList<Integer>();
        final long currenttime = System.currentTimeMillis();
        final List<Triple<MapleInventoryType, Item, Boolean>> toberemove = new ArrayList<Triple<MapleInventoryType, Item, Boolean>>(); // This is here to prevent deadlock.
        final List<Item> tobeunlock = new ArrayList<Item>(); // This is here to prevent deadlock.

        for (final MapleInventoryType inv : MapleInventoryType.values()) {
            for (final Item item : getInventory(inv)) {
                expiration = item.getExpiration();

                if ((expiration != -1 && !GameConstants.isPet(item.getItemId()) && currenttime > expiration) || (firstLoad && ii.isLogoutExpire(item.getItemId()))) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        tobeunlock.add(item);
                    } else if (currenttime > expiration) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                    }
                } else if (item.getItemId() == 5000054 && item.getPet() != null && item.getPet().getSecondsLeft() <= 0) {
                    toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, false));
                } else if (item.getPosition() == -59) {
                    if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < currenttime) {
                        toberemove.add(new Triple<MapleInventoryType, Item, Boolean>(inv, item, true));
                    }
                }
            }
        }
        Item item;
        for (final Triple<MapleInventoryType, Item, Boolean> itemz : toberemove) {
            item = itemz.getMid();
            getInventory(itemz.getLeft()).removeItem(item.getPosition(), item.getQuantity(), false);
            if (itemz.getRight() && getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot() > -1) {
                item.setPosition(getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot());
                getInventory(GameConstants.getInventoryType(item.getItemId())).addFromDB(item);
            } else {
                ret.add(item.getItemId());
            }
            if (!firstLoad) {
                final Pair<Integer, String> replace = ii.replaceItemInfo(item.getItemId());
                if (replace != null && replace.left > 0) {
                    Item theNewItem = null;
                    if (GameConstants.getInventoryType(replace.left) == MapleInventoryType.EQUIP) {
                        theNewItem = ii.getEquipById(replace.left);
                        theNewItem.setPosition(item.getPosition());
                    } else {
                        theNewItem = new Item(replace.left, item.getPosition(), (short) 1, (byte) 0);
                    }
                    getInventory(itemz.getLeft()).addFromDB(theNewItem);
                }
            }
        }
        for (final Item itemz : tobeunlock) {
            itemz.setExpiration(-1);
            itemz.setFlag((byte) (itemz.getFlag() - ItemFlag.LOCK.getValue()));
        }
        this.pendingExpiration = ret;

        final Map<Skill, SkillEntry> skilz = new HashMap<>();
        final List<Skill> toberem = new ArrayList<Skill>();
        for (Entry<Skill, SkillEntry> skil : skills.entrySet()) {
            if (skil.getValue().expiration != -1 && currenttime > skil.getValue().expiration) {
                toberem.add(skil.getKey());
            }
        }
        for (Skill skil : toberem) {
            skilz.put(skil, new SkillEntry(0, (byte) 0, -1));
            this.skills.remove(skil);
            changed_skills = true;
        }
        this.pendingSkills = skilz;
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) < currenttime) { //expired bro
            quests.remove(MapleQuest.getInstance(7830));
            quests.remove(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        }
    }

    public void refreshBattleshipHP() {
        if (getJob() == 422) {
            client.getSession().write(CWvsContext.giveKilling(currentBattleshipHP()));
        }
    }

    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public int getMeso() {
        return meso;
    }

    public final int[] getSavedLocations() {
        return savedLocations;
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.getValue()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = getMapId();
        changed_savedlocations = true;
    }

    public void saveLocation(SavedLocationType type, int mapz) {
        savedLocations[type.getValue()] = mapz;
        changed_savedlocations = true;
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = -1;
        changed_savedlocations = true;
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false);
    }
        
    public int getAchievementLog(String achid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            int ret_count = 0;
            PreparedStatement ps;
            ps = con1.prepareStatement("select count(*) from achievementlog where accountid = ? and achid = ? and lastattempt >= subtime(current_timestamp, '1 0:0:0.0')");
            ps.setInt(1, accountid);
            ps.setString(2, achid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) 
                ret_count = rs.getInt(1);
            else
                ret_count = -1;
            rs.close();
            ps.close();
            return ret_count;
        } catch (Exception Ex) {
            return -1;
        }
    }
    
    public void setAchievementLog(String achid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ps = con1.prepareStatement("insert into achievementlog (accountid, characterid, achid) values (?,?,?)");
            ps.setInt(1, accountid);
            ps.setInt(2, id);
            ps.setString(3, achid);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
        }
    }
    
    public int checkAchievementLog(String achid) {
    return getAchievementLog(achid);
}
public void setNewAchievementLog(String achid) {
    setAchievementLog(achid);
}

    public void gainMeso(int gain, boolean show, boolean inChat) {
        if (meso + gain < 0) {
            client.getSession().write(CWvsContext.enableActions());
            return;
        }
        meso += gain;
        if (meso >= 1000000) {
          //  if (chr.checkAchievementLog("Complete31") <= 0) {
          //  setAchievementLog("Complete31");
            finishAchievement(31);
          //  }
        }
        if (meso >= 10000000) {
           // if (chr.checkAchievementLog("Complete32") <= 0) {
          //  setAchievementLog("Complete32");
            finishAchievement(32);
           // }
        }
        if (meso >= 100000000) {
           // if (chr.checkAchievementLog("Complete33") <= 0) {
          //  setAchievementLog("Complete33");
            finishAchievement(33);
         //   }
        }
        if (meso >= 1000000000) {
           // if (chr.checkAchievementLog("Complete34") <= 1) {
           // setAchievementLog("Complete35");
            finishAchievement(34);
           // }
        }
        updateSingleStat(MapleStat.MESO, meso, false);
        client.getSession().write(CWvsContext.enableActions());
        if (show) {
            client.getSession().write(InfoPacket.showMesoGain(gain, inChat));
        }
    }

      public void controlMonster(MapleMonster monster, boolean aggro) {
        if (clone || monster == null) {
            return;
        }
        monster.setController(this);
        controlledLock.writeLock().lock();
        try {
            controlled.add(monster);
        } finally {
            controlledLock.writeLock().unlock();
        }
        client.getSession().write(MobPacket.controlMonster(monster, false, aggro));
        monster.sendStatus(client);
    }

    public void stopControllingMonster(MapleMonster monster) {
        if (clone || monster == null) {
            return;
        }
        controlledLock.writeLock().lock();
        try {
            if (controlled.contains(monster)) {
                controlled.remove(monster);
            }
        } finally {
            controlledLock.writeLock().unlock();
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (clone || monster == null) {
            return;
        }
        if (monster.getController() == this) {
            monster.setControllerHasAggro(true);
        } else {
            monster.switchController(this, true);
        }
    }

    public int getControlledSize() {
        return controlled.size();
    }

    public int getAccountID() {
        return accountid;
    }

    public void mobKilled(final int id, final int skillID) {
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() != 1 || !q.hasMobKills()) {
                continue;
            }
            if (q.mobKilled(id, skillID)) {
                client.getSession().write(InfoPacket.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.getSession().write(CWvsContext.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
        
    }
    
 

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 1 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<MapleQuestStatus>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<Pair<Integer, Long>> getCompletedMedals() {
        List<Pair<Integer, Long>> ret = new ArrayList<Pair<Integer, Long>>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked() && q.getQuest().getMedalItem() > 0 && GameConstants.getInventoryType(q.getQuest().getMedalItem()) == MapleInventoryType.EQUIP) {
                ret.add(new Pair<Integer, Long>(q.getQuest().getId(), q.getCompletionTime()));
            }
        }
        return ret;
    }

    public Map<Skill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getTotalSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return Math.min(skill.getTrueMax(), ret.skillevel + (skill.isBeginnerSkill() ? 0 : (stats.combatOrders + (skill.getMaxLevel() > 10 ? stats.incAllskill : 0) + stats.getSkillIncrement(skill.getId()))));
    }

    public int getAllSkillLevels() {
        int rett = 0;
        for (Entry<Skill, SkillEntry> ret : skills.entrySet()) {
            if (!ret.getKey().isBeginnerSkill() && !ret.getKey().isSpecialSkill() && ret.getValue().skillevel > 0) {
                rett += ret.getValue().skillevel;
            }
        }
        return rett;
    }

    public long getSkillExpiry(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.expiration;
    }

    public int getSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.skillevel;
    }

    public byte getMasterLevel(final int skill) {
        return getMasterLevel(SkillFactory.getSkill(skill));
    }

    public byte getMasterLevel(final Skill skill) {
        final SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }
    
    public void levelUpParagon() {
        if (level == 200 && reborns == 0) {
             if (Paragons == 1) {
        remainingAp += 1; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededParagon();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededParagon() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
        if (getParagons() == 1) {
            finishAchievement(42);
        }
        if (getParagons() == 70) {
            finishAchievement(43);
        }
        if (getParagons() == 120) {
            finishAchievement(44);
        }
        if (getParagons() == 200) {
            finishAchievement(45);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 8); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), true);
        client.getSession().write(EffectPacket.showForeignEffect(0));
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        doParagon();
        }
            if (Paragons >= 2) {
        remainingAp += 1; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededParagon();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededParagon() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
        if (getParagons() == 1) {
            finishAchievement(42);
        }
        if (getParagons() == 70) {
            finishAchievement(43);
        }
        if (getParagons() == 120) {
            finishAchievement(44);
        }
        if (getParagons() == 200) {
            finishAchievement(45);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 8); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), true);
        client.getSession().write(EffectPacket.showForeignEffect(0));
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        doParagon();
        }
             if (Paragons >= 100) {
        remainingAp += 2; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededParagon();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededParagon() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
        if (getParagons() == 1) {
            finishAchievement(42);
        }
        if (getParagons() == 70) {
            finishAchievement(43);
        }
        if (getParagons() == 120) {
            finishAchievement(44);
        }
        if (getParagons() == 200) {
            finishAchievement(45);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 8); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), true);
        client.getSession().write(EffectPacket.showForeignEffect(0));
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        doParagon();
        }
              if (Paragons >= 180) {
        remainingAp += 3; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededParagon();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededParagon() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
        if (getParagons() == 1) {
            finishAchievement(42);
        }
        if (getParagons() == 70) {
            finishAchievement(43);
        }
        if (getParagons() == 120) {
            finishAchievement(44);
        }
        if (getParagons() == 200) {
            finishAchievement(45);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 8); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), true);
        client.getSession().write(EffectPacket.showForeignEffect(0));
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        doParagon();
        }
               if (Paragons >= 300) {
        remainingAp += 4; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededParagon();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededParagon() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
        if (getParagons() == 1) {
            finishAchievement(42);
        }
        if (getParagons() == 70) {
            finishAchievement(43);
        }
        if (getParagons() == 120) {
            finishAchievement(44);
        }
        if (getParagons() == 200) {
            finishAchievement(45);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 8); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), true);
        client.getSession().write(EffectPacket.showForeignEffect(0));
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        doParagon();
        }
        }
     
    }

    public void levelUp() {
        if (reborns == 1) {
            final MapleInventory equip = client.getPlayer().getInventory(MapleInventoryType.EQUIPPED); //no need to refesh chr - Rummy
        //we can add it here since when you level up you get 3 ap points
        
        remainingAp += 3; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededExp();
        expL -= getNeedReborn();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        level += 1;
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededExp() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
       
        if (getReborns() == 1) {
            finishAchievement(2);
        }
        if (getReborns() == 70) {
            finishAchievement(3);
        }
        if (getReborns() == 120) {
            finishAchievement(4);
        }
        if (getReborns() == 200) {
            finishAchievement(5);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), false);
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        //Auto Job
        if (job >= 430 && job <= 434) { // Dual Blade
            switch (getLevel()) {
                case 20:
                    changeJob((short) 430);
                    break;
                case 30:
                    changeJob((short) 431);
                    break;
                case 55:
                    changeJob((short) 432);
                    break;
                case 70:
                    changeJob((short) 433);
                    break;
                case 120:
                    changeJob((short) 434);
                    break;
            }

        } 			
  

         else if (GameConstants.isAran(job)) { // Only one class
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2100);
                    break;
                case 30:
                    changeJob((short) 2110);
                    break;
                case 70:
                    changeJob((short) 2111);
                    break;
                case 120:
                    changeJob((short) 2112);
                    break;
            }

        } else if (GameConstants.isEvan(job)) { // 2218, 2217, 2216, 2215, 2214, 2213, 2212, 2211, 2210, 2200, 2001
            if (level >= 160 && job != 2218) {
                changeJob((short) 2218); // which jobs give level 70 job?
            } else if (level >= 120 && level <= 159 && job != 2217) {
                changeJob((short) 2217);
            } else if (level >= 100 && level <= 119 && job != 2216) {
                changeJob((short) 2216);
            } else if (level >= 80 && level <= 99 && job != 2215) {
                changeJob((short) 2215);
            } else if (level >= 60 && level <= 79 && job != 2214) {
                changeJob((short) 2214);
            } else if (level >= 50 && level <= 59 && job != 2213) {
                changeJob((short) 2213);
            } else if (level >= 40 && level <= 49 && job != 2212) {
                changeJob((short) 2212);
            } else if (level >= 30 && level <= 39 && job != 2211) {
                changeJob((short) 2211);
            } else if (level >= 20 && level <= 29 && job != 2210) {
                changeJob((short) 2210);
            } else if (level >= 10 && level <= 19 && job != 2200) {
                changeJob((short) 2200);
            }

        } else if (GameConstants.isDemon(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 3100);
                    setShield(1099000);
                    setWeapon(1322122);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 30:
                    changeJob((short) 3110);
                    setShield(1099002);
                    //setWeapon(1322124);
                   dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                   chr.fakeRelog();
                    break;
                case 70:
                    changeJob((short) 3111);
                    setShield(1099003);
                   // setWeapon(1322125);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 120:
                    changeJob((short) 3112);
                    setShield(1099004);
                   // setWeapon(1322127);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
            }

        }  else if (GameConstants.isMercedes(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2300);
                    break;
                case 30:
                    changeJob((short) 2310);
                    break;
                case 70:
                    changeJob((short) 2311);
                    break;
                case 120:
                    changeJob((short) 2312);
                    break;
            }
            
            } else if (GameConstants.isMihile(job)) { // Mihile
            switch (getLevel()) {
                 case 1:
                    setWeapon(1302000);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 10:
                    changeJob((short) 5100);
                    setShield(1098000);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 30:
                    changeJob((short) 5110);
                    setShield(1098001);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 70:
                    changeJob((short) 5111);
                    setShield(1098002);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 120:
                    changeJob((short) 5112);
                    setShield(1098003);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
            }

        } 
            



            //  PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),



       
        



        //    PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),
        // End of auto job [someone optimize this]
        //if (map.getForceMove() > 0 && map.getForceMove() <= getLevel()) {
        //    changeMap(map.getReturnMap(), map.getReturnMap().getPortal(0));
        //    dropMessage(-1, "You have been expelled from the map.");
        //}
        
        if (level == 200) {
            finishAchievement(46);
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Congratulations] " + getName() + " has reached Level 200! Congratulate " + getName() + " on such an amazing achievement!")); 
               //reborn message is now with @rebirth/reborn command, makes more sense that way..
        }
                       if (level == 10) {
       dropMessage(6, "[" + getName() + "]:" + "I have become strong. Yet my enemies strength still outrank me, although I think I am ready to choose my Job. Time to see some masters and make my decision.");           
        }
        
                if (level == 30) {
       dropMessage(6, "[" + getName() + "]:" + "I have become stronger. I still have much to yearn, but I think I am ready to advance. It's time to see the master.");    
                 
               
                    
        }
                
                                if (level == 70) {
       dropMessage(6, "[" + getName() + "]:" + "I have become much stronger than before. I should go see my master to hopefully gain more power.");          
        }
                                                                if (level == 120) {
       dropMessage(6, "[" + getName() + "]:" + "I have become appaling to evil. So much untapped power. I should go back to my master to gain god-like powers.");          
        }
        
                                                                 if (level == 200) {
       dropMessage(6, "[" + getName() + "]:" + "I am scintillating with power. I have surpassed my master. It's time to choose my path to be reborn and face the evils of the MapleWorld again or to continue to fight for eternity as I am now and gaining more strength.");          
        }
        
                if (Paragons == 10) {
            
                    if (getReborns() >= 1) {
                dropMessage(6, "[" + getName() + "]:" + "I have become more formidable.");    
                
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");     
        }
        }
                                if (Paragons == 30) {
                dropMessage(6, "[" + getName() + "]:" + "Yet another unsatisfiying achievement, I must get stronger.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                
                                                                if (Paragons == 70) {
                dropMessage(6, "[" + getName() + "]:" + "My enemies grow stronger, so must I.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                                                
                  if (Paragons == 120) {
                dropMessage(6, "[" + getName() + "]:" + "I have become more formidable.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                    if (Paragons == 200) {
                dropMessage(6, "[" + getName() + "]:" + "I must not waste time. I must keep training.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                    
                                                                    if (Paragons == 300) {
                dropMessage(6, "[" + getName() + "]:" + "Yet another unsatisfiying achievement, I must get stronger.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
  if (Paragons == 400) {
                dropMessage(6, "[" + getName() + "]:" + "I must not waste time. I must keep training.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
  
                                                                  if (Paragons == 500) {
                dropMessage(6, "[" + getName() + "]:" + "My enemies grow stronger, so must I.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
            
        }
        if (reborns == 2) {
            final MapleInventory equip = client.getPlayer().getInventory(MapleInventoryType.EQUIPPED); //no need to refesh chr - Rummy
        //we can add it here since when you level up you get 3 ap points
        
        remainingAp += 2; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededExp();
        expL -= getNeedReborn();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        level += 1;
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededExp() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
       
        if (getReborns() == 1) {
            finishAchievement(2);
        }
        if (getReborns() == 70) {
            finishAchievement(3);
        }
        if (getReborns() == 120) {
            finishAchievement(4);
        }
        if (getReborns() == 200) {
            finishAchievement(5);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), false);
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        //Auto Job
        if (job >= 430 && job <= 434) { // Dual Blade
            switch (getLevel()) {
                case 20:
                    changeJob((short) 430);
                    break;
                case 30:
                    changeJob((short) 431);
                    break;
                case 55:
                    changeJob((short) 432);
                    break;
                case 70:
                    changeJob((short) 433);
                    break;
                case 120:
                    changeJob((short) 434);
                    break;
            }

        } 			
  

         else if (GameConstants.isAran(job)) { // Only one class
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2100);
                    break;
                case 30:
                    changeJob((short) 2110);
                    break;
                case 70:
                    changeJob((short) 2111);
                    break;
                case 120:
                    changeJob((short) 2112);
                    break;
            }

        } else if (GameConstants.isEvan(job)) { // 2218, 2217, 2216, 2215, 2214, 2213, 2212, 2211, 2210, 2200, 2001
            if (level >= 160 && job != 2218) {
                changeJob((short) 2218); // which jobs give level 70 job?
            } else if (level >= 120 && level <= 159 && job != 2217) {
                changeJob((short) 2217);
            } else if (level >= 100 && level <= 119 && job != 2216) {
                changeJob((short) 2216);
            } else if (level >= 80 && level <= 99 && job != 2215) {
                changeJob((short) 2215);
            } else if (level >= 60 && level <= 79 && job != 2214) {
                changeJob((short) 2214);
            } else if (level >= 50 && level <= 59 && job != 2213) {
                changeJob((short) 2213);
            } else if (level >= 40 && level <= 49 && job != 2212) {
                changeJob((short) 2212);
            } else if (level >= 30 && level <= 39 && job != 2211) {
                changeJob((short) 2211);
            } else if (level >= 20 && level <= 29 && job != 2210) {
                changeJob((short) 2210);
            } else if (level >= 10 && level <= 19 && job != 2200) {
                changeJob((short) 2200);
            }

        } else if (GameConstants.isDemon(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 3100);
                    setShield(1099000);
                    setWeapon(1322122);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 30:
                    changeJob((short) 3110);
                    setShield(1099002);
                    //setWeapon(1322124);
                   dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                   chr.fakeRelog();
                    break;
                case 70:
                    changeJob((short) 3111);
                    setShield(1099003);
                   // setWeapon(1322125);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 120:
                    changeJob((short) 3112);
                    setShield(1099004);
                   // setWeapon(1322127);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
            }

        }  else if (GameConstants.isMercedes(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2300);
                    break;
                case 30:
                    changeJob((short) 2310);
                    break;
                case 70:
                    changeJob((short) 2311);
                    break;
                case 120:
                    changeJob((short) 2312);
                    break;
            }
            
            } else if (GameConstants.isMihile(job)) { // Mihile
            switch (getLevel()) {
                case 1:
                    setWeapon(1302000);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 10:
                    changeJob((short) 5100);
                    setShield(1098000);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 30:
                    changeJob((short) 5110);
                    setShield(1098001);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 70:
                    changeJob((short) 5111);
                    setShield(1098002);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 120:
                    changeJob((short) 5112);
                    setShield(1098003);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
            }

        } 
            



            //  PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),



       
        



        //    PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),
        // End of auto job [someone optimize this]
        //if (map.getForceMove() > 0 && map.getForceMove() <= getLevel()) {
        //    changeMap(map.getReturnMap(), map.getReturnMap().getPortal(0));
        //    dropMessage(-1, "You have been expelled from the map.");
        //}
        
        if (level == 200) {
            finishAchievement(46);
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Congratulations] " + getName() + " has reached Level 200! Congratulate " + getName() + " on such an amazing achievement!")); 
               //reborn message is now with @rebirth/reborn command, makes more sense that way..
        }
                       if (level == 10) {
       dropMessage(6, "[" + getName() + "]:" + "I have become strong. Yet my enemies strength still outrank me, although I think I am ready to choose my Job. Time to see some masters and make my decision.");           
        }
        
                if (level == 30) {
       dropMessage(6, "[" + getName() + "]:" + "I have become stronger. I still have much to yearn, but I think I am ready to advance. It's time to see the master.");    
                 
               
                    
        }
                
                                if (level == 70) {
       dropMessage(6, "[" + getName() + "]:" + "I have become much stronger than before. I should go see my master to hopefully gain more power.");          
        }
                                                                if (level == 120) {
       dropMessage(6, "[" + getName() + "]:" + "I have become appaling to evil. So much untapped power. I should go back to my master to gain god-like powers.");          
        }
        
                                                                 if (level == 200) {
       dropMessage(6, "[" + getName() + "]:" + "I am scintillating with power. I have surpassed my master. It's time to choose my path to be reborn and face the evils of the MapleWorld again or to continue to fight for eternity as I am now and gaining more strength.");          
        }
        
                if (Paragons == 10) {
            
                    if (getReborns() >= 1) {
                dropMessage(6, "[" + getName() + "]:" + "I have become more formidable.");    
                
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");     
        }
        }
                                if (Paragons == 30) {
                dropMessage(6, "[" + getName() + "]:" + "Yet another unsatisfiying achievement, I must get stronger.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                
                                                                if (Paragons == 70) {
                dropMessage(6, "[" + getName() + "]:" + "My enemies grow stronger, so must I.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                                                
                  if (Paragons == 120) {
                dropMessage(6, "[" + getName() + "]:" + "I have become more formidable.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                    if (Paragons == 200) {
                dropMessage(6, "[" + getName() + "]:" + "I must not waste time. I must keep training.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                    
                                                                    if (Paragons == 300) {
                dropMessage(6, "[" + getName() + "]:" + "Yet another unsatisfiying achievement, I must get stronger.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
  if (Paragons == 400) {
                dropMessage(6, "[" + getName() + "]:" + "I must not waste time. I must keep training.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
  
                                                                  if (Paragons == 500) {
                dropMessage(6, "[" + getName() + "]:" + "My enemies grow stronger, so must I.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
            
        }
        if (reborns >= 3) {
            final MapleInventory equip = client.getPlayer().getInventory(MapleInventoryType.EQUIPPED); //no need to refesh chr - Rummy
        //we can add it here since when you level up you get 3 ap points
        
        remainingAp += 1; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededExp();
        expL -= getNeedReborn();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        level += 1;
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededExp() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
       
        if (getReborns() == 1) {
            finishAchievement(2);
        }
        if (getReborns() == 70) {
            finishAchievement(3);
        }
        if (getReborns() == 120) {
            finishAchievement(4);
        }
        if (getReborns() == 200) {
            finishAchievement(5);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), false);
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        //Auto Job
        if (job >= 430 && job <= 434) { // Dual Blade
            switch (getLevel()) {
                case 20:
                    changeJob((short) 430);
                    break;
                case 30:
                    changeJob((short) 431);
                    break;
                case 55:
                    changeJob((short) 432);
                    break;
                case 70:
                    changeJob((short) 433);
                    break;
                case 120:
                    changeJob((short) 434);
                    break;
            }

        } 			
  

         else if (GameConstants.isAran(job)) { // Only one class
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2100);
                    break;
                case 30:
                    changeJob((short) 2110);
                    break;
                case 70:
                    changeJob((short) 2111);
                    break;
                case 120:
                    changeJob((short) 2112);
                    break;
            }

        } else if (GameConstants.isEvan(job)) { // 2218, 2217, 2216, 2215, 2214, 2213, 2212, 2211, 2210, 2200, 2001
            if (level >= 160 && job != 2218) {
                changeJob((short) 2218); // which jobs give level 70 job?
            } else if (level >= 120 && level <= 159 && job != 2217) {
                changeJob((short) 2217);
            } else if (level >= 100 && level <= 119 && job != 2216) {
                changeJob((short) 2216);
            } else if (level >= 80 && level <= 99 && job != 2215) {
                changeJob((short) 2215);
            } else if (level >= 60 && level <= 79 && job != 2214) {
                changeJob((short) 2214);
            } else if (level >= 50 && level <= 59 && job != 2213) {
                changeJob((short) 2213);
            } else if (level >= 40 && level <= 49 && job != 2212) {
                changeJob((short) 2212);
            } else if (level >= 30 && level <= 39 && job != 2211) {
                changeJob((short) 2211);
            } else if (level >= 20 && level <= 29 && job != 2210) {
                changeJob((short) 2210);
            } else if (level >= 10 && level <= 19 && job != 2200) {
                changeJob((short) 2200);
            }

        } else if (GameConstants.isDemon(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 3100);
                    setShield(1099000);
                    setWeapon(1322122);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 30:
                    changeJob((short) 3110);
                    setShield(1099002);
                    //setWeapon(1322124);
                   dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                   chr.fakeRelog();
                    break;
                case 70:
                    changeJob((short) 3111);
                    setShield(1099003);
                   // setWeapon(1322125);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 120:
                    changeJob((short) 3112);
                    setShield(1099004);
                   // setWeapon(1322127);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
            }

        }  else if (GameConstants.isMercedes(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2300);
                    break;
                case 30:
                    changeJob((short) 2310);
                    break;
                case 70:
                    changeJob((short) 2311);
                    break;
                case 120:
                    changeJob((short) 2312);
                    break;
            }
            
            } else if (GameConstants.isMihile(job)) { // Mihile
            switch (getLevel()) {
                case 1:
                    setWeapon(1302000);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 10:
                    changeJob((short) 5100);
                    setShield(1098000);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 30:
                    changeJob((short) 5110);
                    setShield(1098001);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 70:
                    changeJob((short) 5111);
                    setShield(1098002);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 120:
                    changeJob((short) 5112);
                    setShield(1098003);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
            }

        } 
            



            //  PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),



       
        



        //    PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),
        // End of auto job [someone optimize this]
        //if (map.getForceMove() > 0 && map.getForceMove() <= getLevel()) {
        //    changeMap(map.getReturnMap(), map.getReturnMap().getPortal(0));
        //    dropMessage(-1, "You have been expelled from the map.");
        //}
        
       if (level == 200) {
            finishAchievement(46);
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Congratulations] " + getName() + " has reached Level 200! Congratulate " + getName() + " on such an amazing achievement!")); 
               //reborn message is now with @rebirth/reborn command, makes more sense that way..
        }
                       if (level == 10) {
       dropMessage(6, "[" + getName() + "]:" + "I have become strong. Yet my enemies strength still outrank me, although I think I am ready to choose my Job. Time to see some masters and make my decision.");           
        }
        
                if (level == 30) {
       dropMessage(6, "[" + getName() + "]:" + "I have become stronger. I still have much to yearn, but I think I am ready to advance. It's time to see the master.");    
                 
               
                    
        }
                
                                if (level == 70) {
       dropMessage(6, "[" + getName() + "]:" + "I have become much stronger than before. I should go see my master to hopefully gain more power.");          
        }
                                                                if (level == 120) {
       dropMessage(6, "[" + getName() + "]:" + "I have become appaling to evil. So much untapped power. I should go back to my master to gain god-like powers.");          
        }
        
                                                                 if (level == 200) {
       dropMessage(6, "[" + getName() + "]:" + "I am scintillating with power. I have surpassed my master. It's time to choose my path to be reborn and face the evils of the MapleWorld again or to continue to fight for eternity as I am now and gaining more strength.");          
        }
        
                if (Paragons == 10) {
            
                    if (getReborns() >= 1) {
                dropMessage(6, "[" + getName() + "]:" + "I have become more formidable.");    
                
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");     
        }
        }
                                if (Paragons == 30) {
                dropMessage(6, "[" + getName() + "]:" + "Yet another unsatisfiying achievement, I must get stronger.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                
                                                                if (Paragons == 70) {
                dropMessage(6, "[" + getName() + "]:" + "My enemies grow stronger, so must I.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                                                
                  if (Paragons == 120) {
                dropMessage(6, "[" + getName() + "]:" + "I have become more formidable.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                    if (Paragons == 200) {
                dropMessage(6, "[" + getName() + "]:" + "I must not waste time. I must keep training.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                    
                                                                    if (Paragons == 300) {
                dropMessage(6, "[" + getName() + "]:" + "Yet another unsatisfiying achievement, I must get stronger.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
  if (Paragons == 400) {
                dropMessage(6, "[" + getName() + "]:" + "I must not waste time. I must keep training.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
  
                                                                  if (Paragons == 500) {
                dropMessage(6, "[" + getName() + "]:" + "My enemies grow stronger, so must I.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
            
        }
        else if (reborns == 0) {
        final MapleInventory equip = client.getPlayer().getInventory(MapleInventoryType.EQUIPPED); //no need to refesh chr - Rummy
        //we can add it here since when you level up you get 5 ap points
        
        remainingAp += 5; // all 5
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);//its already there lol
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp += stats.getTotalInt() / 10;
        

        exp -= getNeededExp();
        expL -= getNeedReborn();
        if (getOccupation() / 100 == 2) { // Gamer 
            final int chance = OccupationConstants.getGamerChance(getOccupation());
            if (chance > 0) {
                if (Randomizer.nextInt(100) < chance) {
                    level += 1; // or maybe give an item?..then can sell to players (instant level item)
                    dropMessage(-3, "Level UP! Yeah!");
                    dropMessage(-1, "[Game] You've obtained an extra level!");
                }
            }
        }
        level += 1;
        if (GameConstants.isKOC(job) && level < 120 && level > 10) {
            exp += getNeededExp() / 10;
        }
        if (level == 10 && MapConstants.isStorylineMap(mapid)) {
            MapleQuest.getInstance(50000).forceComplete(this, 0);
        }
       
        if (getReborns() == 1) {
            finishAchievement(2);
        }
        if (getReborns() == 70) {
            finishAchievement(3);
        }
        if (getReborns() == 120) {
            finishAchievement(4);
        }
        if (getReborns() == 200) {
            finishAchievement(5);
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        
        final Map<MapleStat, Integer> statup = new EnumMap<MapleStat, Integer>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, maxhp);
        statup.put(MapleStat.MP, maxmp);
        statup.put(MapleStat.EXP, exp);
        statup.put(MapleStat.LEVEL, (int) level);
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here

        if (level <= 10) { //are this part really important? ;O
            stats.str += remainingAp;
            remainingAp = 0;

            statup.put(MapleStat.STR, (int) stats.getStr());
        }
        if (LoginInformationProvider.isExtendedSpJob(job)) {
            if (level >= 11) {
                remainingSp[GameConstants.getSkillBook(job, level)] += 3;
            }
        } else if (level >= 11) {
            remainingSp[GameConstants.getSkillBook(job)] += 3;
        }

        statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        statup.put(MapleStat.AVAILABLESP, remainingSp[GameConstants.getSkillBook(job, level)]);
        stats.setInfo(maxhp, maxmp, maxhp, maxmp);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), false);
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        //Auto Job
        if (job >= 430 && job <= 434) { // Dual Blade
            switch (getLevel()) {
                case 20:
                    changeJob((short) 430);
                    break;
                case 30:
                    changeJob((short) 431);
                    break;
                case 55:
                    changeJob((short) 432);
                    break;
                case 70:
                    changeJob((short) 433);
                    break;
                case 120:
                    changeJob((short) 434);
                    break;
            }

        } 			
  

         else if (GameConstants.isAran(job)) { // Only one class
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2100);
                    break;
                case 30:
                    changeJob((short) 2110);
                    break;
                case 70:
                    changeJob((short) 2111);
                    break;
                case 120:
                    changeJob((short) 2112);
                    break;
            }

        } else if (GameConstants.isEvan(job)) { // 2218, 2217, 2216, 2215, 2214, 2213, 2212, 2211, 2210, 2200, 2001
            if (level >= 160 && job != 2218) {
                changeJob((short) 2218); // which jobs give level 70 job?
            } else if (level >= 120 && level <= 159 && job != 2217) {
                changeJob((short) 2217);
            } else if (level >= 100 && level <= 119 && job != 2216) {
                changeJob((short) 2216);
            } else if (level >= 80 && level <= 99 && job != 2215) {
                changeJob((short) 2215);
            } else if (level >= 60 && level <= 79 && job != 2214) {
                changeJob((short) 2214);
            } else if (level >= 50 && level <= 59 && job != 2213) {
                changeJob((short) 2213);
            } else if (level >= 40 && level <= 49 && job != 2212) {
                changeJob((short) 2212);
            } else if (level >= 30 && level <= 39 && job != 2211) {
                changeJob((short) 2211);
            } else if (level >= 20 && level <= 29 && job != 2210) {
                changeJob((short) 2210);
            } else if (level >= 10 && level <= 19 && job != 2200) {
                changeJob((short) 2200);
            }

        } else if (GameConstants.isDemon(job)) {
            switch (getLevel()) {
               case 10:
                    changeJob((short) 3100);
                    setShield(1099000);
                    setWeapon(1322122);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 30:
                    changeJob((short) 3110);
                    setShield(1099002);
                    //setWeapon(1322124);
                   dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                   chr.fakeRelog();
                    break;
                case 70:
                    changeJob((short) 3111);
                    setShield(1099003);
                   // setWeapon(1322125);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 120:
                    changeJob((short) 3112);
                    setShield(1099004);
                   // setWeapon(1322127);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
            }

        }  else if (GameConstants.isMercedes(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2300);
                    break;
                case 30:
                    changeJob((short) 2310);
                    break;
                case 70:
                    changeJob((short) 2311);
                    break;
                case 120:
                    changeJob((short) 2312);
                    break;
            }
            
            } else if (GameConstants.isMihile(job)) { // Mihile
            switch (getLevel()) {
                 case 1:
                    setWeapon(1302000);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 10:
                    changeJob((short) 5100);
                    setShield(1098000);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 30:
                    changeJob((short) 5110);
                    setShield(1098001);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 70:
                    changeJob((short) 5111);
                    setShield(1098002);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
                case 120:
                    changeJob((short) 5112);
                    setShield(1098003);
                    dropMessage(6, "If you do not see your new shield/wep please CC, Relog, Or go in & out of CS.");
                    chr.fakeRelog();
                    break;
            }

        } 
            



            //  PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),



       
        



        //    PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),
        // End of auto job [someone optimize this]
        //if (map.getForceMove() > 0 && map.getForceMove() <= getLevel()) {
        //    changeMap(map.getReturnMap(), map.getReturnMap().getPortal(0));
        //    dropMessage(-1, "You have been expelled from the map.");
        //}
        
        if (level == 200) {
            finishAchievement(46);
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Congratulations] " + getName() + " has reached Level 200! Congratulate " + getName() + " on such an amazing achievement!")); 
               //reborn message is now with @rebirth/reborn command, makes more sense that way..
        }
                       if (level == 10) {
       dropMessage(6, "[" + getName() + "]:" + "I have become strong. Yet my enemies strength still outrank me, although I think I am ready to choose my Job. Time to see some masters and make my decision.");           
        }
        
                if (level == 30) {
       dropMessage(6, "[" + getName() + "]:" + "I have become stronger. I still have much to yearn, but I think I am ready to advance. It's time to see the master.");    
                 
               
                    
        }
                
                                if (level == 70) {
       dropMessage(6, "[" + getName() + "]:" + "I have become much stronger than before. I should go see my master to hopefully gain more power.");          
        }
                                                                if (level == 120) {
       dropMessage(6, "[" + getName() + "]:" + "I have become appaling to evil. So much untapped power. I should go back to my master to gain god-like powers.");          
        }
        
                                                                 if (level == 200) {
       dropMessage(6, "[" + getName() + "]:" + "I am scintillating with power. I have surpassed my master. It's time to choose my path to be reborn and face the evils of the MapleWorld again or to continue to fight for eternity as I am now and gaining more strength.");          
        }
        
                if (Paragons == 10) {
            
                    if (getReborns() >= 1) {
                dropMessage(6, "[" + getName() + "]:" + "I have become more formidable.");    
                
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");     
        }
        }
                                if (Paragons == 30) {
                dropMessage(6, "[" + getName() + "]:" + "Yet another unsatisfiying achievement, I must get stronger.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                
                                                                if (Paragons == 70) {
                dropMessage(6, "[" + getName() + "]:" + "My enemies grow stronger, so must I.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                                                
                  if (Paragons == 120) {
                dropMessage(6, "[" + getName() + "]:" + "I have become more formidable.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                    if (Paragons == 200) {
                dropMessage(6, "[" + getName() + "]:" + "I must not waste time. I must keep training.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                                    
                                                                    if (Paragons == 300) {
                dropMessage(6, "[" + getName() + "]:" + "Yet another unsatisfiying achievement, I must get stronger.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
  if (Paragons == 400) {
                dropMessage(6, "[" + getName() + "]:" + "I must not waste time. I must keep training.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
  
                                                                  if (Paragons == 500) {
                dropMessage(6, "[" + getName() + "]:" + "My enemies grow stronger, so must I.");    
                dropMessage(6, "[" + getName() + "]:" + "I may now do @Evolve to evolve one item.");
        }
                        
        }
    }

    public void changeKeybinding(int key, byte type, int action) {
        if (type != 0) {
            keylayout.Layout().put(Integer.valueOf(key), new Pair<Byte, Integer>(type, action));
        } else {
            keylayout.Layout().remove(Integer.valueOf(key));
        }
    }

    public void sendMacros() {
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] != null) {
                client.getSession().write(CField.getMacros(skillMacros));
                break;
            }
        }
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
        changed_skillmacros = true;
    }

    public final SkillMacro[] getMacros() {
        return skillMacros;
    }

    public void tempban(String reason, Calendar duration, int greason, boolean IPMac) {
        if (IPMac) {
            client.banMacs();
        }
        client.getSession().write(CWvsContext.GMPoliceMessage(true));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (IPMac) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSession().getRemoteAddress().toString().split(":")[0]);
                ps.execute();
                ps.close();
            }

            client.getSession().close();

            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
        }

    }

    public int getMaxHp() {
        return getStat().getMaxHp();
    }

    public int getMaxMp() {
        return getStat().getMaxMp();
    }

    public void setHp(int amount) {
        getStat().setHp(amount, this);
    }

    public void setMp(int amount) {
        getStat().setMp(amount, this);
    }
    
    public final boolean hwid(String reason) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Hwid ban a non-existent character");
        }
        client.getSession().write(CWvsContext.GMPoliceMessage(true));
        try {
            Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO hwidbans VALUES (DEFAULT, ?)"); //make sure you make a SQL table l0l
            ps.setString(1, client.getHWID());
                ps.execute();
                ps.close();
            
        }
        catch (SQLException ex) {
            System.err.println("Error while banning the hwid :c" + ex);
            return false;
        }
        client.getSession().close();
        return true;
    }
    

    public final boolean ban(String reason, boolean IPMac, boolean autoban, boolean hellban) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        client.getSession().write(CWvsContext.GMPoliceMessage(true));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, autoban ? 2 : 1);
            ps.setString(2, reason);
            ps.setInt(3, accountid);
            ps.execute();
            ps.close();

            if (IPMac) {
                client.banMacs();
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSessionIPAddress());
                ps.execute();
                ps.close();

                if (hellban) {
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, accountid);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE email = ? OR SessionIP = ?");
                        pss.setInt(1, autoban ? 2 : 1);
                        pss.setString(2, reason);
                        pss.setString(3, rsa.getString("email"));
                        pss.setString(4, client.getSessionIPAddress());
                        pss.execute();
                        pss.close();
                    }
                    rsa.close();
                    psa.close();
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
            return false;
        }
        client.getSession().close();
        return true;
    }

    public static boolean ban(String id, String reason, boolean accountId, int gmlevel, boolean hellban) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.execute();
                ps.close();
                return true;
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int z = rs.getInt(1);
                PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ? AND gm < ?");
                psb.setString(1, reason);
                psb.setInt(2, z);
                psb.setInt(3, gmlevel);
                psb.execute();
                psb.close();

                if (gmlevel > 100) { //admin ban
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, z);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        String sessionIP = rsa.getString("sessionIP");
                        if (sessionIP != null && sessionIP.matches("/[0-9]{1,3}\\..*")) {
                            PreparedStatement psz = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                            psz.setString(1, sessionIP);
                            psz.execute();
                            psz.close();
                        }
                        if (rsa.getString("macs") != null) {
                            String[] macData = rsa.getString("macs").split(", ");
                            if (macData.length > 0) {
                                MapleClient.banMacs(macData);
                            }
                        }
                        if (hellban) {
                            PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE email = ?" + (sessionIP == null ? "" : " OR SessionIP = ?"));
                            pss.setString(1, reason);
                            pss.setString(2, rsa.getString("email"));
                            if (sessionIP != null) {
                                pss.setString(3, sessionIP);
                            }
                            pss.execute();
                            pss.close();
                        }
                    }
                    rsa.close();
                    psa.close();
                }
                ret = true;
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
        }
        return false;
    }

    /**
     * Oid of players is always = the cid
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * Throws unsupported operation exception, oid of players is read only
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        if (clone) {
            return;
        }
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.add(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        if (clone) {
            return;
        }
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.remove(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        visibleMapObjectsLock.readLock().lock();
        try {
            return !clone && visibleMapObjects.contains(mo);
        } finally {
            visibleMapObjectsLock.readLock().unlock();
        }
    }

    public Collection<MapleMapObject> getAndWriteLockVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().lock();
        return visibleMapObjects;
    }

    public void unlockWriteVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().unlock();
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(CField.removePlayerFromMap(this.getObjectId()));
        for (final WeakReference<MapleCharacter> chr : clones) {
            if (chr.get() != null) {
                chr.get().sendDestroyData(client);
            }
        }
        //don't need this, client takes care of it
        /* oh ok :(
         * if (dragon != null) {
         * client.getSession().write(CField.removeDragon(this.getId())); } if
         * (android != null) {
         * client.getSession().write(CField.deactivateAndroid(this.getId())); }
         * if (summonedFamiliar != null) {
         * client.getSession().write(CField.removeFamiliar(this.getId())); }
         */
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (client.getPlayer().allowedToTarget(this)) {
            client.getSession().write(CField.spawnPlayerMapobject(this));

            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    client.getSession().write(PetPacket.showPet(this, pet, false, false));
                }
            }
            for (final WeakReference<MapleCharacter> chr : clones) {
                if (chr.get() != null) {
                    chr.get().sendSpawnData(client);
                }
            }
            if (dragon != null) {
                client.getSession().write(CField.spawnDragon(dragon));
            }
            if (android != null) {
                client.getSession().write(CField.spawnAndroid(this, android));
            }
            if (summonedFamiliar != null) {
                client.getSession().write(CField.spawnFamiliar(summonedFamiliar, true, true));
            }
            if (summons != null && summons.size() > 0) {
                summonsLock.readLock().lock();
                try {
                    for (final MapleSummon summon : summons) {
                        client.getSession().write(SummonPacket.spawnSummon(summon, false));
                    }
                } finally {
                    summonsLock.readLock().unlock();
                }
            }
            if (followid > 0 && followon) {
                client.getSession().write(CField.followEffect(followinitiator ? followid : id, followinitiator ? id : followid, null));
            }
        }
    }

    public final void equipChanged() {
        if (map == null) {
            return;
        }
        map.broadcastMessage(this, CField.updateCharLook(this), false);
        stats.recalcLocalStats(this);
        if (getMessenger() != null) {
            World.Messenger.updateMessenger(getMessenger().getId(), getName(), client.getChannel());
        }
    }

    public final MaplePet getPet(final int index) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (count == index) {
                    return pet;
                }
                count++;
            }
        }
        return null;
    }

    public void removePetCS(MaplePet pet) {
        pets.remove(pet);
    }

    public void addPet(final MaplePet pet) {
        if (pets.contains(pet)) {
            pets.remove(pet);
        }
        pets.add(pet);
        // So that the pet will be at the last
        // Pet index logic :(
    }

    public void removePet(MaplePet pet, boolean shiftLeft) {
        pet.setSummoned(0);
        /*
         * int slot = -1; for (int i = 0; i < 3; i++) { if (pets[i] != null) {
         * if (pets[i].getUniqueId() == pet.getUniqueId()) { pets[i] = null;
         * slot = i; break; } } } if (shiftLeft) { if (slot > -1) { for (int i =
         * slot; i < 3; i++) { if (i != 2) { pets[i] = pets[i + 1]; } else {
         * pets[i] = null; } } } }
         */
    }

    public final byte getPetIndex(final MaplePet petz) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == petz.getUniqueId()) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final byte getPetIndex(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final List<MaplePet> getSummonedPets() {
        List<MaplePet> ret = new ArrayList<MaplePet>();
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                ret.add(pet);
            }
        }
        return ret;
    }

    public final byte getPetById(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getPetItemId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final List<MaplePet> getPets() {
        return pets;
    }

    public final void unequipAllPets() {
        for (final MaplePet pet : pets) {
            if (pet != null) {
                unequipPet(pet, true, false);
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shiftLeft, boolean hunger) {
        if (pet.getSummoned()) {
            pet.saveToDb();

            client.getSession().write(PetPacket.updatePet(pet, getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), false));
            if (map != null) {
                map.broadcastMessage(this, PetPacket.showPet(this, pet, true, hunger), true);
            }
            removePet(pet, shiftLeft);
            //List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>();
            //stats.put(MapleStat.PET, Integer.valueOf(0)));
            //showpetupdate isn't done here...
            if (GameConstants.GMS) {
                client.getSession().write(PetPacket.petStatUpdate(this));
            }
            client.getSession().write(CWvsContext.enableActions());
        }
    }

    /*
     * public void shiftPetsRight() { if (pets[2] == null) { pets[2] = pets[1];
     * pets[1] = pets[0]; pets[0] = null; } }
     */
    public final long getLastFameTime() {
        return lastfametime;
    }

    public final List<Integer> getFamedCharacters() {
        return lastmonthfameids;
    }

    public final List<Integer> getBattledCharacters() {
        return lastmonthbattleids;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastfametime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (from == null || lastmonthfameids == null || lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        }
        return FameStatus.OK;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(Integer.valueOf(to.getId()));
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog for char " + getName() + " to " + to.getName() + e);
        }
    }

    public boolean canBattle(MapleCharacter to) {
        if (to == null || lastmonthbattleids == null || lastmonthbattleids.contains(Integer.valueOf(to.getAccountID()))) {
            return false;
        }
        return true;
    }

    public void hasBattled(MapleCharacter to) {
        lastmonthbattleids.add(Integer.valueOf(to.getAccountID()));
        Connection con = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO battlelog (accid, accid_to) VALUES (?, ?)");
            ps.setInt(1, getAccountID());
            ps.setInt(2, to.getAccountID());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing battlelog for char " + getName() + " to " + to.getName() + e);
        }
    }

    public final MapleKeyLayout getKeyLayout() {
        return this.keylayout;
    }

    public MapleParty getParty() {
        if (party == null) {
            return null;
        } else if (party.isDisbanded()) {
            party = null;
        }
        return party;
    }

    public byte getWorld() {
        return world;
    }

    public void setWorld(byte world) {
        this.world = world;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<MapleDoor>(doors);
    }

    public void addMechDoor(MechDoor door) {
        mechDoors.add(door);
    }

    public void clearMechDoors() {
        mechDoors.clear();
    }

    public List<MechDoor> getMechDoors() {
        return new ArrayList<MechDoor>(mechDoors);
    }

    public void setSmega() {
        if (smega) {
            smega = false;
            dropMessage(5, "You have set megaphone to disabled mode");
        } else {
            smega = true;
            dropMessage(5, "You have set megaphone to enabled mode");
        }
    }

    public boolean getSmega() {
        return smega;
    }

    public List<MapleSummon> getSummonsReadLock() {
        summonsLock.readLock().lock();
        return summons;
    }

    public int getSummonsSize() {
        return summons.size();
    }

    public void unlockSummonsReadLock() {
        summonsLock.readLock().unlock();
    }

    public void addSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.add(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public void removeSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.remove(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setChair(int chair) {
        this.chair = chair;
        stats.relocHeal(this);
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public int getFamilyId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getFamilyId();
    }

    public int getSeniorId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getSeniorId();
    }

    public int getJunior1() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior1();
    }

    public int getJunior2() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior2();
    }

    public int getCurrentRep() {
        return currentrep;
    }

    public int getTotalRep() {
        return totalrep;
    }

    public void setCurrentRep(int _rank) {
        currentrep = _rank;
        if (mfc != null) {
            mfc.setCurrentRep(_rank);
        }
    }

    public void setTotalRep(int _rank) {
        totalrep = _rank;
        if (mfc != null) {
            mfc.setTotalRep(_rank);
        }
    }

    public int getTotalWins() {
        return totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void increaseTotalWins() {
        totalWins++;
    }

    public void increaseTotalLosses() {
        totalLosses++;
    }

    public int getGuildId() {
        return guildid;
    }

    public byte getGuildRank() {
        return guildrank;
    }

    public int getGuildContribution() {
        return guildContribution;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
            guildContribution = 0;
        }
    }

    public void setGuildRank(byte _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setGuildContribution(int _c) {
        this.guildContribution = _c;
        if (mgc != null) {
            mgc.setGuildContribution(_c);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public void setAllianceRank(byte rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public byte getAllianceRank() {
        return allianceRank;
    }

    public MapleGuild getGuild() {
        if (getGuildId() <= 0) {
            return null;
        }
        return World.Guild.getGuild(getGuildId());
    }

    public void setJob(int j) {
        this.job = (short) j;
    }

    public void guildUpdate() {
        if (guildid <= 0) {
            return;
        }
        mgc.setLevel((short) level);
        mgc.setJobId(job);
        World.Guild.memberLevelJobUpdate(mgc);
    }

    public void saveGuildStatus() {
        MapleGuild.setOfflineGuildStatus(guildid, guildrank, guildContribution, allianceRank, id);
    }

    public void familyUpdate() {
        if (mfc == null) {
            return;
        }
        World.Family.memberFamilyUpdate(mfc, this);
    }

    public void saveFamilyStatus() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET familyid = ?, seniorid = ?, junior1 = ?, junior2 = ? WHERE id = ?");
            if (mfc == null) {
                ps.setInt(1, 0);
                ps.setInt(2, 0);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
            } else {
                ps.setInt(1, mfc.getFamilyId());
                ps.setInt(2, mfc.getSeniorId());
                ps.setInt(3, mfc.getJunior1());
                ps.setInt(4, mfc.getJunior2());
            }
            ps.setInt(5, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
            se.printStackTrace();
        }
        //MapleFamily.setOfflineFamilyStatus(familyid, seniorid, junior1, junior2, currentrep, totalrep, id);
    }

    public void modifyCSPoints(int type, int quantity) {
        modifyCSPoints(type, quantity, false);
    }

    public void modifyCSPoints(int type, int quantity, boolean show) {

        switch (type) {
            case 1:
                if (nxcredit + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max cash. No cash will be awarded.");
                    }
                    return;
                }
                if (quantity > 0 && GameConstants.GMS) {
                    quantity = (quantity / 2); //stuff is cheaper lol
                }
                nxcredit += quantity;
                break;
            case 4:
                if (acash + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max cash. No cash will be awarded.");
                    }
                    return;
                }
                if (quantity > 0 && GameConstants.GMS) {
                    quantity = (quantity / 2); //stuff is cheaper lol
                }
                acash += quantity;
                break;
            case 2:
                if (maplepoints + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max maple points. No cash will be awarded.");
                    }
                    return;
                }
                maplepoints += quantity;
                break;
            default:
                break;
        }
        if (show && quantity != 0) {
            dropMessage(-1, "You have " + (quantity > 0 ? "gained " : "lost ") + quantity + (type == 1 ? " cash." : " maple points."));
            client.getSession().write(EffectPacket.showForeignEffect(20));
        }
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
                return nxcredit;
            case 4:
                return acash;
            case 2:
                return maplepoints;
            default:
                return 0;
        }
    }

    public final boolean hasEquipped(int itemid) {
        return inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid) >= 1;
    }

    public final boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        int possesed = inventory[type.ordinal()].countById(itemid);
        if (checkEquipped && type == MapleInventoryType.EQUIP) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        if (greaterOrEquals) {
            return possesed >= quantity;
        } else {
            return possesed == quantity;
        }
    }

    public final boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, true, true);
    }

    public final boolean haveItem(int itemid) {
        return haveItem(itemid, 1, true, true);
    }

    public static boolean tempban(String reason, Calendar duration, int greason, int accountid) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.executeUpdate();
            ps.close();
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            //log.error("Error while tempbanning", ex);
        }
        return false;
    }

    public void setGM(byte level) {
        this.gmLevel = level;
    }

 
    public static enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public byte getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    public void setBuddyCapacity(byte capacity) {
        buddylist.setCapacity(capacity);
        client.getSession().write(BuddylistPacket.updateBuddyCapacity(capacity));
    }
   public boolean getPetAutoFeed(){
        return petAutoFeed;
    }
    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void addCooldown(int skillId, long startTime, long length) {
        coolDowns.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(skillId, startTime, length));
    }

    public void removeCooldown(int skillId) {
        if (coolDowns.containsKey(Integer.valueOf(skillId))) {
            coolDowns.remove(Integer.valueOf(skillId));
        }
    }

    public boolean skillisCooling(int skillId) {
        return coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        addCooldown(skillid, starttime, length);
    }

    public void giveCoolDowns(final List<MapleCoolDownValueHolder> cooldowns) {
        int time;
        if (cooldowns != null) {
            for (MapleCoolDownValueHolder cooldown : cooldowns) {
                coolDowns.put(cooldown.skillId, cooldown);
            }
        } else {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?");
                ps.setInt(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                        continue;
                    }
                    giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
                }
                ps.close();
                rs.close();
                deleteWhereCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");

            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
            }
        }
    }

    public int getCooldownSize() {
        return coolDowns.size();
    }

    public int getDiseaseSize() {
        return diseases.size();
    }

    public List<MapleCoolDownValueHolder> getCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<MapleCoolDownValueHolder>();
        for (MapleCoolDownValueHolder mc : coolDowns.values()) {
            if (mc != null) {
                ret.add(mc);
            }
        }
        return ret;
    }

    public final List<MapleDiseaseValueHolder> getAllDiseases() {
        return new ArrayList<MapleDiseaseValueHolder>(diseases.values());
    }

    public final boolean hasDisease(final MapleDisease dis) {
        return diseases.containsKey(dis);
    }

     public void giveDebuff(MapleDisease disease, MobSkill skill) {
/* 5181 */     giveDebuff(disease, skill.getX(), skill.getDuration(), skill.getSkillId(), skill.getSkillLevel());
/*      */   }
/*      */ 
/*      */       public void giveDebuff(MapleDisease disease, int x, long duration, int skillid, int level) {
            if ((this.map != null) && (!hasDisease(disease))) {
            if ((disease != MapleDisease.SEDUCE) && (disease != MapleDisease.STUN) && (disease != MapleDisease.FLAG)
                    && (getBuffedValue(MapleBuffStat.HOLY_SHIELD) != null)) {
                return;
            }

            int mC = getBuffSource(MapleBuffStat.MECH_CHANGE);
            if ((mC > 0) && (mC != 35121005)) {
                return;
            }
            if ((this.stats.ASR > 0) && (Randomizer.nextInt(100) < this.stats.ASR)) {
                return;
            }
            if (isVaccinated() == 0) {
            this.diseases.put(disease, new MapleDiseaseValueHolder(disease, System.currentTimeMillis(), duration - this.stats.decreaseDebuff));
            this.client.getSession().write(CWvsContext.BuffPacket.giveDebuff(disease, x, skillid, level, (int) duration));
            this.map.broadcastMessage(this, CWvsContext.BuffPacket.giveForeignDebuff(this.id, disease, skillid, level, x), false);
            return;
            }

            if ((x > 0) && (disease == MapleDisease.POISON)) {
                addHP((int) (-(x * ((duration - this.stats.decreaseDebuff) / 1000L))));
            }
        }
    }

    public final void giveSilentDebuff(final List<MapleDiseaseValueHolder> ld) {
        if (ld != null) {
            for (final MapleDiseaseValueHolder disease : ld) {
                diseases.put(disease.disease, disease);
            }
        }
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (hasDisease(debuff)) {
            client.getSession().write(BuffPacket.cancelDebuff(debuff));
            map.broadcastMessage(this, BuffPacket.cancelForeignDebuff(id, debuff), false);

            diseases.remove(debuff);
        }
    }

    public void dispelDebuffs() {
        List<MapleDisease> diseasess = new ArrayList<MapleDisease>(diseases.keySet());
        for (MapleDisease d : diseasess) {
            dispelDebuff(d);
        }
    }
    
//    public Map<Byte, Integer> getTotems() {
//        final Map<Byte, Integer> eq = new HashMap<>();
//        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
//            eq.put((byte) (item.getPosition() + 5000), item.getItemId());
//        }
//        return eq;
//    }

    public void cancelAllDebuffs() {
        diseases.clear();
    }

    public void setLevel(final short level) {
        this.level = level;
    }

    public void sendNote(String to, String msg) {
        sendNote(to, msg, 0);
    }

    public void sendNote(String to, String msg, int fame) {
        MapleCharacterUtil.sendNote(to, getName(), msg, fame);
    }

    public void showNote() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, getName());
            ResultSet rs = ps.executeQuery();
            rs.last();
            int count = rs.getRow();
            rs.first();
            client.getSession().write(MTSCSPacket.showNotes(rs, count));
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
        }
    }

    public void deleteNote(int id, int fame) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT gift FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("gift") == fame && fame > 0) { //not exploited! hurray
                    addFame(fame);
                    updateSingleStat(MapleStat.FAME, getFame());
                    client.getSession().write(InfoPacket.getShowFameGain(fame));
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
        }
    }

    public int getMulungEnergy() {
        return mulung_energy;
    }

    public void mulung_EnergyModify(boolean inc) {
        if (inc) {
            if (mulung_energy + 100 > 10000) {
                mulung_energy = 10000;
            } else {
                mulung_energy += 100;
            }
        } else {
            mulung_energy = 0;
        }
        client.getSession().write(CWvsContext.MulungEnergy(mulung_energy));
    }

    public void writeMulungEnergy() {
        client.getSession().write(CWvsContext.MulungEnergy(mulung_energy));
    }

    public void writeEnergy(String type, String inc) {
        client.getSession().write(CWvsContext.sendPyramidEnergy(type, inc));
    }

    public void writeStatus(String type, String inc) {
        client.getSession().write(CWvsContext.sendGhostStatus(type, inc));
    }

    public void writePoint(String type, String inc) {
        client.getSession().write(CWvsContext.sendGhostPoint(type, inc));
    }

    public final short getCombo() {
        return combo;
    }

    public void setCombo(final short combo) {
        this.combo = combo;
    }

    public final long getLastCombo() {
        return lastCombo;
    }

    public void setLastCombo(final long combo) {
        this.lastCombo = combo;
    }

    public final long getKeyDownSkill_Time() {
        return keydown_skill;
    }

    public void setKeyDownSkill_Time(final long keydown_skill) {
        this.keydown_skill = keydown_skill;
    }

    public void checkBerserk() { //berserk is special in that it doesn't use worldtimer :)
        if (job != 132 || lastBerserkTime < 0 || lastBerserkTime + 10000 > System.currentTimeMillis()) {
            return;
        }
        final Skill BerserkX = SkillFactory.getSkill(1320006);
        final int skilllevel = getTotalSkillLevel(BerserkX);
        if (skilllevel >= 1 && map != null) {
            lastBerserkTime = System.currentTimeMillis();
            final MapleStatEffect ampStat = BerserkX.getEffect(skilllevel);
            stats.Berserk = stats.getHp() * 100 / stats.getCurrentMaxHp() >= ampStat.getX();
            client.getSession().write(EffectPacket.showOwnBuffEffect(1320006, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)));
            map.broadcastMessage(this, EffectPacket.showBuffeffect(getId(), 1320006, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)), false);
        } else {
            lastBerserkTime = -1; // somebody thre? O_O
        }
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
        if (map != null) {
            map.broadcastMessage(MTSCSPacket.useChalkboard(getId(), text));
        }
    }

    public String getChalkboard() {
        return chalktext;
    }

    public MapleMount getMount() {
        return mount;
    }

    public int[] getWishlist() {
        return wishlist;
    }

    public void clearWishlist() {
        for (int i = 0; i < 10; i++) {
            wishlist[i] = 0;
        }
        changed_wishlist = true;
    }

    public int getWishlistSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (wishlist[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public void setWishlist(int[] wl) {
        this.wishlist = wl;
        changed_wishlist = true;
    }

    public int[] getRocks() {
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == map) {
                rocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addRockMap() {
        if (getRockSize() >= 10) {
            return;
        }
        rocks[getRockSize()] = getMapId();
        changed_trocklocations = true;
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getRegRocks() {
        return regrocks;
    }

    public int getRegRockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRegRocks(int map) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == map) {
                regrocks[i] = 999999999;
                changed_regrocklocations = true;
                break;
            }
        }
    }

    public void addRegRockMap() {
        if (getRegRockSize() >= 5) {
            return;
        }
        regrocks[getRegRockSize()] = getMapId();
        changed_regrocklocations = true;
    }

    public boolean isRegRockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getHyperRocks() {
        return hyperrocks;
    }

    public int getHyperRockSize() {
        int ret = 0;
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromHyperRocks(int map) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == map) {
                hyperrocks[i] = 999999999;
                changed_hyperrocklocations = true;
                break;
            }
        }
    }

    public void addHyperRockMap() {
        if (getRegRockSize() >= 13) {
            return;
        }
        hyperrocks[getHyperRockSize()] = getMapId();
        changed_hyperrocklocations = true;
    }

    public boolean isHyperRockMap(int id) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public List<LifeMovementFragment> getLastRes() {
        return lastres;
    }

    public void setLastRes(List<LifeMovementFragment> lastres) {
        this.lastres = lastres;
    }

    public void dropMessage(int type, String message) {
        if (type == -1) {
            client.getSession().write(CWvsContext.getTopMsg(message));
        } else if (type == -2) {
            client.getSession().write(PlayerShopPacket.shopChat(message, 0)); //0 or what
        } else if (type == -3) {
            client.getSession().write(CField.getChatText(getId(), message, isSuperGM(), 0)); //1 = hide
        } else if (type == -4) {
            client.getSession().write(CField.getChatText(getId(), message, isSuperGM(), 1)); //1 = hide
        } else if (type == -5) {
            client.getSession().write(CField.getGameMessage(message, false)); //pink
        } else if (type == -6) {
            client.getSession().write(CField.getGameMessage(message, true)); //white bg
        } else if (type == -7) {
            client.getSession().write(CWvsContext.getMidMsg(message, false, 0));
        } else if (type == -8) {
            client.getSession().write(CWvsContext.getMidMsg(message, true, 0));
        } else {
            client.getSession().write(CWvsContext.serverNotice(type, message));
        }
    }

    public IMaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(IMaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public int getConversation() {
        return inst.get();
    }

    public void setConversation(int inst) {
        this.inst.set(inst);
    }

    public int getDirection() {
        return insd.get();
    }

    public void setDirection(int inst) {
        this.insd.set(inst);
    }

    public MapleCarnivalParty getCarnivalParty() {
        return carnivalParty;
    }

    public void setCarnivalParty(MapleCarnivalParty party) {
        carnivalParty = party;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
    }

    public void useCP(int ammount) {
        availableCP -= ammount;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void resetCP() {
        totalCP = 0;
        availableCP = 0;
    }

    public void addCarnivalRequest(MapleCarnivalChallenge request) {
        pendingCarnivalRequests.add(request);
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return pendingCarnivalRequests.pollLast();
    }

    public void clearCarnivalRequests() {
        pendingCarnivalRequests = new LinkedList<MapleCarnivalChallenge>();
    }

    public void startMonsterCarnival(final int enemyavailable, final int enemytotal) {
        client.getSession().write(MonsterCarnivalPacket.startMonsterCarnival(this, enemyavailable, enemytotal));
    }
     public java.util.Timer getHealTimer() {
        return healTimer;
    }

    public void setHealTimer(java.util.Timer timer) {
        healTimer = timer;
    }

    public java.util.Timer getDFRecoveryTimer() {
        return DFRecoveryTimer;
    }

    public void setDFRecoveryTimer(java.util.Timer timer) {
        DFRecoveryTimer = timer;
    }

    public void CPUpdate(final boolean party, final int available, final int total, final int team) {
        client.getSession().write(MonsterCarnivalPacket.CPUpdate(party, available, total, team));
    }

    public void playerDiedCPQ(final String name, final int lostCP, final int team) {
        client.getSession().write(MonsterCarnivalPacket.playerDiedMessage(name, lostCP, team));
    }
    
    

    public void setAchievementFinished(int id) {
        if (!finishedAchievements.contains(id)) {
            finishedAchievements.add(id);
            changed_achievements = true;
        }
    }

    public boolean achievementFinished(int achievementid) {
        return finishedAchievements.contains(achievementid);
    }

    public void finishAchievement(int id) {
        if (!achievementFinished(id)) {
            if (isAlive() && !isClone()) {
                MapleAchievements.getInstance().getById(id).finishAchievement(this);
            }
        }
    }

    public List<Integer> getFinishedAchievements() {
        return finishedAchievements;
    }

    public boolean getCanTalk() {
        return this.canTalk;
    }

    public void canTalk(boolean talk) {
        this.canTalk = talk;
    }

    public double getEXPMod() {
        return stats.expMod;
    }

    public double getDropMod() {
        return stats.dropMod;
    }

    public int getCashMod() {
        return stats.cashMod;
    }

    public void setPoints(int p) {
        this.points = p;
        if (this.points >= 1) {
            finishAchievement(1);
        }
    }

    public int getPoints() {
        return points;
    }
    
    public int getNX() {
        return nxcredit;
    }


    public void setVPoints(int p) {
        this.vpoints = p;
    }
    public int getVPoints() {
        return vpoints;
    }
    
    public int getDonatorPoints() {
        return DonatorPoints;
    }

    public int getGMlevel() {
        return gmLevel;
    }

    public CashShop getCashInventory() {
        return cs;
    }

    public void removeItem(int id, int quantity) {
        MapleInventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        client.getSession().write(InfoPacket.getShowItemGain(id, (short) quantity, true));
    }

    public void removeAll(int id) {
        removeAll(id, true);
    }

    public void removeAll(int id, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().write(InfoPacket.getShowItemGain(id, (short) -possessed, true));
            }
        }
        /*
         * if (type == MapleInventoryType.EQUIP) { //check equipped type =
         * MapleInventoryType.EQUIPPED; possessed =
         * getInventory(type).countById(id);
         *
         * if (possessed > 0) {
         * MapleInventoryManipulator.removeById(getClient(), type, id,
         * possessed, true, false);
         * getClient().getSession().write(CField.getShowItemGain(id,
         * (short)-possessed, true)); } }
         */
    }

    public Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> getRings(boolean equip) {
        MapleInventory iv = getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        List<MapleRing> crings = new ArrayList<MapleRing>(), frings = new ArrayList<MapleRing>(), mrings = new ArrayList<MapleRing>();
        MapleRing ring;
        for (Item ite : equipped) {
            Equip item = (Equip) ite;
            if (item.getRing() != null) {
                ring = item.getRing();
                ring.setEquipped(true);
                if (GameConstants.isEffectRing(item.getItemId())) {
                    if (equip) {
                        if (GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (GameConstants.isMarriageRing(item.getItemId())) {
                            mrings.add(ring);
                        }
                    } else {
                        if (crings.size() == 0 && GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (frings.size() == 0 && GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (mrings.size() == 0 && GameConstants.isMarriageRing(item.getItemId())) {
                            mrings.add(ring);
                        } //for 3rd person the actual slot doesnt matter, so we'll use this to have both shirt/ring same?
                        //however there seems to be something else behind this, will have to sniff someone with shirt and ring, or more conveniently 3-4 of those
                    }
                }
            }
        }
        if (equip) {
            iv = getInventory(MapleInventoryType.EQUIP);
            for (Item ite : iv.list()) {
                Equip item = (Equip) ite;
                if (item.getRing() != null && GameConstants.isCrushRing(item.getItemId())) {
                    ring = item.getRing();
                    ring.setEquipped(false);
                    if (GameConstants.isFriendshipRing(item.getItemId())) {
                        frings.add(ring);
                    } else if (GameConstants.isCrushRing(item.getItemId())) {
                        crings.add(ring);
                    } else if (GameConstants.isMarriageRing(item.getItemId())) {
                        mrings.add(ring);
                    }
                }
            }
        }
        Collections.sort(frings, new MapleRing.RingComparator());
        Collections.sort(crings, new MapleRing.RingComparator());
        Collections.sort(mrings, new MapleRing.RingComparator());
        return new Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>>(crings, frings, mrings);
    }

    public int getFH() {
        MapleFoothold fh = getMap().getFootholds().findBelow(getTruePosition());
        if (fh != null) {
            return fh.getId();
        }
        return 0;
    }

    public void startFairySchedule(boolean exp) {
        startFairySchedule(exp, false);
    }

    public void startFairySchedule(boolean exp, boolean equipped) {
        cancelFairySchedule(exp || stats.equippedFairy == 0);
        if (fairyExp <= 0) {
            fairyExp = (byte) stats.equippedFairy;
        }
        if (equipped && fairyExp < stats.equippedFairy * 3 && stats.equippedFairy > 0) {
            dropMessage(5, "The Fairy Pendant's experience points will increase to " + (fairyExp + stats.equippedFairy) + "% after one hour.");
        }
        lastFairyTime = System.currentTimeMillis();
    }

    public final boolean canFairy(long now) {
        return lastFairyTime > 0 && lastFairyTime + (60 * 60 * 1000) < now;
    }

    public final boolean canHP(long now) {
        if (lastHPTime + 5000 < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMP(long now) {
        if (lastMPTime + 5000 < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canHPRecover(long now) {
        if (stats.hpRecoverTime > 0 && lastHPTime + stats.hpRecoverTime < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMPRecover(long now) {
        if (stats.mpRecoverTime > 0 && lastMPTime + stats.mpRecoverTime < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public void cancelFairySchedule(boolean exp) {
        lastFairyTime = 0;
        if (exp) {
            this.fairyExp = 0;
        }
    }

    public void doFairy() {
        if (fairyExp < stats.equippedFairy * 3 && stats.equippedFairy > 0) {
            fairyExp += stats.equippedFairy;
            dropMessage(5, "The Fairy Pendant's EXP was boosted to " + fairyExp + "%.");
        }
        if (getGuildId() > 0) {
            World.Guild.gainGP(getGuildId(), 20, id);
            client.getSession().write(InfoPacket.getGPContribution(20));
        }
        traits.get(MapleTraitType.will).addExp(5, this); //willpower every hour
        startFairySchedule(false, true);
    }

    public byte getFairyExp() {
        return fairyExp;
    }

    public int getTeam() {
        return coconutteam;
    }

    public void setTeam(int v) {
        this.coconutteam = v;
    }

    public void spawnPet(byte slot) {
        spawnPet(slot, false, true);
    }

    public void spawnPet(byte slot, boolean lead) {
        spawnPet(slot, lead, true);
    }

    public void spawnPet(byte slot, boolean lead, boolean broadcast) {
        final Item item = getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() >= 5010000 || item.getItemId() < 5000000) {
            return;
        }
        switch (item.getItemId()) {
            case 5000047:
            case 5000028: {
                final MaplePet pet = MaplePet.createPet(item.getItemId() + 1, MapleInventoryIdentifier.getInstance());
                if (pet != null) {
                    MapleInventoryManipulator.addById(client, item.getItemId() + 1, (short) 1, item.getOwner(), pet, 45, "Evolved from pet " + item.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                    MapleInventoryManipulator.removeFromSlot(client, MapleInventoryType.CASH, slot, (short) 1, false);
                }
                break;
            }
            default: {
                final MaplePet pet = item.getPet();
                if (pet != null && (item.getItemId() != 5000054 || pet.getSecondsLeft() > 0) && (item.getExpiration() == -1 || item.getExpiration() > System.currentTimeMillis())) {
                    if (pet.getSummoned()) { // Already summoned, let's keep it
                        unequipPet(pet, true, false);
                    } else {
                        int leadid = 8;
                        if (GameConstants.isKOC(getJob())) {
                            leadid = 10000018;
                        } else if (GameConstants.isAran(getJob())) {
                            leadid = 20000024;
                        } else if (GameConstants.isEvan(getJob())) {
                            leadid = 20011024;
                        } else if (GameConstants.isMercedes(getJob())) {
                            leadid = 20021024;
                        } else if (GameConstants.isPhantom(getJob())) {
                            leadid = 20031024;
                        } else if (GameConstants.isDemon(getJob())) {
                            leadid = 30011024;
                        } else if (GameConstants.isResist(getJob())) {
                            leadid = 30001024;
                            //} else if (GameConstants.isCannon(getJob())) {
                            //    leadid = 10008; //idk, TODO JUMP
                        }
                        if (getSkillLevel(SkillFactory.getSkill(leadid)) == 0 && getPet(0) != null) {
                            unequipPet(getPet(0), false, false);
                        } else if (lead && getSkillLevel(SkillFactory.getSkill(leadid)) > 0) { // Follow the Lead
                            //			    shiftPetsRight();
                        }
                        final Point pos = getPosition();
                        pet.setPos(pos);
                        try {
                            pet.setFh(getMap().getFootholds().findBelow(pos).getId());
                        } catch (NullPointerException e) {
                            pet.setFh(0); //lol, it can be fixed by movement
                        }
                        pet.setStance(0);
                        pet.setSummoned(1); //let summoned be true..
                        addPet(pet);
                        pet.setSummoned(getPetIndex(pet) + 1); //then get the index
                        if (broadcast && getMap() != null) {
                            getMap().broadcastMessage(this, PetPacket.showPet(this, pet, false, false), true);
                            client.getSession().write(PetPacket.showPetUpdate(this, pet.getUniqueId(), (byte) (pet.getSummonedValue() - 1)));
                            if (GameConstants.GMS) {
                                client.getSession().write(PetPacket.petStatUpdate(this));
                            }
                        }
                    }
                }
                break;
            }
        }
        client.getSession().write(CWvsContext.enableActions());
    }

    public void clearLinkMid() {
        linkMobs.clear();
        cancelEffectFromBuffStat(MapleBuffStat.HOMING_BEACON);
        cancelEffectFromBuffStat(MapleBuffStat.ARCANE_AIM);
    }

    public int getFirstLinkMid() {
        for (Integer lm : linkMobs.keySet()) {
            return lm.intValue();
        }
        return 0;
    }

    public Map<Integer, Integer> getAllLinkMid() {
        return linkMobs;
    }

    public void setLinkMid(int lm, int x) {
        linkMobs.put(lm, x);
    }

    public int getDamageIncrease(int lm) {
        if (linkMobs.containsKey(lm)) {
            return linkMobs.get(lm);
        }
        return 0;
    }

    public boolean isClone() {
        return clone;
    }

    public void setClone(boolean c) {
        this.clone = c;
    }

    public WeakReference<MapleCharacter>[] getClones() {
        return clones;
    }

    public MapleCharacter cloneLooks() {
        MapleClient cs = new MapleClient(null, null, new MockIOSession());

        final int minus = (getId() + Randomizer.nextInt(Integer.MAX_VALUE - getId())); // really randomize it, dont want it to fail

        MapleCharacter ret = new MapleCharacter(true);
        ret.id = minus;
        ret.client = cs;
        ret.exp = 0;
        ret.meso = 0;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.anticheat = anticheat;
        ret.name = name;
        ret.level = level;
        ret.fame = fame;
        ret.job = job;
        ret.hair = hair;
        ret.face = face;
        ret.demonMarking = demonMarking;
        ret.skinColor = skinColor;
        ret.monsterbook = monsterbook;
        ret.mount = mount;
        ret.CRand = new PlayerRandomStream();
        ret.gmLevel = gmLevel;
        ret.gender = gender;
        ret.mapid = map.getId();
        ret.map = map;
        ret.setStance(getStance());
        ret.chair = chair;
        ret.itemEffect = itemEffect;
        ret.guildid = guildid;
        ret.currentrep = currentrep;
        ret.totalrep = totalrep;
        ret.stats = stats;
        ret.effects.putAll(effects);
        ret.dispelSummons();
        ret.guildrank = guildrank;
        ret.guildContribution = guildContribution;
        ret.allianceRank = allianceRank;
        ret.setPosition(getTruePosition());
        for (Item equip : getInventory(MapleInventoryType.EQUIPPED).newList()) {
            ret.getInventory(MapleInventoryType.EQUIPPED).addFromDB(equip.copy());
        }
        ret.skillMacros = skillMacros;
        ret.keylayout = keylayout;
        ret.questinfo = questinfo;
        ret.savedLocations = savedLocations;
        ret.wishlist = wishlist;
        ret.buddylist = buddylist;
        ret.keydown_skill = 0;
        ret.lastmonthfameids = lastmonthfameids;
        ret.lastfametime = lastfametime;
        /*
         * START OF CUSTOM FEATURES
         */
        ret.loginTime = loginTime;
        /*
         * END OF CUSTOM FEATURES
         */
        ret.storage = storage;
        ret.cs = this.cs;
        ret.client.setAccountName(client.getAccountName());
        ret.nxcredit = nxcredit;
        ret.acash = acash;
        ret.maplepoints = maplepoints;
        ret.clone = true;
        ret.client.setChannel(this.client.getChannel());
        while (map.getCharacterById(ret.id) != null || client.getChannelServer().getPlayerStorage().getCharacterById(ret.id) != null) {
            ret.id++;
        }
        ret.client.setPlayer(ret);
        return ret;
    }

    public final void cloneLook() {
        if (clone || inPVP() || (getOccupation() / 100 != 1 && !isGM())) {
            return;
        }
        for (int i = 0; i < clones.length; i++) {
            if (clones[i].get() == null) {
                final MapleCharacter newp = cloneLooks();
                map.addPlayer(newp);
                map.broadcastMessage(CField.updateCharLook(newp));
                map.movePlayer(newp, getTruePosition());
                clones[i] = new WeakReference<MapleCharacter>(newp);
                return;
            }
        }
    }

    public final void disposeClones() {
        numClones = 0;
        for (int i = 0; i < clones.length; i++) {
            if (clones[i].get() != null) {
                map.removePlayer(clones[i].get());
                if (clones[i].get().getClient() != null) {
                    clones[i].get().getClient().setPlayer(null);
                    clones[i].get().client = null;
                }
                clones[i] = new WeakReference<MapleCharacter>(null);
                numClones++;
            }
        }
    }

    public final int getCloneSize() {
        int z = 0;
        for (int i = 0; i < clones.length; i++) {
            if (clones[i].get() != null) {
                z++;
            }
        }
        return z;
    }

    public void spawnClones() {
        if (!isGM()) { //removed tetris piece likely, expired or whatever
            numClones = (byte) (stats.hasClone ? 1 : 0);
        }
        for (int i = 0; i < numClones; i++) {
            cloneLook();
        }
        numClones = 0;
    }

    public byte getNumClones() {
        return numClones;
    }

    public void setDragon(MapleDragon d) {
        this.dragon = d;
    }

    public MapleExtractor getExtractor() {
        return extractor;
    }

    public void setExtractor(MapleExtractor me) {
        removeExtractor();
        this.extractor = me;
    }

    public void removeExtractor() {
        if (extractor != null) {
            map.broadcastMessage(CField.removeExtractor(this.id));
            map.removeMapObject(extractor);
            extractor = null;
        }
    }

    public final void spawnSavedPets() {
        for (int i = 0; i < petStore.length; i++) {
            if (petStore[i] > -1) {
                spawnPet(petStore[i], false, false);
            }
        }
        if (GameConstants.GMS) {
            client.getSession().write(PetPacket.petStatUpdate(this));
        }
        petStore = new byte[]{-1, -1, -1};
    }

    public final byte[] getPetStores() {
        return petStore;
    }

    public void resetStats(final int str, final int dex, final int int_, final int luk) {
        Map<MapleStat, Integer> stat = new EnumMap<MapleStat, Integer>(MapleStat.class);
        int total = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp();

        total -= str;
        stats.str = (int) str;

        total -= dex;
        stats.dex = (int) dex;

        total -= int_;
        stats.int_ = (int) int_;

        total -= luk;
        stats.luk = (int) luk;

        setRemainingAp((int) total);
        stats.recalcLocalStats(this);
        stat.put(MapleStat.STR, str);
        stat.put(MapleStat.DEX, dex);
        stat.put(MapleStat.INT, int_);
        stat.put(MapleStat.LUK, luk);
        stat.put(MapleStat.AVAILABLEAP, total);
        client.getSession().write(CWvsContext.updatePlayerStats(stat, false, this));
    }

    public Event_PyramidSubway getPyramidSubway() {
        return pyramidSubway;
    }

    public void setPyramidSubway(Event_PyramidSubway ps) {
        this.pyramidSubway = ps;
    }

    public byte getSubcategory() {
        if (GameConstants.isJett(job)) {
            return 10;
        }
        if (job >= 430 && job <= 434) {
            return 1;
        }
        if (GameConstants.isCannon(job) || job == 1) {
            return 2;
        }
        if (job != 0 && job != 400) {
            return 0;
        }
        return subcategory;
    }

    public void setSubcategory(int z) {
        this.subcategory = (byte) z;
    }

    public int itemQuantity(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).countById(itemid);
    }

    public void setRPS(RockPaperScissors rps) {
        this.rps = rps;
    }

    public RockPaperScissors getRPS() {
        return rps;
    }

    public long getNextConsume() {
        return nextConsume;
    }

    public void setNextConsume(long nc) {
        this.nextConsume = nc;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

   public void changeChannel(final int channel) {
        final ChannelServer toch = ChannelServer.getInstance(channel);

        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            client.getSession().write(CField.serverBlocked(1));
            return;
        }
        changeRemoval();

        final ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        PlayerBuffStorage.addBuffsToStorage(getId(), getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(getId(), getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(getId(), getAllDiseases());
        World.ChannelChange_Data(new CharacterTransfer(this), getId(), channel);
        ch.removePlayer(this);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        final String s = client.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        client.getSession().write(CField.getChannelChange(client, Integer.parseInt(toch.getIP().split(":")[1])));
        saveToDB(false, false);
        getMap().removePlayer(this);

        client.setPlayer(null);
        client.setReceiving(false);
    }

    public void expandInventory(byte type, int amount) {
        final MapleInventory inv = getInventory(MapleInventoryType.getByType(type));
        inv.addSlot((byte) amount);
        client.getSession().write(InventoryPacket.getSlotUpdate(type, (byte) inv.getSlotLimit()));
    }

    public boolean allowedToTarget(MapleCharacter other) {
        return other != null && (!other.isHidden() || getGMLevel() >= other.getGMLevel());
    }

    public int getFollowId() {
        return followid;
    }

    public void setFollowId(int fi) {
        this.followid = fi;
        if (fi == 0) {
            this.followinitiator = false;
            this.followon = false;
        }
    }

    public void setFollowInitiator(boolean fi) {
        this.followinitiator = fi;
    }

    public void setFollowOn(boolean fi) {
        this.followon = fi;
    }

    public boolean isFollowOn() {
        return followon;
    }

    public boolean isFollowInitiator() {
        return followinitiator;
    }

    public void checkFollow() {
        if (followid <= 0) {
            return;
        }
        if (followon) {
            map.broadcastMessage(CField.followEffect(id, 0, null));
            map.broadcastMessage(CField.followEffect(followid, 0, null));
        }
        MapleCharacter tt = map.getCharacterById(followid);
        client.getSession().write(CField.getFollowMessage("Follow canceled."));
        if (tt != null) {
            tt.setFollowId(0);
            tt.getClient().getSession().write(CField.getFollowMessage("Follow canceled."));
        }
        setFollowId(0);
    }

    public int getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(final int mi) {
        this.marriageId = mi;
    }
    
    public int getMarriageItemId() {
        return marriageItemId;
    }

    public void setMarriageItemId(final int mi) {
        this.marriageItemId = mi;
    }

    public boolean isStaff() {
        return this.gmLevel >= 7;
    }

    public boolean isDonator() {
        return this.gmLevel >= 2;
    }

    // TODO: gvup, vic, lose, draw, VR
    public boolean startPartyQuest(final int questid) {
        boolean ret = false;
        MapleQuest q = MapleQuest.getInstance(questid);
        if (q == null || !q.isPartyQuest()) {
            return false;
        }
        if (!quests.containsKey(q) || !questinfo.containsKey(questid)) {
            final MapleQuestStatus status = getQuestNAdd(q);
            status.setStatus((byte) 1);
            updateQuest(status);
            switch (questid) {
                case 1300:
                case 1301:
                case 1302: //carnival, ariants.
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0;gvup=0;vic=0;lose=0;draw=0");
                    break;
                case 1303: //ghost pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0;vic=0;lose=0");
                    break;
                case 1204: //herb town pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;have2=0;have3=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                case 1206: //ellin pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                default:
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
            }
            ret = true;
        } //started the quest.
        return ret;
    }

    public String getOneInfo(final int questid, final String key) {
        if (!questinfo.containsKey(questid) || key == null || MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return null;
        }
        final String[] split = questinfo.get(questid).split(";");
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length == 2 && split2[0].equals(key)) {
                return split2[1];
            }
        }
        return null;
    }

    public void updateOneInfo(final int questid, final String key, final String value) {
        if (!questinfo.containsKey(questid) || key == null || value == null || MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        final String[] split = questinfo.get(questid).split(";");
        boolean changed = false;
        final StringBuilder newQuest = new StringBuilder();
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length != 2) {
                continue;
            }
            if (split2[0].equals(key)) {
                newQuest.append(key).append("=").append(value);
            } else {
                newQuest.append(x);
            }
            newQuest.append(";");
            changed = true;
        }

        updateInfoQuest(questid, changed ? newQuest.toString().substring(0, newQuest.toString().length() - 1) : newQuest.toString());
    }
    
      public void setCData(int questid, int points) {
        final MapleQuestStatus record = client.getPlayer().getQuestNAdd(MapleQuest.getInstance(questid));

        if (record.getCustomData() != null) {
            record.setCustomData(String.valueOf(points + Integer.parseInt(record.getCustomData())));
        } else {
            record.setCustomData(String.valueOf(points)); // First time
        }
    }

      
       public short getHcMode() {
        return hcMode;
    }

    public void setHcMode(short mode) {
        this.hcMode = mode;
    }
    public int getCData(MapleCharacter sai, int questid) {
        final MapleQuestStatus record = sai.getQuestNAdd(MapleQuest.getInstance(questid));
        if (record.getCustomData() != null) {
            return Integer.parseInt(record.getCustomData());
        }
        return 0;
    }

    public void recalcPartyQuestRank(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        if (!startPartyQuest(questid)) {
            final String oldRank = getOneInfo(questid, "rank");
            if (oldRank == null || oldRank.equals("S")) {
                return;
            }
            String newRank = null;
            if (oldRank.equals("A")) {
                newRank = "S";
            } else if (oldRank.equals("B")) {
                newRank = "A";
            } else if (oldRank.equals("C")) {
                newRank = "B";
            } else if (oldRank.equals("D")) {
                newRank = "C";
            } else if (oldRank.equals("F")) {
                newRank = "D";
            } else {
                return;
            }
            final List<Pair<String, Pair<String, Integer>>> questInfo = MapleQuest.getInstance(questid).getInfoByRank(newRank);
            if (questInfo == null) {
                return;
            }
            for (Pair<String, Pair<String, Integer>> q : questInfo) {
                boolean found = false;
                final String val = getOneInfo(questid, q.right.left);
                if (val == null) {
                    return;
                }
                int vall = 0;
                try {
                    vall = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return;
                }
                if (q.left.equals("less")) {
                    found = vall < q.right.right;
                } else if (q.left.equals("more")) {
                    found = vall > q.right.right;
                } else if (q.left.equals("equal")) {
                    found = vall == q.right.right;
                }
                if (!found) {
                    return;
                }
            }
            //perfectly safe
            updateOneInfo(questid, "rank", newRank);
        }
    }

    public void tryPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            pqStartTime = System.currentTimeMillis();
            updateOneInfo(questid, "try", String.valueOf(Integer.parseInt(getOneInfo(questid, "try")) + 1));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("tryPartyQuest error");
        }
    }

    public void endPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            if (pqStartTime > 0) {
                final long changeTime = System.currentTimeMillis() - pqStartTime;
                final int mins = (int) (changeTime / 1000 / 60), secs = (int) (changeTime / 1000 % 60);
                final int mins2 = Integer.parseInt(getOneInfo(questid, "min"));
                if (mins2 <= 0 || mins < mins2) {
                    updateOneInfo(questid, "min", String.valueOf(mins));
                    updateOneInfo(questid, "sec", String.valueOf(secs));
                    updateOneInfo(questid, "date", FileoutputUtil.CurrentReadable_Date());
                }
                final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp")) + 1;
                updateOneInfo(questid, "cmp", String.valueOf(newCmp));
                updateOneInfo(questid, "CR", String.valueOf((int) Math.ceil((newCmp * 100.0) / Integer.parseInt(getOneInfo(questid, "try")))));
                recalcPartyQuestRank(questid);
                pqStartTime = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("endPartyQuest error");
        }

    }

    public void havePartyQuest(final int itemId) {
        int questid = 0, index = -1;
        switch (itemId) {
            case 1002798:
                questid = 1200; //henesys
                break;
            case 1072369:
                questid = 1201; //kerning
                break;
            case 1022073:
                questid = 1202; //ludi
                break;
            case 1082232:
                questid = 1203; //orbis
                break;
            case 1002571:
            case 1002572:
            case 1002573:
            case 1002574:
                questid = 1204; //herbtown
                index = itemId - 1002571;
                break;
            case 1102226:
                questid = 1303; //ghost
                break;
            case 1102227:
                questid = 1303; //ghost
                index = 0;
                break;
            case 1122010:
                questid = 1205; //magatia
                break;
            case 1032061:
            case 1032060:
                questid = 1206; //ellin
                index = itemId - 1032060;
                break;
            case 3010018:
                questid = 1300; //ariant
                break;
            case 1122007:
                questid = 1301; //carnival
                break;
            case 1122058:
                questid = 1302; //carnival2
                break;
            default:
                return;
        }
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        startPartyQuest(questid);
        updateOneInfo(questid, "have" + (index == -1 ? "" : index), "1");
    }

    public void resetStatsByJob(boolean beginnerJob) {
        int baseJob = (beginnerJob ? (job % 1000) : (((job % 1000) / 100) * 100)); //1112 -> 112 -> 1 -> 100
        boolean UA = getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER)) != null;
        if (baseJob == 100) { //first job = warrior
            resetStats(UA ? 4 : 35, 4, 4, 4);
        } else if (baseJob == 200) {
            resetStats(4, 4, UA ? 4 : 20, 4);
        } else if (baseJob == 300 || baseJob == 400) {
            resetStats(4, UA ? 4 : 25, 4, 4);
        } else if (baseJob == 500) {
            resetStats(4, UA ? 4 : 20, 4, 4);
        } else if (baseJob == 0) {
            resetStats(4, 4, 4, 4);
        }
    }

    public boolean hasSummon() {
        return hasSummon;
    }

    public void setHasSummon(boolean summ) {
        this.hasSummon = summ;
    }

    public void removeDoor() {
        final MapleDoor door = getDoors().iterator().next();
        for (final MapleCharacter chr : door.getTarget().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleCharacter chr : door.getTown().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleDoor destroyDoor : getDoors()) {
            door.getTarget().removeMapObject(destroyDoor);
            door.getTown().removeMapObject(destroyDoor);
        }
        clearDoors();
    }

    public void removeMechDoor() {
        for (final MechDoor destroyDoor : getMechDoors()) {
            for (final MapleCharacter chr : getMap().getCharactersThreadsafe()) {
                destroyDoor.sendDestroyData(chr.getClient());
            }
            getMap().removeMapObject(destroyDoor);
        }
        clearMechDoors();
    }

    public void changeRemoval() {
        changeRemoval(false);
    }

    public void changeRemoval(boolean dc) {
        if (getCheatTracker() != null && dc) {
            getCheatTracker().dispose();
        }
        removeFamiliar();
        dispelSummons();
        if (!dc) {
            cancelEffectFromBuffStat(MapleBuffStat.SOARING);
            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
            cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
        }
        if (getPyramidSubway() != null) {
            getPyramidSubway().dispose(this);
        }
        if (playerShop != null && !dc) {
            playerShop.removeVisitor(this);
            if (playerShop.isOwner(this)) {
                playerShop.setOpen(true);
            }
        }
        if (!getDoors().isEmpty()) {
            removeDoor();
        }
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        disposeClones();
        NPCScriptManager.getInstance().dispose(client);
        cancelFairySchedule(false);
    }

    public void updateTick(int newTick) {
        anticheat.updateTick(newTick);
    }

    public boolean canUseFamilyBuff(MapleFamilyBuff buff) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(buff.questID));
        if (stat == null) {
            return true;
        }
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Long.parseLong(stat.getCustomData()) + (24 * 3600000) < System.currentTimeMillis();
    }

    public void useFamilyBuff(MapleFamilyBuff buff) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(buff.questID));
        stat.setCustomData(String.valueOf(System.currentTimeMillis()));
    }

    public List<Integer> usedBuffs() {
        //assume count = 1
        List<Integer> used = new ArrayList<Integer>();
        MapleFamilyBuff[] z = MapleFamilyBuff.values();
        for (int i = 0; i < z.length; i++) {
            if (!canUseFamilyBuff(z[i])) {
                used.add(i);
            }
        }
        return used;
    }

    public String getTeleportName() {
        return teleportname;
    }

    public void setTeleportName(final String tname) {
        teleportname = tname;
    }

    public int getNoJuniors() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getNoJuniors();
    }

    public MapleFamilyCharacter getMFC() {
        return mfc;
    }

    public void makeMFC(final int familyid, final int seniorid, final int junior1, final int junior2) {
        if (familyid > 0) {
            MapleFamily f = World.Family.getFamily(familyid);
            if (f == null) {
                mfc = null;
            } else {
                mfc = f.getMFC(id);
                if (mfc == null) {
                    mfc = f.addFamilyMemberInfo(this, seniorid, junior1, junior2);
                }
                if (mfc.getSeniorId() != seniorid) {
                    mfc.setSeniorId(seniorid);
                }
                if (mfc.getJunior1() != junior1) {
                    mfc.setJunior1(junior1);
                }
                if (mfc.getJunior2() != junior2) {
                    mfc.setJunior2(junior2);
                }
            }
        } else {
            mfc = null;
        }
    }

    public void setFamily(final int newf, final int news, final int newj1, final int newj2) {
        if (mfc == null || newf != mfc.getFamilyId() || news != mfc.getSeniorId() || newj1 != mfc.getJunior1() || newj2 != mfc.getJunior2()) {
            makeMFC(newf, news, newj1, newj2);
        }
    }

    public int maxBattleshipHP(int skillid) {
        return (getTotalSkillLevel(skillid) * 5000) + ((getLevel() - 120) * 3000);
    }

    public int currentBattleshipHP() {
        return battleshipHP;
    }

    public void setBattleshipHP(int v) {
        this.battleshipHP = v;
    }

    public void decreaseBattleshipHP() {
        this.battleshipHP--;
    }

    public int getGachExp() {
        return gachexp;
    }

    public void setGachExp(int ge) {
        this.gachexp = ge;
    }

    public boolean isInBlockedMap() {
        if (!isAlive() || getPyramidSubway() != null || getMap().getSquadByMap() != null || getEventInstance() != null || getMap().getEMByMap() != null) {
            return true;
        }
        if ((getMapId() >= 680000210 && getMapId() <= 680000502) || (getMapId() / 10000 == 92502 && getMapId() >= 925020100) || (getMapId() / 10000 == 92503) || getMapId() == GameConstants.JAIL) {
            return true;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return true;
            }
        }
        return false;
    }

    public boolean isInTownMap() {
        if (hasBlockedInventory() || !getMap().isTown() || FieldLimitType.VipRock.check(getMap().getFieldLimit()) || getEventInstance() != null) {
            return false;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return false;
            }
        }
        return true;
    }
    
     public boolean isEventMap() {
        for (int i : GameConstants.EventMaps) {
            if (getMapId() == i) {
                return false;
            }
        }
        return true;
    }
     
      public void setKeepBuffs(boolean yesno) {
        keepBuffs = yesno;
    }
      
        public boolean inAzwan() {
        return mapid / 1000000 == 262;
    }

    public boolean getKeepBuffs() {
        return keepBuffs;
    }

    public boolean hasBlockedInventory() {
        return !isAlive() || getTrade() != null || getConversation() > 0 || getDirection() >= 0 || getPlayerShop() != null || getBattle() != null || map == null;
    }

    public void startPartySearch(final List<Integer> jobs, final int maxLevel, final int minLevel, final int membersNeeded) {
        for (MapleCharacter chr : map.getCharacters()) {
            if (chr.getId() != id && chr.getParty() == null && chr.getLevel() >= minLevel && chr.getLevel() <= maxLevel && (jobs.isEmpty() || jobs.contains(Integer.valueOf(chr.getJob()))) && (isGM() || !chr.isGM())) {
                if (party != null && party.getMembers().size() < 6 && party.getMembers().size() < membersNeeded) {
                    chr.setParty(party);
                    World.Party.updateParty(party.getId(), PartyOperation.JOIN, new MaplePartyCharacter(chr));
                    chr.receivePartyMemberHP();
                    chr.updatePartyMemberHP();
                } else {
                    break;
                }
            }
        }
    }

    public Battler getBattler(int pos) {
        return battlers[pos];
    }

    public Battler[] getBattlers() {
        return battlers;
    }

    public List<Battler> getBoxed() {
        return boxed;
    }

    public PokemonBattle getBattle() {
        return battle;
    }

    public void setBattle(PokemonBattle b) {
        this.battle = b;
    }

    public int countBattlers() {
        int ret = 0;
        for (int i = 0; i < battlers.length; i++) {
            if (battlers[i] != null) {
                ret++;
            }
        }
        return ret;
    }

    public void changedBattler() {
        changed_pokemon = true;
    }

    public void makeBattler(int index, int monsterId) {
        final MapleMonsterStats mons = MapleLifeFactory.getMonsterStats(monsterId);
        this.battlers[index] = new Battler(mons);
        this.battlers[index].setCharacterId(id);
        changed_pokemon = true;
        getMonsterBook().monsterCaught(client, monsterId, mons.getName());
    }

    public boolean removeBattler(int ind) {
        if (countBattlers() <= 1) {
            return false;
        }
        if (ind == battlers.length) {
            this.battlers[ind] = null;
        } else {
            for (int i = ind; i < battlers.length; i++) {
                this.battlers[i] = ((i + 1) == battlers.length ? null : this.battlers[i + 1]);
            }
        }
        changed_pokemon = true;
        return true;
    }

    public int getChallenge() {
        return challenge;
    }

    public void setChallenge(int c) {
        this.challenge = c;
    }

    public short getFatigue() {
        return fatigue;
    }

    public void setFatigue(int j) {
        this.fatigue = (short) Math.max(0, j);
        updateSingleStat(MapleStat.FATIGUE, this.fatigue);
    }
    
    public int getReborns() {
        return reborns;
    }
    
    public int getParagons() {
        return Paragons;
    }

    public void fakeRelog() {
        client.getSession().write(CField.getCharInfo(this));
        final MapleMap mapp = getMap();
        mapp.setCheckStates(false);
        mapp.removePlayer(this);
        mapp.addPlayer(this);
        mapp.setCheckStates(true);

        client.getSession().write(CWvsContext.getFamiliarInfo(this));
    }

    public boolean canSummon() {
        return canSummon(5000);
    }

    public boolean canSummon(int g) {
        if (lastSummonTime + g < System.currentTimeMillis()) {
            lastSummonTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public int getIntNoRecord(int questID) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(questID));
        if (stat == null || stat.getCustomData() == null) {
            return 0;
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public int getIntRecord(int questID) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public void updatePetAuto() {
        if (getIntNoRecord(GameConstants.HP_ITEM) > 0) {
            client.getSession().write(CField.petAutoHP(getIntRecord(GameConstants.HP_ITEM)));
        }
        if (getIntNoRecord(GameConstants.MP_ITEM) > 0) {
            client.getSession().write(CField.petAutoMP(getIntRecord(GameConstants.MP_ITEM)));
        }
    }

    public void sendEnglishQuiz(String msg) {
        //client.getSession().write(CField.englishQuizMsg(msg));
    }

    public void setChangeTime() {
        mapChangeTime = System.currentTimeMillis();
    }

    public long getChangeTime() {
        return mapChangeTime;
    }

    public Map<ReportType, Integer> getReports() {
        return reports;
    }

    public void addReport(ReportType type) {
        Integer value = reports.get(type);
        reports.put(type, value == null ? 1 : (value + 1));
        changed_reports = true;
    }

    public void clearReports(ReportType type) {
        reports.remove(type);
        changed_reports = true;
    }

    public void clearReports() {
        reports.clear();
        changed_reports = true;
    }

    public final int getReportPoints() {
        int ret = 0;
        for (Integer entry : reports.values()) {
            ret += entry;
        }
        return ret;
    }

    public final String getReportSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<Pair<ReportType, Integer>> offenseList = new ArrayList<Pair<ReportType, Integer>>();
        for (final Entry<ReportType, Integer> entry : reports.entrySet()) {
            offenseList.add(new Pair<ReportType, Integer>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(offenseList, new Comparator<Pair<ReportType, Integer>>() {

            @Override
            public final int compare(final Pair<ReportType, Integer> o1, final Pair<ReportType, Integer> o2) {
                final int thisVal = o1.getRight();
                final int anotherVal = o2.getRight();
                return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
            }
        });
        for (int x = 0; x < offenseList.size(); x++) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).left.name()));
            ret.append(": ");
            ret.append(offenseList.get(x).right);
            ret.append(" ");
        }
        return ret.toString();
    }

    public short getScrolledPosition() {
        return scrolledPosition;
    }

    public void setScrolledPosition(short s) {
        this.scrolledPosition = s;
    }

    public MapleTrait getTrait(MapleTraitType t) {
        return traits.get(t);
    }

    public void forceCompleteQuest(int id) {
        MapleQuest.getInstance(id).forceComplete(this, 9270035); //troll
    }

    public List<Integer> getExtendedSlots() {
        return extendedSlots;
    }

    public int getExtendedSlot(int index) {
        if (extendedSlots.size() <= index || index < 0) {
            return -1;
        }
        return extendedSlots.get(index);
    }

    public void changedExtended() {
        changed_extendedSlots = true;
    }

    public MapleAndroid getAndroid() {
        return android;
    }

    public void removeAndroid() {
        if (map != null) {
            map.broadcastMessage(CField.deactivateAndroid(this.id));
        }
        android = null;
    }

    public void setAndroid(MapleAndroid a) {
        this.android = a;
        if (map != null && a != null) {
            map.broadcastMessage(CField.spawnAndroid(this, a));
            
        }
    }

    public List<Item> getRebuy() {
        return rebuy;
    }

    public Map<Integer, MonsterFamiliar> getFamiliars() {
        return familiars;
    }

    public MonsterFamiliar getSummonedFamiliar() {
        return summonedFamiliar;
    }

    public void removeFamiliar() {
        if (summonedFamiliar != null && map != null) {
            removeVisibleFamiliar();
        }
        summonedFamiliar = null;
    }

    public void removeVisibleFamiliar() {
        getMap().removeMapObject(summonedFamiliar);
        removeVisibleMapObject(summonedFamiliar);
        getMap().broadcastMessage(CField.removeFamiliar(this.getId()));
        anticheat.resetFamiliarAttack();
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        cancelEffect(ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).passive), false, System.currentTimeMillis());
    }

    public void spawnFamiliar(MonsterFamiliar mf, boolean respawn) {
        summonedFamiliar = mf;

        mf.setStance(0);
        mf.setPosition(getPosition());
        mf.setFh(getFH());
        addVisibleMapObject(mf);
        getMap().spawnFamiliar(mf, respawn);

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleStatEffect eff = ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).passive);
        if (eff != null && eff.getInterval() <= 0 && eff.makeChanceResult()) { //i think this is actually done through a recv, which is ATTACK_FAMILIAR +1
            eff.applyTo(this);
        }
        lastFamiliarEffectTime = System.currentTimeMillis();
    }

    public final boolean canFamiliarEffect(long now, MapleStatEffect eff) {
        return lastFamiliarEffectTime > 0 && lastFamiliarEffectTime + eff.getInterval() < now;
    }

    public void doFamiliarSchedule(long now) {
        if (familiars == null) {
            return;
        }
        for (MonsterFamiliar mf : familiars.values()) {
            if (summonedFamiliar != null && summonedFamiliar.getId() == mf.getId()) {
                mf.addFatigue(this, 5);
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final MapleStatEffect eff = ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).passive);
                if (eff != null && eff.getInterval() > 0 && canFamiliarEffect(now, eff) && eff.makeChanceResult()) {
                    eff.applyTo(this);
                }
            } else if (mf.getFatigue() > 0) {
                mf.setFatigue(Math.max(0, mf.getFatigue() - 5));
            }
        }
    }

    public MapleImp[] getImps() {
        return imps;
    }

    public void sendImp() {
        for (int i = 0; i < imps.length; i++) {
            if (imps[i] != null) {
                client.getSession().write(CWvsContext.updateImp(imps[i], ImpFlag.SUMMONED.getValue(), i, true));
            }
        }
    }

    public int getBattlePoints() {
        return pvpPoints;
    }

    public int getTotalBattleExp() {
        return pvpExp;
    }

    public void setBattlePoints(int p) {
        if (p != pvpPoints) {
            client.getSession().write(InfoPacket.getBPMsg(p - pvpPoints));
            updateSingleStat(MapleStat.BATTLE_POINTS, p);
        }
        this.pvpPoints = p;
    }

    public void setTotalBattleExp(int p) {
        final int previous = pvpExp;
        this.pvpExp = p;
        if (p != previous) {
            stats.recalcPVPRank(this);

            updateSingleStat(MapleStat.BATTLE_EXP, stats.pvpExp);
            updateSingleStat(MapleStat.BATTLE_RANK, stats.pvpRank);
        }
    }

    public void changeTeam(int newTeam) {
        this.coconutteam = newTeam;

        if (inPVP()) {
            client.getSession().write(CField.getPVPTransform(newTeam + 1));
            map.broadcastMessage(CField.changeTeam(id, newTeam + 1));
        } else {
            client.getSession().write(CField.showEquipEffect(newTeam));
        }
    }

    public void disease(int type, int level) {
        if (MapleDisease.getBySkill(type) == null) {
            return;
        }
        chair = 0;
        client.getSession().write(CField.cancelChair(-1));
        map.broadcastMessage(this, CField.showChair(id, 0), false);
        giveDebuff(MapleDisease.getBySkill(type), MobSkillFactory.getMobSkill(type, level));
    }

    public boolean inPVP() {
        return eventInstance != null && eventInstance.getName().startsWith("PVP");
    }

    public void clearAllCooldowns() {
        for (MapleCoolDownValueHolder m : getCooldowns()) {
            final int skil = m.skillId;
            removeCooldown(skil);
            client.getSession().write(CField.skillCooldown(skil, 0));
        }
    }

    public Pair<Double, Boolean> modifyDamageTaken(double damage, MapleMapObject attacke) {
        Pair<Double, Boolean> ret = new Pair<Double, Boolean>(damage, false);
        if (damage <= 0) {
            return ret;
        }
        if (stats.ignoreDAMr > 0 && Randomizer.nextInt(100) < stats.ignoreDAMr_rate) {
            damage -= Math.floor((stats.ignoreDAMr * damage) / 100.0f);
        }
        if (stats.ignoreDAM > 0 && Randomizer.nextInt(100) < stats.ignoreDAM_rate) {
            damage -= stats.ignoreDAM;
        }
        final Integer div = getBuffedValue(MapleBuffStat.DIVINE_SHIELD);
        final Integer div2 = getBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL);
        if (div2 != null) {
            if (div2 <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.HOLY_MAGIC_SHELL);
            } else {
                setBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL, div2 - 1);
                damage = 0;
            }
        } else if (div != null) {
            if (div <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.DIVINE_SHIELD);
            } else {
                setBuffedValue(MapleBuffStat.DIVINE_SHIELD, div - 1);
                damage = 0;
            }
        }
        MapleStatEffect barrier = getStatForBuff(MapleBuffStat.COMBO_BARRIER);
        if (barrier != null) {
            damage = ((barrier.getX() / 1000.0) * damage);
        }
        barrier = getStatForBuff(MapleBuffStat.MAGIC_SHIELD);
        if (barrier != null) {
            damage = ((barrier.getX() / 1000.0) * damage);
        }
        barrier = getStatForBuff(MapleBuffStat.WATER_SHIELD);
        if (barrier != null) {
            damage = ((barrier.getX() / 1000.0) * damage);
        }
        List<Integer> attack = attacke instanceof MapleMonster || attacke == null ? null : (new ArrayList<Integer>());
        if (damage > 0) {
            if (getJob() == 122 && !skillisCooling(1220013)) {
                final Skill divine = SkillFactory.getSkill(1220013);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(CField.skillCooldown(1220013, divineShield.getCooldown(this)));
                        addCooldown(1220013, System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
            } else if (getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC) != null && getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB) != null && getBuffedValue(MapleBuffStat.PUPPET) != null) {
                double buff = getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC).doubleValue();
                double buffz = getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB).doubleValue();
                if ((int) ((buff / 100.0) * getStat().getMaxHp()) <= damage) {
                    damage -= ((buffz / 100.0) * damage);
                    cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                }
            } else if (getJob() == 433 || getJob() == 434) {
                final Skill divine = SkillFactory.getSkill(4330001);
                if (getTotalSkillLevel(divine) > 0 && getBuffedValue(MapleBuffStat.DARKSIGHT) == null && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (Randomizer.nextInt(100) < divineShield.getX()) {
                        divineShield.applyTo(this);
                    }
                }
            } else if ((getJob() == 512 || getJob() == 522) && getBuffedValue(MapleBuffStat.PIRATES_REVENGE) == null) {
                final Skill divine = SkillFactory.getSkill(getJob() == 512 ? 5120011 : 5220012);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(CField.skillCooldown(divine.getId(), divineShield.getCooldown(this)));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
            } else if (getJob() == 312 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(3120010);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final Rectangle bounds = divineShield.calculateBoundingBox(getTruePosition(), isFacingLeft());
                            final List<MapleMapObject> affected = getMap().getMapObjectsInRect(bounds, Arrays.asList(attacke.getType()));
                            int i = 0;

                            for (final MapleMapObject mo : affected) {
                                MapleMonster mons = (MapleMonster) mo;
                                if (mons.getStats().isFriendly() || mons.isFake()) {
                                    continue;
                                }
                                mons.applyStatus(this, new MonsterStatusEffect(MonsterStatus.STUN, 1, divineShield.getSourceId(), null, false), false, divineShield.getDuration(), true, divineShield);
                                final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                                mons.damage(this, theDmg, true);
                                getMap().broadcastMessage(MobPacket.damageMonster(mons.getObjectId(), theDmg));
                                i++;
                                if (i >= divineShield.getMobCount()) {
                                    break;
                                }
                            }
                        } else {
                            MapleCharacter chr = (MapleCharacter) attacke;
                            chr.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            } else if ((getJob() == 531 || getJob() == 532) && attacke != null) {
                final Skill divine = SkillFactory.getSkill(5310009); //slea.readInt() = 5310009, then slea.readInt() = damage. (175000)
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            } else if (getJob() == 132 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(1320011);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId()) && getBuffSource(MapleBuffStat.BEHOLDER) == 1321007) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        client.getSession().write(CField.skillCooldown(divine.getId(), divineShield.getCooldown(this)));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            }
            if (attacke != null) {
                final int damr = (Randomizer.nextInt(100) < getStat().DAMreflect_rate ? getStat().DAMreflect : 0) + (getBuffedValue(MapleBuffStat.POWERGUARD) != null ? getBuffedValue(MapleBuffStat.POWERGUARD) : 0);
                final int bouncedam_ = damr + (getBuffedValue(MapleBuffStat.PERFECT_ARMOR) != null ? getBuffedValue(MapleBuffStat.PERFECT_ARMOR) : 0);
                if (bouncedam_ > 0) {
                    long bouncedamage = (long) (damage * bouncedam_ / 100);
                    long bouncer = (long) (damage * damr / 100);
                    damage -= bouncer;
                    if (attacke instanceof MapleMonster) {
                        final MapleMonster attacker = (MapleMonster) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);
                        attacker.damage(this, bouncedamage, true);
                        getMap().broadcastMessage(this, MobPacket.damageMonster(attacker.getObjectId(), bouncedamage), getTruePosition());
                        if (getBuffSource(MapleBuffStat.PERFECT_ARMOR) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.PERFECT_ARMOR);
                            if (eff.makeChanceResult()) {
                                attacker.applyStatus(this, new MonsterStatusEffect(MonsterStatus.STUN, 1, eff.getSourceId(), null, false), false, eff.getSubTime(), true, eff);
                            }
                        }
                    } else {
                        final MapleCharacter attacker = (MapleCharacter) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getStat().getCurrentMaxHp() / 10);
                        attacker.addHP(-((int) bouncedamage));
                        attack.add((int) bouncedamage);
                        if (getBuffSource(MapleBuffStat.PERFECT_ARMOR) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.PERFECT_ARMOR);
                            if (eff.makeChanceResult()) {
                                attacker.disease(MapleDisease.STUN.getDisease(), 1);
                            }
                        }
                    }
                    ret.right = true;
                }
                if ((getJob() == 411 || getJob() == 412 || getJob() == 421 || getJob() == 422) && getBuffedValue(MapleBuffStat.SUMMON) != null && attacke != null) {
                    final List<MapleSummon> ss = getSummonsReadLock();
                    try {
                        for (MapleSummon sum : ss) {
                            if (sum.getTruePosition().distanceSq(getTruePosition()) < 400000.0 && (sum.getSkill() == 4111007 || sum.getSkill() == 4211007)) {
                                final List<Pair<Integer, Integer>> allDamage = new ArrayList<Pair<Integer, Integer>>();
                                if (attacke instanceof MapleMonster) {
                                    final MapleMonster attacker = (MapleMonster) attacke;
                                    final int theDmg = (int) (SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX() * damage / 100.0);
                                    allDamage.add(new Pair<Integer, Integer>(attacker.getObjectId(), theDmg));
                                    getMap().broadcastMessage(SummonPacket.summonAttack(sum.getOwnerId(), sum.getObjectId(), (byte) 0x84, allDamage, getLevel(), true));
                                    attacker.damage(this, theDmg, true);
                                    checkMonsterAggro(attacker);
                                    if (!attacker.isAlive()) {
                                        getClient().getSession().write(MobPacket.killMonster(attacker.getObjectId(), 1));
                                    }
                                } else {
                                    final MapleCharacter chr = (MapleCharacter) attacke;
                                    final int dmg = SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX();
                                    chr.addHP(-dmg);
                                    attack.add(dmg);
                                }
                            }
                        }
                    } finally {
                        unlockSummonsReadLock();
                    }
                }
            }
        }
        if (attack != null && attack.size() > 0 && attacke != null) {
            getMap().broadcastMessage(CField.pvpCool(attacke.getObjectId(), attack));
        }
        ret.left = damage;
        return ret;
    }

  
      public void onAttack(long maxhp, int maxmp, int skillid, int oid, int totDamage, int critCount) {
        if (stats.hpRecoverProp > 0) {
            if (Randomizer.nextInt(100) <= stats.hpRecoverProp) {//i think its out of 100, anyway
                if (stats.hpRecover > 0) {
                    healHP(stats.hpRecover);
                }
                if (stats.hpRecoverPercent > 0) {
                    addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) stats.hpRecoverPercent / 100.0)), stats.getMaxHp() / 2))));
                }
            }
        }
        if (stats.mpRecoverProp > 0 && !GameConstants.isDemon(getJob())) {
            if (Randomizer.nextInt(100) <= stats.mpRecoverProp) {//i think its out of 100, anyway
                healMP(stats.mpRecover);
            }
        }
        if (getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
            addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) getStatForBuff(MapleBuffStat.COMBO_DRAIN).getX() / 100.0)), stats.getMaxHp() / 2))));
        }
        if (getBuffSource(MapleBuffStat.COMBO_DRAIN) == 23101003) {
            addMP(((int) Math.min(maxmp, Math.min(((int) ((double) totDamage * (double) getStatForBuff(MapleBuffStat.COMBO_DRAIN).getX() / 100.0)), stats.getMaxMp() / 2))));
        }
        if (getBuffedValue(MapleBuffStat.REAPER) != null && getBuffedValue(MapleBuffStat.SUMMON) == null && getSummonsSize() < 4 && canSummon()) {
            final MapleStatEffect eff = getStatForBuff(MapleBuffStat.REAPER);
            if (eff.makeChanceResult()) {
                eff.applyTo(this, this, false, null, eff.getDuration());
            }
        }
        if (getJob() == 212 || getJob() == 222 || getJob() == 232) {
            int[] skills = {2120010, 2220010, 2320011};
            for (int i : skills) {
                final Skill skill = SkillFactory.getSkill(i);
                if (getTotalSkillLevel(skill) > 0) {
                    final MapleStatEffect venomEffect = skill.getEffect(getTotalSkillLevel(skill));
                    if (venomEffect.makeChanceResult() && getAllLinkMid().size() < venomEffect.getY()) {
                        setLinkMid(oid, venomEffect.getX());
                        venomEffect.applyTo(this);
                    }
                    break;
                }
            }
        }
        int[] skills = {4110011, 4120005, 4210010, 4220005, 4320005, 4340001, 14110004};
        for (int i : skills) {
            if (i == 4110011) {
                if (getTotalSkillLevel(4120011) > 0) {
                    i = 4120011;
                }
            } else if (i == 4210010) {
                if (getTotalSkillLevel(4220011) > 0) {
                    i = 4220011;
                }
            }
            final Skill skill = SkillFactory.getSkill(i);
            if (getTotalSkillLevel(skill) > 0) {
                final MapleStatEffect venomEffect = skill.getEffect(getTotalSkillLevel(skill));
                final MapleMonster monster = map.getMonsterByOid(oid);
                if (venomEffect.makeChanceResult() && monster != null) {
                    monster.applyStatus(this, new MonsterStatusEffect(MonsterStatus.POISON, 1, i, null, false), true, venomEffect.getDuration(), true, venomEffect);
                }
                break;
            }
        }
        if (getJob() == 2410 || getJob() == 2411 || getJob() == 2412) {
            final Skill skil = SkillFactory.getSkill(getJob() == 2412 ? 24120002 : 24100003);
            if (getTotalSkillLevel(skil) > 0 && critCount > 0 && skillid != 24120002 && skillid != 24100003) {
                final MapleStatEffect eff = skil.getEffect(getTotalSkillLevel(skil));
                if (eff.makeChanceResult()) {
                    setBattleshipHP(Math.min(getJob() == 2412 ? 40 : 20, currentBattleshipHP() + 1));
                    attackCarte(eff, oid, 1);
                }
            }
        }
        // effects
        if (skillid > 0) {
            final Skill skil = SkillFactory.getSkill(skillid);
            final MapleStatEffect effect = skil.getEffect(getTotalSkillLevel(skil));
            switch (skillid) {
                case 15111001:
                case 3111008:
                case 1078:
                case 31111003:
                case 11078:
                case 14101006:
                case 33111006: //swipe
                case 4101005: //drain
                case 5111004: { // Energy Drain
                    addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)), stats.getMaxHp() / 2))));
                    break;
                }
                case 5211006:
                case 22151002: //killer wing
                case 5220011: {//homing
                    setLinkMid(oid, effect.getX());
                    break;
                }
                case 33101007: { //jaguar
                    clearLinkMid();
                    break;
                }
            }
        }
    }

    public void attackCarte(final MapleStatEffect eff, final int oid, final int x) {
        if (x > 0) {
            lastBerserkTime += x; //lol unused variable.
            map.broadcastMessage(CField.getCarteAnimation(id, oid, job, (int) lastBerserkTime, x));
            client.getSession().write(CField.updateCardStack(currentBattleshipHP()));
        }
    }
  public void handleForceGain(int oid, int skillid) {
        handleForceGain(oid, skillid, 0);
    }

    public void handleForceGain(int oid, int skillid, int extraForce) {
        /*if (!GameConstants.isForceIncrease(skillid) && extraForce <= 0) {
         return;
         }*/
        int forceGain = 1;
        if (getLevel() >= 30 && getLevel() < 70) {
            forceGain = 2;
        } else if (getLevel() >= 70 && getLevel() < 120) {
            forceGain = 3;
        } else if (getLevel() >= 120) {
            forceGain = 4;
        }
        force++; // counter
        if (GameConstants.isDemon(getJob())) {
            addMP(extraForce > 0 ? extraForce : forceGain, true);
        }
        getClient().getSession().write(CField.gainForce(oid, force, forceGain));
        if (GameConstants.isDemon(getJob())) {
            if (stats.mpRecoverProp > 0 && extraForce <= 0) {
                if (Randomizer.nextInt(100) <= stats.mpRecoverProp) {//i think its out of 100, anyway
                    force++; // counter
                    addMP(stats.mpRecover, true);
                    getClient().getSession().write(CField.gainForce(oid, force, stats.mpRecover));
                }
            }
        }
    }

    public void gainForce(int gain) {
        if (GameConstants.isDemon(getJob())) {
            addMP(gain);
            getClient().getSession().write(CField.gainForce(0, 1, gain));
        }
    }

    public void afterAttack(int mobCount, int attackCount, int skillid) {
        switch (getJob()) {
            case 511:
            case 512: {
                handleEnergyCharge(5110001, mobCount * attackCount);
                break;
            }
            case 1510:
            case 1511:
            case 1512: {
                handleEnergyCharge(15100004, mobCount * attackCount);
                break;
            }
            case 422:
                if (skillid == 4221001) {
                    setBattleshipHP(0);
                } else {
                    setBattleshipHP(Math.min(5, currentBattleshipHP() + 1)); //max 5
                }
                refreshBattleshipHP();
                break;
            case 111:
            case 112:
            case 1111:
            case 1112:
            case 2411:
            case 2412:
                if (skillid != 1111008 & getBuffedValue(MapleBuffStat.COMBO) != null) { // shout should not give orbs
                    handleOrbgain();
                }
                break;
        }
        if (getBuffedValue(MapleBuffStat.OWL_SPIRIT) != null) {
            if (currentBattleshipHP() > 0) {
                decreaseBattleshipHP();
            }
            if (currentBattleshipHP() <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.OWL_SPIRIT);
            }
        }
        cancelEffectFromBuffStat(MapleBuffStat.WIND_WALK);
        cancelEffectFromBuffStat(MapleBuffStat.INFILTRATE);
        final MapleStatEffect ds = getStatForBuff(MapleBuffStat.DARKSIGHT);
        if (ds != null) {
            if (!ds.makeChanceResult()) {
                cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
            }
        }
    }

    public void applyIceGage(int x) {
        updateSingleStat(MapleStat.ICE_GAGE, x);
    }

    public Rectangle getBounds() {
        return new Rectangle(getTruePosition().x - 25, getTruePosition().y - 75, 50, 75);
    }

    public final Map<Byte, Integer> getEquips() {
        final Map<Byte, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            eq.put((byte) item.getPosition(), item.getItemId());
        }
        return eq;
    }
    private transient PlayerRandomStream CRand;

    public final PlayerRandomStream CRand() {
        return CRand;
    }

    public void handleCardStack() {
/* 8260 */     Skill noir = SkillFactory.getSkill(24120002);
/* 8261 */     Skill blanc = SkillFactory.getSkill(24100003);
/* 8262 */     MapleStatEffect ceffect = null;
/* 8263 */     int advSkillLevel = getTotalSkillLevel(noir);
/* 8264 */     boolean isAdv = false;
/* 8265 */     if (advSkillLevel > 0) {
/* 8266 */       ceffect = noir.getEffect(advSkillLevel);
/* 8267 */       isAdv = true;
/* 8268 */     } else if (getSkillLevel(blanc) > 0) {
/* 8269 */       ceffect = blanc.getEffect(getTotalSkillLevel(blanc));
/*      */     } else {
/* 8271 */       return;
/*      */     }
                if (getJob() == 2412 && getCardStack() == 40){  
                    return;
                }
                if (getJob() == 2400 && getCardStack() == 20 || getJob() == 2410 && getCardStack() == 20 || getJob() == 2411 && getCardStack() == 20){  
                    return;
                }
/* 8273 */     if (ceffect.makeChanceResult()) {
/* 8274 */       if (this.cardStack < (getJob() == 2412 ? 40 : 20)) {
/* 8275 */         this.cardStack = (byte)(this.cardStack + 1);
/*      */       }
/* 8277 */       this.runningStack += 1;
/* 8278 */       this.client.getSession().write(CField.gainCardStack(getId(), this.runningStack, isAdv ? 2 : 1, ceffect.getSourceId(), Randomizer.rand(100000, 500000), 1));
/* 8279 */       this.client.getSession().write(CField.updateCardStack(this.cardStack));
/*      */     }
/*      */   }
/*      */ 
    
/* This should work once we fix the issue that happens after a long while of using the skill
  public void handleCardStack() {
    Skill noir = SkillFactory.getSkill(24120002);
    Skill blanc = SkillFactory.getSkill(24100003);
    MapleStatEffect ceffect = null;
    int advSkillLevel = getTotalSkillLevel(noir);
    boolean isAdv = false;
    if (advSkillLevel > 0) {
      ceffect = noir.getEffect(advSkillLevel);
      isAdv = true;
    } else if (getSkillLevel(blanc) > 0) {
      ceffect = blanc.getEffect(getTotalSkillLevel(blanc));
    } else {
      return;
    }
    if (ceffect.makeChanceResult()) {
            if (this.cardStack < ((getJob() == 2412) ? 40 : 20))
      {
        MapleCharacter tmp98_97 = this; tmp98_97.cardStack = (byte)(tmp98_97.cardStack + 1);
      }
      this.runningStack += 1;
      this.client.getSession().write(CField.gainCardStack(getId(), this.runningStack, (isAdv) ? 2 : 1, ceffect.getSourceId(), Randomizer.rand(100000, 500000), 1));
      this.client.getSession().write(CField.updateCardStack(this.cardStack));
    }
  }

    
    /*     */ public void resetRunningStack() {
/* 8292 */     this.runningStack = 0;
/*      */   }
    
    public int getRunningStack() {
/* 8288 */     return this.runningStack;
/*      */   }
/*      */ 
/*      */   public void addRunningStack(int s) {
/* 8292 */     this.runningStack += s;
/*      */   }

    public void setCardStack(byte amount) {
        this.cardStack = amount;
    }

    public byte getCardStack() {
        return cardStack;
    } // running id plox
    public final MapleCharacterCards getCharacterCard() {
        return characterCard;
    }

    /*
     * Start of Custom Feature
     */

    public int getAPS() {
        return apstorage;
    }

    public void gainAPS(int aps) {
        apstorage += aps;
    }
    public void doParagon() {
        if (level == 200) {
        Map<MapleStat, Integer> stat = new EnumMap<>(MapleStat.class);
        this.Paragons += 1;
        setExp(0);
        List<Pair<MapleStat, Integer>> paragon = new ArrayList<Pair<MapleStat, Integer>>(4);
        // final int oriStats = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt();

        // final int str = Randomizer.rand(25, stats.getStr());
        //  final int dex = Randomizer.rand(25, stats.getDex());
        // final int int_ = Randomizer.rand(25, stats.getInt());
        // final int luk = Randomizer.rand(25, stats.getLuk());

        //final int afterStats = str + dex + int_ + luk;

        // final int MAS = (oriStats - afterStats) + getRemainingAp();
        //   client.getPlayer().gainAPS(MAS);

        //  stats.recalcLocalStats(this);
        // stats.setStr((short) str, client.getPlayer());
        // stats.setDex((short) dex, client.getPlayer());
        // stats.setInt((short) int_, client.getPlayer());
        // stats.setLuk((short) luk, client.getPlayer());
         stat.put(MapleStat.STR, 1);
         stat.put(MapleStat.DEX, 1);
         stat.put(MapleStat.INT, 1);
         stat.put(MapleStat.LUK, 1);
         stat.put(MapleStat.AVAILABLEAP, 8); // doesnt update until cc/relog/cs I believe we can use updatesinglestat for this
        updateSingleStat(MapleStat.AVAILABLEAP, 8); 
        updateSingleStat(MapleStat.EXP, 0);
        client.getSession().write(CWvsContext.updatePlayerStats(stat, false, this));
        dropMessage(6, "You have reached Paragon level " + getParagons());
        }
    }
    

    public void doReborn() {
        if (level == 200 && Paragons < 8) {
        Map<MapleStat, Integer> stat = new EnumMap<>(MapleStat.class);
        this.reborns += 1;
        setLevel((short) 1); 
        setExp(0);
        setRemainingAp((int) 0);
        setRemainingSp((int) 0);
        setRemainingSp(0);
        setJob(GameConstants.isBeginnerJob);
        List<Pair<MapleStat, Integer>> reborn = new ArrayList<Pair<MapleStat, Integer>>(4);
        // final int oriStats = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt();

        // final int str = Randomizer.rand(25, stats.getStr());
        //  final int dex = Randomizer.rand(25, stats.getDex());
        // final int int_ = Randomizer.rand(25, stats.getInt());
        // final int luk = Randomizer.rand(25, stats.getLuk());

        //final int afterStats = str + dex + int_ + luk;

        // final int MAS = (oriStats - afterStats) + getRemainingAp();
        //   client.getPlayer().gainAPS(MAS);

        //  stats.recalcLocalStats(this);
        // stats.setStr((short) str, client.getPlayer());
        // stats.setDex((short) dex, client.getPlayer());
        // stats.setInt((short) int_, client.getPlayer());
        // stats.setLuk((short) luk, client.getPlayer());
        //  stat.put(MapleStat.STR, str);
        // stat.put(MapleStat.DEX, dex);
        // stat.put(MapleStat.INT, int_);
        // stat.put(MapleStat.LUK, luk);
        stat.put(MapleStat.AVAILABLEAP, 0);
        stat.put(MapleStat.AVAILABLESP, 0);
        updateSingleStat(MapleStat.AVAILABLESP, 0);
        updateSingleStat(MapleStat.AVAILABLEAP, 0);
        updateSingleStat(MapleStat.LEVEL, 1);
        updateSingleStat(MapleStat.JOB, 0); // check in Command instead. @rb, @rbd, @rbc.. whatever..
        updateSingleStat(MapleStat.EXP, 0);
        client.getSession().write(CWvsContext.updatePlayerStats(stat, false, this));
        }
        else {
            dropMessage(6, "You are not level 200 or you have more than 8 paragons.");
        }
    }

    public final boolean canHold(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public void gainCurrency(short amount, boolean sound) {
        if (sound) {
            client.getSession().write(CField.playSound("Coconut/Victory"));
        }
        MapleInventoryManipulator.addById(client, ServerConstants.CURRENCY, (short) amount, "gainCurrency function");
        dropMessage(-1, "You have gained " + amount + " " + (amount > 1 ? "" + MapleItemInformationProvider.getInstance().getName(ServerConstants.CURRENCY) + "s " : "" + MapleItemInformationProvider.getInstance().getName(ServerConstants.CURRENCY) + " ") + "");
    }

    public long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(long login) {
        this.loginTime = login;
    }

    public short getOccupation() {
        return occupationId;
    }

    public void changeOccupation(short occu) {
        this.occupationId = occu;
    }

    public short getOccupationEXP() {
        return occupationEXP;
    }
    


    public void gainOccupationEXP(int amount) {
        if (getOccupation() <= 0 || (getOccupation() % 10) >= 10 || amount >= 30000 || occupationEXP >= 30000) { // Max level already
            return;
        }
        dropMessage(-1, "You have " + (amount > 1 ? "lost " : "gained ") + "" + amount + " occupation experience.");
        final short occLevel = (short) (occupationId % 10);
        if (occupationEXP + amount >= OccupationConstants.getOccExpForLevel(occLevel)) {
            occupationEXP += amount;
            levelOccupation();

            final short needed = (short) OccupationConstants.getOccExpForLevel(occLevel);
            if (occupationEXP > needed) {
                occupationEXP = needed;
            }
        } else {
            occupationEXP += amount;
        }
    }

    public void levelOccupation() {
        if (this.occupationId % 10 < 10) {
            this.occupationId++; // Add 1
        }
        if (this.occupationId % 10 == 0) { // Max level occupation
            final StringBuilder sb = new StringBuilder("[" + TutorialConstants.beginnerNPCName + "] ");
            sb.append(getName());
            sb.append(" has leveled up to a Level 10 ").append(OccupationConstants.toString(occupationId)).append(". Congrats!!");
            World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, sb.toString())); // maybe <<use red color msg
        }
    }
    //prefix..
    /*
     * End of Custom Feature
     */

    ;   public int getStr() {
        return str;
    }

    public void setStr(int str) {
        this.str = str;
        stats.recalcLocalStats(this);
    }

    public int getInt() {
        return int_;
    }

    public void setInt(int int_) {
        this.int_ = int_;
        stats.recalcLocalStats(this);
    }

    public int getLuk() {
        return luk;
    }

    public int getDex() {
        return dex;
    }

    public void setLuk(int luk) {
        this.luk = luk;
        stats.recalcLocalStats(this);
    }

    public void setDex(int dex) {
        this.dex = dex;
        stats.recalcLocalStats(this);
    }

    public void gainvpoints(int hi) {
        vpoints = vpoints + hi;
    }

    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("vv", "Vv");
        i = i.replace("VV", "Vv");
        return i;
    }

    public void ban(String reason, boolean autoban, boolean mac) {
        try {
            if (mac) {
                client.banMacs();
            }
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
            ps.setString(1, reason);
            ps.setInt(2, accountid);
            ps.executeUpdate();
            ps.close();
            final String ip = client.getSession().getRemoteAddress().toString().split(":")[0];
            ps = con.prepareStatement("SELECT ip FROM ipbans WHERE ip = ?");
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, ip);
                ps.executeUpdate();
                ps.close();
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.getSession().close();
    }

    public void ban(String reason, boolean permBan) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        try {
            getClient().banMacs();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, 1);
            ps.setString(2, reason);
            ps.setInt(3, accountid);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
            String[] ipSplit = client.getSession().getRemoteAddress().toString().split(":");
            ps.setString(1, ipSplit[0]);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
        }
        client.getSession().close();
    }

    public static boolean ban(String id, String reason, boolean accountId) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.executeUpdate();
                ps.close();
                return true;
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PreparedStatement psb = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?");
                psb.setString(1, reason);
                psb.setInt(2, rs.getInt(1));
                psb.executeUpdate();
                psb.close();
                ret = true;
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }
        return false;
    }

    public static boolean unban(String name) {
        try {
            int accountid = -1;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                accountid = rs.getInt("accountid");
            }
            ps.close();
            rs.close();
            if (accountid == -1) {
                return false;
            }
            ps = con.prepareStatement("UPDATE accounts SET banned = -1 WHERE id = ?");
            ps.setInt(1, accountid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            //  log.error("Error while unbanning", ex);
            return false;
        }
        return true;
    }

    public void changeMap(int map, int portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }

    public void unchooseStolenSkill(int skillID) { //base skill
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final int job = GameConstants.getJobNumber(skillID / 10000);
        boolean changed = false;
        for (Pair<Integer, Boolean> sk : stolenSkills) {
            if (sk.right && GameConstants.getJobNumber(sk.left / 10000) == job) {
                cancelStolenSkill(sk.left);
                sk.right = false;
                changed = true;
            }
        }
        if (changed) {
            final Skill skil = SkillFactory.getSkill(skillID);
            changeSkillLevel_Skip(skil, getSkillLevel(skil), (byte) 0);
            client.getSession().write(CField.replaceStolenSkill(GameConstants.getStealSkill(job), 0));
        }
    }

    public void cancelStolenSkill(int skillID) {
        final Skill skk = SkillFactory.getSkill(skillID);
        final MapleStatEffect eff = skk.getEffect(getTotalSkillLevel(skk));

        if (eff.isMonsterBuff() || (eff.getStatups().isEmpty() && !eff.getMonsterStati().isEmpty())) {
            for (MapleMonster mons : map.getAllMonstersThreadsafe()) {
                for (MonsterStatus b : eff.getMonsterStati().keySet()) {
                    if (mons.isBuffed(b) && mons.getBuff(b).getFromID() == this.id) {
                        mons.cancelStatus(b);
                    }
                }
            }
        } else if (eff.getDuration() > 0 && !eff.getStatups().isEmpty()) {
            for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                chr.cancelEffect(eff, false, -1, eff.getStatups());

            }
        }
    }

    public void chooseStolenSkill(int skillID) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final Pair<Integer, Boolean> dummy = new Pair<Integer, Boolean>(skillID, false);
        if (stolenSkills.contains(dummy)) {
            unchooseStolenSkill(skillID);
            stolenSkills.get(stolenSkills.indexOf(dummy)).right = true;

            client.getSession().write(CField.replaceStolenSkill(GameConstants.getStealSkill(GameConstants.getJobNumber(skillID / 10000)), skillID));
            //if (ServerConstants.CUSTOM_SKILL) {
            //    client.getSession().write(MaplePacketCreator.skillCooldown(20031208, 5));
            //    addCooldown(20031208, System.currentTimeMillis(), 5000);
            //}
        }
    }
    
        public boolean gainItem(int id, short quantity) { 
        if (quantity >= 0) { 
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance(); 
            Item item = ii.getEquipById(id); 
            MapleInventoryType type = GameConstants.getInventoryType(id); 
 	if (type.equals(MapleInventoryType.EQUIP) && !GameConstants.isThrowingStar(item.getItemId()) && !GameConstants.isBullet(item.getItemId())) { 
            if (!getInventory(type).isFull()) { 
                MapleInventoryManipulator.addFromDrop(getClient(), (Equip) item, false); 
            } else { 
 		dropMessage(1, "Your inventory is full. Please remove an item from your " + type.name().toLowerCase() + " inventory."); 
 		return false; 
            } 
 	} else { 
            dropMessage(1, "Your inventory is full. Please remove an item from your " + type.name().toLowerCase() + " inventory."); 
            return false; 
 	}  
        
        } else { 
            MapleInventoryManipulator.removeById(getClient(), GameConstants.getInventoryType(id), id, -quantity, false, false); 
 	} 
 	return true; 
    } 

    public void addStolenSkill(int skillID, int skillLevel) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final Pair<Integer, Boolean> dummy = new Pair<Integer, Boolean>(skillID, true);
        final Skill skil = SkillFactory.getSkill(skillID);
        if (!stolenSkills.contains(dummy) && GameConstants.canSteal(skil)) {
            dummy.right = false;
            skillLevel = Math.min(skil.getMaxLevel(), skillLevel);
            final int job = GameConstants.getJobNumber(skillID / 10000);
            if (!stolenSkills.contains(dummy) && getSkillLevel(GameConstants.getStealSkill(job)) > 0) {
                int count = 0;
                skillLevel = Math.min(getSkillLevel(GameConstants.getStealSkill(job)), skillLevel);
                for (Pair<Integer, Boolean> sk : stolenSkills) {
                    if (GameConstants.getJobNumber(sk.left / 10000) == job) {
                        count++;
                    }
                }
                if (count < GameConstants.getNumSteal(job)) {
                    stolenSkills.add(dummy);
                    changed_skills = true;
                    changeSkillLevel_Skip(skil, skillLevel, (byte) skillLevel);
                    client.getSession().write(CField.addStolenSkill(job, count, skillID, skillLevel));
                    //client.getSession().write(MaplePacketCreator.updateStolenSkills(this, job));
                }
            }
        }
    }

    public void removeStolenSkill(int skillID) {
        if (skillisCooling(20031208) || stolenSkills == null) {
            dropMessage(-6, "[Loadout] The skill is under cooldown. Please wait.");
            return;
        }
        final int job = GameConstants.getJobNumber(skillID / 10000);
        final Pair<Integer, Boolean> dummy = new Pair<Integer, Boolean>(skillID, false);
        int count = -1, cc = 0;
        for (int i = 0; i < stolenSkills.size(); i++) {
            if (stolenSkills.get(i).left == skillID) {
                if (stolenSkills.get(i).right) {
                    unchooseStolenSkill(skillID);
                }
                count = cc;
                break;
            } else if (GameConstants.getJobNumber(stolenSkills.get(i).left / 10000) == job) {
                cc++;
            }
        }
        if (count >= 0) {
            cancelStolenSkill(skillID);
            stolenSkills.remove(dummy);
            dummy.right = true;
            stolenSkills.remove(dummy);
            changed_skills = true;
            changeSkillLevel_Skip(SkillFactory.getSkill(skillID), 0, (byte) 0);
            //hacky process begins here
            client.getSession().write(CField.replaceStolenSkill(GameConstants.getStealSkill(job), 0));
            for (int i = 0; i < GameConstants.getNumSteal(job); i++) {
                client.getSession().write(CField.removeStolenSkill(job, i));
            }
            count = 0;
            for (Pair<Integer, Boolean> sk : stolenSkills) {
                if (GameConstants.getJobNumber(sk.left / 10000) == job) {
                    client.getSession().write(CField.addStolenSkill(job, count, sk.left, getSkillLevel(sk.left)));
                    if (sk.right) {
                        client.getSession().write(CField.replaceStolenSkill(GameConstants.getStealSkill(job), sk.left));
                    }
                    count++;
                }
            }
            client.getSession().write(CField.removeStolenSkill(job, count));
            //client.getSession().write(MaplePacketCreator.updateStolenSkills(this, job));
        }
    }

    public List<Pair<Integer, Boolean>> getStolenSkills() {
        return stolenSkills;
    }

    public void changeSkillLevel_Skip(Skill skil, int skilLevel, byte masterLevel) {
        final Map<Skill, SkillEntry> enry = new HashMap<Skill, SkillEntry>(1);
        enry.put(skil, new SkillEntry(skilLevel, masterLevel, -1L));
        changeSkillLevel_Skip(enry, true);
    }
  
public int getBlackJackChips() {
return blackjackchips;
}

public void gainBlackJackChips(int gain) {
blackjackchips += gain;
}  
public final void changeMap(int targetMap) {
changeMap(this.client.getChannelServer().getMapFactory().getMap(targetMap));
}  

public final boolean haveEXPCard(){ 
        return haveItem(5211048, 1);
}  
  public long getInvulerableTime() {
        return invulnerableTime;
    }

    public void setInvulnerableTime() {
        this.invulnerableTime = System.currentTimeMillis();
    }

    public long getLastLotto() {
        return lastlotto;
    }

    public void setLastLotto(long lastlottos) {
        this.lastlotto = lastlottos;
    }
    
    public long getPendingMarriageId() {
        return pendingMarriageId;
    }

    public void setPendingMarriageId(int marriageid) {
        this.pendingMarriageId = marriageid;
    }
    
    public long getGmCall() {
        return lastgmcall;
    }

    public void setGmCall(long gmcall) {
        this.lastgmcall = gmcall;
    }
    
       public void serverNotice(final String msg) {
        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, msg));
    }
       
         public int getCharsOnMap() {
        return getMap().getCharacters().size();
    }
         
         	public int getFfaKills() {
        return ffakills;
    }

    public void setFfaKills(int k) {
        ffakills = k;
    }
	
	public void gainFfaKills(int k) {
        ffakills += k;
    }
	
	public int getFfaDeaths() {
        return ffadeaths;
    }

    public void setFfaDeaths(int d) {
        ffadeaths = d;
    }
	
	public void gainFfaDeaths(int d) {
        ffadeaths += d;
    }
        
            public int getNumber() {
        return getClient().getChannelServer().number;
    }
            
                public int getAcDualHp() {
        return acdualhp;
    }
    
    public void gainAcDualHp(int HP) {
        acdualhp += HP;
    }
    
    public void setAcDualHp(int HP) {
        acdualhp = HP;
    }
    
            
    public int vote(int num) {
        final long now = System.currentTimeMillis() / 1000;
        if (num < 0 || num > 1) {
            return -1;
        }
        if ((num == 0 ? lastVoteTime : lastVoteTime2) >= (now - 21600)) {
            return (int) (21600 - (now - (num == 0 ? lastVoteTime : lastVoteTime2))) / 60;
        }
        try {
            final Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT votetime FROM ipvotelog WHERE (ipaddress = ? OR accid = ?) AND votetype = ? AND votetime >= ?");
            ps.setString(1, client.getSessionIPAddress().replace("/", ""));
            ps.setInt(2, getAccountID());
            ps.setInt(3, num);
            ps.setLong(4, (now - 21600));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                final int ret = (int) ((21600 - (now - rs.getLong("votetime"))) / 60);
                rs.close();
                ps.close();
                return ret;
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("INSERT INTO ipvotelog (ipaddress, accid, votetime, votetype) VALUES (?, ?, ?, ?)");
            ps.setString(1, client.getSessionIPAddress().replace("/", ""));
            ps.setInt(2, getAccountID());
            ps.setLong(3, now);
            ps.setInt(4, num);
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("UPDATE accounts SET lastvoteip = ? WHERE id = ?");
            ps.setString(1, client.getSessionIPAddress().replace("/", ""));
            ps.setInt(2, getAccountID());
            ps.executeUpdate();
            ps.close();

            vpoints += client.getChannelServer().getPointsPerVote();
            totalvotes++;
            if (num == 0) {
                lastVoteTime = now;
                if (GameConstants.GMS) {
                    client.getSession().write(UIPacket.getGMBoard(totalvotes, ServerConstants.vote1));
                }
                client.getSession().write(LoginPacket.getCustomEncryption(ServerConstants.vote1));
            } else {
                lastVoteTime2 = now;
                if (GameConstants.GMS) {
                    client.getSession().write(UIPacket.getGMBoard(totalvotes, ServerConstants.vote2));
                }
                client.getSession().write(LoginPacket.getCustomEncryption(ServerConstants.vote2));
            }
            return 0;
        } catch (SQLException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            return -1;
        }
    }  
   /* 
   public void handlePvpDmg(int damage) {
        if ((getStat().dodgeChance > 0) && (Randomizer.nextInt(100) < getStat().dodgeChance)) {
            getClient().getSession().write(CField.EffectPacket.showForeignEffect(20));
            return;
        }    
        if (getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
            int hploss = 0; int mploss = 0;
            mploss = (int)(damage * (getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0D));
            hploss = damage - mploss;
            if (getBuffedValue(MapleBuffStat.INFINITY) != null) {
                mploss = 0;
            } else if (mploss > stats.getMp()) {
                mploss = stats.getMp();
                hploss = damage - mploss;
            }
            addMPHP(-hploss, -mploss);
        } else if (getStat().mesoGuardMeso > 0.0D) {
            int mesoloss = (int)(damage * (getStat().mesoGuardMeso / 100.0D));
            if (getMeso() < mesoloss) {
                gainMeso(-getMeso(), false);
                cancelBuffStats(new MapleBuffStat[] {MapleBuffStat.MESOGUARD});
            } else {
                gainMeso(-mesoloss, false);
            }
            addMPHP(-damage, 0);
        } else {    
            addMPHP(-damage, 0);
        }
        if (!GameConstants.GMS) {
            handleBattleshipHP(-damage);
        }
        
        byte[] dmgPacket = CField.damagePlayer(getId(), -2, damage, -1, (byte) -1, -1, -1, false, -1, (byte) -1, null, (byte) -1, -1, 0);
        getMap().broadcastMessage(dmgPacket);
        getMap().broadcastMessage(MobPacket.showMonsterHP(map.getMobFromChr(this).getObjectId(), getStat().getHPPercent()));
    }
*/
     public void setWeapon(int id){  //Use this to give mihiles and those other fugazi classes their weapons when they job adv. - Rummy
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        Item item = li.getEquipById(id);
        item.setPosition((byte) -11); //-1 Hat | -2 Face | -3 Eye acc | -4 Ear acc | -5 Overall | -7 Shoes | -9 Cape | -10 Shield | -11 Weapon -Rummy
        equip.addFromDB(item);
        this.equipChanged();
    }
     
     public void setOverall(int id) {
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        Item item = li.getEquipById(id);
        item.setPosition((byte) -5); //-1 Hat | -2 Face | -3 Eye acc | -4 Ear acc | -5 Overall | -7 Shoes | -9 Cape | -10 Shield | -11 Weapon -Rummy
        equip.addFromDB(item);
        this.equipChanged();
     }
     
     public void setTop(int id) {
         final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
         final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
         Item item = li.getEquipById(id);
         item.setPosition((byte) -6);
         equip.addFromDB(item);
         this.equipChanged();
     }
     
     public void setBottom(int id) {
         final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
         final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
         Item item = li.getEquipById(id);
         item.setPosition((byte) -13);
         equip.addFromDB(item);
         this.equipChanged();
     }


    public void setShield(int id){ //as above use this to give mihiles and stuff their shields that can only be given from the server. Thanks based nexon! - Rummy
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        Item item = li.getEquipById(id);
        item.setPosition((byte) -10); //-1 Hat | -2 Face | -3 Eye acc | -4 Ear acc | -5 Overall | -7 Shoes | -9 Cape | -10 Shield | -11 Weapon -Rummy
        equip.addFromDB(item);
        this.equipChanged();
    }
        public void setEvoItemStats(double stats) {
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        chr.getEquips().get(id);
        
             
        this.setStr((int) (Math.random() + 6));
        this.setDex((short)(this.getDex() + stats));
        this.setInt((short)(this.getInt() + stats));
        this.setLuk((short)(this.getLuk() + stats));
        
    }


    public void evolveItem(int id) {
       if (level == 200) {
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
       }
        if (Paragons == 10) {
        final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
                
                }
        
                if (Paragons == 30) {

       final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
        
       }
                                if (Paragons == 70) {

       final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
        
       }
        if (Paragons == 120) {

       final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
        
       }
                if (Paragons == 200) {

       final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
        
       }
                    if (Paragons == 300) {

       final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
        
       }
          if (Paragons == 500) {

       final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
        
       }
                              if (Paragons == 600) {

       final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
        final MapleInventory equip = this.getInventory(MapleInventoryType.EQUIPPED);
        this.getInventory(MapleInventoryType.EQUIPPED);
        this.getEquips().get(id);
        
        Item item = li.getEquipById(id);
        this.setEvoItemStats(id);
        this.setEvoItemStats(str);
        this.setEvoItemStats(dex);
        this.setEvoItemStats(luk);
        this.getInventory(MapleInventoryType.EQUIPPED);
        
        item.setPosition((byte) -11);
        equip.addFromDB(item);
        this.equipChanged();
        
       }
    
                              else {
                              dropMessage(6, "You do not meet the requirments to evolve :( ");
                              }
    }
    

            
            public int getBossLog(String bossid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            int ret_count = 0;
            PreparedStatement ps;
            ps = con1.prepareStatement("select count(*) from bosslog where characterid = ? and bossid = ? and lastattempt >= subtime(current_timestamp, '1 0:0:0.0')");
            ps.setInt(1, id);
            ps.setString(2, bossid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) 
                ret_count = rs.getInt(1);
            else
                ret_count = -1;
            rs.close();
            ps.close();
            return ret_count;
        } catch (Exception Ex) {
            return -1;
        }
    }
            
                     public int getScavengerLog(String scavid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            int ret_count = 0;
            PreparedStatement ps;
            ps = con1.prepareStatement("select count(*) from scavengerlog where characterid = ? and scavid = ? and lastattempt >= subtime(current_timestamp, '1 0:0:0.0')");
            ps.setInt(1, id);
            ps.setString(2, scavid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) 
                ret_count = rs.getInt(1);
            else
                ret_count = -1;
            rs.close();
            ps.close();
            return ret_count;
        } catch (Exception Ex) {
            return -1;
        }
    }
                     
                           public int getBountyLog(String bountyid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            int ret_count = 0;
            PreparedStatement ps;
            ps = con1.prepareStatement("select count(*) from bountylog where characterid = ? and bountyid = ? and lastattempt >= subtime(current_timestamp, '1 0:0:0.0')");
            ps.setInt(1, id);
            ps.setString(2, bountyid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) 
                ret_count = rs.getInt(1);
            else
                ret_count = -1;
            rs.close();
            ps.close();
            return ret_count;
        } catch (Exception Ex) {
            return -1;
        }
    }
                     
                       public int getCodeLog(String codeid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            int ret_count = 0;
            PreparedStatement ps;
            ps = con1.prepareStatement("select count(*) from codelog where characterid = ? and codeid = ? and lastattempt >= subtime(current_timestamp, '1 0:0:0.0')");
            ps.setInt(1, id);
            ps.setString(2, codeid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) 
                ret_count = rs.getInt(1);
            else
                ret_count = -1;
            rs.close();
            ps.close();
            return ret_count;
        } catch (Exception Ex) {
            return -1;
        }
    }

    public int getGiftLog(String bossid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            int ret_count = 0;
            PreparedStatement ps;
            ps = con1.prepareStatement("select count(*) from bosslog where accountid = ? and bossid = ? and lastattempt >= subtime(current_timestamp, '1 0:0:0.0')");
            ps.setInt(1, accountid);
            ps.setString(2, bossid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) 
                ret_count = rs.getInt(1);
            else
                ret_count = -1;
            rs.close();
            ps.close();
            return ret_count;
        } catch (Exception Ex) {
            return -1;
        }
    }

    //setBossLog module
    public void setBossLog(String bossid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ps = con1.prepareStatement("insert into bosslog (accountid, characterid, bossid) values (?,?,?)");
            ps.setInt(1, accountid);
            ps.setInt(2, id);
            ps.setString(3, bossid);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
        }
    }
    
     public void setScavLog(String scavid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ps = con1.prepareStatement("insert into scavengerlog (accountid, characterid, scavid) values (?,?,?)");
            ps.setInt(1, accountid);
            ps.setInt(2, id);
            ps.setString(3, scavid);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
        }
    }
     
     
      public void setBountyLog(String bountyid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ps = con1.prepareStatement("insert into bountylog (accountid, characterid, scavid) values (?,?,?)");
            ps.setInt(1, accountid);
            ps.setInt(2, id);
            ps.setString(3, bountyid);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
        }
    }
     
       public void setCodeLog(String codeid) {
        Connection con1 = DatabaseConnection.getConnection();
        try {
            PreparedStatement ps;
            ps = con1.prepareStatement("insert into codelog (accountid, characterid, codeid) values (?,?,?)");
            ps.setInt(1, accountid);
            ps.setInt(2, id);
            ps.setString(3, codeid);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
        }
    }
    


    public void gainItem(int code, int amount) {
        MapleInventoryManipulator.addById(client, code, (short) amount, null);
    }

   public int getElf() {
        return elf;
    }

    public void setElf(int elf) {
        this.elf = elf;
        equipChanged();
    }
    
    public void InjectVaccination(int i) {
        	vaccinated += i;
    	}
        
                public byte isVaccinated() {
            return vaccinated;
        }
                
                     public int getMonsterkills() {
        return monsterkills;
    }

    public void setMonsterkills1(final int countmob) { // You're an asshole if you use this for anything other than MapleMap.java.
        monsterkills += countmob;
    }
    
    public void setMonsterkills2(final int countmob) { // You're an asshole if you use this for anything other than MapleMap.java. This is sort of worthless.
        monsterkills = countmob;
    }  

    public final void maxAllSkills() {
HashMap<Skill, SkillEntry> sa = new HashMap<>();
for (Skill skil : SkillFactory.getAllSkills()) {
if (GameConstants.isApplicableSkill(skil.getId()) && skil.getId() < 90000000) { //no db/additionals/resistance skills
sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
}
}
chr.changeSkillsLevel(sa);
}

   
    public short getTlvl() {
        return Tlvl;
    }
    
     public final void setTormentLevel(final short Tlvl){ //this will set the Torment level
         if (setTormentMode()) {
             this.Tlvl = Tlvl;
           
         }
             
    }
     
         public void TurnT1On() {

    }
         
          public final int getTormentLvL() {
        return Tlvl;
    }
    
    
           public boolean setTormentMode() {
        return toggleTormentMode1;
    }
    public static boolean toggleTormentMode1 = false;
    
    public void isTormentMode() {
        if (!toggleTormentMode1) {
            
            toggleTormentMode1 = true;
            chr.fakeRelog();
        } else {
            toggleTormentMode1 = false;
            chr.fakeRelog();
        }
    }
    
    public boolean setT1Mode() {
        return tormentmode1;
    }
    public static boolean tormentmode1 = false;
    
    public void isTorment1() {
        if (MapleCharacter.Tchr.getClient().getChannel() == 11) {
            tormentmode1 = true;
            
        }
        else {
            tormentmode1 = false;
        }
    }
       public boolean setT2Mode() {
        return tormentmode2;
    }
    public static boolean tormentmode2 = false; 
    
      public void isTorment2() {
        if (MapleCharacter.Tchr.getClient().getChannel() == 12) {
            tormentmode2 = true;
            
        }
        else {
            tormentmode2 = false;
        }
    }
      
       
    
     public boolean setT7Mode() {
        return tormentmode7;
    }
    
      public static boolean tormentmode7 = false;
      
       public void isTorment7() {

        if ((!toggleTormentMode1) && MapleCharacter.Tchr.getClient().getChannel() == 13) {
            tormentmode7 = true;
            chr.fakeRelog();
            
        }
        else {
            tormentmode7 = false;
            chr.fakeRelog();
        }
    }
       
       public int getPandoraLog(String itemid) {
    	Connection con1 = DatabaseConnection.getConnection();
    	try {
    	int ret_count = 0;
    	PreparedStatement ps;
    	ps = con1.prepareStatement("select count(*) from pandoralog where characterid = ? and itemid = ? and lastattempt >= subtime(current_timestamp, '1 0:0:0.0')");
    	ps.setInt(1, id);
    	ps.setString(2, itemid);
    	ResultSet rs = ps.executeQuery();
    	if (rs.next()) {
    	ret_count = rs.getInt(1);
    	} else {
    	ret_count = -1;
    	}
    	rs.close();
    	ps.close();
    	return ret_count;
    	} catch (Exception Ex) {
    		System.out.print("Error reading Pandora Log.");
    	return -1;
    	}
    	}
       
        public void setPandoraLog(String itemid) {
    	Connection con1 = DatabaseConnection.getConnection();
    	try {
    	PreparedStatement ps;
    	ps = con1.prepareStatement("insert into pandoralog (characterid, itemid) values (?,?)");
    	ps.setInt(1, id);
    	ps.setString(2, itemid);
    	ps.executeUpdate();
    	ps.close();
    	} catch (Exception Ex) {
    		System.out.print("Error setting the Pandora Log.");
    	}
    	}
          public List<InnerSkillValueHolder> getInnerSkills() {
        return innerSkills;
    }
        
     public void applyInners() {
        stats.recalcLocalStats(this);
        innerskill_changed = true;
    }

    public void setHonourExp(int exp) {
        this.honourExp = exp;
    }

    public int getHonourExp() {
        return honourExp;
    }

    public void setHonourLevel(int level) {
        this.honourLevel = level;
    }

    public int getHonourLevel() {
        if (honourLevel == 0) {
            honourLevel++;
        }
        return honourLevel;
    }



    public void addHonourExp(int amount) {
        int oldHonourExp = getHonourExp();
        int newHonourExp = getHonourExp() + amount;
        if (getHonourLevel() == 0) {
            setHonourLevel(1);
        }
        do {
            honourLevelUp();
            newHonourExp = (newHonourExp) - ((getHonourLevel() - 1) * 500);
            //leftamount = Math.min(leftamount, ((getHonourLevel()) * 500) - 1);
            setHonourExp(newHonourExp);
        } while (newHonourExp >= getHonourLevel() * 500);
        
        setHonourExp(getHonourExp() + amount);
        client.getSession().write(CField.updateHonour(getHonourLevel(), getHonourExp(), true));
        client.getSession().write(CWvsContext.professionInfo("honorLeveling", 0, getHonourLevel(), getHonourNextExp()));
    }

    public int getHonourNextExp() {
        if (getHonourLevel() == 0) {
            return 0; //shush 
        }
        return (getHonourLevel() + 1) * 500;
    }

    public void honourLevelUp() {
        setHonourLevel(getHonourLevel() + 1);
        client.getSession().write(CField.updateHonour(getHonourLevel(), getHonourExp(), true));
        InnerSkillValueHolder inner = null;
        byte ability = 0;
        switch (getHonourLevel()) {
            case 2:
                inner = InnerAbillity.getInstance().renewSkill(0, -1);
                ability = 1;
                break;
            case 30:
                inner = InnerAbillity.getInstance().renewSkill(Randomizer.rand(0, 2), -1);
                ability = 2;
                break;
            case 70:
                inner = InnerAbillity.getInstance().renewSkill(Randomizer.rand(1, 3), -1);
                ability = 3;
                break;
        }
        if (getHonourLevel() == 2 || getHonourLevel() == 30 || getHonourLevel() == 70 && inner != null && ability >= 1 && ability <= 3) {
            innerSkills.add(inner);
            changeSingleSkillLevel(SkillFactory.getSkill(inner.getSkillId()), inner.getSkillLevel(), inner.getSkillLevel());
            client.getSession().write(CField.getCharInfo(this));
            client.getSession().write(CField.updateInnerPotential(ability, inner.getSkillId(), inner.getSkillLevel(), inner.getRank()));
            applyInners();
        }
        makeNewAzwanShop();
    }
    
     public void makeNewAzwanShop() {
        azwanShopList = new MapleShop(100000000 + getId(), 2182002);
        int itemid = GameConstants.getAzwanRecipes()[(int) Math.floor(Math.random() * GameConstants.getAzwanRecipes().length)];
        azwanShopList.addItem(new MapleShopItem((short) 1, itemid, 1, 4310038, 70, (byte) 0, 0, 0, 0));
        itemid = GameConstants.getAzwanScrolls()[(int) Math.floor(Math.random() * GameConstants.getAzwanScrolls().length)];
        azwanShopList.addItem(new MapleShopItem((short) 1, itemid, 1, 4310036, 15, (byte) 0, 0, 0, 0));
        itemid = GameConstants.getAzwanScrolls()[(int) Math.floor(Math.random() * GameConstants.getAzwanScrolls().length)];
        azwanShopList.addItem(new MapleShopItem((short) 1, itemid, 1, 4310036, 15, (byte) 0, 0, 0, 0));
        itemid = (Integer) GameConstants.getUseItems()[(int) Math.floor(Math.random() * GameConstants.getUseItems().length)].getLeft();
        int price = (Integer) GameConstants.getUseItems()[(int) Math.floor(Math.random() * GameConstants.getUseItems().length)].getRight();
        azwanShopList.addItem(new MapleShopItem((short) 1, itemid, 1, price, 0, (byte) 0, 0, 0, 0));
        itemid = GameConstants.getCirculators()[(int) Math.floor(Math.random() * GameConstants.getCirculators().length)];
        price = InnerAbillity.getInstance().getCirculatorRank(itemid) * 10;
        if (InnerAbillity.getInstance().getCirculatorRank(itemid) > 3) {
            price *= 2;
        }
        azwanShopList.addItem(new MapleShopItem((short) 1, itemid, 1, InnerAbillity.getInstance().getCirculatorRank(itemid) > 5 ? 4310038 : 4310036, 0, (byte) 0, 0, 0, 0));
        if(getCheatTracker().canJeanPierreWhisper()){
            client.getSession().write(CField.getWhisper("Jean Pierre", client.getChannel(), "Psst! I got some new items in stock! Come take a look! Oh, but if your Honor Level increased, why not wait until you get a Circulator?"));
        }
    }

    public MapleShop getAzwanShop() {
        return azwanShopList;
    }

    public void openAzwanShop() {
        if (azwanShopList == null) {
            MapleShopFactory.getInstance().getShop(2182002).sendShop(client);
        } else {
            getAzwanShop().sendShop(client);
        }
    }

    public void rewardAzwanPrize(int mobsKilled) {
        int gainedHonourExp = mobsKilled;
        if (getLevel() < 100) {
            gainedHonourExp *= 30;
        } else if (getLevel() >= 100 && getLevel() <= 200) {
            gainedHonourExp *= 40;
        } else if (getLevel() >= 201) {
            gainedHonourExp *= 50;
        }
        int availCCoins = getAzwanCCoinsAvail();
        int availECoins = getAzwanECoinsAvail();
        if (mobsKilled >= 0 && mobsKilled < 25) {
            addHonourExp(gainedHonourExp);
            dropMessage(5, "Your party has thwarted " + mobsKilled + " minions! You have received " + gainedHonourExp + " Honor experience!");
        } else if (mobsKilled >= 25 && mobsKilled < 100) {
            setAzwanCCoinsAvail(availCCoins + 3);
            addHonourExp(gainedHonourExp);
            dropMessage(5, "Your party has thwarted " + mobsKilled + " minions! You have received " + 3 + " Conqueror's Coins and " + gainedHonourExp + " Honor experience!");
        } else if (mobsKilled >= 100 && mobsKilled < 250) {
            setAzwanCCoinsAvail(availCCoins + 15);
            addHonourExp(gainedHonourExp);
            dropMessage(5, "Your party has thwarted " + mobsKilled + " minions! You have received " + 15 + " Conqueror's Coins and " + gainedHonourExp + " Honor experience!");
        } else if (mobsKilled >= 250 && mobsKilled < 450) {
            setAzwanECoinsAvail(availECoins + 2);
            addHonourExp(gainedHonourExp);
            dropMessage(5, "Your party has thwarted " + mobsKilled + " minions! You have received " + 2 + " Emperor's Coins and " + gainedHonourExp + " Honor experience!");
        } else if (mobsKilled >= 450 && mobsKilled != 9999) {
            setAzwanECoinsAvail(availECoins + 4);
            addHonourExp(gainedHonourExp);
            dropMessage(5, "Your party has thwarted " + mobsKilled + " minions! You have received " + 4 + " Emperor's Coins and " + gainedHonourExp + " Honor experience!");
        } else if (mobsKilled == 9999) {
            setAzwanECoinsAvail(availECoins + 7);
            setAzwanCCoinsAvail(availCCoins + 25);
            addHonourExp(gainedHonourExp);
        }
    }

    public int getAzwanCoinsAvail() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT azwanCoinsAvail FROM characters WHERE id = ?");
            ps.setInt(1, client.getPlayer().getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                azwanCoinsAvail = rs.getInt("azwanCoinsAvail");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting azwanCoinsAvail for character: " + client.getPlayer().getName() + ". error: " + e);
        }

        return azwanCoinsAvail;
    }

    public int getAzwanCCoinsAvail() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT azwanCCoinsAvail FROM characters WHERE id = ?");
            ps.setInt(1, client.getPlayer().getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                azwanECoinsAvail = rs.getInt("azwanCCoinsAvail");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting azwanCCoinsAvail for character: " + client.getPlayer().getName() + ". error: " + e);
        }

        return azwanECoinsAvail;
    }

    public int getAzwanECoinsAvail() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT azwanECoinsAvail FROM characters WHERE id = ?");
            ps.setInt(1, client.getPlayer().getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                azwanCCoinsAvail = rs.getInt("azwanECoinsAvail");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting azwanECoinsAvail for character: " + client.getPlayer().getName() + ". error: " + e);
        }

        return azwanCCoinsAvail;
    }

    public int getAzwanCoinsRedeemed() {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT azwanCoinsRedeemed FROM characters WHERE id = ?");
            ps.setInt(1, client.getPlayer().getId());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                azwanCoinsRedeemed = rs.getInt("azwanCoinsRedeemed");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting azwanCoinsRedeemed for character: " + client.getPlayer().getName() + ". error: " + e);
        }

        return azwanCoinsRedeemed;
    }

    public void setAzwanCoinsAvail(int quantity) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("UPDATE characters SET azwanCoinsAvail = ? WHERE id = ?");
            ps.setInt(1, quantity);
            ps.setInt(2, client.getPlayer().getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error setting azwanCoinsAvail for character: " + client.getPlayer().getName() + ". error: " + e);
        }
    }

    public void setAzwanECoinsAvail(int quantity) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("UPDATE characters SET azwanECoinsAvail = ? WHERE id = ?");
            ps.setInt(1, quantity);
            ps.setInt(2, client.getPlayer().getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error setting azwanECoinsAvail for character: " + client.getPlayer().getName() + ". error: " + e);
        }
    }

    public void setAzwanCCoinsAvail(int quantity) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("UPDATE characters SET azwanCCoinsAvail = ? WHERE id = ?");
            ps.setInt(1, quantity);
            ps.setInt(2, client.getPlayer().getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error setting azwanCCoinsAvail for character: " + client.getPlayer().getName() + ". error: " + e);
        }
    }

    public void setAzwanCoinsRedeemed(int quantity) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("UPDATE characters SET azwanCoinsRedeemed = ? WHERE id = ?");
            ps.setInt(1, quantity);
            ps.setInt(2, client.getPlayer().getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error setting azwanCoinsRedeemed for character: " + client.getPlayer().getName() + ". error: " + e);
        }
    }

    public boolean checkArdentmill() {
        boolean inDBAlready = false;
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT characterID FROM ardentmill WHERE characterID = ?");
            ps.setInt(1, client.getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                inDBAlready = true;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error checking checkArdentmill for character: " + client.getPlayer().getName() + ". error: " + e);
            inDBAlready = false;
        }

        return inDBAlready;
    }

    public void setArdentmillPortal(String Portal, int quantity) {
        if (checkArdentmill()) {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                ps = con.prepareStatement("UPDATE ardentmill SET " + Portal + " = ? WHERE characterID = ?");
                ps.setInt(1, quantity);
                ps.setInt(2, client.getPlayer().getId());
                ps.execute();
                ps.close();
            } catch (SQLException e) {
                System.err.println("Error setting ArdentmillPortal #1" + Portal + "(" + quantity + ") for character: " + client.getPlayer().getName() + ". error: " + e);
            }
        } else {
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps;
                ps = con.prepareStatement("INSERT INTO ardentmill (characterID, " + Portal + ") VALUES (?, ?)");
                ps.setInt(1, client.getPlayer().getId());
                ps.setInt(2, quantity);
                ps.execute();
                ps.close();
            } catch (SQLException e) {
                System.err.println("Error setting ArdentmillPortal #2" + Portal + "(" + quantity + ") for character: " + client.getPlayer().getName() + ". error: " + e);
            }
        }
    }

    public int getArdentmillPortal(String Portal) {
        int timesEnteredDaily = 0;
        if (!checkArdentmill()) {
            return 0;
        }
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT " + Portal + " FROM ardentmill WHERE characterID = ?");
            ps.setInt(1, client.getPlayer().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                timesEnteredDaily = rs.getInt(Portal);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting ArdentmillPortal for character: " + client.getPlayer().getName() + ". error: " + e);
        }

        return timesEnteredDaily;
    }
     
//    public boolean getpetAutoFeed() {
 //     return petAutoFeed;
 //   }
     
    public void dropEquippedItems() {
        List<Item> items = new LinkedList<Item>();
        for(Item equips : getInventory(MapleInventoryType.EQUIPPED)) {
            items.add(equips);
        }
        for(Item equips : getInventory(MapleInventoryType.EQUIPPED)) {
            Item toDrop = MapleItemInformationProvider.getInstance().getEquipById(equips.getItemId());
            getMap().spawnItemDrop(this, this, toDrop, this.getPosition(), true, true);
        }
        for (Item item : items) {
            MapleInventoryManipulator.removeById(client, MapleInventoryType.EQUIPPED, item.getItemId(), item.getQuantity(), false, false);
        }
        items.clear();
    }  
    
   private void playerDeadHC() {
        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, chr.getName() + "has died in Nightmare mode. Lulz RIP in peace. Their equips were dropped where they died."));
       client.getSession().close();
        client.deleteCharacter(id);
        dropEquippedItems();
    // for HC Mode to delete char when they die ;^) whatever other things need to be done
}
  
private long mesosInBank;

public long getMesosInBank() {
    return mesosInBank;
}

public void depositMesosToBank(long mesos) {
    if(getMeso() >= mesos) {
        this.mesosInBank += mesos;
        if(getMesosInBank() >= 1000000000000L) {
            long difference = getMesosInBank() - 1000000000000L;
            this.mesosInBank = 1000000000000L;
            chr.dropMessage(6, "You currently have 1 Trillion mesos in the bank, so you cannot deposit any more mesos.");
            gainMeso((int) -(mesos - difference), true);
        } else {
            gainMeso((int) -mesos, true);
            chr.dropMessage(6, "You have successfully deposited "+ mesos +" mesos to the bank. You now have "+ getMesosInBank() +" mesos saved.");
        }
    } else {
        chr.dropMessage(6, "You do not have enough mesos to complete this deposit.");
    }
}

public void withdrawMesosFromBank(long mesos) {
    if(getMesosInBank() > mesos) {
        if(getMeso() + mesos <= 2100000000) {
            this.mesosInBank -= mesos;
            gainMeso((int) mesos, true);
            chr.dropMessage(6, "You have successfully withdrawn "+ mesos +" mesos from the bank. You now have "+ getMesosInBank() +" mesos in the bank.");
        } else {
            chr.dropMessage(6, "You cannot withdraw "+ mesos +" mesos or else you will go over the limit for the max mesos you can hold.");
        }
    } else {
        chr.dropMessage(6, "You cannot withdraw "+ mesos +" from the bank. You only have "+ getMesosInBank() +" mesos saved.");
    }
}  

public void withdrawMesosBank(long mesos) {
        mesosbank = mesosbank - mesos;
    }


    public void addMesosBank(long mesos) {
        mesosbank = mesosbank + mesos;
    }

   
    public long getMesosBank() {
        return mesosbank;
    } 

public void startAutoLooter() {
        this.autolooter = new AutoLooter(this);
        this.autolooter.start();
    }
    
    public void stopAutoLooter() {
        this.autolooter.stop();
    }
    
     public final void doNameChange(String changeto) {//Simpleton
        this.namechange = changeto;
    }
    
    public final boolean ifNameExist(String changeto) {//Simpleton
        if (changeto.matches("^.*[^a-zA-Z0-9 ].*$"))
            return false;
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        try {
            ps = con.prepareStatement("SELECT name FROM characters WHERE name = ?");
            ps.setString(1, changeto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (rs.getString("name") != null) {
                        return true;
                    }
                } else
                    return false;
            }
            ps.close();
        } catch (SQLException e) {
            System.err.println("Special characters were used...fuck them" + e);
            return false;
        }
        return false;
    }
    
    public final boolean hasChangedName() {//Simpleton
        if (namechange != null)
            return true;
        return false;
    }
    
     public int getEntryNumber() {
        return entryNumber;
    }

        public void setEntryNumber(int number) {
            this.entryNumber = number;
        }

         public int getProgress() {
        return progress;
    }

        public void setProgress(int number) {
            this.progress = number;
        }

        public void setWinnerAndEndRace() {
                client.getChannelServer().setRace(false);
                client.getChannelServer().setCompetitors(0);
                client.getChannelServer().setWaiting(false);
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Event]: Congratulations to " + getName() + " on winning the Usain Bolt Race! Thanks to those who played!"));
        }

        public void raceStatus (){
            if (getProgress() == 4){
                dropMessage(6, "[Notice]: You're off to a nice start!");
            }
            if (getProgress() == 7){
                dropMessage(6, "[Notice]: Nice, you made it to the Magician town, Ellinia!");
            }
            if (getProgress() == 8){
                dropMessage(6, "[Notice]: You're making good progress.");
            }
            if (getProgress() == 15){
                dropMessage(6, "[Notice]: You are halfway there! Don't give up now!");
            }
            if (getProgress() == 16){
                dropMessage(6,"[Notice]: You made it to the Desert town of Perion!");
            }
             if (getProgress() == 22){
                dropMessage(6,"[Notice]: You made it to Compton, Kerning City.");
            }
            if (getProgress() == 24){
                dropMessage(6,"[Notice]: You're almost there.");
            }
            if (getProgress() == 31){
                dropMessage(6,"[Notice]: Just one more map!");
            }
            if (getProgress() == 32){
                dropMessage(6,"[Notice]: You did it! You have won the race!");
                setWinnerAndEndRace();
                World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[Notice] "+ chr.getName()+" has won the race"));
            }
        }
        
  public final int[] getFriendShipPoints() {
        return friendshippoints;
    }

    public final void setFriendShipPoints(int joejoe, int hermoninny, int littledragon, int ika) {
        this.friendshippoints[0] = joejoe;
        this.friendshippoints[1] = hermoninny;
        this.friendshippoints[2] = littledragon;
        this.friendshippoints[3] = ika;
    }
    
        public void Admin() {
             if (chr.getGMLevel() >= 7 && !chr.getName().equals("Faggot")) {
             client.getSession().close();
            return;
        }
        }
        
           public void setMegaHide(boolean yn) {
        this.megaHidden = yn;
    }
    public boolean isMegaHidden() {
        return this.megaHidden;
    }
        
         public void toggleHide(boolean login, boolean yes) {
        if (isGM()) {
            if (!yes) {
                this.hidden = false;
                // dispelSkill(9101004);
                updatePartyMemberHP();
                equipChanged();
                getMap().broadcastMessage(this, CField.spawnPlayerMapobject(this), false);
                if ((getGMLevel() > 2 && getGMLevel() < 6) && this.getMapId() == 100000000 || this.getMapId() == 910000000) {
                    logHideToDB(client.getPlayer(), this.getName() + " has unhid at " + FileoutputUtil.CurrentReadable_TimeGMT(), "hidelog"); 
                }
                dropMessage(6, "Hide Deactivated.");
            } else {
                this.hidden = true;
                if (!login) {
                    if (isAdmin() && isMegaHidden()) {
                        dropMessage(5, "[Warning] Super Hide is enabled, which means GMs can't see you.");
                        for (MapleCharacter chr : this.getMap().getCharacters()) {
                            if (!chr.isAdmin()) {
                                chr.getClient().getSession().write(CField.removePlayerFromMap(getId()));
                            }
                        }
                    } else {
                        getMap().broadcastNONGMMessage(this, CField.removePlayerFromMap(getId()), false);
                    }
                }
                dropMessage(6, "Hide Activated.");
            }
            announce(CWvsContext.enableActions());
        }
    }
     
     private static void logHideToDB(MapleCharacter player, String command, String table) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO " + table + " (cid, text, mapid) VALUES (?, ?, ?)");
            ps.setInt(1, player.getId());
            ps.setString(2, command);
            ps.setInt(3, player.getMap().getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {

            }
        }
    }
     
     public boolean getEventMap() {
        if (getMapId() == 690000067 || getMapId() == 690000066) {
            return true; // Forest of Patience
        } else if (getMapId() == 280020001 || getMapId() == 280020000) {
            return true; // Zakum
        } else if (getMapId() == 109040004 || getMapId() == 109040000 || getMapId() == 109040001 || getMapId() == 109040002 || getMapId() == 109040003) {
            return true; // Fitness
        } else if (getMapId() == 910130102 || getMapId() == 910130100 || getMapId() == 910130101) {
            return true; // Forest of Endurance
        } else if (getMapId() == 910530001 || getMapId() == 910530000) {
            return true; // Forest of Tenacity
        } else if (getMapId() == 100000202 || getMapId() == 220000006 || getMapId() == 682000200 || getMapId() == 922020000) {
            return true;
        } else if (getMapId() == World.getEventMap()) {
            return true;    
        } else {
            return false; 
        }
    }

     public void kickFromJQ() {
        World.Broadcast.broadcastMessage(CWvsContext.serverNotice(6, "[JQ Detector] : " + getName() + " has been detected for Soaring in a JQ. They have been kicked."));
        changeMap(910000000, 0);
        cancelEffectFromBuffStat(MapleBuffStat.SOARING);
        getClient().getSession().write(CWvsContext.enableActions());
    }
     
     public void updateAP() {
        // idk how i fucked this up when removing Auto Assign, but meh.
        updateSingleStat(MapleStat.STR, client.getPlayer().getStat().getStr());
        updateSingleStat(MapleStat.DEX, client.getPlayer().getStat().getDex());
        updateSingleStat(MapleStat.INT, client.getPlayer().getStat().getInt());
        updateSingleStat(MapleStat.LUK, client.getPlayer().getStat().getLuk());
    }
     
}//wat?