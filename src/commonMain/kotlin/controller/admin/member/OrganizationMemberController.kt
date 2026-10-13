package controller.admin.member

import enums.OrgMemberSource
import enums.OrgRole
import kotlinx.serialization.Serializable
import logic.OrganizationMemberLogic
import model.OrganizationMember
import neton.core.annotations.Body
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
import neton.core.annotations.Post

@Serializable
data class AssignMembersReq(
    val orgId: Long,
    /** 待分配到组织的账号 id 集合（后台"把用户批量分配给某组织"）。 */
    val accountIds: List<Long>,
    val role: Int = OrgRole.MEMBER.code,
)

@Serializable
data class AdminUpdateMemberRoleReq(
    val orgId: Long,
    val accountId: Long,
    val role: Int,
)

@Serializable
data class AdminRemoveMemberReq(
    val orgId: Long,
    val accountId: Long,
)

/**
 * 组织成员·后台管理（B 端管理台调用，走 system 管理员鉴权 + RBAC）。
 *
 * 路由前缀 /org/member。核心能力是"批量分配"：super_admin 把一批账号一次性纳入某个组织，
 * 之后该组织的主账号 / 管理员就能在 C 端自行维护这些成员。权限串 organization:member:query /
 * :assign / :update / :delete。
 */
@Controller("/org/member")
class OrganizationMemberController(
    private val memberLogic: OrganizationMemberLogic,
) {
    @Get("/list")
    @Permission("organization:member:query")
    suspend fun list(orgId: Long): List<OrganizationMemberLogic.OrgMemberView> =
        memberLogic.listMembersWithProfile(orgId)

    @Get("/page")
    @Permission("organization:member:query")
    suspend fun page(orgId: Long, page: Int = 1, size: Int = 20) =
        memberLogic.pageMembers(orgId, page, size)

    /** 批量把账号分配到组织（已是成员则按需更新角色）。返回涉及账号数。 */
    @Post("/assign")
    @Permission("organization:member:assign")
    suspend fun assign(@Body req: AssignMembersReq): Int {
        val n = memberLogic.assignMany(req.orgId, req.accountIds, req.role, OrgMemberSource.ADMIN_ASSIGNED.code)
        return n
    }

    @Post("/update-role")
    @Permission("organization:member:update")
    suspend fun updateRole(@Body req: AdminUpdateMemberRoleReq): OrganizationMember =
        memberLogic.updateRole(req.orgId, req.accountId, req.role)

    @Post("/remove")
    @Permission("organization:member:delete")
    suspend fun remove(@Body req: AdminRemoveMemberReq) =
        memberLogic.remove(req.orgId, req.accountId)
}
