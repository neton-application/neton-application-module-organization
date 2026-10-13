# neton-application-module-organization SPEC

> Status: active
> Scope: 通用地基模块【组织】，moduleId `organization`，坐标 `com.netonstream.app:module-organization`
> 定位：账号（module-account）之上的"组织分组"能力，**与任何具体业务无关**；设备管理、privchat 工作组等都是它的消费方。

## 1. 目的与边界

组织回答一个问题：**"一批账号如何组成一个可自治的群体，并被业务归属引用"**。

对标钉钉："我登录后可以创建一个自己的组织，组织里放成员，成员各自管理本组织事务。"

- **认识**：账号（成员 = module-account 的 `account`）、账号在组织内的角色、组织生命周期。
- **不认识**：设备、工作组、任何业务实体。业务只引用 `organization.id` 把自己的资源归属到某组织。
- 依赖箭头单向：`organization → account → (system, infra)`。业务 `→ organization`。**组织永不 import 业务**（同 module-account 的地基模块纪律）。

### 不做什么
- 不做通用多租户 SaaS 隔离框架（无租户拦截器、无按组织分库）。它是"分组 + 局部角色"，隔离强度由消费方按引用决定。
- 不新造账号类型。"子账号"不是 account 的一种 type，而是一个 account 被纳入组织并带上组织内角色。
- 不管理业务数据（设备/群组等），也不在删组织时反向清理业务归属。

## 2. 数据模型

### `organization`（组织本体）
| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| name | VARCHAR(128) | 组织名称 |
| logo | VARCHAR(255) | 头像（infra 文件 fileId，空=未设） |
| code | VARCHAR(64) | 全局唯一编码（空=未设；部分唯一索引 WHERE deleted=0 AND code<>''） |
| invite_code | VARCHAR(32) | 邀请码，成员凭此自助加入（建组织生成，可重置） |
| owner_account_id | BIGINT | 主账号（创建人）account.id |
| status | SMALLINT | 0 停用 / 1 正常（`OrgStatus`） |
| remark | VARCHAR(512) | 简介 |
| deleted / created_at / updated_at | | 软删 + epoch 审计 |

### `organization_member`（成员）
| 列 | 类型 | 说明 |
|---|---|---|
| id | BIGSERIAL PK | |
| org_id | BIGINT | organization.id |
| account_id | BIGINT | 成员账号 account.id |
| role | SMALLINT | 1 主 / 2 管理员 / 3 成员（`OrgRole`，rank 3>2>1） |
| status | SMALLINT | 0 停用 / 1 正常 |
| source | SMALLINT | 1 创建人 / 2 后台分配 / 3 邀请码加入 / 4 后台邀请（`OrgMemberSource`，仅审计） |
| joined_at | BIGINT | 加入时刻 epoch |
| deleted / created_at / updated_at | | |

唯一约束 `(org_id, account_id) WHERE deleted = 0`。

## 3. 角色与策略

- **一个账号可创建多个组织、可加入多个组织**（钉钉式）。"主账号创建一个组织"是能力不是硬上限；如需"一人一组织"上限，在消费方装配层用 `listOwnedOrgIds` 校验即可，本模块不写死。
- **OWNER 唯一且特殊**：建组织时创建人自动登记为 OWNER；OWNER 不能经成员接口授予/批量分配/移除（见 `OrganizationMemberLogic` 的多处 guard）。转移主账号是未来的独立操作（本期未做）。
- 鉴权门槛：**读=MEMBER；改资料/启停/改成员/重置邀请码=ADMIN；删组织=OWNER**。

## 4. 对外接缝（业务如何消费）

### 4.1 `port.OrganizationDirectory`（只读鉴权端口，注入即用）
`getRole / isMember / hasRole / requireRole(抛 PERMISSION_DENIED) / listOrgIdsOf / listOwnedOrgIds / listMembers`。
消费方（设备管理、privchat）**不查组织表**，一律经此端口判归属。

> ⚠️ **两条腿都要卡**：HTTP 侧在控制器/逻辑里 `requireRole` 即可；但**绕过 HTTP 的实时链路**（如 msgtrans 设备腿下行命令按 deviceId 直发）也必须经本端口校验"发起账号是否拥有该设备所属组织"，否则跨组织越权。

### 4.2 `port.OrganizationExtensions`（生命周期扩展点，登记钩子）
`OrganizationCreationHook.afterCreated(org, ownerAccountId)`、`OrganizationMemberChangedHook.afterMembersChanged(orgId, changedAccountIds)`。
消费方在**自己的** bootstrap（拓扑序在 organization 之后）`ctx.get(OrganizationExtensions::class).addCreationHook(...)` 登记；钩子实现须自行 catch（尽力而为，不拖垮建组织）。

### 4.3 归属落地范式
业务表加一列 `organization_id BIGINT`（引用 `organization.id`）+ 一条 `WHERE organization_id = :org` 的过滤索引即可；组织不感知该表存在。

## 5. HTTP API

### C 端（client 控制台，account 会话鉴权，当前账号来自 `Identity`）
- `OrganizationSelfController` `/app/org`：`GET /list`、`GET /get/{id}`、`POST /create`、`PUT /update`、`PUT /update-status`、`POST /reset-invite-code`、`POST /join`、`DELETE /delete/{id}`
- `OrganizationMemberSelfController` `/app/org/member`：`GET /list`、`POST /add`、`PUT /update-role`、`POST /remove`

### B 端（front 管理台，system 管理员鉴权 + RBAC）
- `OrganizationController` `/org`：`GET /page`、`GET /list`、`GET /get/{id}`、`PUT /update-status`、`DELETE /delete/{id}` — 权限 `organization:org:query/update/delete`
- `OrganizationMemberController` `/org/member`：`GET /list`、`GET /page`、`POST /assign`（**批量把账号分配到组织**）、`POST /update-role`、`POST /remove` — 权限 `organization:member:query/assign/update/delete`

> "后台把用户批量分配给某组织" = `POST /org/member/assign`（super_admin 一次性把一批 account 纳入某组织），之后该组织主/子账号在 C 端自行维护之。

命名遵循 module-template-spec §4.1：类名只描述资源，app/admin 侧别由**包路径 + 路由前缀**区分（app 侧当前用户资源用 `...SelfController`，与 module-account 的 `AccountSelfController` 一致）。

## 6. 装配

- **独立仓**（external module），`settings.gradle.kts` `includeBuild` neton + geolite4k + module-{system,infra,account}，可独立编译。
- 聚合宿主（deviceadmin-backend / privchat-application）用 `include(":module-organization")` + `projectDir` 把本仓当源码子项目，并在 ksp `neton.modules` 追加 `organization`；**切勿**让本模块反向 `includeBuild` 宿主（成环）。
- 迁移内嵌：`@Module(migrations = true)` + `sql/postgresql/V*.sql`（V001 建表、V002 后台菜单与按钮权限种子，授 super_admin）。

## 7. 平台侧配对

一个模块对应多平台各有其模块（用户要求）：
- 后端：`neton-application-module-organization`（本仓）
- 后台管理 UI：`neton-application-front-organization`（super_admin：组织列表、成员、批量分配）
- 客户端 UI：`neton-application-client-organization`（主/子账号：创建组织、我的组织、成员邀请/管理）

## 8. 迁移与兼容

V001/V002 走框架迁移器（checksum 锁定，已应用不可改，新增用 V003+）。字段/角色码一经发布即冻结，扩展走新增列或新枚举值，不破坏既有坐标。
