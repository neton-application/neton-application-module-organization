-- organization 模块（postgresql）—— 1.0.0 基线建表。
--
-- 账号之上的通用组织地基：organization（组织本体，含主账号 owner_account_id）+
-- organization_member（成员=account 账号在该组织的身份与角色）。业务模块只引用 organization.id
-- 做归属，本模块不含任何业务列。
--   * 时间列一律 epoch（BIGINT，可空，由框架 @CreatedAt/@UpdatedAt 写入时填充）。
--   * 是/否与状态一律 SMALLINT 0/1 或多值码，列名 snake_case，与数据库 DSL 列映射一致。
--   * 唯一约束一律"部分唯一索引 WHERE deleted = 0"，软删后可复用同键。
--
-- 🔴 只给全新数据库用。已应用的 V001 不可编辑（迁移器按 checksum 校验，改动会拒绝启动）；
--    加东西请新增 V002、V003…。

-- ===================
-- 组织本体
-- ===================
CREATE TABLE IF NOT EXISTS organization (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(128) NOT NULL,               -- 组织名称
    logo             VARCHAR(255) NOT NULL DEFAULT '',    -- 组织头像（infra 文件 fileId）
    code             VARCHAR(64)  NOT NULL DEFAULT '',    -- 全局唯一组织编码（可空串=未设置）
    invite_code      VARCHAR(32)  NOT NULL DEFAULT '',    -- 邀请码：成员凭此码自助加入
    owner_account_id BIGINT       NOT NULL,               -- 主账号（创建人）account.id
    status           SMALLINT     NOT NULL DEFAULT 1,     -- 0 停用 / 1 正常
    remark           VARCHAR(512) NOT NULL DEFAULT '',    -- 简介 / 备注
    deleted          SMALLINT     NOT NULL DEFAULT 0,
    created_at       BIGINT,
    updated_at       BIGINT
);
-- 组织编码非空时全局唯一（仅未软删行）
CREATE UNIQUE INDEX IF NOT EXISTS idx_organization_code
    ON organization (code) WHERE deleted = 0 AND code <> '';
-- 邀请码唯一（仅未软删且非空）
CREATE UNIQUE INDEX IF NOT EXISTS idx_organization_invite_code
    ON organization (invite_code) WHERE deleted = 0 AND invite_code <> '';
CREATE INDEX IF NOT EXISTS idx_organization_owner ON organization (owner_account_id);

-- ===================
-- 组织成员（成员=account 账号在该组织的身份）
-- ===================
CREATE TABLE IF NOT EXISTS organization_member (
    id         BIGSERIAL PRIMARY KEY,
    org_id     BIGINT   NOT NULL,                          -- organization.id
    account_id BIGINT   NOT NULL,                          -- account.id（成员账号）
    role       SMALLINT NOT NULL DEFAULT 3,                -- 1 主 / 2 管理员 / 3 成员
    status     SMALLINT NOT NULL DEFAULT 1,                -- 0 停用 / 1 正常
    source     SMALLINT NOT NULL DEFAULT 0,                -- 1 创建人 / 2 后台分配 / 3 邀请码 / 4 后台邀请
    joined_at  BIGINT,                                     -- 加入时刻（epoch 秒）
    deleted    SMALLINT NOT NULL DEFAULT 0,
    created_at BIGINT,
    updated_at BIGINT
);
-- 一个账号在一个组织里只有一条有效成员行
CREATE UNIQUE INDEX IF NOT EXISTS idx_org_member_org_account
    ON organization_member (org_id, account_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_org_member_account ON organization_member (account_id);
CREATE INDEX IF NOT EXISTS idx_org_member_org ON organization_member (org_id);
