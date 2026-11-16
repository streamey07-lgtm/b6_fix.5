package com.b6core.entity;

import com.b6core.config.B6Config;
import com.b6core.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * B6. One entity type, three forms (stage 1, the missing-texture block, is a block):
 *   FORM_GLITCH (stage 2): harmless black-and-white glitch that stares; vanishes with a jumpscare
 *                          when stared at, approached, or hit.
 *   FORM_STONE  (stage 3): slow, tough, dark stone body with glowing cracks. Hostile.
 *   FORM_MAIN   (stage 4): floating half-shadow / half-white player model. Hostile. (default)
 */
public class B6Entity extends Monster {
    public static final int FORM_MAIN = 0;
    public static final int FORM_GLITCH = 1;
    public static final int FORM_STONE = 2;

    private static final EntityDataAccessor<Integer> DATA_FORM =
            SynchedEntityData.defineId(B6Entity.class, EntityDataSerializers.INT);

    private static final int GLITCH_LIFETIME_TICKS = 900; // 45 s
    private static final int GLITCH_STARE_TICKS = 40;     // 2 s of being looked at

    private int ageTicks = 0;
    private int lookTicks = 0;

    public B6Entity(EntityType<? extends B6Entity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D);
    }

    // ---------- form ----------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FORM, FORM_MAIN);
    }

    public int getForm() {
        return this.entityData.get(DATA_FORM);
    }

    /** Sets the form and updates health, speed and damage (server side). */
    public void setForm(int form) {
        this.entityData.set(DATA_FORM, form);
        applyForm(true);
    }

    private void applyForm(boolean heal) {
        if (this.level().isClientSide) {
            return;
        }
        int form = getForm();
        double difficulty = switch (B6Config.B6_DIFFICULTY.get()) {
            case EASY -> 0.6D;
            case NORMAL -> 1.0D;
            case HARD -> 1.5D;
            case NIGHTMARE -> 2.0D;
        };
        double health = form == FORM_GLITCH ? 20.0D : form == FORM_STONE ? 150.0D : 100.0D;
        double flying = form == FORM_STONE ? 0.25D : 0.5D;
        double damage = form == FORM_GLITCH ? 0.0D : 6.0D * difficulty;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(flying);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(damage);
        if (heal) {
            this.setHealth(this.getMaxHealth());
        }
        if (form != FORM_GLITCH) {
            this.setPersistenceRequired();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Form", getForm());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_FORM, tag.getInt("Form"));
        applyForm(false);
    }

    // ---------- movement / AI ----------

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The glitch form never attacks. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (getForm() == FORM_GLITCH) {
            target = null;
        }
        super.setTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || getForm() != FORM_GLITCH) {
            return;
        }
        ageTicks++;
        if (ageTicks > GLITCH_LIFETIME_TICKS) {
            this.discard();
            return;
        }
        for (Player other : this.level().players()) {
            if (!(other instanceof ServerPlayer player) || player.isSpectator()) {
                continue;
            }
            double dist = this.distanceTo(player);
            if (dist < 5.0D) {
                vanish(player);
                return;
            }
            if (dist > 48.0D) {
                continue;
            }
            Vec3 view = player.getViewVector(1.0F).normalize();
            Vec3 toMe = this.getEyePosition().subtract(player.getEyePosition()).normalize();
            if (view.dot(toMe) > 0.97D && player.hasLineOfSight(this)) {
                lookTicks++;
                if (lookTicks >= GLITCH_STARE_TICKS) {
                    vanish(player);
                    return;
                }
            } else if (lookTicks > 0) {
                lookTicks--;
            }
        }
    }

    /** Disappears with a scream and a jumpscare flash for that player. Placeholder sound for now. */
    private void vanish(ServerPlayer player) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.5F);
        ModNetwork.sendScare(player, 1);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && getForm() == FORM_GLITCH) {
            if (source.getEntity() instanceof ServerPlayer player) {
                vanish(player);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // floating: no fall damage handling needed
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }
}
