package net.fabricmc.mygolf.entity.renderer;

import net.fabricmc.mygolf.EntityTestingClient;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.fabricmc.mygolf.entity.model.GolfBallEntityModel;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class GolfBallEntityRenderer extends MobEntityRenderer<GolfBallEntity, GolfBallEntityModel> {

    public GolfBallEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new GolfBallEntityModel(context.getPart(EntityTestingClient.MODEL_CUBE_LAYER)), 0.5f);
    }

    public Identifier getTexture(GolfBallEntity entity) {
        return new Identifier(CommonStr.modId, "textures/entity/cube/golf_ball.png");
    }
}
