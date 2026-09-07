package io.github.pasze888.enchantcmdtweak;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 以同名重注册的方式替换原版 /enchant 的执行逻辑。
 *
 * Brigadier 的 CommandNode#addChild 遇到同名子节点时会用新节点的 executor
 * 覆盖旧节点的（见 brigadier 1.1.8 字节码），RegisterCommandsEvent 在原版
 * 命令注册完成后才发出（Commands 构造函数末尾），因此重注册即可原地替换，
 * 无需 Mixin、无需移除节点。
 *
 * 与原版（net.minecraft.server.commands.EnchantCommand）的差异：
 * - 不再检查 enchantment.getMaxLevel()，等级允许 1~255
 *   （255 是 ItemEnchantments.LEVEL_CODEC 的硬上限，再高存档序列化时会被静默丢弃）；
 * - 不再检查 ItemStack#supportsEnchantment 与 EnchantmentHelper#isEnchantmentCompatible，
 *   任意附魔可施加到主手任意物品上，可与已有互斥附魔共存。
 * 权限要求（hasPermission(2)）、仅作用主手、单目标报错/多目标跳过的语义与原版一致。
 */
public final class EnchantCommandOverride {
    /** ItemEnchantments.LEVEL_CODEC = Codec.intRange(0, 255)，超过会存不进组件 */
    private static final int LEVEL_CODEC_MAX = 255;

    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
        entityName -> Component.translatableEscape("commands.enchant.failed.entity", entityName)
    );
    private static final DynamicCommandExceptionType ERROR_NO_ITEM = new DynamicCommandExceptionType(
        entityName -> Component.translatableEscape("commands.enchant.failed.itemless", entityName)
    );
    private static final Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH = new Dynamic2CommandExceptionType(
        (level, max) -> Component.translatableEscape("commands.enchant.failed.level", level, max)
    );
    private static final SimpleCommandExceptionType ERROR_NOTHING_HAPPENED =
        new SimpleCommandExceptionType(Component.translatable("commands.enchant.failed"));

    private EnchantCommandOverride() {}

    /** RegisterCommandsEvent 监听：在原版注册之后以同名命令覆盖执行逻辑 */
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    /** 命令树结构与原版 EnchantCommand#register 完全一致，仅 executes 换成本类的实现 */
    private static void register(
        CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context
    ) {
        dispatcher.register(
            Commands.literal("enchant")
                .requires(source -> source.hasPermission(2))
                .then(
                    Commands.argument("targets", EntityArgument.entities())
                        .then(
                            Commands.argument("enchantment", ResourceArgument.resource(context, Registries.ENCHANTMENT))
                                .executes(
                                    p_248131_ -> enchant(
                                            p_248131_.getSource(),
                                            EntityArgument.getEntities(p_248131_, "targets"),
                                            ResourceArgument.getEnchantment(p_248131_, "enchantment"),
                                            1
                                        )
                                )
                                .then(
                                    Commands.argument("level", IntegerArgumentType.integer(0))
                                        .executes(
                                            p_248132_ -> enchant(
                                                    p_248132_.getSource(),
                                                    EntityArgument.getEntities(p_248132_, "targets"),
                                                    ResourceArgument.getEnchantment(p_248132_, "enchantment"),
                                                    IntegerArgumentType.getInteger(p_248132_, "level")
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int enchant(
        CommandSourceStack source, Collection<? extends Entity> targets, Holder<Enchantment> enchantment, int level
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (level < 1) {
            throw ERROR_NOTHING_HAPPENED.create();
        }
        if (level > LEVEL_CODEC_MAX) {
            throw ERROR_LEVEL_TOO_HIGH.create(level, LEVEL_CODEC_MAX);
        }

        int i = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity livingentity) {
                ItemStack itemstack = livingentity.getMainHandItem();
                if (!itemstack.isEmpty()) {
                    itemstack.enchant(enchantment, level);
                    i++;
                } else if (targets.size() == 1) {
                    throw ERROR_NO_ITEM.create(livingentity.getName().getString());
                }
            } else if (targets.size() == 1) {
                throw ERROR_NOT_LIVING_ENTITY.create(entity.getName().getString());
            }
        }

        if (i == 0) {
            throw ERROR_NOTHING_HAPPENED.create();
        }
        if (targets.size() == 1) {
            source.sendSuccess(
                () -> Component.translatable(
                        "commands.enchant.success.single",
                        Enchantment.getFullname(enchantment, level),
                        targets.iterator().next().getDisplayName()
                    ),
                true
            );
        } else {
            source.sendSuccess(
                () -> Component.translatable("commands.enchant.success.multiple", Enchantment.getFullname(enchantment, level), targets.size()),
                true
            );
        }
        return i;
    }
}
