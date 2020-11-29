package handling.cashshop.handler;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

import constants.GameConstants;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleQuestStatus;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.Item;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CharacterTransfer;
import handling.world.World;
import java.util.List;
import server.CashItemFactory;
import server.CashItemInfo;
import server.MTSCart;
import server.MTSStorage;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.packet.CField;
import tools.packet.MTSCSPacket;
import tools.Triple;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;



public class CashShopOperation {
    private MapleCharacter chr;

    public static void LeaveCS(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        CashShopServer.getPlayerStorageMTS().deregisterPlayer(chr);
        CashShopServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());

        try {

            World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), c.getChannel());
            c.getSession().write(CField.getChannelChange(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1])));
        } finally {
            final String s = c.getSessionIPAddress();
            LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
            chr.saveToDB(false, true);
            c.setPlayer(null);
            c.setReceiving(false);
           // c.getSession().close();
            // World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, "[RumShield] " + MapleCharacterUtil.makeMapleReadable(chr.getName()) + " ( had an issue leaving CashShop! RumShield was about to D/C them but instead did not due to unstable Shield Protection. If this is a false report please report it to Rummy so the RumShield protection module can be adjusted." ));
              //  System.out.println("D/C ATTEMPT:  on " + chr.getName() + "PLEASE LOOK INTO THIS PLAYER!!!!");
        }
    }

	
   public static void EnterCS(final CharacterTransfer transfer, final MapleClient c) {
            if (transfer == null) {
                c.getSession().close();
                return;
            }
MapleCharacter chr = MapleCharacter.ReconstructChr(transfer, c, false);

        c.setPlayer(chr);
        c.setAccID(chr.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
             World.Broadcast.broadcastGMMessage(CWvsContext.serverNotice(6, "[RumShield] " + MapleCharacterUtil.makeMapleReadable(chr.getName()) + " Used a Remote Hack in CashShop!" ));
            return;
        }

        final int state = c.getLoginState();
        boolean allowLogin = false;
        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
            if (!World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()))) {
                allowLogin = true;
            }
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close();
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        /*if (mts) { //your server doesn't have mts does it no 
            CashShopServer.getPlayerStorageMTS().registerPlayer(chr);
            c.getSession().write(MTSCSPacket.startMTS(chr));
            final MTSCart cart = MTSStorage.getInstance().getCart(c.getPlayer().getId());
            cart.refreshCurrentView();
            MTSOperation.MTSUpdate(cart, c);
        } else {*/
            CashShopServer.getPlayerStorage().registerPlayer(chr);
            c.getSession().write(MTSCSPacket.warpCS(c));
            CSUpdate(c);
        //}

    }

    public static void CSUpdate(final MapleClient c) {
        c.getSession().write(MTSCSPacket.getCSGifts(c));
        doCSPackets(c);
        c.getSession().write(MTSCSPacket.sendWishList(c.getPlayer(), false));
    }

    public static void CouponCode(final String code, final MapleClient c) {
        if (code.length() <= 0) {
            return;
        }
        Triple<Boolean, Integer, Integer> info = null;
        try {
            info = MapleCharacterUtil.getNXCodeInfo(code);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (info != null && info.left) {
            int type = info.mid, item = info.right;
            try {
                MapleCharacterUtil.setNXCodeUsed(c.getPlayer().getName(), code);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            /*
             * Explanation of type!
             * Basically, this makes coupon codes do
             * different things!
             *
             * Type 1: A-Cash,
             * Type 2: Maple Points
             * Type 3: Item.. use SN
             * Type 4: Mesos
             */
            Map<Integer, Item> itemz = new HashMap<Integer, Item>();
            int maplePoints = 0, mesos = 0;
            switch (type) {
                case 1:
                case 2:
                    c.getPlayer().modifyCSPoints(type, item, false);
                    maplePoints = item;
                    break;
                case 3:
                    CashItemInfo itez = CashItemFactory.getInstance().getItem(item);
                    if (itez == null) {
                        c.getSession().write(MTSCSPacket.sendCSFail(0));
                        return;
                    }
                    byte slot = MapleInventoryManipulator.addId(c, itez.getId(), (short) 1, "", "Cash shop: coupon code" + " on " + FileoutputUtil.CurrentReadable_Date());
                    if (slot <= -1) {
                        c.getSession().write(MTSCSPacket.sendCSFail(0));
                        return;
                    } else {
                        itemz.put(item, c.getPlayer().getInventory(GameConstants.getInventoryType(item)).getItem(slot));
                    }
                    break;
                case 4:
                    c.getPlayer().gainMeso(item, false);
                    mesos = item;
                    break;
            }
            c.getSession().write(MTSCSPacket.showCouponRedeemedItem(itemz, mesos, maplePoints, c));
        } else {
            c.getSession().write(MTSCSPacket.sendCSFail(info == null ? 0xA7 : 0xA5)); //A1, 9F
        }
    }

    public static final void BuyCashItem(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int action = slea.readByte();
        if (action == 0) {
            slea.skip(2);
            CouponCode(slea.readMapleAsciiString(), c);
        } else if (action == 3) {
            slea.skip(1);
            final int toCharge = GameConstants.GMS ? slea.readInt() : 1;
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            
            if (item != null && chr.getCSPoints(toCharge) >= item.getPrice()) {
                if (!item.genderEquals(c.getPlayer().getGender())) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0xA7));
                    doCSPackets(c);
                    return;
                    } else if (item.getId() == 5530201) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                } else if (item.getId() == 5211048) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5211046) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360000) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360001) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360002) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360003) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360004) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360005) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360006) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360007) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360008) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5360042) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                else if (item.getId() == 5040004) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                }
                    else if (item.getId() == 5040006) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                     } else if (item.getId() == 5530201) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                     } else if (item.getId() == 5530201) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                    } else if (item.getId() == 5530201) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                    
                    } else if (item.getId() == 5530201 || item.getId() == 5530201) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                    
                    } else if (item.getId() == 5062000 || item.getId() == 5062001 || item.getId() == 5062002 || item.getId() == 5062003 || item.getId() == 5062005) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                    
                    } else if (item.getId() == 5530172 || item.getId() == 5530173 || item.getId() == 5530174 || item.getId() == 5530175 || item.getId() == 5530176 || item.getId() == 5530177 || item.getId() == 5220082 || item.getId() == 5222060 || item.getId() == 5064200 || item.getId() == 5050100 || item.getId() == 5051001 || item.getId() == 5062003 || item.getId() == 05062005 || item.getId() == 5062300 || item.getId() == 5064000 || item.getId() == 5064002 || item.getId() == 5064101 || item.getId() == 5064201 || item.getId() == 5064300 || item.getId() == 5064200 || item.getId() == 5064301 || item.getId() == 5502000 || item.getId() == 5502001 || item.getId() == 5502002 || item.getId() == 5502003 || item.getId() == 5502004  || item.getId() == 5502005 || item.getId() == 5502006 || item.getId() == 5502007 || item.getId() == 5252017 || item.getId() == 5534000 || item.getId() == 5510000 || item.getId() == 5550000 || item.getId() == 5550001 || item.getId() == 5550002) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                    
            }  else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0xB2));
                    doCSPackets(c);
                    return;
                }
                 
                
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                Item itemz = chr.getCashInventory().toItem(item);
                if (itemz != null && itemz.getUniqueId() > 0 && itemz.getItemId() == item.getId() && itemz.getQuantity() == item.getCount()) {
                    chr.getCashInventory().addToInventory(itemz);
                    //c.getSession().write(MTSCSPacket.confirmToCSInventory(itemz, c.getAccID(), item.getSN()));
                    c.getSession().write(MTSCSPacket.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
                     
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(0));
                }
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
            }
        } else if (action == 4 || action == 34) { //gift, package
            slea.readMapleAsciiString(); // pic
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            if (action == 4) {
                slea.skip(1);
            }
            String partnerName = slea.readMapleAsciiString();
            String msg = slea.readMapleAsciiString();
            if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice() || msg.length() > 73 || msg.length() < 1) { //dont want packet editors gifting random stuff =P
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId() || info.getMid().intValue() == c.getAccID()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xA2)); //9E v75
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(info.getRight().intValue())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xA3));
                doCSPackets(c);
                return;
            } else {
                //get the packets for that
                c.getPlayer().getCashInventory().gift(info.getLeft().intValue(), c.getPlayer().getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
                c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
                c.getSession().write(MTSCSPacket.sendGift(item.getPrice(), item.getId(), item.getCount(), partnerName, action == 34));
            }
        } else if (action == 5) { // Wishlist
            chr.clearWishlist();
            if (slea.available() < 40) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            int[] wishlist = new int[10];
            for (int i = 0; i < 10; i++) {
                wishlist[i] = slea.readInt();
            }
            chr.setWishlist(wishlist);
            c.getSession().write(MTSCSPacket.sendWishList(chr, true));

        } else if (action == 6) { // Increase inv
            slea.skip(1);
            final int toCharge = GameConstants.GMS ? slea.readInt() : 1;
            final boolean coupon = slea.readByte() > 0;
            if (coupon) {
                final MapleInventoryType type = getInventoryType(slea.readInt());
                if (chr.getCSPoints(toCharge) >= (GameConstants.GMS ? 6000 : 12000) && chr.getInventory(type).getSlotLimit() < 89) {
                    chr.modifyCSPoints(toCharge, (GameConstants.GMS ? -6000 : -12000), false);
                    chr.getInventory(type).addSlot((byte) 8);
                    chr.dropMessage(1, "Slots has been increased to " + chr.getInventory(type).getSlotLimit());
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(0xA4));
                }
            } else {
                final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                if (chr.getCSPoints(toCharge) >= (GameConstants.GMS ? 4000 : 8000) && chr.getInventory(type).getSlotLimit() < 93) {
                    chr.modifyCSPoints(toCharge, (GameConstants.GMS ? -4000 : -8000), false);
                    chr.getInventory(type).addSlot((byte) 4);
                    chr.dropMessage(1, "Slots has been increased to " + chr.getInventory(type).getSlotLimit());
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(0xA4));
                }
            }

        } else if (action == 7) { // Increase slot space
            slea.skip(1);
            final int toCharge = GameConstants.GMS ? slea.readInt() : 1;
            final int coupon = slea.readByte() > 0 ? 2 : 1;
            if (chr.getCSPoints(toCharge) >= (GameConstants.GMS ? 4000 : 8000) * coupon && chr.getStorage().getSlots() < (49 - (4 * coupon))) {
                chr.modifyCSPoints(toCharge, (GameConstants.GMS ? -4000 : -8000) * coupon, false);
                chr.getStorage().increaseSlots((byte) (4 * coupon));
                chr.getStorage().saveToDB();
                chr.dropMessage(1, "Storage slots increased to: " + chr.getStorage().getSlots());
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0xA4));
            }
        } else if (action == 8) { //...9 = pendant slot expansion
            slea.skip(1);
            final int toCharge = GameConstants.GMS ? slea.readInt() : 1;
            CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            int slots = c.getCharacterSlots();
            if (item == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || slots > 15 || item.getId() != 5430000) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            if (c.gainCharacterSlot()) {
                c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
                chr.dropMessage(1, "Character slots increased to: " + (slots + 1));
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
            }
        }
             else if (action == 10) { //...10 = pendant slot expansion
             final byte unk = slea.readByte(); // ?
             final int quantity = slea.readInt(); // Quantity?
             final int sn = slea.readInt();
             CashItemInfo item = CashItemFactory.getInstance().getItem(sn);
             if (item == null || c.getPlayer().getCSPoints(1) < item.getPrice() || item.getId() / 10000 != 555) {
             c.getSession().write(MTSCSPacket.sendCSFail(0));
             doCSPackets(c);
             return;
             }
             MapleQuestStatus marr = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
             if (marr != null && marr.getCustomData() != null && Long.parseLong(marr.getCustomData()) >= System.currentTimeMillis()) {
             c.getSession().write(MTSCSPacket.sendCSFail(39)); //The quest is already used, meaning the player already has the expanded slot.
             } else {
             c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).setCustomData(String.valueOf(System.currentTimeMillis() + ((long)/*item.getPeriod() is 0, default to 90 days*/90 * 24 * 60 * 60000)));
             c.getPlayer().modifyCSPoints(1, -item.getPrice(), false);
             MapleQuest.getInstance(7830).forceStart(c.getPlayer(), 0, "1");
             c.getSession().write(CWvsContext.pendantSlot(true));
             chr.dropMessage(1, "Additional pendant slot gained.");
             }
                    
        } else if (action == 14) { //get item from csinventory
            //uniqueid, 00 01 01 00, type->position(short)
            Item item = c.getPlayer().getCashInventory().findByCashId((int) slea.readLong());
            if (item != null && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                Item item_ = item.copy();
                short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                if (pos >= 0) {
                    if (item_.getPet() != null) {
                        item_.getPet().setInventoryPosition(pos);
                        c.getPlayer().addPet(item_.getPet());
                    }
                    c.getPlayer().getCashInventory().removeFromInventory(item);
                    c.getSession().write(MTSCSPacket.confirmFromCSInventory(item_, pos));
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(0xB1));
                }
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB1));
            }
        } else if (action == 15) { //put item in cash inventory
            int uniqueid = (int) slea.readLong();
            MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            Item item = c.getPlayer().getInventory(type).findByUniqueId(uniqueid);
            if (item != null && item.getQuantity() > 0 && item.getUniqueId() > 0 && c.getPlayer().getCashInventory().getItemsSize() < 100) {
                Item item_ = item.copy();
                MapleInventoryManipulator.removeFromSlot(c, type, item.getPosition(), item.getQuantity(), false);
                if (item_.getPet() != null) {
                    c.getPlayer().removePetCS(item_.getPet());
                }
                item_.setPosition((byte) 0);
                c.getPlayer().getCashInventory().addToInventory(item_);
                //warning: this d/cs
                //c.getSession().write(MTSCSPacket.confirmToCSInventory(item, c.getAccID(), c.getPlayer().getCashInventory().getSNForItem(item)));
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB1));
            }
        } else if (action == 32 || action == 38) { //38 = friendship, 32 = crush
            //c.getSession().write(MTSCSPacket.sendCSFail(0));
            slea.readMapleAsciiString(); // as13
            final int toCharge = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            final String partnerName = slea.readMapleAsciiString();
            final String msg = slea.readMapleAsciiString();
            if (item == null || !GameConstants.isEffectRing(item.getId()) || c.getPlayer().getCSPoints(toCharge) < item.getPrice() || msg.length() > 73 || msg.length() < 1) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xA6));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB1));
                doCSPackets(c);
                return;
            
            }
            Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, c.getPlayer().getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == c.getPlayer().getId()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB4)); //9E v75
                doCSPackets(c);
                return;
            } else if (info.getMid().intValue() == c.getAccID()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xA3)); //9D v75
                doCSPackets(c);
                return;
            } else {
                if (info.getRight().intValue() == c.getPlayer().getGender() && action == 30) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0xA1)); //9B v75
                    doCSPackets(c);
                    return;
                }

                int err = MapleRing.createRing(item.getId(), c.getPlayer(), partnerName, msg, info.getLeft().intValue(), item.getSN());

                if (err != 1) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0)); //9E v75
                    doCSPackets(c);
                    return;
                }
                c.getPlayer().modifyCSPoints(toCharge, -item.getPrice(), false);
            }
        } else if (action == 33) {
            slea.skip(1);
            final int toCharge = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            List<Integer> ccc = null;
            if (item != null) {
                    c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase Cashshop Packages."));
                    c.getSession().write(CWvsContext.enableActions());
                    doCSPackets(c);
                    return;
                //ccc = CashItemFactory.getInstance().getPackageItems(item.getId());
            }
            if (item == null || ccc == null || c.getPlayer().getCSPoints(toCharge) < item.getPrice()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(c.getPlayer().getGender())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xA6));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getCashInventory().getItemsSize() >= (100 - ccc.size())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB1));
                doCSPackets(c);
                return;
            }
          
           
            Map<Integer, Item> ccz = new HashMap<Integer, Item>();
            for (int i : ccc) {
                final CashItemInfo cii = CashItemFactory.getInstance().getSimpleItem(i);
                if (cii == null) {
                    continue;
                }
                Item itemz = c.getPlayer().getCashInventory().toItem(cii);
                if (itemz == null || itemz.getUniqueId() <= 0) {
                    continue;
                }
                for (int iz : GameConstants.cashBlock) {
                    if (itemz.getItemId() == iz) {
                        c.getSession().write(CWvsContext.serverNotice(1, "You cannot purchase this item through cash shop."));
						c.getSession().write(CWvsContext.enableActions());
						return;
					}
				}
                ccz.put(i, itemz);
                c.getPlayer().getCashInventory().addToInventory(itemz);
            }
            chr.modifyCSPoints(toCharge, -item.getPrice(), false);
            c.getSession().write(MTSCSPacket.showBoughtCSPackage(ccz, c.getAccID()));

        } else if (action == 35) {
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            if (item == null || !MapleItemInformationProvider.getInstance().isQuestItem(item.getId())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getMeso() < item.getPrice()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB8));
                doCSPackets(c);
                return;
            } else if (c.getPlayer().getInventory(GameConstants.getInventoryType(item.getId())).getNextFreeSlot() < 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB1));
                doCSPackets(c);
                return;
            }
            
         
            
            byte pos = MapleInventoryManipulator.addId(c, item.getId(), (short) item.getCount(), null, "Cash shop: quest item" + " on " + FileoutputUtil.CurrentReadable_Date());
            if (pos < 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB1));
                doCSPackets(c);
                return;
            }
            chr.gainMeso(-item.getPrice(), false);
            c.getSession().write(MTSCSPacket.showBoughtCSQuestItem(item.getPrice(), (short) item.getCount(), pos, item.getId()));
        } else if (action == 48) {
            c.getSession().write(MTSCSPacket.updatePurchaseRecord());
        } else if (action == 91) { // Open random box.
            final int uniqueid = (int) slea.readLong();
			
		c.getSession().write(MTSCSPacket.sendRandomBox(uniqueid, new Item(1302000, (short) 1, (short) 1, (short) 0, 10), (short) 0));
        } else {
            System.out.println("New Action: " + action + " Remaining: " + slea.toString());
            c.getSession().write(MTSCSPacket.sendCSFail(0));
        }
        doCSPackets(c);
    }

    private static final MapleInventoryType getInventoryType(final int id) {
        switch (id) {
            case 50200093:
                return MapleInventoryType.EQUIP;
            case 50200094:
                return MapleInventoryType.USE;
            case 50200197:
                return MapleInventoryType.SETUP;
            case 50200095:
                return MapleInventoryType.ETC;
            default:
                return MapleInventoryType.UNDEFINED;
        }
    }

    public static final void doCSPackets(MapleClient c) {
        c.getSession().write(MTSCSPacket.getCSInventory(c));
        c.getSession().write(MTSCSPacket.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(MTSCSPacket.enableCSUse());
        c.getPlayer().getCashInventory().checkExpire(c); 
    }

    private static void blockedCSNotice(MapleClient c) {
        throw new UnsupportedOperationException("You cannot purchase this item through the cash shop.");
    }
    

}