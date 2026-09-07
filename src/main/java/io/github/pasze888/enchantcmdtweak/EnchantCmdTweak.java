package io.github.pasze888.enchantcmdtweak;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 服务端 mod：原地替换原版 /enchant，放开等级上限与附魔兼容性检查。
 */
@Mod(EnchantCmdTweak.MODID)
public class EnchantCmdTweak {
    public static final String MODID = "enchantcmdtweak";

    public EnchantCmdTweak(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(EnchantCommandOverride::onRegisterCommands);
    }
}
