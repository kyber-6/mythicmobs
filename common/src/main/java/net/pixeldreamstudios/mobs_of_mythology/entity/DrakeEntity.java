package net.pixeldreamstudios.mobs_of_mythology.entity;

import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animatable.instance.SingletonAnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.pixeldreamstudios.mobs_of_mythology.MobsOfMythology;
import net.pixeldreamstudios.mobs_of_mythology.entity.constant.DefaultAnimations;
import net.pixeldreamstudios.mobs_of_mythology.entity.variant.DrakeVariant;
import net.pixeldreamstudios.mobs_of_mythology.registry.ItemRegistry;
import net.pixeldreamstudios.mobs_of_mythology.registry.SoundRegistry;
import net.pixeldreamstudios.mobs_of_mythology.registry.TagRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class DrakeEntity extends TamableAnimal implements GeoEntity {
    private AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public static final RawAnimation FIRE = RawAnimation.begin().thenLoop("fire");
    private boolean firing = false;
    protected static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(DrakeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HAS_EGG;
    public static final Predicate<LivingEntity> PREY_SELECTOR;

    public DrakeEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
        this.xpReward = Monster.XP_REWARD_MEDIUM;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData) {
        DrakeVariant variant = Util.getRandom(DrakeVariant.values(), this.random);
        setVariant(variant);
        return super.finalizeSpawn(world, difficulty, spawnReason, entityData);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

//    @Override
//    public boolean isBreedingItem(@NotNull ItemStack stack) {
//        return stack.getItem() == Items.GOLDEN_APPLE;
//    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ID_TYPE_VARIANT, 0);
        builder.define(HAS_EGG, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Variant", this.getTypeVariant());
        nbt.putBoolean("HasEgg", this.hasEgg());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(DATA_ID_TYPE_VARIANT, nbt.getInt("Variant"));
        this.setHasEgg(nbt.getBoolean("HasEgg"));
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return itemStack.is(TagRegistry.DRAKE_FOOD);
    }

    public DrakeVariant getVariant() {
        return DrakeVariant.byId(this.getTypeVariant() & 255);
    }

    private void setVariant(DrakeVariant variant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, variant.getId() & 255);
    }

    private int getTypeVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MobsOfMythology.config.drakeHealth)
                .add(Attributes.ATTACK_DAMAGE, MobsOfMythology.config.drakeAttackDamage)
                .add(Attributes.ATTACK_SPEED, 2)
                .add(Attributes.ATTACK_KNOCKBACK, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
//        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
//        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this, new Class[0])).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(5, new NonTameRandomTargetGoal(this, Animal.class, false, PREY_SELECTOR));
        this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal(this, true));
    }

    static {
        PREY_SELECTOR = (livingEntity) -> {
            EntityType<?> entityType = livingEntity.getType();
            return entityType == EntityType.VILLAGER || entityType == EntityType.WANDERING_TRADER || entityType == EntityType.WOLF;
        };
        HAS_EGG = SynchedEntityData.defineId(DrakeEntity.class, EntityDataSerializers.BOOLEAN);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "livingController", 3, state -> {
            if (isInSittingPose()) {
                state.getController().setAnimation(DefaultAnimations.SIT);
                return PlayState.CONTINUE;
            } else if (state.isMoving() && !swinging) {
                if (isAggressive() && !swinging) {
                    state.getController().setAnimation(DefaultAnimations.RUN);
                    return PlayState.CONTINUE;
                }
                else {
                    state.getController().setAnimation(DefaultAnimations.WALK);
                    return PlayState.CONTINUE;
                }
            } else if (swinging) {
                state.getController().setAnimation(DefaultAnimations.ATTACK);
                return PlayState.CONTINUE;
            }
            state.getController().setAnimation(DefaultAnimations.IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand interactionHand) {
        ItemStack itemStack = player.getItemInHand(interactionHand);
        Item item = itemStack.getItem();
        if (this.level().isClientSide && (!this.isBaby() || !this.isFood(itemStack))) {
            boolean bl = this.isOwnedBy(player) || this.isTame() || itemStack.is(ItemRegistry.COOKED_CHUPACABRA_MEAT.get()) && !this.isTame();
            return bl ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else if (this.isTame()) {
            if (this.isFood(itemStack) && this.getHealth() < this.getMaxHealth()) {
                itemStack.consume(1, player);
                FoodProperties foodProperties = (FoodProperties)itemStack.get(DataComponents.FOOD);
                float f = foodProperties != null ? (float)foodProperties.nutrition() : 1.0F;
                this.heal(2.0F * f);
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
            InteractionResult interactionResult = super.mobInteract(player, interactionHand);
            if (!interactionResult.consumesAction() && this.isOwnedBy(player)) {
                this.setOrderedToSit(!this.isOrderedToSit());
                this.jumping = false;
                this.navigation.stop();
                this.setTarget((LivingEntity)null);
                return InteractionResult.SUCCESS_NO_ITEM_USED;
            } else {
                return interactionResult;
            }
        } else if (itemStack.is(ItemRegistry.COOKED_CHUPACABRA_MEAT.get())) {
            itemStack.consume(1, player);
            this.tryToTame(player);
            return InteractionResult.SUCCESS;
        } else {
            return super.mobInteract(player, interactionHand);
        }
    }

    private void tryToTame(Player player) {
        if (this.random.nextInt(3) == 0) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget((LivingEntity)null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte)7);
        } else {
            this.level().broadcastEntityEvent(this, (byte)6);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        this.playSound(SoundRegistry.DRAKE_ROAR.get(), 1.0f, 1.0f);
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        this.playSound(SoundRegistry.DRAKE_ROAR.get(), 1.0f, 2.0f);
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        this.playSound(SoundRegistry.DRAKE_DEATH.get(), 1.0f, 1.0f);
        return null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WOLF_STEP, 0.75f, 1.0f);
    }

    public boolean hasEgg() {
        return (Boolean) this.entityData.get(HAS_EGG);
    }

    void setHasEgg(boolean hasEgg) {
        this.entityData.set(HAS_EGG, hasEgg);
    }

//    private static class LayEggGoal extends MoveToTargetPosGoal {
//        private final DrakeEntity drake;
//
//        LayEggGoal(DrakeEntity drake, double speed) {
//            super(drake, speed, 16);
//            this.drake = drake;
//        }
//
//        public boolean canStart() {
//            return this.drake.hasEgg() ? super.canStart() : false;
//        }
//
//        public boolean shouldContinue() {
//            return super.shouldContinue() && this.drake.hasEgg();
//        }
//
//        public void tick() {
//            super.tick();
//            BlockPos blockPos = this.drake.getBlockPos();
//            if (!this.drake.isTouchingWater() && this.hasReached()) {
//                World world = this.drake.world;
//                world.playSound((PlayerEntity) null, blockPos, SoundEvents.ENTITY_TURTLE_LAY_EGG, SoundCategory.BLOCKS, 1.0f, 1.25f);
//                world.setBlockState(this.targetPos.up(), (BlockState) BlockRegistry.DRAKE_EGG_BLOCK.getDefaultState());
//                this.drake.setHasEgg(false);
//                this.drake.setLoveTicks(600);
//            }
//
//        }
//
//        protected boolean isTargetPos(WorldView world, BlockPos pos) {
//            return !world.isAir(pos.up());
//        }
//    }

//    private static class MateGoal extends AnimalMateGoal {
//        private final DrakeEntity drake;
//
//        MateGoal(DrakeEntity drake, double speed) {
//            super(drake, speed);
//            this.drake = drake;
//        }
//
//        public boolean canStart() {
//            return super.canStart() && !this.drake.hasEgg();
//        }
//
//        protected void breed() {
//            ServerPlayerEntity serverPlayerEntity = this.animal.getLovingPlayer();
//            if (serverPlayerEntity == null && this.mate.getLovingPlayer() != null) {
//                serverPlayerEntity = this.mate.getLovingPlayer();
//            }
//
//            if (serverPlayerEntity != null) {
//                serverPlayerEntity.incrementStat(Stats.ANIMALS_BRED);
//                Criteria.BRED_ANIMALS.trigger(serverPlayerEntity, this.animal, this.mate, (PassiveEntity) null);
//            }
//
//            this.drake.setHasEgg(true);
//            this.animal.resetLoveTicks();
//            this.mate.resetLoveTicks();
//            Random random = this.animal.getRandom();
//            if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
//                this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.animal.getX(), this.animal.getY(), this.animal.getZ(), random.nextInt(7) + 1));
//            }
//
//        }
//    }
}
