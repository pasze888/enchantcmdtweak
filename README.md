# Enchant Command Tweak (`enchantcmdtweak`)

Minecraft 1.21.1 / NeoForge 21.1.x 服务端 mod，原地替换原版 `/enchant` 命令的执行逻辑：

- **突破等级上限**：不再受附魔自身 `maxLevel` 限制，等级允许 1~255。
  255 是原版数据组件编码（`ItemEnchantments.LEVEL_CODEC` = `intRange(0, 255)`）的硬上限，
  再高的等级在存档序列化时会被静默丢弃，故命令直接拒绝。
- **无视冲突**：跳过 `ItemStack#supportsEnchantment`（物品是否支持该附魔）与
  `EnchantmentHelper#isEnchantmentCompatible`（附魔互斥），可往主手任意物品硬塞任意附魔，
  与已有互斥附魔共存。

与原版保持一致的部分：`hasPermission(2)` 权限要求、仅作用于目标主手物品、
单目标失败报错/多目标失败静默跳过、成功与失败消息（复用原版 lang key，无新增语言文件）。

## 实现方式

纯 NeoForge 事件，零 Mixin：`RegisterCommandsEvent` 在原版命令注册完成后发出，
监听器用**同名命令重注册**覆盖原版 executor（Brigadier `CommandNode#addChild`
对同名子节点会替换 command）。

## 构建

```bash
export JAVA_HOME=<jdk21>   # 工作区约定：dragonwell21
./gradlew build
```
