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
package constants;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

public class ServerConstants {

    public static boolean TESPIA = false; // true = uses GMS test server, for MSEA it does nothing though
     public static final byte[] Gateway_IP = new byte[]{(byte) 8, (byte) 31, (byte) 98, (byte) 53};
    public static boolean Use_Fixed_IV;
    
      //QUEST CONSTANTS
    public static final int Q_MAXLEVEL = 2000001;
    public static final int Q_GMDELAY = 2000002;
    
    
        public static final List<Integer> instancedDropMobs = Collections.unmodifiableList(Arrays.asList(
        8850011,8850005,8850006,8850007,8850008,8850009,8850010, //empress
        8860000,8860001, //arkarium
        8800002,8810018,8820001,8800102,8810122, //HT, zakum, PB, and chaos counterparts
        8870000,8870100, //hilla
        8840000,8500002, //VL, pap
        9420549,9420544, //scar & targa
        7220005,8220010,8220011,8220012,8220015 //neo city
    ));
     

    public static final byte Class_Bonus_EXP(final int job) {
        switch (job) {
            case 501:
            case 530:
            case 531:
            case 532:
            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
            case 800:
            case 900:
            case 910:
                return 10;
        }
        return 0;
    }
    
    public static boolean getEventTime() {
        int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        switch (Calendar.DAY_OF_WEEK) {
            case 1:
                return time >= 1 && time <= 5;
            case 2:
                return time >= 4 && time <= 9;
            case 3:
                return time >= 7 && time <= 12;
            case 4:
                return time >= 10 && time <= 15;
            case 5:
                return time >= 13 && time <= 18;
            case 6:
                return time >= 16 && time <= 21;
         }
        return time >= 19 && time <= 24;
    }

    // Start of Poll
    public static final boolean PollEnabled = false;
    public static final String Poll_Question = "Are you mudkiz?";
    public static final String[] Poll_Answers = {"test1", "test2", "test3"};
    // End of Poll
    public static final short MAPLE_VERSION = (short) 117; //maple version
    public static final String MAPLE_PATCH = "2"; //patch revision
    public static final String Update_Name = "Prepare to Die";//server update name
    public static final String Update_Version = "1.6.0.0";//server update
    public static final String Update_Revision = "REV 10";//server revision
    public static final boolean BLOCK_CS = false;
    public static final boolean CASHSHOPSTATUS = true;
    public static boolean Use_Localhost = false; // true = packets are logged, false = others can connect to server
    public static final int MIN_MTS = 100; //lowest amount an item can be, GMS = 110
    public static final int MTS_BASE = 0; //+amount to everything, GMS = 500, MSEA = 1000
    public static final int MTS_TAX = 5; //+% to everything, GMS = 10
    public static final int MTS_MESO = 10000; //mesos needed, GMS = 5000
    public static final boolean TRIPLE_TRIO = true;
    public static final int CURRENCY = 4001024; //maybe chg to something else
    public static final String FM_BGM = "Bgm03/Elfwood"; //music for FM
    public static final String SQL_USER = "root", SQL_PASSWORD = ""; //user / pass
    public static final long number3 = 202227478981090217L;
    public static final int[] BLOCKED_CS_ITEMS = {};   
    public static final boolean ALLOW_UNDROPPABLE_DROP = true; //
    public static final String vote1 = "http://www.gtop100.com/in.php?site=82861";
    public static final String vote2 = "http://www.ultimateprivateservers.com/maple-story/index.php?a=in&u=Sylux";
    public static final int number1 = (142449577 + 753356065 + 611816275);
    public static final short number2 = 18773, updateNumber = 18774, linkNumber = 18775, messageNumber = 18776;  
    
       
    public static enum PlayerGMRank {   
        NORMAL('@', 0),
        DONATOR('^', 1),
        SUPERDONATOR('$', 2),
        INTERN('%', 3),
        GM('!', 4),
        SUPERGM('!', 5),
        HEADGM('!', 6),
        ADMIN('!', 7);
        private char commandPrefix;
        private int level;

        PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }
        
        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }
    
     public static enum CommandType {

        NORMAL(0),
        TRADE(1),
        POKEMON(2);
        private int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }
}
