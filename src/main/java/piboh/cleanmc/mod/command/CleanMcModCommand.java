package piboh.cleanmc.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import piboh.cleanmc.mod.CleanMcModClient;
import piboh.cleanmc.mod.clean.CacheScan;
import piboh.cleanmc.mod.clean.CacheTarget;
import piboh.cleanmc.mod.clean.CleanMcModService;
import piboh.cleanmc.mod.clean.CleanReport;
import piboh.cleanmc.mod.clean.TargetResult;
import piboh.cleanmc.mod.clean.TargetState;
import piboh.cleanmc.mod.ui.CleanMenuScreen;
import piboh.cleanmc.mod.ui.Texts;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-only command: {@code /cleanmcmod scan|clean|screen}.
 *
 * <p>Registered through the client command API, so it never exists on a server and needs no
 * permissions.</p>
 */
public final class CleanMcModCommand {

    private CleanMcModCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("cleanmcmod")
                .executes(context -> help(context.getSource()))
                .then(ClientCommandManager.literal("scan")
                        .executes(context -> scan(context.getSource())))
                .then(ClientCommandManager.literal("clean")
                        .executes(context -> clean(context.getSource())))
                .then(ClientCommandManager.literal("screen")
                        .executes(context -> openScreen(context.getSource()))));
    }

    private static int help(FabricClientCommandSource source) {
        source.sendFeedback(Text.translatable("cleanmcmod.command.help"));
        return 1;
    }

    private static int scan(FabricClientCommandSource source) {
        CleanMcModService service = CleanMcModClient.service();
        CacheScan scan = service.scan();
        source.sendFeedback(Text.translatable("cleanmcmod.command.scan.header",
                scan.presentCount(), scan.files(), CleanReport.formatBytes(scan.bytes())));
        if (scan.empty()) {
            source.sendFeedback(Text.translatable("cleanmcmod.command.scan.empty"));
            return 1;
        }
        for (TargetState state : scan.presentTargets()) {
            source.sendFeedback(Text.literal(" • ")
                    .append(Texts.targetName(state.target()))
                    .append(Text.literal(" — " + state.files() + " · " + CleanReport.formatBytes(state.bytes()))));
        }
        return 1;
    }

    private static int clean(FabricClientCommandSource source) {
        CleanMcModService service = CleanMcModClient.service();
        if (service.isRunning()) {
            source.sendFeedback(Text.translatable("cleanmcmod.command.running"));
            return 0;
        }
        boolean dryRun = service.config().dryRun;
        source.sendFeedback(Text.translatable(dryRun
                ? "cleanmcmod.command.clean.started_dry"
                : "cleanmcmod.command.clean.started"));

        Map<String, CacheTarget> targetsById = new HashMap<>();
        List<CacheTarget> targets = service.targets();
        for (CacheTarget target : targets) {
            targetsById.put(target.id(), target);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        service.cleanAsync().thenAccept(report -> client.execute(() -> {
            if (report == null) {
                source.sendFeedback(Text.translatable("cleanmcmod.command.clean.failed"));
                return;
            }
            source.sendFeedback(report.dryRun()
                    ? Text.translatable("cleanmcmod.command.clean.done_dry",
                            report.cleanedTargets(), report.filesPlanned(),
                            CleanReport.formatBytes(report.bytesPlanned()))
                    : Text.translatable("cleanmcmod.command.clean.done",
                            report.cleanedTargets(), report.filesDeleted(),
                            CleanReport.formatBytes(report.bytesFreed())));
            for (TargetResult result : report.results()) {
                if (!result.successful()) {
                    continue;
                }
                CacheTarget target = targetsById.get(result.targetId());
                Text name = target != null ? Texts.targetName(target) : Text.literal(result.targetId());
                TargetResult.Counts counts = report.dryRun() ? result.planned() : result.deleted();
                source.sendFeedback(Text.literal(" • ")
                        .append(name)
                        .append(Text.literal(" — " + counts.files() + " · "
                                + CleanReport.formatBytes(counts.bytes()))));
            }
        }));
        return 1;
    }

    private static int openScreen(FabricClientCommandSource source) {
        MinecraftClient.getInstance().setScreen(
                new CleanMenuScreen(CleanMcModClient.service(), CleanMcModClient.updater()));
        return 1;
    }
}
