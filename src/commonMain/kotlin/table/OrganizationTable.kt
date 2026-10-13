package table

import model.Organization
import model.OrganizationTableImpl
import neton.database.api.Table

object OrganizationTable : Table<Organization, Long> by OrganizationTableImpl
