package table

import model.OrganizationMember
import model.OrganizationMemberTableImpl
import neton.database.api.Table

object OrganizationMemberTable : Table<OrganizationMember, Long> by OrganizationMemberTableImpl
