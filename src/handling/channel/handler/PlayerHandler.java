 package handling.channel.handler;
 
 import client.MapleBuffStat;
 import client.MapleCharacter;
import client.MapleCharacterUtil;

 import client.MapleClient;
 import client.MapleJob;
 import client.PlayerStats;
 import client.Skill;
 import client.SkillFactory;
 import client.SkillMacro;
 import client.anticheat.CheatingOffense;
import client.inventory.Equip;
 import client.inventory.Item;
 import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
 import client.status.MonsterStatus;
 import client.status.MonsterStatusEffect;
 import constants.GameConstants;
 import handling.channel.ChannelServer;
import handling.world.World;
           import java.lang.ref.WeakReference;
 import java.awt.Point;
 import java.util.List;
import scripting.NPCConversationManager;
           import server.Timer.CloneTimer;
 import server.MapleInventoryManipulator;
 import server.MapleItemInformationProvider;
 import server.MaplePortal;
 import server.MapleStatEffect;
 import server.Randomizer;
 import server.events.MapleEvent;
 import server.events.MapleEventType;
 import server.events.MapleSnowball.MapleSnowballs;
 import server.life.MapleMonster;
 import server.life.MobAttackInfo;
 import server.life.MobSkill;
 import server.life.MobSkillFactory;
 import server.maps.FieldLimitType;
 import server.maps.MapleMap;
import server.movement.LifeMovementFragment;
 import server.quest.MapleQuest;
 import tools.FileoutputUtil;
 import tools.Pair;
 import tools.data.LittleEndianAccessor;
 import tools.packet.CField;
 import tools.packet.CWvsContext;
 import tools.packet.MTSCSPacket;
 import tools.packet.MobPacket;
 
 public class PlayerHandler
 {
   public static int isFinisher(int skillid)
   {
/*   68 */     switch (skillid) {
     case 1111003:
/*   70 */       return GameConstants.GMS ? 1 : 10;
     case 1111005:
/*   72 */       return GameConstants.GMS ? 2 : 10;
     case 11111002:
/*   74 */       return GameConstants.GMS ? 1 : 10;
     case 11111003:
/*   76 */       return GameConstants.GMS ? 2 : 10;
     }
/*   78 */     return 0;
   }
 
   public static void ChangeSkillMacro(LittleEndianAccessor slea, MapleCharacter chr) {
/*   82 */     int num = slea.readByte();
 
/*   87 */     for (int i = 0; i < num; i++) {
/*   88 */       String name = slea.readMapleAsciiString();
/*   89 */       int shout = slea.readByte();
/*   90 */       int skill1 = slea.readInt();
/*   91 */       int skill2 = slea.readInt();
/*   92 */       int skill3 = slea.readInt();
 
/*   94 */       SkillMacro macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
/*   95 */       chr.updateMacros(i, macro);
     }
   }
 
   public static final void ChangeKeymap(LittleEndianAccessor slea, MapleCharacter chr) {
/*  100 */     if ((slea.available() > 8L) && (chr != null)) {
/*  101 */       slea.skip(4);
/*  102 */       int numChanges = slea.readInt();
 
/*  104 */       for (int i = 0; i < numChanges; i++) {
/*  105 */         int key = slea.readInt();
/*  106 */         byte type = slea.readByte();
/*  107 */         int action = slea.readInt();
/*  108 */         if ((type == 1) && (action >= 1000)) {
/*  109 */           Skill skil = SkillFactory.getSkill(action);
/*  110 */           if ((skil != null) && (
/*  111 */             ((!skil.isFourthJob()) && (!skil.isBeginnerSkill()) && (skil.isInvisible()) && (chr.getSkillLevel(skil) <= 0)) || (GameConstants.isLinkedAranSkill(action)) || (action % 10000 < 1000) || (action >= 91000000)))
           {
             continue;
           }
         }
/*  116 */         chr.changeKeybinding(key, type, action);
       }
/*  118 */     } else if (chr != null) {
/*  119 */       int type = slea.readInt(); int data = slea.readInt();
/*  120 */       switch (type) {
       case 1:
/*  122 */         if (data <= 0)
/*  123 */           chr.getQuestRemove(MapleQuest.getInstance(122221));
         else {
/*  125 */           chr.getQuestNAdd(MapleQuest.getInstance(122221)).setCustomData(String.valueOf(data));
         }
/*  127 */         break;
       case 2:
/*  129 */         if (data <= 0)
/*  130 */           chr.getQuestRemove(MapleQuest.getInstance(122223));
         else
/*  132 */           chr.getQuestNAdd(MapleQuest.getInstance(122223)).setCustomData(String.valueOf(data));
       }
     }
   }
 
   public static final void UseTitle(int itemId, MapleClient c, MapleCharacter chr)
   {
/*  140 */     if ((chr == null) || (chr.getMap() == null)) {
/*  141 */       return;
     }
/*  143 */     Item toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);
/*  144 */     if (toUse == null) {
/*  145 */       return;
     }
/*  147 */     if (itemId <= 0)
/*  148 */       chr.getQuestRemove(MapleQuest.getInstance(124000));
     else {
/*  150 */       chr.getQuestNAdd(MapleQuest.getInstance(124000)).setCustomData(String.valueOf(itemId));
     }
/*  152 */     chr.getMap().broadcastMessage(chr, CField.showTitle(chr.getId(), itemId), false);
/*  153 */     c.getSession().write(CWvsContext.enableActions());
   }
 
    public static final void UseChair(final int itemId, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        final Item toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);
        if (toUse == null) {
            chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(itemId));
            return;
        }
        if (GameConstants.isFishingMap(chr.getMapId()) && itemId == 3011000) {
            chr.startFishingTask();
        }
        chr.setChair(itemId);
        chr.getMap().broadcastMessage(chr, CField.showChair(chr.getId(), itemId), false);
        c.getSession().write(CWvsContext.enableActions());
    }
 
   public static final void CancelChair(short id, MapleClient c, MapleCharacter chr) {
/*  176 */     if (id == -1) {
/*  177 */       chr.cancelFishingTask();
/*  178 */       chr.setChair(0);
/*  179 */       c.getSession().write(CField.cancelChair(-1));
/*  180 */       if (chr.getMap() != null)
/*  181 */         chr.getMap().broadcastMessage(chr, CField.showChair(chr.getId(), 0), false);
     }
     else {
/*  184 */       chr.setChair(id);
/*  185 */       c.getSession().write(CField.cancelChair(id));
     }
   }
 
    public static void TrockAddMap(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte addrem = slea.readByte();
        final byte vip = slea.readByte();

        if (vip == 1) { // Regular rocks
            if (addrem == 0) {
                chr.deleteFromRegRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRegRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        } else if (vip == 2) { // VIP Rock
            if (addrem == 0) {
                chr.deleteFromRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        } else if (vip == 3) { // Hyper Rocks
            if (addrem == 0) {
                chr.deleteFromHyperRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addHyperRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        }
        c.getSession().write(MTSCSPacket.OnMapTransferResult(chr, vip, addrem == 0));
    }
 
   public static final void CharInfoRequest(int objectid, MapleClient c, MapleCharacter chr) {
/*  228 */     if ((c.getPlayer() == null) || (c.getPlayer().getMap() == null)) {
/*  229 */       return;
     }
/*  231 */     MapleCharacter player = c.getPlayer().getMap().getCharacterById(objectid);
/*  232 */     c.getSession().write(CWvsContext.enableActions());
/*  233 */     if ((player != null) && (
/*  234 */       (!player.isGM()) || (c.getPlayer().isGM())))
/*  235 */       c.getSession().write(CWvsContext.charInfo(player, c.getPlayer().getId() == objectid));
   }
 
   public static final void TakeDamage(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {

        slea.skip(4);
        chr.updateTick(slea.readInt());
        byte type = slea.readByte();
        slea.skip(1);
        int damage = slea.readInt();
        slea.skip(2);
        boolean isDeadlyAttack = false;
        boolean pPhysical = false;
        int oid = 0;
        int monsteridfrom = 0;
        int fake = 0;
        int mpattack = 0;
        int skillid = 0;
        int pID = 0;
        int pDMG = 0;
        byte direction = 0;
        byte pType = 0;
        Point pPos = new Point(0, 0);
        MapleMonster attacker = null;
        if ((chr == null) || (chr.isHidden()) || (chr.getMap() == null)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if ((chr.isGM()) && (chr.isInvincible())) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        PlayerStats stats = chr.getStat();
        if ((type != -2) && (type != -3) && (type != -4)) {
            monsteridfrom = slea.readInt();
            oid = slea.readInt();
            attacker = chr.getMap().getMonsterByOid(oid);
            direction = slea.readByte();

            if ((attacker == null) || (attacker.getId() != monsteridfrom) || (attacker.getLinkCID() > 0) || (attacker.isFake()) || (attacker.getStats().isFriendly())) {
                return;
            }
            if (chr.getMapId() == 915000300) {
                MapleMap to = chr.getClient().getChannelServer().getMapFactory().getMap(915000200);
                chr.dropMessage(5, "You've been found out! Retreat!");
                chr.changeMap(to, to.getPortal(1));
                return;
            }
            if ((type != -1) && (damage > 0)) {
                MobAttackInfo attackInfo = attacker.getStats().getMobAttack(type);
                if (attackInfo != null) {
                    if ((attackInfo.isElement) && (stats.TER > 0)) {
                        damage *= (100 - stats.TER) / 100;
                        return;
                    }
                    if (attackInfo.isDeadlyAttack()) {
                        isDeadlyAttack = true;
                        mpattack = stats.getMp() - 1;
                    } else {
                        mpattack += attackInfo.getMpBurn();
                    }
                    MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
                    if ((skill != null) && ((damage == -1) || (damage > 0))) {
                        skill.applyEffect(chr, attacker, false);
                    }
                    attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                }
            }
            if (stats.DAMreduceR > 0 && damage > 1) {
                damage *= (100.0d - stats.DAMreduceR) / 100.0d;
            }
            skillid = slea.readInt();
            pDMG = slea.readInt();
            byte defType = slea.readByte();
            slea.skip(1);
            if (defType == 1) {
                Skill bx = SkillFactory.getSkill(31110008);
                int bof = chr.getTotalSkillLevel(bx);
                if (bof > 0) {
                    MapleStatEffect eff = bx.getEffect(bof);
                    chr.handleForceGain(oid, 31110008, eff.getZ());
                    int hpHeal = (int) (chr.getMaxHp() * (eff.getY() / 100.0d));
                    chr.healHP(hpHeal);
                }
            }
            if (skillid != 0) {
                pPhysical = slea.readByte() > 0;
                pID = slea.readInt();
                pType = slea.readByte();
                slea.skip(4);
                pPos = slea.readPos();
            }
        }
        if(chr.getBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL) != null){
            int attacksLeft = chr.getCData(chr, 2311009);
            if(attacksLeft <= 0){
                chr.cancelEffectFromBuffStat(MapleBuffStat.HOLY_MAGIC_SHELL);
            } else {
                chr.setCData(2311009, -1);
            }
        }
        if (damage == -1) {
            fake = 4020002 + (chr.getJob() / 10 - 40) * 100000;
            if ((fake != 4120002) && (fake != 4220002)) {
                fake = 4120002;
            }
            if ((type == -1) && (chr.getJob() == 122) && (attacker != null) && (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null)
                    && (chr.getTotalSkillLevel(1220006) > 0)) {
                MapleStatEffect eff = SkillFactory.getSkill(1220006).getEffect(chr.getTotalSkillLevel(1220006));
                attacker.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, Integer.valueOf(1), 1220006, null, false), false, eff.getDuration(), true, eff);
                fake = 1220006;
            }

            if (chr.getTotalSkillLevel(fake) <= 0) {
                return;
            }
        } else if ((damage < -1) || (damage > 500000)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if ((pPhysical) && (skillid == 1201007) && (chr.getTotalSkillLevel(1201007) > 0)) {
            damage -= pDMG;
            if (damage > 0) {
                MapleStatEffect eff = SkillFactory.getSkill(1201007).getEffect(chr.getTotalSkillLevel(1201007));
                long enemyDMG = Math.min(damage * (eff.getY() / 100), attacker.getMobMaxHp() / 2L);
                if (enemyDMG > pDMG) {
                    enemyDMG = pDMG;
                }
                if (enemyDMG > 1000L) {
                    enemyDMG = 1000L;
                }
                attacker.damage(chr, enemyDMG, true, 1201007);
            } else {
                damage = 1;
            }
        }
        chr.getCheatTracker().checkTakeDamage(damage);
        //TODO: Damage can desync here
        Pair modify = chr.modifyDamageTaken(damage, attacker, !pPhysical);
        damage = ((Double) modify.left).intValue();
        if (damage > 0) {
            chr.getCheatTracker().setAttacksWithoutHit(false);

            if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
                chr.cancelMorphs();
            }

            boolean mpAttack = (chr.getBuffedValue(MapleBuffStat.MECH_CHANGE) != null) && (chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != 35121005);
            if (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                int hploss = 0;
                int mploss = 0;
                if (isDeadlyAttack) {
                    if (stats.getHp() > 1) {
                        hploss = stats.getHp() - 1;
                    }
                    if (stats.getMp() > 1) {
                        mploss = stats.getMp() - 1;
                    }
                    if (chr.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                        mploss = 0;
                    }
                  /*  if (chr.getHcMode() == 1) { //Nightmare characters should be immune from 1/1 attacks, at least partially.
                        hploss = (int) GameConstants.handleHealthGate(chr, hploss);
                    } */
                    chr.addMPHP(-hploss, -mploss);
                } else {
                    mploss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0D)) + mpattack;
                    hploss = damage - mploss;
                    if (chr.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                        mploss = 0;
                    } else if (mploss > stats.getMp()) {
                        mploss = stats.getMp();
                        hploss = damage - mploss + mpattack;
                       // hploss = (int) GameConstants.handleHealthGate(chr, hploss);
                    }
                    chr.addMPHP(-hploss, -mploss);
                }
            } else if (chr.getBuffedValue(MapleBuffStat.MESOGUARD) != null) {
                int mesoloss = (int) (damage * (chr.getStat().mesoGuardMeso / 100.0D));
                damage = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MESOGUARD) / 100.0d));
                if (chr.getMeso() < mesoloss) {
                    chr.gainMeso(-chr.getMeso(), false);
                    chr.cancelBuffStats(new MapleBuffStat[]{MapleBuffStat.MESOGUARD});
                } else {
                    chr.gainMeso(-mesoloss, false);
                }
                if ((isDeadlyAttack) && (stats.getMp() > 1)) {
                    mpattack = stats.getMp() - 1;
                }
                chr.addMPHP(-damage, -mpattack);
            } else if (isDeadlyAttack) {
                int hploss = stats.getHp() > 1 ? -(stats.getHp() - 1) : 0;
              /*  if (chr.getHcMode() == 1) {
                    hploss = (int) GameConstants.handleHealthGate(chr, hploss);
                } */
                chr.addMPHP(hploss, (stats.getMp() > 1) && (!mpAttack) ? -(stats.getMp() - 1) : 0);
            } else {
                chr.addMPHP(-damage, mpAttack ? 0 : -mpattack);
            }

            if (!GameConstants.GMS) {
                chr.handleBattleshipHP(-damage);
            }
            if ((chr.inPVP()) && (chr.getStat().getHPPercent() <= 20)) {
                chr.getStat();
                SkillFactory.getSkill(PlayerStats.getSkillByJob(93, chr.getJob())).getEffect(1).applyTo(chr);
            }
        }
        if (damage == 0 && chr.getSkillLevel(4330009) > 0)//Shadowmeld 
        {
            SkillFactory.getSkill(4330009).getEffect(chr.getSkillLevel(4330009)).applyTo(chr);
        }

        byte offset = 0;
        int offset_d = 0;
        if (slea.available() == 1L) {
            offset = slea.readByte();
            if ((offset == 1) && (slea.available() >= 4L)) {
                offset_d = slea.readInt();
            }
            if ((offset < 0) || (offset > 2)) {
                offset = 0;
            }
        }

        chr.getMap().broadcastMessage(chr, CField.damagePlayer(chr.getId(), type, damage, monsteridfrom, direction, skillid, pDMG, pPhysical, pID, pType, pPos, offset, offset_d, fake), false);

    }
 
  public static final void AranCombo(MapleClient c, MapleCharacter chr, int toAdd) {
        if ((chr != null) && (chr.getJob() >= 2000) && (chr.getJob() <= 2112)) {
            short combo = chr.getCombo();
            long curr = System.currentTimeMillis();

            if ((combo > 0) && (curr - chr.getLastCombo() > 7000L)) {
                combo = 0;
            }
            combo = (short) Math.min(30000, combo + toAdd);
            chr.setLastCombo(curr);
            chr.setCombo(combo);

            c.getSession().write(CField.testCombo(combo));

            switch (combo) {
                case 10:
                case 20:
                case 30:
                case 40:
                case 50:
                case 60:
                case 70:
                case 80:
                case 90:
                case 100:
                    if (chr.getSkillLevel(21000000) < combo / 10) {
                        break;
                    }
                    SkillFactory.getSkill(21000000).getEffect(combo / 10).applyComboBuff(chr, combo);
            }
        }
    }
 
  public static final void UseItemEffect(int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = chr.getInventory((itemId == 4290001) || (itemId == 4290000) ? MapleInventoryType.ETC : MapleInventoryType.CASH).findById(itemId);
        if ((toUse == null) || (toUse.getItemId() != itemId) || (toUse.getQuantity() < 1)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (itemId != 5510000) {
            chr.setItemEffect(itemId);
        }
        chr.getMap().broadcastMessage(chr, CField.itemEffect(chr.getId(), itemId), false);
    }

    public static final void CancelItemEffect(int id, MapleCharacter chr) {
        if(id <= -2257000 && id >= -2257999){
            return;
        }
        chr.cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(-id), false, -1L);
    }

    public static final void CancelBuffHandler(int sourceid, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        if (sourceid == 24121005) {
            return;
        }
        Skill skill = SkillFactory.getSkill(sourceid);

        if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0L);
            chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, sourceid), false);
        } else {
            if (sourceid == 35001002) {
                if (chr.getTotalSkillLevel(35120000) <= 0) {
                    int bufftoGive = 2259120 + chr.getTotalSkillLevel(35001002);
                    chr.cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(bufftoGive), true, -1L);
                } else {
                    int bufftoGive = 2259131 + chr.getTotalSkillLevel(35120000);
                    chr.cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(bufftoGive), true, -1L);
                }
            }
            chr.cancelEffect(skill.getEffect(1), false, -1L);
        }
    }

    public static final void CancelMech(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        int sourceid = slea.readInt();
        if ((sourceid % 10000 < 1000) && (SkillFactory.getSkill(sourceid) == null)) {
            sourceid += 1000;
        }
        Skill skill = SkillFactory.getSkill(sourceid);
        if (skill == null) {
            return;
        }
        if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0L);
            chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, sourceid), false);
        } else {
            chr.cancelEffect(skill.getEffect(slea.readByte()), false, -1L);
        }
    }
 
   public static final void QuickSlot(LittleEndianAccessor slea, MapleCharacter chr) {
/*  543 */     if ((slea.available() == 32L) && (chr != null)) {
/*  544 */       StringBuilder ret = new StringBuilder();
/*  545 */       for (int i = 0; i < 8; i++) {
/*  546 */         ret.append(slea.readInt()).append(",");
       }
/*  548 */       ret.deleteCharAt(ret.length() - 1);
/*  549 */       chr.getQuestNAdd(MapleQuest.getInstance(123000)).setCustomData(ret.toString());
     }
   }
 
   public static final void SkillEffect(LittleEndianAccessor slea, MapleCharacter chr) {
/*  554 */     int skillId = slea.readInt();
/*  555 */     if (skillId >= 91000000) {
/*  556 */       chr.getClient().getSession().write(CWvsContext.enableActions());
/*  557 */       return;
     }
/*  559 */     byte level = slea.readByte();
/*  560 */     short direction = slea.readShort();
/*  561 */     byte unk = slea.readByte();
 
/*  563 */     Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(skillId));
/*  564 */     if ((chr == null) || (skill == null) || (chr.getMap() == null)) {
/*  565 */       return;
     }
/*  567 */     int skilllevel_serv = chr.getTotalSkillLevel(skill);
 
/*  569 */     if ((skilllevel_serv > 0) && (skilllevel_serv == level) && ((skillId == 33101005) || (skill.isChargeSkill()))) {
/*  570 */       chr.setKeyDownSkill_Time(System.currentTimeMillis());
/*  571 */       if (skillId == 33101005) {
/*  572 */         chr.setLinkMid(slea.readInt(), 0);
       }
/*  574 */       chr.getMap().broadcastMessage(chr, CField.skillEffect(chr, skillId, level, direction, unk), false);
     }
   }
 
   public static final void SpecialMove(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.hasBlockedInventory()) || (chr.getMap() == null) || (slea.available() < 9L)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        slea.skip(4);
        int skillid = slea.readInt();

        if (skillid == 5211011 || skillid == 5211015 || skillid == 5211016) {
            chr.dispelSummons();
            int mobToSummon = Randomizer.nextInt(3); //0 for muirhat, 1 for valerie, 2 for jack
            if (mobToSummon == 1) {
                skillid = 5211015;
            } else if (mobToSummon == 2) {
                skillid = 5211016;
            } else {
                skillid = 5211011;
            }
        }
        if (skillid >= 91000000) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (skillid == 23111008) {
            skillid += Randomizer.nextInt(2);
        }
        int skillLevel = slea.readByte();
        Skill skill = SkillFactory.getSkill(skillid);
        if ((skill == null) || ((GameConstants.isAngel(skillid)) && (chr.getStat().equippedSummon % 10000 != skillid % 10000)) || ((chr.inPVP()) && (skill.isPVPDisabled()))) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int levelCheckSkill = 0;
        if ((GameConstants.isPhantom(chr.getJob())) && (!MapleJob.getById(skillid / 10000).isPhantom())) {
            int skillJob = skillid / 10000;
            if (skillJob % 100 == 0) {
                levelCheckSkill = 24001001;
            } else if (skillJob % 10 == 0) {
                levelCheckSkill = 24101001;
            } else if (skillJob % 10 == 1) {
                levelCheckSkill = 24111001;
            } else {
                levelCheckSkill = 24121001;
            }
        }

        if ((levelCheckSkill == 0)) {
            if ((!GameConstants.isMulungSkill(skillid)) && (!GameConstants.isPyramidSkill(skillid)) && (GameConstants.isLinkedAranSkill(skillid))) {
                if (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) {
                    c.getSession().close();
                    return;
                }
            }
            if (GameConstants.isMulungSkill(skillid)) {
                if (chr.getMapId() / 10000 != 92502) {
                    return;
                }
                if (chr.getMulungEnergy() < 10000) {
                    return;
                }
                chr.mulung_EnergyModify(false);
            } else if ((GameConstants.isPyramidSkill(skillid)) && (chr.getMapId() / 10000 != 92602) && (chr.getMapId() / 10000 != 92601)) {
                return;
            }
        }
        if (GameConstants.isEventMap(chr.getMapId())) {
            for (MapleEventType t : MapleEventType.values()) {
                MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                if ((e.isRunning()) && (!chr.isGM())) {
                    for (int i : e.getType().mapids) {
                        if (chr.getMapId() == i) {
                            chr.dropMessage(5, "You may not use that here.");
                            return;
                        }
                    }
                }
            }
        }
        skillLevel = chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid));
        MapleStatEffect effect = chr.inPVP() ? skill.getPVPEffect(skillLevel) : skill.getEffect(skillLevel);
        if ((effect.isMPRecovery()) && (chr.getStat().getHp() < chr.getStat().getMaxHp() / 100 * 10)) {
            c.getPlayer().dropMessage(5, "You do not have the HP to use this skill.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if ((effect.getCooldown(chr) > 0) && (!chr.isGM())) {
            if (chr.skillisCooling(skillid)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
        }
        int mobID;
        MapleMonster mob;
        switch (skillid) {
            case 1121001:
            case 1221001:
            case 1321001:
            case 9001020:
            case 9101020:
            case 31111003:
                byte number_of_mobs = slea.readByte();
                slea.skip(3);
                for (int i = 0; i < number_of_mobs; i++) {
                    int mobId = slea.readInt();
                    mob = chr.getMap().getMonsterByOid(mobId);
                    if (mob == null) {
                        continue;
                    }
                    mob.switchController(chr, mob.isControllerHasAggro());
                    mob.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, Integer.valueOf(1), skillid, null, false), false, effect.getDuration(), true, effect);
                }
                chr.getMap().broadcastMessage(chr, CField.EffectPacket.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, slea.readByte()), chr.getTruePosition());
                c.getSession().write(CWvsContext.enableActions());
                break;
            case 30001061:
                mobID = slea.readInt();
                mob = chr.getMap().getMonsterByOid(mobID);
                if (mob != null) {
                    boolean success = (mob.getHp() <= mob.getMobMaxHp() / 2L) && (mob.getId() >= 9304000) && (mob.getId() < 9305000);
                    chr.getMap().broadcastMessage(chr, CField.EffectPacket.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, (byte) (success ? 1 : 0)), chr.getTruePosition());
                    if (success) {
                        chr.getQuestNAdd(MapleQuest.getInstance(111112)).setCustomData(String.valueOf((mob.getId() - 9303999) * 10));
                        chr.getMap().killMonster(mob, chr, true, false, (byte) 1);
                        chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                        c.getSession().write(CWvsContext.updateJaguar(chr));
                    } else {
                        chr.dropMessage(5, "The monster has too much physical strength, so you cannot catch it.");
                    }
                }
                c.getSession().write(CWvsContext.enableActions());
                break;
            case 30001062:
                chr.dropMessage(5, "No monsters can be summoned. Capture a monster first.");
                c.getSession().write(CWvsContext.enableActions());
                break;
            case 33101005:
                mobID = chr.getFirstLinkMid();
                mob = chr.getMap().getMonsterByOid(mobID);
                chr.setKeyDownSkill_Time(0L);
                chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, skillid), false);
                if (mob != null) {
                    boolean success = (mob.getStats().getLevel() < chr.getLevel()) && (mob.getId() < 9000000) && (!mob.getStats().isBoss());
                    if (success) {
                        chr.getMap().broadcastMessage(MobPacket.suckMonster(mob.getObjectId(), chr.getId()));
                        chr.getMap().killMonster(mob, chr, false, false, (byte) -1);
                    } else {
                        chr.dropMessage(5, "The monster has too much physical strength, so you cannot catch it.");
                    }
                } else {
                    chr.dropMessage(5, "No monster was sucked. The skill failed.");
                }
                c.getSession().write(CWvsContext.enableActions());
                break;
            case 4341003:
                chr.setKeyDownSkill_Time(0L);
                chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, skillid), false);
            default:
                Point pos = null;
                if ((slea.available() == 5L) || (slea.available() == 7L)) {
                    pos = slea.readPos();
                }
                if (effect.isMagicDoor()) {
                    if (!FieldLimitType.MysticDoor.check(chr.getMap().getFieldLimit())) {
                        effect.applyTo(c.getPlayer(), pos);
                    } else {
                        c.getSession().write(CWvsContext.enableActions());
                    }
                } else {
                    int mountid = MapleStatEffect.parseMountInfo(c.getPlayer(), skill.getId());
                    if ((mountid != 0) && (mountid != GameConstants.getMountItem(skill.getId(), c.getPlayer())) && (!c.getPlayer().isIntern()) && (c.getPlayer().getBuffedValue(MapleBuffStat.MONSTER_RIDING) == null)
                            && (c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -122) == null) && (!GameConstants.isMountItemAvailable(mountid, c.getPlayer().getJob()))) {
                        c.getSession().write(CWvsContext.enableActions());
                        return;
                    }
                    effect.applyTo(c.getPlayer(), pos);
                }
        }
    }
 
	 public static final void closeRangeAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, final boolean energy) {
        if (chr == null || (energy && chr.getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null && chr.getBuffedValue(MapleBuffStat.BODY_PRESSURE) == null && chr.getBuffedValue(MapleBuffStat.DARK_AURA) == null && chr.getBuffedValue(MapleBuffStat.TORNADO) == null && chr.getBuffedValue(MapleBuffStat.SUMMON) == null && chr.getBuffedValue(MapleBuffStat.RAINING_MINES) == null && chr.getBuffedValue(MapleBuffStat.TELEPORT_MASTERY) == null)) {
            return;
        }
        if (chr.hasBlockedInventory() || chr.getMap() == null) {
            return;
        }
        AttackInfo attack = DamageParse.parseDmgM(slea, chr);
        if (attack == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final boolean mirror = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null;
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage();
        final Item shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
        int attackCount = (shield != null && shield.getItemId() / 10000 == 134 ? 2 : 1);
        int skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;

        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
            if (skill == null || (GameConstants.isAngel(attack.skill) && (chr.getStat().equippedSummon % 10000) != (attack.skill % 10000))) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }
            if (GameConstants.isEventMap(chr.getMapId())) {
                for (MapleEventType t : MapleEventType.values()) {
                    final MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                    if (e.isRunning() && !chr.isGM()) {
                        for (int i : e.getType().mapids) {
                            if (chr.getMapId() == i) {
                                chr.dropMessage(5, "You may not use that here.");
                                return; //non-skill cannot use
                            }
                        }
                    }
                }
            }
            //Additional check to see if Advanced Dark Sight is active, if so, apply the additional DamageIncreases provided by PlayerStats->HandlePassiveSkills()
            if ((attack.skill / 10000 == 433) || (attack.skill / 10000 == 434)) {
                if (chr.getBuffedValue(MapleBuffStat.DARKSIGHT) != null) {
                    maxdamage *= (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0;
                } else {
                    maxdamage *= (effect.getDamage()) / 100.0;
                }

            } else {
                maxdamage *= (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0;
            }
            attackCount = effect.getAttackCount();
            if (chr.getJob() == 2412 && chr.getCardStack() == 40) {
                SkillFactory.getSkill(20031210).getEffect(1).applyTo(chr);
            }
            if (effect.getCooldown(chr) > 0 && !chr.isGM() && !energy) {
                if (chr.skillisCooling(attack.skill)) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                c.getSession().write(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
                chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
            }
        }
        attack = DamageParse.Modify_AttackCrit(attack, chr, 1, effect);
        attackCount *= (mirror ? 2 : 1);
        if (!energy) {
            if ((chr.getMapId() == 109060000 || chr.getMapId() == 109060002 || chr.getMapId() == 109060004) && attack.skill == 0) {
                MapleSnowballs.hitSnowball(chr);
            }
            // handle combo orbconsume
            int numFinisherOrbs = 0;
            final Integer comboBuff = chr.getBuffedValue(MapleBuffStat.COMBO);

            if (isFinisher(attack.skill) > 0) { // finisher
                if (comboBuff != null) {
                    numFinisherOrbs = comboBuff.intValue() - 1;
                }
                if (numFinisherOrbs <= 0) {
                    return;
                }
                chr.handleOrbconsume(isFinisher(attack.skill));
                if (!GameConstants.GMS) {
                    maxdamage *= numFinisherOrbs;
                }
            }
        }
        chr.checkFollow();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, CField.closeRangeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, energy, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk, attack.charge), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.closeRangeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, energy, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk, attack.charge), false);
        }
        DamageParse.applyAttack(attack, skill, c.getPlayer(), attackCount, maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);
        WeakReference<MapleCharacter>[] clones = chr.getClones();
        for (int i = 0; i < clones.length; i++) {
            if (clones[i].get() != null) {
                final MapleCharacter clone = clones[i].get();
                final Skill skil2 = skill;
                final int skillLevel2 = skillLevel;
                final int attackCount2 = attackCount;
                final double maxdamage2 = maxdamage;
                final MapleStatEffect eff2 = effect;
                final AttackInfo attack2 = DamageParse.DivideAttack(attack, chr.isGM() ? 1 : 4);
                CloneTimer.getInstance().schedule(new Runnable() {
                    public void run() {
                        if (!clone.isHidden()) {
                            clone.getMap().broadcastMessage(CField.closeRangeAttack(clone.getId(), attack2.tbyte, attack2.skill, skillLevel2, attack2.display, attack2.speed, attack2.allDamage, energy, clone.getLevel(), clone.getStat().passive_mastery(), attack2.unk, attack2.charge));
                        } else {
                            clone.getMap().broadcastGMMessage(clone, CField.closeRangeAttack(clone.getId(), attack2.tbyte, attack2.skill, skillLevel2, attack2.display, attack2.speed, attack2.allDamage, energy, clone.getLevel(), clone.getStat().passive_mastery(), attack2.unk, attack2.charge), false);
                        }
                        DamageParse.applyAttack(attack2, skil2, chr, attackCount2, maxdamage2, eff2, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);
                    }
                }, 500 * i + 500);
            }
        }
    }

    public static final void rangedAttack(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {

        if (chr == null) {
            return;
        }
        if ((chr.hasBlockedInventory()) || (chr.getMap() == null)) {
            return;
        }
        AttackInfo attack = DamageParse.parseDmgR(slea, chr);
        if (attack == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int bulletCount = 1;
        int skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;
        boolean AOE = attack.skill == 4111004;
        boolean noBullet = ((chr.getJob() >= 3500) && (chr.getJob() <= 3512)) || (GameConstants.isCannon(chr.getJob())) || (GameConstants.isJett(chr.getJob())) || (GameConstants.isPhantom(chr.getJob())) || (GameConstants.isMercedes(chr.getJob()));
        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
            if ((skill == null) || ((GameConstants.isAngel(attack.skill)) && (chr.getStat().equippedSummon % 10000 != attack.skill % 10000))) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }
            if (GameConstants.isEventMap(chr.getMapId())) {
                for (MapleEventType t : MapleEventType.values()) {
                    MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                    if ((e.isRunning()) && (!chr.isGM())) {
                        for (int i : e.getType().mapids) {
                            if (chr.getMapId() == i) {
                                chr.dropMessage(5, "You may not use that here.");
                                return;
                            }
                        }
                    }
                }
            }
            switch (attack.skill) {
                case 13101005:
                case 21110004: // Ranged but uses attackcount instead
                case 14101006: // Vampure
                case 21120006:
                case 11101004:
                case 51001004: // Mihile || Soul Blade
                case 1077:
                case 1078:
                case 1079:
                case 11077:
                case 11078:
                case 11079:
                case 15111007:
                case 13111007: //Wind Shot
                case 33101007:
                case 33101002:
                case 33121002:
                case 33121001:
                case 21100004:
                case 21110011:
                case 21100007:
                case 21000004:
                case 5121002:
                case 5921002:
                case 4121003:
                case 4221003:
                case 5221017:

                case 5721007:

                case 5221016:
                case 5721006:
                case 5211008:
                case 5201001:
                case 5721003:
                case 5711000:
                case 4111013:
                case 5121016:
                case 51111007: // Mihile || Radiant Buster
                case 51121008: // Mihile || Radiant Buster
                case 5121013:
                case 5221013:
                case 5721004:
                case 5721001:
                case 5321001:
                case 14111008:
                    AOE = true;
                    bulletCount = effect.getAttackCount();
                    break;
                case 35121005:
                case 35111004:
                case 35121013:
                    AOE = true;
                    bulletCount = 6;
                    break;
                default:
                    bulletCount = effect.getBulletCount();
                    break;
            }
            if (noBullet && effect.getBulletCount() < effect.getAttackCount()) {
                bulletCount = effect.getAttackCount();
            }
            if ((noBullet) && (effect.getBulletCount() < effect.getAttackCount())) {
                bulletCount = effect.getAttackCount();
            }
            if ((effect.getCooldown(chr) > 0) && (!chr.isGM()) && (((attack.skill != 35111004) && (attack.skill != 35121013)) || (chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != attack.skill))) {
                if (chr.skillisCooling(attack.skill)) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                boolean nocd = false;
                if(!nocd){
                    c.getSession().write(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
                    chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
                }
            }
        }
        attack = DamageParse.Modify_AttackCrit(attack, chr, 2, effect);
        Integer ShadowPartner = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER);
        if (ShadowPartner != null) {
            bulletCount *= 2;
        }
        int projectile = 0;
        int visProjectile = 0;
        if ((!AOE) && (chr.getBuffedValue(MapleBuffStat.SOULARROW) == null) && (!noBullet)) {
            Item ipp = chr.getInventory(MapleInventoryType.USE).getItem((short) attack.slot);
            if (ipp == null) {
                return;
            }
            projectile = ipp.getItemId();

            if (attack.csstar > 0) {
                if (chr.getInventory(MapleInventoryType.CASH).getItem((short) attack.csstar) == null) {
                    return;
                }
                visProjectile = chr.getInventory(MapleInventoryType.CASH).getItem((short) attack.csstar).getItemId();
            } else {
                visProjectile = projectile;
            }

            if (chr.getBuffedValue(MapleBuffStat.SPIRIT_CLAW) == null) {
                int bulletConsume = bulletCount;
                if ((effect != null) && (effect.getBulletConsume() != 0)) {
                    bulletConsume = effect.getBulletConsume() * (ShadowPartner != null ? 2 : 1);
                }
                //claw mastery
                int slotMax = MapleItemInformationProvider.getInstance().getSlotMax(projectile);
                int masterySkill = chr.getTotalSkillLevel(GameConstants.getMasterySkill(chr.getJob()));
                if (chr.getJob() >= 410 && chr.getJob() <= 412 && masterySkill > 0) {
                    slotMax += 10 * masterySkill;
                }
                if ((chr.getJob() == 412 || chr.getJob() == 411) && (bulletConsume > 0) && (ipp.getQuantity() < slotMax)) {
                    Skill expert = SkillFactory.getSkill(4110012);
                    if (chr.getTotalSkillLevel(expert) > 0) {
                        MapleStatEffect eff = expert.getEffect(chr.getTotalSkillLevel(expert));
                        if (eff.makeChanceResult()) {
                            ipp.setQuantity((short) (ipp.getQuantity() + 1));
                            c.getSession().write(CWvsContext.InventoryPacket.updateInventorySlot(MapleInventoryType.USE, ipp, false));
                            bulletConsume = 0;
                            c.getSession().write(CWvsContext.InventoryPacket.getInventoryStatus());
                        }
                    }
                }
                if ((bulletConsume > 0)
                        && (!MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true))) {
                    chr.dropMessage(5, "You do not have enough projectiles");
                    return;
                }
            }
        } else if ((chr.getJob() >= 3500) && (chr.getJob() <= 3512)) {
            visProjectile = 2333000;
        } else if (GameConstants.isCannon(chr.getJob())) {
            visProjectile = 2333001;
        }

        int projectileWatk = 0;
        if (projectile != 0) {
            projectileWatk = MapleItemInformationProvider.getInstance().getWatkForProjectile(projectile);
        }
        PlayerStats statst = chr.getStat();
        double basedamage;
        switch (attack.skill) {
            case 4001344:
            case 4121007:
            case 14001004:
            case 14111005:
                basedamage = Math.max(statst.getCurrentMaxBaseDamage(), statst.getTotalLuk() * 5.0F * (statst.getTotalWatk() + projectileWatk) / 100.0F);
                break;
            case 4111004:
                basedamage = 53000.0D;
                break;
            default:
                basedamage = statst.getCurrentMaxBaseDamage();
                switch (attack.skill) {
                    case 3101005:
                        basedamage *= effect.getX() / 100.0D;
                }

        }

        if (effect != null) {
            basedamage *= (effect.getDamage() + statst.getDamageIncrease(attack.skill)) / 100.0D;

            int money = effect.getMoneyCon();
            if (money != 0) {
                if (money > chr.getMeso()) {
                    money = chr.getMeso();
                }
                chr.gainMeso(-money, false);
            }
        }
        chr.checkFollow();
        if (!chr.isHidden()) {
            if (attack.skill == 3211006) {
                chr.getMap().broadcastMessage(chr, CField.strafeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk, chr.getTotalSkillLevel(3220010)), chr.getTruePosition());
            } else {
                chr.getMap().broadcastMessage(chr, CField.rangedAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk), chr.getTruePosition());
            }
        } else if (attack.skill == 3211006) {
            chr.getMap().broadcastGMMessage(chr, CField.strafeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk, chr.getTotalSkillLevel(3220010)), false);
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.rangedAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.unk), false);
        }

        DamageParse.applyAttack(attack, skill, chr, bulletCount, basedamage, effect, ShadowPartner != null ? AttackType.RANGED_WITH_SHADOWPARTNER : AttackType.RANGED);
    }

    public static final void MagicDamage(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.hasBlockedInventory()) || (chr.getMap() == null)) {
            return;
        }
        AttackInfo attack = DamageParse.parseDmgMa(slea, chr);
        if (attack == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
        if ((skill == null) || ((GameConstants.isAngel(attack.skill)) && (chr.getStat().equippedSummon % 10000 != attack.skill % 10000))) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int skillLevel = chr.getTotalSkillLevel(skill);
        MapleStatEffect effect = attack.getAttackEffect(chr, skillLevel, skill);
        if (effect == null) {
            return;
        }
        attack = DamageParse.Modify_AttackCrit(attack, chr, 3, effect);
        if (GameConstants.isEventMap(chr.getMapId())) {
            for (MapleEventType t : MapleEventType.values()) {
                MapleEvent e = ChannelServer.getInstance(chr.getClient().getChannel()).getEvent(t);
                if ((e.isRunning()) && (!chr.isGM())) {
                    for (int i : e.getType().mapids) {
                        if (chr.getMapId() == i) {
                            chr.dropMessage(5, "You may not use that here.");
                            return;
                        }
                    }
                }
            }
        }
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage() * (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0D;
        if (GameConstants.isPyramidSkill(attack.skill)) {
            maxdamage = 1.0D;
        } else if ((GameConstants.isBeginnerJob(skill.getId() / 10000)) && (skill.getId() % 10000 == 1000)) {
            maxdamage = 40.0D;
        }
        if ((effect.getCooldown(chr) > 0) && (!chr.isGM())) {
            if (chr.skillisCooling(attack.skill)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            int cd = effect.getCooldown(chr);
            c.getSession().write(CField.skillCooldown(attack.skill, cd));
            chr.addCooldown(attack.skill, System.currentTimeMillis(), cd * 1000);
        }
        chr.checkFollow();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, CField.magicAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, attack.charge, chr.getLevel(), attack.unk), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.magicAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, attack.charge, chr.getLevel(), attack.unk), false);
        }
        DamageParse.applyAttackMagic(attack, skill, c.getPlayer(), effect, maxdamage);
    }

    public static final void DropMeso(int meso, MapleCharacter chr) {
        if ((!chr.isAlive()) || (meso < 10) || (meso > 50000) || (meso > chr.getMeso())) {
            chr.getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
        chr.gainMeso(-meso, false, true);
        chr.getMap().spawnMesoDrop(meso, chr.getTruePosition(), chr, chr, true, (byte) 0);
       // chr.getCheatTracker().checkDrop(true);
    }

    public static final void ChangeAndroidEmotion(int emote, MapleCharacter chr) {
        if ((emote > 0) && (chr != null) && (chr.getMap() != null) && (!chr.isHidden()) && (emote <= 17) && (chr.getAndroid() != null)) {
            chr.getMap().broadcastMessage(CField.showAndroidEmotion(chr.getId(), emote));
        }
    }


    public static void MoveAndroid(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        slea.skip(8);
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3);

        if (res != null && chr != null && !res.isEmpty() && chr.getMap() != null && chr.getAndroid() != null) { // map crash hack
            final Point pos = new Point(chr.getAndroid().getPos());
            chr.getAndroid().updatePosition(res);
            chr.getMap().broadcastMessage(chr, CField.moveAndroid(chr.getId(), pos, res), false);
        }
    }

    public static final void ChangeEmotion(int emote, MapleCharacter chr) {
        if (emote > 7) {
            int emoteid = 5159992 + emote;
            MapleInventoryType type = GameConstants.getInventoryType(emoteid);
            if (chr.getInventory(type).findById(emoteid) == null) {
                chr.getCheatTracker().registerOffense(CheatingOffense.USING_UNAVAILABLE_ITEM, Integer.toString(emoteid));
                return;
            }
        }
        if ((emote > 0) && (chr != null) && (chr.getMap() != null) && (!chr.isHidden())) {
            chr.getMap().broadcastMessage(chr, CField.facialExpression(chr, emote), false);
        }
    }
 
   public static final void Heal(LittleEndianAccessor slea, MapleCharacter chr)
   {
/* 1227 */     if (chr == null) {
/* 1228 */       return;
     }
/* 1230 */     chr.updateTick(slea.readInt());
/* 1231 */     if (slea.available() >= 8L) {
/* 1232 */       slea.skip((slea.available() >= 12L) && (GameConstants.GMS) ? 8 : 4);
     }
/* 1234 */     int healHP = slea.readShort();
/* 1235 */     int healMP = slea.readShort();
 
/* 1237 */     PlayerStats stats = chr.getStat();
 
/* 1239 */     if (stats.getHp() <= 0) {
/* 1240 */       return;
     }
/* 1242 */     long now = System.currentTimeMillis();
/* 1243 */     if ((healHP != 0) && (chr.canHP(now + 1000L))) {
/* 1244 */       if (healHP > stats.getHealHP())
       {
/* 1246 */         healHP = (int)stats.getHealHP();
       }
/* 1248 */       chr.addHP(healHP);
     }
/* 1250 */     if ((healMP != 0) && (!GameConstants.isDemon(chr.getJob())) && (chr.canMP(now + 1000L))) {
/* 1251 */       if (healMP > stats.getHealMP())
       {
/* 1253 */         healMP = (int)stats.getHealMP();
       }
/* 1255 */       chr.addMP(healMP);
     }
   }
 
   public static final void MovePlayer(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(1);
        slea.skip(4);
        slea.skip(4);
        slea.skip(4);
        slea.skip(4);
        if (chr == null) {
            return;
        }
        Point Original_Pos = chr.getPosition();
        List res;
        try {
            res = MovementParse.parseMovement(slea, 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(new StringBuilder().append("AIOBE Type1:\n").append(slea.toString(true)).toString());
            return;
        }

        if ((res != null) && (c.getPlayer().getMap() != null)) {
            if ((slea.available() < 11L) || (slea.available() > 26L)) {
                return;
            }
            MapleMap map = c.getPlayer().getMap();

            if (chr.isHidden()) {
                chr.setLastRes(res);
                c.getPlayer().getMap().broadcastGMMessage(chr, CField.movePlayer(chr.getId(), res, Original_Pos), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.movePlayer(chr.getId(), res, Original_Pos), false);
            }

            MovementParse.updatePosition(res, chr, 0);
            Point pos = chr.getTruePosition();
            map.movePlayer(chr, pos);
            if ((chr.getFollowId() > 0) && (chr.isFollowOn()) && (chr.isFollowInitiator())) {
                MapleCharacter fol = map.getCharacterById(chr.getFollowId());
                if (fol != null) {
                    Point original_pos = fol.getPosition();
                    fol.getClient().getSession().write(CField.moveFollow(Original_Pos, original_pos, pos, res));
                    MovementParse.updatePosition(res, fol, 0);
                    map.movePlayer(fol, pos);
                    map.broadcastMessage(fol, CField.movePlayer(fol.getId(), res, original_pos), false);
                } else {
                    chr.checkFollow();
                }

            }

            int count = c.getPlayer().getFallCounter();
            boolean samepos = (pos.y > c.getPlayer().getOldPosition().y) && (Math.abs(pos.x - c.getPlayer().getOldPosition().x) < 5);
            if ((samepos) && ((pos.y > map.getBottom() + 250) || (map.getFootholds().findBelow(pos) == null))) {
                if (count > 5) {
                    c.getPlayer().changeMap(map, map.getPortal(0));
                    c.getPlayer().setFallCounter(0);
                } else {
                    count++;
                    c.getPlayer().setFallCounter(count);
                }
            } else if (count > 0) {
                c.getPlayer().setFallCounter(0);
            }
            c.getPlayer().setOldPosition(pos);
            if ((!samepos) && (c.getPlayer().getBuffSource(MapleBuffStat.DARK_AURA) == 32120000)) {
                c.getPlayer().getStatForBuff(MapleBuffStat.DARK_AURA).applyMonsterBuff(c.getPlayer());
            } else if ((!samepos) && (c.getPlayer().getBuffSource(MapleBuffStat.YELLOW_AURA) == 32120001)) {
                c.getPlayer().getStatForBuff(MapleBuffStat.YELLOW_AURA).applyMonsterBuff(c.getPlayer());
            }
        }
    }
 
   public static final void ChangeMapSpecial(String portal_name, MapleClient c, MapleCharacter chr)
   {
/* 1353 */     if ((chr == null) || (chr.getMap() == null)) {
/* 1354 */       return;
     }
/* 1356 */     MaplePortal portal = chr.getMap().getPortal(portal_name);
 
/* 1358 */    // if (chr.getGMLevel() > ServerConstants.PlayerGMRank.GM.getLevel()) {
/* 1359 */     //  chr.dropMessage(6, new StringBuilder().append(portal.getScriptName()).append(" accessed").toString());
   //  }
/* 1361 */     if ((portal != null) && (!chr.hasBlockedInventory()))
/* 1362 */       portal.enterPortal(c);
     else
/* 1364 */       c.getSession().write(CWvsContext.enableActions());
   }
 
   public static final void ChangeMap(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr)
   {
/* 1369 */     if ((chr == null) || (chr.getMap() == null)) {
/* 1370 */       return;
     }
/* 1372 */     if (slea.available() != 0L)
     {
/* 1374 */       slea.readByte();
/* 1375 */       int targetid = slea.readInt();
/* 1376 */       if (GameConstants.GMS) {
/* 1377 */         slea.readInt();
       }
/* 1379 */       MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
/* 1380 */       if (slea.available() >= 7L) {
/* 1381 */         chr.updateTick(slea.readInt());
       }
/* 1383 */       slea.skip(1);
/* 1384 */       boolean wheel = (slea.readShort() > 0) && (!GameConstants.isEventMap(chr.getMapId())) && (chr.haveItem(5510000, 1, false, true)) && (chr.getMapId() / 1000000 != 925);
 
/* 1386 */       if ((targetid != -1) && (!chr.isAlive())) {
/* 1387 */         chr.setStance(0);
/* 1388 */         if ((chr.getEventInstance() != null) && (chr.getEventInstance().revivePlayer(chr)) && (chr.isAlive())) {
/* 1389 */           return;
         }
/* 1391 */         if (chr.getPyramidSubway() != null) {
/* 1392 */           chr.getStat().setHp(50, chr);
/* 1393 */           chr.getPyramidSubway().fail(chr);
/* 1394 */           return;
         }
 
/* 1397 */         if (!wheel) {
/* 1398 */           chr.getStat().setHp(50, chr);
 
/* 1400 */           MapleMap to = chr.getMap().getReturnMap();
/* 1401 */           chr.changeMap(to, to.getPortal(0));
         } else {
/* 1403 */           c.getSession().write(CField.EffectPacket.useWheel((byte)(chr.getInventory(MapleInventoryType.CASH).countById(5510000) - 1)));
/* 1404 */           chr.getStat().setHp(chr.getStat().getMaxHp() / 100 * 40, chr);
/* 1405 */           MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);
 
/* 1407 */           MapleMap to = chr.getMap();
/* 1408 */           chr.changeMap(to, to.getPortal(0));
         }
/* 1410 */       } else if ((targetid != -1) && (chr.isIntern())) {
/* 1411 */         MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
/* 1412 */         if (to != null)
/* 1413 */           chr.changeMap(to, to.getPortal(0));
         else
/* 1415 */           chr.dropMessage(5, "Map is NULL. Use !warp <mapid> instead.");
       }
/* 1417 */       else if ((targetid != -1) && (!chr.isIntern())) {
/* 1418 */         int divi = chr.getMapId() / 100;
/* 1419 */         boolean unlock = false; boolean warp = false;
/* 1420 */         if (divi == 9130401) {
/* 1421 */           warp = (targetid / 100 == 9130400) || (targetid / 100 == 9130401);
/* 1422 */           if (targetid / 10000 != 91304) {
/* 1423 */             warp = true;
/* 1424 */             unlock = true;
/* 1425 */             targetid = 130030000;
           }
/* 1427 */         } else if (divi == 9130400) {
/* 1428 */           warp = (targetid / 100 == 9130400) || (targetid / 100 == 9130401);
/* 1429 */           if (targetid / 10000 != 91304) {
/* 1430 */             warp = true;
/* 1431 */             unlock = true;
/* 1432 */             targetid = 130030000;
           }
/* 1434 */         } else if (divi == 9140900) {
/* 1435 */           warp = (targetid == 914090011) || (targetid == 914090012) || (targetid == 914090013) || (targetid == 140090000);
/* 1436 */         } else if ((divi == 9120601) || (divi == 9140602) || (divi == 9140603) || (divi == 9140604) || (divi == 9140605)) {
/* 1437 */           warp = (targetid == 912060100) || (targetid == 912060200) || (targetid == 912060300) || (targetid == 912060400) || (targetid == 912060500) || (targetid == 3000100);
/* 1438 */           unlock = true;
/* 1439 */         } else if (divi == 9101500) {
/* 1440 */           warp = (targetid == 910150006) || (targetid == 101050010);
/* 1441 */           unlock = true;
/* 1442 */         } else if ((divi == 9140901) && (targetid == 140000000)) {
/* 1443 */           unlock = true;
/* 1444 */           warp = true;
/* 1445 */         } else if ((divi == 9240200) && (targetid == 924020000)) {
/* 1446 */           unlock = true;
/* 1447 */           warp = true;
/* 1448 */         } else if ((targetid == 980040000) && (divi >= 9800410) && (divi <= 9800450)) {
/* 1449 */           warp = true;
/* 1450 */         } else if ((divi == 9140902) && ((targetid == 140030000) || (targetid == 140000000))) {
/* 1451 */           unlock = true;
/* 1452 */           warp = true;
/* 1453 */         } else if ((divi == 9000900) && (targetid / 100 == 9000900) && (targetid > chr.getMapId())) {
/* 1454 */           warp = true;
/* 1455 */         } else if ((divi / 1000 == 9000) && (targetid / 100000 == 9000)) {
/* 1456 */           unlock = (targetid < 900090000) || (targetid > 900090004);
/* 1457 */           warp = true;
/* 1458 */         } else if ((divi / 10 == 1020) && (targetid == 1020000)) {
/* 1459 */           unlock = true;
/* 1460 */           warp = true;
/* 1461 */         } else if ((chr.getMapId() == 900090101) && (targetid == 100030100)) {
/* 1462 */           unlock = true;
/* 1463 */           warp = true;
/* 1464 */         } else if ((chr.getMapId() == 2010000) && (targetid == 104000000)) {
/* 1465 */           unlock = true;
/* 1466 */           warp = true;
/* 1467 */         } else if ((chr.getMapId() == 106020001) || (chr.getMapId() == 106020502)) {
/* 1468 */           if (targetid == chr.getMapId() - 1) {
/* 1469 */             unlock = true;
/* 1470 */             warp = true;
           }
/* 1472 */         } else if ((chr.getMapId() == 0) && (targetid == 10000)) {
/* 1473 */           unlock = true;
/* 1474 */           warp = true;
/* 1475 */         } else if ((chr.getMapId() == 931000011) && (targetid == 931000012)) {
/* 1476 */           unlock = true;
/* 1477 */           warp = true;
/* 1478 */         } else if ((chr.getMapId() == 931000021) && (targetid == 931000030)) {
/* 1479 */           unlock = true;
/* 1480 */           warp = true;
         }
/* 1482 */         if (unlock) {
/* 1483 */           c.getSession().write(CField.UIPacket.IntroDisableUI(false));
/* 1484 */           c.getSession().write(CField.UIPacket.IntroLock(false));
/* 1485 */           c.getSession().write(CWvsContext.enableActions());
         }
/* 1487 */         if (warp) {
/* 1488 */           MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
/* 1489 */           chr.changeMap(to, to.getPortal(0));
         }
       }
/* 1492 */       else if ((portal != null) && (!chr.hasBlockedInventory())) {
/* 1493 */         portal.enterPortal(c);
       } else {
/* 1495 */         c.getSession().write(CWvsContext.enableActions());
       }
     }
   }
 
   public static final void InnerPortal(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr)
   {
/* 1502 */     if ((chr == null) || (chr.getMap() == null)) {
/* 1503 */       return;
     }
/* 1505 */     MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
/* 1506 */     int toX = slea.readShort();
/* 1507 */     int toY = slea.readShort();
 
/* 1511 */     if (portal == null)
/* 1512 */       return;
/* 1513 */     if ((portal.getPosition().distanceSq(chr.getTruePosition()) > 22500.0D) && (!chr.isGM())) {
/* 1514 */       chr.getCheatTracker().registerOffense(CheatingOffense.USING_FARAWAY_PORTAL);
/* 1515 */       return;
     }
/* 1517 */     chr.getMap().movePlayer(chr, new Point(toX, toY));
/* 1518 */     chr.checkFollow();
   }
 
   public static final void snowBall(LittleEndianAccessor slea, MapleClient c)
   {
/* 1527 */     c.getSession().write(CWvsContext.enableActions());
   }
 
   public static final void leftKnockBack(LittleEndianAccessor slea, MapleClient c)
   {
/* 1532 */     if (c.getPlayer().getMapId() / 10000 == 10906) {
/* 1533 */       c.getSession().write(CField.leftKnockBack());
/* 1534 */       c.getSession().write(CWvsContext.enableActions());
     }
   }
 
   public static final void ReIssueMedal(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
/* 1539 */     MapleQuest q = MapleQuest.getInstance(slea.readShort());
/* 1540 */     int itemid = q.getMedalItem();
/* 1541 */     if ((itemid != slea.readInt()) || (itemid <= 0) || (q == null) || (chr.getQuestStatus(q.getId()) != 2)) {
/* 1542 */       c.getSession().write(CField.UIPacket.reissueMedal(itemid, 4));
/* 1543 */       return;
     }
/* 1545 */     if (chr.haveItem(itemid, 1, true, true)) {
/* 1546 */       c.getSession().write(CField.UIPacket.reissueMedal(itemid, 3));
/* 1547 */       return;
     }
/* 1549 */     if (!MapleInventoryManipulator.checkSpace(c, itemid, 1, "")) {
/* 1550 */       c.getSession().write(CField.UIPacket.reissueMedal(itemid, 2));
/* 1551 */       return;
     }
/* 1553 */     if (chr.getMeso() < 100) {
/* 1554 */       c.getSession().write(CField.UIPacket.reissueMedal(itemid, 1));
/* 1555 */       return;
     }
/* 1557 */     chr.gainMeso(-100, true, true);
/* 1558 */     MapleInventoryManipulator.addById(c, itemid, (byte)1, new StringBuilder().append("Redeemed item through medal quest ").append(q.getId()).append(" on ").append(FileoutputUtil.CurrentReadable_Date()).toString());
/* 1559 */     c.getSession().write(CField.UIPacket.reissueMedal(itemid, 0));
   }
 
public static void buySilentCrusade(LittleEndianAccessor slea, MapleClient c) {   
   int thutu= slea.readShort();
   int itemId= slea.readInt();
   slea.skip(2);//01 00 wtf???
   int coin= 0;
   
   switch(thutu){
    case 0:
        coin= 50;
        break;
    case 1:
        coin= 40;
        break;
    case 2:
        coin= 60;
        break;
    case 3:
    case 4:
        coin= 20;
        break;
    case 5:
    case 6:
        coin= 15;
        break;
    case 7:
    case 8:
        coin= 25;
        break;
    default:
        coin=0;
        break;
   }
   
   NPCConversationManager cm= new NPCConversationManager(c, 0, 0, (byte)0, null);
   if(!cm.haveItem(4310029, coin)) {
       return;
   }
   cm.gainItem(itemId, (short)1);
   cm.gainItem(4310029, (short)-coin);
}

 public static void DoRing(final MapleClient c, final String name, final int itemid) {
        final int newItemId = itemid == 2240000 ? 1112803 : (itemid == 2240001 ? 1112806 : (itemid == 2240002 ? 1112807 : (itemid == 2240003 ? 1112809 : (1112300 + (itemid - 2240004)))));
        final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
        int errcode = 0;
        if (c.getPlayer().getMarriageId() > 0) {
            errcode = 0x17;
        } else if (chr == null) {
            errcode = 0x12;
        } else if (chr.getMapId() != c.getPlayer().getMapId()) {
            errcode = 0x13;
        } else if (!c.getPlayer().haveItem(itemid, 1) || itemid < 2240000 || itemid > 2240015) {
            errcode = 0x0D;
        } else if (chr.getMarriageId() > 0 || chr.getMarriageItemId() > 0) {
            errcode = 0x18;
        } else if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "")) {
            errcode = 0x14;
        } else if (!MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
            errcode = 0x15;
        }
        if (errcode > 0) {
            c.getSession().write(CWvsContext.sendEngagement((byte) errcode, 0, null, null));
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        c.getPlayer().setMarriageItemId(itemid);
        chr.getClient().getSession().write(CWvsContext.sendEngagementRequest(c.getPlayer().getName(), c.getPlayer().getId()));
    }

    public static void RingAction(final LittleEndianAccessor slea, final MapleClient c) {
        final byte mode = slea.readByte();
        if (mode == 0) {
            DoRing(c, slea.readMapleAsciiString(), slea.readInt());
            //1112300 + (itemid - 2240004)
        } else if (mode == 1) {
            c.getPlayer().setMarriageItemId(0);
        } else if (mode == 2) { //accept/deny proposal
            final boolean accepted = slea.readByte() > 0;
            final String name = slea.readMapleAsciiString();
            final int id = slea.readInt();
            final MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
            if (c.getPlayer().getMarriageId() > 0 || chr == null || chr.getId() != id || chr.getMarriageItemId() <= 0 || !chr.haveItem(chr.getMarriageItemId(), 1) || chr.getMarriageId() > 0 || !chr.isAlive() || chr.getEventInstance() != null || !c.getPlayer().isAlive() || c.getPlayer().getEventInstance() != null) {
                c.getSession().write(CWvsContext.sendEngagement((byte) 0x1D, 0, null, null));
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (accepted) {
                final int itemid = chr.getMarriageItemId();
                final int newItemId = itemid == 2240000 ? 1112803 : (itemid == 2240001 ? 1112806 : (itemid == 2240002 ? 1112807 : (itemid == 2240003 ? 1112809 : (1112300 + (itemid - 2240004)))));
                if (!MapleInventoryManipulator.checkSpace(c, newItemId, 1, "") || !MapleInventoryManipulator.checkSpace(chr.getClient(), newItemId, 1, "")) {
                    c.getSession().write(CWvsContext.sendEngagement((byte) 0x15, 0, null, null));
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                try {
                    final int[] ringID = MapleRing.makeRing(newItemId, c.getPlayer(), chr);
                    Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[1]);
                    MapleRing ring = MapleRing.loadFromDb(ringID[1]);
                    if (ring != null) {
                        eq.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(c, eq);

                    eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(newItemId, ringID[0]);
                    ring = MapleRing.loadFromDb(ringID[0]);
                    if (ring != null) {
                        eq.setRing(ring);
                    }
                    MapleInventoryManipulator.addbyItem(chr.getClient(), eq);

                    MapleInventoryManipulator.removeById(chr.getClient(), MapleInventoryType.USE, chr.getMarriageItemId(), 1, false, false);

                    chr.getClient().getSession().write(CWvsContext.sendEngagement((byte) 0x10, newItemId, chr, c.getPlayer()));
                    chr.setMarriageId(c.getPlayer().getId());
                    c.getPlayer().setMarriageId(chr.getId());

                    chr.fakeRelog();
                    c.getPlayer().fakeRelog();
                } catch (Exception e) {
                    FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                }

            } else {
                chr.getClient().getSession().write(CWvsContext.sendEngagement((byte) 0x1E, 0, null, null));
            }
            c.getSession().write(CWvsContext.enableActions());
            chr.setMarriageItemId(0);
        } else if (mode == 3) { //drop, only works for ETC
            final int itemId = slea.readInt();
            final MapleInventoryType type = GameConstants.getInventoryType(itemId);
            final Item item = c.getPlayer().getInventory(type).findById(itemId);
            if (item != null && type == MapleInventoryType.ETC && itemId / 10000 == 421) {
                MapleInventoryManipulator.drop(c, type, item.getPosition(), item.getQuantity());
            }
        }
    }
 }
