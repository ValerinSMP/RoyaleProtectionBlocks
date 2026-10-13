package company.pluginName.Modules.PlayerInteractionsPckg;

import company.pluginName.Exceptions.Exceptions;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaFieldContainer;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException.Type;

@RegisteredPandaFieldContainer("lang")
public final class ReadOnlyFeedback {
    private ReadOnlyFeedback() {}

    public static final Exceptions.ThrowableField READ_ONLY = new Exceptions.ThrowableField(
        Type.PROTECTIONS_PERMISSIONDENIED, "Error.Exception.protection.ReadOnly",
        "&#FF3300No puedes modificar protecciones desde este servidor: están en modo de solo lectura.");
}
