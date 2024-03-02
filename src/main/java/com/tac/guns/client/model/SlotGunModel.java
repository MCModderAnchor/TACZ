package com.tac.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tac.guns.client.model.bedrock.BedrockCubePerFace;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.pojo.model.FaceUVsItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.entity.Entity;


public class SlotGunModel extends EntityModel<Entity> {
    private final BedrockPart bone;

    public SlotGunModel() {
        bone = new BedrockPart("slot");
        bone.setPos(8.0F, 24.0F, -10.0F);
        bone.cubes.add(new BedrockCubePerFace(-16.0F, -16.0F, 9.5F, 16.0F, 16.0F, 0, 0, 16, 16, FaceUVsItem.singleSouthFace()));
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bone.render(poseStack, ItemTransforms.TransformType.GUI, buffer, packedLight, packedOverlay);
    }
}
