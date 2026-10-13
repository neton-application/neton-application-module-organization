package logic

import enums.OrgRole
import neton.core.http.HttpException
import neton.core.http.NetonErrorCode
import port.OrganizationDirectory

/**
 * [OrganizationDirectory] 的默认实现：把端口方法委托给 [OrganizationMemberLogic]。
 *
 * 不是 `@Logic`（避免被自动装配成 logic 与端口混淆），由
 * [init.OrganizationRuntimeBootstrap] 取出已注册的 [OrganizationMemberLogic] 后手动 bind
 * 到 [OrganizationDirectory] 坐标，供任意消费方（含设备腿的非 HTTP 链路）注入使用。
 */
class OrganizationDirectoryLogic(
    private val memberLogic: OrganizationMemberLogic,
) : OrganizationDirectory {

    override suspend fun getRole(orgId: Long, accountId: Long): Int? =
        memberLogic.getRole(orgId, accountId)

    override suspend fun isMember(orgId: Long, accountId: Long): Boolean =
        memberLogic.isMember(orgId, accountId)

    override suspend fun hasRole(orgId: Long, accountId: Long, min: OrgRole): Boolean =
        memberLogic.hasRole(orgId, accountId, min)

    override suspend fun requireRole(orgId: Long, accountId: Long, min: OrgRole) {
        if (!memberLogic.hasRole(orgId, accountId, min)) {
            throw HttpException(
                NetonErrorCode.PERMISSION_DENIED,
                "需要组织 $orgId 的至少 ${min.name} 角色（账号 $accountId 权限不足）",
            )
        }
    }

    override suspend fun listOrgIdsOf(accountId: Long): List<Long> =
        memberLogic.listOrgIdsOf(accountId)

    override suspend fun listOwnedOrgIds(accountId: Long): List<Long> =
        memberLogic.listOwnedOrgIds(accountId)

    override suspend fun listMembers(orgId: Long) = memberLogic.listMembers(orgId)
}
