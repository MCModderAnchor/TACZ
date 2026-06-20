package com.tacz.guns.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.init.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.ForgeRegistries;

public class BulletHoleOption implements ParticleOptions {
    public static final Codec<BulletHoleOption> CODEC = RecordCodecBuilder.create(builder ->
            builder.group(Codec.INT.fieldOf("dir").forGetter(option -> option.direction.ordinal()),
                    Codec.LONG.fieldOf("pos").forGetter(option -> option.pos.asLong()),
                    Codec.STRING.fieldOf("ammo_id").forGetter(option -> option.ammoId),
                    Codec.STRING.fieldOf("gun_id").forGetter(option -> option.gunId),
                    Codec.STRING.optionalFieldOf("gun_display_id", DefaultAssets.DEFAULT_GUN_DISPLAY_ID.toString()).forGetter(option -> option.gunDisplayId),
                    Codec.BOOL.optionalFieldOf("incendiary", false).forGetter(option -> option.incendiary)
            ).apply(builder, BulletHoleOption::new));

    @SuppressWarnings("deprecation")
    public static final ParticleOptions.Deserializer<BulletHoleOption> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public BulletHoleOption fromCommand(ParticleType<BulletHoleOption> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int dir = reader.readInt();
            reader.expect(' ');
            long pos = reader.readLong();
            reader.expect(' ');
            String ammoId = reader.readString();
            reader.expect(' ');
            String gunId = reader.readString();
            reader.expect(' ');
            String gunDisplayId = reader.readString();
            boolean incendiary = false;
            if (reader.canRead()) {
                reader.expect(' ');
                incendiary = reader.readBoolean();
            }
            return new BulletHoleOption(dir, pos, ammoId, gunId, gunDisplayId, incendiary);
        }

        @Override
        public BulletHoleOption fromNetwork(ParticleType<BulletHoleOption> particleType, FriendlyByteBuf buffer) {
            return new BulletHoleOption(buffer.readVarInt(), buffer.readLong(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readBoolean());
        }
    };

    private final Direction direction;
    private final BlockPos pos;
    private final String ammoId;
    private final String gunId;
    private final String gunDisplayId;
    private final boolean incendiary;

    public BulletHoleOption(int dir, long pos, String ammoId, String gunId, String gunDisplayId) {
        this(dir, pos, ammoId, gunId, gunDisplayId, false);
    }

    public BulletHoleOption(int dir, long pos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        this.direction = Direction.values()[dir];
        this.pos = BlockPos.of(pos);
        this.ammoId = ammoId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.incendiary = incendiary;
    }

    public BulletHoleOption(Direction dir, BlockPos pos, String ammoId, String gunId, String gunDisplayId) {
        this(dir, pos, ammoId, gunId, gunDisplayId, false);
    }

    public BulletHoleOption(Direction dir, BlockPos pos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        this.direction = dir;
        this.pos = pos;
        this.ammoId = ammoId;
        this.gunId = gunId;
        this.gunDisplayId = gunDisplayId;
        this.incendiary = incendiary;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public String getAmmoId() {
        return ammoId;
    }

    public String getGunId() {
        return gunId;
    }

    public String getGunDisplayId() {
        return gunDisplayId;
    }

    public boolean isIncendiary() {
        return incendiary;
    }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.BULLET_HOLE.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.direction);
        buffer.writeBlockPos(this.pos);
        buffer.writeUtf(this.ammoId);
        buffer.writeUtf(this.gunId);
        buffer.writeUtf(this.gunDisplayId);
        buffer.writeBoolean(this.incendiary);
    }

    @Override
    public String writeToString() {
        return ForgeRegistries.PARTICLE_TYPES.getKey(this.getType()) + " "
                + this.direction.ordinal() + " "
                + this.pos.asLong() + " "
                + this.ammoId + " "
                + this.gunId + " "
                + this.gunDisplayId + " "
                + this.incendiary;
    }
}
