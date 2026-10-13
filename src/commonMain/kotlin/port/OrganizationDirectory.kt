package port

import enums.OrgRole
import model.OrganizationMember

/**
 * 组织目录（对外只读鉴权端口）。
 *
 * 业务模块（设备管理、privchat……）**不直接查 organization / organization_member 表**，
 * 而是注入本端口做"某账号在某组织里是什么角色 / 能不能操作"的判定。这是"组织通用、
 * 业务解耦"的关键接缝：组织不认识业务，业务通过这里反向消费组织。
 *
 * 由 [init.OrganizationRuntimeBootstrap] bind 一个实现（[logic.OrganizationDirectoryLogic]）；
 * 依赖组织的模块在自己的装配里 `ctx.get(OrganizationDirectory::class)` 取出使用。
 *
 * 实时/非 HTTP 链路（如 msgtrans 设备腿下行命令绕过 HTTP 鉴权）同样必须经本端口校验归属，
 * 否则就是跨组织越权。
 */
interface OrganizationDirectory {
    /** 返回 [accountId] 在 [orgId] 里的成员角色 code（未软删且状态正常）；不是有效成员返回 null。 */
    suspend fun getRole(orgId: Long, accountId: Long): Int?

    /** [accountId] 是否为 [orgId] 的有效成员。 */
    suspend fun isMember(orgId: Long, accountId: Long): Boolean

    /** [accountId] 在 [orgId] 里的角色是否达到 [min] 门槛（按 [OrgRole.rank] 比较）。 */
    suspend fun hasRole(orgId: Long, accountId: Long, min: OrgRole): Boolean

    /**
     * 断言 [accountId] 在 [orgId] 里至少是 [min] 角色；不满足抛
     * `HttpException(PERMISSION_DENIED)`（HTTP 403 语义）。
     */
    suspend fun requireRole(orgId: Long, accountId: Long, min: OrgRole)

    /** [accountId] 参与（有效成员）的全部组织 id。 */
    suspend fun listOrgIdsOf(accountId: Long): List<Long>

    /** [accountId] 作为主账号（OWNER）持有的全部组织 id。 */
    suspend fun listOwnedOrgIds(accountId: Long): List<Long>

    /** 列出 [orgId] 的全部有效成员（含角色，供上层做二次判定/展示）。 */
    suspend fun listMembers(orgId: Long): List<OrganizationMember>
}
