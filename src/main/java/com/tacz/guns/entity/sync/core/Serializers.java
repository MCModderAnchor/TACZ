package com.tacz.guns.entity.sync.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Framework provided serializers used for creating a {@link SyncedDataKey}. This covers all
 * primitive types and common objects. You can create your custom serializer by implementing
 * {@link IDataSerializer}.
 * <p>
 * Author: MrCrayfish
 * Open source at <a href="https://github.com/MrCrayfish/Framework">Github</a> under LGPL License.
 */
public class Serializers {
    public static final IDataSerializer<Boolean> BOOLEAN = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Boolean value) {
            buf.writeBoolean(value);
        }

        @Override
        public Boolean read(FriendlyByteBuf buf) {
            return buf.readBoolean();
        }

        @Override
        public Tag write(Boolean value) {
            return ByteTag.valueOf(value);
        }

        @Override
        public Boolean read(Tag tag) {
            return ((ByteTag) tag).getAsByte() != 0;
        }
    };

    public static final IDataSerializer<Byte> BYTE = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Byte value) {
            buf.writeByte(value);
        }

        @Override
        public Byte read(FriendlyByteBuf buf) {
            return buf.readByte();
        }

        @Override
        public Tag write(Byte value) {
            return ByteTag.valueOf(value);
        }

        @Override
        public Byte read(Tag tag) {
            return ((ByteTag) tag).getAsByte();
        }
    };

    public static final IDataSerializer<Short> SHORT = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Short value) {
            buf.writeShort(value);
        }

        @Override
        public Short read(FriendlyByteBuf buf) {
            return buf.readShort();
        }

        @Override
        public Tag write(Short value) {
            return ShortTag.valueOf(value);
        }

        @Override
        public Short read(Tag tag) {
            return ((ShortTag) tag).getAsShort();
        }
    };

    public static final IDataSerializer<Integer> INTEGER = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Integer value) {
            buf.writeVarInt(value);
        }

        @Override
        public Integer read(FriendlyByteBuf buf) {
            return buf.readVarInt();
        }

        @Override
        public Tag write(Integer value) {
            return IntTag.valueOf(value);
        }

        @Override
        public Integer read(Tag tag) {
            return ((IntTag) tag).getAsInt();
        }
    };

    public static final IDataSerializer<Long> LONG = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Long value) {
            buf.writeLong(value);
        }

        @Override
        public Long read(FriendlyByteBuf buf) {
            return buf.readLong();
        }

        @Override
        public Tag write(Long value) {
            return LongTag.valueOf(value);
        }

        @Override
        public Long read(Tag tag) {
            return ((LongTag) tag).getAsLong();
        }
    };

    public static final IDataSerializer<Float> FLOAT = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Float value) {
            buf.writeFloat(value);
        }

        @Override
        public Float read(FriendlyByteBuf buf) {
            return buf.readFloat();
        }

        @Override
        public Tag write(Float value) {
            return FloatTag.valueOf(value);
        }

        @Override
        public Float read(Tag tag) {
            return ((FloatTag) tag).getAsFloat();
        }
    };

    public static final IDataSerializer<Double> DOUBLE = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Double value) {
            buf.writeDouble(value);
        }

        @Override
        public Double read(FriendlyByteBuf buf) {
            return buf.readDouble();
        }

        @Override
        public Tag write(Double value) {
            return DoubleTag.valueOf(value);
        }

        @Override
        public Double read(Tag tag) {
            return ((DoubleTag) tag).getAsDouble();
        }
    };

    public static final IDataSerializer<Character> CHARACTER = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, Character value) {
            buf.writeChar(value);
        }

        @Override
        public Character read(FriendlyByteBuf buf) {
            return buf.readChar();
        }

        @Override
        public Tag write(Character value) {
            return IntTag.valueOf(value);
        }

        @Override
        public Character read(Tag tag) {
            return (char) ((IntTag) tag).getAsInt();
        }
    };

    public static final IDataSerializer<String> STRING = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, String value) {
            buf.writeUtf(value);
        }

        @Override
        public String read(FriendlyByteBuf buf) {
            return buf.readUtf();
        }

        @Override
        public Tag write(String value) {
            return StringTag.valueOf(value);
        }

        @Override
        public String read(Tag tag) {
            return tag.getAsString();
        }
    };

    public static final IDataSerializer<CompoundTag> TAG_COMPOUND = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, CompoundTag value) {
            buf.writeNbt(value);
        }

        @Override
        public CompoundTag read(FriendlyByteBuf buf) {
            return buf.readNbt();
        }

        @Override
        public Tag write(CompoundTag value) {
            return value;
        }

        @Override
        public CompoundTag read(Tag tag) {
            return (CompoundTag) tag;
        }
    };

    public static final IDataSerializer<BlockPos> BLOCK_POS = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, BlockPos value) {
            buf.writeBlockPos(value);
        }

        @Override
        public BlockPos read(FriendlyByteBuf buf) {
            return buf.readBlockPos();
        }

        @Override
        public Tag write(BlockPos value) {
            return LongTag.valueOf(value.asLong());
        }

        @Override
        public BlockPos read(Tag tag) {
            return BlockPos.of(((LongTag) tag).getAsLong());
        }
    };

    public static final IDataSerializer<UUID> UUID = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, UUID value) {
            buf.writeUUID(value);
        }

        @Override
        public UUID read(FriendlyByteBuf buf) {
            return buf.readUUID();
        }

        @Override
        public Tag write(UUID value) {
            CompoundTag compound = new CompoundTag();
            compound.putLong("Most", value.getMostSignificantBits());
            compound.putLong("Least", value.getLeastSignificantBits());
            return compound;
        }

        @Override
        public UUID read(Tag tag) {
            CompoundTag compound = (CompoundTag) tag;
            return new UUID(compound.getLong("Most"), compound.getLong("Least"));
        }
    };

    public static final IDataSerializer<ItemStack> ITEM_STACK = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, ItemStack value) {
            buf.writeItem(value);
        }

        @Override
        public ItemStack read(FriendlyByteBuf buf) {
            return buf.readItem();
        }

        @Override
        public Tag write(ItemStack value) {
            return value.save(new CompoundTag());
        }

        @Override
        public ItemStack read(Tag tag) {
            return ItemStack.of((CompoundTag) tag);
        }
    };

    public static final IDataSerializer<ResourceLocation> RESOURCE_LOCATION = new IDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buf, ResourceLocation value) {
            buf.writeResourceLocation(value);
        }

        @Override
        public ResourceLocation read(FriendlyByteBuf buf) {
            return buf.readResourceLocation();
        }

        @Override
        public Tag write(ResourceLocation value) {
            return StringTag.valueOf(value.toString());
        }

        @Override
        public ResourceLocation read(Tag tag) {
            return ResourceLocation.tryParse(tag.getAsString());
        }
    };
}
