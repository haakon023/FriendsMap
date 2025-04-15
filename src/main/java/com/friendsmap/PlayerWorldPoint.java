package com.friendsmap;

import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;

public class PlayerWorldPoint extends WorldMapPoint
{
    public static final int IMAGE_Z_OFFSET = 50;

    private static final BufferedImage ARROW = ImageUtil.loadImageResource(FriendsMapPlugin.class, "/util/clue_arrow.png");

    private BufferedImage partyImage;
    private final String PlayerName;

    public PlayerWorldPoint(final WorldPoint worldPoint, final String playerName) {
        super(worldPoint, ARROW);

        this.setSnapToEdge(true);
        this.setJumpOnClick(true);
        this.setName(playerName);

        this.setImagePoint(new Point(
                ARROW.getWidth() / 2,
                ARROW.getHeight()));

        this.PlayerName = playerName;
    }

    @Override
    public void onEdgeSnap()
    {
        this.setImage(null);
        this.setImagePoint(null);
    }

    @Override
    public void onEdgeUnsnap()
    {
        this.setImage(null);
        this.setImagePoint(null);
    }

    @Override
    public String getName()
    {
        return PlayerName;
    }

    @Override
    public String getTooltip()
    {
        return PlayerName;
    }

    @Override
    public BufferedImage getImage()
    {
        partyImage = new BufferedImage(ARROW.getWidth(), ARROW.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = partyImage.getGraphics();
        g.drawImage(ARROW, 0, 0, null);

        return partyImage;
    }
}
