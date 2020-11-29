package server; 

import client.SkillFactory; 
import client.inventory.MapleInventoryIdentifier; 
import console.consoleStart; 
import constants.BattleConstants; 
import constants.ServerConstants; 
import database.DatabaseConnection; 
import handling.MapleServerHandler; 
import handling.cashshop.CashShopServer; 
import handling.channel.ChannelServer;  
import handling.channel.MapleGuildRanking; 
import handling.login.LoginInformationProvider; 
import handling.login.LoginServer; 
import handling.world.World; 
import handling.world.family.MapleFamily; 
import handling.world.guild.MapleGuild; 
import java.sql.Connection;
import java.sql.PreparedStatement; 
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import server.Timer.*; 
import server.Timer.BuffTimer; 
import server.Timer.CheatTimer; 
import server.Timer.CloneTimer; 
import server.Timer.EtcTimer; 
import server.Timer.EventTimer; 
import server.Timer.MapTimer; 
import server.Timer.PingTimer; 
import server.Timer.WorldTimer;
import server.events.MapleOxQuizFactory;
import server.life.AbstractLoadedMapleLife;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;

public class Start { 
    

    public static long startTime = System.currentTimeMillis(); 
    public static final Start instance = new Start(); 
    public static final consoleStart instances = new consoleStart();
    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0); 

    public void run() throws InterruptedException {
        
        

        if (Boolean.parseBoolean(ServerProperties.getProperty("net.sf.odinms.world.admin")) || ServerConstants.Use_Localhost) { 
            ServerConstants.Use_Fixed_IV = false; 
            System.out.println("[!!! Admin Only Mode Active !!!]"); 
        } 

        try { 
            final PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0"); 
            ps.executeUpdate(); 
            ps.close(); 
        } catch (SQLException ex) { 
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active."); 
        } 
        System.out.println("[" + ServerProperties.getProperty("net.sf.odinms.login.serverName") + "] " + ServerConstants.Update_Name + " " + ServerConstants.Update_Version + " " + ServerConstants.Update_Revision +  "| TEST_BUILD" ); 
        System.out.println("[AlmightyMS] Console Intializing...");
        System.out.println("[AlmightyMS] Console Initialized. Begin loading...");
        System.out.println("Loading Timers...");
        World.init();
        System.out.println("World initialized...");
        System.out.println("Starting timers...");
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        CloneTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();
        
        System.out.println("Timers started...");
        System.out.println("Loading guilds...");
        MapleGuildRanking.getInstance().load();
        MapleGuild.loadAll(); //(this); 
        System.out.println("Guilds loaded...");
        System.out.println("Loading families...");
        MapleFamily.loadAll(); //(this); 
        System.out.println("Families loaded...");
        System.out.println("Initializing quests...");
        MapleLifeFactory.loadQuestCounts();
        MapleQuest.initQuests();
        System.out.println("Quests initialized...");
        System.out.println("Preparing ItemInformationProvider...");
        MapleItemInformationProvider.getInstance().runEtc(); 
        System.out.println("ItemInformationProvider initialized...");
        System.out.println("Preparing MonsterInformationProvider...");
        MapleMonsterInformationProvider.getInstance().load(); 
        BattleConstants.init(); 
        System.out.println("MapleMonsterInformationProvider ready...");
        System.out.println("Preparing MapleItemInformationProvider...");
        MapleItemInformationProvider.getInstance().runItems(); 
        System.out.println("MapleItemInformationProvider ready...");
        System.out.println("Loading skills...");
        SkillFactory.load();
        System.out.println("Skills loaded...");
        System.out.println("Loading miscellaneous...");
        LoginInformationProvider.getInstance();
        RandomRewards.load();
        MapleOxQuizFactory.getInstance();
        MapleCarnivalFactory.getInstance();
        CharacterCardFactory.getInstance().initialize(); 
        MobSkillFactory.getInstance();
        SpeedRunner.loadSpeedRuns();
        MTSStorage.load();
        System.out.println("Miscellaneous loaded...");
        System.out.println("Preparing custom life...");
        MapleInventoryIdentifier.getInstance();
        MapleMapFactory.loadCustomLife();
        System.out.println("Custom life loaded...");
        System.out.println("Preparing Cash Shop...");
        CashItemFactory.getInstance().initialize(); 
        MapleServerHandler.initiate();
        System.out.println("[Loading Login Server...]");
        LoginServer.run_startup_configurations();
        System.out.println("[LoginServer initialized and listening...]");

        System.out.println("[Loading Channel Server...]");
        ChannelServer.startChannel_Main();
        System.out.println("[All Channels initialized and listening...]");
        System.out.println("[Torment Mode activated Channels 11-17...]");
        System.out.println("[Loading CashShop Server...]");
        CashShopServer.run_startup_configurations();
        System.out.println("[CS initialized and listening...]");
        System.out.println("Finalizing startup tasks...");
        CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000);
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        World.registerRespawn();
        //ChannelServer.getInstance(1).getMapFactory().getMap(910000000).spawnRandDrop(); //start it off
        ShutdownServer.registerMBean();
        //ServerConstants.registerMBean();
        PlayerNPC.loadAll();// touch - so we see database problems early...
        MapleMonsterInformationProvider.getInstance().addExtra();
        LoginServer.setOn(); //now or later
        
        RankingWorker.run();
        System.out.print("Initializing Auto Save, AzwanDailyCheck, and Ardentmill reset timers...");
        World.runAutoSave();
        World.runAzwanDailyCheck();
        World.runArdentmillReset();
        System.out.println("Initialized...");
        System.out.println("Starting Packet Logging...");

 

    
        
       if (Boolean.parseBoolean(ServerProperties.getProperty("net.sf.odinms.world.logpackets"))) {
           System.out.println("Packet Logging Initialized...");
      }
        
        System.out.println("[Buffering Complete...]");
        //System.out.println("[AlmightyMS] Packets are NOT being logged...");
        System.out.println("[AlmightyMS] RumShield Online...]");
        System.out.println("[AlmightyMS Fully Initialized in " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds]");
        System.out.println("[AlmightyMS Initialized, All servers (-1) ONLINE. Welcome to AlmightyMS "+ ServerConstants.Update_Name + " " + ServerConstants.Update_Version + " " + ServerConstants.Update_Revision + "]");
    } 

    public static class Shutdown implements Runnable { 

        @Override 
        public void run() { 
            ShutdownServer.getInstance().run(); 
            ShutdownServer.getInstance().run(); 
        } 
    } 

   public static void main(final String args[]) throws InterruptedException {
        instance.run();
        instances.run();
    }
}  

