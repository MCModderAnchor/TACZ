package com.tac.guns.block.entity;

import com.mojang.authlib.GameProfile;
import com.tac.guns.init.ModBlocks;
import com.tac.guns.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import static com.tac.guns.block.TargetBlock.STAND;

public class TargetBlockEntity extends BlockEntity {
    public static final BlockEntityType<TargetBlockEntity> TYPE = BlockEntityType.Builder.of(TargetBlockEntity::new, ModBlocks.TARGET.get()).build(null);
    public float rot = 0;
    public float oRot = 0;
    @Nullable
    private GameProfile owner;

    public TargetBlockEntity(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }

    public void setOwner(@Nullable GameProfile owner) {
        this.owner = owner;
    }

    @Nullable
    public GameProfile getOwner() {
        return owner;
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Owner", Tag.TAG_COMPOUND)) {
            this.owner = NbtUtils.readGameProfile(tag.getCompound("Owner"));
        }

    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if(owner!=null) {
            tag.put("Owner", NbtUtils.writeGameProfile(new CompoundTag(), owner));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if(pkt.getTag()!=null) {
            handleUpdateTag(pkt.getTag());
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if(owner!=null) {
            tag.put("Owner", NbtUtils.writeGameProfile(new CompoundTag(), owner));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains("Owner", Tag.TAG_COMPOUND)) {
            this.owner = NbtUtils.readGameProfile(tag.getCompound("Owner"));
        }
    }


    public void hit(Level pLevel, BlockState state, BlockPos pos) {
        if (this.level != null && state.getValue(STAND)) {
            pLevel.setBlock(pos,state.setValue(STAND,false),3);
            pLevel.scheduleTick(pos, state.getBlock(), 100);
            //todo 这个声音是占位符
            pLevel.playSound(null, pos, ModSounds.TARGET_HIT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static void clientTick(Level pLevel, BlockPos pPos, BlockState state, TargetBlockEntity pBlockEntity){
        pBlockEntity.oRot = pBlockEntity.rot;
        if(state.getValue(STAND)){
            pBlockEntity.rot = Math.max(pBlockEntity.rot-18, 0);
        }else {
            pBlockEntity.rot = Math.min(pBlockEntity.rot+45, 90);
        }
    }
}
