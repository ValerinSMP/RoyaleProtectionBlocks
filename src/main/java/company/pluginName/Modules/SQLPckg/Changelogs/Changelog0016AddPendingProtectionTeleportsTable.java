package company.pluginName.Modules.SQLPckg.Changelogs;

import java.sql.Types;

import darkpanda73.PandaUtils.PandaSQLModule.v2.SQL.Objects.Changelogs.SQLChangelog;
import darkpanda73.PandaUtils.PandaSQLModule.v2.SQL.Objects.Statements.CreateTableStatement;
import darkpanda73.PandaUtils.PandaSQLModule.v2.SQL.Objects.Tables.Column;
import darkpanda73.PandaUtils.PandaSQLModule.v2.SQL.Objects.Tables.Table;

public class Changelog0016AddPendingProtectionTeleportsTable extends SQLChangelog {

	public static final Table PENDING_PROTECTION_TELEPORTS_TABLE = new Table("PendingProtectionTeleports").addColumns(
			new Column("PlayerUuid", Types.CHAR, "CHAR(36)").setPrimary(true).setNotNull(true),
			new Column("RegionId", Types.VARCHAR, "VARCHAR(256)").setNotNull(true),
			new Column("CreatedDate", Types.BIGINT).setNotNull(true));

	public Changelog0016AddPendingProtectionTeleportsTable() {
		super(16, "Adding pending protection teleports table", sqlConnection -> sqlConnection
				.executeCreateTable(CreateTableStatement.inst(PENDING_PROTECTION_TELEPORTS_TABLE)));
	}
}
