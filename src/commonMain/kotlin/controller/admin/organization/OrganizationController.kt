package controller.admin.organization

import kotlinx.serialization.Serializable
import logic.OrganizationLogic
import model.Organization
import neton.core.annotations.Body
import neton.core.annotations.Controller
import neton.core.annotations.Delete
import neton.core.annotations.Get
import neton.core.annotations.PathVariable
import neton.core.annotations.Permission
import neton.core.annotations.Put
import neton.core.annotations.Query

@Serializable
data class AdminUpdateOrgStatusReq(
    val id: Long,
    val status: Int,
)

/**
 * 组织·后台管理（B 端管理台调用，走 system 管理员鉴权 + RBAC）。
 *
 * 路由前缀 /org，包 controller.admin.organization 表明后台侧别。权限串 organization:org:query /
 * :update / :delete。后台做的是全局治理（列表 / 详情 / 启停 / 删除 / 批量分配成员），
 * 不介入某组织内部的日常成员管理（那是 C 端主/子账号的事）。
 */
@Controller("/org")
class OrganizationController(
    private val organizationLogic: OrganizationLogic,
) {
    @Get("/page")
    @Permission("organization:org:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 20,
        @Query name: String? = null,
    ) = organizationLogic.page(pageNo, pageSize, name)

    @Get("/list")
    @Permission("organization:org:query")
    suspend fun list(): List<Organization> = organizationLogic.listAll()

    @Get("/get/{id}")
    @Permission("organization:org:query")
    suspend fun get(@PathVariable id: Long): Organization = organizationLogic.get(id)

    @Put("/update-status")
    @Permission("organization:org:update")
    suspend fun updateStatus(@Body body: AdminUpdateOrgStatusReq): Organization =
        organizationLogic.updateStatus(body.id, body.status)

    @Delete("/delete/{id}")
    @Permission("organization:org:delete")
    suspend fun delete(@PathVariable id: Long) = organizationLogic.delete(id)
}
