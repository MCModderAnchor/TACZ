package com.tacz.guns.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.init.ModBlocks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Author: Forked from MrCrayfish, continued by Timeless devs
 */
public class BulletHoleParticle extends TextureSheetParticle {
    private final Direction direction;
    private final BlockPos pos;
    private int uOffset;
    private int vOffset;
    private float textureDensity;

    public BulletHoleParticle(ClientLevel world, double x, double y, double z, Direction direction, BlockPos pos) {
        super(world, x, y, z);
        this.setSprite(this.getSprite(pos));
        this.direction = direction;
        this.pos = pos;
        this.lifetime = this.getLifetimeFromConfig(world);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = 0.05F;

        // 如果方块是空气，则立即移除粒子
        BlockState state = world.getBlockState(pos);
        if (world.getBlockState(pos).isAir() || state.is(ModBlocks.TARGET.get())) {
            this.remove();
        }

        // 依据方块颜色决定粒子颜色
        int color = this.getBlockColor(state, world, pos, direction);
        this.rCol = ((float) (color >> 16 & 255) / 255.0F) / 3.0F;
        this.gCol = ((float) (color >> 8 & 255) / 255.0F) / 3.0F;
        this.bCol = ((float) (color & 255) / 255.0F) / 3.0F;
        this.alpha = 0.9F;
    }

    private int getLifetimeFromConfig(ClientLevel world) {
        int configLife = RenderConfig.BULLET_HOLE_PARTICLE_LIFE.get();
        if (configLife <= 1) {
            return configLife;
        }
        return configLife + world.random.nextInt(configLife / 2);
    }

    private int getBlockColor(BlockState state, Level world, BlockPos pos, Direction direction) {
        // 草方块是个例外（草方块是代码着色的）
        if (state.is(Blocks.GRASS_BLOCK)) {
            return Integer.MAX_VALUE;
        }
        return Minecraft.getInstance().getBlockColors().getColor(state, world, pos, 0);
    }

    @Override
    protected void setSprite(TextureAtlasSprite sprite) {
        super.setSprite(sprite);
        this.uOffset = this.random.nextInt(16);
        this.vOffset = this.random.nextInt(16);
        // 材质应该都是方形
        this.textureDensity = (sprite.getU1() - sprite.getU0()) / 16.0F;
    }

    private TextureAtlasSprite getSprite(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        Level world = minecraft.level;
        if (world != null) {
            BlockState state = world.getBlockState(pos);
            return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getTexture(state, world, pos);
        }
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    protected float getU0() {
        return this.sprite.getU0() + this.uOffset * this.textureDensity;
    }

    @Override
    protected float getV0() {
        return this.sprite.getV0() + this.vOffset * this.textureDensity;
    }

    @Override
    protected float getU1() {
        return this.getU0() + this.textureDensity;
    }

    @Override
    protected float getV1() {
        return this.getV0() + this.textureDensity;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.getBlockState(this.pos).isAir()) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Vec3 view = renderInfo.getPosition();
        float particleX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - view.x());
        float particleY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - view.y());
        float particleZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - view.z());
        Quaternionf quaternion = this.direction.getRotation();
        Vector3f[] points = new Vector3f[]{
                // Y 值稍微大一点点，防止 z-fight
                new Vector3f(-1.0F, 0.01F, -1.0F),
                new Vector3f(-1.0F, 0.01F, 1.0F),
                new Vector3f(1.0F, 0.01F, 1.0F),
                new Vector3f(1.0F, 0.01F, -1.0F)
        };
        float scale = this.getQuadSize(partialTicks);

        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = points[i];
            vector3f.transform(quaternion);
            vector3f.mul(scale);
            vector3f.add(particleX, particleY, particleZ);
        }

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        // 0 - 30 tick 内，从 15 亮度到 0 亮度
        int light = Math.max(15 - this.age / 2, 0);
        int lightColor = LightTexture.pack(light, light);
        double threshold = RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD.get() * this.lifetime;
        float fade = 1.0f - (float) (Math.max(this.age - threshold, 0) / (this.lifetime - threshold));
        buffer.vertex(points[0].x(), points[0].y(), points[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha * fade).uv2(lightColor).endVertex();
        buffer.vertex(points[1].x(), points[1].y(), points[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha * fade).uv2(lightColor).endVertex();
        buffer.vertex(points[2].x(), points[2].y(), points[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha * fade).uv2(lightColor).endVertex();
        buffer.vertex(points[3].x(), points[3].y(), points[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha * fade).uv2(lightColor).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}