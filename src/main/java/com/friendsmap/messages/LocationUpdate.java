package com.friendsmap.messages;

import lombok.Getter;
import lombok.ToString;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;

public class LocationUpdate {
    private final int c;

    @Getter
    private final String PlayerName;

    @Getter
    private final int RegionId;


    public LocationUpdate(WorldPoint worldPoint, String PlayerName)
    {
       this(worldPoint, PlayerName, -3);
    }

    public LocationUpdate(WorldPoint worldPoint, String PlayerName, int regionId)
    {
        c = (worldPoint.getPlane() << 28) | (worldPoint.getX() << 14) | (worldPoint.getY());
        this.PlayerName = PlayerName;
        this.RegionId = regionId;
    }

    @ToString.Include
    public WorldPoint getWorldPoint()
    {
        return new WorldPoint(
                (c >> 14) & 0x3fff,
                c & 0x3fff,
                (c >> 28) & 3
        );
    }
}
