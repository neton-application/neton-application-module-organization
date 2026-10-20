package controller.app.member

import enums.OrgMemberSource
import enums.OrgRole
import kotlinx.serialization.Serializable
import logic.OrganizationMemberLogic
import model.OrganizationMember
import neton.core.annotations.Body
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.interfaces.Identity
import port.OrganizationDirectory

@Serializable
data class AddMemberReq(
    val orgId: Long,
    val accountId: Long,
    val role: Int = OrgRole.MEMBER.code,
)

@Serializable
data class UpdateMemberRoleReq(
    val orgId: Long,
    val accountId: Long,
    val role: Int,
)

@Serializable
data class RemoveMemberReq(
    val orgId: Long,
    val accountId: Long,
)

/**
 * 组织成员·用户面（主账号 / 管理员在自己的组织里增删改子账号）。
 *
 * 路由前缀 /app/org/member。"添加子账号"= 把一个已存在的 account 纳入组织并赋予成员角色，
 * 不是新造账号类型（账号本体由 module-account 负责）。读=成员；增删改角色=至少 ADMIN。
 */
@Controller("/org/member")
class OrganizationMemberSelfController(
    private val memberLogic: OrganizationMemberLogic,
    private val directory: OrganizationDirectory,
    private val extensions: port.OrganizationExtensions? = null,
) {
    /** 成员列表（含账号昵称/头像投影），仅成员可见。 */
    @Get("/list")
    suspend fun list(identity: Identity, orgId: Long): List<OrganizationMemberLogic.OrgMemberView> {
        directory.requireRole(orgId, identity.id.toLong(), OrgRole.MEMBER)
        return memberLogic.listMembersWithProfile(orgId)
    }

    /** 添加 / 更新成员（至少 ADMIN；不能直接授 OWNER）。 */
    @Post("/add")
    suspend fun add(identity: Identity, @Body req: AddMemberReq): OrganizationMember {
        directory.requireRole(req.orgId, identity.id.toLong(), OrgRole.ADMIN)
        val member = memberLogic.add(req.orgId, req.accountId, req.role, OrgMemberSource.ADMIN_INVITED.code)
        extensions?.memberChangedHooks()?.forEach { runCatching { it.afterMembersChanged(req.orgId, listOf(req.accountId)) } }
        return member
    }

    /** 变更成员角色（至少 ADMIN）。 */
    @Put("/update-role")
    suspend fun updateRole(identity: Identity, @Body req: UpdateMemberRoleReq): OrganizationMember {
        directory.requireRole(req.orgId, identity.id.toLong(), OrgRole.ADMIN)
        val member = memberLogic.updateRole(req.orgId, req.accountId, req.role)
        extensions?.memberChangedHooks()?.forEach { runCatching { it.afterMembersChanged(req.orgId, listOf(req.accountId)) } }
        return member
    }

    /** 移除成员（至少 ADMIN，不能移除 OWNER）。 */
    @Post("/remove")
    suspend fun remove(identity: Identity, @Body req: RemoveMemberReq) {
        directory.requireRole(req.orgId, identity.id.toLong(), OrgRole.ADMIN)
        memberLogic.remove(req.orgId, req.accountId)
        extensions?.memberChangedHooks()?.forEach { runCatching { it.afterMembersChanged(req.orgId, listOf(req.accountId)) } }
    }
}
