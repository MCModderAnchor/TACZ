package com.tacz.guns.client.resource.pojo.model;

import com.google.gson.annotations.SerializedName;
import net.minecraft.core.Direction;

public class FaceUVsItem {
    @SerializedName("down")
    private FaceItem down;
    @SerializedName("east")
    private FaceItem east;
    @SerializedName("north")
    private FaceItem north;
    @SerializedName("south")
    private FaceItem south;
    @SerializedName("up")
    private FaceItem up;
    @SerializedName("west")
    private FaceItem west;

    public static FaceUVsItem singleSouthFace() {
        FaceUVsItem faces = new FaceUVsItem();
        faces.north = FaceItem.EMPTY;
        faces.east = FaceItem.EMPTY;
        faces.west = FaceItem.EMPTY;
        faces.south = FaceItem.single16X();
        faces.up = FaceItem.EMPTY;
        faces.down = FaceItem.EMPTY;
        return faces;
    }

    public FaceItem getFace(Direction direction) {
        switch (direction) {
            case EAST:
                return west == null ? FaceItem.EMPTY : west;
            case WEST:
                return east == null ? FaceItem.EMPTY : east;
            case NORTH:
                return north == null ? FaceItem.EMPTY : north;
            case SOUTH:
                return south == null ? FaceItem.EMPTY : south;
            case UP:
                return down == null ? FaceItem.EMPTY : down;
            case DOWN:
            default:
                return up == null ? FaceItem.EMPTY : up;
        }
    }
}
