package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.SoftDelete
import neton.database.annotations.CreatedAt
import neton.database.annotations.UpdatedAt

/**
 * 组织成员：一个账号在某个组织里的成员身份与角色。
 *
 * 这就是"主账号添加子账号"的通用实现——所谓"子账号"不是新的账号类型，而是一个
 * account 被纳入组织并带上组织内角色（[role]，见 [enums.OrgRole]）。成员来源 [source]
 * 记录其如何进入组织（创建人 / 后台批量分配 / 邀请码加入 / 后台邀请）。
 *
 * 唯一约束在 (orgId, accountId)（未软删行，见 SQL 迁移的部分唯一索引）。
 */
@Serializable
@Table("organization_member")
data class OrganizationMember(
    @Id
    val id: Long = 0,
    /** 所属组织 id（[Organization.id]）。 */
    val orgId: Long,
    /** 成员账号 id（module-account 的 account.id）。 */
    val accountId: Long,
    /** 组织内角色：1 主 / 2 管理员 / 3 成员（见 [enums.OrgRole]）。 */
    val role: Int = 3,
    /** 成员状态：0 停用 / 1 正常（见 [enums.OrgMemberStatus]）。 */
    val status: Int = 1,
    /** 来源：1 创建人 / 2 后台分配 / 3 邀请码加入 / 4 后台邀请（见 [enums.OrgMemberSource]）。 */
    val source: Int = 0,
    /** 加入时刻（epoch 秒）。 */
    val joinedAt: Long? = null,
    @SoftDelete
    val deleted: Int = 0,
    @CreatedAt
    val createdAt: Long? = null,
    @UpdatedAt
    val updatedAt: Long? = null,
)
