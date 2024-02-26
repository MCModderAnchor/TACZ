package com.tac.guns.client.resource.model.bedrock.pojo;

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
        faces.north = FaceItem.empty();
        faces.east = FaceItem.empty();
        faces.west = FaceItem.empty();
        faces.south = FaceItem.single16X();
        faces.up = FaceItem.empty();
        faces.down = FaceItem.empty();
        return faces;
    }

    public FaceItem getFace(Direction direction) {
        switch (direction) {
            case EAST:
                return west == null ? FaceItem.empty() : west;
            case WEST:
                return east == null ? FaceItem.empty() : east;
            case NORTH:
                return north == null ? FaceItem.empty() : north;
            case SOUTH:
                return south == null ? FaceItem.empty() : south;
            case UP:
                return down == null ? FaceItem.empty() : down;
            case DOWN:
            default:
                return up == null ? FaceItem.empty() : up;
        }
    }
}
