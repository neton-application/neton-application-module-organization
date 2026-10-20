package controller.app.organization

import enums.OrgMemberSource
import enums.OrgRole
import kotlinx.serialization.Serializable
import logic.OrganizationLogic
import model.Organization
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.PathVariable
import neton.core.annotations.Body
import neton.core.http.NotFoundException
import neton.core.interfaces.Identity
import port.OrganizationDirectory
import port.OrganizationExtensions

@Serializable
data class CreateOrgReq(
    val name: String,
    val logo: String = "",
    val code: String = "",
    val remark: String = "",
)

@Serializable
data class UpdateOrgReq(
    val id: Long,
    val name: String? = null,
    val logo: String? = null,
    val code: String? = null,
    val remark: String? = null,
)

@Serializable
data class UpdateOrgStatusReq(
    val id: Long,
    val status: Int,
)

@Serializable
data class JoinOrgReq(
    val inviteCode: String,
)

/**
 * 组织·用户面（C 端 client 控制台调用，走 account 会话鉴权，当前账号来自 [Identity]）。
 *
 * 路由前缀 /app/org，包 controller.app.organization 表明"C 端"侧别（类名不带 App 前缀，
 * 遵循 module-template-spec §4.1）。权限模型：读=有效成员；写资料/停用/改成员=至少 ADMIN；
 * 删除=OWNER。鉴权统一走注入的 [OrganizationDirectory] 端口。
 */
@Controller("/org")
class OrganizationSelfController(
    private val organizationLogic: OrganizationLogic,
    private val memberLogic: logic.OrganizationMemberLogic,
    private val directory: OrganizationDirectory,
    private val extensions: OrganizationExtensions? = null,
) {
    /** 我创建 / 加入的组织列表（含我在其中的角色）。 */
    @Get("/list")
    suspend fun list(identity: Identity): List<OrganizationLogic.OrgBrief> =
        organizationLogic.listOrgsOf(identity.id.toLong())

    /** 取组织详情（仅成员可见）。 */
    @Get("/get/{id}")
    suspend fun get(identity: Identity, @PathVariable id: Long): Organization {
        directory.requireRole(id, identity.id.toLong(), OrgRole.MEMBER)
        return organizationLogic.get(id)
    }

    /** 创建组织：当前账号即主账号，自动登记为 OWNER 成员；随后尽力而为触发创建钩子。 */
    @Post("/create")
    suspend fun create(identity: Identity, @Body req: CreateOrgReq): Organization {
        val ownerAccountId = identity.id.toLong()
        val org = organizationLogic.create(
            name = req.name,
            ownerAccountId = ownerAccountId,
            logo = req.logo,
            code = req.code,
            remark = req.remark,
        )
        extensions?.creationHooks()?.forEach { runCatching { it.afterCreated(org, ownerAccountId) } }
        return org
    }

    /** 修改组织资料（至少 ADMIN）。 */
    @Put("/update")
    suspend fun update(identity: Identity, @Body req: UpdateOrgReq): Organization {
        directory.requireRole(req.id, identity.id.toLong(), OrgRole.ADMIN)
        return organizationLogic.update(req.id, req.name, req.logo, req.code, req.remark)
    }

    /** 启停组织（至少 ADMIN）。 */
    @Put("/update-status")
    suspend fun updateStatus(identity: Identity, @Body req: UpdateOrgStatusReq): Organization {
        directory.requireRole(req.id, identity.id.toLong(), OrgRole.ADMIN)
        return organizationLogic.updateStatus(req.id, req.status)
    }

    /** 重置邀请码（至少 ADMIN），返回新码。 */
    @Post("/reset-invite-code")
    suspend fun resetInviteCode(identity: Identity, id: Long): String {
        directory.requireRole(id, identity.id.toLong(), OrgRole.ADMIN)
        return organizationLogic.resetInviteCode(id)
    }

    /** 凭邀请码把当前账号加入组织（幂等；已在则按需保持）。 */
    @Post("/join")
    suspend fun join(identity: Identity, @Body req: JoinOrgReq): Organization {
        val org = organizationLogic.getByInviteCode(req.inviteCode)
            ?: throw NotFoundException("邀请码无效")
        val accountId = identity.id.toLong()
        memberLogic.add(org.id, accountId, OrgRole.MEMBER.code, OrgMemberSource.JOINED_BY_CODE.code)
        extensions?.memberChangedHooks()?.forEach { runCatching { it.afterMembersChanged(org.id, listOf(accountId)) } }
        return org
    }

    /** 删除组织（仅 OWNER）：级联软删成员；业务归属由消费方自理。 */
    @Delete("/delete/{id}")
    suspend fun delete(identity: Identity, @PathVariable id: Long) {
        directory.requireRole(id, identity.id.toLong(), OrgRole.OWNER)
        organizationLogic.delete(id)
    }
}
