package com.aguilam.xaerorecruits.claims;

import com.aguilam.xaerorecruits.XaeroRecruitsMod;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import com.talhanation.recruits.client.gui.worldmap.claim.ClaimEditScreen;
import com.talhanation.recruits.network.MessageUpdateClaim;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

public final class MapClaimActions {
    private static final int CLAIM_AREA_RANGE = 2;
    private static final int MAX_PLAYER_CLAIM_DISTANCE_CHUNKS = 4;
    private static final int BUFFER_ZONE_RADIUS = 3;

    private MapClaimActions() {
    }

    public record ClaimOption(String nameKey, String reasonKey, Runnable action, int cost) {
        public boolean enabled() {
            return this.reasonKey == null && this.action != null;
        }
    }

    public static List<ClaimOption> getOptions(int blockX, int blockZ, ResourceKey<Level> dimension) {
        try {
            return buildOptions(blockX, blockZ, dimension);
        } catch (Throwable throwable) {
            XaeroRecruitsMod.LOGGER.warn("Could not build the map claim options", throwable);
            return List.of();
        }
    }

    private static List<ClaimOption> buildOptions(int blockX, int blockZ, ResourceKey<Level> dimension) {
        List<ClaimOption> options = new ArrayList<>();
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        RecruitsFaction ownFaction = ClientManager.ownFaction;
        if (player == null || ownFaction == null || minecraft.level == null) {
            return options;
        }
        if (!Level.OVERWORLD.equals(dimension) || !Level.OVERWORLD.equals(minecraft.level.dimension())) {
            return options;
        }

        ChunkPos selectedChunk = new ChunkPos(blockX >> 4, blockZ >> 4);
        ClaimIndex.Snapshot snapshot = ClaimIndex.get().getSnapshot();
        RecruitsClaim clickedClaim = findClaimAt(selectedChunk);
        if (clickedClaim != null) {
            boolean manageable = canManageClaim(player, clickedClaim.getOwnerFaction(), clickedClaim);
            options.add(new ClaimOption(
                    "xaerorecruits.map_claim.edit",
                    manageable ? null : "xaerorecruits.reason.no_permission",
                    manageable ? () -> openClaimEdit(clickedClaim, player) : null,
                    0
            ));
            return options;
        }
        if (snapshot.hasChunk(selectedChunk.x, selectedChunk.z)) {
            return options;
        }

        int areaCost = getClaimAreaCost(ownFaction);
        String areaReason = getAreaBlockReason(player, ownFaction, selectedChunk, snapshot, areaCost);
        options.add(new ClaimOption(
                "xaerorecruits.map_claim.area",
                areaReason,
                areaReason == null ? () -> sendAreaClaim(player, selectedChunk) : null,
                areaCost
        ));

        RecruitsClaim neighborClaim = findAdjacentOwnClaim(selectedChunk, ownFaction);
        if (neighborClaim != null) {
            int expandCost = Math.max(0, ClientManager.configValueChunkCost);
            String expandReason = getExpansionBlockReason(player, ownFaction, neighborClaim, selectedChunk, snapshot, expandCost);
            options.add(new ClaimOption(
                    "xaerorecruits.map_claim.expand",
                    expandReason,
                    expandReason == null ? () -> sendExpansion(neighborClaim, selectedChunk) : null,
                    expandCost
            ));
        }
        return options;
    }

    public static void addOptions(ArrayList<RightClickOption> options, IRightClickableElement target,
                                  int blockX, int blockZ, ResourceKey<Level> dimension) {
        for (ClaimOption option : getOptions(blockX, blockZ, dimension)) {
            options.add(option.enabled()
                    ? createAction(option, options.size(), target)
                    : createBlockedOption(option, options.size(), target));
        }
    }

    public static boolean hasPrice(RightClickOption option) {
        return option instanceof ClaimRightClickOption;
    }

    private static RightClickOption createAction(ClaimOption option, int index, IRightClickableElement target) {
        return new ClaimRightClickOption(option, index, target);
    }

    private static RightClickOption createBlockedOption(ClaimOption option, int index, IRightClickableElement target) {
        ClaimRightClickOption clickOption = new ClaimRightClickOption(option, index, target);
        clickOption.setActive(false);
        clickOption.setNameFormatArgs(Component.translatable(option.reasonKey()));
        return clickOption;
    }

    private static final class ClaimRightClickOption extends RightClickOption {
        private final Runnable action;
        private final int cost;

        private ClaimRightClickOption(ClaimOption option, int index, IRightClickableElement target) {
            super(option.nameKey(), index, target);
            this.action = option.action();
            this.cost = option.cost();
        }

        @Override
        public void onAction(Screen screen) {
            if (action != null) {
                action.run();
            }
        }

        @Override
        public Component getDisplayName() {
            Component name = super.getDisplayName();
            ItemStack currency = ClientManager.currencyItemStack;
            if (cost <= 0 || currency == null || currency.isEmpty()) {
                return name;
            }
            return Component.empty().append(name).append(Component.translatable(
                    "xaerorecruits.map_claim.price", cost, currency.getHoverName()).withStyle(name.getStyle()));
        }
    }

    private static String getAreaBlockReason(Player player, RecruitsFaction ownFaction, ChunkPos center,
                                             ClaimIndex.Snapshot snapshot, int cost) {
        if (isTooFar(player, center)) {
            return "xaerorecruits.reason.too_far";
        }
        if (!isFactionLeader(player, ownFaction)) {
            return "xaerorecruits.reason.no_permission";
        }

        List<ChunkPos> area = getClaimArea(center);
        if (area.size() > getMaxClaimChunks()) {
            return "xaerorecruits.reason.claim_full";
        }

        for (ChunkPos chunk : area) {
            if (snapshot.hasChunk(chunk.x, chunk.z)) {
                return "xaerorecruits.reason.occupied";
            }
        }
        for (ChunkPos chunk : area) {
            if (isInBufferZone(chunk, ownFaction)) {
                return "xaerorecruits.reason.buffer";
            }
        }
        if (!canPlayerPay(cost, player)) {
            return "xaerorecruits.reason.no_payment";
        }
        return null;
    }

    private static String getExpansionBlockReason(Player player, RecruitsFaction ownFaction,
                                                  RecruitsClaim neighborClaim, ChunkPos selectedChunk,
                                                  ClaimIndex.Snapshot snapshot, int cost) {
        if (isTooFar(player, selectedChunk)) {
            return "xaerorecruits.reason.too_far";
        }
        List<ChunkPos> claimedChunks = neighborClaim.getClaimedChunks();
        if (claimedChunks == null || claimedChunks.size() >= getMaxClaimChunks()) {
            return "xaerorecruits.reason.claim_full";
        }
        if (!canManageClaim(player, ownFaction, neighborClaim)) {
            return "xaerorecruits.reason.no_permission";
        }
        if (snapshot.hasChunk(selectedChunk.x, selectedChunk.z)) {
            return "xaerorecruits.reason.occupied";
        }
        if (isInBufferZone(selectedChunk, ownFaction)) {
            return "xaerorecruits.reason.buffer";
        }
        if (!canPlayerPay(cost, player)) {
            return "xaerorecruits.reason.no_payment";
        }
        return null;
    }

    private static void sendAreaClaim(Player player, ChunkPos center) {
        try {
            RecruitsFaction faction = ClientManager.ownFaction;
            if (faction == null) {
                return;
            }

            RecruitsClaim claim = new RecruitsClaim(faction);
            claim.setCenter(center);
            claim.setPlayer(new RecruitsPlayerInfo(player.getUUID(), player.getName().getString(), faction));
            for (ChunkPos chunk : getClaimArea(center)) {
                claim.addChunk(chunk);
            }

            List<RecruitsClaim> claims = ClientManager.recruitsClaims;
            if (claims != null) {
                claims.add(claim);
            }

            sendClaim(claim);
        } catch (Throwable throwable) {
            XaeroRecruitsMod.LOGGER.warn("Could not send the claim area request", throwable);
        }
    }

    private static void sendExpansion(RecruitsClaim neighborClaim, ChunkPos selectedChunk) {
        try {
            neighborClaim.addChunk(selectedChunk);
            neighborClaim.setCenter(calculateCenter(neighborClaim.getClaimedChunks()));
            sendClaim(neighborClaim);
        } catch (Throwable throwable) {
            XaeroRecruitsMod.LOGGER.warn("Could not send the claim expansion request", throwable);
        }
    }

    private static void openClaimEdit(RecruitsClaim claim, Player player) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.screen instanceof GuiMap map)) {
                minecraft.setScreen(new ClaimEditScreen(null, claim, player));
                return;
            }
            MapProcessor processor = map.getMapProcessor();
            WorldMapScreen parent = new WorldMapScreen() {
                @Override
                protected void init() {
                    Minecraft.getInstance().setScreen(new GuiMap(null, null, processor, Minecraft.getInstance().getCameraEntity()));
                }
            };
            minecraft.setScreen(new ClaimEditScreen(parent, claim, player));
        } catch (Throwable throwable) {
            XaeroRecruitsMod.LOGGER.warn("Could not open the claim edit screen", throwable);
        }
    }

    private static void sendClaim(RecruitsClaim claim) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpdateClaim(claim));
    }

    private static int getMaxClaimChunks() {
        int configured = ClientManager.configValueMaxClaimChunks;
        return configured > 0 ? configured : RecruitsClaim.DEFAULT_MAX_SIZE;
    }

    private static ChunkPos calculateCenter(List<ChunkPos> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ChunkPos(0, 0);
        }
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ChunkPos chunk : chunks) {
            if (chunk == null) {
                continue;
            }
            minX = Math.min(minX, chunk.x);
            maxX = Math.max(maxX, chunk.x);
            minZ = Math.min(minZ, chunk.z);
            maxZ = Math.max(maxZ, chunk.z);
        }
        return new ChunkPos((minX + maxX) / 2, (minZ + maxZ) / 2);
    }

    private static List<ChunkPos> getClaimArea(ChunkPos center) {
        List<ChunkPos> area = new ArrayList<>((CLAIM_AREA_RANGE * 2 + 1) * (CLAIM_AREA_RANGE * 2 + 1));
        for (int dx = -CLAIM_AREA_RANGE; dx <= CLAIM_AREA_RANGE; dx++) {
            for (int dz = -CLAIM_AREA_RANGE; dz <= CLAIM_AREA_RANGE; dz++) {
                area.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
        return area;
    }

    private static RecruitsClaim findAdjacentOwnClaim(ChunkPos chunk, RecruitsFaction ownFaction) {
        ChunkPos[] neighbors = {
                new ChunkPos(chunk.x + 1, chunk.z),
                new ChunkPos(chunk.x - 1, chunk.z),
                new ChunkPos(chunk.x, chunk.z + 1),
                new ChunkPos(chunk.x, chunk.z - 1)
        };
        for (ChunkPos neighbor : neighbors) {
            RecruitsClaim claim = findClaimAt(neighbor);
            if (claim != null && !claim.isRemoved && sameFaction(claim.getOwnerFaction(), ownFaction)) {
                return claim;
            }
        }
        return null;
    }

    private static RecruitsClaim findClaimAt(ChunkPos chunk) {
        List<RecruitsClaim> claims = new ArrayList<>(ClientManager.recruitsClaims);
        for (RecruitsClaim claim : claims) {
            if (claim == null || claim.isRemoved || claim.getClaimedChunks() == null) {
                continue;
            }
            if (claim.getClaimedChunks().contains(chunk)) {
                return claim;
            }
        }
        return null;
    }

    private static boolean isInBufferZone(ChunkPos chunk, RecruitsFaction ownFaction) {
        List<RecruitsClaim> claims = new ArrayList<>(ClientManager.recruitsClaims);
        for (RecruitsClaim claim : claims) {
            if (claim == null || claim.getOwnerFaction() == null || claim.getClaimedChunks() == null) {
                continue;
            }
            if (sameFaction(claim.getOwnerFaction(), ownFaction)) {
                continue;
            }
            for (ChunkPos claimChunk : claim.getClaimedChunks()) {
                if (claimChunk == null) {
                    continue;
                }
                int dx = Math.abs(chunk.x - claimChunk.x);
                int dz = Math.abs(chunk.z - claimChunk.z);
                if (dx <= BUFFER_ZONE_RADIUS && dz <= BUFFER_ZONE_RADIUS && (dx != 0 || dz != 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canManageClaim(Player player, RecruitsFaction ownFaction, RecruitsClaim claim) {
        if (isFactionLeader(player, ownFaction)) {
            return true;
        }
        if (player.isCreative() && player.hasPermissions(2)) {
            return true;
        }
        RecruitsPlayerInfo playerInfo = claim.getPlayerInfo();
        return playerInfo != null && Objects.equals(playerInfo.getUUID(), player.getUUID());
    }

    private static boolean isFactionLeader(Player player, RecruitsFaction faction) {
        return faction != null && Objects.equals(faction.getTeamLeaderUUID(), player.getUUID());
    }

    private static boolean isTooFar(Player player, ChunkPos chunk) {
        ChunkPos playerChunk = player.chunkPosition();
        return Math.abs(playerChunk.x - chunk.x) > MAX_PLAYER_CLAIM_DISTANCE_CHUNKS
                || Math.abs(playerChunk.z - chunk.z) > MAX_PLAYER_CLAIM_DISTANCE_CHUNKS;
    }

    private static boolean canPlayerPay(int cost, Player player) {
        if (cost <= 0 || player.isCreative()) {
            return true;
        }
        ItemStack currency = ClientManager.currencyItemStack;
        return currency != null
                && !currency.isEmpty()
                && player.getInventory().countItem(currency.getItem()) >= cost;
    }

    private static int getClaimAreaCost(RecruitsFaction ownFaction) {
        if (!ClientManager.configValueCascadeClaimCost) {
            return Math.max(0, ClientManager.configValueClaimCost);
        }
        int amount = 1;
        List<RecruitsClaim> claims = new ArrayList<>(ClientManager.recruitsClaims);
        for (RecruitsClaim claim : claims) {
            if (claim != null && sameFaction(claim.getOwnerFaction(), ownFaction)) {
                amount++;
            }
        }
        return amount * Math.max(0, ClientManager.configValueClaimCost);
    }

    private static boolean sameFaction(RecruitsFaction left, RecruitsFaction right) {
        if (left == null || right == null) {
            return false;
        }
        return Objects.equals(left.getStringID(), right.getStringID());
    }
}
