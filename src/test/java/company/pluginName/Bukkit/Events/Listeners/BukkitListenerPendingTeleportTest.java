package company.pluginName.Bukkit.Events.Listeners;

public final class BukkitListenerPendingTeleportTest {

	public static void main(String[] args) {
		assert !BukkitListener.isPendingTeleportExpired(1_000L, 2_000L, 1_000L);
		assert BukkitListener.isPendingTeleportExpired(1_000L, 2_001L, 1_000L);
		assert BukkitListener.isPendingTeleportExpired(0L, 2_000L, 1_000L);
	}
}
