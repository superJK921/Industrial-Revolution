package me.steven.indrev.mixin;

import com.mojang.authlib.GameProfile;
import me.steven.indrev.armor.IRArmorMaterial;
import me.steven.indrev.armor.Module;
import me.steven.indrev.items.armor.IRModularArmor;
import me.steven.indrev.items.energy.IRGamerAxeItem;
import me.steven.indrev.items.energy.IRPortableChargerItem;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.reborn.energy.Energy;

import java.util.HashSet;
import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity {
    private int ticks = 0;
    private int lastDamageTick = 0;
    private final Set<Module> appliedEffects = new HashSet<>();

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void indrev_applyEffects(CallbackInfo ci) {
        ticks++;
        if (ticks % 40 == 0) {
            applyArmorEffects();
            useActiveAxeEnergy();
        }
    }

    @ModifyVariable(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
    private float indrev_absorbExplosionDamage(float amount, DamageSource source) {
        lastDamageTick = ticks;
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        PlayerInventory inventory = player.inventory;
        float damageAbsorbed = 0;
        for (ItemStack itemStack : inventory.armor) {
            Item item = itemStack.getItem();
            if (!(item instanceof IRModularArmor)) continue;
            int level = Module.Companion.getLevel(itemStack, Module.PROTECTION);
            double absorb = amount * (0.25 * (level / 3f));
            if (level > 0
                    && ((IRModularArmor) item).getShield(itemStack) > absorb
                    && canUseShield(itemStack, source)
            ) {
                if (source.equals(DamageSource.FALL) || source.isFire())
                    damageAbsorbed += ((IRModularArmor) item).useShield(itemStack, amount);
                else
                    damageAbsorbed += ((IRModularArmor) item).useShield(itemStack, absorb);
            }
        }
        return Math.max(amount - damageAbsorbed, 0);
    }

    private boolean canUseShield(ItemStack itemStack, DamageSource source) {
        if (source.equals(DamageSource.FALL)) return Module.Companion.isInstalled(itemStack, Module.FEATHER_FALLING);
        else if (source.isFire()) return Module.Companion.isInstalled(itemStack, Module.FIRE_RESISTANCE);
        else return !source.bypassesArmor();
    }

    private void useActiveAxeEnergy() {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        PlayerInventory inventory = player.inventory;
        for (ItemStack itemStack : inventory.main) {
            if (itemStack.getItem() instanceof IRGamerAxeItem) {
                CompoundTag tag = itemStack.getOrCreateTag();
                if (tag.contains("Active") && tag.getBoolean("Active") && !Energy.of(itemStack).use(5.0)) {
                    tag.putBoolean("Active", false);
                }
            }
        }
    }

    private void applyArmorEffects() {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        PlayerInventory inventory = player.inventory;
        Set<Module> effectsToRemove = new HashSet<>(appliedEffects);
        appliedEffects.clear();
        for (ItemStack itemStack : inventory.armor) {
            if (itemStack.getItem() instanceof ArmorItem && ((ArmorItem) itemStack.getItem()).getMaterial() == IRArmorMaterial.MODULAR) {
                Module[] modules = Module.Companion.getInstalled(itemStack);
                for (Module module : modules) {
                    int level = Module.Companion.getLevel(itemStack, module);
                    switch (module) {
                        case NIGHT_VISION:
                        case SPEED:
                        case JUMP_BOOST:
                        case BREATHING:
                        case FIRE_RESISTANCE:
                            StatusEffectInstance effect = module.getApply().invoke(player, level);
                            if (effect != null && Energy.of(itemStack).use(20.0)) {
                                if (!player.hasStatusEffect(effect.getEffectType()))
                                    player.addStatusEffect(effect);
                                appliedEffects.add(module);
                                effectsToRemove.remove(module);
                            }
                            break;
                        case AUTO_FEEDER:
                            HungerManager hunger = player.getHungerManager();
                            if (hunger.isNotFull()) {
                                for (int slot = 0; slot <= inventory.size(); slot++) {
                                    ItemStack stack = inventory.getStack(slot);
                                    FoodComponent food = stack.getItem().getFoodComponent();
                                    if (food != null && food.getHunger() <= 20 - hunger.getFoodLevel() && Energy.of(itemStack).use(30.0))
                                        player.eatFood(world, stack);
                                    if (!hungerManager.isNotFull()) break;
                                }
                            }
                            break;
                        case CHARGER:
                            IRPortableChargerItem.Companion.chargeItemsInInv(Energy.of(itemStack), player.inventory.main);
                            break;
                        case SOLAR_PANEL:
                            if (world.isDay() && world.isSkyVisible(player.getBlockPos().up())) {
                                for (ItemStack stackToCharge : inventory.armor) {
                                    if (Energy.valid(stackToCharge))
                                        Energy.of(stackToCharge).insert(75.0 * level);
                                }
                            }
                            break;
                        case PROTECTION:
                            if (ticks - 120 > lastDamageTick) {
                                ((IRModularArmor) itemStack.getItem()).regenShield(itemStack, level);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        for (Module module : effectsToRemove) {
            StatusEffectInstance effect = module.getApply().invoke(player, 1);
            if (effect != null)
                player.removeStatusEffect(effect.getEffectType());
        }
    }
}
