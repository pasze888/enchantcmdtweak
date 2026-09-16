# enchantcmdtweak 已验证 API 事实

## 已验证 API 签名（1.21.1 / NeoForge 21.1.244，build 通过）

- `net.minecraft.server.commands.EnchantCommand`（原版，`api-sources` 实读）：
  三道闸 = `level > Enchantment#getMaxLevel()` → `commands.enchant.failed.level`；
  `ItemStack#supportsEnchantment(Holder<Enchantment>)`（Neo 补丁，尊重 IItemExtension）；
  `EnchantmentHelper#isEnchantmentCompatible(...)`。执行体 `enchant()` 为 private static，
  无法复用，本 mod 整树复制后删检查。
- **同名重注册替换原版命令**（零 Mixin 的关键手法）：
  `Commands` 构造函数末尾（api-sources `Commands.java:251`）才发
  `EventHooks.onCommandRegister` → `RegisterCommandsEvent`，即 mod 监听器运行于原版
  `EnchantCommand.register` 之后。Brigadier 1.1.8 `CommandNode#addChild`（javap -c 核实）
  对同名子节点：`existing.command = new.getCommand()`（新 command 非空时）并递归合并 children，
  因此用同结构命令树重注册即可原地覆盖 executor；`requires` 与参数类型不会被更新（保留原版的）。
  注意 CommandNode/RootCommandNode/CommandDispatcher **均无 removeChild/removeCommand**（1.1.8 javap 核实）。
- `ItemEnchantments.LEVEL_CODEC = Codec.intRange(0, 255)`（api-sources `ItemEnchantments.java:33`）：
  附魔等级 >255 时游戏内当场生效但**存档序列化会被丢**，故 /enchant 的等级上限取 255，
  "任意正整数"在原版数据组件层物理不可行。
- `ItemStack#enchant(Holder<Enchantment>, int)`（ItemStack.java:951）→
  `EnchantmentHelper.updateEnchantments(this, b -> b.upgrade(holder, level))`。
- `Enchantment#getFullname(Holder<Enchantment>, int)` 返回 Component（Enchantment.java:199），
  成功消息直接复用。
- 报错文案全部复用原版 lang key（`commands.enchant.failed.*` / `.success.*`），无需新增语言文件。
