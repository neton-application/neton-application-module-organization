package logic

import enums.OrgMemberSource
import enums.OrgRole
import enums.OrgStatus
import kotlinx.serialization.Serializable
import model.Account
import model.OrganizationMember
import neton.core.annotations.Logic
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.database.dsl.*
import neton.logging.Logger
import table.AccountTable
import table.OrganizationMemberTable

/**
 * 组织成员业务逻辑：加入 / 批量分配 / 移除 / 改角色 / 查询。
 *
 * "主账号添加子账号""后台批量把用户分配到组织"都收敛到这里：成员就是 account 的一个
 * 组织内身份（角色 + 来源）。跨模块读取 [Account] 档案做成员投影（与 module-member 直接
 * 用 AccountTable 的口径一致）。
 */
@Logic(logger = "logic.organization.member")
class OrganizationMemberLogic(
    private val log: Logger,
) {
    /** 成员投影：成员行 + 账号昵称/头像。 */
    @Serializable
    data class OrgMemberView(
        val orgId: Long,
        val accountId: Long,
        val role: Int,
        val status: Int,
        val source: Int,
        val joinedAt: Long?,
        val nickname: String,
        val avatar: String?,
    )

    private fun nowEpoch(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000

    private fun requireRoleCode(role: Int): OrgRole =
        OrgRole.fromCode(role) ?: throw BadRequestException("Invalid org role: $role")

    private suspend fun findActive(orgId: Long, accountId: Long): OrganizationMember? =
        OrganizationMemberTable.oneWhere {
            and(OrganizationMember::orgId eq orgId, OrganizationMember::accountId eq accountId)
        }

    /**
     * 加入 / 更新成员。已是成员则按需刷新角色与状态（幂等）；否则新建。
     * OWNER 角色不由这里直接授予——组织创建人在 [OrganizationLogic.create] 里落 OWNER，
     * 转移主账号是单独操作。
     */
    suspend fun add(
        orgId: Long,
        accountId: Long,
        role: Int = OrgRole.MEMBER.code,
        source: Int = OrgMemberSource.ADMIN_ASSIGNED.code,
    ): OrganizationMember {
        requireRoleCode(role)
        if (role == OrgRole.OWNER.code) throw BadRequestException("OWNER 角色不能通过成员接口直接授予")
        val existing = findActive(orgId, accountId)
        return if (existing == null) {
            OrganizationMemberTable.insert(
                OrganizationMember(
                    orgId = orgId,
                    accountId = accountId,
                    role = role,
                    status = OrgStatus.ACTIVE.code,
                    source = source,
                    joinedAt = nowEpoch(),
                )
            )
        } else {
            val updated = existing.copy(role = role, status = OrgStatus.ACTIVE.code)
            OrganizationMemberTable.update(updated)
            updated
        }
    }

    /**
     * 批量分配账号到组织（后台"把用户批量分配给某组织"）。已存在的成员按需更新角色，
     * 不存在的创建为新成员。返回本次涉及的账号数。
     */
    suspend fun assignMany(
        orgId: Long,
        accountIds: List<Long>,
        role: Int = OrgRole.MEMBER.code,
        source: Int = OrgMemberSource.ADMIN_ASSIGNED.code,
    ): Int {
        requireRoleCode(role)
        if (role == OrgRole.OWNER.code) throw BadRequestException("OWNER 角色不能批量分配")
        accountIds.forEach { add(orgId, it, role, source) }
        return accountIds.size
    }

    suspend fun updateRole(orgId: Long, accountId: Long, role: Int): OrganizationMember {
        requireRoleCode(role)
        if (role == OrgRole.OWNER.code) throw BadRequestException("OWNER 角色变更请用主账号转移接口")
        val existing = findActive(orgId, accountId)
            ?: throw NotFoundException("Member not found: org=$orgId account=$accountId")
        val updated = existing.copy(role = role)
        OrganizationMemberTable.update(updated)
        return updated
    }

    /** 移除成员（软删）。不允许移除 OWNER。 */
    suspend fun remove(orgId: Long, accountId: Long) {
        val existing = findActive(orgId, accountId) ?: return
        if (existing.role == OrgRole.OWNER.code) throw BadRequestException("不能移除组织主账号")
        OrganizationMemberTable.destroy(existing.id)
    }

    suspend fun getRole(orgId: Long, accountId: Long): Int? {
        val m = findActive(orgId, accountId) ?: return null
        return if (m.status == OrgStatus.ACTIVE.code) m.role else null
    }

    suspend fun isMember(orgId: Long, accountId: Long): Boolean = getRole(orgId, accountId) != null

    suspend fun hasRole(orgId: Long, accountId: Long, min: OrgRole): Boolean =
        OrgRole.atLeast(getRole(orgId, accountId), min)

    suspend fun listMembers(orgId: Long): List<OrganizationMember> =
        OrganizationMemberTable.query {
            where { OrganizationMember::orgId eq orgId }
            orderBy(OrganizationMember::id.asc())
        }.list()

    suspend fun countMembers(orgId: Long): Long =
        OrganizationMemberTable.query { where { OrganizationMember::orgId eq orgId } }.count()

    suspend fun pageMembers(orgId: Long, pageNo: Int, size: Int) =
        OrganizationMemberTable.query {
            where { OrganizationMember::orgId eq orgId }
            orderBy(OrganizationMember::id.asc())
        }.page(pageNo, size)

    /** 成员 + 账号档案投影（按 accountId 批量捞 account，缺失档案的成员被跳过）。 */
    suspend fun listMembersWithProfile(orgId: Long): List<OrgMemberView> {
        val members = listMembers(orgId).filter { it.status == OrgStatus.ACTIVE.code }
        if (members.isEmpty()) return emptyList()
        val accounts = AccountTable.query { where { Account::id `in` members.map { it.accountId } } }.list()
        val accById = accounts.associateBy { it.id }
        return members.mapNotNull { m ->
            accById[m.accountId]?.let { a ->
                OrgMemberView(
                    orgId = m.orgId,
                    accountId = m.accountId,
                    role = m.role,
                    status = m.status,
                    source = m.source,
                    joinedAt = m.joinedAt,
                    nickname = a.nickname,
                    avatar = a.avatar,
                )
            }
        }
    }

    suspend fun listOrgIdsOf(accountId: Long): List<Long> =
        OrganizationMemberTable.query {
            where { OrganizationMember::accountId eq accountId }
        }.list().filter { it.status == OrgStatus.ACTIVE.code }.map { it.orgId }

    suspend fun listOwnedOrgIds(accountId: Long): List<Long> =
        OrganizationMemberTable.query {
            where { and(OrganizationMember::accountId eq accountId, OrganizationMember::role eq OrgRole.OWNER.code) }
        }.list().map { it.orgId }
}
