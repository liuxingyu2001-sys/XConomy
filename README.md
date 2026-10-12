![img.png](img.png)
## 关于

基础经济插件，以 Vault 为前置，支持 BossShop、QuickShop 等使用 Vault API 的插件。
支持 MySQL / MariaDB / SQLite 保存玩家数据，并可通过 Redis 进行多服数据同步。

本项目面向 **Paper / Spigot 服务端**，代理端使用 **Velocity**，无需在代理端安装插件。

## 特性

- 多种存储后端：SQLite、MySQL、MariaDB
- MySQL 交易记录，支持玩家离线期间的转账提示
- Redis 跨服数据同步（无需在代理端安装插件）
- 跨服转账提示：
  - 优先使用 LiuChat 私密投递，只发给收款玩家（需各子服安装 LiuChat）
  - LiuChat 不可用时，回退内置 Redis 通道
- `/baltop hide|display` 隐藏 / 显示排行数据（玩家自助 + 管理员指定）
- 指令玩家名 TAB 补全支持跨服在线玩家
- **财富税自动扣税**：按日对玩家总财富征税，可并入 Residence 领地银行，多服共用 MySQL 保证一天只结算一次

## 文档

请查看 [说明文档](docs/说明文档.md)。

## XConomyAPI

请查看 [XConomyAPI](https://github.com/YiC200333/XConomyAPI)。

## 下载

Github: https://github.com/YiC200333/XConomy/releases
