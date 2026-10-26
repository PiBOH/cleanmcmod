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
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("cleanmcmod")
                .executes(context -> help(context.getSource()))
                .then(ClientCommands.literal("scan")
                        .executes(context -> scan(context.getSource())))
                .then(ClientCommands.literal("clean")
                        .executes(context -> clean(context.getSource())))
                .then(ClientCommands.literal("screen")
                        .executes(context -> openScreen(context.getSource()))));
    }

    private static int help(FabricClientCommandSource source) {
        source.sendFeedback(Component.translatable("cleanmcmod.command.help"));
        return 1;
    }

    private static int scan(FabricClientCommandSource source) {
        CleanMcModService service = CleanMcModClient.service();
        CacheScan scan = service.scan();
        source.sendFeedback(Component.translatable("cleanmcmod.command.scan.header",
                scan.presentCount(), scan.files(), CleanReport.formatBytes(scan.bytes())));
        if (scan.empty()) {
            source.sendFeedback(Component.translatable("cleanmcmod.command.scan.empty"));
            return 1;
        }
        for (TargetState state : scan.presentTargets()) {
            source.sendFeedback(Component.literal(" • ")
                    .append(Texts.targetName(state.target()))
                    .append(Component.literal(" — " + state.files() + " · " + CleanReport.formatBytes(state.bytes()))));
        }
        return 1;
    }

    private static int clean(FabricClientCommandSource source) {
        CleanMcModService service = CleanMcModClient.service();
        if (service.isRunning()) {
            source.sendFeedback(Component.translatable("cleanmcmod.command.running"));
            return 0;
        }
        boolean dryRun = service.config().dryRun;
        source.sendFeedback(Component.translatable(dryRun
                ? "cleanmcmod.command.clean.started_dry"
                : "cleanmcmod.command.clean.started"));

        Map<String, CacheTarget> targetsById = new HashMap<>();
        List<CacheTarget> targets = service.targets();
        for (CacheTarget target : targets) {
            targetsById.put(target.id(), target);
        }

        Minecraft client = Minecraft.getInstance();
        service.cleanAsync().thenAccept(report -> client.execute(() -> {
            if (report == null) {
                source.sendFeedback(Component.translatable("cleanmcmod.command.clean.failed"));
                return;
            }
            source.sendFeedback(report.dryRun()
                    ? Component.translatable("cleanmcmod.command.clean.done_dry",
                            report.cleanedTargets(), report.filesPlanned(),
                            CleanReport.formatBytes(report.bytesPlanned()))
                    : Component.translatable("cleanmcmod.command.clean.done",
                            report.cleanedTargets(), report.filesDeleted(),
                            CleanReport.formatBytes(report.bytesFreed())));
            for (TargetResult result : report.results()) {
                if (!result.successful()) {
                    continue;
                }
                CacheTarget target = targetsById.get(result.targetId());
                Component name = target != null ? Texts.targetName(target) : Component.literal(result.targetId());
                TargetResult.Counts counts = report.dryRun() ? result.planned() : result.deleted();
                source.sendFeedback(Component.literal(" • ")
                        .append(name)
                        .append(Component.literal(" — " + counts.files() + " · "
                                + CleanReport.formatBytes(counts.bytes()))));
            }
        }));
        return 1;
    }

    private static int openScreen(FabricClientCommandSource source) {
        Minecraft.getInstance().setScreenAndShow(
                new CleanMenuScreen(CleanMcModClient.service(), CleanMcModClient.updater()));
        return 1;
    }
}
