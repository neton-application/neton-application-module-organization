package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.SoftDelete
import neton.database.annotations.CreatedAt
import neton.database.annotations.UpdatedAt

/**
 * 组织：账号之上的通用分组地基（像钉钉里"我创建的组织"），与具体业务无关。
 *
 * 一个组织由一个**主账号**（[ownerAccountId]，创建人）持有，下面挂若干**成员**
 * （[OrganizationMember]，成员就是 module-account 的账号）。业务模块（设备管理、privchat
 * 工作组……）只引用 [id] 把自己的资源归属到某个组织，组织本身不认识任何业务。
 *
 * 时间字段用 epoch（Long），与其余 Neton 表一致；yes/no 一律 0/1。
 */
@Serializable
@Table("organization")
data class Organization(
    @Id
    val id: Long = 0,
    /** 组织名称。 */
    val name: String,
    /** 组织头像（infra 文件 fileId，空串表示未设置）。 */
    val logo: String = "",
    /** 组织编码：全局唯一的短标识（可用于对外接入 / 检索），空串表示未设置。 */
    val code: String = "",
    /**
     * 邀请码：成员凭此码自助加入组织。创建时生成，可重置。
     * 建组织后不为空（[logic.OrganizationLogic.create] 保证）。
     */
    val inviteCode: String = "",
    /** 主账号：创建并持有该组织的账号 id（module-account 的 account.id）。 */
    val ownerAccountId: Long,
    /** 组织状态：0 停用 / 1 正常（见 [enums.OrgStatus]）。 */
    val status: Int = 1,
    /** 组织简介 / 备注。 */
    val remark: String = "",
    @SoftDelete
    val deleted: Int = 0,
    @CreatedAt
    val createdAt: Long? = null,
    @UpdatedAt
    val updatedAt: Long? = null,
)
