package com.tac.guns.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tac.guns.init.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

public class BulletHoleOption implements ParticleOptions {
    public static final Codec<BulletHoleOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(Codec.INT.fieldOf("dir").forGetter(option -> option.direction.ordinal()),
                    Codec.LONG.fieldOf("pos").forGetter(option -> option.pos.asLong())).apply(builder, BulletHoleOption::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<BulletHoleOption> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public BulletHoleOption fromCommand(ParticleType<BulletHoleOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int dir = reader.readInt();
            reader.expect(' ');
            long pos = reader.readLong();
            return new BulletHoleOption(dir, pos);
        }

        @Override
        public BulletHoleOption fromNetwork(ParticleType<BulletHoleOption> particleType, FriendlyByteBuf buffer) {
            return new BulletHoleOption(buffer.readVarInt(), buffer.readLong());
        }
    };

    private final Direction direction;
    private final BlockPos pos;

    public BulletHoleOption(int dir, long pos) {
        this.direction = Direction.values()[dir];
        this.pos = BlockPos.of(pos);
    }

    public BulletHoleOption(Direction dir, BlockPos pos) {
        this.direction = dir;
        this.pos = pos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BULLET_HOLE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.direction);
        buffer.writeBlockPos(this.pos);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " " + this.direction.getName();
    }
}
