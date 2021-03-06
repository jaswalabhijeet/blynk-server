package cc.blynk.server.application.handlers.main.logic.dashboard.widget.tile;

import cc.blynk.server.application.handlers.main.auth.AppStateHolder;
import cc.blynk.server.core.model.widgets.ui.tiles.DeviceTiles;
import cc.blynk.server.core.model.widgets.ui.tiles.TileTemplate;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.CommonByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split3;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class DeleteTileTemplateLogic {

    private static final Logger log = LogManager.getLogger(DeleteTileTemplateLogic.class);

    private DeleteTileTemplateLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, AppStateHolder state, StringMessage message) {
        var split = split3(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        var dashId = Integer.parseInt(split[0]);
        var widgetId = Long.parseLong(split[1]);
        var tileId = Long.parseLong(split[2]);

        var user = state.user;
        var dash = user.profile.getDashByIdOrThrow(dashId);
        var widget = dash.getWidgetByIdOrThrow(widgetId);

        if (!(widget instanceof DeviceTiles)) {
            throw new IllegalCommandException("Income widget id is not DeviceTiles.");
        }

        var deviceTiles = (DeviceTiles) widget;
        var existingTileIndex = deviceTiles.getTileTemplateIndexByIdOrThrow(tileId);

        log.debug("Deleting tile template dashId : {}, widgetId : {}, tileId : {}.", dash, widgetId, tileId);

        user.addEnergy(deviceTiles.templates[existingTileIndex].getPrice());
        deviceTiles.templates = ArrayUtil.remove(deviceTiles.templates, existingTileIndex, TileTemplate.class);
        deviceTiles.deleteDeviceTilesByTemplateId(tileId);

        dash.updatedAt = System.currentTimeMillis();

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
