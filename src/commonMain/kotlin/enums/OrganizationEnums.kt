package enums

/**
 * 组织状态。落库存 [code]。
 */
enum class OrgStatus(val code: Int) {
    DISABLED(0),
    ACTIVE(1),
    ;

    companion object {
        fun fromCode(code: Int): OrgStatus? = entries.firstOrNull { it.code == code }
    }
}

/**
 * 组织成员状态。落库存 [code]。
 */
enum class OrgMemberStatus(val code: Int) {
    DISABLED(0),
    ACTIVE(1),
    ;

    companion object {
        fun fromCode(code: Int): OrgMemberStatus? = entries.firstOrNull { it.code == code }
    }
}

/**
 * 组织内角色。落库存 [code]。
 *
 * [rank] 越大权限越高，用于"至少某角色"的最小角色校验（[OrgRole.atLeast]）：
 * OWNER(3) > ADMIN(2) > MEMBER(1)。
 */
enum class OrgRole(val code: Int, val rank: Int) {
    OWNER(1, 3),
    ADMIN(2, 2),
    MEMBER(3, 1),
    ;

    companion object {
        fun fromCode(code: Int): OrgRole? = entries.firstOrNull { it.code == code }

        /** [role] 是否达到 [min] 门槛（按 [rank] 比较）；[role] 为 null 视为不足。 */
        fun atLeast(role: Int?, min: OrgRole): Boolean {
            val r = role?.let { fromCode(it) } ?: return false
            return r.rank >= min.rank
        }
    }
}

/**
 * 成员进入组织的方式。落库存 [code]，仅作审计/展示，不参与鉴权。
 */
enum class OrgMemberSource(val code: Int) {
    OWNER(1),
    ADMIN_ASSIGNED(2),
    JOINED_BY_CODE(3),
    ADMIN_INVITED(4),
    ;

    companion object {
        fun fromCode(code: Int): OrgMemberSource? = entries.firstOrNull { it.code == code }
    }
}
