package logic

import enums.OrgMemberSource
import enums.OrgRole
import enums.OrgStatus
import dto.PageResponse
import kotlin.random.Random
import kotlinx.serialization.Serializable
import model.Organization
import model.OrganizationMember
import neton.core.annotations.Logic
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.database.dsl.*
import neton.logging.Logger
import table.OrganizationMemberTable
import table.OrganizationTable

/**
 * 组织业务逻辑：创建 / 查询 / 改名 / 停用 / 删除，以及邀请码管理。
 *
 * 建组织时把创建人自动登记为该组织的 OWNER 成员（同事务内两次写，失败即整体失败）。
 * 组织不认识任何业务，业务归属通过 [OrganizationTable] 主键 [Organization.id] 被外部引用。
 */
@Logic(logger = "logic.organization")
class OrganizationLogic(
    private val log: Logger,
) {
    /** 带账号昵称/头像投影的组织简要（供"我的组织"列表展示）。 */
    @Serializable
    data class OrgBrief(
        val id: Long,
        val name: String,
        val logo: String,
        val role: Int,
        val ownerAccountId: Long,
        val status: Int,
    )

    private fun nowEpoch(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds() / 1000

    private fun newInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString { repeat(10) { append(alphabet[Random.nextInt(alphabet.length)]) } }
    }

    /**
     * 创建组织：建 [Organization] + 把 [ownerAccountId] 登记为 OWNER 成员。
     * [code] 为空表示不设组织编码；非空则要求全局唯一（未软删）。
     */
    suspend fun create(
        name: String,
        ownerAccountId: Long,
        logo: String = "",
        code: String = "",
        remark: String = "",
    ): Organization {
        if (name.isBlank()) throw BadRequestException("组织名称不能为空")
        if (code.isNotBlank() && OrganizationTable.existsWhere { Organization::code eq code }) {
            throw BadRequestException("组织编码已存在：$code")
        }
        val now = nowEpoch()
        val org = OrganizationTable.insert(
            Organization(
                name = name.trim(),
                logo = logo,
                code = code.trim(),
                inviteCode = newInviteCode(),
                ownerAccountId = ownerAccountId,
                status = OrgStatus.ACTIVE.code,
                remark = remark,
            )
        )
        OrganizationMemberTable.insert(
            OrganizationMember(
                orgId = org.id,
                accountId = ownerAccountId,
                role = OrgRole.OWNER.code,
                status = OrgStatus.ACTIVE.code,
                source = OrgMemberSource.OWNER.code,
                joinedAt = now,
            )
        )
        return org
    }

    suspend fun getByIdOrNull(id: Long): Organization? = OrganizationTable.get(id)

    suspend fun get(id: Long): Organization =
        OrganizationTable.get(id) ?: throw NotFoundException("Organization not found: $id")

    suspend fun getByInviteCode(code: String): Organization? =
        if (code.isBlank()) null else OrganizationTable.oneWhere { Organization::inviteCode eq code }

    suspend fun getByCode(code: String): Organization? =
        if (code.isBlank()) null else OrganizationTable.oneWhere { Organization::code eq code }

    suspend fun page(pageNo: Int, pageSize: Int, name: String? = null): PageResponse<Organization> {
        val result = OrganizationTable.query {
            if (!name.isNullOrBlank()) where { Organization::name like "%$name%" }
            orderBy(Organization::id.desc())
        }.page(pageNo, pageSize)
        return PageResponse(
            result.items, result.total, pageNo, pageSize,
            if (pageSize > 0) ((result.total + pageSize - 1) / pageSize).toInt() else 0,
        )
    }

    suspend fun listAll(): List<Organization> =
        OrganizationTable.query { orderBy(Organization::id.desc()) }.list()

    /** 更新组织资料（名称/头像/编码/简介）；不改状态与主账号。 */
    suspend fun update(
        id: Long,
        name: String? = null,
        logo: String? = null,
        code: String? = null,
        remark: String? = null,
    ): Organization {
        val existing = get(id)
        val nextCode = code?.trim() ?: existing.code
        if (nextCode.isNotBlank() && nextCode != existing.code &&
            OrganizationTable.existsWhere { Organization::code eq nextCode }
        ) {
            throw BadRequestException("组织编码已存在：$nextCode")
        }
        val updated = existing.copy(
            name = name?.trim()?.takeIf { it.isNotEmpty() } ?: existing.name,
            logo = logo ?: existing.logo,
            code = nextCode,
            remark = remark ?: existing.remark,
        )
        OrganizationTable.update(updated)
        return updated
    }

    suspend fun updateStatus(id: Long, status: Int): Organization {
        require(OrgStatus.fromCode(status) != null) { "Invalid org status: $status" }
        val existing = get(id)
        val updated = existing.copy(status = status)
        OrganizationTable.update(updated)
        return updated
    }

    /** 重置邀请码，返回新码。 */
    suspend fun resetInviteCode(id: Long): String {
        val existing = get(id)
        val newCode = newInviteCode()
        OrganizationTable.update(existing.copy(inviteCode = newCode))
        return newCode
    }

    /**
     * 删除组织：软删组织本体，并级联软删其全部成员。
     * 业务归属（设备/工作组）由消费方自行决定如何处理，组织不反向清理业务数据。
     */
    suspend fun delete(id: Long) {
        val existing = get(id)
        val members = OrganizationMemberTable.query {
            where { OrganizationMember::orgId eq existing.id }
        }.list()
        members.forEach { OrganizationMemberTable.destroy(it.id) }
        OrganizationTable.destroy(existing.id)
    }

    /**
     * 账号"我的组织"列表（含在该组织里的角色），按参与时间倒序。
     * 先取成员行拿到 orgId+role，再批量捞组织档案组装投影。
     */
    suspend fun listOrgsOf(accountId: Long): List<OrgBrief> {
        val memberships = OrganizationMemberTable.query {
            where { OrganizationMember::accountId eq accountId }
        }.list()
        if (memberships.isEmpty()) return emptyList()
        val orgIds = memberships.map { it.orgId }
        val orgs = OrganizationTable.query { where { Organization::id `in` orgIds } }.list()
        val orgById = orgs.associateBy { it.id }
        return memberships
            .mapNotNull { m -> orgById[m.orgId]?.let { o ->
                OrgBrief(
                    id = o.id,
                    name = o.name,
                    logo = o.logo,
                    role = m.role,
                    ownerAccountId = o.ownerAccountId,
                    status = o.status,
                )
            } }
            .sortedByDescending { it.id }
    }
}
