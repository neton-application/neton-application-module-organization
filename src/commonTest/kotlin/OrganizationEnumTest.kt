package

import enums.OrgRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 组织角色 rank 比较的最小校验（纯逻辑，无 DB / 无 reactor）。 */
class OrganizationEnumTest {
    @Test
    fun roleRankOrdering() {
        assertTrue(OrgRole.atLeast(OrgRole.OWNER.code, OrgRole.ADMIN))
        assertTrue(OrgRole.atLeast(OrgRole.ADMIN.code, OrgRole.ADMIN))
        assertFalse(OrgRole.atLeast(OrgRole.MEMBER.code, OrgRole.ADMIN))
        assertFalse(OrgRole.atLeast(null, OrgRole.MEMBER))
        assertEquals(OrgRole.MEMBER, OrgRole.fromCode(3))
    }
}
