package net.fabricmc.mygolf.entity;

import com.jme3.math.Vector3f;
import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import dev.lazurite.rayon.impl.bullet.thread.PhysicsThread;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.mygolf.MyGolfMod;
import net.fabricmc.mygolf.RegisterItems;
import net.fabricmc.mygolf.entity.base.BaseEntity;
import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.items.GolfClub;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class GolfBallEntity extends BaseEntity implements EntityPhysicsElement {
    private final EntityRigidBody rigidBody = new EntityRigidBody(this);
    ///被击打次数
    private int hitTimes = 0;

    public GolfBallEntity(EntityType<? extends LivingEntity> entityType, World level) {
        super(entityType, level);
        this.rigidBody.setMass(0.457f);
    }

    @Override
    public EntityRigidBody getRigidBody() {
        return this.rigidBody;
    }

    @Override
    public boolean isSilent() {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    protected void fall(double d, boolean bl, BlockState blockState, BlockPos blockPos) {
    }

    @Override
    public boolean handleFallDamage(float f, float g, DamageSource damageSource) {
        return false;
    }

    /// public
    /// 被玩家击打
    public void hitByPlayer(PlayerEntity player) {
        /// 被高尔夫球杆击打则次数+1
        if (player.getMainHandStack().getItem() instanceof GolfClub) {
            hitTimes++;
            MyGolfMod.LOGGER.info("击打次数为" + hitTimes);
        }
    }

    /**
     * 注册EntityType
     */
    public static EntityType<GolfBallEntity> register() {
        return Registry.register(
                Registry.ENTITY_TYPE,
                new Identifier(CommonStr.modId, "golf_ball_entity"),
                FabricEntityTypeBuilder.createLiving()
                        .entityFactory(GolfBallEntity::new)
                        .spawnGroup(SpawnGroup.MISC)
                        .defaultAttributes(LivingEntity::createLivingAttributes)
                        .dimensions(EntityDimensions.fixed(0.75f, 0.75f))
                        .trackRangeBlocks(80)
                        .build());
    }

    /**
     * 事件注册
     */
    public static void registerEvents() {
        AttackEntityCallback.EVENT.register(getAttckCallback()); //受击打
        UseEntityCallback.EVENT.register(getUseCallback());
    }

    /**
     * 受击打回调
     */
    private static AttackEntityCallback getAttckCallback() {
        return ((player, world, hand, entity, hitResult) -> {
            /* 手动的旁观者检查是必要的，因为 AttackBlockCallbacks 会在旁观者检查之前应用 */
            //敲打高尔夫球 远低近高
            if (entity instanceof GolfBallEntity && !player.isSpectator()) {
                //根据玩家与球的位置计算
                Vec3d ballPos = entity.getPos();
                Vec3d playerPos = player.getPos();
                Vec3d posDelta3d = ballPos.subtract(playerPos);
                //Vector3f posDelta3f = new Vector3f((float) posDelta3d.x,(float) posDelta3d.y,(float) posDelta3d.z);
                Vec2f posDelta2f = new Vec2f((float) posDelta3d.x, (float) posDelta3d.z); //水平方向向量差
                float hitDistance = posDelta2f.length(); //向量模
                Vec2f hitDirection = posDelta2f.normalize(); //单位向量
                float hitPitch = (30 - hitDistance) / 5;
                Vector3f impulse = new Vector3f(hitDirection.x * 5, hitPitch, hitDirection.y * 5);
                //rayon:给球一个冲量
                PhysicsThread.get(world.getServer()).execute(() -> {
                    ((GolfBallEntity) entity).getRigidBody().applyCentralImpulse(impulse);
                });
                if (world.isClient) System.out.println("imp " + impulse);
                ((GolfBallEntity) entity).hitByPlayer(player);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }

    /**
     * 使用回调
     */
    private static UseEntityCallback getUseCallback() {
        return (player, world, hand, entity, hitResult) -> {
            //高尔夫球没被移除时，捡起高尔夫球
            if (entity instanceof GolfBallEntity && entity.isAlive() && hand == Hand.MAIN_HAND && !player.isSpectator() && player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty()) {
                entity.discard();
                player.giveItemStack(new ItemStack(RegisterItems.GOLF_BALL));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        };
    }
}