package com.tac.guns.client.gui;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.gui.components.ResultButton;
import com.tac.guns.crafting.GunSmithTableIngredient;
import com.tac.guns.crafting.GunSmithTableResult;
import com.tac.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.commons.compress.utils.Lists;

import java.util.List;
import java.util.Map;

public class GunSmithTableScreen extends AbstractContainerScreen<GunSmithTableMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GunMod.MOD_ID, "textures/gui/gun_smith_table.png");
    private final Map<String, List<ResourceLocation>> recipes = Maps.newHashMap();
    private ItemStack resultStack = ItemStack.EMPTY;
    private List<ResourceLocation> selectRecipeList;
    private int selectIndex;
    private int indexPage;

    public GunSmithTableScreen(GunSmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 192 * 2;
        this.imageHeight = 123;
        this.classifyRecipes();
        this.selectRecipeList = recipes.get(GunSmithTableResult.AMMO);
        this.indexPage = 0;
        this.selectIndex = 0;
    }

    private void classifyRecipes() {
        TimelessAPI.getAllRecipes().forEach((id, recipe) -> {
            GunSmithTableResult result = recipe.getResult();
            ItemStack itemStack = result.getResult();
            if (result.isGun() && itemStack.getItem() instanceof IGun iGun) {
                TimelessAPI.getCommonGunIndex(iGun.getGunId(itemStack)).ifPresent(index -> {
                    recipes.putIfAbsent(index.getType(), Lists.newArrayList());
                    recipes.get(index.getType()).add(id);
                });
                return;
            }
            if (result.isAmmo()) {
                recipes.putIfAbsent(GunSmithTableResult.AMMO, Lists.newArrayList());
                recipes.get(GunSmithTableResult.AMMO).add(id);
                return;
            }
            if (result.isAttachment()) {
                recipes.putIfAbsent(GunSmithTableResult.ATTACHMENT, Lists.newArrayList());
                recipes.get(GunSmithTableResult.ATTACHMENT).add(id);
            }
        });
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        this.addRenderableWidget(new ImageButton(leftPos + 192 + 5, topPos + 10, 16, 8, 20, 208, 8, TEXTURE, b -> {
            if (this.indexPage > 0) {
                this.indexPage--;
                this.init();
            }
        }));
        this.addRenderableWidget(new ImageButton(leftPos + 192 + 5, topPos + 108, 16, 8, 38, 208, 8, TEXTURE, b -> {
            if (selectRecipeList != null && !selectRecipeList.isEmpty()) {
                int maxIndexPage = (selectRecipeList.size() - 1) / 5;
                if (this.indexPage < maxIndexPage) {
                    this.indexPage++;
                    this.init();
                }
            }
        }));


        if (selectRecipeList != null && !selectRecipeList.isEmpty()) {
            for (int i = 0; i < 5; i++) {
                int finalIndex = i + indexPage * 5;
                if (finalIndex >= selectRecipeList.size()) {
                    break;
                }
                int yOffset = topPos + 21 + 17 * i;
                TimelessAPI.getRecipe(selectRecipeList.get(finalIndex)).ifPresent(recipe -> {
                    ResultButton button = addRenderableWidget(new ResultButton(leftPos + 192 + 22, yOffset, recipe.getOutput(), b -> {
                        resultStack = recipe.getOutput();
                        this.selectIndex = finalIndex;
                        this.init();
                    }));
                    if (finalIndex == selectIndex) {
                        button.setSelected(true);
                    }
                });
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderLeftModel();

        if (selectRecipeList != null && !selectRecipeList.isEmpty() && this.selectIndex < selectRecipeList.size()) {
            ResourceLocation id = selectRecipeList.get(this.selectIndex);
            TimelessAPI.getRecipe(id).ifPresent(recipe -> {
                List<GunSmithTableIngredient> inputs = recipe.getInputs();
                for (int i = 0; i < 5; i++) {
                    if (i >= inputs.size()) {
                        return;
                    }
                    int offsetX = leftPos + 192 + 117;
                    int offsetY = topPos + 52 + 17 * i;

                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, TEXTURE);
                    blit(poseStack, offsetX, offsetY, 128, 165, 53, 16);

                    GunSmithTableIngredient smithTableIngredient = inputs.get(i);
                    Ingredient ingredient = smithTableIngredient.getIngredient();
                    int count = smithTableIngredient.getCount();

                    ItemStack[] items = ingredient.getItems();
                    int itemIndex = ((int) (System.currentTimeMillis() / 1_000)) % items.length;
                    itemRenderer.renderGuiItem(items[itemIndex], offsetX + 2, offsetY);
                    font.draw(poseStack, String.format("x %d", count), offsetX + 24, offsetY + 4, 0xFFFFFF);
                }
            });
        }

        this.renderables.stream().filter(w -> w instanceof ResultButton)
                .forEach(w -> ((ResultButton) w).renderTooltips(stack -> this.renderTooltip(poseStack, stack, mouseX, mouseY)));
    }

    @SuppressWarnings("deprecation")
    private void renderLeftModel() {
        float scale = 64;
        float rotationPeriod = 8f;

        Minecraft.getInstance().textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        posestack.translate((double) leftPos + 100, (double) topPos + 50, 100);
        posestack.translate(8.0D, 8.0D, 0.0D);
        posestack.scale(1.0F, -1.0F, 1.0F);
        posestack.scale(scale, scale, scale);
        float rot = (System.currentTimeMillis() % (int) (rotationPeriod * 1000)) * (360f / (rotationPeriod * 1000));
        posestack.mulPose(Vector3f.YP.rotationDegrees(rot));
        RenderSystem.applyModelViewMatrix();
        PoseStack tmpPose = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        Minecraft.getInstance().getItemRenderer().renderStatic(resultStack, ItemTransforms.TransformType.FIXED, 0xf000f0, OverlayTexture.NO_OVERLAY, tmpPose, bufferSource, 0);
        bufferSource.endBatch();
        RenderSystem.enableDepthTest();
        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        this.renderBackground(poseStack);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(poseStack, leftPos + 192, topPos, 0, 0, 192, 123);
        blit(poseStack, leftPos + 192 - 50, topPos, 0, 128, 48, 11);
    }
}
